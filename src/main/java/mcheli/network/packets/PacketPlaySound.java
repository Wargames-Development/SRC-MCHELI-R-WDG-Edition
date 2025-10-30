package mcheli.network.packets;

import cpw.mods.fml.client.FMLClientHandler;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import mcheli.MCH_MOD;
import mcheli.network.PacketBase;
import mcheli.wrapper.W_MOD;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ResourceLocation;

import java.util.Random;

public class PacketPlaySound extends PacketBase {
    public static Random rand = new Random();
    public float posX, posY, posZ;
    public String sound;
    public boolean distort, silenced;

    public PacketPlaySound() {
    }

    public PacketPlaySound(double x, double y, double z, String s) {
        this(x, y, z, s, false);
    }

    public PacketPlaySound(double x, double y, double z, String s, boolean distort) {
        this(x, y, z, s, distort, false);
    }

    public PacketPlaySound(double x, double y, double z, String s, boolean distort, boolean silenced) {
        posX = (float) x;
        posY = (float) y;
        posZ = (float) z;
        sound = s;
        this.distort = distort;
        this.silenced = silenced;
    }

    public static void sendSoundPacket(double x, double y, double z, double range, int dimension, String s, boolean distort) {
        sendSoundPacket(x, y, z, range, dimension, s, distort, false);
    }

    public static void sendSoundPacket(double x, double y, double z, double range, int dimension, String s, boolean distort, boolean silenced) {
        if (s != null && !s.isEmpty()) {
            MCH_MOD.getPacketHandler().sendToAllAround(new PacketPlaySound(x, y, z, s, distort, silenced), x, y, z, (float) range, dimension);
        }
    }

    @Override
    public void encodeInto(ChannelHandlerContext ctx, ByteBuf data) {
        data.writeFloat(posX);
        data.writeFloat(posY);
        data.writeFloat(posZ);
        writeUTF(data, sound);
        data.writeBoolean(distort);
        data.writeBoolean(silenced);
    }

    @Override
    public void decodeInto(ChannelHandlerContext ctx, ByteBuf data) {
        posX = data.readFloat();
        posY = data.readFloat();
        posZ = data.readFloat();
        sound = readUTF(data);
        distort = data.readBoolean();
        silenced = data.readBoolean();
    }

    @Override
    public void handleServerSide(EntityPlayerMP playerEntity) {
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void handleClientSide(EntityPlayer clientPlayer) {
        FMLClientHandler.instance().getClient().getSoundHandler().playSound(new PositionedSoundRecord(getSound(sound), silenced ? 50F : 100F, (distort ? 1.0F / (rand.nextFloat() * 0.4F + 0.8F) : 1.0F) * (silenced ? 2F : 1F), posX, posY, posZ));
    }

    public ResourceLocation getSound(String sound) {
        return new ResourceLocation(W_MOD.DOMAIN, sound);
    }
}
