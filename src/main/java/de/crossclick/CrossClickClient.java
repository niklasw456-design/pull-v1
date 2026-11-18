package de.crossclick;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Account 1 (SENDER): Taste gedrückt/losgelassen -> UDP-Nachricht "DOWN"/"UP" an 127.0.0.1.
 * Account 2 (RECEIVER): empfängt die Nachrichten und hält die "Benutzen"-Taste (Rechtsklick) gedrückt.
 * Die Rolle wird in config/crossclick.json oder im Mod-Menü eingestellt (nach Änderung Neustart).
 */
public class CrossClickClient implements ClientModInitializer {
    public static final String MOD_ID = "crossclick";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    /** Wie viele Ticks ein einzelner Klick (hold = false) gedrückt bleibt. */
    private static final int CLICK_TICKS = 2;

    // Sender
    private DatagramSocket sendSocket;
    private InetAddress localhost;
    private boolean wasDown = false;

    // Empfänger (Netzwerk-Thread -> Client-Thread)
    private volatile DatagramSocket recvSocket;
    private final AtomicInteger downEvents = new AtomicInteger();
    private final AtomicInteger upEvents = new AtomicInteger();
    private int holdTicks = 0;
    private boolean useHeld = false;

    @Override
    public void onInitializeClient() {
        CrossClickConfig.load();
        CrossClickConfig cfg = CrossClickConfig.get();
        if (cfg.isSender()) {
            setupSender(cfg);
        } else {
            setupReceiver(cfg);
        }
    }

    // ---------------- Sender ----------------

    private void setupSender(CrossClickConfig cfg) {
        try {
            sendSocket = new DatagramSocket();
            localhost = InetAddress.getByName("127.0.0.1");
        } catch (IOException e) {
            LOGGER.error("Sender konnte nicht gestartet werden", e);
            return;
        }
        ClientTickEvents.END_CLIENT_TICK.register(this::senderTick);
        LOGGER.info("CrossClick: SENDER, Port {}", cfg.port);
    }

    private void senderTick(MinecraftClient client) {
        int key = CrossClickConfig.get().key;
        if (key < 0 || client.getWindow() == null) {
            wasDown = false;
            return;
        }
        // Nur im Spiel (nicht beim Tippen im Chat oder in Menüs)
        boolean down = client.currentScreen == null && client.player != null
                && GLFW.glfwGetKey(client.getWindow().getHandle(), key) == GLFW.GLFW_PRESS;
        if (down != wasDown) {
            send(down ? "DOWN" : "UP");
            wasDown = down;
        }
    }

    private void send(String msg) {
        try {
            byte[] data = msg.getBytes(StandardCharsets.UTF_8);
            sendSocket.send(new DatagramPacket(data, data.length, localhost, CrossClickConfig.get().port));
        } catch (IOException e) {
            LOGGER.warn("Senden fehlgeschlagen", e);
        }
    }

    // ---------------- Empfänger ----------------

    private void setupReceiver(CrossClickConfig cfg) {
        Thread t = new Thread(() -> {
            try (DatagramSocket socket = new DatagramSocket(cfg.port, InetAddress.getByName("127.0.0.1"))) {
                recvSocket = socket;
                byte[] buf = new byte[32];
                while (!socket.isClosed()) {
                    DatagramPacket p = new DatagramPacket(buf, buf.length);
                    socket.receive(p);
                    String msg = new String(p.getData(), 0, p.getLength(), StandardCharsets.UTF_8);
                    if (msg.equals("DOWN")) downEvents.incrementAndGet();
                    else if (msg.equals("UP")) upEvents.incrementAndGet();
                }
            } catch (IOException e) {
                LOGGER.error("Empfänger-Port {} nicht verfügbar (läuft schon ein anderer Empfänger?)", cfg.port, e);
            }
        }, "CrossClick-Receiver");
        t.setDaemon(true);
        t.start();
        ClientTickEvents.END_CLIENT_TICK.register(this::receiverTick);
        LOGGER.info("CrossClick: RECEIVER, Port {}", cfg.port);
    }

    private void receiverTick(MinecraftClient client) {
        // Fenster darf im Hintergrund weiterlaufen
        client.options.pauseOnLostFocus = false;

        boolean hold = CrossClickConfig.get().hold;
        int downs = downEvents.getAndSet(0);
        int ups = upEvents.getAndSet(0);
        boolean inGame = client.player != null && client.currentScreen == null;

        if (downs > 0 && inGame) {
            useHeld = true;
            holdTicks = CLICK_TICKS;
        }
        if (hold) {
            // gehalten: bis "UP" kommt (UP gewinnt, falls beides im selben Tick ankam)
            if (ups > 0) useHeld = false;
        } else if (useHeld && --holdTicks <= 0) {
            useHeld = false;
        }
        if (!inGame) useHeld = false;

        client.options.useKey.setPressed(useHeld);
    }
}
