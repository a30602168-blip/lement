package com.example.config;

import com.example.Elementsmp;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ConfigManager {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	private final Path file;
	private ElementConfig config = new ElementConfig();

	public ConfigManager(Path directory) {
		this.file = directory.resolve("config.json");
		this.config.fillDefaults();
	}

	public ElementConfig config() {
		return config;
	}

	/**
	 * Laedt die Konfiguration. Bei einer kaputten Datei bleibt die bisherige Konfiguration aktiv
	 * und die Datei wird nicht ueberschrieben.
	 *
	 * @return true, wenn erfolgreich geladen
	 */
	public boolean load() {
		if (!Files.exists(file)) {
			config = new ElementConfig();
			config.fillDefaults();
			save();
			return true;
		}
		try (Reader reader = Files.newBufferedReader(file)) {
			ElementConfig loaded = GSON.fromJson(reader, ElementConfig.class);
			if (loaded == null) {
				loaded = new ElementConfig();
			}
			loaded.fillDefaults();
			config = loaded;
			save();
			return true;
		} catch (IOException | JsonParseException ex) {
			Elementsmp.LOGGER.error("config.json konnte nicht geladen werden, behalte bisherige Werte", ex);
			return false;
		}
	}

	public void save() {
		try {
			Files.createDirectories(file.getParent());
			try (Writer writer = Files.newBufferedWriter(file)) {
				GSON.toJson(config, writer);
			}
		} catch (IOException ex) {
			Elementsmp.LOGGER.error("config.json konnte nicht gespeichert werden", ex);
		}
	}
}
