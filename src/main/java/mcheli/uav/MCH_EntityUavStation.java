package mcheli.uav;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import mcheli.*;
import mcheli.aircraft.MCH_EntityAircraft;
import mcheli.helicopter.MCH_HeliInfo;
import mcheli.helicopter.MCH_HeliInfoManager;
import mcheli.helicopter.MCH_ItemHeli;
import mcheli.multiplay.MCH_Multiplay;
import mcheli.plane.MCP_ItemPlane;
import mcheli.plane.MCP_PlaneInfo;
import mcheli.plane.MCP_PlaneInfoManager;
import mcheli.tank.MCH_ItemTank;
import mcheli.tank.MCH_TankInfo;
import mcheli.tank.MCH_TankInfoManager;
import mcheli.wrapper.W_Entity;
import mcheli.wrapper.W_EntityContainer;
import mcheli.wrapper.W_EntityPlayer;
import mcheli.wrapper.W_WorldFunc;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

import java.util.List;

public class MCH_EntityUavStation extends W_EntityContainer {

    protected static final int DATAWT_ID_KIND = 27;
    protected static final int DATAWT_ID_LAST_AC = 28;
    protected static final int DATAWT_ID_UAV_X = 29;
    protected static final int DATAWT_ID_UAV_Y = 30;
    protected static final int DATAWT_ID_UAV_Z = 31;
    public boolean isRequestedSyncStatus;
    public int posUavX;
    public int posUavY;
    public int posUavZ;
    public float rotCover;
    public float prevRotCover;
    protected Entity lastRiddenByEntity;
    @SideOnly(Side.CLIENT)
    protected double velocityX;
    @SideOnly(Side.CLIENT)
    protected double velocityY;
    @SideOnly(Side.CLIENT)
    protected double velocityZ;
    protected int aircraftPosRotInc;
    protected double aircraftX;
    protected double aircraftY;
    protected double aircraftZ;
    protected double aircraftYaw;
    protected double aircraftPitch;
    private MCH_EntityAircraft controlAircraft;
    private MCH_EntityAircraft lastControlAircraft;
    private String loadedLastControlAircraftGuid;


    public MCH_EntityUavStation(World world) {
        super(world);
        super.dropContentsWhenDead = false;
        super.preventEntitySpawning = true;
        this.setSize(1.0F, 0.7F);
        super.yOffset = super.height / 2.0F;
        super.motionX = 0.0D;
        super.motionY = 0.0D;
        super.motionZ = 0.0D;
        super.ignoreFrustumCheck = true;
        this.lastRiddenByEntity = null;
        this.aircraftPosRotInc = 0;
        this.aircraftX = 0.0D;
        this.aircraftY = 0.0D;
        this.aircraftZ = 0.0D;
        this.aircraftYaw = 0.0D;
        this.aircraftPitch = 0.0D;
        this.posUavX = 0;
        this.posUavY = 0;
        this.posUavZ = 0;
        this.rotCover = 0.0F;
        this.prevRotCover = 0.0F;
        this.setControlAircract((MCH_EntityAircraft) null);
        this.setLastControlAircraft((MCH_EntityAircraft) null);
        this.loadedLastControlAircraftGuid = "";
    }

    protected void entityInit() {
        super.entityInit();
        this.getDataWatcher().addObject(27, (byte) 0);
        this.getDataWatcher().addObject(28, 0);
        this.getDataWatcher().addObject(29, 0);
        this.getDataWatcher().addObject(30, 0);
        this.getDataWatcher().addObject(31, 0);
        this.setOpen(true);
    }

    public int getStatus() {
        return this.getDataWatcher().getWatchableObjectByte(27);
    }

    public void setStatus(int n) {
        if (!super.worldObj.isRemote) {
            MCH_Lib.DbgLog(super.worldObj, "MCH_EntityUavStation.setStatus(%d)", n);
            this.getDataWatcher().updateObject(27, (byte) n);
        }

    }

    public int getKind() {
        return 127 & this.getStatus();
    }

    public void setKind(int n) {
        this.setStatus(this.getStatus() & 128 | n);
    }

    public boolean isOpen() {
        return (this.getStatus() & 128) != 0;
    }

    public void setOpen(boolean b) {
        this.setStatus((b ? 128 : 0) | this.getStatus() & 127);
    }

    public MCH_EntityAircraft getControlAircract() {
        return this.controlAircraft;
    }

    public void setControlAircract(MCH_EntityAircraft ac) {
        this.controlAircraft = ac;
        if (ac != null && !ac.isDead) {
            this.setLastControlAircraft(ac);
        }

    }

