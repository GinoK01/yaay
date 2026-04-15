# Language system

The addon supports global fallback language, optional player locale detection, per-player manual language selection from GUI, and persistent language preference storage.

## Resolution order

When resolving language for a player, the addon uses this order:

1. Player language saved in SQLite (`language-preferences.db`).
2. Player client locale (only when `language.use-player-locale: true`).
3. `language.fallback-language` from `config.yml`.
4. First available locale file if fallback is unavailable.

For non-player senders (console), fallback language is used.

## Why messages can appear in Spanish when fallback is English

If `language.use-player-locale` is true and player client locale is `es_*`, Spanish text will be selected unless player has a saved preference in DB.

If you want strict English by default:

```yaml
language:
  use-player-locale: false
  fallback-language: en
```

## GUI language switch

The language switch button is controlled in `gui.yml`:

```yaml
navigation:
  language:
    enabled: true
    slot: 48
    material: GLOBE_BANNER_PATTERN
```

Behavior:

- Clicking button cycles available locale files alphabetically.
- Selected language is saved per player in SQLite.
- GUI reopens immediately with new language.
- Session close handling is token-safe, so players can switch repeatedly without closing/reopening manually.

## Language switch cooldown

You can throttle GUI language switching per player in `config.yml`:

```yaml
language:
  change-cooldown-ms: 1500
```

- `0` disables cooldown.
- During cooldown, player receives `messages.language-change-cooldown`.

## SQLite persistence

Database path:

- `plugins/YAAY/language-preferences.db`

Table schema:

- `player_language_preferences`
  - `player_uuid` TEXT PRIMARY KEY
  - `language_code` TEXT NOT NULL
  - `updated_at` INTEGER NOT NULL

## Runtime requirements

The addon shades SQLite JDBC into the addon jar, so no external DB plugin is required.

If persistence cannot initialize, language switching still attempts to run but preference saving will not persist.

## Public API and sync event

The addon registers `ImguiLanguageService` in Bukkit `ServicesManager`.

It also emits `PlayerImguiLanguageChangeEvent` with source:

- `GUI_BUTTON`
- `API_CALL`

See `API-Integration.md` for usage examples.

## Locale file structure

Each language file (`locales/en.yml`, `locales/es.yml`, or custom) can include:

- `messages.*` for chat text.
- `gui.*` for GUI text.
- `item.*` for item name/lore templates.
- `languages.<code>` for language selector labels.

## Adding a new language

1. Create a new locale file, for example `locales/fr.yml`.
2. Copy all keys from `en.yml` and translate values.
3. Add `languages.fr` labels in locale files you care about.
4. Reload addon.
5. Use GUI language button to cycle to new language.

## Fallback behavior by key type

- Chat keys: fallback to fallback-language message key, then key literal.
- GUI and item templates: fallback to fallback-language key, then built-in template default.
- Display per-path localized values: fallback chain includes exact language, base language, fallback language, and `default` override key.
