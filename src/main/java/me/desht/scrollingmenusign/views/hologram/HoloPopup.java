package me.desht.scrollingmenusign.views.hologram;

import com.dsh105.holoapi.HoloAPI;
import com.dsh105.holoapi.api.Hologram;
import com.dsh105.holoapi.api.HologramFactory;
import com.dsh105.holoapi.api.touch.TouchAction;
import com.dsh105.holoapi.api.visibility.Visibility;
import com.dsh105.holoapi.protocol.Action;
import me.desht.dhutils.Debugger;
import me.desht.dhutils.MiscUtil;
import me.desht.scrollingmenusign.SMSException;
import me.desht.scrollingmenusign.SMSMenu;
import me.desht.scrollingmenusign.ScrollingMenuSign;
import me.desht.scrollingmenusign.enums.SMSUserAction;
import me.desht.scrollingmenusign.views.SMSPopup;
import me.desht.scrollingmenusign.views.SMSPrivateHoloView;
import me.desht.scrollingmenusign.views.SMSView;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;

import java.util.LinkedHashMap;

public class HoloPopup implements SMSPopup {
    private static final double POPUP_DISTANCE = 2.5;

    private final SMSPrivateHoloView view;
    private final Player player;
    private Hologram hologram = null;

    public HoloPopup(Player player, SMSPrivateHoloView view) {
        this.view = view;
        this.player = player;
    }

    @Override
    public SMSView getView() {
        return view;
    }

    public Player getPlayer() {
        return player;
    }

    @Override
    public void repaint() {
        String[] text = HoloUtil.buildText(view, player, (Integer) view.getAttribute(SMSPrivateHoloView.LINES));
        if (text.length != hologram.getLines().length) {
            popdown();  // force a new hologram to be created with the new size
        }
        if (hologram == null) {
            hologram = buildHologram(player, text);
        } else {
            hologram.updateLines(text);
        }
    }

    @Override
    public boolean isPoppedUp() {
        return hologram != null;
    }

    @Override
    public void popup() {
        String[] text = HoloUtil.buildText(view, player, (Integer) view.getAttribute(SMSPrivateHoloView.LINES));
        hologram = buildHologram(player, text);
    }

    @Override
    public void popdown() {
        if (hologram != null) {
            HoloAPI.getManager().stopTracking(hologram);
            hologram = null;
        }
    }

    public Hologram getHologram() {
        return hologram;
    }

    private Hologram buildHologram(Player player, String[] text) {
        Debugger.getInstance().debug("creating new private hologram for " + view.getName() + "/" + player.getName());
        Hologram h = new HologramFactory(ScrollingMenuSign.getInstance())
                .withLocation(getHologramPosition(player))
                .withText(text)
                .withSimplicity(true)
                .withVisibility(new HologramVisibility(player))
                .build();

        h.addTouchAction(new SMSHoloTouchAction(player, view));
        h.setTouchEnabled(true);

        return h;
    }

    private Location getHologramPosition(Player player) {
        return player.getEyeLocation().add(player.getLocation().getDirection().multiply(POPUP_DISTANCE));
    }

    private class HologramVisibility implements Visibility {
        private final Player player;

        public HologramVisibility(Player player) {
            this.player = player;
        }

        @Override
        public boolean isVisibleTo(Player player, String s) {
            return this.player.equals(player);
        }

        @Override
        public String getSaveKey() {
            return null;
        }

        @Override
        public LinkedHashMap<String, Object> getDataToSave() {
            return null;
        }
    }

    private class SMSHoloTouchAction implements TouchAction {
        private final SMSPrivateHoloView view;
        private final Player player;

        private SMSHoloTouchAction(Player player, SMSPrivateHoloView view) {
            this.view = view;
            this.player = player;
        }

        @Override
        public void onTouch(Player player, Action action) {
            if (player.equals(this.player)) {
                Debugger.getInstance().debug("Hologram action: player=" + player.getName() + " action=" + action + " view = " + view.getName());
                SMSUserAction ua = getAction(player, action);
                try {
                    SMSMenu m = view.getActiveMenu(player);
                    if (ua != null) {
                        ua.execute(player, view);
                    }
                    player.setMetadata(HoloUtil.LAST_HOLO_INTERACTION,
                            new FixedMetadataValue(ScrollingMenuSign.getInstance(), System.currentTimeMillis()));

                    if (ua == SMSUserAction.EXECUTE && ((Boolean) view.getAttribute(SMSPrivateHoloView.AUTOPOPDOWN)) && view.getActiveMenu(player) == m) {
                        view.hideGUI(player);
                    }
                } catch (SMSException e) {
                    MiscUtil.errorMessage(player, e.getMessage());
                }
            }
        }

        @Override
        public String getSaveKey() {
            return null;
        }

        @Override
        public LinkedHashMap<String, Object> getDataToSave() {
            return null;
        }

        private SMSUserAction getAction(Player player, Action action) {
            StringBuilder key = new StringBuilder();
            switch (action) {
                case RIGHT_CLICK:
                    key.append("sms.actions.rightclick.");
                    break;
                case LEFT_CLICK:
                    key.append("sms.actions.leftclick.");
                    break;
            }
            key.append(player.isSneaking() ? "sneak" : "normal");

            String s = ScrollingMenuSign.getInstance().getConfig().getString(key.toString(), "none");
            return SMSUserAction.valueOf(s.toUpperCase());
        }
    }
}