    public void setUavPosition(int x, int y, int z) {
        if (!super.worldObj.isRemote) {
            this.posUavX = x;
            this.posUavY = y;
            this.posUavZ = z;
            this.getDataWatcher().updateObject(29, x);
            this.getDataWatcher().updateObject(30, y);
            this.getDataWatcher().updateObject(31, z);
        }

    }

    public void updateUavPosition() {
        this.posUavX = this.getDataWatcher().getWatchableObjectInt(29);
        this.posUavY = this.getDataWatcher().getWatchableObjectInt(30);
        this.posUavZ = this.getDataWatcher().getWatchableObjectInt(31);
    }

    protected void writeEntityToNBT(NBTTagCompound nbt) {
        super.writeEntityToNBT(nbt);
        nbt.setInteger("UavStatus", this.getStatus());
        nbt.setInteger("PosUavX", this.posUavX);
        nbt.setInteger("PosUavY", this.posUavY);
        nbt.setInteger("PosUavZ", this.posUavZ);
        String s = "";
        if (this.getLastControlAircraft() != null && !this.getLastControlAircraft().isDead) {
            s = this.getLastControlAircraft().getCommonUniqueId();
        }

        if (s.isEmpty()) {
            s = this.loadedLastControlAircraftGuid;
        }

        nbt.setString("LastCtrlAc", s);
    }

    protected void readEntityFromNBT(NBTTagCompound nbt) {
        super.readEntityFromNBT(nbt);
        this.setUavPosition(nbt.getInteger("PosUavX"), nbt.getInteger("PosUavY"), nbt.getInteger("PosUavZ"));
        if (nbt.hasKey("UavStatus")) {
            this.setStatus(nbt.getInteger("UavStatus"));
        } else {
            this.setKind(1);
        }

        this.loadedLastControlAircraftGuid = nbt.getString("LastCtrlAc");
    }

    public void initUavPostion() {
        int rt = (int) (MCH_Lib.getRotate360((double) (super.rotationYaw + 45.0F)) / 90.0D);
        boolean D = true;
        this.posUavX = rt != 0 && rt != 3 ? -12 : 12;
        this.posUavZ = rt != 0 && rt != 1 ? -12 : 12;
        this.posUavY = 2;
        this.setUavPosition(this.posUavX, this.posUavY, this.posUavZ);
    }

    public void setDead() {
        super.setDead();
    }

    public boolean attackEntityFrom(DamageSource damageSource, float damage) {
        if (this.isEntityInvulnerable()) {
            return false;
        } else if (super.isDead) {
            return true;
        } else if (super.worldObj.isRemote) {
            return true;
        } else {
            String dmt = damageSource.getDamageType();
            MCH_Config var10000 = MCH_MOD.config;
            damage = MCH_Config.applyDamageByExternal(this, damageSource, damage);
            if (!MCH_Multiplay.canAttackEntity(damageSource, this)) {
                return false;
            } else {
                boolean isCreative = false;
                Entity entity = damageSource.getEntity();
                boolean isDamageSourcePlayer = false;
                if (entity instanceof EntityPlayer) {
                    isCreative = ((EntityPlayer) entity).capabilities.isCreativeMode;
                    if (dmt.compareTo("player") == 0) {
                        isDamageSourcePlayer = true;
                    }

                    W_WorldFunc.MOD_playSoundAtEntity(this, "hit", 1.0F, 1.0F);
                } else {
                    W_WorldFunc.MOD_playSoundAtEntity(this, "helidmg", 1.0F, 0.9F + super.rand.nextFloat() * 0.1F);
                }

                this.setBeenAttacked();
                if (damage > 0.0F) {
                    if (super.riddenByEntity != null) {
                        super.riddenByEntity.mountEntity(this);
                    }

                    super.dropContentsWhenDead = true;
                    this.setDead();
                    if (!isDamageSourcePlayer) {
                        MCH_ExplosionParam param = MCH_ExplosionParam.builder()
                            .exploder(null)
                            .player(super.riddenByEntity instanceof EntityPlayer ?
                                (EntityPlayer) super.riddenByEntity : null)
                            .x(super.posX).y(super.posY).z(super.posZ)
                            .size(1.0F)
                            .sizeBlock(0.0F)
                            .isPlaySound(true)
                            .isSmoking(true)
                            .isFlaming(false)
                            .isDestroyBlock(false)
                            .countSetFireEntity(0)
                            .isInWater(false)
                            .build();
                        MCH_Explosion.newExplosion(super.worldObj, param);
                    }

                    if (!isCreative) {
                        int kind = this.getKind();
                        if (kind > 0) {
                            this.dropItemWithOffset(MCH_MOD.itemUavStation[kind - 1], 1, 0.0F);
                        }
                    }
                }

                return true;
            }
        }
    }

