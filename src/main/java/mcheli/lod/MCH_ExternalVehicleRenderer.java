package mcheli.lod;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import java.util.*;
import mcheli.aircraft.*;
import mcheli.helicopter.*;
import mcheli.plane.*;
import mcheli.tank.*;
import mcheli.vehicle.*;
import mcheli.weapon.MCH_WeaponSet;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraftforge.client.model.IModelCustom;
import org.lwjgl.opengl.GL11;

/** Render-only detached models. No entity tick, world insertion, packets or source gameplay methods. */
@SideOnly(Side.CLIENT)
public final class MCH_ExternalVehicleRenderer {
    public static final int API_VERSION=3;
    private static final Set<Context> contexts=Collections.newSetFromMap(new IdentityHashMap<Context,Boolean>());
    private static java.util.function.BooleanSupplier owner;
    private static java.util.function.Predicate<Entity> replacement,observation;
    private static boolean ownerFailed;
    private MCH_ExternalVehicleRenderer() { }
    public static void setOwner(java.util.function.BooleanSupplier owner,java.util.function.Predicate<Entity> replacement,java.util.function.Predicate<Entity> observation) {
        MCH_ExternalVehicleRenderer.owner=owner;MCH_ExternalVehicleRenderer.replacement=replacement;MCH_ExternalVehicleRenderer.observation=observation;ownerFailed=false;
    }
    public static boolean ownsLegacy() {
        if(owner==null) return false;if(ownerFailed) return true;
        try { return owner.getAsBoolean(); }
        catch(RuntimeException | LinkageError failure) { failed(failure);return true; }
    }
    public static boolean replaceNative(Entity entity) {
        if(entity==null) return false;
        if(isVisual(entity) || !ownsLegacy() || replacement==null) return false;
        try { return replacement.test(entity); }
        catch(RuntimeException | LinkageError failure) { failed(failure);return false; }
    }
    public static void observedNative(Entity entity) {
        if(entity==null) return;
        if(isVisual(entity) || !ownsLegacy() || observation==null) return;
        try {
            if(observation.test(entity)) for(Context context:contexts)
                if(context.sample.appearance!=null && context.sample.appearance.uuid.equals(entity.getUniqueID())) context.nativeSinceRender=true;
        } catch(RuntimeException | LinkageError failure) { failed(failure); }
    }
    private static void failed(Throwable failure) {
        if(!ownerFailed) mcheli.MCH_Lib.Log("External vehicle presentation ownership failed; legacy draw withheld: %s",failure.toString());ownerFailed=true;
    }
    public static mcheli.render.MCH_RenderFarVehicle.LaserTarget findLaserTarget(World world,net.minecraft.util.Vec3 start,net.minecraft.util.Vec3 end,UUID preferred,int excludedId) {
        if(world==null || start==null || end==null) return null;
        long now=System.nanoTime();double closest=start.squareDistanceTo(end);mcheli.render.MCH_RenderFarVehicle.LaserTarget best=null;
        for(Context context:contexts) {
            if(!context.renderValid || context.world!=world || context.definition==null || context.sample.appearance==null || context.sample.appearance.entityId==excludedId
                || now-context.renderedNanos>250000000L || context.nativeSinceRender) continue;
            MCH_VehicleVisualSnapshot pose=context.renderedPose;
            if(pose==null || context.sample.appearance.entityId!=pose.entityId || !context.sample.appearance.uuid.equals(pose.uuid)
                || !context.sample.appearance.type.equals(pose.type)) continue;
            mcheli.MCH_EntityInfo radar=mcheli.MCH_EntityInfoClientTracker.getEntityInfo(pose.entityId);
            if(radar==null || !pose.uuid.equals(radar.aircraftUuid) || radar.destroyed || System.currentTimeMillis()-radar.lastUpdateTime>500L
                || (radar.countermeasureFlags&mcheli.MCH_EntityInfo.CM_FLAG_JAMMING)!=0) continue;
            MCH_AircraftInfo info=context.definition;
            double radius=Math.max(.5,Math.max(info.bodyWidth/2,Math.max(Math.abs(info.bbZmin),Math.abs(info.bbZmax)))),height=Math.max(.5,info.bodyHeight);
            net.minecraft.util.Vec3 centre=net.minecraft.util.Vec3.createVectorHelper(context.renderX,context.renderY+height/2,context.renderZ);
            if(start.squareDistanceTo(centre)>(double)context.range*context.range) continue;
            net.minecraft.util.AxisAlignedBB box=net.minecraft.util.AxisAlignedBB.getBoundingBox(context.renderX-radius,context.renderY,context.renderZ-radius,
                context.renderX+radius,context.renderY+height,context.renderZ+radius);
            net.minecraft.util.MovingObjectPosition hit=box.calculateIntercept(start,end);
            boolean retained=pose.uuid.equals(preferred);
            double distance=retained?start.squareDistanceTo(centre):box.isVecInside(start)?0:hit==null?Double.NaN:start.squareDistanceTo(hit.hitVec);
            if(!Double.isFinite(distance) || (!retained && distance>=closest)) continue;
            best=new mcheli.render.MCH_RenderFarVehicle.LaserTarget(pose.uuid,centre,distance);if(retained) return best;closest=distance;
        }
        return best;
    }
    public static double nativeRadius(Entity entity) {
        return entity instanceof MCH_EntityAircraft?MCH_VisualModelBounds.radius(((MCH_EntityAircraft)entity).getAcInfo()):Double.NaN;
    }
    public static Context create(World world) { if(world==null || !world.isRemote) throw new IllegalArgumentException("Client world required");if(contexts.size()>=128) throw new IllegalStateException("Visual context limit");Context context=new Context(world);contexts.add(context);return context; }
    private interface Detached { }
    public static boolean isVisual(Entity entity) { return entity instanceof Detached; }
    public static final class Context implements AutoCloseable {
        private final World world;
        private final MCH_VehicleVisualTrack track=new MCH_VehicleVisualTrack();
        private final MCH_VehicleVisualTrack.Sample sample=new MCH_VehicleVisualTrack.Sample();
        private MCH_AircraftInfo definition;
        private MCH_EntityAircraft actor;
        private MCH_VehicleVisualParts first,last,current;
        private int visualTick=-1;
        private double previousX,previousY,previousZ,radius;
        private boolean sampled;
        private long renderedNanos;
        private boolean renderValid,nativeSinceRender;
        private double renderX,renderY,renderZ;
        private int range;
        private MCH_VehicleVisualSnapshot renderedPose;
        private Context(World world) { this.world=world; }
        public void observe(byte[] bytes,UUID uuid,int id,double x,double y,double z,long sequence,long receipt,int interval,int range) {
            if(range<0 || range>4096) throw new IllegalArgumentException("Invalid visual range");this.range=range;
            MCH_VehicleVisualSnapshot pose=MCH_VehicleVisualSnapshot.decode(bytes);
            if(!pose.uuid.equals(uuid) || pose.entityId!=id || Double.compare(x,pose.x)!=0 || Double.compare(y,pose.y)!=0 || Double.compare(z,pose.z)!=0)
                throw new IllegalArgumentException("Vehicle source/envelope identity mismatch");
            track.observe(pose,sequence,receipt,interval);
        }
        /** Output: x,y,z, conservative radius, packed light, partial tick. No model allocation. */
        public boolean sample(long now,double[] out) {
            sampled=track.sample(now,sample);if(!sampled || out.length<6) { invalidate();return false; }
            MCH_VehicleVisualSnapshot pose=sample.appearance;
            MCH_AircraftInfo info=info(pose.kind,pose.type);
            if(info==null || info.model==null) { invalidate();return false; }
            if(definition!=info) { actor=null;visualTick=-1;definition=info;radius=MCH_VisualModelBounds.radius(info); }
            double envelope=radius*Math.max(1,sample.state[MCH_VehicleVisualSnapshot.THROTTLE]);
            if(!Double.isFinite(envelope) || envelope<=0 || envelope>128) { invalidate();return false; }
            out[0]=sample.x;out[1]=sample.y;out[2]=sample.z;out[3]=envelope;out[4]=pose.light;out[5]=sample.age-(int)sample.age;return true;
        }
        public boolean prepare() {
            if(!sampled || definition==null) return false;
            first=sample.first.decodedParts();last=sample.last.decodedParts();current=sample.appearance.decodedParts();
            if(first==null || last==null || current==null) { invalidate();return false; }
            if(actor==null || actor.getEntityId()!=sample.appearance.entityId) {
                actor=make(sample.appearance.kind,world,this);actor.changeType(sample.appearance.type);actor.setEntityId(sample.appearance.entityId);
                visualTick=-1;
            }
            if(actor.getAcInfo()!=definition || !matches(current)) { invalidate();return false; }
            actor.setTextureName(sample.appearance.texture);
            actor.prevRotationYaw=actor.rotationYaw=sample.state[0];actor.prevRotationPitch=actor.rotationPitch=sample.state[1];
            actor.prevRotationRoll=actor.rotationRoll=sample.state[2];actor.prevLastRiderYaw=actor.lastRiderYaw=sample.state[3];
            actor.prevLastRiderPitch=actor.lastRiderPitch=sample.state[4];
            actor.setCurrentThrottle(sample.state[11]);actor.throttleBack=mix(first.throttleBack,last.throttleBack);
            actor.moveLeft=(sample.appearance.flags&2)!=0;actor.moveRight=(sample.appearance.flags&4)!=0;
            actor.onGround=(sample.appearance.flags&8)!=0;actor.setCommonStatus(6,(sample.appearance.flags&16)!=0,true);
            int tick=(int)sample.age;
            if(visualTick<0 || tick<visualTick || tick-visualTick>20) { visualTick=tick;previousX=sample.x;previousY=sample.y;previousZ=sample.z; }
            int steps=tick-visualTick;
            for(int i=0;i<steps;i++) {
                actor.motionX=(sample.x-previousX)/Math.max(1,steps);actor.motionY=(sample.y-previousY)/Math.max(1,steps);actor.motionZ=(sample.z-previousZ)/Math.max(1,steps);
                actor.updatePartWheel();actor.updatePartCrawlerTrack();rotateParts();
            }
            visualTick=tick;previousX=sample.x;previousY=sample.y;previousZ=sample.z;
            actor.setPosition(sample.x,sample.y,sample.z);actor.prevPosX=actor.lastTickPosX=sample.x;actor.prevPosY=actor.lastTickPosY=sample.y;actor.prevPosZ=actor.lastTickPosZ=sample.z;
            actor.ticksExisted=tick;
            part(actor.partHatch,sample.state[6]);part(actor.partCanopy,sample.state[7]);part(actor.partLandingGear,sample.state[8]);
            actor.prevRotLightHatch=actor.rotLightHatch=mix(first.lightHatch,last.lightHatch);
            actor.lastSearchLightYaw=angle(first.searchYaw,last.searchYaw);actor.lastSearchLightPitch=mix(first.searchPitch,last.searchPitch);
            if(actor instanceof MCH_EntityTank && ((MCH_EntityTank)actor).isWheeledHandling()) actor.prevRotYawWheel=actor.rotYawWheel=mix(first.steering,last.steering);
            if(actor instanceof MCP_EntityPlane) {
                MCP_EntityPlane plane=(MCP_EntityPlane)actor;part(plane.partNozzle,sample.state[9]);part(plane.partWing,sample.state[10]);
                plane.prevRotationRotor=plane.rotationRotor=sample.state[5];
            }
            if(actor instanceof MCH_EntityHeli) {
                MCH_EntityHeli heli=(MCH_EntityHeli)actor;heli.prevRotationRotor=heli.rotationRotor=sample.state[5];
                for(int r=0;r<current.rotorCount();r++) for(int b=0;b<current.bladeCount(r);b++) {
                    MCH_Blade blade=heli.rotors[r].blades[b];
                    float rotation=first.foldState==2 && last.foldState==2?(float)(sample.rotorPhase%360)+blade.getBaseRotation():angle(first.blade(r,b),last.blade(r,b));
                    blade.setPrevRotation(rotation);blade.setRotation(rotation);
                }
            }
            for(int i=0;i<current.bayCount(false);i++) actor.weaponBays[i].prevRot=actor.weaponBays[i].rot=mix(first.bay(false,i),last.bay(false,i));
            for(int i=0;i<current.bayCount(true);i++) actor.turretWeaponBays[i].prevRot=actor.turretWeaponBays[i].rot=mix(first.bay(true,i),last.bay(true,i));
            for(int i=0;i<current.weaponCount();i++) {
                MCH_WeaponSet weapon=actor.weapons[i];MCH_VehicleVisualParts.Weapon a=first.weapon(i),b=last.weapon(i),state=current.weapon(i);
                weapon.prevRotationYaw=weapon.rotationYaw=angle(a.yaw,b.yaw);weapon.prevRotationPitch=weapon.rotationPitch=mix(a.pitch,b.pitch);
                weapon.rotationTurretYaw=angle(a.turretYaw,b.turretYaw);weapon.prevRotBarrel=weapon.rotBarrel=angle(a.barrel,b.barrel);
                weapon.setAmmoNum(state.ammo);
                for(int j=0;j<state.barrels();j++) weapon.recoilBuf[j].prevRecoilBuf=weapon.recoilBuf[j].recoilBuf=mix(a.recoil(j),b.recoil(j));
            }
            int era=0;for(MCH_BoundingBox box:actor.extraBoundingBox) if(box.isERA) box.eraActive=(current.eraMask&(1L<<era++))!=0;
            return true;
        }
        private void rotateParts() {
            boolean occupied=(sample.appearance.flags&1)!=0;
            for(int i=0;i<actor.rotPartRotation.length;i++) {
                MCH_AircraftInfo.RotPart part=(MCH_AircraftInfo.RotPart)definition.partRotPart.get(i);
                actor.prevRotPartRotation[i]=actor.rotPartRotation[i];
                if(part.rotAlways || occupied) actor.rotPartRotation[i]=(actor.rotPartRotation[i]+part.rotSpeed)%360;
            }
            for(int i=0;i<actor.turretRotPartRotation.length;i++) {
                MCH_AircraftInfo.TurretRotPart part=(MCH_AircraftInfo.TurretRotPart)definition.partTurretRotPart.get(i);
                actor.prevTurretRotPartRotation[i]=actor.turretRotPartRotation[i];
                if((sample.appearance.flags&32)!=0 && (part.rotAlways || sample.state[11]>.01F || occupied))
                    actor.turretRotPartRotation[i]=(actor.turretRotPartRotation[i]+360F/(definition.radarScanTick>0?definition.radarScanTick:40))%360;
            }
        }
        private boolean matches(MCH_VehicleVisualParts parts) {
            int weapons=definition.weaponSetList.isEmpty()?0:actor.weapons.length;
            if(parts.weaponCount()!=weapons || parts.bayCount(false)!=actor.weaponBays.length || parts.bayCount(true)!=actor.turretWeaponBays.length) return false;
            if(first.weaponCount()!=parts.weaponCount() || last.weaponCount()!=parts.weaponCount()
                    || first.bayCount(false)!=parts.bayCount(false) || last.bayCount(false)!=parts.bayCount(false)
                    || first.bayCount(true)!=parts.bayCount(true) || last.bayCount(true)!=parts.bayCount(true)) return false;
            for(int i=0;i<weapons;i++) if(parts.weapon(i).barrels()!=actor.weapons[i].recoilBuf.length
                    || first.weapon(i).barrels()!=parts.weapon(i).barrels() || last.weapon(i).barrels()!=parts.weapon(i).barrels()) return false;
            if(actor instanceof MCH_EntityHeli) {
                MCH_EntityHeli heli=(MCH_EntityHeli)actor;
                if(parts.rotorCount()!=heli.rotors.length || first.rotorCount()!=parts.rotorCount() || last.rotorCount()!=parts.rotorCount()) return false;
                for(int i=0;i<parts.rotorCount();i++) if(parts.bladeCount(i)!=heli.rotors[i].blades.length
                    || first.bladeCount(i)!=parts.bladeCount(i) || last.bladeCount(i)!=parts.bladeCount(i)) return false;
            }
            int era=0;for(MCH_BoundingBox box:actor.extraBoundingBox) if(box.isERA) era++;
            return era==parts.eraCount;
        }
        private boolean cooldown(MCH_WeaponSet weapon,int index) {
            if(current==null || actor==null || index<0 || index>=32) return false;
            for(int i=0;i<current.weaponCount();i++) if(actor.weapons[i]==weapon) return (current.weapon(i).usedMask&(1<<index))!=0;return false;
        }
        public void render(double x,double y,double z,float partial) {
            if(actor==null) return;
            Object renderer=RenderManager.instance.getEntityRenderObject(actor);
            if(!(renderer instanceof MCH_RenderAircraft)) return;
            MCH_RenderAircraft draw=(MCH_RenderAircraft)renderer;
            GL11.glPushMatrix();
            try {
                draw.setCommonRenderParam(definition.smoothShading,sample.appearance.light);
                // Preserve coverage alpha in the capture while matching native RGB blending.
                org.lwjgl.opengl.GL14.glBlendFuncSeparate(GL11.GL_SRC_ALPHA,GL11.GL_ONE_MINUS_SRC_ALPHA,GL11.GL_ONE,GL11.GL_ONE_MINUS_SRC_ALPHA);
                GL11.glEnable(GL11.GL_ALPHA_TEST);GL11.glColor4f(1,1,1,1);
                draw.renderAircraft(actor,x,y,z,sample.state[0],sample.state[1],sample.state[2],partial);
                draw.renderCommonPart(actor,definition,0,0,0,partial);
                renderedNanos=System.nanoTime();renderValid=true;nativeSinceRender=false;renderedPose=sample.appearance;renderX=sample.x;renderY=sample.y;renderZ=sample.z;
            } finally { draw.restoreCommonRenderParam();GL11.glPopMatrix(); }
        }
        private float mix(float a,float b) { return a+(b-a)*sample.fraction; }
        private float angle(float a,float b) { float d=b-a;d-=Math.floor((d+180)/360)*360;return a+d*sample.fraction; }
        private static void part(MCH_Parts part,float value) { if(part!=null) part.prevRotation=part.rotation=value; }
        public void invalidate() { renderValid=false; }
        public void close() { contexts.remove(this);actor=null;definition=null;sampled=false; }
    }
    private static MCH_AircraftInfo info(int kind,String name) {
        switch(kind) {
            case MCH_VehicleVisualSnapshot.HELICOPTER:return MCH_HeliInfoManager.get(name);
            case MCH_VehicleVisualSnapshot.PLANE:return MCP_PlaneInfoManager.get(name);
            case MCH_VehicleVisualSnapshot.TANK:return MCH_TankInfoManager.get(name);
            case MCH_VehicleVisualSnapshot.VEHICLE:return MCH_VehicleInfoManager.get(name);
            default:return null;
        }
    }
    private static MCH_EntityAircraft make(int kind,World world,Context context) {
        switch(kind) {
            case MCH_VehicleVisualSnapshot.HELICOPTER:return new VisualHeli(world,context);
            case MCH_VehicleVisualSnapshot.PLANE:return new VisualPlane(world,context);
            case MCH_VehicleVisualSnapshot.TANK:return new VisualTank(world,context);
            case MCH_VehicleVisualSnapshot.VEHICLE:return new VisualVehicle(world,context);
            default:throw new IllegalArgumentException("Unsupported visual vehicle");
        }
    }
    private static final class VisualHeli extends MCH_EntityHeli implements Detached {
        private final Context context;VisualHeli(World world,Context context) { super(world);this.context=context; }
        public boolean isWeaponNotCooldown(MCH_WeaponSet weapon,int index) { return context.cooldown(weapon,index); }
    }
    private static final class VisualPlane extends MCP_EntityPlane implements Detached {
        private final Context context;VisualPlane(World world,Context context) { super(world);this.context=context; }
        public boolean isWeaponNotCooldown(MCH_WeaponSet weapon,int index) { return context.cooldown(weapon,index); }
    }
    private static final class VisualTank extends MCH_EntityTank implements Detached {
        private final Context context;VisualTank(World world,Context context) { super(world);this.context=context; }
        public boolean isWeaponNotCooldown(MCH_WeaponSet weapon,int index) { return context.cooldown(weapon,index); }
    }
    private static final class VisualVehicle extends MCH_EntityVehicle implements Detached {
        private final Context context;VisualVehicle(World world,Context context) { super(world);this.context=context; }
        public boolean isWeaponNotCooldown(MCH_WeaponSet weapon,int index) { return context.cooldown(weapon,index); }
    }
}
