package neverlose.command.impl;

import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import neverlose.IMinecraft;
import neverlose.command.Command;
import neverlose.command.helpers.TabCompleteHelper;
import neverlose.modules.impl.render.Arrows;
import neverlose.modules.module.ModuleStructure;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public class GpsCommand extends Command implements IMinecraft {

    public GpsCommand() {
        super("gps", "GPS arrow to coordinates");
    }

    @Override
    public void execute(String label, String[] args) {
        if (mc.player == null) {
            logDirect("You must be in game!", Formatting.RED);
            return;
        }

        String action = args.length > 0 ? args[0].toLowerCase() : "help";

        switch (action) {
            case "add", "set" -> {
                if (args.length < 2) {
                    logDirect("Usage: gps add <x> <z> or <x> <y> <z>", Formatting.RED);
                    return;
                }

                try {
                    BlockPos pos = parseTarget(args);

                    Arrows.setGpsTarget(pos);
                    Arrows arrows = Arrows.getInstance();
                    if (arrows != null && !arrows.isState()) {
                        ModuleStructure.runWithoutToggleFeedback(() -> arrows.setState(true));
                    }
                    logDirect(String.format("GPS set to %d %d %d", pos.getX(), pos.getY(), pos.getZ()), Formatting.GREEN);
                } catch (NumberFormatException exception) {
                    logDirect("Invalid coordinates!", Formatting.RED);
                }
            }
            case "clear", "remove", "del", "off" -> {
                Arrows.clearGpsTarget();
                logDirect("GPS cleared", Formatting.GREEN);
            }
            default -> {
                logDirect("Usage: gps add <x> <z> or <x> <y> <z>");
                logDirect("Clear: gps clear");
            }
        }
    }

    @Override
    public Stream<String> tabComplete(String label, String[] args) {
        if (args.length == 1) {
            return new TabCompleteHelper()
                    .append("add", "set", "clear")
                    .filterPrefix(args[0])
                    .stream();
        }
        return Stream.empty();
    }

    @Override
    public List<String> getLongDesc() {
        return List.of(
                "GPS arrow to coordinates",
                "",
                "Usage:",
                "> gps add <x> <z> - set GPS arrow on your current Y level",
                "> gps add <x> <y> <z> - set GPS arrow",
                "> gps clear - clear GPS arrow"
        );
    }

    private BlockPos parseTarget(String[] args) {
        String raw = String.join(" ", Arrays.copyOfRange(args, 1, args.length)).replace(',', ' ');
        String[] coordinates = raw.trim().split("\\s+");

        if (coordinates.length == 2) {
            return new BlockPos(
                    parseCoordinate(coordinates[0], mc.player.getBlockX()),
                    mc.player.getBlockY(),
                    parseCoordinate(coordinates[1], mc.player.getBlockZ())
            );
        }

        if (coordinates.length >= 3) {
            return new BlockPos(
                    parseCoordinate(coordinates[0], mc.player.getBlockX()),
                    parseCoordinate(coordinates[1], mc.player.getBlockY()),
                    parseCoordinate(coordinates[2], mc.player.getBlockZ())
            );
        }

        throw new NumberFormatException();
    }

    private int parseCoordinate(String input, int current) {
        if (input.startsWith("~")) {
            if (input.length() == 1) {
                return current;
            }
            return current + Integer.parseInt(input.substring(1));
        }
        return Integer.parseInt(input);
    }
}
