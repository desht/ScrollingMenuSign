package me.desht.scrollingmenusign.spout;

import java.net.MalformedURLException;
import java.net.URL;

import me.desht.dhutils.LogUtils;
import me.desht.scrollingmenusign.ScrollingMenuSign;
import me.desht.scrollingmenusign.views.SMSPopup;
import me.desht.scrollingmenusign.views.SMSSpoutView;

import org.getspout.spoutapi.gui.GenericTexture;
import org.getspout.spoutapi.gui.RenderPriority;

public class SMSListTexture extends GenericTexture {
    private final SMSPopup popup;

    public SMSListTexture(SMSPopup popup) {
        this.popup = popup;

        setDrawAlphaChannel(true);
        setPriority(RenderPriority.Highest);    // put it behind the list widget

        updateURL();
    }

    public void updateURL() {
        try {
            String textureName = popup.getView().getAttributeAsString(SMSSpoutView.TEXTURE);
            if (textureName != null && !textureName.isEmpty()) {
                URL textureURL = ScrollingMenuSign.makeImageURL(textureName);
                setUrl(textureURL.toString());
            } else {
                setUrl("");
            }
        } catch (MalformedURLException e) {
            LogUtils.warning("malformed texture URL for spout view " + popup.getView().getName() + ": " + e.getMessage());
        }
    }
}
