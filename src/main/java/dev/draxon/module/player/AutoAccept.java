package dev.draxon.module.player;

import dev.draxon.combat.DelayManager;
import dev.draxon.core.FriendManager;
import dev.draxon.hud.NotificationManager;
import dev.draxon.module.Category;
import dev.draxon.module.Module;
import dev.draxon.setting.BooleanSetting;
import dev.draxon.setting.StringSetting;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Accepts requests that servers announce in system messages (not in player chat, so players cannot trigger it by typing).
 * Each request type is its own toggle. "Only Friends" requires the sender to be on the friend list (.friends add <name>).
 * Message wording differs between servers: the built-in matchers cover common English/Russian Essentials-style texts,
 * Custom Request lets you match any text (all comma-separated keywords must appear) and choose the command ({player} = sender).
 */
public class AutoAccept extends Module {
    private final BooleanSetting onlyFriends = bool("Only Friends", true);
    private final BooleanSetting tpa = bool("Teleport Requests", true), party = bool("Party Invites", false), duel = bool("Duel Requests", false), custom = bool("Custom Request", false);
    private final StringSetting customKeys = add(new StringSetting("Custom Keywords", "", 64)), customCmd = add(new StringSetting("Custom Command", "", 48));
    private final DelayManager.Delay delay = delay("Accept Delay", 400, 3000, "Random Delay", 100, 1000);
    private final BooleanSetting notify = bool("Notify", true);

    private static final Pattern WORD = Pattern.compile("[A-Za-z0-9_]{3,16}");
    private static final String[] NEGATIVE = {"sent", "отправ", "expired", "истек", "истёк", "denied", "отклон", "cancel", "отмен"};
    private record Pending(String command, String label, long due) {}
    private final List<Pending> queue = new ArrayList<>();
    private static AutoAccept instance; private static boolean hooked;

    public AutoAccept() {
        super("AutoAccept", Category.PLAYER);
        instance = this;
        customKeys.visibleIf(custom::get); customCmd.visibleIf(custom::get);
        if (!hooked) { hooked = true; ClientReceiveMessageEvents.GAME.register((msg, overlay) -> { if (!overlay && instance != null && instance.isEnabled()) instance.onMessage(msg.getString()); }); }
    }
    @Override protected void onDisable() { queue.clear(); }
    @Override public void reset() { queue.clear(); }

    private static boolean has(String low, String... words) { for (String w : words) if (low.contains(w)) return true; return false; }

    /** First word of the message that is a friend or a player currently in the tab list. */
    private String sender(MinecraftClient mc, String msg) {
        ClientPlayNetworkHandler nh = mc.getNetworkHandler();
        Matcher m = WORD.matcher(msg);
        String found = null;
        while (m.find()) {
            String w = m.group();
            if (FriendManager.isFriend(w)) return w;
            if (found == null && nh != null && nh.getPlayerListEntry(w) != null) found = w;
        }
        return found;
    }

    private void onMessage(String msg) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || msg.isBlank()) return;
        String low = msg.toLowerCase(Locale.ROOT);
        if (has(low, NEGATIVE)) return;
        String who = sender(mc, msg);
        if (onlyFriends.get() && (who == null || !FriendManager.isFriend(who))) return;

        if (tpa.get() && has(low, "teleport", "tpa", "телепорт")) queue("tpaccept", "teleport request", who);
        else if (party.get() && has(low, "party", "пати") && has(low, "invit", "пригла") && who != null) queue("party accept " + who, "party invite", who);
        else if (duel.get() && has(low, "duel", "дуэл") && has(low, "request", "challeng", "invit", "вызов", "вызыв", "пригла") && who != null) queue("duel accept " + who, "duel request", who);
        else if (custom.get() && !customKeys.get().isBlank() && !customCmd.get().isBlank()) {
            for (String k : customKeys.get().toLowerCase(Locale.ROOT).split(",")) if (!k.isBlank() && !low.contains(k.trim())) return;
            String c = customCmd.get().trim(); if (c.startsWith("/")) c = c.substring(1);
            if (c.contains("{player}")) { if (who == null) return; c = c.replace("{player}", who); }
            queue(c, "request", who);
        }
    }
    private void queue(String cmd, String label, String who) {
        long now = System.currentTimeMillis();
        for (Pending p : queue) if (p.command().equals(cmd)) return;   // already waiting
        if (queue.size() >= 3) return;
        queue.add(new Pending(cmd, label + (who == null ? "" : " from " + who), now + delay.roll()));
    }

    @Override public void onTick(MinecraftClient mc) {
        if (queue.isEmpty() || mc.player == null || mc.getNetworkHandler() == null) return;
        long now = System.currentTimeMillis();
        for (int i = queue.size() - 1; i >= 0; i--) {
            Pending p = queue.get(i);
            if (now < p.due()) continue;
            mc.getNetworkHandler().sendChatCommand(p.command());
            if (notify.get()) NotificationManager.push("Auto Accept", "Accepted " + p.label(), NotificationManager.SUCCESS);
            queue.remove(i);
        }
    }
}
