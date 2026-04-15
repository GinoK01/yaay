# Configuration reference

This page documents every addon configuration file and key.

## File map

- `config.yml`: global behavior, language, command access, visibility, claim item defaults, reload and performance options.
- `gui.yml`: inventory menu layout and navigation items.
- `limits.yml`: anti-exploit and rate limits.
- `display.yml`: per-path display and item option overrides.
- `locales/*.yml`: localized chat, GUI, and item templates.

## config.yml

### Top-level

| Key | Type | Default | Description |
|---|---|---|---|
| enabled | boolean | true | Enables addon runtime behavior. |

### language

| Key | Type | Default | Description |
|---|---|---|---|
| language.use-player-locale | boolean | false | If true, player client locale is used when no per-player language preference exists in DB. |
| language.change-cooldown-ms | long | 0 | Per-player cooldown for GUI language switching. `0` disables cooldown. |
| language.fallback-language | string | en | Server fallback language code. |
| language.prefix | string | &8[&bImgui&8] &7 | Prefix for chat messages from locale `messages.*`. |

### command

| Key | Type | Default | Description |
|---|---|---|---|
| command.use-permission | string | yamipa.imgui.use | Permission required for `/imgui`. |
| command.reload-permission | string | yamipa.imgui.reload | Permission required for `/imgui reload`. |
| command.reload-subcommand-enabled | boolean | true | Enables or disables reload subcommand handling. |

### visibility

| Key | Type | Default | Description |
|---|---|---|---|
| visibility.require-public-or-own | boolean | true | If true, addon applies regex filters below to visible file list. |
| visibility.own-path-pattern | string | ^private/#player#/.+ | Regex for personal files. `#player#` and `#uuid#` tokens are supported. |
| visibility.public-path-pattern | string | ^public/.+ | Regex for public files. |

### item

| Key | Type | Default | Description |
|---|---|---|---|
| item.material | string | ITEM_FRAME | Base item material for claimed image items. |
| item.amount | int | 1 | Base amount. Clamped to 1..64. |
| item.width | int | 3 | Base image width for Yamipa metadata. |
| item.height | int | 0 | Base image height. If 0 and auto-height true, calculated proportionally. |
| item.auto-height | boolean | true | Enables proportional height when height <= 0. |
| item.flags | int | 7 | Default Yamipa image flags. |
| item.name-format | string | &b{filename} | Default item display name template. |
| item.clear-lore | boolean | true | Clears lore when no custom lore override exists. |
| item.lore | string list | [] | Default item lore template list. |

### performance

| Key | Type | Default | Description |
|---|---|---|---|
| performance.async-open | boolean | true | Reserved compatibility key from earlier iterations. |
| performance.cache-ttl-ms | long | 5000 | TTL for visible filename cache per player. |

### reload

| Key | Type | Default | Description |
|---|---|---|---|
| reload.close-open-menus | boolean | false | If true, closes open addon menus after reload. |
| reload.auto-reload-enabled | boolean | true | Enables file watcher auto reload. |
| reload.auto-reload-debounce-ms | long | 800 | Debounce time between auto reload executions. |

## gui.yml

### Layout

| Key | Type | Default | Description |
|---|---|---|---|
| title | string | &0Image Claim Menu | Inventory title (can also be overridden by locale `gui.title`). |
| rows | int | 6 | Inventory rows (1..6). |
| content-slots | int list | 0..44 | Slots used for image entries. |

### filler

| Key | Type | Default | Description |
|---|---|---|---|
| filler.enabled | boolean | true | Fills unused slots with filler item. |
| filler.material | string | GRAY_STAINED_GLASS_PANE | Filler material. |
| filler.name | string | " " | Filler display name. |
| filler.lore | string list | [] | Filler lore. |

### image-icon

| Key | Type | Default | Description |
|---|---|---|---|
| image-icon.material | string | PAPER | Material for image list icon. |
| image-icon.name-format | string | &f{filename} | Default icon name template. |
| image-icon.lore | string list | See default | Default icon lore template list. |

### navigation

For each navigation item (`previous`, `refresh`, `language`, `info`, `close`, `next`):

| Key pattern | Type | Description |
|---|---|---|
| navigation.<node>.slot | int | Slot position in inventory. |
| navigation.<node>.material | string | Item material. |
| navigation.<node>.name | string | Display name template. |
| navigation.<node>.lore | string list | Lore template list. |

