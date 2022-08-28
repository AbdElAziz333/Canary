<img src="src/main/resources/icon.png" width="128">

# Canary for Minecraft Forge
Canary is a general-purpose optimization mod and unofficial fork of Lithium mod for Minecraft
free and open-source Minecraft mod which works to optimize many areas of the game in order to provide
better overall performance. It works on both the **client and server**, and **doesn't require the mod to be installed
on both sides**.

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

See [the Wiki page](https://github.com/CaffeineMC/lithium-fabric/wiki/Configuration-File) on the configuration file
format and all available options. The wiki may be outdated.
