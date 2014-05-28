package me.desht.scrollingmenusign;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import me.desht.dhutils.LogUtils;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.junit.Test;

public class SMSMenuItemTest {
    private final SMSMenu testMenu;

    public SMSMenuItemTest() {
        LogUtils.init("SMSMenuItemTest");

        testMenu = new SMSMenu("testmenu", "Test Menu", (Player) null);
    }

    @Test
    public void testCreation() {
        SMSMenuItem item1 = new SMSMenuItem(testMenu, "Item One", "\\hello", "a message");

        assertEquals("Label is correct", item1.getLabel(), "Item One");
        assertEquals("Command is correct", item1.getCommand(), "\\hello");
        assertEquals("Message is correct", item1.getMessage(), "a message");

        SMSMenuItem item2 = new SMSMenuItem(testMenu, ChatColor.RED + "Item Two", "\\bye", "another message");
        assertEquals("Raw label is correct", item2.getLabelStripped(), "Item Two");

        boolean ok = false;
        try {
            SMSMenuItem item3 = new SMSMenuItem(null, null, null, null);
        } catch (IllegalArgumentException e) {
            ok = true;
        }
        assertTrue("Null parameters throw an IllegalArgumentException", ok);
    }

    @Test
    public void testBuilderCreation() {
        SMSMenuItem item1 = new SMSMenuItem.Builder(testMenu, "Item Three").build();
        assertTrue("Comand is empty", item1.getCommand().isEmpty());
        assertTrue("Alt comand is empty", item1.getAltCommand().isEmpty());
        assertFalse("Icon is null", item1.hasIcon());

        SMSMenuItem item2 = new SMSMenuItem.Builder(testMenu, "Item Four")
                .withIcon("diamond")
                .withLore("Lore 1", "Lore 2", "Lore 3")
                .withPermissionNode("some.permission.node")
                .build();

        assertEquals("Icon is diamond", item2.getIcon().getType(), Material.DIAMOND);
        assertEquals("Lore has 3 lines", item2.getLore().length, 3);
        assertEquals("Permission node is correct", item2.getPermissionNode(), "some.permission.node");
    }
}
