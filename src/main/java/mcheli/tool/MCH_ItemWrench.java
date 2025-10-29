package mcheli.tool;

import com.google.common.collect.Multimap;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import java.util.List;
import java.util.Random;

import mcheli.MCH_Config;
import mcheli.MCH_MOD;
import mcheli.MCH_RayTracer;
import mcheli.aircraft.MCH_EntityAircraft;
import mcheli.aircraft.MCH_EntitySeat;
import mcheli.wrapper.W_Item;
import mcheli.wrapper.W_WorldFunc;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.material.MaterialLogic;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumAction;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Item.ToolMaterial;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

public class MCH_ItemWrench extends W_Item {

   private float damageVsEntity;
   private final ToolMaterial toolMaterial;
   private static Random rand = new Random();


   public MCH_ItemWrench(int itemId, ToolMaterial material) {
      super(itemId);
      this.toolMaterial = material;
      super.maxStackSize = 1;
      this.setMaxDamage(material.getMaxUses());
      this.damageVsEntity = 4.0F + material.getDamageVsEntity();
   }

   public boolean func_150897_b(Block b) {
      Material material = b.getMaterial();
      return material == Material.iron || material instanceof MaterialLogic;
   }

   public float func_150893_a(ItemStack itemStack, Block block) {
      Material material = block.getMaterial();
      return material == Material.iron?20.5F:(material instanceof MaterialLogic?5.5F:2.0F);
   }

   public static int getUseAnimCount(ItemStack stack) {
      return getAnimCount(stack, "MCH_WrenchAnim");
   }

   public static void setUseAnimCount(ItemStack stack, int n) {
      setAnimCount(stack, "MCH_WrenchAnim", n);
   }

   public static int getAnimCount(ItemStack stack, String name) {
      if(!stack.hasTagCompound()) {
         stack.stackTagCompound = new NBTTagCompound();
      }

      if(stack.stackTagCompound.hasKey(name)) {
         return stack.stackTagCompound.getInteger(name);
      } else {
         stack.stackTagCompound.setInteger(name, 0);
         return 0;
      }
   }

   public static void setAnimCount(ItemStack stack, String name, int n) {
      if(!stack.hasTagCompound()) {
         stack.stackTagCompound = new NBTTagCompound();
      }

      stack.stackTagCompound.setInteger(name, n);
   }

   public boolean hitEntity(ItemStack itemStack, EntityLivingBase entity, EntityLivingBase player) {

      if (MCH_Config.wrenchdropitem.prmBool) {
         if (!player.worldObj.isRemote) {
            if (rand.nextInt(40) == 0) {
               entity.entityDropItem(new ItemStack(W_Item.getItemByName("iron_ingot"), 1, 0), 0.0F);
            } else if (rand.nextInt(20) == 0) {
               entity.entityDropItem(new ItemStack(W_Item.getItemByName("gunpowder"), 1, 0), 0.0F);
            }
         }

         itemStack.damageItem(2, player);
         return true;
      } else {
         return false;
      }
   }

   public void onPlayerStoppedUsing(ItemStack stack, World world, EntityPlayer player, int count) {
      setUseAnimCount(stack, 0);
   }

   public void onUsingTick(ItemStack stack, EntityPlayer player, int count) {
      MCH_EntityAircraft ac;
      if(player.worldObj.isRemote) {
         ac = this.getMouseOverAircraft(player);
         if(ac != null) {
            int cnt = getUseAnimCount(stack);
            if(cnt <= 0) {
               cnt = 16;
            } else {
               --cnt;
            }

            setUseAnimCount(stack, cnt);
         }
      }

      if(!player.worldObj.isRemote && count < this.getMaxItemUseDuration(stack) && count % 20 == 0) {
         ac = this.getMouseOverAircraft(player);
         if(ac != null && ac.getHP() > 0 && ac.repair(10)) {
            stack.damageItem(1, player);
            W_WorldFunc.MOD_playSoundEffect(player.worldObj, (double)((int)ac.posX), (double)((int)ac.posY), (double)((int)ac.posZ), "wrench", 1.0F, 0.9F + rand.nextFloat() * 0.2F);
         }
      }

   }

   public void onUpdate(ItemStack item, World world, Entity entity, int n, boolean b) {
      if(entity instanceof EntityPlayer) {
         EntityPlayer player = (EntityPlayer)entity;
         ItemStack itemStack = player.getCurrentEquippedItem();
         if(itemStack == item) {
            MCH_MOD.proxy.setCreativeDigDelay(0);
         }
      }

   }