    protected boolean canTriggerWalking() {
        return false;
    }

    public AxisAlignedBB getCollisionBox(Entity par1Entity) {
        return par1Entity.boundingBox;
    }

    public AxisAlignedBB getBoundingBox() {
        return super.boundingBox;
    }

    public boolean canBePushed() {
        return false;
    }

    public double getMountedYOffset() {
        if (this.getKind() == 2 && super.riddenByEntity != null) {
            double px = -Math.sin((double) super.rotationYaw * 3.141592653589793D / 180.0D) * 0.9D;
            double pz = Math.cos((double) super.rotationYaw * 3.141592653589793D / 180.0D) * 0.9D;
            int x = (int) (super.posX + px);
            int y = (int) (super.posY - 0.5D);
            int z = (int) (super.posZ + pz);
            Block block = super.worldObj.getBlock(x, y, z);
            return block.isOpaqueCube() ? -0.4D : -0.9D;
        } else {
            return 0.35D;
        }
    }

    @SideOnly(Side.CLIENT)
    public float getShadowSize() {
        return 0.0F;
    }

    @Override
    public boolean canBeCollidedWith() {
        return !super.isDead;
    }

    public void applyEntityCollision(Entity par1Entity) {
    }

    public void addVelocity(double par1, double par3, double par5) {
    }

    @SideOnly(Side.CLIENT)
    public void setVelocity(double par1, double par3, double par5) {
        this.velocityX = super.motionX = par1;
        this.velocityY = super.motionY = par3;
        this.velocityZ = super.motionZ = par5;
    }

    public void onUpdate() {
        super.onUpdate();
        this.prevRotCover = this.rotCover;
        if (this.isOpen()) {
            if (this.rotCover < 1.0F) {
                this.rotCover += 0.1F;
            } else {
                this.rotCover = 1.0F;
            }
        } else if (this.rotCover > 0.0F) {
            this.rotCover -= 0.1F;
        } else {
            this.rotCover = 0.0F;
        }

        if (super.riddenByEntity == null) {
            if (this.lastRiddenByEntity != null) {
                this.unmountEntity(true);
            }

            this.setControlAircract(null);
        }

        int uavStationKind = this.getKind();
        if (super.ticksExisted < 30 && uavStationKind == 2) {
            ;
        }

        if (super.worldObj.isRemote && !this.isRequestedSyncStatus) {
            this.isRequestedSyncStatus = true;
        }

        super.prevPosX = super.posX;
        super.prevPosY = super.posY;
        super.prevPosZ = super.posZ;
        if (this.getControlAircract() != null && this.getControlAircract().isDead) {
            this.setControlAircract((MCH_EntityAircraft) null);
        }

        if (this.getLastControlAircraft() != null && this.getLastControlAircraft().isDead) {
            this.setLastControlAircraft((MCH_EntityAircraft) null);
        }

        if (super.worldObj.isRemote) {
            this.onUpdate_Client();
        } else {
            this.onUpdate_Server();
        }

        this.lastRiddenByEntity = super.riddenByEntity;
    }

    public MCH_EntityAircraft getLastControlAircraft() {
        return this.lastControlAircraft;
    }

    public void setLastControlAircraft(MCH_EntityAircraft ac) {
        MCH_Lib.DbgLog(super.worldObj, "MCH_EntityUavStation.setLastControlAircraft:" + ac);
        this.lastControlAircraft = ac;
    }

    public MCH_EntityAircraft getAndSearchLastControlAircraft() {
        if (this.getLastControlAircraft() == null) {
            int id = this.getLastControlAircraftEntityId();
            if (id > 0) {
                Entity entity = super.worldObj.getEntityByID(id);
                if (entity instanceof MCH_EntityAircraft) {
                    MCH_EntityAircraft ac = (MCH_EntityAircraft) entity;
                    if (ac.isUAV()) {
                        this.setLastControlAircraft(ac);
                    }
                }
            }
        }

        return this.getLastControlAircraft();
    }

    public Integer getLastControlAircraftEntityId() {
        return Integer.valueOf(this.getDataWatcher().getWatchableObjectInt(28));
    }

    public void setLastControlAircraftEntityId(int s) {
        if (!super.worldObj.isRemote) {
            this.getDataWatcher().updateObject(28, s);
        }

    }

