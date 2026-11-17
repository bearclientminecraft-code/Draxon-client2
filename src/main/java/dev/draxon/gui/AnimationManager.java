package dev.draxon.gui;
/** Frame-rate independent exponential smoothing. */
public class AnimationManager {
    public static class Anim {
        public float value, target;
        public Anim(float v) { value = target = v; }
        public void update(float dt, float speed) { value += (target - value) * (1f - (float) Math.exp(-speed * dt)); if (Math.abs(target - value) < 0.0005f) value = target; }
    }
}
