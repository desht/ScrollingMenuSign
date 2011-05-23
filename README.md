# ScrollingMenuSign

ScrollingMenuSign is a Bukkit plugin that allows you to attach a scrolling command menu to any sign on your craftbukkit
minecraft server.  A command menu includes 0 or more entries: a label, a command to execute, and an optional feedback message
to send to the player.  Players can scroll through the menu entries on a sign by right-clicking it, and execute the 
currently-selected entry by left-clicking it.

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

#### Method 2

Alternatively, you can create a sign with the following text:

	LINE 1: scrollingmenu
	LINE 2: <name>
	LINE 3: <title>
	LINE 4: (BLANK)

When you left-click such a sign, a menu will be created and associated with the sign, if possible.

#### Method 3

Finally, you can create a new menu as a copy of an existing menu.  Look at a sign and type:

 	/sms create <name> from <other-name>

where _other-name_ is the unique identifier of a menu that already exists.

### Destroying a menu

To destroy a menu, there are three options:

- Look at a menu sign, and type "/sms break"

- Type "/sms break _name_" where _name_ is a unique sign identifier

- Take an axe to the sign!  Menus will be automatically removed if their associated sign is destroyed.

Destroying a sign which has a menu created by another player is only allowed with the appropriate permission
(see **Permissions Support** below).

### Adding menu entries

To add a menu item to an existing menu, type "/sms add _name_ _item-specifier_"

The syntax of _item-specifier_ is "_label_|_command_[|_message_]".  This is most easily explained by example:

	/sms add mymenu Day|/time day|It's daytime!
	/sms add mymenu Night|/time night|It's night time!
  
adds a menu entry to the menu called _mymenu_ with a label of "Day".  If executed (left-clicked), this will issue "/time set 0" as a command,
and send a message to the player of "It's daytime!".  Similarly for night-time.

You can leave out the message if you want, e.g.:

	/sms add mymenu Compass|/compass

If the command doesn't start with a slash (/), then the player will speak the command string instead, e.g.:

	/sms add mymenu Say hello|Hello everyone!

### Removing menu entries

To remove a menu item from an existing menu, type "/sms remove _name_ _index_".  E.g.:

	/sms remove mymenu 1
  
removes the first item in the menu.  To find which index corresponds to which item, you can use "/sms show",
see below.  Removing items by label is on the TODO list.

### Listing all menus

To show all menus that have been defined, use "/sms list", e.g.:

	/sms list
	mymenu @ -100,65,50 world "Time Control" [3]

That's the menu named "mymenu" at location (-100,65,50) on world "world" with title "Time Control", and
has 3 entries.

### Show menu detail

To show information for a menu including all entries, use "/sms show _name_", e.g.:

	/sms show mymenu
  	mymenu "Time Control" [3]:
	 1) Day [/time day] "It's daytime!"
	 2) Night [/time night] "It's night time!"
	 3) Compass [/compass] ""

### Forcing menu data to be saved

To force menu data to be written to disk immediately:

	/sms save
	
You will not normally need to do this, since it's automatically done if the server is stopped, or if the plugin
is reloaded.

## Permissions support

ScrollingMenuSign supports the Permissions plugin, but if Permissions is not present, only server ops are allowed 
to manipulate menus.  Non-ops are, however, allowed to scroll and execute menus.

If Permissions is present, the following command nodes are understood, each of which correspond to directly
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

- There is no paging yet if /sms list or /sms show has a lot of output.

## TODO/planned features

- Allow commands to be executed with elevated permissions?
- Allow menu entries to be deleted by label in addition to numeric index.

 