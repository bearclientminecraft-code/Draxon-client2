package dev.draxon.module.render;
import dev.draxon.module.Category;
import dev.draxon.module.Module;
import dev.draxon.setting.*;
public class ParticlesModule extends Module {
    public final ModeSetting type = mode("Particle Type", "Mixed", "Mixed", "Glow", "Dust", "Soft Light", "Sparkle", "Circle", "Floating");
    public final ModeSetting colorMode = mode("Color Mode", "Theme", "Theme", "Custom", "Rainbow");
    public final NumberSetting count = num("Particle Count", 120, 10, 400, 10), density = num("Spawn Density", 1.0, 0.1, 3, 0.1), speed = num("Particle Speed", 1.0, 0.1, 4, 0.1),
        size = num("Particle Size", 2.0, 0.5, 8, 0.5), opacity = num("Opacity", 0.7, 0.1, 1, 0.05), lifetime = num("Lifetime (s)", 8, 2, 30, 1), distance = num("Rendering Distance", 16, 6, 40, 2);
    public final ColorSetting color = color("Custom Color", 0x79B9F5);
    public final BooleanSetting glow = bool("Glow Effect", true), aroundCamera = bool("Move Around Camera", false), performance = bool("Performance Mode", false);
    public ParticlesModule() { super("Particles", Category.RENDER); }
}
