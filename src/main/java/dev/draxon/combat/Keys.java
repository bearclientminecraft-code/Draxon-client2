package dev.draxon.combat;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

/** Helpers for modules that override movement/sprint keys for a few ticks. */
public final class Keys {
    private Keys() {}

    /** Real keyboard state of a binding (ignores overrides). Mouse-bound keys fall back to the binding state. */
    public static boolean physical(MinecraftClient mc, KeyBinding kb) {
        InputUtil.Key k = InputUtil.fromTranslationKey(kb.getBoundKeyTranslationKey());
        if (k.getCategory() != InputUtil.Type.KEYSYM || mc.getWindow() == null) return kb.isPressed();
        return GLFW.glfwGetKey(mc.getWindow().getHandle(), k.getCode()) == GLFW.GLFW_PRESS;
    }
    public static void restore(MinecraftClient mc, KeyBinding... kbs) { for (KeyBinding kb : kbs) kb.setPressed(physical(mc, kb)); }
    public static void restoreMovement(MinecraftClient mc) {
        restore(mc, mc.options.forwardKey, mc.options.backKey, mc.options.leftKey, mc.options.rightKey);
    }
    /** Queues one vanilla click for the binding (handled by the game on the next input pass). */
    public static void click(KeyBinding kb) { KeyBinding.onKeyPressed(InputUtil.fromTranslationKey(kb.getBoundKeyTranslationKey())); }
}
