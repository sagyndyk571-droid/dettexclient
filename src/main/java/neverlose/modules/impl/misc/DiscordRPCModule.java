package neverlose.modules.impl.misc;

import com.adl.nativeprotect.User;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import neverlose.events.api.EventHandler;
import neverlose.events.impl.TickEvent;
import neverlose.modules.module.ModuleStructure;
import neverlose.modules.module.category.ModuleCategory;
import neverlose.modules.module.setting.implement.BooleanSetting;
import neverlose.util.Instance;
import neverlose.util.config.impl.consolelogger.Logger;

import java.io.Closeable;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class DiscordRPCModule extends ModuleStructure {
    private static final String APPLICATION_ID = "1370033521955049542";
    private static final String IMAGE_KEY = "https://media1.tenor.com/m/KEwIgZLY_w0AAAAC/neverlose-client.gif";
    private static final String SMALL_IMAGE_KEY = "https://media1.tenor.com/m/KEwIgZLY_w0AAAAC/neverlose-client.gif";
    private static final String IMAGE_TEXT = "https://t.me/neverlosedevlog";
    private static final long UPDATE_DELAY_MS = 5_000L;

    private final BooleanSetting showServer = new BooleanSetting("Show Server", "Show current server in Discord RPC")
            .setValue(true);
    private final BooleanSetting showUid = new BooleanSetting("Show UID", "Show client UID in Discord RPC")
            .setValue(true);
    private final BooleanSetting showRole = new BooleanSetting("Show Role", "Show client role in Discord RPC")
            .setValue(true);

    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicLong sessionId = new AtomicLong();
    private final DiscordIpcClient ipcClient = new DiscordIpcClient(APPLICATION_ID);
    private long startTimestamp;
    private Thread rpcThread;

    public DiscordRPCModule() {
        super("DiscordRPC", "Discord Rich Presence", ModuleCategory.MISC);
        settings(showServer, showUid, showRole);
    }

    public static DiscordRPCModule getInstance() {
        return Instance.get(DiscordRPCModule.class);
    }

    @Override
    public void activate() {
        startRPC();
    }

    @Override
    public void deactivate() {
//        stopRPC();
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (!isState()) {
            if (running.get()) {
          //a      stopRPC();
            }
            return;
        }

        if (mc.player == null || mc.world == null) {
            return;
        }

        if (!running.get()) {
            startRPC();
        }
    }

    public void startRPC() {
        if (!running.compareAndSet(false, true)) {
            return;
        }

        startTimestamp = System.currentTimeMillis() / 1000L;
        ipcClient.resetAssets();
        long session = sessionId.incrementAndGet();
        startThread(session);
    }

//    public void stopRPC() {
//        if (!running.compareAndSet(true, false)) {
//            return;
//        }
//
//        sessionId.incrementAndGet();
//        ipcClient.close();
//        if (rpcThread != null) {
//            rpcThread.interrupt();
//        }
//        rpcThread = null;
//    }

    private void startThread(long session) {
        rpcThread = new Thread(() -> {
            while (running.get() && sessionId.get() == session) {
                try {
                    updatePresence();
                    Thread.sleep(UPDATE_DELAY_MS);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Throwable throwable) {
                    Logger.error("DiscordRPC: update failed: " + throwable.getMessage());
                    ipcClient.close();
                }
            }
            if (sessionId.get() == session) {
                running.set(false);
            }
        }, "Neverlose-DiscordRPC");

        rpcThread.setDaemon(true);
        rpcThread.start();
    }

    private void updatePresence() {
        if (!running.get()) {
            return;
        }

        User user = User.getInstance();
        String role = safe(user.profile("role"), "User");
        String uid = safe(user.profile("uid"), "unknown");

        String details = showRole.isValue()
                ? "Role: " + role
                : "DettexClient";

        String state = showUid.isValue()
                ? "UID: " + uid
                : "Playing";
        if (showServer.isValue()) {
            state += " | " + getServerName();
        }

        ipcClient.updateActivity(details, state, startTimestamp, IMAGE_KEY, SMALL_IMAGE_KEY, IMAGE_TEXT);
    }

    private String getServerName() {
        if (mc == null) {
            return "Main menu";
        }
        if (mc.getCurrentServerEntry() != null && mc.getCurrentServerEntry().address != null) {
            return mc.getCurrentServerEntry().address;
        }
        if (mc.world != null) {
            return mc.isInSingleplayer() ? "Singleplayer" : "Server";
        }
        return "Main menu";
    }

    private String safe(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private final class DiscordIpcClient implements Closeable {
        private static final Gson GSON = new Gson();
        private static final int OPCODE_HANDSHAKE = 0;
        private static final int OPCODE_FRAME = 1;
        private static final int OPCODE_CLOSE = 2;
        private static final int MAX_FRAME_SIZE = 32_768;

        private final String clientId;
        private volatile RandomAccessFile pipe;
        private volatile boolean ready;
        private volatile boolean assetsAllowed = true;
        private Thread readerThread;

        private DiscordIpcClient(String clientId) {
            this.clientId = clientId;
        }

        synchronized void updateActivity(String details,
                                         String state,
                                         long startTimestamp,
                                         String largeImage,
                                         String smallImage,
                                         String largeText) {
            if (!ensureConnected()) {
                return;
            }
            if (!ready) {
                return;
            }

            JsonObject activity = new JsonObject();
            activity.addProperty("details", details);
            activity.addProperty("state", state);

            JsonObject timestamps = new JsonObject();
            timestamps.addProperty("start", startTimestamp);
            activity.add("timestamps", timestamps);

            if (assetsAllowed) {
                JsonObject assets = new JsonObject();
                if (largeImage != null && !largeImage.isBlank()) {
                    assets.addProperty("large_image", largeImage);
                }
                if (smallImage != null && !smallImage.isBlank()) {
                    assets.addProperty("small_image", smallImage);
                }
                assets.addProperty("large_text", largeText);
                assets.addProperty("small_text", "DettexClient");
                activity.add("assets", assets);
            }

            JsonObject args = new JsonObject();
            args.addProperty("pid", ProcessHandle.current().pid());
            args.add("activity", activity);

            sendCommand("SET_ACTIVITY", args);
        }

        synchronized void resetAssets() {
            assetsAllowed = true;
        }

        synchronized void clearActivity() {
            if (!ensureConnected()) {
                return;
            }
            if (!ready) {
                return;
            }

            JsonObject args = new JsonObject();
            args.addProperty("pid", ProcessHandle.current().pid());
            args.add("activity", null);
            sendCommand("SET_ACTIVITY", args);
        }

        private void sendCommand(String command, JsonObject args) {
            try {
                JsonObject frame = new JsonObject();
                frame.addProperty("cmd", command);
                frame.add("args", args);
                frame.addProperty("nonce", UUID.randomUUID().toString());
                writeFrame(OPCODE_FRAME, frame);
            } catch (IOException ignored) {
                Logger.error("DiscordRPC: command failed, reconnecting");
                close();
            }
        }

        private boolean ensureConnected() {
            if (pipe != null) {
                return true;
            }

            RandomAccessFile openedPipe = openPipe();
            if (openedPipe == null) {
                return false;
            }
            pipe = openedPipe;
            ready = false;

            try {
                JsonObject handshake = new JsonObject();
                handshake.addProperty("v", 1);
                handshake.addProperty("client_id", clientId);
                startReader(openedPipe);
                writeFrame(OPCODE_HANDSHAKE, handshake);
                return true;
            } catch (IOException exception) {
                Logger.error("DiscordRPC: handshake failed: " + exception.getMessage());
                close();
                return false;
            }
        }

        private RandomAccessFile openPipe() {
            for (String path : getPipePaths()) {
                try {
                    return new RandomAccessFile(path, "rw");
                } catch (IOException ignored) {
                }
            }
            Logger.error("DiscordRPC: Discord IPC pipe not found. Discord desktop app is probably closed.");
            return null;
        }

        private void startReader(RandomAccessFile connectedPipe) {
            readerThread = new Thread(() -> {
                byte[] headerBytes = new byte[8];
                while (pipe == connectedPipe) {
                    try {
                        connectedPipe.readFully(headerBytes);
                        ByteBuffer header = ByteBuffer.wrap(headerBytes).order(ByteOrder.LITTLE_ENDIAN);
                        int opcode = header.getInt();
                        int length = header.getInt();
                        if (length < 0 || length > MAX_FRAME_SIZE) {
                            break;
                        }
                        byte[] payload = new byte[length];
                        connectedPipe.readFully(payload);
                        handleFrame(opcode, payload);
                    } catch (IOException ignored) {
                        break;
                    }
                }

                synchronized (this) {
                    if (pipe == connectedPipe) {
                        close();
                    }
                }
            }, "Neverlose-DiscordRPC-Reader");

            readerThread.setDaemon(true);
            readerThread.start();
        }

        private void handleFrame(int opcode, byte[] payload) {
            String json = new String(payload, StandardCharsets.UTF_8);
            try {
                JsonObject object = GSON.fromJson(json, JsonObject.class);
                String event = object.has("evt") && !object.get("evt").isJsonNull()
                        ? object.get("evt").getAsString()
                        : "";
                if ("READY".equalsIgnoreCase(event)) {
                    ready = true;
                    Logger.success("DiscordRPC: connected");
                    if (running.get()) {
                        updatePresence();
                    }
                    return;
                }
                if ("ERROR".equalsIgnoreCase(event) || opcode == OPCODE_CLOSE) {
                    Logger.error("DiscordRPC: " + json);
                    if (json.toLowerCase(Locale.ROOT).contains("asset") || json.toLowerCase(Locale.ROOT).contains("image")) {
                        assetsAllowed = false;
                        Logger.info("DiscordRPC: retrying without image assets");
                    }
                    close();
                }
            } catch (Throwable throwable) {
                Logger.error("DiscordRPC: bad IPC frame: " + throwable.getMessage());
            }
        }

        private List<String> getPipePaths() {
            List<String> paths = new ArrayList<>();
            String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
            for (int i = 0; i < 10; i++) {
                if (os.contains("win")) {
                    paths.add("\\\\?\\pipe\\discord-ipc-" + i);
                    paths.add("\\\\.\\pipe\\discord-ipc-" + i);
                } else {
                    for (String basePath : getUnixPipeBases()) {
                        paths.add(basePath + "/discord-ipc-" + i);
                    }
                }
            }
            return paths;
        }

        private List<String> getUnixPipeBases() {
            List<String> paths = new ArrayList<>();
            addEnvPath(paths, "XDG_RUNTIME_DIR");
            addEnvPath(paths, "TMPDIR");
            addEnvPath(paths, "TMP");
            addEnvPath(paths, "TEMP");
            paths.add("/tmp");
            return paths;
        }

        private void addEnvPath(List<String> paths, String name) {
            String value = System.getenv(name);
            if (value != null && !value.isBlank()) {
                paths.add(value);
            }
        }

        private void writeFrame(int opcode, JsonObject payload) throws IOException {
            RandomAccessFile currentPipe = pipe;
            if (currentPipe == null) {
                throw new IOException("Discord IPC pipe is closed");
            }

            byte[] json = GSON.toJson(payload).getBytes(StandardCharsets.UTF_8);
            ByteBuffer header = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN);
            header.putInt(opcode);
            header.putInt(json.length);
            currentPipe.write(header.array());
            currentPipe.write(json);
        }

        @Override
        public synchronized void close() {
            RandomAccessFile currentPipe = pipe;
            Thread currentReader = readerThread;
            pipe = null;
            ready = false;
            readerThread = null;
            if (currentReader != null) {
                currentReader.interrupt();
            }
            if (currentPipe != null) {
                try {
                    currentPipe.close();
                } catch (IOException ignored) {
                }
            }
        }
    }
}
