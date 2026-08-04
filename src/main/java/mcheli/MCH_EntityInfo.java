package mcheli;

import mcheli.aircraft.MCH_EntityAircraft;
import mcheli.weapon.MCH_EntityBaseBullet;
import net.minecraft.entity.Entity;

public class MCH_EntityInfo {
    public static final byte CM_FLAG_CHAFF = 1;
    public static final byte CM_FLAG_ECM = 1 << 1;
    public static final byte CM_FLAG_JAMMING = 1 << 2;

    public int entityId;
    public String worldName;
    public String entityName;
    public String entityClassName;
    public double posX;
    public double posY;
    public double posZ;
    public double lastTickPosX;
    public double lastTickPosY;
    public double lastTickPosZ;
    public float rotationYaw;
    public float rotationPitch;
    public float rotationRoll;
    public float turretYaw;
    public float turretPitch;
    public boolean destroyed;
    public byte countermeasureFlags;
    public long countermeasureUntilTick;
    public long lastUpdateTime;

    public MCH_EntityInfo(int entityId, String worldName, String entityName, String entityClassName, double posX, double posY, double posZ, double lastTickPosX, double lastTickPosY, double lastTickPosZ) {
        this(entityId, worldName, entityName, entityClassName, posX, posY, posZ, lastTickPosX, lastTickPosY, lastTickPosZ, 0.0F, 0.0F, (byte)0, -1L);
    }

    public MCH_EntityInfo(int entityId, String worldName, String entityName, String entityClassName, double posX, double posY, double posZ, double lastTickPosX, double lastTickPosY, double lastTickPosZ, byte countermeasureFlags, long countermeasureUntilTick) {
        this(entityId, worldName, entityName, entityClassName, posX, posY, posZ, lastTickPosX, lastTickPosY, lastTickPosZ, 0.0F, 0.0F, countermeasureFlags, countermeasureUntilTick);
    }

    public MCH_EntityInfo(int entityId, String worldName, String entityName, String entityClassName, double posX, double posY, double posZ, double lastTickPosX, double lastTickPosY, double lastTickPosZ,
                          float rotationYaw, float rotationPitch, byte countermeasureFlags, long countermeasureUntilTick) {
        this(entityId, worldName, entityName, entityClassName, posX, posY, posZ, lastTickPosX, lastTickPosY, lastTickPosZ,
            rotationYaw, rotationPitch, countermeasureFlags, countermeasureUntilTick, 0.0F);
    }

    public MCH_EntityInfo(int entityId, String worldName, String entityName, String entityClassName, double posX, double posY, double posZ, double lastTickPosX, double lastTickPosY, double lastTickPosZ,
                          float rotationYaw, float rotationPitch, byte countermeasureFlags, long countermeasureUntilTick, float rotationRoll) {
        this(entityId, worldName, entityName, entityClassName, posX, posY, posZ, lastTickPosX, lastTickPosY, lastTickPosZ,
            rotationYaw, rotationPitch, countermeasureFlags, countermeasureUntilTick, rotationRoll, false);
    }

    public MCH_EntityInfo(int entityId, String worldName, String entityName, String entityClassName, double posX, double posY, double posZ, double lastTickPosX, double lastTickPosY, double lastTickPosZ,
                          float rotationYaw, float rotationPitch, byte countermeasureFlags, long countermeasureUntilTick, float rotationRoll, boolean destroyed) {
        this(entityId, worldName, entityName, entityClassName, posX, posY, posZ, lastTickPosX, lastTickPosY, lastTickPosZ,
            rotationYaw, rotationPitch, countermeasureFlags, countermeasureUntilTick, rotationRoll, destroyed, rotationYaw, rotationPitch);
    }

