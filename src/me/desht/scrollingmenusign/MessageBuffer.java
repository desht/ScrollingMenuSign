package me.desht.scrollingmenusign;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import me.jascotty2.bukkit.MinecraftChatStr;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class MessageBuffer {

	private static final Map<String, List<String>> bufferMap = new HashMap<String, List<String>>();
	private static final Map<String, Integer> currentPage = new HashMap<String, Integer>();
	private static final int pageSize = 18;	// 20 lines total, minus 2 for header and footer

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
	static void add(Player p, String message) {
		init(p);
		bufferMap.get(name(p)).add(message);
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
	static void add(Player p, String[] lines) {
		init(p);
		add(p, Arrays.asList(lines));
	}

	static void add(Player p, List<String> lines) {
		init(p);
		//TODO: apply MinecraftChatStr.alignTags(lines, true)
		//		in pagesize segments before adding to buffer
		
		// if block is bigger than a page, just add it
		if (lines.size() <= pageSize
				&& (getSize(p) % pageSize) + lines.size() > pageSize) {
			// else, add padding above to keep the block on one page
//			System.out.println("add " + lines.size() + " lines, cur size = " + getSize(p));
			for (int i = getSize(p) % pageSize; i < pageSize; ++i) {
//				System.out.println("pad " + i);
				bufferMap.get(name(p)).add("");
			}
		}
		for (String line : lines) {
			bufferMap.get(name(p)).add(line);
		}
	}

	/**
	 * Clear the player's message buffer
	 * 
	 * @param p
	 *            The player
	 */
	static void clear(Player p) {
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
	static void nextPage(Player player) {
		setPage(player, getPage(player) + 1, true);
	}

	/**
	 * Move to the previous page of the player's buffer.
	 * 
	 * @param player
	 *            The player
	 */
	static void prevPage(Player player) {
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
	static void showPage(Player player) {
		showPage(player, currentPage.get(name(player)));
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
			SMSUtils.errorMessage(player, "invalid argument '" + pageStr + "'");
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
	static void showPage(Player player, int pageNum) {
		if (!bufferMap.containsKey(name(player))) {
			return;
		}

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
			SMSUtils.statusMessage(player, ChatColor.GREEN + MinecraftChatStr.strPadCenterChat(header, 310, '-'));

			for (; i < nMessages && i < pageNum * pageSize; ++i) {
				SMSUtils.statusMessage(player, getLine(player, i));
			}

			String footer = nMessages > pageSize * pageNum ? "| Use /sms page [#|n|p] to see other pages |" : "";
			SMSUtils.statusMessage(player, ChatColor.GREEN + MinecraftChatStr.strPadCenterChat(footer, 310, '-'));

			setPage(player, pageNum);
		} else {
			// just dump the whole message buffer to the console
			for (String s : bufferMap.get(name(player))) {
				SMSUtils.statusMessage(null, ChatColor.stripColor(SMSUtils.parseColourSpec(s)));
			}
		}
	}
}
