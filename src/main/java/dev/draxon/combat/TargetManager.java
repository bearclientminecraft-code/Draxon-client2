package dev.draxon.combat;

import dev.draxon.core.DraxonClient;
import dev.draxon.module.combat.TargetsModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;

import java.util.ArrayList;
import java.util.List;

/** Target search, validation, priority sorting, visibility and sticky target switching. */
public final class TargetManager {
    private TargetManager() {}

    public enum Priority {
        BALANCED, DISTANCE, ANGLE, HEALTH;
        public static Priority of(String s) { for (Priority p : values()) if (p.name().equalsIgnoreCase(s)) return p; return BALANCED; }
    }
    /** @param distWeight 0..1, weight of distance vs angle in BALANCED mode. */
    public record Req(double range, double fov, boolean visibility, boolean validate, boolean ignoreTeam, Priority prio, double distWeight) {}

    public static boolean valid(ClientPlayerEntity p, Entity e, boolean validate, boolean ignoreTeam) {
        if (!(e instanceof LivingEntity le) || e == p || !le.isAlive()) return false;
        if (!validate) return true;
        if (le.isSpectator() || !le.isAttackable() || le instanceof ArmorStandEntity) return false;
        if (ignoreTeam && p.isTeammate(e)) return false;
        TargetsModule t = DraxonClient.modules == null ? null : DraxonClient.modules.get(TargetsModule.class);
        if (le.isInvisible() && t != null && !t.invisible.get()) return false;
        if (le instanceof PlayerEntity) return t == null || t.players.get();
        if (le instanceof Monster) return t != null && t.mobs.get();
        if (le instanceof AnimalEntity) return t != null && t.animals.get();
        return false;
    }

    public static boolean accepts(MinecraftClient mc, ClientPlayerEntity p, Entity e, Req r) {
        if (!valid(p, e, r.validate(), r.ignoreTeam())) return false;
        if (Geo.eyeDistance(p, e) > r.range()) return false;
        if (Geo.angleTo(p, e.getBoundingBox().getCenter()) > r.fov() / 2.0) return false;
        return !r.visibility() || Geo.visible(mc, p, e);
    }

    public static List<LivingEntity> candidates(MinecraftClient mc, ClientPlayerEntity p, Req r) {
        List<LivingEntity> out = new ArrayList<>();
        for (LivingEntity e : mc.world.getEntitiesByClass(LivingEntity.class, p.getBoundingBox().expand(r.range() + 1), x -> true))
            if (accepts(mc, p, e, r)) out.add(e);
        return out;
    }

    private static double score(ClientPlayerEntity p, LivingEntity e, Req r) {
        double d = Geo.eyeDistance(p, e), a = Geo.angleTo(p, e.getBoundingBox().getCenter());
        return switch (r.prio()) {
            case DISTANCE -> d;
            case ANGLE -> a;
            case HEALTH -> e.getHealth();
            case BALANCED -> d / Math.max(0.1, r.range()) * r.distWeight() + a / Math.max(1.0, r.fov() / 2.0) * (1 - r.distWeight());
        };
    }
    public static LivingEntity best(MinecraftClient mc, ClientPlayerEntity p, Req r) {
        LivingEntity best = null; double bs = Double.MAX_VALUE;
        for (LivingEntity e : candidates(mc, p, r)) { double s = score(p, e, r); if (s < bs) { bs = s; best = e; } }
        return best;
    }

    /** Per-module selector: keeps the current target while valid, switching to a better one at most every switchDelay ms. */
    public static final class Selector {
        private LivingEntity current; private long switchedAt;
        public LivingEntity select(MinecraftClient mc, ClientPlayerEntity p, Req r, long switchDelayMs) {
            LivingEntity best = best(mc, p, r);
            boolean keep = current != null && accepts(mc, p, current, r);
            if (keep && (best == current || System.currentTimeMillis() - switchedAt < switchDelayMs)) return current;
            if (best != current) { current = best; switchedAt = System.currentTimeMillis(); }
            return current;
        }
        public void clear() { current = null; }
    }
}
