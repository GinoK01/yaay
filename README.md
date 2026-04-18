# YAAY

Yes, Another Addon for Yamipa.

YAAY adds the /imgui menu flow to Yamipa with a paginated GUI, anti-exploit protections, hourly limits, language support, and per-path overrides.

<p align="center">
    <a href="imgur.com/a/W5IZJDc"><img src="https://i.imgur.com/fe19mi6.gif" autoplay loop muted playsinline></img></a>
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
- If the server has no internet access, pre-cache `sqlite-jdbc` or use an offline build that shades it back in

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
