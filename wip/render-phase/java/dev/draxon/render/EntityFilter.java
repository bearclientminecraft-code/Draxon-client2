package dev.draxon.render;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.player.PlayerEntity;
public final class EntityFilter {
    public static final int NONE = 0, PLAYER = 1, HOSTILE = 2, PASSIVE = 3, ANIMAL = 4;
    private EntityFilter() {}
    public static int category(Entity e) {
        if (!(e instanceof LivingEntity)) return NONE;
        if (e instanceof PlayerEntity) return PLAYER;
        if (e instanceof Monster) return HOSTILE;
        if (e instanceof AnimalEntity) return ANIMAL;
        if (e instanceof PassiveEntity) return PASSIVE;
        return NONE;
    }
}
