# Display overrides

`display.yml` lets you override GUI name/lore and claimed item options per image path.

## Purpose

Use `display.yml` when you need path-specific behavior without changing global defaults in `config.yml`.

Typical use cases:

- Different names/lore for public banners.
- Custom item material and amount for selected paths.
- Path-specific width/height/flags.
- Per-language custom display text.

## Entry model

Each entry under `entries` can define:

- `path` (required)
- `name` (optional)
- `lore` (optional)
- `item` (optional)

Example:

```yaml
entries:
  - path: "public/logo.png"
    item:
      amount: 1
      width: 3
      height: 0
      auto-height: true
      flags: "+GLOW,-DROP"
      material: ITEM_FRAME
    name:
      default: "&bServer Logo"
      es: "&bLogo del servidor"
    lore:
      default:
        - "&7Main logo image"
        - "&7Size: &f{width}x{height}"
      es:
        - "&7Imagen principal del servidor"
        - "&7Tamano: &f{width}x{height}"
```

## Placeholders

The following placeholders are available in name/lore templates:

- `{filename}` or `{basename}`: image basename without extension.
- `{filepath}`: relative path without extension.
- `{amount}`: final item stack amount.
- `{width}` and `{height}`: final metadata dimensions.

## File extension behavior

The addon removes extension from all display placeholders used in GUI/chat/item text.

Example:

- Source file: `public/maps/spawn.png`
- `{filename}` => `spawn`
- `{filepath}` => `public/maps/spawn`

## Item override rules

Per-entry values override global defaults.

If a value is not set in entry, global value from `config.yml` is used.

`item.flags` accepts:

- Integer (`7`)
- Name list (`ANIM,REMO,DROP`)
- Relative operations (`+GLOW,-DROP`)

## Localization fallback for display entries

When localized `name` or `lore` is defined in entry:

1. Exact resolved language code.
2. Base language code (for locale variants).
3. Exact fallback language code.
4. Base fallback language code.
5. `default`.

If no localized entry is found, addon uses default GUI/item templates.

## Path matching notes

- `path` in entry is literal, not regex.
- It must match the exact relative image filename used by Yamipa storage.
- Path separator normalization is handled internally (`\` and `/`).

## Testing checklist

1. Add a single entry in `display.yml`.
2. Reload addon.
3. Open `/imgui` and check icon text.
4. Claim item and verify name/lore and metadata behavior.
5. Switch language and verify localized override fallback.
