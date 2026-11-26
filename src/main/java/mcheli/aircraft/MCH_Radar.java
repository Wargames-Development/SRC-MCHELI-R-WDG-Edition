package mcheli.aircraft;

import mcheli.MCH_Vector2;
import net.minecraft.block.Block;
import net.minecraft.command.IEntitySelector;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

import java.util.ArrayList;
import java.util.List;

public class MCH_Radar {
    private static final IEntitySelector LIVING_TARGETS = entity -> entity instanceof EntityLiving;

    private World worldObj;
    private ArrayList entityList = new ArrayList();
    private ArrayList enemyList = new ArrayList();


    public MCH_Radar(World world) {
        this.worldObj = world;
    }

    public ArrayList getEntityList() {
        return this.entityList;
    }

    public ArrayList getEnemyList() {
        return this.enemyList;
    }

    public void clear() {
        this.entityList.clear();
        this.enemyList.clear();
    }

    public void updateXZ(Entity centerEntity, int range) {
        if (this.worldObj.isRemote) {
            this.clear();
            List list = centerEntity.worldObj.getEntitiesWithinAABBExcludingEntity(centerEntity,
                    centerEntity.boundingBox.expand(range, range, range), LIVING_TARGETS);

            for (int i = 0; i < list.size(); ++i) {
                EntityLiving entity = (EntityLiving) list.get(i);
                double x = entity.posX - centerEntity.posX;
                double z = entity.posZ - centerEntity.posZ;
                if (x * x + z * z < (double) (range * range) && !hasOverheadCover(entity)) {
                    (entity instanceof EntityMob ? this.enemyList : this.entityList).add(new MCH_Vector2(x, z));
                }
            }
        }
    }

    private boolean hasOverheadCover(EntityLiving entity) {
        int x = (int) entity.posX;
        int z = (int) entity.posZ;
        int y = 1 + (int) entity.posY;
        if (y < 0) y = 1;
        if (y >= 200 || x < -30000000 || x >= 30000000 || z < -30000000 || z >= 30000000) return false;
        // The whole vertical probe stays in one chunk. Keep the legacy Y bounds and block-ID test.
        Chunk chunk = worldObj.getChunkFromChunkCoords(x >> 4, z >> 4);
        int covered = 0;
        for (; y < 200; ++y) {
            if (Block.getIdFromBlock(chunk.getBlock(x & 15, y, z & 15)) != 0 && ++covered == 5) return true;
        }
        return false;
    }
}
