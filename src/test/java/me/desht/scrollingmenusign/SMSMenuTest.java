package me.desht.scrollingmenusign;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import me.desht.dhutils.LogUtils;

import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.entity.Player;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class SMSMenuTest {
    public SMSMenuTest() {
        LogUtils.init("SMSMenuTest");
    }

    @Test
    public void testCreation() {
        SMSMenu menu = new SMSMenu("testmenu", "A Test Menu", (Player) null);
        menu.setAutosave(false);
        assertEquals("Name is 'testmenu'", "testmenu", menu.getName());
        assertEquals("Title is 'A Test Menu'", "A Test Menu", menu.getTitle());
        assertEquals("Owner is console", ScrollingMenuSign.CONSOLE_OWNER, menu.getOwner());
    }

    @Test
    public void testDeserialization() {
        ConfigurationSection node = new MemoryConfiguration();
        node.set("name", "testmenu");
        node.set("title", "&1Title 1");
        node.set("owner", "");

        Map<String, Object> item1 = Maps.newHashMap();
        item1.put("menu", "testmenu");
        item1.put("label", "Label 1 2 3");
        item1.put("command", "\\foo!");
        item1.put("message", "a message for you");
        item1.put("icon", "INK_SACK:WHITE");
        item1.put("lore", Arrays.asList("line 1", "line 2"));
        item1.put("usesRemaining.limits.globalMax", 10);
        item1.put("usesRemaining.limits.globalRemaining", 8);

        node.set("items", Lists.newArrayList(item1));

        boolean ok = true;
        try {
            SMSMenu menu = new SMSMenu(node);
            assertEquals("Name is correct", menu.getName(), "testmenu");
            assertEquals("Title is correct", menu.getTitle(), ChatColor.DARK_BLUE + "Title 1");
            assertEquals("Owner is console", menu.getOwnerId(), ScrollingMenuSign.CONSOLE_UUID);
            assertEquals("Item label is correct", menu.getItemAt(1).getLabel(), "Label 1 2 3");
        } catch (SMSException e) {
            ok = false;
        }
        assertTrue("Menu created ok", ok);
    }

    @Test
    public void testAddRemoveItems() {
        SMSMenu menu = new SMSMenu("testmenu", "A Test Menu", (Player) null);
        menu.setAutosave(false);
        menu.addItem(new SMSMenuItem(menu, "Label 1", "a command", ""));
        menu.addItem(new SMSMenuItem(menu, "Label 2", "another command", "msg 2"));
        menu.addItem(new SMSMenuItem(menu, "Label 3", "third command", ""));

        assertEquals("Menu has three items", 3, menu.getItemCount());
        assertEquals("Second label is 'Label 2'", "Label 2", menu.getItemAt(2).getLabel());

        menu.insertItem(2, new SMSMenuItem(menu, "Inserted item", "", "msg 3"));
        assertEquals("Inserted at position", "msg 3", menu.getItemAt(2).getMessage());

        menu.removeItem(1);
        assertEquals("Remove item", "Label 2", menu.getItemAt(2).getLabel());

        menu.removeAllItems();
        assertTrue("Menu is empty", menu.getItemCount() == 0);
    }

    @Test
    public void testSortMenu() {
        SMSMenu menu = new SMSMenu("testmenu", "A Test Menu", (Player) null);
        menu.setAutosave(false);
        menu.addItem(new SMSMenuItem(menu, "Zebra", "a command", ""));
        menu.addItem(new SMSMenuItem(menu, "Hotel", "another command", "msg 2"));
        menu.addItem(new SMSMenuItem(menu, "Charlie", "third command", ""));
        menu.addItem(new SMSMenuItem(menu, "Tango", "blah blah", ""));
        menu.sortItems();

        assertEquals("Sort items", "third command", menu.getItemAt(1).getCommand());
        assertEquals("Sort items", "", menu.getItemAt(4).getMessage());
    }
}
