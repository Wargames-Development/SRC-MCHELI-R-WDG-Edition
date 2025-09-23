package mcheli.event;

import cpw.mods.fml.common.eventhandler.Event;

public class AircraftDestoryEvent extends Event {
    private final String attackerName;
    private final String vehicleName;

    public AircraftDestoryEvent(String attackerName, String vehicleName) {
        this.attackerName = attackerName;
        this.vehicleName = vehicleName;
    }

    public String getAttackerName() {
        return attackerName;
    }

    public String getVehicleName() {
        return vehicleName;
    }
}
