# YAAY Wiki

This wiki documents YAAY end to end.

The addon provides a secure, paginated GUI to claim Yamipa images as items, with per-path display control, hourly limits, and full locale support.

## What This Addon Does

- Adds the `/imgui` command.
- Shows players a paginated inventory GUI of visible images.
- Lets players claim image items with configurable item options.
- Applies anti-exploit inventory protections.
- Supports per-player hourly limits and claim cooldown.
- Supports multilingual chat and GUI text.
- Supports per-player language preference persistence in SQLite.
- Supports per-path name, lore, and item option overrides through `display.yml`.
- Keeps compatibility with Folia and Yamipa metadata.

## Main Features

- Additive design: no direct changes to Yamipa core classes.
- Runtime reload support (`/imgui reload` and optional auto-reload watcher).
- Visibility filtering (`public` and `private/<player>` patterns by default).
- Extension-free display placeholders (`{filename}`, `{basename}`, `{filepath}`).
- Language switch button in GUI with persistent player preference.

## Quick Start

1. Build the project with `build-all.bat` from repository root.
2. Copy addon jar from `extensions/yamipa-imgui-addon/target` to your server `plugins` folder.
3. Start server once to generate addon config files.
4. Edit `config.yml`, `gui.yml`, `limits.yml`, `display.yml`, and locale files as needed.
5. Run `/imgui reload` or restart.
6. Open menu with `/imgui`.

## Wiki Pages

- [Installation and setup](Installation-and-Setup.md)
- [Commands and permissions](Commands-and-Permissions.md)
- [Configuration reference](Configuration-Reference.md)
- [Language system](Language-System.md)
- [API integration](API-Integration.md)
- [Display overrides](Display-Overrides.md)
- [Security and limits](Security-and-Limits.md)
- [Troubleshooting](Troubleshooting.md)
- [Developer architecture](Developer-Architecture.md)

## Data and Runtime Paths

The addon stores data in:

- `plugins/YAAY/config.yml`
- `plugins/YAAY/gui.yml`
- `plugins/YAAY/limits.yml`
- `plugins/YAAY/display.yml`
- `plugins/YAAY/locales/*.yml`
- `plugins/YAAY/language-preferences.db`

## Related Project Files

- `extensions/yamipa-imgui-addon/src/main/resources/plugin.yml`
- `extensions/yamipa-imgui-addon/src/main/resources/config.yml`
- `extensions/yamipa-imgui-addon/src/main/resources/gui.yml`
- `extensions/yamipa-imgui-addon/src/main/resources/limits.yml`
- `extensions/yamipa-imgui-addon/src/main/resources/display.yml`
- `extensions/yamipa-imgui-addon/src/main/resources/locales/en.yml`
- `extensions/yamipa-imgui-addon/src/main/resources/locales/es.yml`
