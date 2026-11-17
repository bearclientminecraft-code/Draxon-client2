package dev.draxon.core;
import dev.draxon.module.Module;
import net.minecraft.client.MinecraftClient;
public final class Events {
    private Events() {}
    /** END_CLIENT_TICK. */
    public record Tick(MinecraftClient client) {}
    /** START_CLIENT_TICK: runs before the game reads movement input for the tick. */
    public record PreTick(MinecraftClient client) {}
    /** World change / disconnect: every module must drop its transient state. */
    public record Reset() {}
    public record ModuleToggle(Module module, boolean enabled) {}
}
