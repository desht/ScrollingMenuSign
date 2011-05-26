# ScrollingMenuSign v0.2

ScrollingMenuSign is a Bukkit plugin that allows you to attach a scrolling command menu to any sign on your CraftBukkit
Minecraft server.  A command menu consists of a title and zero or more entries: a label, a command to execute, and an optional
feedback message to send to the player.  By default, players can scroll through the menu entries on a sign by right-clicking it,
and execute the currently-selected entry by left-clicking it, but this is fully configurable.

## Installation

Very simple: copy ScrollingMenuSign.jar into your plugins/ folder.  Restart/reload your server.  Done.

## Usage

### Creating a menu

#### Method 1

To create a new scrolling menu, place a sign (contents don't matter, it can be blank), look at it, then type:

	/sms create <name> <title>
  
_name_ is a unique name to identify the menu.  _title_ is the menu title, and is always shown as the top line
of the scrolling menu sign.

When you do this, the menu becomes associated with the sign.  The sign's appearance will change; the title will
be shown as "Title" centred on the first line, and the third line will look like ">" (this is the currently
selected menu item - it's blank because the menu has no entries yet).

Example:

	/sms create mymenu Time of Day
	
Menu titles can be coloured - use the '&X' notation to specify a colour.  Example:

	/sms create mymenu &4Time of Day
	
gives the sign a red title.  See http://www.minecraftwiki.net/wiki/Classic_server_protocol#Color_Codes for
a list of the numeric (hex) codes (yes, that page is from Classic, but the codes are still valid).  Be aware that colour
codes use up characters from the sign's 15 characters-per-line limit.

#### Method 2

Alternatively, you can place a sign with the following text:

	LINE 1: scrollingmenu
	LINE 2: <name>
	LINE 3: <title>
	LINE 4: (BLANK)

When you punch (i.e. left-click with no held item) such a sign, a menu will be created and associated with the sign, if possible.

#### Method 3

Finally, you can create a new menu as a copy of an existing menu.  Look at a sign and type:

 	/sms create <name> from <other-name>

where _other-name_ is the unique identifier of a menu that already exists.

Example:

	/sms create mymenu2 from mymenu
	
_mymenu2_ will be an exact copy of _mymenu_, including the title.  However, after it's created, the two menus
will be independent of each other; any items added to one menu will not appear in the other (automatically
synchronised menus are on the TODO list).

### Destroying a menu

To destroy a menu, there are three options:

- Look at a menu sign, and type "/sms break"

- Type "/sms break _name_" where _name_ is a unique sign identifier

- Take an axe to the sign!  Menus will be automatically removed if their associated sign is destroyed.

Destroying a sign which has a menu created by another player is only allowed with the appropriate permission
(see **Permissions Support** below).

Example:

	/sms break mymenu2

### Changing a menu's title

To change a menu's title, type "/sms title _name_ _new title_"

Example:

	/sms title mymenu &2Time of Day

Colour codes are accepted, as in "/sms create".

### Adding menu entries

To add a menu item to an existing menu, type "/sms add _name_ _item-specifier_"

The syntax of _item-specifier_ is "_label_|_command_[|_message_]".  This is most easily explained by example:

	/sms add mymenu Day|/time day|It's daytime!
	/sms add mymenu Night|/time night|It's night time!
  
adds a menu entry to the menu called _mymenu_ with a label of "Day", and one with a label of "Night".  If 
"Day" is executed (left-clicked), it will issue "/time day" as a command, and send a message to the player
of "It's daytime!".  Similarly for "Night".

If you don't like the default field separator (|), you can change it to any other string like this:

	/sms setcfg menuitem_separator ::

Then you can do:

	/sms add mymenu Day::/time day::It's daytime!

You can also leave out the message if you want, e.g.:

	/sms add mymenu Compass|/compass

If the command doesn't start with a slash (/), then the player will speak the command string instead, e.g.:

	/sms add mymenu Say hello|Hello everyone!

Menu labels can be coloured, just like sign titles.  The same syntax applies.  Example:

	/sms add mymenu &1Compass|/compass

gives this entry a blue label.

### Removing menu entries

To remove a menu item from an existing menu, type "/sms remove _name_ _index_".  Example:

	/sms remove mymenu 1
  
removes the first item in the menu.  To find which index corresponds to which item, you can use "/sms show",
see below.  Removing items by label is on the TODO list.

### Interacting with a menu

These are the default bindings to interact with a menu (but they are fully configurable - see section **Config File** below):

- Left-click on a menu executes the currently-selected item
- Right-click on a menu scrolls the menu downwards
- Shift (sneak) + right-click on a menu scrolls the menu upwards
- Shift + mouse wheel up or down while looking at a menu scrolls it in the corresponding direction
 
### Listing all menus

To show all menus that have been defined, use "/sms list".  Example:

	/sms list
	1 line (page 1/1)
	-------------------------
	mymenu @ -100,65,50 world "Time of Day" [3]
    -------------------------
	
That's the menu named "mymenu" at location (-100,65,50) on world "world" with title "Time Control", and
has 3 entries.

### Show menu detail

To show information for a menu including all entries, use "/sms show _name_".  Example:

	/sms show mymenu
    4 lines (page 1/1)
	-------------------------
	Menu 'mymenu': title 'Time of Day'
	 1) Day [/time day] "It's daytime!"
	 2) Night [/time night] "It's night time!"
	 3) Compass [/compass] ""
	-------------------------

### Output paging

