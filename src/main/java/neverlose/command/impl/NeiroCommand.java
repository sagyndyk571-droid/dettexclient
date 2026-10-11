package neverlose.command.impl;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.entity.LivingEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.MathHelper;
import neverlose.IMinecraft;
import neverlose.command.Command;
import neverlose.command.helpers.TabCompleteHelper;
import neverlose.events.api.EventHandler;
import neverlose.events.api.EventManager;
import neverlose.events.impl.RotationUpdateEvent;
import neverlose.modules.impl.combat.aura.Angle;
import neverlose.modules.impl.combat.aura.MathAngle;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.stream.Stream;

import static neverlose.command.impl.HelpCommand.getLine;

public class NeiroCommand extends Command implements IMinecraft {

    public static boolean isRecording = false;
    public static String currentRecordingName = null;
    
    public static List<float[]> currentTrace = new ArrayList<>();
    public static Map<String, List<float[]>> loadedTraces = new HashMap<>();

    public static float lastYaw = 0;
    public static float lastPitch = 0;
    public static float lastHeadYaw = 0;
    public static float lastBodyYaw = 0;
    public static float lastTargetYaw = 0;
    public static float lastTargetPitch = 0;
    public static boolean hadTargetLastTick = false;
    public static int lastTargetId = -1;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File DIR = new File(mc.runDirectory, "neverlose/neiro");

    public NeiroCommand() {
        super("neiro", "Управление Neuro Aura", "neuro", "n");
        EventManager.register(this);
        if (!DIR.exists()) {
            DIR.mkdirs();
        }
    }

    @Override
    public void execute(String label, String[] args) {
        if (args.length == 0) {
            printHelp();
            return;
        }

        String action = args[0].toLowerCase();

        switch (action) {
            case "rec" -> {
                if (args.length < 2) {
                    logDirect("Использование: .neiro rec <name>", Formatting.RED);
                    return;
                }
                String name = args[1];
                currentRecordingName = name;
                currentTrace.clear();
                isRecording = true;
                
                if (mc.player != null) {
                    lastYaw = mc.player.getYaw();
                    lastPitch = mc.player.getPitch();
                    lastHeadYaw = mc.player.headYaw;
                    lastBodyYaw = mc.player.bodyYaw;
                }
                lastTargetYaw = 0;
                lastTargetPitch = 0;
                hadTargetLastTick = false;
                lastTargetId = -1;
                
                logDirect("Запись нейро-ауры '" + name + "' начата!", Formatting.GREEN);
            }
            case "stop" -> {
                if (!isRecording) {
                    logDirect("Запись не активна!", Formatting.RED);
                    return;
                }
                isRecording = false;
                saveTrace(currentRecordingName, currentTrace);
                logDirect("Запись '" + currentRecordingName + "' остановлена и сохранена (" + currentTrace.size() + " тиков).", Formatting.GREEN);
            }
            case "load" -> {
                if (args.length < 2) {
                    logDirect("Использование: .neiro load <name>", Formatting.RED);
                    return;
                }
                String name = args[1];
                loadTrace(name);
            }
            case "del" -> {
                if (args.length < 2) {
                    logDirect("Использование: .neiro del <name>", Formatting.RED);
                    return;
                }
                String name = args[1];
                File file = new File(DIR, name + ".json");
                if (file.exists()) {
                    file.delete();
                    loadedTraces.remove(name);
                    try {
                        neverlose.modules.impl.combat.Aura aura = neverlose.modules.impl.combat.Aura.getInstance();
                        if (aura != null) {
                            List<String> list = getAvailableProfiles();
                            aura.neuroProfile.value(list.toArray(new String[0]));
                            if (aura.neuroProfile.getSelected().equals(name)) {
                                aura.neuroProfile.selected(list.isEmpty() ? "Нет профилей" : list.get(0));
                            }
                        }
                    } catch (Exception ignored) {}
                    logDirect("Запись '" + name + "' успешно удалена!", Formatting.GREEN);
                } else {
                    logDirect("Запись '" + name + "' не найдена!", Formatting.RED);
                }
            }
            case "list" -> {
                if (!DIR.exists()) return;
                File[] files = DIR.listFiles((d, n) -> n.endsWith(".json"));
                if (files == null || files.length == 0) {
                    logDirect("Сохраненных записей нет.", Formatting.RED);
                    return;
                }
                logDirectRaw(Text.literal(getLine()));
                logDirect("§f§lСПИСОК СОХРАНЕНИЙ NEIRO");
                logDirectRaw(Text.literal(getLine()));
                for (File file : files) {
                    String name = file.getName().replace(".json", "");
                    logDirect("§7> §f" + name + (loadedTraces.containsKey(name) ? " §a(загружено)" : ""));
                }
                logDirectRaw(Text.literal(getLine()));
            }
            default -> printHelp();
        }
    }