   public MCH_EntityAircraft getMouseOverAircraft(EntityPlayer player) {
      MovingObjectPosition m = this.getMouseOver(player, 1.0F);
//       Vec3 start = Vec3.createVectorHelper(player.posX,
//           player.posY + player.getEyeHeight(),
//           player.posZ);
//
//        // End point: 100 meters (≈100 blocks) along the line of sight
//       final double range = 100.0D; // 100 米 ≈ 100 方块
//       Vec3 look = player.getLook(1.0F); // 单位向量
//       Vec3 end  = start.addVector(look.xCoord * range,
//           look.yCoord * range,
//           look.zCoord * range);
//       MovingObjectPosition m = MCH_RayTracer.rayTraceVehicleCollision(player.worldObj, start, end , null);

      MCH_EntityAircraft ac = null;
      if(m != null) {
         if(m.entityHit instanceof MCH_EntityAircraft) {
            ac = (MCH_EntityAircraft)m.entityHit;
         } else if(m.entityHit instanceof MCH_EntitySeat) {
            MCH_EntitySeat seat = (MCH_EntitySeat)m.entityHit;
            if(seat.getParent() != null) {
               ac = seat.getParent();
            }
         }
      }

      return ac;
   }

   private static MovingObjectPosition rayTrace(EntityLivingBase entity, double dist, float tick) {
      Vec3 start = Vec3.createVectorHelper(entity.posX, entity.posY + (double)entity.getEyeHeight(), entity.posZ);
      Vec3 look = entity.getLook(tick);
      Vec3 end = start.addVector(look.xCoord * dist, look.yCoord * dist, look.zCoord * dist);
      return entity.worldObj.func_147447_a(start, end, false, false, true);
   }

   private MovingObjectPosition getMouseOver(EntityLivingBase user, float tick) {
       Vec3 lookVec = user.getLook(tick);
       Vec3 start = Vec3.createVectorHelper(user.posX, user.posY + (double)user.getEyeHeight(), user.posZ);
      Entity pointedEntity = null;
      double maxDoubledDist = 4D;
      MovingObjectPosition result = rayTrace(user, maxDoubledDist, tick);
      double dist = maxDoubledDist;
      if(result != null) {
         dist = result.hitVec.distanceTo(start);
      }
      Vec3 end = start.addVector(lookVec.xCoord * maxDoubledDist, lookVec.yCoord * maxDoubledDist, lookVec.zCoord * maxDoubledDist);
      Vec3 hitVec = null;
      float f1 = 1.0F;
      List list = user.worldObj.getEntitiesWithinAABBExcludingEntity(user,
          user.boundingBox.addCoord(lookVec.xCoord * maxDoubledDist, lookVec.yCoord * maxDoubledDist, lookVec.zCoord * maxDoubledDist).expand(f1, f1, f1));
      double d2 = dist;

       for (Object o : list) {
           Entity entity = (Entity) o;
           if (entity.canBeCollidedWith()) {
               float f2 = entity.getCollisionBorderSize();
               AxisAlignedBB axisalignedbb = entity.boundingBox.expand(f2, f2, f2);
               MovingObjectPosition movingobjectposition = axisalignedbb.calculateIntercept(start, end);
               if (axisalignedbb.isVecInside(start)) {
                   if (0.0D < d2 || d2 == 0.0D) {
                       pointedEntity = entity;
                       hitVec = movingobjectposition == null ? start : movingobjectposition.hitVec;
                       d2 = 0.0D;
                   }
               } else if (movingobjectposition != null) {
                   double d3 = start.distanceTo(movingobjectposition.hitVec);
                   if (d3 < d2 || d2 == 0.0D) {
                       if (entity == user.ridingEntity && !entity.canRiderInteract()) {
                           if (d2 == 0.0D) {
                               pointedEntity = entity;
                               hitVec = movingobjectposition.hitVec;
                           }
                       } else {
                           pointedEntity = entity;
                           hitVec = movingobjectposition.hitVec;
                           d2 = d3;
                       }
                   }
               }
           }
       }

      if(pointedEntity != null && (d2 < dist || result == null)) {
         result = new MovingObjectPosition(pointedEntity, hitVec);
      }

      return result;
   }

   public boolean onBlockDestroyed(ItemStack itemStack, World world, Block block, int x, int y, int z, EntityLivingBase entity) {
      if((double)block.getBlockHardness(world, x, y, z) != 0.0D) {
         itemStack.damageItem(2, entity);
      }

      return true;
   }

   @SideOnly(Side.CLIENT)
   public boolean isFull3D() {
      return true;
   }

   public EnumAction getItemUseAction(ItemStack itemStack) {
      return EnumAction.block;
   }

   public int getMaxItemUseDuration(ItemStack itemStack) {
      return 72000;
   }

   public ItemStack onItemRightClick(ItemStack itemStack, World world, EntityPlayer player) {
      player.setItemInUse(itemStack, this.getMaxItemUseDuration(itemStack));
      return itemStack;
   }

   public int getItemEnchantability() {
      return this.toolMaterial.getEnchantability();
   }

   public String getToolMaterialName() {
      return this.toolMaterial.toString();
   }

   public boolean getIsRepairable(ItemStack item1, ItemStack item2) {
      return this.toolMaterial.func_150995_f() == item2.getItem()?true:super.getIsRepairable(item1, item2);
   }

   public Multimap getItemAttributeModifiers() {
      Multimap multimap = super.getItemAttributeModifiers();
      multimap.put(SharedMonsterAttributes.attackDamage.getAttributeUnlocalizedName(), new AttributeModifier(Item.field_111210_e, "Weapon modifier", (double)this.damageVsEntity, 0));
      return multimap;
   }

}
