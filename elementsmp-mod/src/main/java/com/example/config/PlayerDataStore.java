package com.example.config;

import com.example.Elementsmp;
import com.example.element.Element;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Speichert die Element-Zuweisung pro Spieler-UUID in players.json.
 */
public final class PlayerDataStore {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Type MAP_TYPE = new TypeToken<Map<String, String>>() { }.getType();

	private final Path file;
	private final Map<UUID, Element> elements = new HashMap<>();

	public PlayerDataStore(Path file) {
		this.file = file;
	}

	public synchronized void load() {
		elements.clear();
		if (!Files.exists(file)) {
			return;
		}
		try (Reader reader = Files.newBufferedReader(file)) {
			Map<String, String> raw = GSON.fromJson(reader, MAP_TYPE);
			if (raw == null) {
				return;
			}
			for (Map.Entry<String, String> entry : raw.entrySet()) {
				try {
					Element element = Element.fromKey(entry.getValue());
					if (element != null) {
						elements.put(UUID.fromString(entry.getKey()), element);
					}
				} catch (IllegalArgumentException ex) {
					Elementsmp.LOGGER.warn("Ungueltiger Eintrag in players.json: {}", entry.getKey());
				}
			}
		} catch (IOException | JsonParseException ex) {
			Elementsmp.LOGGER.error("players.json konnte nicht gelesen werden", ex);
		}
	}

	public synchronized void save() {
		Map<String, String> raw = new HashMap<>();
		for (Map.Entry<UUID, Element> entry : elements.entrySet()) {
			raw.put(entry.getKey().toString(), entry.getValue().key());
		}
		Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
		try {
			Files.createDirectories(file.getParent());
			try (Writer writer = Files.newBufferedWriter(tmp)) {
				GSON.toJson(raw, writer);
			}
			Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException ex) {
			Elementsmp.LOGGER.error("players.json konnte nicht gespeichert werden", ex);
		}
	}

	public synchronized Element get(UUID uuid) {
		return elements.get(uuid);
	}

	public synchronized void set(UUID uuid, Element element) {
		elements.put(uuid, element);
		save();
	}

	public synchronized void remove(UUID uuid) {
		if (elements.remove(uuid) != null) {
			save();
		}
	}

	/** Anzahl bereits vergebener Elemente. */
	public synchronized Map<Element, Integer> counts() {
		Map<Element, Integer> counts = new EnumMap<>(Element.class);
		for (Element element : elements.values()) {
			counts.merge(element, 1, Integer::sum);
		}
		return counts;
	}
}
