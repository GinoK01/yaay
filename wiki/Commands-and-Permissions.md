# Commands and permissions

## Commands

### `/imgui`

Opens the image claim GUI for the player.

### `/imgui reload`

Reloads addon configuration files.

- Controlled by `command.reload-subcommand-enabled`.
- Requires reload permission.

## Permissions

Defined in addon `plugin.yml`:

- `yamipa.imgui.use` (default: true)
- `yamipa.imgui.reload` (default: op)

These can be customized in `config.yml`:

- `command.use-permission`
- `command.reload-permission`

## Access logic

When a player runs `/imgui`:

1. Addon checks `command.use-permission`.
2. Addon collects visible image list from Yamipa storage.
3. Addon applies visibility filters.
4. GUI opens with page 1.

When a sender runs `/imgui reload`:

1. Addon checks `command.reload-subcommand-enabled`.
2. Addon checks `command.reload-permission`.
3. Addon reloads all runtime config and locale data.

## Typical role mapping

- Regular users:
  - Grant `yamipa.imgui.use`.
- Staff/admin:
  - Grant `yamipa.imgui.reload`.

## Example permission setup (LuckPerms)

```text
/lp group default permission set yamipa.imgui.use true
/lp group admin permission set yamipa.imgui.reload true
```
