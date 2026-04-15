# API integration

This page shows how other plugins can integrate with YAAY language state.

## Service contract

The addon registers this Bukkit service:

- `io.josemmo.bukkit.plugin.addon.imgui.api.ImguiLanguageService`

Main capabilities:

- Get available languages.
- Resolve effective language for sender or player UUID.
- Read stored preference.
- Set language with normalization/fallback.
- Clear stored preference.

## Resolve service from Bukkit

```java
RegisteredServiceProvider<ImguiLanguageService> provider =
    Bukkit.getServicesManager().getRegistration(ImguiLanguageService.class);
if (provider == null) {
    return;
}
ImguiLanguageService service = provider.getProvider();
```

## Typical usage patterns

### Read effective language

```java
String effective = service.resolveLanguage(player.getUniqueId());
```

### Set language from external plugin

```java
String applied = service.setPlayerLanguage(player.getUniqueId(), "es_MX");
// Returns the effective code after normalization/fallback, for example "es"
```

### Clear stored preference

```java
boolean cleared = service.clearPlayerLanguage(player.getUniqueId());
```

## Language normalization behavior

When a requested code is not directly available, addon resolves in this order:

1. Exact match in locale files.
2. Base language variant (for example `es` from `es_MX`).
3. Config fallback language.
4. First available language file.
5. `en` fallback if none exists.

## Language change event

The addon emits:

- `io.josemmo.bukkit.plugin.addon.imgui.api.event.PlayerImguiLanguageChangeEvent`

Event payload:

- `playerId`
- `previousLanguage`
- `newLanguage`
- `source` (`GUI_BUTTON` or `API_CALL`)

Example listener:

```java
@EventHandler
public void onImguiLanguageChange(PlayerImguiLanguageChangeEvent event) {
    UUID playerId = event.getPlayerId();
    String before = event.getPreviousLanguage();
    String after = event.getNewLanguage();
    LanguageChangeSource source = event.getSource();

    // Sync your plugin-local cache here
}
```

## Sync recommendations

1. Treat this addon as source of truth for language preference when both plugins affect player language.
2. Always use returned value from `setPlayerLanguage(...)` instead of requested code.
3. Avoid event feedback loops by checking source and previous/new values before writing back.
4. Handle provider-null case for startup order or missing addon scenarios.