    public MCH_EntityInfo(int entityId, String worldName, String entityName, String entityClassName, double posX, double posY, double posZ, double lastTickPosX, double lastTickPosY, double lastTickPosZ,
                          float rotationYaw, float rotationPitch, byte countermeasureFlags, long countermeasureUntilTick, float rotationRoll, boolean destroyed,
                          float turretYaw, float turretPitch) {
        this.entityId = entityId;
        this.worldName = worldName;
        this.entityName = entityName;
        this.entityClassName = entityClassName;
        this.posX = posX;
        this.posY = posY;
        this.posZ = posZ;
        this.lastTickPosX = lastTickPosX;
        this.lastTickPosY = lastTickPosY;
        this.lastTickPosZ = lastTickPosZ;
        this.rotationYaw = rotationYaw;
        this.rotationPitch = rotationPitch;
        this.rotationRoll = rotationRoll;
        this.turretYaw = turretYaw;
        this.turretPitch = turretPitch;
        this.destroyed = destroyed;
        this.countermeasureFlags = countermeasureFlags;
        this.countermeasureUntilTick = countermeasureUntilTick;
        this.lastUpdateTime = System.currentTimeMillis();
    }

    public static MCH_EntityInfo createInfo(Entity e) {
        long worldTick = (e != null && e.worldObj != null) ? e.worldObj.getTotalWorldTime() : 0L;
        return createInfo(e, worldTick);
    }

    public static MCH_EntityInfo createInfo(Entity e, long worldTick) {
        String name = e.getCommandSenderName();
        byte countermeasureFlags = 0;
        long countermeasureUntilTick = -1L;
        if (e instanceof MCH_EntityAircraft) {
            MCH_EntityAircraft ac = (MCH_EntityAircraft) e;
            if (ac.getAcInfo() != null) {
                name = ac.getAcInfo().name;
            }
            if (ac.isChaffUsing()) {
                countermeasureFlags |= CM_FLAG_CHAFF;
            }
            if (ac.isECMJammerUsing()) {
                countermeasureFlags |= CM_FLAG_ECM;
            }
            if (ac.jammingTick > 0) {
                countermeasureFlags |= CM_FLAG_JAMMING;
            }
            if (countermeasureFlags != 0) {
                countermeasureUntilTick = worldTick + 8L;
            }
        }
        if (e instanceof MCH_EntityBaseBullet) {
            MCH_EntityBaseBullet b = (MCH_EntityBaseBullet) e;
            if (b.getInfo() != null) {
                name = b.getInfo().name;
            }
        }
        MCH_EntityAircraft aircraft = e instanceof MCH_EntityAircraft ? (MCH_EntityAircraft)e : null;
        float rotationRoll = aircraft != null ? aircraft.getRotRoll() : 0.0F;
        float turretYaw = aircraft != null ? aircraft.getLastRiderYaw() : e.rotationYaw;
        float turretPitch = aircraft != null ? aircraft.getLastRiderPitch() : e.rotationPitch;
        boolean destroyed = aircraft != null && aircraft.isDestroyed();
        return new MCH_EntityInfo(e.getEntityId(),
            e.worldObj.getWorldInfo().getWorldName(),
            name,
            e.getClass().getName(),
            e.posX, e.posY, e.posZ,
            e.lastTickPosX, e.lastTickPosY, e.lastTickPosZ,
            e.rotationYaw, e.rotationPitch,
            countermeasureFlags, countermeasureUntilTick, rotationRoll, destroyed, turretYaw, turretPitch
        );
    }

    public boolean isCountermeasureActive(long worldTick) {
        return this.countermeasureFlags != 0 && this.countermeasureUntilTick >= worldTick;
    }

    public boolean isElectronicCountermeasureActive(long worldTick) {
        int electronicFlags = CM_FLAG_ECM | CM_FLAG_JAMMING;
        return (this.countermeasureFlags & electronicFlags) != 0 && this.countermeasureUntilTick >= worldTick;
    }

    public double getDistanceToEntity(Entity e) {
        return Math.sqrt((e.posX - posX) * (e.posX - posX) + (e.posY - posY) * (e.posY - posY) + (e.posZ - posZ) * (e.posZ - posZ));
    }

    public double getDistanceSqToEntity(Entity e) {
        return (e.posX - posX) * (e.posX - posX) + (e.posY - posY) * (e.posY - posY) + (e.posZ - posZ) * (e.posZ - posZ);
    }

    public double getHorizonalDistanceSqToEntity(Entity e) {
        return (e.posX - posX) * (e.posX - posX) + (e.posZ - posZ) * (e.posZ - posZ);
    }
}
