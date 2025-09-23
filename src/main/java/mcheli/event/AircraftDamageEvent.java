package mcheli.event;

import cpw.mods.fml.common.eventhandler.Event;

public class AircraftDamageEvent extends Event {
    private final String attackerName;
    private final String vehicleName;
    private final float damage;
    private final float maxDamage;

    public AircraftDamageEvent(String attackerName, String vehicleName, float damage, float maxDamage) {
        this.attackerName = attackerName;
        this.vehicleName = vehicleName;
        this.damage = damage;
        this.maxDamage = maxDamage;
    }

    public String getAttackerName() {
        return attackerName;
    }

    public String getVehicleName() {
        return vehicleName;
    }

    public float getDamage() {
        return damage;
    }

    public float getMaxDamage() {
        return maxDamage;
    }
}
