package mcheli.mob;

import net.minecraft.nbt.NBTTagCompound;
import org.junit.Test;
import static org.junit.Assert.*;

public class MCH_GunnerPersistenceTest {
    @Test public void savesTeamOwnerAndProfileWithoutSavingTransientTarget() {
        MCH_EntityGunner original = new MCH_EntityGunner(null);
        original.ownerUUID = "11111111-1111-1111-1111-111111111111";
        original.setTeamName("wgc:22222222-2222-2222-2222-222222222222");
        original.setTargetType(MCH_EntityGunner.TARGET_AA_AMMO);
        original.applyProfile(new MCH_GunnerInfo("gunner_aa_default"));
        NBTTagCompound saved = new NBTTagCompound();
        original.writeEntityToNBT(saved);
        MCH_EntityGunner restored = new MCH_EntityGunner(null);
        restored.readEntityFromNBT(saved);
        assertEquals(original.ownerUUID, restored.ownerUUID);
        assertEquals(original.getTeamName(), restored.getTeamName());
        assertEquals(MCH_EntityGunner.TARGET_AA_AMMO, restored.getTargetType());
        assertEquals("gunner_aa_default", saved.getString("GunnerProfile"));
        assertNull(restored.targetEntity);
    }

    @Test public void legacyGunnerNbtRetainsIdentityAndDoesNotConvertToAa() {
        NBTTagCompound saved = new NBTTagCompound();
        saved.setString("TeamName", "wge_t1");
        saved.setString("OwnerUUID", "legacy_owner");
        saved.setInteger("TargetType", MCH_EntityGunner.TARGET_PLAYER);
        MCH_EntityGunner restored = new MCH_EntityGunner(null);
        restored.readEntityFromNBT(saved);
        assertEquals("wge_t1", restored.getTeamName());
        assertEquals("legacy_owner", restored.ownerUUID);
        assertEquals(MCH_EntityGunner.TARGET_PLAYER, restored.getTargetType());
    }
}
