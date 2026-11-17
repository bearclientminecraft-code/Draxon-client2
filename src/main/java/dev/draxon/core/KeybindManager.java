package dev.draxon.core;
import dev.draxon.gui.ClickGuiScreen;
import dev.draxon.module.Module;
import dev.draxon.module.ModuleManager;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;
import java.util.List;
/** Single place that polls keys: GUI open key + per-module binds. Edge-detected; key state is tracked even while a screen is open. */
public final class KeybindManager {
    public static final int GUI_KEY = GLFW.GLFW_KEY_RIGHT_SHIFT;
    private final ModuleManager modules; private boolean[] state = new boolean[0]; private boolean guiDown;
    public KeybindManager(ModuleManager m) { modules = m; EventBus.INSTANCE.subscribe(Events.Tick.class, e -> tick(e.client())); }
    private void tick(MinecraftClient mc) {
        if (mc.getWindow() == null) return;
        long w = mc.getWindow().getHandle();
        boolean g = GLFW.glfwGetKey(w, GUI_KEY) == GLFW.GLFW_PRESS;
        if (g && !guiDown && mc.currentScreen == null && mc.world != null) mc.setScreen(new ClickGuiScreen());
        guiDown = g;
        List<Module> l = modules.all();
        if (state.length != l.size()) state = new boolean[l.size()];
        for (int i = 0; i < state.length; i++) {
            Module m = l.get(i); int k = m.bind.get();
            if (k < 0 || k == GUI_KEY) { state[i] = false; continue; }
            boolean d = GLFW.glfwGetKey(w, k) == GLFW.GLFW_PRESS;
            if (d && !state[i] && mc.currentScreen == null && mc.world != null) m.toggle();
            state[i] = d;
        }
    }
}
