package mcheli.tank;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import mcheli.aircraft.MCH_PacketPlayerControlBase;

import java.io.DataOutputStream;
import java.io.IOException;

public class MCH_TankPacketPlayerControl extends MCH_PacketPlayerControlBase {

    public byte switchVtol = -1;
    public boolean valid;


    public int getMessageID() {
        return 537919504;
    }

    public void readData(ByteArrayDataInput data) {
        this.valid = false;
        try {
            // The existing payload is seven bytes. Reject truncation before applying any intent.
            byte[] payload = new byte[7];
            data.readFully(payload);
            ByteArrayDataInput input = ByteStreams.newDataInput(payload);
            super.readData(input);
            this.switchVtol = input.readByte();
            this.valid = true;
        } catch (Exception var3) {
            this.valid = false;
        }

    }

    public void writeData(DataOutputStream dos) {
        super.writeData(dos);

        try {
            dos.writeByte(this.switchVtol);
        } catch (IOException var3) {
            var3.printStackTrace();
        }

    }
}
