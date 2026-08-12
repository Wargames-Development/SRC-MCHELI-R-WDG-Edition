package mcheli.aircraft;

import com.google.common.io.ByteArrayDataInput;
import mcheli.MCH_Packet;
import mcheli.weapon.MCH_EntityTvMissile;
import mcheli.wrapper.W_Entity;
import mcheli.wrapper.W_Network;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MathHelper;

import java.io.DataOutputStream;
import java.io.IOException;

public class MCH_PacketTVMissileGuidance extends MCH_Packet {

    private static final int MESSAGE_ID = 536875064;
    public int missileEntityId = -1;
    public float yaw;
    public float pitch;

    public static void send(int missileEntityId, float yaw, float pitch) {
        MCH_PacketTVMissileGuidance packet = new MCH_PacketTVMissileGuidance();
        packet.missileEntityId = missileEntityId;
        packet.yaw = yaw;
        packet.pitch = pitch;
        W_Network.sendToServer(packet);
    }

    public static void handleServer(EntityPlayer player, ByteArrayDataInput data) {
        MCH_PacketTVMissileGuidance packet = new MCH_PacketTVMissileGuidance();
        packet.readData(data);
        if (player == null || player.worldObj == null || player.worldObj.isRemote
            || Float.isNaN(packet.yaw) || Float.isInfinite(packet.yaw)
            || Float.isNaN(packet.pitch) || Float.isInfinite(packet.pitch)) {
            return;
        }
        Entity entity = player.worldObj.getEntityByID(packet.missileEntityId);
        if (!(entity instanceof MCH_EntityTvMissile)) {
            return;
        }
        MCH_EntityTvMissile missile = (MCH_EntityTvMissile)entity;
        MCH_EntityAircraft aircraft = MCH_EntityAircraft.getAircraft_RiddenOrControl(player);
        if (!missile.isTVMissile || missile.isDead || aircraft == null || aircraft.isDestroyed()
            || !W_Entity.isEqual(missile.shootingEntity, player)
            || !W_Entity.isEqual(missile.shootingAircraft, aircraft)
            || !W_Entity.isEqual(aircraft.getTVMissile(), missile)) {
            return;
        }
        float yaw = MathHelper.wrapAngleTo180_float(packet.yaw);
        float pitch = MathHelper.clamp_float(packet.pitch, -89.9F, 89.9F);
        missile.setTVGuidanceAngles(yaw, pitch);
    }

    @Override
    public int getMessageID() {
        return MESSAGE_ID;
    }

    @Override
    public void readData(ByteArrayDataInput data) {
        try {
            this.missileEntityId = data.readInt();
            this.yaw = data.readFloat();
            this.pitch = data.readFloat();
        } catch (Exception ignored) {
            this.missileEntityId = -1;
        }
    }

    @Override
    public void writeData(DataOutputStream dos) {
        try {
            dos.writeInt(this.missileEntityId);
            dos.writeFloat(this.yaw);
            dos.writeFloat(this.pitch);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
