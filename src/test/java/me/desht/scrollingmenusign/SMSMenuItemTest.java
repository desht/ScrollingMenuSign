package me.desht.scrollingmenusign;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import me.desht.dhutils.LogUtils;

import org.bukkit.ChatColor;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.material.Dye;
import org.junit.Test;

import java.util.Arrays;
import java.util.UUID;

import static org.mockito.Mockito.*;

public class SMSMenuItemTest {
    private final UUID mockUUID1 = UUID.randomUUID();
    private final UUID mockUUID2 = UUID.randomUUID();
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

    @Test
    public void testDeserialization() {
        ConfigurationSection node = new MemoryConfiguration();
        node.set("menu", testMenu.getName());
        node.set("label", "Label 1 2 3");
        node.set("command", "\\foo!");
        node.set("message", "a message for you");
        node.set("icon", "INK_SACK:WHITE");
        node.set("lore", Arrays.asList("line 1", "line 2"));
        node.set("usesRemaining.limits.globalMax", 10);
        node.set("usesRemaining.limits.globalRemaining", 8);
        SMSMenuItem item1 = new SMSMenuItem(testMenu, node);

        assertEquals("Label", item1.getLabel(), "Label 1 2 3");
        assertEquals("Command", item1.getCommand(), "\\foo!");
        assertEquals("Message", item1.getMessage(), "a message for you");
        assertEquals("Alt Command", item1.getAltCommand(), "");
        Player mockedPlayer = mock(Player.class);
        when(mockedPlayer.getUniqueId()).thenReturn(mockUUID1);
        assertEquals("Usage", item1.getUseLimits().getRemainingUses(mockedPlayer), 8);
        assertEquals("Icon mat", item1.getIcon().getType(), Material.INK_SACK);
        Dye dye = (Dye) item1.getIcon().getData();
        assertEquals("Icon data", dye.getColor(), DyeColor.WHITE);
        assertEquals("Lore", item1.getLore()[1], "line 2");
    }

    @Test
    public void testItemUseLimits() {
        Player mockedPlayer = mock(Player.class);
        when(mockedPlayer.getUniqueId()).thenReturn(mockUUID1);
        Player mockedPlayer2 = mock(Player.class);
        when(mockedPlayer2.getUniqueId()).thenReturn(mockUUID2);

        SMSMenuItem item1 = new SMSMenuItem.Builder(testMenu, "Use Limits")
                .withCommand("\\hello!")
                .build();

        item1.getUseLimits().setUses(5);
        item1.getUseLimits().use(mockedPlayer);
        item1.getUseLimits().use(mockedPlayer2);
        item1.getUseLimits().use(mockedPlayer2);
        assertEquals("Per-player use limits 1", item1.getUseLimits().getRemainingUses(mockedPlayer), 4);
        assertEquals("Per-player use limits 2", item1.getUseLimits().getRemainingUses(mockedPlayer2), 3);

        item1.getUseLimits().setGlobalUses(10);
        item1.getUseLimits().use(mockedPlayer);
        item1.getUseLimits().use(mockedPlayer);
        item1.getUseLimits().use(mockedPlayer);
        item1.getUseLimits().use(mockedPlayer2);
        assertEquals("Global use limits 1", item1.getUseLimits().getRemainingUses(mockedPlayer), 6);
        assertEquals("Global use limits 2", item1.getUseLimits().getRemainingUses(mockedPlayer2), 6);
    }
}
