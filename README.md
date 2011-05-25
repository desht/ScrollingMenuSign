# ScrollingMenuSign

ScrollingMenuSign is a Bukkit plugin that allows you to attach a scrolling command menu to any sign on your craftbukkit
minecraft server.  A command menu includes 0 or more entries: a label, a command to execute, and an optional feedback message
to send to the player.  Players can scroll through the menu entries on a sign by right-clicking it, and execute the 
currently-selected entry by left-clicking it.

## Installation

Very simple: copy ScrollingMenuSign.jar into your plugins/ folder.  Restart/reload your server.  Done.

## Usage

### Creating a menu

#### Method 1

To create a new scrolling menu, create a blank sign, look at it, then type:

	/sms create <name> <title>
  
_name_ is a unique name to identify the menu.  _title_ is the menu title, and is always shown as the top line
of the scrolling menu sign.

When you do this, the menu becomes associated with the sign.  The sign's appearance will change; the title will
be shown as "-Title-" in blue on the first line, and the third line will look like ">   <" (this is the currently
selected line - it's blank because the menu has no entries yet).

Example:

	/sms create mymenu Time of Day

#### Method 2

Alternatively, you can create a sign with the following text:

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
will be independent of each other; any items added to one menu will not appear in the other (optional automatic
synchronisation is on the TODO list).

### Destroying a menu

To destroy a menu, there are three options:

- Look at a menu sign, and type "/sms break"

- Type "/sms break _name_" where _name_ is a unique sign identifier

- Take an axe to the sign!  Menus will be automatically removed if their associated sign is destroyed.

Destroying a sign which has a menu created by another player is only allowed with the appropriate permission
(see **Permissions Support** below).

Example:

	/sms break mymenu2
	
### Adding menu entries

To add a menu item to an existing menu, type "/sms add _name_ _item-specifier_"

The syntax of _item-specifier_ is "_label_|_command_[|_message_]".  This is most easily explained by example:

	/sms add mymenu Day|/time day|It's daytime!
	/sms add mymenu Night|/time night|It's night time!
  
adds a menu entry to the menu called _mymenu_ with a label of "Day", and one with a label of "Night".  If 
"Day" is executed (left-clicked), it will issue "/time day" as a command, and send a message to the player
of "It's daytime!".  Similarly for "Night".

You can leave out the message if you want, e.g.:

	/sms add mymenu Compass|/compass

If the command doesn't start with a slash (/), then the player will speak the command string instead, e.g.:

	/sms add mymenu Say hello|Hello everyone!

### Removing menu entries

To remove a menu item from an existing menu, type "/sms remove _name_ _index_".  Example:

	/sms remove mymenu 1
  
removes the first item in the menu.  To find which index corresponds to which item, you can use "/sms show",
see below.  Removing items by label is on the TODO list.

### Listing all menus

To show all menus that have been defined, use "/sms list".  Example:

	/sms list
	1 line (page 1/1)
	-------------------------
	mymenu @ -100,65,50 world "Time Control" [3]
    -------------------------
	
That's the menu named "mymenu" at location (-100,65,50) on world "world" with title "Time Control", and
has 3 entries.

### Show menu detail

To show information for a menu including all entries, use "/sms show _name_".  Example:

	/sms show mymenu
    3 lines (page 1/1)
	-------------------------
	 1) Day [/time day] "It's daytime!"
	 2) Night [/time night] "It's night time!"
	 3) Compass [/compass] ""
	-------------------------

### Output paging

The output of "/sms list" and "/sms show" could be too large to fit on screen.  In this case, further pages of
output from the last _list_ or _show_ command can be shown with "/sms page _page-num_".  Example:

	/sms page 2

### Forcing menu data to be saved

To force menu data to be written to disk immediately:

	/sms save
	
You will not normally need to do this, since it's automatically done if the server is stopped or if the plugin
is reloaded.

## Permissions support

ScrollingMenuSign supports the Permissions plugin, but if Permissions is not present, only server ops are allowed 
to manipulate menus.  Non-ops are, however, allowed to scroll and execute menus.

If Permissions is present, the following command nodes are understood, each of which correspond directly
to _/sms **command**_:

- scrollingmenusign.commands.create
- scrollingmenusign.commands.break
- scrollingmenusign.commands.add
- scrollingmenusign.commands.remove
- scrollingmenusign.commands.list
- scrollingmenusign.commands.show
- scrollingmenusign.commands.save

In addition, the following nodes are understood:

- scrollingmenusign.scroll: allow signs to be scrolled by right-clicking
- scrollingmenusign.execute: allow sign commands to be executed by left-clicking
- scrollingmenusign.destroy: allow destruction of signs with a menu that is owned by another player

## Known bugs/limitations

- Indirectly breaking (e.g breaking the block it's attached to) a sign with a menu won't give the player any feedback that the menu is destroyed, but the menu will be deleted.
- Some of the commands (e.g. list, show, save) should work on the console but they don't, yet.
- Menu entry labels, commands, feedback messages cannot contain a '|' character.  Need a more elegant way of specifying menu entries.

## TODO/planned features

- Allow commands to be executed with elevated permissions?
- Allow menu entries to be deleted by label in addition to numeric index.
- Configuration file?  Not sure at this point what should be configurable...
- Fully synchronised menus (update menu A, changes also appear on menu B)
- Some kind of order/sorting control on menu items.

## Changelog

#### ScrollingMenuSign 0.1 (23/5/2011)
- Initial release
