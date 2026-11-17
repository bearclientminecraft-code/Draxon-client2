# Draxon Reborn V1 (base)

Client-side Fabric mod for Minecraft Java 1.21.11 (Java 21): glassmorphism ClickGUI, settings system, theme, config, event bus.

## Build
Versions in `gradle.properties` are taken from https://fabricmc.net/develop (Minecraft 1.21.11, Yarn).

- `./gradlew build` -> `build/libs/draxon-reborn-*.jar` (the remapped mod jar; the `-sources` jar is not the mod).
- `./gradlew runClient` to test in a dev client.

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
- `module` Module, ModuleManager, Category; `module/demo` showcase modules (removable)
- `setting` Boolean/Number/Mode/Color/Keybind/String/Multi settings
- `gui` ClickGuiScreen, SettingRenderer, ThemeManager, FontManager, AnimationManager, Icons
- `util` RenderUtils
- `wip/render-phase` unfinished Render category sources, NOT compiled

Config is stored in `config/draxon/` (`theme.json`, `modules.json`, optional `fonts.txt`).
Fonts: Instrument Sans (SIL OFL), see `assets/draxon/font/OFL-InstrumentSans.txt`.
