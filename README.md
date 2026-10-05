# Arthou's Inventory View 👀

Arthou's Inventory View lets admins open and live-edit another player's inventory and ender chest straight from a GUI — no commands to memorize, just click and change whatever needs changing. Handy for support, moderation, or just double-checking what someone's carrying.

## Versions

| Loader | Minecraft | Mod version | JDK | Source |
|---|---|---|---|---|
| forge | 1.20.1 | 1.0.0 | 17 | [forge/1.20.1](forge/1.20.1) |
| neoforge | 1.21.1 | 1.0.0 | 21 | [neoforge/1.21.1](neoforge/1.21.1) |
| neoforge | 1.21.11 | 1.0.0 | 21 | [neoforge/1.21.11](neoforge/1.21.11) |

## Building from source

Install the JDK listed above for the version you want, then build from inside that folder:

```sh
cd neoforge/1.21.11
./gradlew build
```

On Windows, use `gradlew.bat build` instead. The finished jar lands in `build/libs/` for that version. The very first build will take a little longer, since Gradle needs to download itself and every dependency the project declares.

## About this repository

This repo keeps the source code and resources for every published version, each in its own folder. Local caches, test worlds, compiled output and backup copies are intentionally left out — only the real, published code lives here.

## License & credits

Every license, credit and notice file that shipped with each version has been kept as-is. Check the metadata for the version you're looking at before redistributing — publishing the source here doesn't change any declared license.
