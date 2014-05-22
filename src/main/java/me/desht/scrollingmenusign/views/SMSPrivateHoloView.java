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
    private final Map<UUID, HoloPopup> holograms = new HashMap<UUID, HoloPopup>();

    public SMSPrivateHoloView(String name, SMSMenu menu) {
        super(name, menu);

        registerAttribute(LINES, 4, "Number of lines visible in the hologram (including title)");
    }

    @Override
    public void showGUI(Player p) {
        if (!holograms.containsKey(p.getUniqueId())) {
            HoloPopup h = new HoloPopup(p, this);
            h.popup();
            holograms.put(p.getUniqueId(), h);
        }
    }

    @Override
    public void hideGUI(Player p) {
        HoloPopup popup = holograms.get(p.getUniqueId());
        if (popup != null) {
            popup.popdown();
            holograms.remove(p.getUniqueId());
        }
    }

    @Override
    public void toggleGUI(Player p) {
        if (hasActiveGUI(p)) {
            hideGUI(p);
        } else {
            showGUI(p);
        }
        p.closeInventory();
    }

    @Override
    public boolean hasActiveGUI(Player p) {
        return holograms.containsKey(p.getUniqueId());
    }

    @Override
    public SMSPopup getActiveGUI(Player p) {
        return holograms.get(p.getUniqueId());
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
}
