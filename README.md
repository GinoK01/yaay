# YAAY

Yes, Another Addon for Yamipa.

YAAY adds the /imgui menu flow to Yamipa with a paginated GUI, anti-exploit protections, hourly limits, language support, and per-path overrides.

<p align="center">
    <a href="https://i.imgur.com/fCYMsAw.mp4">
        <video src="https://i.imgur.com/fCYMsAw.mp4" autoplay loop muted playsinline></video>
    </a>
</p>

## License

This project is released under the MIT License, same as Yamipa.

- Addon license: LICENSE
- Yamipa project: https://github.com/josemmo/yamipa

## Features

- /imgui and /imgui reload commands
- Paginated inventory GUI
- Visibility filtering by path patterns (public/private)
- Claim cooldown and hourly limits
- Player language selector with persistence
- Public language sync API for other plugins
- Per-file display and item overrides through display.yml

## Requirements

- Java 8+
- Paper/Folia compatible server
- Yamipa core plugin installed on the server

## Build inside this monorepo

From repository root:

```bat
build-all.bat
```

Build only the addon:

```bat
cd extensions\yamipa-imgui-addon
mvn -DskipTests package
```

## Publish as a standalone repository

If you extract only this folder into a new repository, update the core jar path in pom.xml.

Current setting:

```xml
<yamipa.core.jar>${project.basedir}/../../target/YamipaPlugin-${yamipa.core.version}.jar</yamipa.core.jar>
```

Typical standalone setting:

```xml
<yamipa.core.jar>${project.basedir}/libs/YamipaPlugin-${yamipa.core.version}.jar</yamipa.core.jar>
```

Standalone build flow:

1. Create a libs directory in the addon repository.
2. Place the Yamipa core jar there with the expected name.
3. Run mvn -DskipTests package.

## Runtime configuration files

- config.yml
- gui.yml
- limits.yml
- display.yml
- locales/en.yml
- locales/es.yml

## Commands and permissions

- /imgui
- /imgui reload
- yamipa.imgui.use
- yamipa.imgui.reload

## Documentation

Full docs are in the wiki directory.
