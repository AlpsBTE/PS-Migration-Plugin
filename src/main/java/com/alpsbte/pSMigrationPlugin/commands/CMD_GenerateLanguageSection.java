package com.alpsbte.pSMigrationPlugin.commands;

import com.alpsbte.pSMigrationPlugin.PSMigrationPlugin;
import com.alpsbte.pSMigrationPlugin.core.database.model.CityProjectV1;
import com.alpsbte.pSMigrationPlugin.core.database.provider.CityProjectDataProvider;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class CMD_GenerateLanguageSection implements CommandExecutor {
    private static final String indentation = "  ";

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String @NotNull [] strings) {
        if (!commandSender.hasPermission("plotsystem.admin")) return true;

        File file = new File(PSMigrationPlugin.getPlugin().getDataFolder(), "languageSection.txt");
        createTextFile(file);
        return false;
    }

    public void createTextFile(File file) {
        StringBuilder sb = new StringBuilder();
        sb.append("database:\n");

        sb.append(indentation).append("city-project:\n");
        List<CityProjectV1> cities = CityProjectDataProvider.getAllCityProjects();
        for (CityProjectV1 city : cities) {
            String newId = city.getName().toLowerCase()
                    .replace(" ", "-")
                    .replace("`", "")
                    .replace("'", "")
                    .replace("ä", "ae")
                    .replace("ö", "oe")
                    .replace("ü", "ue")
                    .replace("ß", "ss");
            sb.append(indentation.repeat(2)).append(newId).append(":\n");
            sb.append(indentation.repeat(3)).append("name: '").append(city.getName()).append("'\n");
            sb.append(indentation.repeat(3)).append("description: '").append(city.getDescription()).append("'\n");
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write(sb.toString());
        } catch (IOException e) {
            PSMigrationPlugin.getPlugin().getComponentLogger().error("Error occurred while writing to file {}", file.getName());
        }
    }
}
