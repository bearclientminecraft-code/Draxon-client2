package dev.draxon.module.demo;
import dev.draxon.module.Category;
import dev.draxon.module.Module;
import dev.draxon.module.ModuleManager;
import dev.draxon.setting.*;
/** GUI showcase only: toggles and settings, no gameplay behaviour. Safe to delete. */
public final class DemoModules {
    private DemoModules() {}
    private static final class Demo extends Module {
        Demo(String n, Category c, boolean rich) {
            super(n, c);
            bool("Notify", true); num("Strength", 5, 0, 10, 0.5); mode("Mode", "Normal", "Normal", "Smooth", "Fast");
            if (rich) { color("Tint", 0x79B9F5); str("Label", "", 16); add(new MultiSetting("Targets", new String[]{"Players", "Mobs", "Animals"}, "Players")); }
        }
    }
    public static void registerAll(ModuleManager mm) {
        String[][] names = {
            {"Aim Assistant","Aura Demo","Auto Swap","Hit Boxes"}, {"Air Stuck","Fast Break","Free Camera","Safe Walk"},
            {"Ambience","Animations","Full Bright","Interface"}, {"Anti AFK","Auto Eat","Auto Tool","Click Action"},
            {"Chat Helper","Collector","No Commands","No Interact"}};
        for (Category c : Category.values()) for (int i = 0; i < names[c.ordinal()].length; i++) mm.register(new Demo(names[c.ordinal()][i], c, i == 0));
    }
}
