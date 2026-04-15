# Installation and setup

## Requirements

- Java 8+ runtime on server.
- Folia or compatible Paper fork for API target.
- Yamipa core plugin installed and loaded.
- Addon jar built from this repository.

## Build

From repository root:

```bat
build-all.bat
```

This builds:

- Core Yamipa plugin.
- Imgui addon plugin.

Expected success output:

```text
[build-all] Core + addon build succeeded
```

## Deploy

1. Stop the server.
2. Copy addon jar from `extensions/yamipa-imgui-addon/target` into `plugins`.
3. Keep Yamipa core plugin jar in `plugins` as dependency.
4. Start server.

The addon descriptor is in `plugin.yml` with:

- `name: YAAY`
- `depend: [YamipaPlugin]`
- `folia-supported: true`

## First boot

On first boot, the addon writes default files under `plugins/YAAY`:

- `config.yml`
- `gui.yml`
- `limits.yml`
- `display.yml`
- `locales/en.yml`
- `locales/es.yml`

## Recommended first configuration

In `config.yml`:

- Keep `language.fallback-language: en` for English default.
- Keep `language.use-player-locale: false` if you want strict server language default.
- Set your own permissions if needed.
- Verify visibility patterns for your image folder strategy.

## Reload workflow

- Manual reload command:

```text
/imgui reload
```

- Optional file watcher reload:
  - Controlled by `reload.auto-reload-enabled`.
  - Debounced by `reload.auto-reload-debounce-ms`.

## Verifying installation

1. Join as player.
2. Run `/imgui`.
3. Confirm menu opens.
4. Confirm visible images list matches your visibility patterns.
5. Claim an item and verify metadata/appearance.

## Upgrade notes

When upgrading addon version:

1. Backup `plugins/YAAY` folder.
2. Replace jar.
3. Start server and review logs.
4. Compare your config files with latest resource defaults.
5. Test `/imgui` and language switch button.
