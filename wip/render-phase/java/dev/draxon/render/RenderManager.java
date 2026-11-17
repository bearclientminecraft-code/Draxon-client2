package dev.draxon.render;
import dev.draxon.core.DraxonClient;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
/** Single entry point: one HUD callback drives particles -> ESP -> nametags -> watermark -> notifications. */
public final class RenderManager {
    private static long last = System.nanoTime();
    private RenderManager() {}
    public static void init() {
        NotificationManager.init();
        HudRenderCallback.EVENT.register(RenderManager::onHud);
    }
    private static void onHud(DrawContext c, RenderTickCounter tc) {
        MinecraftClient mc = MinecraftClient.getInstance();
        long now = System.nanoTime(); float dt = Math.min((now - last) / 1e9f, 0.1f); last = now;
        if (mc.player == null || mc.world == null || mc.options.hudHidden || DraxonClient.modules == null) return;
        float td = tc.getTickProgress(false); // UNVERIFIED name (1.21.5+: getTickProgress)
        WorldRenderManager.render(c, mc, td, dt);
        HudManager.render(c, mc, dt);
    }
}
