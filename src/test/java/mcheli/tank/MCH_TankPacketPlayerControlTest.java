package mcheli.tank;

import com.google.common.io.ByteStreams;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.util.Arrays;

import static org.junit.Assert.*;

public class MCH_TankPacketPlayerControlTest {
    @Test
    public void drivingIntentRetainsExistingSevenBytePayload() {
        MCH_TankPacketPlayerControl source = new MCH_TankPacketPlayerControl();
        source.throttleDown = source.moveLeft = source.useBrake = true;
        source.switchVtol = 2;
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        source.writeData(new DataOutputStream(bytes));
        byte[] payload = bytes.toByteArray();
        assertEquals(7, payload.length);
        assertEquals(537919504, source.getMessageID());
        assertArrayEquals(new byte[]{1, 12, 0, -1, -1, 0, 2}, payload);
        MCH_TankPacketPlayerControl decoded = new MCH_TankPacketPlayerControl();
        decoded.readData(ByteStreams.newDataInput(payload));
        assertTrue(decoded.valid);
        assertTrue(decoded.throttleDown && decoded.moveLeft && decoded.useBrake);
        assertFalse(decoded.throttleUp || decoded.moveRight);
        assertEquals(2, decoded.switchVtol);
        for (int length = 0; length < payload.length; ++length) {
            decoded.readData(ByteStreams.newDataInput(Arrays.copyOf(payload, length)));
            assertFalse(decoded.valid);
        }
    }
}
