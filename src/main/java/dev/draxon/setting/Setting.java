package dev.draxon.setting;
public abstract class Setting<T> {
    public final String name; protected T value;
    protected Setting(String name, T def) { this.name = name; this.value = def; }
    public T get() { return value; }
    public void set(T v) { value = v; }
    private java.util.function.BooleanSupplier visible = () -> true;
    /** Hides the setting in the GUI while the condition is false (it is still saved/loaded). */
    public Setting<T> visibleIf(java.util.function.BooleanSupplier v) { visible = v; return this; }
    public boolean isVisible() { return visible.getAsBoolean(); }
    public abstract String serialize();
    public abstract void deserialize(String s);
}
