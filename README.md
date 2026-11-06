# GeyserVanilla

GeyserVanilla is a development fork of [Geyser](https://github.com/GeyserMC/Geyser).
It replaces MCProtocolLib with the vanilla Minecraft client, including its networking, packet codecs, registries, and item components.
The launcher downloads the pinned Java 26.3 client and libraries into a local cache and checks their sizes and SHA-1 hashes.
The built Geyser jar does not contain Minecraft classes.
English locale files and default skins load from the cached client.
Shutdown cancels pending requests for the remote language index.

The standalone jar starts and stops successfully with the downloaded client.
The plugins for Velocity 4.3.1, BungeeCord build 2111, and ViaProxy 3.4.14 also pass startup and shutdown checks.
The Spigot plugin passes these checks on Paper 26.3 build 243.
Velocity tests cover injected status requests, styled chat conversion, console commands, and shutdown without stopping the proxy's network workers.
BungeeCord tests cover injected status requests, console commands, reload with an existing connection, and shutdown without stopping the proxy's network workers.
ViaProxy tests cover native status requests, console commands, host authentication profiles, proxy restart, and shutdown without stopping the proxy's network workers.
Paper tests cover injected status requests, commands, styled chat, integrated ping, world block IDs, biome names, decorated-pot components, Geyser reload, and independent plugin shutdown.
Automated tests cover native networking, codecs, registry isolation, sparse custom item IDs, custom block palettes, components, and core translation behavior.
Fabric 0.19.5 and NeoForge 26.3.0.1-beta dedicated servers also pass runtime isolation checks.
These tests cover injected status requests, offline Java login, configuration, chunks, inventory packets, world reads, commands, ping, reload, and independent shutdown.
Client LAN startup is wired to the isolated runtime but still needs a live test.
Full Bedrock-to-Java gameplay testing remains incomplete, and these development builds are not ready for deployment.

Build and run the standalone jar with Java 25 or newer.
The upstream information below describes Geyser's features and protocol targets, rather than a completed compatibility claim for this fork.

<img src="https://geysermc.org/img/geyser-1760-860.png" alt="Geyser" width="600"/>

[![License: MIT](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)
[![Discord](https://img.shields.io/discord/613163671870242838.svg?color=%237289da&label=discord)](https://discord.gg/geysermc)
[![Crowdin](https://badges.crowdin.net/e/51361b7f8a01644a238d0fe8f3bddc62/localized.svg)](https://translate.geysermc.org/)

Geyser is a bridge between Minecraft: Bedrock Edition and Minecraft: Java Edition, closing the gap from those wanting to play true cross-platform.

Geyser is an [Open Collaboration](https://opencollaboration.dev/) project.

## What is Geyser?
Geyser is a proxy, bridging the gap between Minecraft: Bedrock Edition and Minecraft: Java Edition servers.
The ultimate goal of this project is to allow Minecraft: Bedrock Edition users to join Minecraft: Java Edition servers as seamlessly as possible. However, due to the nature of Geyser translating packets over the network of two different games, *do not expect everything to work perfectly!*

Special thanks to the DragonProxy project for being a trailblazer in protocol translation and for all the team members who have joined us here!

## Supported Versions

| Edition | Supported Versions                                                                                |
|---------|---------------------------------------------------------------------------------------------------|
| Bedrock | 26.40, 26.41, 26.42, 26.43, 26.44, 26.45, 26.50, 26.51, 26.52, 26.60                              |
| Java    | 26.3 (For older versions, [see this guide](https://geysermc.org/wiki/geyser/supported-versions/)) |

## Setting Up
Take a look [here](https://geysermc.org/wiki/geyser/setup/) for how to set up Geyser.

## Links:
- Website: https://geysermc.org
- Docs: https://geysermc.org/wiki/geyser/
- Download: https://geysermc.org/download
- Discord: https://discord.gg/geysermc
- Donate: https://opencollective.com/geysermc
- Test Server: `test.geysermc.org` port `25565` for Java and `19132` for Bedrock

## What's Left to be Added/Fixed
- Near-perfect movement (to the point where anticheat on large servers is unlikely to ban you)
- Some Entity Flags

## What can't be fixed
There are a few things Geyser is unable to support due to various differences between Minecraft Bedrock and Java. For a list of these limitations, see the [Current Limitations](https://geysermc.org/wiki/geyser/current-limitations/) page.

## Compiling

Run these commands from the repository root with Java 25 or newer:

```sh
git submodule update --init --recursive
./gradlew :core:test :vanilla-protocol:test :vanilla-runtime:test :standalone:shadowJar
java -jar bootstrap/standalone/build/libs/Geyser-Standalone.jar --nogui
```

The build and the first launch need network access to download the pinned client and libraries.
Later launches use the cache after the launcher checks each artifact.
Standalone uses `cache/vanilla` as its runtime cache, relative to the launch directory.
Velocity uses `plugins/Geyser-Velocity/cache/vanilla`.
BungeeCord uses `plugins/Geyser-BungeeCord/cache/vanilla`.
ViaProxy uses `plugins/Geyser/cache/vanilla`.
Spigot/Paper uses `plugins/Geyser-Spigot/cache/vanilla`.
To change it, pass `-Dgeyser.vanilla.cache=/path/to/cache` before `-jar`.

## Contributing
Any contributions are appreciated. Please feel free to reach out to us on [Discord](https://discord.gg/geysermc) if
you're interested in helping out with Geyser.

## Libraries Used:
- [Adventure Text Library](https://github.com/KyoriPowered/adventure)
- [CloudburstMC Bedrock Protocol Library](https://github.com/CloudburstMC/Protocol)
- [Vanilla Minecraft client and libraries](vanilla-runtime/src/main/resources/vanilla-runtime.properties), downloaded at runtime
- [TerminalConsoleAppender](https://github.com/Minecrell/TerminalConsoleAppender)
- [Simple Logging Facade for Java (slf4j)](https://github.com/qos-ch/slf4j)
