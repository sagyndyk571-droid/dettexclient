package neverlose.mixin;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.Team;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import neverlose.modules.impl.render.ChunkAnimator;
import org.joml.Matrix3x2fStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.*;
import java.util.regex.Pattern;

@Mixin(PlayerListHud.class)
public class PlayerListHudMixin {
    private static final Pattern NAME_PATTERN = Pattern.compile("^\\w{3,16}$");
    @Unique
    private long neverlose$playerListOpenTime;
    @Unique
    private boolean neverlose$animatedPlayerListFrame;
    @Unique
    private boolean neverlose$wasPlayerListKeyDown;
    @Unique
    private long neverlose$lastPlayerListRenderTime;

    @Inject(method = "render", at = @At("HEAD"))
    private void neverlose$animatePlayerListHead(DrawContext context, int scaledWindowWidth, Scoreboard scoreboard,
                                                 ScoreboardObjective objective, CallbackInfo ci) {
        ChunkAnimator animation = ChunkAnimator.getInstance();
        MinecraftClient client = MinecraftClient.getInstance();
        boolean keyDown = client != null && client.options.playerListKey.isPressed();
        if (animation == null || !animation.shouldAnimatePlayerList() || !keyDown) {
            neverlose$animatedPlayerListFrame = false;
            neverlose$wasPlayerListKeyDown = keyDown;
            if (!keyDown) {
                neverlose$playerListOpenTime = 0L;
            }
            return;
        }

        long now = System.currentTimeMillis();
        boolean reopened = now - neverlose$lastPlayerListRenderTime > 120L;
        if (!neverlose$wasPlayerListKeyDown || neverlose$playerListOpenTime == 0L || reopened) {
            neverlose$playerListOpenTime = now;
        }
        neverlose$wasPlayerListKeyDown = true;
        neverlose$lastPlayerListRenderTime = now;

        float progress = animation.getScreenProgress(now - neverlose$playerListOpenTime);
        float scale = 0.94f + progress * 0.06f;
        float yOffset = (1.0f - progress) * -18.0f;

        Matrix3x2fStack matrices = context.getMatrices();
        matrices.pushMatrix();
        matrices.translate(scaledWindowWidth / 2.0f, 18.0f + yOffset);
        matrices.scale(scale, scale);
        matrices.translate(-scaledWindowWidth / 2.0f, -18.0f);
        neverlose$animatedPlayerListFrame = true;
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void neverlose$animatePlayerListTail(DrawContext context, int scaledWindowWidth, Scoreboard scoreboard,
                                                 ScoreboardObjective objective, CallbackInfo ci) {
        if (neverlose$animatedPlayerListFrame) {
            context.getMatrices().popMatrix();
            neverlose$animatedPlayerListFrame = false;
        }
    }

    @Inject(method = "collectPlayerEntries", at = @At("RETURN"), cancellable = true)
    private void addVanishedEntries(CallbackInfoReturnable<List<PlayerListEntry>> cir) {
        MinecraftClient client = MinecraftClient.getInstance();
        List<PlayerListEntry> originalList = cir.getReturnValue();
        List<PlayerListEntry> vanishedList = new ArrayList<>();

        Scoreboard scoreboard = client.world.getScoreboard();
        List<Team> teams = new ArrayList<>(scoreboard.getTeams());
        teams.sort(Comparator.comparing(Team::getName));

        Collection<PlayerListEntry> online = client.player.networkHandler.getPlayerList();

        for (Team team : teams) {
            Collection<String> members = team.getPlayerList();
            if (members.size() != 1) continue;
            String name = members.iterator().next();
            if (!NAME_PATTERN.matcher(name).matches()) continue;

            boolean present = online.stream().anyMatch(e -> e.getProfile() != null && name.equals(e.getProfile().name()));
            if (present) continue;

            MutableText displayName = Text.empty()
                    .append(Text.literal("[").formatted(Formatting.GRAY))
                    .append(Text.literal("V").formatted(Formatting.RED))
                    .append(Text.literal("] ").formatted(Formatting.GRAY))
                    .append(team.getPrefix())
                    .append(Text.literal(name).formatted(Formatting.GRAY));

            GameProfile fakeProfile = new GameProfile(UUID.randomUUID(), name);
            PlayerListEntry fake = new PlayerListEntry(fakeProfile, client.isInSingleplayer());
            fake.setDisplayName(displayName);
            fake.setListOrder(Integer.MIN_VALUE);
            vanishedList.add(fake);
        }

        List<PlayerListEntry> finalList = new ArrayList<>();
        finalList.addAll(vanishedList);
        finalList.addAll(originalList);

        cir.setReturnValue(finalList);
    }
}
