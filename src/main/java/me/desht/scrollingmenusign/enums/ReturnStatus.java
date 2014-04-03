package me.desht.scrollingmenusign.enums;

public enum ReturnStatus {
	// the command ran successfully
	CMD_OK,
	// the user lacks permission to run this costed or elevated command
	NO_PERMS,
	// ...not used...
	CMD_FAILED,
	// command execution restricted (@ restriction, cooldown, script returned false)
	RESTRICTED,
	// command cost was not affordable
	CANT_AFFORD,
	// a macro is calling itself
	WOULD_RECURSE,
	// unknown macro name
	BAD_MACRO,
	// command status can't be known (command had to be run via player.chat())
	UNKNOWN,
	// command referred to a nonexistent variable
	BAD_VARIABLE,
	// command has one or more substitution variables in it
	SUBSTITUTION_NEEDED,
	// the command was completely empty
	NO_COMMAND,
	// a cost wasn't applicable (e.g. durability cost on an item without durability)
	INAPPLICABLE
}
