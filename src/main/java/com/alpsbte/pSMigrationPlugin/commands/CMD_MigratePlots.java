package com.alpsbte.pSMigrationPlugin.commands;

import com.alpsbte.pSMigrationPlugin.PSMigrationPlugin;
import com.alpsbte.pSMigrationPlugin.core.database.PlotDataProvider;
import com.alpsbte.pSMigrationPlugin.core.database.model.PlotV1;
import com.alpsbte.pSMigrationPlugin.core.database.model.PlotV2;
import com.alpsbte.pSMigrationPlugin.core.database.model.Status;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.BuiltInClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardWriter;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.math.transform.AffineTransform;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

public class CMD_MigratePlots implements CommandExecutor {
    public final static String schematicsPath = Paths.get(PSMigrationPlugin.getPlugin().getDataFolder().getAbsolutePath(), "schematics") + File.separator;

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String @NotNull [] strings) {
        if (!commandSender.hasPermission("plotsystem.admin")) return true;

        List<PlotV2> migrationPlots = PlotDataProvider.getMigrationPlots();
        commandSender.sendMessage(Component.text("Found " + migrationPlots.size() + " plots to migrate..."));

        for (PlotV2 plot : migrationPlots) migratePlot(plot);

        return false;
    }

    private void migratePlot(PlotV2 migratePlot) {
        PSMigrationPlugin.getPlugin().getComponentLogger().info("Migrating plot {}...", migratePlot.getId());

        Optional<PlotV1> oldPlot = PlotDataProvider.getOldPlot(migratePlot.getId());
        if (oldPlot.isEmpty()) return;

        // Mc Version
        String mcVersion = null;
        if (migratePlot.getStatus().equals(Status.COMPLETED)) {
            mcVersion = Bukkit.getServer().getMinecraftVersion();
            PSMigrationPlugin.getPlugin().getComponentLogger().info("set mc version to {}", mcVersion);
        }

        // Initial Schematic
        InitialSchematicData initialSchematic = getInitialSchematic(oldPlot.get());

        // completed schematic
        byte[] completedSchematic = null;
        if (migratePlot.getStatus().equals(Status.COMPLETED)) {
            completedSchematic = getCompletedSchematic(oldPlot.get(), initialSchematic.minY);
        }

        PlotDataProvider.updatePlot(migratePlot, mcVersion, initialSchematic.initialSchematic, completedSchematic);
        PSMigrationPlugin.getPlugin().getComponentLogger().info("#{} migrated!", migratePlot.getId());
    }

    private InitialSchematicData getInitialSchematic(PlotV1 oldPlot) {
        Path filePath = Paths.get(
                schematicsPath,
                String.valueOf(oldPlot.getServerId()),
                String.valueOf(oldPlot.getCityProjectId()),
                oldPlot.getId() + "-env.schem");
        File environmentSchematic = filePath.toFile();

        Clipboard clipboard;
        try (ClipboardReader reader = BuiltInClipboardFormat.FAST_V2.getReader(new FileInputStream(environmentSchematic))) {
            clipboard = reader.read();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        int minY = clipboard.getRegion().getMinimumY();

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (ClipboardWriter writer = BuiltInClipboardFormat.FAST_V2.getWriter(outputStream)) {
            writer.write(clipboard);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return new InitialSchematicData(outputStream.toByteArray(), minY);
    }

    private byte[] getCompletedSchematic(PlotV1 oldPlot, int initialMinY) {
        Path filePath = Paths.get(
                schematicsPath,
                String.valueOf(oldPlot.getServerId()),
                "finishedSchematics",
                String.valueOf(oldPlot.getCityProjectId()),
                oldPlot.getId() + ".schem");
        File completedSchematicFile = filePath.toFile();

        Clipboard clipboard;
        try (ClipboardReader reader = BuiltInClipboardFormat.FAST_V2.getReader(new FileInputStream(completedSchematicFile))) {
            clipboard = reader.read();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (ClipboardWriter writer = BuiltInClipboardFormat.FAST_V2.getWriter(outputStream)) {
            double clipboardMinY = clipboard.getRegion().getMinimumY();
            double offset = clipboardMinY - initialMinY;
            writer.write(clipboard.transform(new AffineTransform().translate(Vector3.at(0,offset,0))));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return outputStream.toByteArray();
    }

    private record InitialSchematicData(byte[] initialSchematic, int minY) { }
}
