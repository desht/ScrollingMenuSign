# ScrollingMenuSign 

ScrollingMenuSign is a Bukkit plugin that allows you to attach a scrolling command menu to any sign on your CraftBukkit
Minecraft server.  A command menu consists of a title and zero or more entries: a label, a command to execute, and an optional
feedback message to send to the player.  By default, players can scroll through the menu entries on a sign by right-clicking it,
and execute the currently-selected entry by left-clicking it, but this is fully configurable.

## Installation

Very simple: copy ScrollingMenuSign.jar into your plugins/ folder.  Restart/reload your server.  Done.

## Building

If you want to build ScrollingMenuSign yourself, you will need Maven.

1) Download a copy of Vault.jar (1.1.1 minimum required) from http://dev.bukkit.org/server-mods/vault/

2) Run 'mvn install:install-file -DgroupId=net.milkbowl -DartifactId=vault -Dversion=1.1.1 -Dpackaging=jar -Dfile=Vault.jar' (adjust the version number to match what you downloaded)

3) Run 'mvn clean install'

This should give you a copy of ScrollingMenuSign.jar under the target/ directory.

Use 'mvn eclipse:eclipse' to create the .project and .classpath files if you want to open the project in Eclipse.

## Usage

Detailed documentation is available at bukkitdev: http://dev.bukkit.org/server-mods/scrollingmenusign/

## License

ScrollingMenuSign by Des Herriott is licensed under a [Creative Commons Attribution-NonCommercial 3.0 Unported License](http://creativecommons.org/licenses/by-nc/3.0/). 