    private void printHelp() {
        logDirectRaw(Text.literal(getLine()));
        logDirect("§f§lУПРАВЛЕНИЕ NEIRO AURA");
        logDirectRaw(Text.literal(getLine()));
        logDirect("§7> neiro rec <name> §8- §fНачать запись движений");
        logDirect("§7> neiro stop §8- §fОстановить и сохранить запись");
        logDirect("§7> neiro load <name> §8- §fЗагрузить нейро-запись");
        logDirect("§7> neiro del <name> §8- §fУдалить нейро-запись");
        logDirect("§7> neiro list §8- §fСписок сохраненных записей");
        logDirectRaw(Text.literal(getLine()));
    }

    public static void saveTrace(String name, List<float[]> trace) {
        if (!DIR.exists()) DIR.mkdirs();
        File file = new File(DIR, name + ".json");
        try (FileWriter writer = new FileWriter(file)) {
            GSON.toJson(trace, writer);
            loadedTraces.put(name, new ArrayList<>(trace));
            
            try {
                neverlose.modules.impl.combat.Aura aura = neverlose.modules.impl.combat.Aura.getInstance();
                if (aura != null) {
                    List<String> list = getAvailableProfiles();
                    aura.neuroProfile.value(list.toArray(new String[0]));
                    if (list.contains(name)) {
                        aura.neuroProfile.selected(name);
                    }
                }
            } catch (Exception ignored) {}
            
        } catch (IOException e) {
            System.err.println("Ошибка сохранения: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void loadTrace(String name) {
        File file = new File(DIR, name + ".json");
        if (!file.exists()) {
            logDirect("Запись '" + name + "' не найдена!", Formatting.RED);
            return;
        }
        try (FileReader reader = new FileReader(file)) {
            List<float[]> loaded = GSON.fromJson(reader, new TypeToken<List<float[]>>() {}.getType());
            if (loaded != null) {
                loadedTraces.put(name, loaded);
                logDirect("Запись '" + name + "' успешно загружена (" + loaded.size() + " тиков).", Formatting.GREEN);
            }
        } catch (IOException e) {
            logDirect("Ошибка загрузки: " + e.getMessage(), Formatting.RED);
            e.printStackTrace();
        }
    }

    @EventHandler
    public void onRotationUpdate(RotationUpdateEvent event) {
        if (event.getType() != neverlose.events.api.types.EventType.PRE) return;
        
        if (isRecording && mc.player != null) {
            float currentYaw = mc.player.getYaw();
            float currentPitch = mc.player.getPitch();
            float currentHeadYaw = mc.player.headYaw;
            float currentBodyYaw = mc.player.bodyYaw;
            
            float deltaYaw = MathHelper.wrapDegrees(currentYaw - lastYaw);
            float deltaPitch = currentPitch - lastPitch;
            float headDeltaYaw = MathHelper.wrapDegrees(currentHeadYaw - lastHeadYaw);
            float bodyDeltaYaw = MathHelper.wrapDegrees(currentBodyYaw - lastBodyYaw);
            float headBodyOffset = MathHelper.wrapDegrees(currentHeadYaw - currentBodyYaw);
            float cameraHeadOffset = MathHelper.wrapDegrees(currentYaw - currentHeadYaw);

            float targetDeltaYaw = 0;
            float targetDeltaPitch = 0;
            float aimOffsetYaw = 0;
            float aimOffsetPitch = 0;
            float targetDistance = 0;
            float hadTarget = 0;

            LivingEntity target = neverlose.modules.impl.combat.Aura.target;
            if (target == null && mc.targetedEntity instanceof LivingEntity living) {
                target = living;
            }

            if (target != null && target.isAlive()) {
                Angle targetAngle = MathAngle.calculateAngle(target.getBoundingBox().getCenter());
                if (hadTargetLastTick && lastTargetId == target.getId()) {
                    targetDeltaYaw = MathHelper.wrapDegrees(targetAngle.getYaw() - lastTargetYaw);
                    targetDeltaPitch = targetAngle.getPitch() - lastTargetPitch;
                }
                aimOffsetYaw = MathHelper.wrapDegrees(currentYaw - targetAngle.getYaw());
                aimOffsetPitch = currentPitch - targetAngle.getPitch();
                targetDistance = mc.player.distanceTo(target);
                lastTargetYaw = targetAngle.getYaw();
                lastTargetPitch = targetAngle.getPitch();
                lastTargetId = target.getId();
                hadTargetLastTick = true;
                hadTarget = 1;
            } else {
                hadTargetLastTick = false;
                lastTargetId = -1;
            }
            
            float humanDeltaYaw = deltaYaw - targetDeltaYaw;
            float humanDeltaPitch = deltaPitch - targetDeltaPitch;
            float attackIntent = (mc.options.attackKey.isPressed() || mc.player.handSwingProgress > 0.0f) ? 1.0f : 0.0f;

            currentTrace.add(new float[]{
                    humanDeltaYaw,
                    humanDeltaPitch,
                    deltaYaw,
                    deltaPitch,
                    aimOffsetYaw,
                    aimOffsetPitch,
                    targetDeltaYaw,
                    targetDeltaPitch,
                    attackIntent,
                    mc.player.forwardSpeed,
                    mc.player.sidewaysSpeed,
                    targetDistance,
                    hadTarget,
                    headDeltaYaw,
                    bodyDeltaYaw,
                    headBodyOffset,
                    cameraHeadOffset
            });
            
            lastYaw = currentYaw;
            lastPitch = currentPitch;
            lastHeadYaw = currentHeadYaw;
            lastBodyYaw = currentBodyYaw;
        }
    }

    @Override
    public Stream<String> tabComplete(String label, String[] args) {
        if (args.length == 1) {
            return new TabCompleteHelper()
                    .append("rec", "stop", "load", "del", "list")
                    .filterPrefix(args[0])
                    .stream();
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("load") || args[0].equalsIgnoreCase("del"))) {
            if (DIR.exists()) {
                File[] files = DIR.listFiles((d, n) -> n.endsWith(".json"));
                if (files != null) {
                    String[] names = Arrays.stream(files)
                            .map(f -> f.getName().replace(".json", ""))
                            .toArray(String[]::new);
                    return new TabCompleteHelper()
                            .append(names)
                            .filterPrefix(args[1])
                            .stream();
                }
            }
        }
        return Stream.empty();
    }

    public static List<String> getAvailableProfiles() {
        if (!DIR.exists()) return Collections.singletonList("Нет профилей");
        File[] files = DIR.listFiles((d, n) -> n.endsWith(".json"));
        if (files == null || files.length == 0) return Collections.singletonList("Нет профилей");
        
        List<String> names = new ArrayList<>();
        for (File f : files) {
            names.add(f.getName().replace(".json", ""));
        }
        return names;
    }

    public static List<float[]> getTraceSilently(String name) {
        if (loadedTraces.containsKey(name)) {
            return loadedTraces.get(name);
        }
        File file = new File(DIR, name + ".json");
        if (!file.exists()) return null;
        try (FileReader reader = new FileReader(file)) {
            List<float[]> loaded = GSON.fromJson(reader, new TypeToken<List<float[]>>() {}.getType());
            if (loaded != null) {
                loadedTraces.put(name, loaded);
                return loaded;
            }
        } catch (IOException ignored) {}
        return null;
    }
}
