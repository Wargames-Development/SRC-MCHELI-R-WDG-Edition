package mcheli.network.packets;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import mcheli.aircraft.MCH_EntityAircraft;
import mcheli.network.PacketBase;
import mcheli.weapon.MCH_WeaponBase;
import mcheli.weapon.MCH_WeaponSet;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;

public class PacketAirburstDistReset extends PacketBase {

    public int acId;
    public int dist;


    public PacketAirburstDistReset() {
    }

    public PacketAirburstDistReset(int acId, int dist) {
        this.acId = acId;
        this.dist = dist;
    }

    @Override
    public void encodeInto(ChannelHandlerContext ctx, ByteBuf data) {
        data.writeInt(acId);
        data.writeInt(dist);
    }

    @Override
    public void decodeInto(ChannelHandlerContext ctx, ByteBuf data) {
        acId = data.readInt();
        dist = data.readInt();
    }

    @Override
    public void handleServerSide(EntityPlayerMP playerEntity) {
        if (playerEntity == null || playerEntity.isDead || playerEntity.worldObj == null
            || (dist != 0 && (dist <= 5 || dist >= 3000))) return;
        Entity e = playerEntity.worldObj.getEntityByID(acId);
        if (e instanceof MCH_EntityAircraft) {
            MCH_EntityAircraft aircraft = (MCH_EntityAircraft) e;
            if (aircraft.isDead || MCH_EntityAircraft.getAircraft_RiddenOrControl(playerEntity) != aircraft) return;
            if (aircraft.getCurrentWeapon(playerEntity) != null) {
                MCH_WeaponSet ws = aircraft.getCurrentWeapon(playerEntity);
                if (ws.getCurrentWeapon() != null) {
                    MCH_WeaponBase wb = ws.getCurrentWeapon();
                    if (wb.getInfo() != null && (wb.getInfo().canAirburst || wb.getInfo().ahead)) {
                        wb.setAirburstDist(dist);
                    }
                }
            }
        }
    }

    @Override
    public void handleClientSide(EntityPlayer clientPlayer) {
    }
}
