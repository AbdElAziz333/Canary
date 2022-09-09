<img src="src/main/resources/logo.png" width="128">

# Canary for Minecraft Forge
![Github License](https://img.shields.io/github/license/AbdElAziz333/Canary)
![Github Issues](https://img.shields.io/github/issues/AbdElAziz333/Canary)
![Curseforge Downloads](https://cf.way2muchnoise.eu/665658.svg)
[![Architectury Forge Loom](https://img.shields.io/badge/Built%20With-Architectury%20Forge%20Loom-aqua)](https://github.com/architectury/architectury-loom)
[![This is a Fork](https://img.shields.io/badge/This%20is%20port-Support%20the%20original!-darkmagenta)](https://github.com/CaffeineMC/lithium-fabric)

Canary is a general-purpose optimization mod and unofficial fork of the Fabric mod [Lithium](https://github.com/CaffeineMC/lithium-fabric) for Minecraft, this mod aims to optimize many areas of the game in order to provide better overall performance. It works on both the **client and server**, and **doesn't require the mod to be installed on both sides**.

### Installation

https://www.curseforge.com/minecraft/mc-mods/canary

### What makes Canary different?

One of the most important design goals in Canary is *correctness*. Unlike other mods which apply optimizations to the
game, Canary does not sacrifice vanilla functionality or behavior in the name of raw speed. It's a no compromises
solution for those wanting to speed up their game, and as such, installing Canary should be completely transparent
to the player.

### Configuration

Out of the box, no additional configuration is necessary once the mod has been installed. Canary makes use of a
configuration override system which allows you to either forcefully disable problematic patches or enable incubating
patches which are otherwise disabled by default. As such, an empty config file simply means you'd like to use the
default configuration, which includes all stable optimizations by default.

See [the Wiki page](https://github.com/AbdElAziz333/Canary/wiki/Configuration-File) on the configuration file
format and all available options. The wiki may be outdated.

### Support the Original Creators

Canary is impossible without the high-quality contributions made by the original Lithium developers, and as such, i would like to ask you support them, for more information you can see [this section](https://github.com/CaffeineMC/lithium-fabric#support-the-developers) in the official repository's readme.

### License

Canary is licensed under GNU LGPLv3, a free and open-source license. For more information, please see the
[license file](LICENSE.txt).

### Issues and Feature Requests
If you'd like to get help with the mod, feel free to open an issue here on GitHub, and if you want to propose new features or otherwise contribute to the mod, we will gladly accept pull requests, as well!

### Building from sources
Support is not provided for setting up build environments or compiling the mod. We ask that users who are looking to get their hands dirty with the code have a basic understanding of compiling Java/Gradle projects. The basic overview is provided here for those familiar.

#### Requirements
JDK 17
You can either install this through a package manager such as Chocolatey on Windows or SDKMAN! on other platforms. If you'd prefer to not use a package manager, you can always grab the installers or packages directly from AdoptOpenJDK.
Gradle 7 or newer (optional)
The Gradle wrapper is provided in this repository can be used instead of installing a suitable version of Gradle yourself. However, if you are building many projects, you may prefer to install it yourself through a suitable package manager as to save disk space and to avoid many Gradle daemons sitting around in memory.
Building with Gradle
Canary uses a typical Gradle project structure and can be built by simply running the default build task. After Gradle finishes building the project, you can find the build artifacts (typical mod binaries, and their sources) in build/libs.

Tip: If this is a one-off build, and you would prefer the Gradle daemon does not stick around in memory afterwards, try adding the --no-daemon flag to ensure that the daemon is torn down after the build is complete. However, subsequent builds of the project will start more slowly if the Gradle daemon is not available to be re-used.

Build artifacts ending in api are for developers compiling against Lithium's API.
