# Draxon Reborn V1 (base)

Client-side Fabric mod for Minecraft Java 1.21.11 (Java 21): glassmorphism ClickGUI, settings system, theme, config, event bus.

## Build
Versions in `gradle.properties` are taken from https://fabricmc.net/develop (Minecraft 1.21.11, Yarn, Loader, Fabric API, Loom).

- `./gradlew build` -> `build/libs/draxon-reborn-*.jar` (the remapped mod jar; the `-sources` jar is not the mod).
- `./gradlew runClient` to test in a dev client.
- `./gradlew genSources` to check mixin targets.

Gradle/Loom 1.19 need a **Java 25** JVM to run the build; the mod classes are still compiled for **Java 21** (`options.release = 21`),
which is what Minecraft 1.21.11 requires.

## GitHub Actions
`.github/workflows/build.yml` builds the jar on every push/PR (and on demand via *Run workflow*) and uploads
`draxon-reborn-jar` as an artifact.

## Controls
Right Shift opens/closes the ClickGUI, ESC closes it. Right click on a module row expands its settings.
Gear button next to the search bar opens the Interface (theme) panel.

## Layout
- `core` Draxon, DraxonClient, EventBus, Events, KeybindManager, ConfigManager
- `module` Module, ModuleManager, Category  (no modules shipped: add yours with `ModuleManager.register(...)`)
- `setting` Boolean/Number/Mode/Color/Keybind/String/Multi settings
- `gui` ClickGuiScreen, SettingRenderer, ThemeManager, FontManager, AnimationManager, Icons
- `util` RenderUtils

Config is stored in `config/draxon/` (`theme.json`, `modules.json`, optional `fonts.txt`).
Fonts: Instrument Sans (SIL OFL), see `assets/draxon/font/OFL-InstrumentSans.txt`.

## Combat / Sword (added)
Systems (`dev.draxon.combat`): `TargetManager`, `RotationManager`, `DelayManager`, `CombatState`, `Geo`, `Keys`.
Modules (`dev.draxon.module.combat`): Targets, TriggerBot, AimAssist, AutoClicker, W-Tap, S-Tap, SprintReset, Velocity, HitSelect, TargetStrafe, ShieldDisabler, AutoSprint.
Velocity needs the mixin `dev.draxon.mixin.ClientPlayNetworkHandlerMixin` (`draxon.mixins.json`); verify its target with `./gradlew genSources`.

Profiles (chat, never sent to the server): `.profile save|load|delete <name>`, `.profile list`, `.profile reset`, `.help`.
Files: `config/draxon/modules.json` (autosave) and `config/draxon/profiles/<name>.json`.

## Performance notes (GUI / HUD)
- Menu background blur is off by default (Interface -> "Background blur"); it is the most expensive part of the screen on weak GPUs.
- "Fast rendering" (Interface, on by default): switches, sliders, hover rows and other small shapes (radius <= 5) use hard-edged corners. Panel/pill corners stay anti-aliased with the same radius.
- The HUD is not drawn while the ClickGUI is open. ClickGUI: Up/Down (24px), PageUp/PageDown, Home/End scroll the module list under the cursor (or the last used one).
