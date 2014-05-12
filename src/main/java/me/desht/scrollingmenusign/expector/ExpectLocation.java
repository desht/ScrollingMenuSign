package me.desht.scrollingmenusign.expector;

import me.desht.dhutils.responsehandler.ExpectBase;

import org.bukkit.Location;

public abstract class ExpectLocation extends ExpectBase {
    private Location loc = null;

    public Location getLocation() {
        if (loc == null) {
            throw new NullPointerException("location must be initialised first");
        }
        return loc;
    }

    public void setLocation(Location loc) {
        this.loc = loc;
    }
}
