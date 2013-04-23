package me.desht.scrollingmenusign;

import static org.junit.Assert.*;

import me.desht.dhutils.LogUtils;

import org.junit.Test;

public class SMSMenuTest {
	public SMSMenuTest() {
		LogUtils.init(null);
	}

	@Test
	public void testCreation() {
		SMSMenu menu = new SMSMenu("testmenu", "A Test Menu", "someone");
		assertEquals("Name is 'testmenu'", "testmenu", menu.getName());
		assertEquals("Title is 'A Test Menu'", "A Test Menu", menu.getTitle());
		assertEquals("Owner is 'someone'", "someone", menu.getOwner());
	}
	
	@Test
	public void testOwnerNotEmpty() {
		boolean ok = false;
		try {
			new SMSMenu("testmenu", "A Test Menu", "");
		} catch (SMSException e) {
			ok = true;
		}
		assertTrue("Exception thrown when empty owner passed", ok);
	}
	
	@Test
	public void testAddRemoveItems() {
		SMSMenu menu = new SMSMenu("testmenu", "A Test Menu", "someone");
		menu.addItem("Label 1", "a command", "");
		menu.addItem("Label 2", "another command", "msg 2");
		menu.addItem("Label 3", "third command", "");
		
		assertEquals("Menu has three items", 3, menu.getItemCount());
		assertEquals("Second label is 'Label 2'", "Label 2", menu.getItemAt(2).getLabel());
	}
}
