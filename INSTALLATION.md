# Installation and build

## Requirements

- Paper or Purpur 1.21.x;
- Java 21;
- Gradle 8.10+ for local builds.

## Build locally

```bash
gradle clean build
```

The server plugin is created at:

```text
build/libs/VcustomCrafts-1.0.0.jar
```

The `-sources.jar` file is for developers and must not be placed on the server.

## Install

1. Stop the Minecraft server.
2. Put the compiled VcustomCrafts JAR in the server's `plugins` directory.
3. Start the server and wait for `VcustomCrafts Ready` in the console.
4. Edit `plugins/VcustomCrafts/config.yml` if needed.
5. Run `/vcc reload` after changing recipes or saved items.

Use a full server restart to install a staged plugin update. Do not use Bukkit `/reload` for plugin upgrades.

## Main commands

| Command | Purpose |
| --- | --- |
| `/recipes` | Open the recipe book |
| `/vcc editor <id> [shaped\|shapeless]` | Create a recipe in game |
| `/vcc item save <id>` | Capture the held item |
| `/vcc item give <player> <id>` | Give a captured item |
| `/vcc item list` | List captured item IDs |
| `/vcc reload` | Reload config, language, saved items and recipes |
| `/vcc update check` | Check the latest GitHub Release |
| `/vcc update download` | Verify and stage the latest release |

