package dev.draxon.module.combat;

import dev.draxon.module.Category;
import dev.draxon.module.Module;
import dev.draxon.setting.BooleanSetting;

/** Entity filter used by TargetManager for every combat module. Always on by default. */
public class TargetsModule extends Module {
    public final BooleanSetting players = bool("Players", true), mobs = bool("Hostile Mobs", false),
            animals = bool("Animals", false), invisible = bool("Invisible", true);
    public TargetsModule() { super("Targets", Category.COMBAT, "Sword"); setDefaultEnabled(true); }
}
