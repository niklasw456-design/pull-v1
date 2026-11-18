package de.crossclick;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class CrossClickConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static CrossClickConfig instance = new CrossClickConfig();

    /** "SENDER" (Account 1, hat die Taste) oder "RECEIVER" (Account 2, führt den Rechtsklick aus). */
    public String role = "SENDER";

    /** GLFW-Keycode der Taste (nur Sender), -1 = nicht belegt. Standard: R. */
    public int key = 82;

    /** UDP-Port auf localhost. Muss bei beiden Clients gleich sein. */
    public int port = 25599;

    /** false = ein Rechtsklick pro Tastendruck, true = Rechtsklick gehalten, solange die Taste unten ist. */
    public boolean hold = false;

    public static CrossClickConfig get() {
        return instance;
    }

    public boolean isSender() {
        return !"RECEIVER".equalsIgnoreCase(role);
    }

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("crossclick.json");
    }

    public static void load() {
        Path f = file();
        if (!Files.isRegularFile(f)) {
            instance.save();
            return;
        }
        try {
            CrossClickConfig loaded = GSON.fromJson(Files.readString(f), CrossClickConfig.class);
            if (loaded != null) {
                if (loaded.port < 1024 || loaded.port > 65535) loaded.port = 25599;
                instance = loaded;
            }
        } catch (Exception e) {
            CrossClickClient.LOGGER.warn("Konnte Config nicht lesen, nutze Standardwerte", e);
        }
    }

    public void save() {
        try {
            Files.createDirectories(file().getParent());
            Files.writeString(file(), GSON.toJson(this));
        } catch (IOException e) {
            CrossClickClient.LOGGER.warn("Konnte Config nicht speichern", e);
        }
    }
}
