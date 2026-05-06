package mcheli.plane;

import com.google.common.io.ByteArrayDataInput;
import mcheli.aircraft.MCH_PacketPlayerControlBase;

import java.io.DataOutputStream;
import java.io.IOException;

public class MCP_PlanePacketPlayerControl extends MCH_PacketPlayerControlBase {

    public byte switchVtol = -1;
    public float advancedPitchInput = 0.0F;
    public float advancedTargetDirX = 0.0F;
    public float advancedTargetDirY = 0.0F;
    public float advancedTargetDirZ = 1.0F;


    public int getMessageID() {
        return 536903696;
    }

    public void readData(ByteArrayDataInput data) {
        super.readData(data);

        try {
            this.switchVtol = data.readByte();
            this.advancedPitchInput = data.readFloat();
            this.advancedTargetDirX = data.readFloat();
            this.advancedTargetDirY = data.readFloat();
            this.advancedTargetDirZ = data.readFloat();
        } catch (Exception var3) {
            var3.printStackTrace();
        }
    }

    public void writeData(DataOutputStream dos) {
        super.writeData(dos);

        try {
            dos.writeByte(this.switchVtol);
            dos.writeFloat(this.advancedPitchInput);
            dos.writeFloat(this.advancedTargetDirX);
            dos.writeFloat(this.advancedTargetDirY);
            dos.writeFloat(this.advancedTargetDirZ);
        } catch (IOException var3) {
            var3.printStackTrace();
        }
    }
}
