package de.crossclick;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/** Einstellungen: Rolle, Taste, Modus. Tastenwahl wie im Pikachu-Styles-Menü per GLFW-Abfrage in tick(). */
public class CrossClickScreen extends Screen {
    private static final Map<Integer, String> KEY_NAMES = buildKeyNames();

    private final Screen parent;
    private boolean listening = false;
    private ButtonWidget roleBtn, keyBtn, modeBtn;

    public CrossClickScreen(Screen parent) {
        super(Text.literal("CrossClick"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        CrossClickConfig cfg = CrossClickConfig.get();
        int w = 220;
        int x = this.width / 2 - w / 2;
        int y = this.height / 2 - 40;

        roleBtn = this.addDrawableChild(ButtonWidget.builder(Text.empty(), b -> {
            cfg.role = cfg.isSender() ? "RECEIVER" : "SENDER";
            cfg.save();
            refresh();
        }).dimensions(x, y, w, 20).build());

        keyBtn = this.addDrawableChild(ButtonWidget.builder(Text.empty(), b -> {
            listening = true;
            refresh();
        }).dimensions(x, y + 26, w, 20).build());

        modeBtn = this.addDrawableChild(ButtonWidget.builder(Text.empty(), b -> {
            cfg.hold = !cfg.hold;
            cfg.save();
            refresh();
        }).dimensions(x, y + 52, w, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Fertig"), b -> close())
                .dimensions(x, y + 86, w, 20).build());
        refresh();
    }

    private void refresh() {
        CrossClickConfig cfg = CrossClickConfig.get();
        roleBtn.setMessage(Text.literal("Rolle: " + (cfg.isSender()
                ? "Sender (Account 1)" : "Empfänger (Account 2)")));
        keyBtn.setMessage(Text.literal(listening ? "Taste drücken..." : "Taste: " + keyName(cfg.key)));
        keyBtn.active = cfg.isSender();
        modeBtn.setMessage(Text.literal("Modus: " + (cfg.hold ? "Halten" : "Einzelklick")));
    }

    @Override
    public void tick() {
        super.tick();
        if (!listening || this.client == null) return;
        long handle = this.client.getWindow().getHandle();
        for (int k = GLFW.GLFW_KEY_SPACE; k <= GLFW.GLFW_KEY_LAST; k++) {
            if (GLFW.glfwGetKey(handle, k) != GLFW.GLFW_PRESS) continue;
            if (k == GLFW.GLFW_KEY_ESCAPE) {
                // Abbruch, Belegung bleibt
            } else if (k == GLFW.GLFW_KEY_DELETE || k == GLFW.GLFW_KEY_BACKSPACE) {
                CrossClickConfig.get().key = -1;
                CrossClickConfig.get().save();
            } else {
                CrossClickConfig.get().key = k;
                CrossClickConfig.get().save();
            }
            listening = false;
            refresh();
            return;
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        int cx = this.width / 2;
        int top = this.height / 2 - 40;
        Text title = Text.literal("CrossClick");
        context.drawText(this.textRenderer, title, cx - this.textRenderer.getWidth(title) / 2, top - 28, 0xFFFFFFFF, true);
        Text hint = Text.literal("Rolle ändern: Spiel danach neu starten. Entf = Taste löschen.");
        context.drawText(this.textRenderer, hint, cx - this.textRenderer.getWidth(hint) / 2, top + 114, 0xFFAAAAAA, false);
    }

    @Override
    public void close() {
        if (this.client != null) this.client.setScreen(parent);
    }

    private static String keyName(int key) {
        if (key < 0) return "Nicht belegt";
        String n = KEY_NAMES.get(key);
        return n != null ? n : "Taste " + key;
    }

    private static Map<Integer, String> buildKeyNames() {
        Map<Integer, String> map = new HashMap<>();
        for (Field f : GLFW.class.getFields()) {
            String n = f.getName();
            if (!n.startsWith("GLFW_KEY_") || f.getType() != int.class) continue;
            String shortName = n.substring("GLFW_KEY_".length());
            if (shortName.equals("LAST") || shortName.equals("UNKNOWN")) continue;
            try {
                map.putIfAbsent(f.getInt(null), shortName.replace('_', ' '));
            } catch (IllegalAccessException ignored) {
            }
        }
        return map;
    }
}
