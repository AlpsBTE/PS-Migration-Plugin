package com.alpsbte.pSMigrationPlugin.commands;

import com.alpsbte.alpslib.utils.AlpsUtils;
import com.alpsbte.pSMigrationPlugin.PSMigrationPlugin;
import com.alpsbte.pSMigrationPlugin.core.MyOS;
import com.alpsbte.pSMigrationPlugin.core.database.model.PlotV1;
import com.alpsbte.pSMigrationPlugin.core.database.model.PlotV2;
import com.alpsbte.pSMigrationPlugin.core.database.model.Status;
import com.alpsbte.pSMigrationPlugin.core.database.provider.PlotDataProvider;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.BuiltInClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardWriter;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class CMD_MigratePlots implements CommandExecutor {
    public static final String schematicsPath = Paths.get(Bukkit.getPluginsFolder().getAbsolutePath(), "Plot-System", "schematics") + File.separator;

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String @NotNull [] strings) {
        if (!commandSender.hasPermission("plotsystem.admin")) return true;

        CompletableFuture.runAsync(() -> {
            List<PlotV2> migrationPlots = PlotDataProvider.getMigrationPlots();

            int max = (strings.length > 0) ? Objects.requireNonNullElse(AlpsUtils.tryParseInt(strings[0]), migrationPlots.size()) : migrationPlots.size();

            commandSender.sendMessage(Component.text("Found " + migrationPlots.size() + " plots to migrate, limit is " + max + "..."));

            int count = 0;

            for (PlotV2 plot : migrationPlots) {
                if (count > max) break;
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    PSMigrationPlugin.getPlugin().getComponentLogger().error("Could not sleep thread!");
                    Thread.currentThread().interrupt();
                }
                try {
                    migratePlot(plot);
                    count++;
                } catch (Exception e) {
                    PSMigrationPlugin.getPlugin().getComponentLogger().error("Could not migrate plot #{}!", plot.getId(), e);
                }
            }
            commandSender.sendMessage(Component.text("Migrated " + count + " plots!"));
            PSMigrationPlugin.getPlugin().getComponentLogger().info("Migrated {} plots!", count);
        });
        return false;
    }

    private void migratePlot(@NotNull PlotV2 migratePlot) {
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

    @Contract("_ -> new")
    private @NotNull InitialSchematicData getInitialSchematic(@NotNull PlotV1 oldPlot) {
        Path filePath = Paths.get(
                schematicsPath,
                String.valueOf(oldPlot.getServerId()),
                String.valueOf(oldPlot.getCityProjectId()),
                oldPlot.getId() + "-env.schem");
        File environmentSchematic = filePath.toFile();

        boolean useGzipWorkaround = false;

        if (!environmentSchematic.exists()) {
            filePath = Paths.get(
                    schematicsPath,
                    String.valueOf(oldPlot.getServerId()),
                    String.valueOf(oldPlot.getCityProjectId()),
                    oldPlot.getId() + "-env.schematic");
            environmentSchematic = filePath.toFile();
            useGzipWorkaround = true;
        }

        if (!environmentSchematic.exists()) {
            filePath = Paths.get(
                    schematicsPath,
                    String.valueOf(oldPlot.getServerId()),
                    String.valueOf(oldPlot.getCityProjectId()),
                    oldPlot.getId() + ".schematic");
            environmentSchematic = filePath.toFile();
            useGzipWorkaround = true;
        }

        PSMigrationPlugin.getPlugin().getComponentLogger().info("Load from file & write to database initial schematic: {}", filePath);

        Clipboard clipboard;
        ClipboardFormat format = ClipboardFormats.findByFile(environmentSchematic);
        try (ClipboardReader reader = format.getReader(new FileInputStream(environmentSchematic))) {
            clipboard = reader.read();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        int minY = clipboard.getRegion().getMinimumY();

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (ClipboardWriter writer = BuiltInClipboardFormat.FAST_V2.getWriter(useGzipWorkaround ? new MyOS(outputStream) : outputStream)) {
            writer.write(clipboard);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return new InitialSchematicData(outputStream.toByteArray(), minY);
    }

    private byte @NotNull [] getCompletedSchematic(@NotNull PlotV1 oldPlot, int initialMinY) {
        Path filePath = Paths.get(
                schematicsPath,
                String.valueOf(oldPlot.getServerId()),
                "finishedSchematics",
                String.valueOf(oldPlot.getCityProjectId()),
                oldPlot.getId() + ".schem");
        File completedSchematicFile = filePath.toFile();

        boolean useGzipWorkaround = false;

        if (!completedSchematicFile.exists()) {
            filePath = Paths.get(
                    schematicsPath,
                    String.valueOf(oldPlot.getServerId()),
                    "finishedSchematics",
                    String.valueOf(oldPlot.getCityProjectId()),
                    oldPlot.getId() + ".schematic");
            completedSchematicFile = filePath.toFile();
            useGzipWorkaround = true;
        }

        PSMigrationPlugin.getPlugin().getComponentLogger().info("Load from file & write to database completed schematic: {}", filePath);

        Clipboard clipboard;

        ClipboardFormat format = ClipboardFormats.findByFile(completedSchematicFile);
        try (ClipboardReader reader = format.getReader(new FileInputStream(completedSchematicFile))) {
            clipboard = reader.read();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (ClipboardWriter writer = BuiltInClipboardFormat.FAST_V2.getWriter(useGzipWorkaround ? new MyOS(outputStream) : outputStream)) {
            //double clipboardMinY = clipboard.getRegion().getMinimumY();
            //double offset = clipboardMinY - initialMinY;
            writer.write(clipboard/*.transform(new AffineTransform().translate(Vector3.at(0,offset,0)))*/);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return outputStream.toByteArray();
    }

    private record InitialSchematicData(byte[] initialSchematic, int minY) { }
}