    public void searchLastControlAircraft() {
        if (!this.loadedLastControlAircraftGuid.isEmpty()) {
            List list = super.worldObj.getEntitiesWithinAABB(MCH_EntityAircraft.class, this.getBoundingBox().expand(500.0D, 500.0D, 500.0D));
            if (list != null) {
                for (Object o : list) {
                    MCH_EntityAircraft ac = (MCH_EntityAircraft) o;
                    if (ac.getCommonUniqueId().equals(this.loadedLastControlAircraftGuid)) {
                        String n = ac.getAcInfo() != null ? ac.getAcInfo().displayName : "no info : " + ac;
                        MCH_Lib.DbgLog(super.worldObj, "MCH_EntityUavStation.searchLastControlAircraft:found" + n);
                        this.setLastControlAircraft(ac);
                        this.setLastControlAircraftEntityId(W_Entity.getEntityId(ac));
                        this.loadedLastControlAircraftGuid = "";
                        return;
                    }
                }

            }
        }
    }

    protected void onUpdate_Client() {
        if (this.aircraftPosRotInc > 0) {
            double rpinc = this.aircraftPosRotInc;
            double yaw = MathHelper.wrapAngleTo180_double(this.aircraftYaw - (double) super.rotationYaw);
            super.rotationYaw = (float) ((double) super.rotationYaw + yaw / rpinc);
            super.rotationPitch = (float) ((double) super.rotationPitch + (this.aircraftPitch - (double) super.rotationPitch) / rpinc);
            this.setPosition(super.posX + (this.aircraftX - super.posX) / rpinc, super.posY + (this.aircraftY - super.posY) / rpinc, super.posZ + (this.aircraftZ - super.posZ) / rpinc);
            this.setRotation(super.rotationYaw, super.rotationPitch);
            --this.aircraftPosRotInc;
        } else {
            this.setPosition(super.posX + super.motionX, super.posY + super.motionY, super.posZ + super.motionZ);
            super.motionY *= 0.96D;
            super.motionX = 0.0D;
            super.motionZ = 0.0D;
        }

        this.updateUavPosition();
    }

    private void onUpdate_Server() {
        super.motionY -= 0.03D;
        this.moveEntity(0.0D, super.motionY, 0.0D);
        super.motionY *= 0.96D;
        super.motionX = 0.0D;
        super.motionZ = 0.0D;
        this.setRotation(super.rotationYaw, super.rotationPitch);
        if (super.riddenByEntity != null) {
            if (super.riddenByEntity.isDead) {
                this.unmountEntity(true);
                super.riddenByEntity = null;
            } else {
                ItemStack item = this.getStackInSlot(0);
                if (item != null && item.stackSize > 0) {
                    this.handleItem(super.riddenByEntity, item);
                    if (item.stackSize == 0) {
                        this.setInventorySlotContents(0, (ItemStack) null);
                    }
                }
            }
        }

        if (this.getLastControlAircraft() == null && super.ticksExisted % 40 == 0) {
            this.searchLastControlAircraft();
        }

    }

    public void setPositionAndRotation2(double par1, double par3, double par5, float par7, float par8, int par9) {
        this.aircraftPosRotInc = par9 + 8;
        this.aircraftX = par1;
        this.aircraftY = par3;
        this.aircraftZ = par5;
        this.aircraftYaw = (double) par7;
        this.aircraftPitch = (double) par8;
        super.motionX = this.velocityX;
        super.motionY = this.velocityY;
        super.motionZ = this.velocityZ;
    }

    public void updateRiderPosition() {
        if (super.riddenByEntity != null) {
            double x = -Math.sin((double) super.rotationYaw * 3.141592653589793D / 180.0D) * 0.9D;
            double z = Math.cos((double) super.rotationYaw * 3.141592653589793D / 180.0D) * 0.9D;
            super.riddenByEntity.setPosition(super.posX + x, super.posY + this.getMountedYOffset() + super.riddenByEntity.getYOffset(), super.posZ + z);
        }

    }

    public void controlLastAircraft(Entity user) {
        if (this.getLastControlAircraft() != null && !this.getLastControlAircraft().isDead) {
            this.getLastControlAircraft().setUavStation(this);
            this.setControlAircract(this.getLastControlAircraft());
            W_EntityPlayer.closeScreen(user);
        }

    }

