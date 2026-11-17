package dev.draxon.core;

import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

/** Chat commands (never sent to the server): .profile save|load|delete|list|reset [name], .help */
public final class Commands {
    private Commands() {}
    public static void init() {
        ClientSendMessageEvents.ALLOW_CHAT.register(msg -> {
            if (!msg.startsWith(".profile") && !msg.equals(".help")) return true;
            String[] a = msg.trim().split("\\s+");
            if (a[0].equals(".help")) { say("Draxon: .profile save <name> | load <name> | delete <name> | list | reset"); return false; }
            String sub = a.length > 1 ? a[1].toLowerCase() : "", name = a.length > 2 ? a[2] : "";
            switch (sub) {
                case "save" -> say(ProfileManager.save(name, DraxonClient.modules) ? "Profile saved: " + name : "Invalid name (A-Z a-z 0-9 _ -, max 32)");
                case "load" -> say(ProfileManager.load(name, DraxonClient.modules) ? "Profile loaded: " + name : "Profile not found or invalid: " + name);
                case "delete" -> say(ProfileManager.delete(name) ? "Profile deleted: " + name : "Profile not found: " + name);
                case "list" -> say("Profiles: " + (ProfileManager.list().isEmpty() ? "(none)" : String.join(", ", ProfileManager.list())));
                case "reset" -> { ProfileManager.resetToDefaults(DraxonClient.modules); say("All settings reset to defaults"); }
                default -> say("Usage: .profile save|load|delete <name> | list | reset");
            }
            return false;
        });
    }
    private static void say(String s) { MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(Text.literal("[Draxon] " + s)); }
}
