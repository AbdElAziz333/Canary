<img src="src/main/resources/logo.png" width="128">

# Canary for Minecraft Forge

![Github License](https://img.shields.io/github/license/AbdElAziz333/Canary)
![Github Issues](https://img.shields.io/github/issues/AbdElAziz333/Canary)
![Github Versions](https://img.shields.io/github/v/tag/AbdElAziz333/Canary)
[![This is a Fork](https://img.shields.io/badge/This%20is%20port-Support%20the%20original!-darkmagenta)](https://github.com/CaffeineMC/lithium-fabric)

Canary is a general-purpose optimization mod and unofficial fork of the Fabric mod [Lithium](https://github.com/CaffeineMC/lithium-fabric) for Minecraft Forge, This mod aims to optimize the general performance of Minecraft(mob ai, physics, chunk loading, etc..) without changing any behavior. It works on both the **client and server**, and **doesn't require the mod to be installed on both sides**.

# Installation

### Manual Installation

![Curseforge Versions](https://cf.way2muchnoise.eu/versions/canary.svg)

Canary supports Minecraft Forge 1.18.2 and up, So make sure you have the latest version of Forge present and simply drop the mod into your mods folder, no other mods or additional setup is required!

### Curseforge

![Curseforge Downloads](https://cf.way2muchnoise.eu/full_665658_downloads.svg)

If you are using Curseforge, you can continue downloading Canary through my [Curseforge page](https://www.curseforge.com/minecraft/mc-mods/canary).

### Modrinth

![Modrinth Downloads](https://img.shields.io/badge/dynamic/json?color=5da545&label=modrinth&prefix=downloads%20&query=downloads&url=https://api.modrinth.com/api/v1/mod/qa2H4BS9&style=flat&logo=data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCAxMSAxMSIgd2lkdGg9IjE0LjY2NyIgaGVpZ2h0PSIxNC42NjciICB4bWxuczp2PSJodHRwczovL3ZlY3RhLmlvL25hbm8iPjxkZWZzPjxjbGlwUGF0aCBpZD0iQSI+PHBhdGggZD0iTTAgMGgxMXYxMUgweiIvPjwvY2xpcFBhdGg+PC9kZWZzPjxnIGNsaXAtcGF0aD0idXJsKCNBKSI+PHBhdGggZD0iTTEuMzA5IDcuODU3YTQuNjQgNC42NCAwIDAgMS0uNDYxLTEuMDYzSDBDLjU5MSA5LjIwNiAyLjc5NiAxMSA1LjQyMiAxMWMxLjk4MSAwIDMuNzIyLTEuMDIgNC43MTEtMi41NTZoMGwtLjc1LS4zNDVjLS44NTQgMS4yNjEtMi4zMSAyLjA5Mi0zLjk2MSAyLjA5MmE0Ljc4IDQuNzggMCAwIDEtMy4wMDUtMS4wNTVsMS44MDktMS40NzQuOTg0Ljg0NyAxLjkwNS0xLjAwM0w4LjE3NCA1LjgybC0uMzg0LS43ODYtMS4xMTYuNjM1LS41MTYuNjk0LS42MjYuMjM2LS44NzMtLjM4N2gwbC0uMjEzLS45MS4zNTUtLjU2Ljc4Ny0uMzcuODQ1LS45NTktLjcwMi0uNTEtMS44NzQuNzEzLTEuMzYyIDEuNjUxLjY0NSAxLjA5OC0xLjgzMSAxLjQ5MnptOS42MTQtMS40NEE1LjQ0IDUuNDQgMCAwIDAgMTEgNS41QzExIDIuNDY0IDguNTAxIDAgNS40MjIgMCAyLjc5NiAwIC41OTEgMS43OTQgMCA0LjIwNmguODQ4QzEuNDE5IDIuMjQ1IDMuMjUyLjgwOSA1LjQyMi44MDljMi42MjYgMCA0Ljc1OCAyLjEwMiA0Ljc1OCA0LjY5MSAwIC4xOS0uMDEyLjM3Ni0uMDM0LjU2bC43NzcuMzU3aDB6IiBmaWxsLXJ1bGU9ImV2ZW5vZGQiIGZpbGw9IiM1ZGE0MjYiLz48L2c+PC9zdmc+)

If you are using Modrinth, you can continue downloading Canary through my [Modrinth page](https://modrinth.com/mod/canary).

### Github Actions

Github Actions builds will be available soon.

#

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

Much like Lithium, Canary is licensed under GNU LGPLv3, a free and open-source license. For more information, please see the
[license file](LICENSE.txt).

### Issues and Feature Requests
If you'd like to get help with the mod, feel free to open an issue here on GitHub, and if you want to propose new features or otherwise contribute to the mod, i will gladly accept pull requests, as well!
