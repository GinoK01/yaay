# Troubleshooting

## `/imgui` does not work

Checklist:

1. Confirm addon jar is present in server `plugins` folder.
2. Confirm Yamipa core plugin is installed and loaded.
3. Confirm startup logs include addon enable message.
4. Confirm player has `yamipa.imgui.use` permission.

## No images appear in GUI

Most common causes:

- Visibility regex filters do not match file paths.
- Files are not allowed by Yamipa path rules.

Default visibility expects:

- `public/...`
- `private/<player>/...`

If your structure differs, update regex in `config.yml`.

## English is configured but messages appear in Spanish

Check these settings:

```yaml
language:
  use-player-locale: false
  fallback-language: en
```

Also note:

- Player may already have a saved language in SQLite.
- Use GUI language button to switch back to English.
- Delete or edit `language-preferences.db` to reset stored preferences.

## Language button does nothing

Possible causes:

- `navigation.language.enabled` is false.
- Locale files are missing or malformed.
- Database init failed (check startup warnings).

## Reload does not apply changes

Checklist:

1. Verify `/imgui reload` permission.
2. Check YAML syntax for changed files.
3. If using auto-reload, verify debounce and watcher settings.
4. Full restart if dependency or jar-level change was made.

## Item lore or name appears inconsistent after pickup

The addon rewrites legacy dropped image item metadata on pickup when needed.

If inconsistency remains:

- Check `display.yml` entry for path.
- Check locale `item.*` templates.
- Check `item.clear-lore` behavior.

## Claims fail even with free inventory slots

- Verify target stack compatibility and available slot capacity.
- Check hourly limit and cooldown messages.
- Check anti-exploit lock behavior under high click rates.

## Build fails

From repository root:

```bat
build-all.bat
```

If failure occurs:

- Build core first and verify `target/YamipaPlugin-*.jar` exists.
- Ensure Maven and Java are installed.
- Re-run with verbose Maven command to inspect dependency resolution.

## Debug checklist for production server

- Save a copy of current config files.
- Reproduce issue with one player and one test image.
- Check server logs around command execution time.
- Verify permissions and locale preference DB row for affected player.
- Test with clean defaults and then reapply custom changes incrementally.
