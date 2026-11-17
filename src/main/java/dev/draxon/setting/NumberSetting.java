package dev.draxon.setting;
public class NumberSetting extends Setting<Double> {
    public final double min, max, step;
    /** Suffix shown after the value in the GUI (e.g. " ms", "%"). Not persisted. */
    public String unit = "";
    public NumberSetting(String n, double d, double min, double max, double step) { super(n, d); this.min = min; this.max = max; this.step = step; }
    public NumberSetting unit(String u) { unit = u; return this; }
    @Override public void set(Double v) { double c = Math.max(min, Math.min(max, v)); value = Math.round(c / step) * step; }
    public void setProgress(double p) { set(min + (max - min) * Math.max(0, Math.min(1, p))); }
    public double progress() { return (value - min) / (max - min); }
    @Override public String serialize() { return String.valueOf(value); }
    @Override public void deserialize(String s) { set(Double.parseDouble(s)); }
}
