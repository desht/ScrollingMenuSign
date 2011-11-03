package me.desht.scrollingmenusign.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class MessagePager {

	private static final Map<String, List<String>> bufferMap = new HashMap<String, List<String>>();
	private static final Map<String, Integer> currentPage = new HashMap<String, Integer>();
	private static final int pageSize = 18;	// 20 lines total, minus 2 for header and footer

	private static String pageCmd = null; // "/sms page [#|n|p]";
	
	/**
	 * initialize the buffer for the player if necessary
	 * 
	 * @param p
	 */
	static private void init(Player p) {
		if (!bufferMap.containsKey(name(p))) {
			bufferMap.put(name(p), new ArrayList<String>());
			currentPage.put(name(p), 1);
		}
	}

	public static String getPageCmd() {
		return pageCmd;
	}

	public static void setPageCmd(String pageCmd) {
		MessagePager.pageCmd = pageCmd;
	}

	/**
	 * Get the player's name
	 * 
	 * @param p
	 *            The player, may be null
	 * @return Player's name, or &CONSOLE if the player is null
	 */
	static private String name(Player p) {
		return p == null ? "&CONSOLE" : p.getName();
	}

	/**
	 * Add a message to the buffer.
	 * 
	 * @param p
	 *            The player
	 * @param message
	 *            The message line to add
	 */
	public static void add(Player p, String message) {
		init(p);
		String wrapped = MinecraftChatStr.strWordWrap(MiscUtil.parseColourSpec(message), 2);
		for (String s : wrapped.split("\\n")) {
			bufferMap.get(name(p)).add(s);
		}
	}

	/**
	 * Add a block of messages. All message should stay on the same page if
	 * possible - add padding to ensure this where necessary. If block is larger
	 * than the page size, then just add it.
	 * 
	 * @param p
	 *            The player
	 * @param messages
	 *            List of message lines to add
	 */
	public static void add(Player p, String[] lines) {
//		init(p);
		add(p, Arrays.asList(lines));
	}

	public static void add(Player p, List<String> lines) {
//		init(p);
		//TODO: apply MinecraftChatStr.alignTags(lines, true)
		//		in pagesize segments before adding to buffer
		
		// if block is bigger than a page, just add it
		int nLines = getLineCount(lines);
		if (nLines <= pageSize
				&& (getSize(p) % pageSize) + nLines > pageSize
				&& p != null) {
			// else, add padding above to keep the block on one page
			for (int i = getSize(p) % pageSize; i < pageSize; ++i) {
				bufferMap.get(name(p)).add("");
			}
		}
		for (String line : lines) {
			add(p, line);
//			bufferMap.get(name(p)).add(line);
		}
	}

	private static int getLineCount(List<String> lines) {
		int ret = 0;
		for (String line : lines) {
			ret += (MinecraftChatStr.getStringWidth(line) / MinecraftChatStr.chatwidth) + 1;
		}
		return ret;
	}
	
	/**
	 * Clear the player's message buffer
	 * 
	 * @param p
	 *            The player
	 */
	public static void clear(Player p) {
		if (!bufferMap.containsKey(name(p))) {
			return;
		}

		bufferMap.get(name(p)).clear();
		currentPage.put(name(p), 1);
	}

	/**
	 * Delete the message buffer for the player. Should be called when the
	 * player logs out.
	 * 
	 * @param p
	 *            The player
	 */
	static void delete(Player p) {
		bufferMap.remove(name(p));
		currentPage.remove(name(p));
	}

	/**
	 * Get the number of lines in the player's message buffer.
	 * 
	 * @param p
	 *            The player
	 * @return The number of lines
	 */
	static int getSize(Player p) {
		if (!bufferMap.containsKey(name(p))) {
			return 0;
		}

		return bufferMap.get(name(p)).size();
	}

	/**
	 * Get the page size (number of lines in one page)
	 * 
	 * @return The page size
	 */
	static int getPageSize() {
		return pageSize;
	}

	/**
	 * Get the number of pages in the player's buffer.
	 * 
	 * @return Number of pages in the buffer, including the partial page at the
	 *         end
	 */
	static int getPageCount(Player p) {
		return (getSize(p) - 1) / pageSize + 1;
	}

	/**
	 * Get a line of text from the player's buffer
	 * 
	 * @param p
	 *            The player
	 * @param i
	 *            The line number
	 * @return The line of text at that line
	 */
	static String getLine(Player p, int i) {
		if (!bufferMap.containsKey(name(p))) {
			return null;
		}

		return bufferMap.get(name(p)).get(i);
	}

	/**
	 * Set the current page for the player. This is ignored if the page number
	 * is out of range.
	 * 
	 * @param player
	 *            The player
	 * @param page
	 *            The page number.
	 */
	static void setPage(Player player, int page) {
		setPage(player, page, false);
	}

	static void setPage(Player player, int page, boolean wrap) {
		if ((page < 1 || page > getPageCount(player)) && !wrap) {
			return;
		}
		if (page < 1) {
			page = getPageCount(player);
		} else if (page > getPageCount(player)) {
			page = 1;
		}
		currentPage.put(name(player), page);
	}
	
	/**
	 * Move to the next page of the player's buffer.
	 * 
	 * @param player
	 *            The player
	 */
	public static void nextPage(Player player) {
		setPage(player, getPage(player) + 1, true);
	}

	/**
	 * Move to the previous page of the player's buffer.
	 * 
	 * @param player
	 *            The player
	 */
	public static void prevPage(Player player) {
		setPage(player, getPage(player) - 1, true);
	}

	/**
	 * Get the current page for the player
	 * 
	 * @param player
	 *            The player
	 * @return The current page for the player
	 */
	static int getPage(Player player) {
		return currentPage.get(name(player));
	}

	/**
	 * Display the current page for the player.
	 * 
	 * @param player
	 *            The player
	 */
	public static void showPage(Player player) {
		Integer p = currentPage.get(name(player));
		showPage(player, p == null ? 1 : p);
	}

	/**
	 * Display the specified page for the player.
	 * 
	 * @param player
	 *            The player
	 * @param pageStr
	 *            A string containing the page number to display
	 */
	static void showPage(Player player, String pageStr) {
		try {
			int pageNum = Integer.parseInt(pageStr);
			showPage(player, pageNum);
		} catch (NumberFormatException e) {
			MiscUtil.errorMessage(player, "invalid argument '" + pageStr + "'");
		}
	}

	/**
	 * Display the specified page for the player.
	 * 
	 * @param player
	 *            The player
	 * @param pageNum
	 *            The page number to display
	 */
	public static void showPage(Player player, int pageNum) {
		if (!bufferMap.containsKey(name(player))) {
			return;
		}
		if (getSize(player) == 0)
			return;

		if (player != null) {
			// pretty paged display
			if (pageNum < 1 || pageNum > getPageCount(player)) {
				throw new IllegalArgumentException("Page number " + pageNum + " is out of range.");
			}

			int nMessages = getSize(player);
			int i = (pageNum - 1) * pageSize;
			
			String header = String.format("| %d-%d of %d lines (page %d/%d) |",
			                              i + 1, Math.min(pageNum * pageSize, nMessages),
			                              nMessages, pageNum, getPageCount(player));
			MiscUtil.statusMessage(player, ChatColor.GREEN + MinecraftChatStr.strPadCenterChat(header, MinecraftChatStr.chatwidth, '-'));

			for (; i < nMessages && i < pageNum * pageSize; ++i) {
				MiscUtil.rawMessage(player, getLine(player, i));
			}

			String footer = "";
			if (nMessages > pageSize * pageNum && pageCmd != null) {
				footer = "| Use " + pageCmd + " to see other pages |";
			}
			MiscUtil.statusMessage(player, ChatColor.GREEN + MinecraftChatStr.strPadCenterChat(footer, MinecraftChatStr.chatwidth, '-'));

			setPage(player, pageNum);
		} else {
			// just dump the whole message buffer to the console
			List<String> buffer = bufferMap.get(name(player));
			if (buffer != null) {
				for (String s : buffer) {
					MiscUtil.statusMessage(null, ChatColor.stripColor(MiscUtil.parseColourSpec(s)));
				}
			}
		}
	}
}