    public void handleItem(Entity user, ItemStack itemStack) {
        if (user != null && !user.isDead && itemStack != null && itemStack.stackSize == 1) {
            if (!super.worldObj.isRemote) {
                MCH_EntityAircraft ac = null;
                double x = super.posX + (double) this.posUavX;
                double y = super.posY + (double) this.posUavY;
                double z = super.posZ + (double) this.posUavZ;
                if (y <= 1.0D) {
                    y = 2.0D;
                }

                Item item = itemStack.getItem();
                if (item instanceof MCP_ItemPlane) {
                    MCP_PlaneInfo hi = MCP_PlaneInfoManager.getFromItem(item);
                    if (hi != null && hi.isUAV) {
                        if (!hi.isSmallUAV && this.getKind() == 2) {
                            ac = null;
                        } else {
                            ac = ((MCP_ItemPlane) item).createAircraft(super.worldObj, x, y, z, itemStack);
                        }
                    }
                }

                if (item instanceof MCH_ItemHeli) {
                    MCH_HeliInfo hi1 = MCH_HeliInfoManager.getFromItem(item);
                    if (hi1 != null && hi1.isUAV) {
                        if (!hi1.isSmallUAV && this.getKind() == 2) {
                            ac = null;
                        } else {
                            ac = ((MCH_ItemHeli) item).createAircraft(super.worldObj, x, y, z, itemStack);
                        }
                    }
                }

                if (item instanceof MCH_ItemTank) {
                    MCH_TankInfo hi2 = MCH_TankInfoManager.getFromItem(item);
                    if (hi2 != null && hi2.isUAV) {
                        if (!hi2.isSmallUAV && this.getKind() == 2) {
                            ac = null;
                        } else {
                            ac = ((MCH_ItemTank) item).createAircraft(super.worldObj, x, y, z, itemStack);
                        }
                    }
                }

                if (ac != null) {
                    ac.rotationYaw = super.rotationYaw - 180.0F;
                    ac.prevRotationYaw = ac.rotationYaw;
                    user.rotationYaw = super.rotationYaw - 180.0F;
                    if (super.worldObj.getCollidingBoundingBoxes(ac, ac.boundingBox.expand(-0.1D, -0.1D, -0.1D)).isEmpty()) {
                        --itemStack.stackSize;
                        MCH_Lib.DbgLog(super.worldObj, "Create UAV: %s : %s", item.getUnlocalizedName(), item);
                        user.rotationYaw = super.rotationYaw - 180.0F;
                        if (!ac.isTargetDrone()) {
                            ac.setUavStation(this);
                            this.setControlAircract(ac);
                        }

                        super.worldObj.spawnEntityInWorld(ac);
                        if (!ac.isTargetDrone()) {
                            ac.setFuel((int) ((float) ac.getMaxFuel() * 0.05F));
                            W_EntityPlayer.closeScreen(user);
                        } else {
                            ac.setFuel(ac.getMaxFuel());
                        }
                    } else {
                        ac.setDead();
                    }

                }
            }
        }
    }

    public void _setInventorySlotContents(int par1, ItemStack itemStack) {
        super.setInventorySlotContents(par1, itemStack);
    }

    public boolean interactFirst(EntityPlayer player) {
        int kind = this.getKind();
        if (kind <= 0) {
            return false;
        } else if (super.riddenByEntity != null) {
            return false;
        } else {
            if (kind == 2) {
                if (player.isSneaking()) {
                    this.setOpen(!this.isOpen());
                    return false;
                }

                if (!this.isOpen()) {
                    return false;
                }
            }

            super.riddenByEntity = null;
            this.lastRiddenByEntity = null;
            if (!super.worldObj.isRemote) {
                player.mountEntity(this);
                player.openGui(MCH_MOD.instance, 0, player.worldObj, (int) super.posX, (int) super.posY, (int) super.posZ);
            }

            return true;
        }
    }

    public int getSizeInventory() {
        return 1;
    }

    public int getInventoryStackLimit() {
        return 1;
    }

    public void unmountEntity(boolean unmountAllEntity) {
        Entity rByEntity = null;
        if (super.riddenByEntity != null) {
            if (!super.worldObj.isRemote) {
                rByEntity = super.riddenByEntity;
                super.riddenByEntity.mountEntity(null);
            }
        } else if (this.lastRiddenByEntity != null) {
            rByEntity = this.lastRiddenByEntity;
        }

        if (this.getControlAircract() != null) {
            this.getControlAircract().setUavStation(null);
        }

        this.setControlAircract(null);
        if (super.worldObj.isRemote) {
            W_EntityPlayer.closeScreen(rByEntity);
        }

        super.riddenByEntity = null;
        this.lastRiddenByEntity = null;
    }
}