Additional key for language node:

| Key | Type | Default | Description |
|---|---|---|---|
| navigation.language.enabled | boolean | true | Enables language switch button in GUI. |

### empty-state

| Key | Type | Default | Description |
|---|---|---|---|
| empty-state.slot | int | 22 | Slot to show empty-state icon. |
| empty-state.material | string | CHEST_MINECART | Empty-state material. |
| empty-state.name | string | &7No available images | Empty-state name. |
| empty-state.lore | string list | See default | Empty-state lore. |

## limits.yml

### hourly-limit

| Key | Type | Default | Description |
|---|---|---|---|
| hourly-limit.enabled | boolean | true | Enables hourly claim quota. |
| hourly-limit.max-items-per-hour | int | 64 | Maximum claimed item amount per player per hour. |

### claim-rate-limit

| Key | Type | Default | Description |
|---|---|---|---|
| claim-rate-limit.enabled | boolean | true | Enables per-player claim cooldown. |
| claim-rate-limit.cooldown-ms | long | 200 | Minimum delay between claims for a player. |

### anti-exploit

| Key | Type | Default | Description |
|---|---|---|---|
| anti-exploit.strict-inventory-lock | boolean | true | Cancels most inventory interactions while menu is open. |
| anti-exploit.block-shift-click | boolean | true | Blocks shift-click behavior. |
| anti-exploit.block-number-key | boolean | true | Blocks hotbar number key swap behavior. |
| anti-exploit.block-collect-to-cursor | boolean | true | Blocks collect-to-cursor behavior. |
| anti-exploit.block-drag | boolean | true | Blocks drag interactions touching menu slots. |
| anti-exploit.block-creative-actions | boolean | true | Blocks creative mode click behavior in menu context. |
| anti-exploit.claim-lock-enabled | boolean | true | Enables per-player claim lock to avoid duplicate races. |

## display.yml

### entries

`entries` is a list of per-path overrides.

Each entry can include:

- `path`: relative Yamipa image path (required).
- `name`: localized or plain string template.
- `lore`: localized or plain list template.
- `item` section with per-path item overrides.

### name format options

- Plain string:

```yaml
name: "&bServer Logo"
```

- Localized map:

```yaml
name:
  default: "&bServer Logo"
  es: "&bLogo del servidor"
```

### lore format options

- Plain list:

```yaml
lore:
  - "&7Main logo"
```

- Localized map of lists:

```yaml
lore:
  default:
    - "&7Main logo"
  es:
    - "&7Logo principal"
```

### item override keys

| Key | Type | Description |
|---|---|---|
| item.material | string | Override item material. |
| item.amount | int | Override amount (1..64). |
| item.width | int | Override width. |
| item.height | int | Override height. |
| item.auto-height | boolean | Override auto-height behavior. |
| item.flags | int or string/list | Override flags. Supports numeric, names, and +/- relative tokens. |

### flags syntax

Supported symbolic names:

- `ANIM`
- `REMO`
- `DROP`
- `GLOW`

Supported formats:

- Integer: `7`
- CSV names: `ANIM,REMO,DROP`
- Relative operations: `+GLOW,-DROP`

## locales/*.yml

### Required sections

- `messages.*` for chat messages.
- `gui.*` for GUI text templates.
- `item.*` for claim item templates.
- `languages.*` for language labels shown in selector.

### Locale fallback behavior

When a key is missing in current language:

1. Addon tries fallback language file (`language.fallback-language`).
2. If still missing, addon uses built-in/default template fallback from config path.

## Placeholder reference

### General filename placeholders

| Placeholder | Meaning |
|---|---|
| {filename} | Basename without extension. |
| {basename} | Alias of `{filename}`. |
| {filepath} | Relative path without extension. |

### Pagination placeholders

| Placeholder | Meaning |
|---|---|
| {current_page} | Current page number. |
| {page} | Alias of current page. |
| {max_page} | Max page number. |
| {count} | Total visible image count. |
| {target_page} | Target page for prev/next icon text. |

### Item placeholders

| Placeholder | Meaning |
|---|---|
| {amount} | Item stack amount. |
| {width} | Final width metadata value. |
| {height} | Final height metadata value. |

### Language button placeholders

| Placeholder | Meaning |
|---|---|
| {language} | Human-readable selected language label. |
| {language_code} | Selected language code (uppercase). |
| {languages} | Number of available language files. |
