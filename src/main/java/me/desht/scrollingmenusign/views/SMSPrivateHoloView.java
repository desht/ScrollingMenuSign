package me.desht.scrollingmenusign.views;

import com.dsh105.holoapi.HoloAPI;
import me.desht.scrollingmenusign.SMSMenu;
import me.desht.scrollingmenusign.views.hologram.HoloPopup;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.Observable;
import java.util.UUID;

public class SMSPrivateHoloView extends SMSScrollableView implements PoppableView {
    public static final String LINES = "lines";
    public static final String AUTOPOPDOWN = "autopopdown";

    private final Map<UUID, HoloPopup> holograms = new HashMap<UUID, HoloPopup>();

    public SMSPrivateHoloView(String name, SMSMenu menu) {
        super(name, menu);

        registerAttribute(LINES, 4, "Number of lines visible in the hologram (including title)");
        registerAttribute(AUTOPOPDOWN, true, "Auto-popdown after item click?");
    }

    @Override
    public void showGUI(Player player) {
        if (!holograms.containsKey(player.getUniqueId())) {
            HoloPopup h = new HoloPopup(player, this);
            h.popup();
            holograms.put(player.getUniqueId(), h);
        }
    }

    @Override
    public void hideGUI(Player player) {
        HoloPopup popup = holograms.get(player.getUniqueId());
        if (popup != null) {
            popup.popdown();
            holograms.remove(player.getUniqueId());
        }
    }

    @Override
    public void toggleGUI(Player player) {
        if (hasActiveGUI(player)) {
            hideGUI(player);
        } else {
            showGUI(player);
        }
        player.closeInventory();
    }

    @Override
    public boolean hasActiveGUI(Player player) {
        return holograms.containsKey(player.getUniqueId());
    }

    @Override
    public SMSPopup getActiveGUI(Player player) {
        return holograms.get(player.getUniqueId());
    }

    @Override
    public void update(Observable obj, Object arg1) {
        ViewUpdateAction vu = ViewUpdateAction.getAction(arg1);
        switch (vu.getAction()) {
            case REPAINT:
            case SCROLLED:
                if (vu.getPlayer() == null) {
                    for (HoloPopup h : holograms.values()) {
                        h.repaint();
                    }
                } else {
                    HoloPopup h = holograms.get(vu.getPlayer().getUniqueId());
                    if (h != null) {
                        h.repaint();
                    }
                }
                break;
            default:
                break;
        }
    }

    @Override
    public String getType() {
        return "private-holo";
    }

    @Override
    public void onDeleted(boolean temporary) {
        for (HoloPopup holoPopup : holograms.values()) {
            HoloAPI.getManager().stopTracking(holoPopup.getHologram());
        }
    }

    @Override
    public void clearPlayerForView(Player player) {
        hideGUI(player);
    }

    @Override
    public String toString() {
        String s = holograms.size() == 1 ? "" : "s";
        return "private-holo: " + holograms.size() + " active popup" + s;
    }
}
