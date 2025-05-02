package org.guardiananticheat.guardianac.utils;

import org.bukkit.Location;
import org.bukkit.event.player.PlayerMoveEvent;

public class PlayerData {
    private Location fromLocation;
    private int airTicks = 0;
    private int violationLevel = 0;
    private boolean jumping = false;
    private long lastDamageTime = 0;
    private int graceTicks = 0;

    public void onMove(PlayerMoveEvent event) {
        this.fromLocation = event.getFrom();

        if (event.getPlayer().getVelocity().getY() > 0.3 && event.getPlayer().isOnGround()) {
            jumping = true;
        } else if (event.getPlayer().isOnGround()) {
            jumping = false;
        }
    }

    public Location getFromLocation() {
        return fromLocation;
    }

    public int getAirTicks() {
        return airTicks;
    }

    public void incrementAirTicks() {
        this.airTicks++;
    }

    public void resetAirTicks() {
        this.airTicks = 0;
    }

    public int getViolationLevel() {
        return violationLevel;
    }

    public void incrementViolationLevel() {
        this.violationLevel++;
    }

    public void decrementViolationLevel() {
        if (this.violationLevel > 0) {
            this.violationLevel--;
        }
    }

    public void resetViolationLevel() {
        this.violationLevel = 0;
    }

    public boolean isJumping() {
        return jumping;
    }

    public long getLastDamageTime() {
        return lastDamageTime;
    }

    public void setLastDamageTime(long time) {
        this.lastDamageTime = time;
    }

    public int getGraceTicks() {
        return graceTicks;
    }

    public void setGraceTicks(int ticks) {
        this.graceTicks = ticks;
    }

    public void decrementGraceTicks() {
        if (this.graceTicks > 0) {
            this.graceTicks--;
        }
    }

    public void reset() {
        resetAirTicks();
        resetViolationLevel();
        jumping = false;
    }
}