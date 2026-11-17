package dev.draxon.core;
import dev.draxon.module.Module;
import net.minecraft.client.MinecraftClient;
public final class Events {
    private Events() {}
    public record Tick(MinecraftClient client) {}
    public record ModuleToggle(Module module, boolean enabled) {}
}
