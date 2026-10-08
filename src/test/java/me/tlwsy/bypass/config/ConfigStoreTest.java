package me.tlwsy.bypass.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ConfigStoreTest {
    @TempDir
    Path directory;

    @Test
    void firstLaunchEnablesOnlyBasicBypassAndSavesDefaults() throws IOException {
        Path file = directory.resolve("config/bypass-fabric-check.json");
        ConfigStore store = new ConfigStore(file);
        store.load();
        assertTrue(store.get().bypassEnabled());
        assertFalse(store.get().filterModdedEffects());
        assertTrue(Files.isRegularFile(file));
        ConfigStore reloaded = new ConfigStore(file);
        reloaded.load();
        assertEquals(BypassSettings.DEFAULTS, reloaded.get());
    }

    @Test
    void explicitChoicesSurviveRestart() throws IOException {
        Path file = directory.resolve("settings.json");
        ConfigStore store = new ConfigStore(file);
        store.save(new BypassSettings(false, true));
        ConfigStore reloaded = new ConfigStore(file);
        reloaded.load();
        assertEquals(new BypassSettings(false, true), reloaded.get());
    }

    @Test
    void missingFilterSettingDoesNotEnableFiltering() throws IOException {
        Path file = directory.resolve("settings.json");
        Files.writeString(file, "{\"bypassEnabled\":false}");
        ConfigStore store = new ConfigStore(file);
        store.load();
        assertEquals(new BypassSettings(false, false), store.get());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void commandsOnlyChangeBypassAndPreserveFilterChoice(boolean filter) throws IOException {
        ConfigStore store = new ConfigStore(directory.resolve("settings.json"));
        store.save(new BypassSettings(true, filter));
        store.setBypassEnabled(false);
        assertEquals(new BypassSettings(false, filter), store.get());
        store.setBypassEnabled(true);
        assertEquals(new BypassSettings(true, filter), store.get());
        store.load();
        assertEquals(new BypassSettings(true, filter), store.get());
    }

    @ParameterizedTest
    @ValueSource(strings = {"null", "[]", "{", "{\"filterModdedEffects\":\"true\"}",
            "{\"filterModdedEffects\":null}", "{\"bypassEnabled\":false,\"filterModdedEffects\":1}"})
    void malformedConfigKeepsSafeDefaultsAndOriginalFile(String content) throws IOException {
        Path file = directory.resolve("settings.json");
        Files.writeString(file, content);
        ConfigStore store = new ConfigStore(file);
        assertThrows(IOException.class, store::load);
        assertEquals(BypassSettings.DEFAULTS, store.get());
        assertEquals(content, Files.readString(file));
    }

    @Test
    void failedSaveDoesNotApplyUnsavedChanges() throws IOException {
        Path parent = directory.resolve("not-a-directory");
        Files.writeString(parent, "keep");
        ConfigStore store = new ConfigStore(parent.resolve("settings.json"));
        assertThrows(IOException.class, () -> store.save(new BypassSettings(false, true)));
        assertEquals(BypassSettings.DEFAULTS, store.get());
        assertEquals("keep", Files.readString(parent));
    }
}
