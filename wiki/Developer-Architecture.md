# Developer architecture

This page explains how the addon is structured internally.

## Design principles

- Additive extension model over Yamipa core.
- Runtime configuration-driven behavior.
- Thread-safe claim control and per-player session state.
- Compatibility with Folia scheduler model through Yamipa scheduler integration.

## Package map

- `command`: `/imgui` command handling.
- `config`: config loading and settings model.
- `api`: public service contract and language change event for third-party plugins.
- `display`: per-path display override parsing and resolution.
- `gui`: menu session, rendering, click routing, claim flow.
- `i18n`: locale loading, language resolution, player language persistence.
- `limits`: hourly quota accounting.
- `security`: inventory protection and dropped-item metadata normalization.
- `util`: text placeholders, inventory checks, item metadata creation.
- `watcher`: YAML file watcher integration for auto-reload.

## Core runtime flow

### Startup

1. Plugin creates config manager and ensures default resources.
2. Plugin initializes locale service and display metadata service.
3. Plugin initializes GUI service and limit service.
4. Plugin loads settings and applies them.
5. Plugin registers command and listeners.
6. Plugin registers `ImguiLanguageService` in Bukkit `ServicesManager`.
7. Plugin starts optional config watcher.

### `/imgui`

1. Command verifies sender and permission.
2. GUI service resolves visible filenames.
3. GUI service opens menu session page 1.
4. Player clicks route through inventory protection listener to GUI service handlers.
5. Claim flow validates path, limits, cooldown, lock, and inventory space.
6. Item is created with Yamipa metadata and localized display templates.

### Reload

1. Command or watcher triggers reload request.
2. Plugin reloads settings and locale data.
3. Display overrides are reparsed.
4. Open menus are either closed or refreshed based on config.

## Session and state model

### GUI session state

Per player session stores:

- Current page.
- Filename list snapshot.
- Slot to filename mapping.
- Session token for inventory holder identity safety.

### Caches

- Visible filename cache per player with TTL.
- Last claim timestamp per player.
- Claim lock set per player.

## Language subsystem

### Components

- `LocaleService`: locale key resolution with fallback.
- `LanguagePreferenceStore`: SQLite persistence for player language code.
- `ImguiLanguageService`: external set/get/resolve/clear language API.
- `PlayerImguiLanguageChangeEvent`: emitted when language changes via GUI or API.

### Language precedence

1. DB preference.
2. Player locale (optional via config).
3. Config fallback language.

## Display metadata subsystem

`DisplayMetadataService` parses `display.yml` into per-path override objects and resolves:

- Localized name/lore templates.
- Item option overrides.
- Fallback chain by language code.

## Security subsystem

### Inventory protection

`InventoryProtectionListener` blocks unauthorized inventory interaction patterns in addon GUI context.

### Legacy item rewrite

`DroppedImageItemMetadataListener` normalizes legacy Yamipa lore behavior by rewriting item display metadata on pickup when applicable.

## Data files

Server runtime data directory:

- `config.yml`
- `gui.yml`
- `limits.yml`
- `display.yml`
- `locales/*.yml`
- `language-preferences.db`

## Extension points for contributors

- Add new locale keys in `locales/*.yml` and consume through `LocaleService.trRaw` / `trRawList`.
- Add new navigation controls via `gui.yml` + `AddonSettings` + `GuiService` click routing.
- Add new display placeholders in `Texts` and rendering points.
- Add new anti-exploit checks in `InventoryProtectionListener`.
