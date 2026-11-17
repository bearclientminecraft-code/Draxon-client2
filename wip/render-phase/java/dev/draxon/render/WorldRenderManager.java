package dev.draxon.render;
import dev.draxon.core.DraxonClient;
import dev.draxon.module.render.EspModule;
import dev.draxon.module.render.NameTagsModule;
import dev.draxon.module.render.ParticlesModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
/** Screen-space "world" rendering: particles, ESP, nametags. Requires first-person (see ProjectionUtils). */
public final class WorldRenderManager {
    private static final int MAXE = 96;
    private static final LivingEntity[] ents = new LivingEntity[MAXE]; private static final float[] dists = new float[MAXE]; private static int n;
    private WorldRenderManager() {}
    public static void render(DrawContext c, MinecraftClient mc, float td, float dt) {
        EspModule esp = DraxonClient.modules.get(EspModule.class); NameTagsModule tags = DraxonClient.modules.get(NameTagsModule.class); ParticlesModule pm = DraxonClient.modules.get(ParticlesModule.class);
        if (!ProjectionUtils.update(mc, td)) return;
        if (pm.isEnabled()) { ParticleManager.update(dt, pm); ParticleManager.render(c, pm); } else ParticleManager.clear();
        boolean wantEsp = esp.isEnabled(), wantTags = tags.isEnabled(); if (!wantEsp && !wantTags) return;
        float range = Math.max(wantEsp ? esp.range.get().floatValue() : 0, wantTags ? tags.range.get().floatValue() : 0), r2 = range * range;
        n = 0;
        for (Entity e : mc.world.getEntities()) {
            if (e == mc.player || !(e instanceof LivingEntity le) || !le.isAlive() || le.isInvisible()) continue;
            float d2 = (float) mc.player.squaredDistanceTo(e); if (d2 > r2) continue;
            insert(le, d2);
        }
        // entries are sorted far -> near so near ones draw on top
        if (wantEsp) { float er = esp.range.get().floatValue(); for (int i = 0; i < n; i++) if (dists[i] <= er * er) EspRenderer.draw(c, mc, ents[i], (float) Math.sqrt(dists[i]), td, esp); }
        if (wantTags) { float tr = tags.range.get().floatValue(); for (int i = 0; i < n; i++) if (ents[i] instanceof PlayerEntity p && dists[i] <= tr * tr) NameTagRenderer.draw(c, mc, p, (float) Math.sqrt(dists[i]), td, tags); }
        for (int i = 0; i < n; i++) ents[i] = null;
    }
    /** Insertion into a fixed array sorted by squared distance descending; keeps the nearest MAXE. */
    private static void insert(LivingEntity e, float d2) {
        if (n == MAXE) { if (d2 >= dists[0]) return; System.arraycopy(ents, 1, ents, 0, MAXE - 1); System.arraycopy(dists, 1, dists, 0, MAXE - 1); n--; }
        int i = n; while (i > 0 && dists[i - 1] < d2) { ents[i] = ents[i - 1]; dists[i] = dists[i - 1]; i--; }
        ents[i] = e; dists[i] = d2; n++;
    }
}
