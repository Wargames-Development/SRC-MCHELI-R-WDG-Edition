package mcheli.tank;

import mcheli.MCH_Config;
import mcheli.MCH_MOD;
import mcheli.aircraft.MCH_AircraftInfo;
import net.minecraft.item.Item;
import net.minecraft.util.Vec3;

import java.util.ArrayList;
import java.util.List;

public class MCH_TankInfo extends MCH_AircraftInfo {

    public MCH_ItemTank item = null;
    public int weightType = 0;
    public float weightedCenterZ = 0.0F;
    private Boolean wheeledHandling;
    private float wheeledAcceleration;
    private float wheeledTurnRadius;


    public MCH_TankInfo(String name) {
        super(name);
        this.setImpactAngleCoefficientDefault(true);
        this.radarFollowTurretYaw = true;
    }

    public Item getItem() {
        return this.item;
    }

    public List getDefaultWheelList() {
        ArrayList list = new ArrayList();
        list.add(new MCH_AircraftInfo.Wheel(Vec3.createVectorHelper(1.5D, -0.24D, 2.0D)));
        list.add(new MCH_AircraftInfo.Wheel(Vec3.createVectorHelper(1.5D, -0.24D, -2.0D)));
        return list;
    }

    public float getDefaultSoundRange() {
        return 50.0F;
    }

    public float getDefaultRotorSpeed() {
        return 47.94F;
    }

    private float getDefaultStepHeight() {
        return 0.6F;
    }

    public float getMaxSpeed() {
        return 1.8F;
        //wtf does this do, it does something idk lmao
    }

    public int getDefaultMaxZoom() {
        return 8;
    }

    public String getDefaultHudName(int seatId) {
        return seatId <= 0 ? "tank" : (seatId == 1 ? "tank" : "gunner");
    }

    public boolean isValidData() throws Exception {
        // Resolve after all fields load: Speed may precede WeightType/WheeledHandling.
        this.speed = MCH_WheeledControlMath.speedLimit(this.speed, MCH_Config.AllTankSpeed.prmDouble, this.isWheeledHandling());
        return super.isValidData();
    }

    public boolean isWheeledHandling() {
        return this.wheeledHandling != null ? this.wheeledHandling : this.weightType == 1;
    }

    public double getWheelbase() {
        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < this.wheels.size(); ++i) {
            double z = ((MCH_AircraftInfo.Wheel) this.wheels.get(i)).pos.zCoord;
            if (Double.isFinite(z)) {
                min = Math.min(min, z);
                max = Math.max(max, z);
            }
        }
        return max > min ? Math.max(1.0D, max - min) : 4.0D;
    }

    public double getWheeledAcceleration() {
        // One block is treated as one metre; 20 ticks/second means 400 ticks squared.
        return this.wheeledAcceleration > 0.0F ? this.wheeledAcceleration / 400.0D
                : 0.012D + this.speed / 240.0D;
    }

    public double getWheeledAcceleration(double speed) {
        return this.wheeledAcceleration > 0.0F
                ? MCH_WheeledControlMath.accelerationStep(this.getWheeledAcceleration(), speed, this.speed)
                : this.getWheeledAcceleration();
    }

    public double getSteeringWheelbase() {
        // Calibrate the bicycle model's full-lock radius without moving terrain probes.
        return this.wheeledTurnRadius > 0.0F ? this.wheeledTurnRadius * Math.tan(Math.toRadians(35.0D))
                : this.getWheelbase();
    }

    private float readWheeledParameter(String data, float min, float max) {
        float value = Float.parseFloat(data.trim());
        if (!Float.isFinite(value) || value < min || value > max) {
            throw new IllegalArgumentException("Invalid wheeled handling value: " + data);
        }
        return value;
    }

    public void loadItemData(String item, String data) {
        if (item.equalsIgnoreCase("Speed")) {
            this.speed = this.toFloat(data, 0.0F, 8.0F);
        } else if (item.equalsIgnoreCase("WeightType")) {
            data = data.toLowerCase();
            this.weightType = data.equals("tank") ? 2 : (data.equals("car") ? 1 : 0);
        } else if (item.equalsIgnoreCase("WeightedCenterZ")) {
            this.weightedCenterZ = this.toFloat(data, -1000.0F, 1000.0F);
        } else if (item.equalsIgnoreCase("WheeledHandling")) {
            this.wheeledHandling = this.toBool(data);
        } else if (item.equalsIgnoreCase("WheeledAcceleration")) {
            this.wheeledAcceleration = this.readWheeledParameter(data, 0.1F, 10.0F);
        } else if (item.equalsIgnoreCase("WheeledTurnRadius")) {
            this.wheeledTurnRadius = this.readWheeledParameter(data, 2.0F, 50.0F);
        } else {
            super.loadItemData(item, data);
        }
        MCH_AircraftInfo.allAircraftInfo.put(name, this);
    }

    public String getDirectoryName() {
        return "tanks";
    }

    public String getKindName() {
        return "tank";
    }

    public void preReload() {
        super.preReload();
        this.wheeledHandling = null;
        this.wheeledAcceleration = 0.0F;
        this.wheeledTurnRadius = 0.0F;
    }

    public void postReload() {
        MCH_MOD.proxy.registerModelsTank(super.name, true);
    }
}
