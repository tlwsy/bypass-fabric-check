package me.tlwsy.bypass.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;

public final class ConfigStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final Path path;
    // One immutable snapshot is shared by the menu, server thread and network threads.
    private volatile BypassSettings settings = BypassSettings.DEFAULTS;

    public ConfigStore(Path path) {
        this.path = path.toAbsolutePath();
    }

    public BypassSettings get() {
        return settings;
    }

    public synchronized void load() throws IOException {
        if (!Files.exists(path)) {
            save(BypassSettings.DEFAULTS);
            return;
        }

        try {
            JsonObject json = JsonParser.parseString(Files.readString(path)).getAsJsonObject();
            settings = new BypassSettings(
                    readBoolean(json, "bypassEnabled", true),
                    readBoolean(json, "filterModdedEffects", false));
        } catch (JsonParseException | IllegalStateException error) {
            throw new IOException("Invalid config: " + path, error);
        }
    }

    public synchronized void save(BypassSettings updated) throws IOException {
        Objects.requireNonNull(updated);
        Files.createDirectories(path.getParent());
        Path temporary = Files.createTempFile(path.getParent(), "bypass-fabric-check-", ".tmp");
        try {
            Files.writeString(temporary, GSON.toJson(updated) + System.lineSeparator());
            try {
                Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING);
            }
            settings = updated;
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    public synchronized void setBypassEnabled(boolean enabled) throws IOException {
        // Commands must never change or implicitly enable packet filtering.
        save(settings.withBypassEnabled(enabled));
    }

    private static boolean readBoolean(JsonObject json, String name, boolean fallback) {
        JsonElement value = json.get(name);
        if (value == null) return fallback;
        if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isBoolean()) {
            throw new JsonParseException(name + " must be a boolean");
        }
        return value.getAsBoolean();
    }
}