The output of "/sms list" and "/sms show" could be too large to fit on screen.  In this case, further pages of
output from the last _list_ or _show_ command can be shown with "/sms page _page-num_".  Example:

	/sms page 2

### Forcing menu data & configuration to be saved

To force menu and configuration data to be written to disk immediately:

	/sms save
	
You will not normally need to do this, since it's automatically done if the server is stopped or if the plugin
is reloaded.  Additionally, configuration data is saved whenever a change is made via "/sms setcfg".

The following data files (in plugins/ScrollingMenuSign) are used:

- config.yml - stores plugin configuration items
- scrollingmenus.yml - menu persistence data

### Forcing menu data & configuration to be reloaded

To force menu and configuration data to be reloaded:

	/sms reload
	
This may be useful if you choose to edit the config.yml or scrollingmenus.yml data files directly.

## Config File

ScrollingMenuSign uses a config file in the plugin data directory: plugins/ScrollingMenuSign/config.yml.
This is a YAML file and can be edited directly if you want.  If you do edit it while the server is running,
it's **strongly** recommended to reload your changes immediately with "/sms reload" - any server reload/restart
or "/sms setcfg" command will cause unreloaded changes to be overwritten.

If config.yml doesn't exist when the plugin starts up, a default one will be written (see below for list of defaults).

To view or change configuration settings from within Minecraft, you can use "/sms getcfg" 
and "/sms setcfg".

Example:

	/sms getcfg
	9 lines (page 1/1)
	-----------------
	sms.actions.leftclick.normal = execute
	sms.actions.leftclick.sneak = none
	....
	-----------------

To update a setting, use "/sms setcfg <key> <value>".  Example:

	/sms setcfg actions.leftclick.sneak execute
	
### Known configuration keys

- **actions.leftclick.normal** - action to take if a sign is left-clicked while not sneaking - default "execute"
- **actions.leftclick.sneak** - action to take if a sign is left-clicked while sneaking - default "none"
- **actions.rightclick.normal** - action to take if a sign is right-clicked while not sneaking - default "scrolldown"
- **actions.rightclick.sneak** - action to take if a sign is right-clicked while sneaking - default "scrollup"
- **actions.wheeldown.normal** - action to take if mouse wheel is rotated down while targetting a sign and not sneaking - default "none"
- **actions.wheeldown.sneak** - action to take if mouse wheel is rotated down while targetting a sign and sneaking - default "scrolldown"
- **actions.wheelup.normal** - action to take if mouse wheel is rotated up while targetting a sign and not sneaking - default "none"
- **actions.wheelup.sneak** - action to take if mouse wheel is rotated up while targetting a sign and sneaking - default "scrollup"
- **menuitem_separator** - the field separator string used in "/sms add" - default "|"

(To sneak, hold down Shift)

Valid actions for configuration keys under "actions.*" are:

- **none** - as name suggests, do nothing
- **execute** - execute the currently selected menu item
- **scrollup** - scroll the menu up
- **scrolldown** - scroll the menu down

## Permissions support

ScrollingMenuSign supports the Permissions plugin, but if Permissions is not present, only server ops are allowed 
to manipulate menus.  Non-ops are, however, allowed to scroll and execute menus.

If Permissions is present, the following command nodes are understood, each of which correspond directly
to _/sms **command**_:

- scrollingmenusign.commands.create
- scrollingmenusign.commands.break
- scrollingmenusign.commands.title
- scrollingmenusign.commands.add
- scrollingmenusign.commands.remove
- scrollingmenusign.commands.list
- scrollingmenusign.commands.show
- scrollingmenusign.commands.save
- scrollingmenusign.commands.reload
- scrollingmenusign.commands.getcfg
- scrollingmenusign.commands.setcfg

In addition, the following nodes are understood:

- scrollingmenusign.scroll: allow signs to be scrolled by right-clicking
- scrollingmenusign.execute: allow sign commands to be executed by left-clicking
- scrollingmenusign.destroy: allow destruction of signs with a menu that is owned by another player
- scrollingmenusign.coloursigns: allow usage of colour codes in titles and entry labels
- scrollingmenusign.colorsigns: synonym for scrollingmenusign.coloursigns

## Known bugs/limitations

- Indirectly breaking (e.g breaking the block it's attached to) a sign with a menu won't give the player any feedback that the menu is destroyed, but the menu will be deleted.
- Some of the commands (e.g. list, show, save) should work on the console but they don't, yet.

## TODO/planned features

- Allow commands to be executed with elevated permissions.  Permissions v3 may be the answer here...
- Allow menu entries to be deleted by label in addition to numeric index.
- Fully synchronised menus (update menu A, changes also appear on menu B)
- Some kind of order/sorting control on menu items.

## Changelog

#### ScrollingMenuSign v0.2 (26/5/2011)
- Added colour support for menu titles & labels.
- Titles are no longer blue by default - colour support means you can choose their colour yourself.  However, any previously saved menus will have lost their colour after upgrading - sorry!  You can re-colour them with the new "/sms title" command.
- Added config file (config.yml) and "/sms getcfg" / "/sms setcfg" commands
- Added "/sms title" command to change an existing menu's title 
- Menu item field separator for "/sms add" can now be set in configuration
- Menu actions (left-click, right-click, mouse wheel) are now configurable.

#### ScrollingMenuSign v0.1.1 (25/5/2011)
- Fixed NPE on sign creation in some circumstances
- Fixed problem where menus were destroyed if any block was placed/removed beside a sign (bad processing of block physics event)

#### ScrollingMenuSign 0.1 (23/5/2011)
- Initial release
