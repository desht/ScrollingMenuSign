package me.desht.scrollingmenusign.expector;

import me.desht.scrollingmenusign.SMSException;
import me.desht.scrollingmenusign.enums.ReturnStatus;
import me.desht.scrollingmenusign.parser.CommandParser;
import me.desht.scrollingmenusign.parser.ParsedCommand;

import org.bukkit.entity.Player;

public class ExpectCommandSubstitution extends ExpectBase {
	private String command;
	private String sub;

	public ExpectCommandSubstitution(String command) {
		this.command = command;
	}

	public String getCommand() {
		return command;
	}

	public void setCommand(String command) {
		this.command = command;
	}

	public String getSub() {
		return sub;
	}

	public void setSub(String sub) {
		this.sub = sub;
	}

	@Override
	public void doResponse(Player p) throws SMSException {
		String newCommand = command.replaceAll("<\\$:.+?>", sub);
		ParsedCommand pCmd = new CommandParser().runCommand(p, newCommand);
		if (pCmd != null && pCmd.getStatus() != ReturnStatus.CMD_OK	) {
			throw new SMSException(pCmd.getLastError());
		}
	}
}
