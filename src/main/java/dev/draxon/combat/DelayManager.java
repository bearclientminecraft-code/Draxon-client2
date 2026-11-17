package dev.draxon.combat;

import dev.draxon.setting.NumberSetting;
import java.util.Random;

/** Single delay system: ActualDelay = BaseDelay + Random(-RandomDelay, +RandomDelay), never below 0. */
public final class DelayManager {
    private static final Random RNG = new Random();
    private DelayManager() {}

    public static long actual(double base, double random) {
        return Math.max(0, Math.round(base + (RNG.nextDouble() * 2 - 1) * random));
    }

    /** A Delay + Random Delay slider pair owned by one module. */
    public static final class Delay {
        public final NumberSetting base, random;
        public Delay(NumberSetting base, NumberSetting random) { this.base = base; this.random = random; }
        public long roll() { return actual(base.get(), random.get()); }
    }

    /** Wall-clock one-shot timer. */
    public static final class Timer {
        private long at;
        public void start(long ms) { at = System.currentTimeMillis() + ms; }
        public boolean passed() { return System.currentTimeMillis() >= at; }
        public void reset() { at = 0; }
    }
}
