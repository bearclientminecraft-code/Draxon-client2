package dev.draxon.module.combat;

import dev.draxon.combat.*;
import dev.draxon.module.Category;
import dev.draxon.module.Module;
import dev.draxon.setting.BooleanSetting;
import dev.draxon.setting.ModeSetting;
import dev.draxon.setting.NumberSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Vec3d;

/** Eases the real view angles toward the selected target through the RotationManager. */
public class AimAssist extends Module {
    private final NumberSetting hSpeed = add(new NumberSetting("Horizontal Speed", 6, 1, 30, 0.5).unit("\u00B0/t"));
    private final NumberSetting vSpeed = add(new NumberSetting("Vertical Speed", 3, 1, 30, 0.5).unit("\u00B0/t"));
    private final NumberSetting maxFov = add(new NumberSetting("Maximum FOV", 90, 1, 360, 1).unit("\u00B0"));
    private final NumberSetting maxRange = add(new NumberSetting("Maximum Range", 4.5, 1.0, 8.0, 0.1));
    private final NumberSetting strength = pct("Strength", 60, 5, 100);
    private final NumberSetting mousePriority = pct("Mouse Priority", 50, 0, 100);
    private final NumberSetting strafeBoost = pct("Strafe Increase", 25, 0, 100);
    private final BooleanSetting clickAim = bool("Click Aim", false);
    private final NumberSetting smoothing = pct("Aim Smoothing", 65, 0, 95);
    private final ModeSetting priority = mode("Target Priority", "Balanced", "Balanced", "Distance", "Angle", "Health");
    private final NumberSetting distPriority = pct("Distance Priority", 50, 0, 100);
    private final NumberSetting switchDelay = ms("Switch Delay", 250, 0, 1000);
    private final NumberSetting rotSpeed = pct("Rotation Speed", 100, 10, 200);
    private final BooleanSetting weaponCheck = bool("Weapon Check", true), visibility = bool("Visibility Check", true),
            noItems = bool("Disable While Using Items", true);

    private final TargetManager.Selector selector = new TargetManager.Selector();

    public AimAssist() {
        super("AimAssist", Category.COMBAT, "Sword");
        distPriority.visibleIf(() -> priority.get().equals("Balanced"));
    }
    @Override protected void onDisable() { selector.clear(); }
    @Override public void reset() { selector.clear(); }

    @Override public void onTick(MinecraftClient mc) {
        ClientPlayerEntity p = mc.player;
        if (p == null || mc.world == null || mc.currentScreen != null) return;
        if (weaponCheck.get() && !CombatState.weaponOk(p)) return;
        if (noItems.get() && p.isUsingItem()) return;
        if (clickAim.get() && !mc.options.attackKey.isPressed()) return;
        TargetManager.Req r = new TargetManager.Req(maxRange.get(), maxFov.get(), visibility.get(), true, true,
                TargetManager.Priority.of(priority.get()), distPriority.get() / 100.0);
        LivingEntity t = selector.select(mc, p, r, switchDelay.get().longValue());
        if (t == null) return;
        float k = (float) (rotSpeed.get() / 100.0);
        if (Keys.physical(mc, mc.options.leftKey) != Keys.physical(mc, mc.options.rightKey)) k *= 1f + (float) (strafeBoost.get() / 100.0);
        // Evaluated every frame from interpolated positions, so the aim glides and follows a moving target smoothly.
        RotationManager.request(10, (m, pl, pt) -> {
            if (!t.isAlive()) return null;
            Vec3d eye = pl.getCameraPosVec(pt), aim = t.getLerpedPos(pt).add(0, t.getHeight() * 0.7, 0);
            return new float[]{Geo.yawTo(eye, aim), Geo.pitchTo(eye, aim)};
        }, hSpeed.get().floatValue() * k, vSpeed.get().floatValue() * k, (float) (smoothing.get() / 100.0),
                (float) (strength.get() / 100.0), (float) (mousePriority.get() / 100.0));
    }
}
