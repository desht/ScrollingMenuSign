package me.desht.scrollingmenusign.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class MessagePager {

	private static String pageCmd = "";
	private static final String CONSOLE = "&CONSOLE";
	private static final int pageSize = 18;	// 20 lines total, minus 2 for header and footer
	private static final Map<String, MessagePager> pagers = new HashMap<String, MessagePager>();

	private int currentPage;
	private List<String> messages;
	private String playerName;

	public MessagePager(String playerName) {
		this.playerName = playerName;
		this.currentPage = 1;
		this.messages = new ArrayList<String>();
	}

	/**
	 * Get the message pager for the given player.
	 * 
	 * @param player	the player object
	 * @return			the player's message pager
	 */
	public static MessagePager getPager(Player player) {
		return getPager(name(player));
	}

	/**
	 * Get the message pager for the given player.
	 * 
	 * @param playerName	the player name
	 * @return				the player's message pager
	 */
	public static MessagePager getPager(String playerName) {
		if (!pagers.containsKey(playerName)) {
			pagers.put(playerName, new MessagePager(playerName));
		}
		return pagers.get(playerName);
	}

	//	/**
	//	 * initialize the buffer for the player if necessary
	//	 * 
	//	 * @param p
	//	 */
	//	static private void init(Player p) {
	//		if (!bufferMap.containsKey(name(p))) {
	//			bufferMap.put(name(p), new ArrayList<String>());
	//			currentPage.put(name(p), 1);
	//		}
	//	}

	/**
	 * Delete the message buffer for the player. Should be called when the
	 * player logs out.
	 * 
	 * @param player		The player object
	 */
	public static void deletePager(Player player) {
		deletePager(name(player));
	}

	/**
	 * Delete the message buffer for the player. Should be called when the
	 * player logs out.
	 * 
	 * @param player		The player name
	 */
	public static void deletePager(String playerName) {
		pagers.remove(playerName);
	}

	/**
	 * Get the page size (number of lines in one page)
	 * 
	 * @return The page size
	 */
	public static int getPageSize() {
		return pageSize;
	}

	/**
	 * Get the player's name
	 * 
	 * @param p
	 *            The player, may be null
	 * @return Player's name, or &CONSOLE if the player is null
	 */
	static private String name(Player p) {
		return p == null ? CONSOLE : p.getName();
	}

	/**
	 * Clear this message buffer
	 */
	public MessagePager clear() {
		currentPage = 1;
		messages.clear();
		return this;
	}

	/**
	 * Add a message to the buffer.
	 * 
	 * @param line	The message line to add
	 */
	public void add(String line) {
		messages.add(line);
	}

	/**
	 * Add a block of messages. All message should stay on the same page if
	 * possible - add padding to ensure this where necessary. If block is larger
	 * than the page size, then just add it.
	 * 
	 * @param p
	 *            The player
	 * @param lines
	 *            List of message lines to add
	 */
	public void add(String[] lines) {
		add(Arrays.asList(lines));
	}

	public void add(List<String> lines) {
		//TODO: apply MinecraftChatStr.alignTags(lines, true)
		//		in pagesize segments before adding to buffer

		// if block is bigger than a page, just add it
		if (lines.size() <= pageSize
				&& (messages.size() % pageSize) + lines.size() > pageSize
				&& !playerName.equals(CONSOLE)) {
			// else, add padding above to keep the block on one page
			for (int i = messages.size() % pageSize; i < pageSize; ++i) {
				//System.out.println("pad " + i);
				messages.add("");
			}
		}
		for (String line : lines) {
			messages.add(line);
		}
	}

	/**
	 * Get the number of lines in the message buffer.
	 * 
	 * @param p		The player
	 * @return 		The number of lines in the buffer
	 */
	public int getSize() {
		return messages.size();
	}

	/**
	 * Get the number of pages in the  buffer.
	 * 
	 * @return number of pages in the buffer, including the partial page at the end
	 */
	public int getPageCount() {
		return (getSize() - 1) / pageSize + 1;
	}

	/**
	 * Get a line of text from the buffer
	 * 
	 * @param i		The line number
	 * @return 		The line of text at that line
	 */
	public String getLine(int i) {
		return messages.get(i);
	}

	public void setPage(int page) {
		setPage(page, false);
	}

	/**
	 * Set the current page for this message buffer.
	 * 
	 * @param page The page number.
	 * @param wrap	If true, automatically wrap to beginning or end if the page number is out of range.
	 */
	public void setPage(int page, boolean wrap) {
		if ((page < 1 || page > getPageCount()) && !wrap) {
			return;
		}
		if (page < 1) {
			page = getPageCount();
		} else if (page > getPageCount()) {
			page = 1;
		}
		currentPage = page;
	}

	/**
	 * Move to the next page of the player's buffer.
	 * 
	 * @param player
	 *            The player
	 */
	public void nextPage() {
		setPage(getPage() + 1, true);
	}

	/**
	 * Move to the previous page of the player's buffer.
	 * 
	 * @param player
	 *            The player
	 */
	public void prevPage() {
		setPage(getPage() - 1, true);
	}

	/**
	 * Get the current page for the message buffer
	 * 
	 * @return The current page for the player
	 */
	public int getPage() {
		return currentPage;
	}

	/**
	 * Display the current page for the player.
	 * 
	 * @param player
	 *            The player
	 */
	public void showPage() {
		showPage(currentPage);
	}

	/**
	 * Display the specified page for the player.

	 * @param pageStr	A string containing the page number to display
	 */
	public void showPage(String pageStr) throws NumberFormatException {
		int pageNum = Integer.parseInt(pageStr);
		showPage(pageNum);
	}

	/**
	 * Display the specified page for the player.
	 * 
	 * @param player
	 *            The player
	 * @param pageNum
	 *            The page number to display
	 */
	public void showPage(int pageNum) {
		if (!playerName.equals(CONSOLE)) {
			// pretty paged display
			if (pageNum < 1 || pageNum > getPageCount()) {
				throw new IllegalArgumentException("Page number " + pageNum + " is out of range.");
			}

			Player player = Bukkit.getServer().getPlayer(playerName);
			if (player == null) {
				return;
			}
			
			int i = (pageNum - 1) * pageSize;
			int nMessages = getSize();
			String header = String.format("| %d-%d of %d lines (page %d/%d) |",
			                                   i + 1, Math.min(pageSize * pageNum, nMessages), nMessages,
			                                   pageNum, getPageCount());
			MiscUtil.statusMessage(player, ChatColor.GREEN + MinecraftChatStr.strPadCenterChat(header, 310, '-'));

			for (; i < nMessages && i < pageNum * pageSize; ++i) {
				MiscUtil.statusMessage(player, getLine(i));
			}

			String footer =  nMessages > pageSize * pageNum ? "| Use " + pageCmd + " to see other pages |" : "";
			MiscUtil.statusMessage(player, ChatColor.GREEN + MinecraftChatStr.strPadCenterChat(footer, 310, '-'));

			setPage(pageNum);
		} else {
			// just dump the whole message buffer to the console
			for (String s : messages) {
				MiscUtil.statusMessage(null, ChatColor.stripColor(MiscUtil.parseColourSpec(s)));
			}
		}
	}

	public static void setPageCmd(String string) {
		pageCmd = string;
	}
}
