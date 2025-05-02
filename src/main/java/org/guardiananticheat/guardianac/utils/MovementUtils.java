package org.guardiananticheat.guardianac.utils;

import org.bukkit.Location;

public class MovementUtils {
    public static double getDistance3D(Location loc1, Location loc2) {
        double dx = loc1.getX() - loc2.getX();
        double dy = loc1.getY() - loc2.getY();
        double dz = loc1.getZ() - loc2.getZ();
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    public static double getHorizontalDistance(Location loc1, Location loc2) {
        double dx = loc1.getX() - loc2.getX();
        double dz = loc1.getZ() - loc2.getZ();
        return Math.sqrt(dx * dx + dz * dz);
    }

    public static double getVerticalDistance(Location loc1, Location loc2) {
        return Math.abs(loc1.getY() - loc2.getY());
    }
}