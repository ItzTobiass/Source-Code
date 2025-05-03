package org.guardiananticheat.guardianac.utils;

import org.bukkit.Location;
import org.bukkit.util.Vector;

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

    public static double getAngle(Location loc1, Location loc2) {
        Vector direction = loc1.getDirection();
        Vector toTarget = loc2.toVector().subtract(loc1.toVector()).normalize();
        return Math.toDegrees(direction.angle(toTarget));
    }

    public static boolean isVerticalMove(Location from, Location to) {
        return from.getX() == to.getX() && from.getZ() == to.getZ() && from.getY() != to.getY();
    }

    public static boolean isHorizontalMove(Location from, Location to) {
        return from.getY() == to.getY() && (from.getX() != to.getX() || from.getZ() != to.getZ());
    }

    public static double getSpeedBPS(Location from, Location to, long timeElapsed) {
        if (timeElapsed <= 0) return 0;
        double distance = getHorizontalDistance(from, to);
        return (distance * 1000) / timeElapsed;
    }

    public static float getDirectionAngle(Location from, Location to) {
        double dx = to.getX() - from.getX();
        double dz = to.getZ() - from.getZ();
        return (float) Math.toDegrees(Math.atan2(dz, dx)) - 90f;
    }
}