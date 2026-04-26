package mcheli.weapon;

import mcheli.MCH_BaseInfo;
import mcheli.MCH_Color;
import mcheli.MCH_DamageFactor;
import mcheli.MCH_PotionEffect;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;

import java.util.*;

public class MCH_WeaponInfo extends MCH_BaseInfo {

    public static Random rand = new Random();
    public final String name;
    public String explosionType;
    public int nukeYield;
    public int chemYield = 0;
    public int effectYield = 0;
    public boolean nukeEffectOnly;
    public boolean enableNuke;
    public float nukeEffectScale;
    public boolean enableNukeFlash;
    public float nukeFlashRadiusFactor;
    public int nukeFlashDurationMin;
    public int nukeFlashDurationMax;
    public String displayName;
    public String type;
    public int power;
    public float acceleration;
    public float accelerationInWater;
    public int explosion;
    public int explosionBlock;
    public int explosionInWater;
    public int explosionAltitude;
    public int delayFuse;
    public float bound;
    public int timeFuse;
    public boolean flaming;
    public MCH_SightType sight;
    public float[] zoom;
    public int delay;
    public int reloadTime;
    public int round;
    public int suppliedNum;
    public int maxAmmo;
    public List roundItems;
    public int soundDelay;
    public float soundVolume;
    public float soundPitch;
    public float soundPitchRandom;
    public int soundPattern;
    public int lockTime;
    public boolean ridableOnly;
    public float proximityFuseDist;
    public int rigidityTime;
    public float accuracy;
    public int bomblet;
    public int bombletSTime;
    public float bombletDiff;
    public int modeNum;
    public int fixMode;
    public int piercing;
    public int heatCount;
    public int maxHeatCount;
    public boolean isFAE;
    public boolean isGuidedTorpedo;
    public float gravity;
    public float gravityInWater;
    public float velocityInWater;
    public boolean destruct;
    public String trajectoryParticleName;
    public int trajectoryParticleStartTick;
    public int trajectoryParticleEndTick;
    public boolean disableSmoke;
    public MCH_Cartridge cartridge;
    public MCH_Color color;
    public MCH_Color colorInWater;
    public String soundFileName;
    public float smokeSize;
    public int smokeNum;
    public int smokeMaxAge;
    public String dispenseItem;
    public int dispenseDamege;
    public int dispenseRange;
    public int recoilBufCount;
    public int recoilBufCountSpeed;
    public float length;
    public float radius;
    public float angle;
    public boolean displayMortarDistance;
    public boolean fixCameraPitch;
    public float cameraRotationSpeedPitch;
    public int target;
    public int markTime;
    public float recoil;
    public String bulletModelName;
    public MCH_BulletModel bulletModel;
    public int bulletModelEndTick;
    public String bulletModelNameEnd;
    public MCH_BulletModel bulletModelEnd;
    public String bombletModelName;
    public MCH_BulletModel bombletModel;
    public MCH_DamageFactor damageFactor;
    public String group;
    public List listMuzzleFlash;
    public List listMuzzleFlashSmoke;

    /**
     * 生成的方块破碎粒子数量
     */
    public int flakParticlesCrack = 10;
    /**
     * 生成的白色烟雾粒子数量
     */
    public int numParticlesFlak = 3;
    /**
     * 生成的方块破碎粒子扩散，推荐值0.1(步枪子弹) ~ 0.6(反坦克步枪)
     */
    public float flakParticlesDiff = 0.3F;
    public String hitSound = "";
    public String hitSoundIron = "hit_metal";
    public String railgunSound = "railgun";
    public float hitSoundRange = 100;
    /**
     * 是否为红外弹，会受到热焰弹干扰
     */
    public boolean isHeatSeekerMissile = true;
    /**
     * 是否为雷达弹，会受到箔条干扰
     */
    public boolean isRadarMissile = false;
    /**
     * 弹药导引头最大导引角度
     */
    public int maxDegreeOfMissile = 60;
    /**
     * 初始导引头角度阶段持续时长（tick），0=禁用
     */
    public int initMaxDegreeTick = 0;
    /**
     * 初始导引头角度阶段使用的导引角度
     */
    public int initMaxDegreeOfMissile = 60;
    /**
     * 脱锁延时，-1为永远锁定
     */
    public int tickEndHoming = -1;
    /**
     * 最大锁定距离
     */
    public int maxLockOnRange = 300;
    /**
     * 机载雷达最大锁定角度
     */
    public int maxLockOnAngle = 10;
    /**
     * 速度门雷达最大角度，超过此角度将脱锁 (也可用于红外弹尾后攻击)
     */
    public float pdHDNMaxDegree = 1000f;
    /**
     * 速度门雷达脱锁间隔，超过最大角度后，在该tick后导弹脱锁
     */
    public int pdHDNMaxDegreeLockOutCount = 10;
    /**
     * 导弹抗干扰时长，-1为不抗干扰
     */
    public int antiFlareCount = -1;
    /**
     * 雷达弹多径杂波检测高度，飞机低于这个高度将使雷达弹脱锁
     */
    public int lockMinHeight = 25;
    /**
     * 半主动雷达弹需要持续引导
     */
    public boolean passiveRadar = false;

    /**
     * 半主动雷达弹脱离引导后脱锁计时
     */
    public int passiveRadarLockOutCount = 20;

    /**
     * 对TV弹启用激光制导
     */
    public boolean laserGuidance = false;

    /**
     * 是否有激光吊舱
     */
    public boolean hasLaserGuidancePod = true;

    /**
     * 允许离轴射击 AA弹
     */
    public boolean enableOffAxis = true;

    /**
     * 导弹机动参数，越小越平滑，值设为1时为原版导弹机动，推荐值为0.1
     */
    public double turningFactor = 0.5;
    /**
     * 初始机动增强阶段持续时长（tick），0=禁用
     */
    public int initTurningFactorTick = 0;
    /**
     * 初始机动增强阶段使用的机动参数
     */
    public double initTurningFactor = 0.5;

    /**
     * 启用区块加载器(试验功能)
     */
    public boolean enableChunkLoader = false;

    /**
     * 主动雷达弹 BVR 发射后自动追踪目标
     */
    public boolean activeRadar = false;
    /**
     * 半主动雷达弹（在数据链模式下可关闭其自身搜跟，仅保留运动学）
     */
    public boolean semiActiveRadar = false;

    /**
     * 主动雷达弹 扫描间隔
     */
    public int scanInterval = 20;

    /**
     * 武器切换冷却
     */
    public int weaponSwitchCount = 0;

    /**
     * 武器切换音效
     */
    public String weaponSwitchSound = "";

    /**
     * 武器垂直后坐力
     */
    public float recoilPitch = 0.0F;
    /**
     * 武器水平后坐力（固定方向）
     */
    public float recoilYaw = 0.0F;
    /**
     * 武器随机垂直后坐力 (Recoil 2 + rndRecoil 0.5 == 1.5-2.5 Recoil range)
     */
    public float recoilPitchRange = 0.0F;
    /**
     * 武器随机水平后坐力
     */
    public float recoilYawRange = 0.0F;
    /**
     * 武器后坐力恢复速度
     */
    public float recoilRecoverFactor = 0.8F;

    /**
     * 每tick速度增加数值，小于0减速，大于0加速
     */
    public float speedFactor = 0F;
    /**
     * 每tick的速度乘数生效时长
     */
    public int speedFactorStartTick = 0;
    /**
     * 每tick的速度乘数结束时长
     */
    public int speedFactorEndTick = 0;
    /**
     * 速度是否跟随载机，最终速度 = 载机速度 + 子弹速度
     */
    public boolean speedDependsAircraft = false;
    /**
     * 是否启用CCIP投弹圈
     */
    public boolean ccip = false;
    public String ccipTexture = "CCIP";
    public float ccipFactor = 1.0F;
    /**
     * 是否可以锁定导弹实体
     */
    public boolean canLockMissile = false;
    /**
     * 允许超视距索敌
     */
    public boolean enableBVR = false;
    /**
     * 超视距索敌功能最小启用距离
     */
    public int minRangeBVR = 300;
    /**
     * 预测实体位置
     */
    public boolean predictTargetPos = true;

    /**
     * 锁定箔条的最大次数，超过此次数导弹将变为直射状态
     */
    public int numLockedChaffMax = 2;

    /**
     * 对不同实体类型的爆炸伤害倍率
     */
    public float explosionDamageVsLiving = 1f;
    public float explosionDamageVsPlayer = 1f;
    public float explosionDamageVsPlane = 1f;
    public float explosionDamageVsVehicle = 1f;
    public float explosionDamageVsTank = 1f;
    public float explosionDamageVsHeli = 1f;
    public float explosionDamageVsShip = 1f;
    public boolean explosionThroughWall;
    public float explosionThroughWallFactor = 1.0f;
    public boolean isNewExplosionBreak = true;
    /**
     * HBM特效阻止破坏方块
     */
    public boolean disableDestroyBlock = true;
    /**
     * 弹药是否能被拦截
     */
    public boolean canBeIntercepted = false;
    /**
     * 是否为可编程空爆弹
     */
    public boolean canAirburst = false;
    /**
     * 可编程空爆弹触发空爆时的爆炸范围
     */
    public int explosionAirburst;
    /**
     * hud自定义字段，用于指示准星hud
     */
    public int crossType = 0;
    /**
     * 是否有迫击炮雷达
     */
    public boolean hasMortarRadar = false;
    /**
     * 迫击炮雷达最大显示距离，应大于曲射武器的最大射程
     */
    public double mortarRadarMaxDist = -1;

    /**
     * Marker Rocket参数
     */
    public int markerRocketSpawnNum = 5;
    public int markerRocketSpawnDiff = 15;
    public int markerRocketSpawnHeight = 200;
    public int markerRocketSpawnSpeed = 5;

    /**
     * 是否有尾焰
     */
    public boolean enableExhaustFlare = false;

    /**
     * 子弹在飞行途中生成其他的子弹，适用于航空布撒器
     */
    public boolean spawnBulletInAir;
    public int spawnBulletMaxNum = 1;
    public int spawnBulletIntervalTick = 20;
    public int spawnBulletPerNum = 1;
    public boolean spawnBulletInheritSpeed;
    public boolean destructAfterSpawnBullet;
    public boolean ahead;
    public int aheadSolveIntervalTick = 2;

    /**
     * 子弹伤害衰减
     */
    public List<MCH_IBulletDecay> bulletDecay = new ArrayList<>();
    public boolean enableBulletDecay;

    /**
     * 被子弹击中时的药水效果
     */
    public List<MCH_PotionEffect> potionEffect = new ArrayList<>();

    /**
     * 是否为GPS导弹
     */
    public boolean isGPSMissile = false;
    /**
     * GPS导弹弹道模式
     */
    public boolean ballisticMissile = false;
    public double ballisticArcFactor = 0.20D;
    public double ballisticArcMinHeight = 20.0D;
    public double ballisticArcMaxHeight = 400.0D;
    public double ballisticMinDistance = 80.0D;
    /**
     * 中段横向正弦机动
     */
    public boolean ballisticLateralSine = false;
    public double ballisticLateralAmplitude = 12.0D;
    public double ballisticLateralWaves = 1.5D;
    public double ballisticLateralPhaseDeg = 0.0D;
    public double ballisticLateralStartRatio = 0.20D;
    public double ballisticLateralEndRatio = 0.85D;
    public double ballisticTerminalNoWeaveDist = 80.0D;
    public double ballisticTerminalCylinderRadius = 40.0D;

    /**
     * 霰弹数量
     */
    public int canister = -1;
    /**
     * 霰弹种类 0为位置散布 1为角度散布
     */
    public int canisterType = 0;

    /**
     * 空气阻力，在x和z方向，正值减速，负值加速
     */
    public double dragInAir = 0;

    /**
     * 激光/GPS导弹可以锁定实体
     */
    public boolean lockEntity = false;

    /**
     * 让视角跟随锁定实体
     */
    public boolean cameraFollowLockEntity = false;
    /**
     * 吸附强度，0.1-1.0
     */
    public float cameraFollowStrength = 0.3f;

    /**
     * 反辐射导弹
     */
    public boolean antiRadiationMissile = false;

    /**
     * 半主动弹，需要载机引导才能命中
     */
    /**
     * 启用数据链模式（雷达导弹默认开启）
     */
    public boolean enableDataLink = false;
    /**
     * 仅数据链模式
     */
    public boolean onlyDataLink = false;
    /**
     * 是否允许头瞄
     */
    public boolean enableHMS = true;

    /**
     * 弹药在RWR上面显示什么
     */
    public String nameOnRWR = "MSL";
    /**
     * 武器RCS前向系数（0.01~10）
     */
    public float rcsFrontFactor = 1.0F;
    /**
     * 武器RCS侧向系数（0.01~10）
     */
    public float rcsSideFactor = 1.0F;
    /**
     * 武器RCS后向系数（0.01~10）
     */
    public float rcsRearFactor = 1.0F;
    /**
     * 武器RCS接触保持时间系数（0.01~10）
     */
    public float rcsTimeFactor = 1.0F;

    /**
     * 多久启用近炸引信，-1永不启用
     */
    public int proximityFuseTick = -1;

    /**
     * 近炸伤害
     */
    public float proximityFuseDamage = 0;

    /**
     * 近炸高度
     */
    public int proximityFuseHeight = 20;
    private boolean hasProximityFuseDistSet = false;
    private boolean hasProximityFuseTickSet = false;
    private boolean hasProximityFuseHeightSet = false;
    private boolean hasBombletDiffSet = false;
    private boolean hasSpawnBulletMaxNumSet = false;
    private boolean hasSpawnBulletIntervalTickSet = false;
    private boolean hasSpawnBulletPerNumSet = false;
    private boolean hasSpawnBulletInheritSpeedSet = false;
    private boolean hasDestructAfterSpawnBulletSet = false;
    private boolean hasAheadSolveIntervalTickSet = false;
    private boolean hasExplosionThroughWallFactorSet = false;
    private boolean hasEnableDataLinkSet = false;

    public MCH_WeaponInfo(String name) {
        this.name = name;
        this.displayName = name;
        this.type = "";
        this.power = 0;
        this.acceleration = 1.0F;
        this.accelerationInWater = 1.0F;
        this.explosion = 0;
        this.explosionBlock = -1;
        this.explosionInWater = 0;
        this.explosionAltitude = 0;
        this.delayFuse = 0;
        this.timeFuse = 0;
        this.flaming = false;
        this.sight = MCH_SightType.NONE;
        this.zoom = new float[]{1.0F};
        this.delay = 10;
        this.reloadTime = 30;
        this.round = 0;
        this.suppliedNum = 1;
        this.roundItems = new ArrayList();
        this.maxAmmo = 0;
        this.soundDelay = 0;
        this.soundPattern = 0;
        this.soundVolume = 1.0F;
        this.soundPitch = 1.0F;
        this.soundPitchRandom = 0.1F;
        this.lockTime = 30;
        this.ridableOnly = false;
        this.proximityFuseDist = 0.0F;
        this.rigidityTime = 7;
        this.accuracy = 0.0F;
        this.bomblet = 0;
        this.bombletSTime = 10;
        this.bombletDiff = 0.3F;
        this.modeNum = 0;
        this.fixMode = 0;
        this.piercing = 0;
        this.heatCount = 0;
        this.maxHeatCount = 0;
        this.bulletModelName = "";
        this.bulletModelNameEnd = "";
        this.bombletModelName = "";
        this.bulletModel = null;
        this.bulletModelEnd = null;
        this.bombletModel = null;
        this.bulletModelEndTick = -1;
        this.isFAE = false;
        this.isGuidedTorpedo = false;
        this.gravity = 0.0F;
        this.gravityInWater = 0.0F;
        this.velocityInWater = 0.999F;
        this.destruct = false;
        this.trajectoryParticleName = "explode";
        this.trajectoryParticleStartTick = 0;
        this.trajectoryParticleEndTick = -1;
        this.cartridge = null;
        this.disableSmoke = false;
        this.color = new MCH_Color();
        this.colorInWater = new MCH_Color();
        this.soundFileName = name + "_snd";
        this.smokeMaxAge = 100;
        this.smokeNum = 1;
        this.smokeSize = 2.0F;
        this.dispenseItem = null;
        this.dispenseDamege = 0;
        this.dispenseRange = 1;
        this.recoilBufCount = 2;
        this.recoilBufCountSpeed = 3;
        this.length = 0.0F;
        this.radius = 200.0F;
        this.target = 1;
        this.recoil = 0.0F;
        this.damageFactor = null;
        this.group = "";
        this.listMuzzleFlash = null;
        this.listMuzzleFlashSmoke = null;
        this.displayMortarDistance = false;
        this.fixCameraPitch = false;
        this.cameraRotationSpeedPitch = 1.0F;
        this.nukeYield = 0;
        this.explosionType = "";
        this.enableNuke = false;
        this.nukeEffectScale = 1.0F;
        this.enableNukeFlash = true;
        this.nukeFlashRadiusFactor = 14.0F;
        this.nukeFlashDurationMin = 20;
        this.nukeFlashDurationMax = 80;
        this.rcsFrontFactor = 1.0F;
        this.rcsSideFactor = 1.0F;
        this.rcsRearFactor = 1.0F;
        this.rcsTimeFactor = 1.0F;
    }

    public void checkData() {
        if (this.explosionBlock < 0) {
            this.explosionBlock = this.explosion;
        }
        if (this.nukeEffectScale < 0.1F) {
            this.nukeEffectScale = 0.1F;
        }
        if (this.nukeFlashRadiusFactor < 1.0F) {
            this.nukeFlashRadiusFactor = 1.0F;
        }
        if (this.nukeFlashDurationMin < 1) {
            this.nukeFlashDurationMin = 1;
        }
        if (this.nukeFlashDurationMax < this.nukeFlashDurationMin) {
            this.nukeFlashDurationMax = this.nukeFlashDurationMin;
        }
        if (!this.hasEnableDataLinkSet && (this.activeRadar || this.passiveRadar || this.semiActiveRadar) && !this.antiRadiationMissile) {
            this.enableDataLink = true;
        }
        if (!this.enableDataLink) {
            this.onlyDataLink = false;
        }

        // Only validate against modeNum when modeNum is explicitly configured (>0).
        // Some weapons (e.g. TV missile) provide mode count in weapon class and may leave modeNum at 0.
        if (this.modeNum > 0 && this.fixMode >= this.modeNum) {
            this.fixMode = 0;
        }

        if (this.round <= 0) {
            this.round = this.maxAmmo;
        }

        if (this.round > this.maxAmmo) {
            this.round = this.maxAmmo;
        }

        if (this.explosion <= 0) {
            this.isFAE = false;
        }

        if (this.delayFuse <= 0) {
            this.bound = 0.0F;
        }

        if (this.isFAE) {
            this.explosionInWater = 0;
        }

        if (this.explosionThroughWall && !this.hasExplosionThroughWallFactorSet) {
            // Requirement: default through-wall damage factor is 0.5 when through-wall mode is enabled.
            this.explosionThroughWallFactor = 0.5f;
        }

        if (this.explosionThroughWallFactor < 0.0f) {
            this.explosionThroughWallFactor = 0.0f;
        }

        if (this.bomblet > 0 && this.bombletSTime < 1) {
            this.bombletSTime = 1;
        }

        if (this.destruct) {
            this.delay = 1000000;
        }

        if (this.ahead && this.spawnBulletInAir) {
            this.ahead = false;
        } else if (this.ahead) {
            if (!this.hasProximityFuseDistSet) {
                this.proximityFuseDist = 15.0F;
            }
            if (!this.hasProximityFuseTickSet) {
                this.proximityFuseTick = 5;
            }
            if (!this.hasProximityFuseHeightSet) {
                this.proximityFuseHeight = 30;
            }
            if (!this.hasDestructAfterSpawnBulletSet) {
                this.destructAfterSpawnBullet = true;
            }
            if (!this.hasBombletDiffSet) {
                this.bombletDiff = 1.0F;
            }
            if (!this.hasSpawnBulletMaxNumSet) {
                this.spawnBulletMaxNum = 1;
            }
            if (!this.hasSpawnBulletIntervalTickSet) {
                this.spawnBulletIntervalTick = 1;
            }
            if (!this.hasSpawnBulletPerNumSet) {
                this.spawnBulletPerNum = 10;
            }
            if (!this.hasSpawnBulletInheritSpeedSet) {
                this.spawnBulletInheritSpeed = true;
            }
            if (!this.hasAheadSolveIntervalTickSet) {
                this.aheadSolveIntervalTick = 2;
            }
        }

        if (this.spawnBulletIntervalTick < 1) {
            this.spawnBulletIntervalTick = 1;
        }
        if (this.aheadSolveIntervalTick < 1) {
            this.aheadSolveIntervalTick = 1;
        }
        if (this.trajectoryParticleEndTick >= 0 && this.trajectoryParticleEndTick < this.trajectoryParticleStartTick) {
            this.trajectoryParticleEndTick = this.trajectoryParticleStartTick;
        }
        if (this.bulletModelEndTick < -1) {
            this.bulletModelEndTick = -1;
        }
        if (this.ballisticArcFactor < 0.0D) {
            this.ballisticArcFactor = 0.0D;
        }
        if (this.ballisticArcMinHeight < 0.0D) {
            this.ballisticArcMinHeight = 0.0D;
        }
        if (this.ballisticArcMaxHeight < this.ballisticArcMinHeight) {
            this.ballisticArcMaxHeight = this.ballisticArcMinHeight;
        }
        if (this.ballisticMinDistance < 0.0D) {
            this.ballisticMinDistance = 0.0D;
        }
        if (this.ballisticLateralAmplitude < 0.0D) {
            this.ballisticLateralAmplitude = 0.0D;
        }
        if (this.ballisticLateralWaves < 0.0D) {
            this.ballisticLateralWaves = 0.0D;
        }
        this.ballisticLateralStartRatio = Math.max(0.0D, Math.min(1.0D, this.ballisticLateralStartRatio));
        this.ballisticLateralEndRatio = Math.max(0.0D, Math.min(1.0D, this.ballisticLateralEndRatio));
        if (this.ballisticLateralEndRatio < this.ballisticLateralStartRatio) {
            this.ballisticLateralEndRatio = this.ballisticLateralStartRatio;
        }
        if (this.ballisticTerminalNoWeaveDist < 0.0D) {
            this.ballisticTerminalNoWeaveDist = 0.0D;
        }
        if (this.ballisticTerminalCylinderRadius < 0.0D) {
            this.ballisticTerminalCylinderRadius = 0.0D;
        }

        if (!isCCIPSupportedType(this.type)) {
            this.ccip = false;
        }

        this.angle = (float) (Math.atan2(this.radius, this.length) * 180.0D / 3.141592653589793D);
    }

    public void loadItemData(String item, String data) {
        if (item.compareTo("displayname") == 0) {
            this.displayName = data;
        } else if (item.compareTo("type") == 0) {
            this.type = data.toLowerCase();
            if (this.type.equalsIgnoreCase("bomb") || this.type.equalsIgnoreCase("dispenser")) {
                this.gravity = -0.03F;
                this.gravityInWater = -0.03F;
            }
        } else if (item.compareTo("group") == 0) {
            this.group = data.toLowerCase().trim();
        } else if (item.compareTo("power") == 0) {
            this.power = this.toInt(data);
        } else if (item.equalsIgnoreCase("sound")) {
            this.soundFileName = data.toLowerCase().trim();
        } else if (item.compareTo("acceleration") == 0) {
            this.acceleration = this.toFloat(data, 0.0F, 100.0F);
        } else if (item.compareTo("accelerationinwater") == 0) {
            this.accelerationInWater = this.toFloat(data, 0.0F, 100.0F);
        } else if (item.compareTo("gravity") == 0) {
            this.gravity = this.toFloat(data, -50.0F, 50.0F);
        } else if (item.compareTo("gravityinwater") == 0) {
            this.gravityInWater = this.toFloat(data, -50.0F, 50.0F);
        } else if (item.equalsIgnoreCase("VelocityInWater")) {
            this.velocityInWater = this.toFloat(data);
        } else if (item.equalsIgnoreCase("explosion")) {
            this.explosion = this.toInt(data, 0, 200);
        } else if (item.equalsIgnoreCase("explosionBlock")) {
            this.explosionBlock = this.toInt(data, 0, 200);
        } else if (item.equalsIgnoreCase("explosioninwater")) {
            this.explosionInWater = this.toInt(data, 0, 50);
        } else if (item.equalsIgnoreCase("ExplosionAltitude")) {
            this.explosionAltitude = this.toInt(data, 0, 100);
        } else if (item.equalsIgnoreCase("TimeFuse")) {
            this.timeFuse = this.toInt(data, 0, 100000);
        } else if (item.equalsIgnoreCase("DelayFuse")) {
            this.delayFuse = this.toInt(data, 0, 100000);
        } else if (item.equalsIgnoreCase("Bound")) {
            this.bound = this.toFloat(data, 0.0F, 100000.0F);
        } else if (item.equalsIgnoreCase("flaming")) {
            this.flaming = this.toBool(data);
        } else if (item.equalsIgnoreCase("DisplayMortarDistance")) {
            this.displayMortarDistance = this.toBool(data);
        } else if (item.equalsIgnoreCase("FixCameraPitch")) {
            this.fixCameraPitch = this.toBool(data);
        } else if (item.equalsIgnoreCase("CameraRotationSpeedPitch")) {
            this.cameraRotationSpeedPitch = this.toFloat(data, 0.0F, 100.0F);
        } else if (item.compareTo("sight") == 0) {
            data = data.toLowerCase();
            if (data.compareTo("movesight") == 0) {
                this.sight = MCH_SightType.ROCKET;
            }

            if (data.compareTo("missilesight") == 0) {
                this.sight = MCH_SightType.LOCK;
            }
        } else {
            String[] s;
            int c;
            if (item.equalsIgnoreCase("Zoom")) {
                s = this.splitParam(data);
                if (s.length > 0) {
                    this.zoom = new float[s.length];

                    for (c = 0; c < s.length; ++c) {
                        this.zoom[c] = this.toFloat(s[c], 0.1F, 10.0F);
                    }
                }
            } else if (item.compareTo("delay") == 0) {
                this.delay = this.toInt(data, 0, 100000);
            } else if (item.equalsIgnoreCase("ExplosionType")) {
                this.explosionType = data;
            } else if (item.equalsIgnoreCase("nukeYield")) {
                this.nukeYield = this.toInt(data, 0, 100000);
            } else if (item.equalsIgnoreCase("chemYield")) {
                this.chemYield = this.toInt(data, 0, 100000);
            } else if (item.equalsIgnoreCase("EffectYield")) {
                this.effectYield = this.toInt(data, 0, 100000);
            } else if (item.equalsIgnoreCase("NukeEffectOnly")) {
                this.nukeEffectOnly = this.toBool(data);
            } else if (item.equalsIgnoreCase("EnableNuke")) {
                this.enableNuke = this.toBool(data);
            } else if (item.equalsIgnoreCase("NukeEffectScale")) {
                this.nukeEffectScale = this.toFloat(data, 0.1F, 10.0F);
            } else if (item.equalsIgnoreCase("EnableNukeFlash")) {
                this.enableNukeFlash = this.toBool(data);
            } else if (item.equalsIgnoreCase("NukeFlashRadiusFactor")) {
                this.nukeFlashRadiusFactor = this.toFloat(data, 1.0F, 100.0F);
            } else if (item.equalsIgnoreCase("NukeFlashDurationMin")) {
                this.nukeFlashDurationMin = this.toInt(data, 1, 400);
            } else if (item.equalsIgnoreCase("NukeFlashDurationMax")) {
                this.nukeFlashDurationMax = this.toInt(data, 1, 400);
            } else if (item.equalsIgnoreCase("MaxDegreeOfMissile")) {
                this.maxDegreeOfMissile = this.toInt(data, 0, 100000);
            } else if (item.equalsIgnoreCase("InitMaxDegreeOfMissile")) {
                s = this.splitParam(data);
                if (s.length >= 2) {
                    this.initMaxDegreeTick = this.toInt(s[0], 0, 100000);
                    this.initMaxDegreeOfMissile = this.toInt(s[1], 0, 100000);
                } else {
                    this.initMaxDegreeTick = 0;
                    this.initMaxDegreeOfMissile = this.maxDegreeOfMissile;
                }
            } else if (item.equalsIgnoreCase("TickEndHoming")) {
                this.tickEndHoming = this.toInt(data, -1, 100000);
            } else if (item.equalsIgnoreCase("FlakParticlesCrack")) {
                this.flakParticlesCrack = this.toInt(data, 0, 300);
            } else if (item.equalsIgnoreCase("ParticlesFlak")) {
                this.numParticlesFlak = this.toInt(data, 0, 100);
            } else if (item.equalsIgnoreCase("FlakParticlesDiff")) {
                this.flakParticlesDiff = this.toFloat(data);
            } else if (item.equalsIgnoreCase("IsRadarMissile")) {
                this.isRadarMissile = this.toBool(data);
            } else if (item.equalsIgnoreCase("IsHeatSeekerMissile")) {
                this.isHeatSeekerMissile = this.toBool(data);
            } else if (item.equalsIgnoreCase("MaxLockOnRange")) {
                this.maxLockOnRange = this.toInt(data, 0, 2000);
            } else if (item.equalsIgnoreCase("MaxLockOnAngle")) {
                this.maxLockOnAngle = this.toInt(data, 0, 200);
            } else if (item.equalsIgnoreCase("PDHDNMaxDegree")) {
                this.pdHDNMaxDegree = this.toFloat(data, -1, 90);
            } else if (item.equalsIgnoreCase("PDHDNMaxDegreeLockOutCount")) {
                this.pdHDNMaxDegreeLockOutCount = this.toInt(data, 0, 200);
            } else if (item.equalsIgnoreCase("AntiFlareCount")) {
                this.antiFlareCount = this.toInt(data, -1, 200);
            } else if (item.equalsIgnoreCase("LockMinHeight")) {
                this.lockMinHeight = this.toInt(data, -1, 100);
            } else if (item.equalsIgnoreCase("PassiveRadar")) {
                this.passiveRadar = this.toBool(data);
            } else if (item.equalsIgnoreCase("PassiveRadarLockOutCount")) {
                this.passiveRadarLockOutCount = this.toInt(data, 0, 200);
            } else if (item.equalsIgnoreCase("LaserGuidance")) {
                this.laserGuidance = this.toBool(data);
            } else if (item.equalsIgnoreCase("HasLaserGuidancePod")) {
                this.hasLaserGuidancePod = this.toBool(data);
            } else if (item.equalsIgnoreCase("ActiveRadar")) {
                this.activeRadar = this.toBool(data);
            } else if (item.equalsIgnoreCase("EnableOffAxis")) {
                this.enableOffAxis = this.toBool(data);
            } else if (item.equalsIgnoreCase("TurningFactor") || item.equalsIgnoreCase("LaserStartDistance")) {
                this.turningFactor = this.toDouble(data);
            } else if (item.equalsIgnoreCase("InitTurningFactor")) {
                s = this.splitParam(data);
                if (s.length >= 2) {
                    this.initTurningFactorTick = this.toInt(s[0], 0, 100000);
                    this.initTurningFactor = this.toDouble(s[1]);
                } else {
                    this.initTurningFactorTick = 0;
                    this.initTurningFactor = this.turningFactor;
                }
            } else if (item.equalsIgnoreCase("EnableChunkLoader")) {
                this.enableChunkLoader = this.toBool(data);
            } else if (item.equalsIgnoreCase("ScanInterval")) {
                this.scanInterval = this.toInt(data);
            } else if (item.equalsIgnoreCase("WeaponSwitchCount")) {
                this.weaponSwitchCount = this.toInt(data);
            } else if (item.equalsIgnoreCase("WeaponSwitchSound")) {
                this.weaponSwitchSound = data.toLowerCase().trim();
            } else if (item.equalsIgnoreCase("RecoilPitch")) {
                this.recoilPitch = this.toFloat(data);
            } else if (item.equalsIgnoreCase("RecoilYaw")) {
                this.recoilYaw = this.toFloat(data);
            } else if (item.equalsIgnoreCase("RecoilPitchRange")) {
                this.recoilPitchRange = this.toFloat(data);
            } else if (item.equalsIgnoreCase("RecoilYawRange")) {
                this.recoilYawRange = this.toFloat(data);
            } else if (item.equalsIgnoreCase("RecoilRecoverFactor")) {
                this.recoilRecoverFactor = this.toFloat(data);
            } else if (item.equalsIgnoreCase("SpeedFactor")) {
                this.speedFactor = this.toFloat(data);
            } else if (item.equalsIgnoreCase("SpeedFactorStartTick")) {
                this.speedFactorStartTick = this.toInt(data);
            } else if (item.equalsIgnoreCase("SpeedFactorEndTick")) {
                this.speedFactorEndTick = this.toInt(data);
            } else if (item.equalsIgnoreCase("SpeedDependsAircraft")) {
                this.speedDependsAircraft = this.toBool(data);
            } else if (item.equalsIgnoreCase("CCIP")) {
                this.ccip = this.toBool(data);
            } else if (item.equalsIgnoreCase("CCIPtexture")) {
                this.ccipTexture = data.trim();
            } else if (item.equalsIgnoreCase("CCIPFactor")) {
                this.ccipFactor = this.toFloat(data, 0.1F, 10.0F);
            } else if (item.equalsIgnoreCase("CanLockMissile")) {
                this.canLockMissile = this.toBool(data);
            } else if (item.equalsIgnoreCase("EnableBVR")) {
                this.enableBVR = this.toBool(data);
            } else if (item.equalsIgnoreCase("MinRangeBVR")) {
                this.minRangeBVR = this.toInt(data);
            } else if (item.equalsIgnoreCase("PredictTargetPos")) {
                this.predictTargetPos = this.toBool(data);
            } else if (item.equalsIgnoreCase("HitSound")) {
                this.hitSound = data.toLowerCase().trim();
            } else if (item.equalsIgnoreCase("HitSoundIron")) {
                this.hitSoundIron = data.toLowerCase().trim();
            } else if (item.equalsIgnoreCase("HitSoundRange")) {
                this.hitSoundRange = this.toInt(data);
            } else if (item.equalsIgnoreCase("NumLockedChaffMax")) {
                this.numLockedChaffMax = this.toInt(data);
            } else if (item.equalsIgnoreCase("ExplosionDamageVsLiving")) {
                this.explosionDamageVsLiving = this.toFloat(data);
            } else if (item.equalsIgnoreCase("ExplosionDamageVsPlayer")) {
                this.explosionDamageVsPlayer = this.toFloat(data);
            } else if (item.equalsIgnoreCase("ExplosionDamageVsPlane")) {
                this.explosionDamageVsPlane = this.toFloat(data);
            } else if (item.equalsIgnoreCase("ExplosionDamageVsVehicle")) {
                this.explosionDamageVsVehicle = this.toFloat(data);
            } else if (item.equalsIgnoreCase("ExplosionDamageVsTank")) {
                this.explosionDamageVsTank = this.toFloat(data);
            } else if (item.equalsIgnoreCase("ExplosionDamageVsHeli")) {
                this.explosionDamageVsHeli = this.toFloat(data);
            } else if (item.equalsIgnoreCase("ExplosionDamageVsShip")) {
                this.explosionDamageVsShip = this.toFloat(data);
            } else if (item.equalsIgnoreCase("ExplosionThroughWall")) {
                this.explosionThroughWall = this.toBool(data);
            } else if (item.equalsIgnoreCase("ExplosionThroughWallFactor")) {
                this.explosionThroughWallFactor = this.toFloat(data, 0.0F, 1.0F);
                this.hasExplosionThroughWallFactorSet = true;
            } else if (item.equalsIgnoreCase("IsNewExplosionBreak")) {
                this.isNewExplosionBreak = this.toBool(data);
            } else if (item.equalsIgnoreCase("DisableDestroyBlock")) {
                this.disableDestroyBlock = this.toBool(data);
            } else if (item.equalsIgnoreCase("RailgunSound")) {
                this.railgunSound = data.toLowerCase().trim();
            } else if (item.equalsIgnoreCase("CanBeIntercepted")) {
                this.canBeIntercepted = this.toBool(data);
            } else if (item.equalsIgnoreCase("CanAirburst")) {
                this.canAirburst = this.toBool(data);
            } else if (item.equalsIgnoreCase("ExplosionAirburst")) {
                this.explosionAirburst = this.toInt(data, 0, 50);
            } else if (item.equalsIgnoreCase("CrossType")) {
                this.crossType = this.toInt(data);
            } else if (item.equalsIgnoreCase("EnableMortarRadar")) {
                this.hasMortarRadar = this.toBool(data);
            } else if (item.equalsIgnoreCase("MortarRadarMaxDist")) {
                this.mortarRadarMaxDist = this.toDouble(data);
            } else if (item.equalsIgnoreCase("MarkerRocketSpawnNum")) {
                this.markerRocketSpawnNum = this.toInt(data);
            } else if (item.equalsIgnoreCase("MarkerRocketSpawnDiff")) {
                this.markerRocketSpawnDiff = this.toInt(data);
            } else if (item.equalsIgnoreCase("MarkerRocketSpawnHeight")) {
                this.markerRocketSpawnHeight = this.toInt(data);
            } else if (item.equalsIgnoreCase("MarkerRocketSpawnSpeed")) {
                this.markerRocketSpawnSpeed = this.toInt(data);
            } else if (item.equalsIgnoreCase("EnableExhaustFlare")) {
                this.enableExhaustFlare = this.toBool(data);
            } else if (item.equalsIgnoreCase("SpawnBulletInAir")) {
                this.spawnBulletInAir = this.toBool(data);
            } else if (item.equalsIgnoreCase("SpawnBulletMaxNum")) {
                this.spawnBulletMaxNum = this.toInt(data);
                this.hasSpawnBulletMaxNumSet = true;
            } else if (item.equalsIgnoreCase("SpawnBulletIntervalTick")) {
                this.spawnBulletIntervalTick = this.toInt(data);
                this.hasSpawnBulletIntervalTickSet = true;
            } else if (item.equalsIgnoreCase("SpawnBulletPerNum")) {
                this.spawnBulletPerNum = this.toInt(data);
                this.hasSpawnBulletPerNumSet = true;
            } else if (item.equalsIgnoreCase("SpawnBulletInheritSpeed")) {
                this.spawnBulletInheritSpeed = this.toBool(data);
                this.hasSpawnBulletInheritSpeedSet = true;
            } else if (item.equalsIgnoreCase("DestructAfterSpawnBullet")) {
                this.destructAfterSpawnBullet = this.toBool(data);
                this.hasDestructAfterSpawnBulletSet = true;
            } else if (item.equalsIgnoreCase("AHEAD")) {
                this.ahead = this.toBool(data);
            } else if (item.equalsIgnoreCase("AheadSolveIntervalTick")) {
                this.aheadSolveIntervalTick = this.toInt(data);
                this.hasAheadSolveIntervalTickSet = true;
            } else if (item.equalsIgnoreCase("AddPotionEffect")) {
                String[] split = data.split("\\s*,\\s*");
                int potionID = Integer.parseInt(split[0]);
                int duration = Integer.parseInt(split[1]);
                int amplifier = Integer.parseInt(split[2]);
                int startDist = -1;
                int endDist = -1;
                if (split.length == 5) {
                    startDist = Integer.parseInt(split[3]);
                    endDist = Integer.parseInt(split[4]);
                }
                this.potionEffect.add(new MCH_PotionEffect(new PotionEffect(potionID, duration, amplifier, false), startDist, endDist));
            } else if (item.equalsIgnoreCase("BulletDecay")) {
                String[] split = data.split("\\s*,\\s*");
                String bulletDecayType = split[0];
                String[] args = new String[split.length - 1];
                System.arraycopy(split, 1, args, 0, args.length);
                this.bulletDecay.add(MCH_BulletDecayFactory.createBulletDecay(bulletDecayType, args));
                this.enableBulletDecay = true;
            } else if (item.equalsIgnoreCase("IsGPSMissile")) {
                this.isGPSMissile = this.toBool(data);
            } else if (item.equalsIgnoreCase("BallisticMissile")) {
                this.ballisticMissile = this.toBool(data);
            } else if (item.equalsIgnoreCase("BallisticArcFactor")) {
                this.ballisticArcFactor = this.toDouble(data);
            } else if (item.equalsIgnoreCase("BallisticArcMinHeight")) {
                this.ballisticArcMinHeight = this.toDouble(data);
            } else if (item.equalsIgnoreCase("BallisticArcMaxHeight")) {
                this.ballisticArcMaxHeight = this.toDouble(data);
            } else if (item.equalsIgnoreCase("BallisticMinDistance")) {
                this.ballisticMinDistance = this.toDouble(data);
            } else if (item.equalsIgnoreCase("BallisticLateralSine")) {
                this.ballisticLateralSine = this.toBool(data);
            } else if (item.equalsIgnoreCase("BallisticLateralAmplitude")) {
                this.ballisticLateralAmplitude = this.toDouble(data);
            } else if (item.equalsIgnoreCase("BallisticLateralWaves")) {
                this.ballisticLateralWaves = this.toDouble(data);
            } else if (item.equalsIgnoreCase("BallisticLateralPhaseDeg")) {
                this.ballisticLateralPhaseDeg = this.toDouble(data);
            } else if (item.equalsIgnoreCase("BallisticLateralStartRatio")) {
                this.ballisticLateralStartRatio = this.toDouble(data);
            } else if (item.equalsIgnoreCase("BallisticLateralEndRatio")) {
                this.ballisticLateralEndRatio = this.toDouble(data);
            } else if (item.equalsIgnoreCase("BallisticTerminalNoWeaveDist")) {
                this.ballisticTerminalNoWeaveDist = this.toDouble(data);
            } else if (item.equalsIgnoreCase("BallisticTerminalCylinderRadius")) {
                this.ballisticTerminalCylinderRadius = this.toDouble(data);
            } else if (item.equalsIgnoreCase("Canister")) {
                this.canister = this.toInt(data);
            } else if (item.equalsIgnoreCase("CanisterType")) {
                this.canisterType = this.toInt(data);
            } else if (item.equalsIgnoreCase("DragInAir")) {
                this.dragInAir = this.toDouble(data);
            } else if (item.equalsIgnoreCase("LockEntity")) {
                this.lockEntity = this.toBool(data);
            } else if (item.equalsIgnoreCase("CameraFollowLockEntity")) {
                this.cameraFollowLockEntity = this.toBool(data);
            } else if (item.equalsIgnoreCase("CameraFollowStrength")) {
                this.cameraFollowStrength = this.toFloat(data);
            } else if (item.equalsIgnoreCase("AntiRadiationMissile")) {
                this.antiRadiationMissile = this.toBool(data);
            } else if (item.equalsIgnoreCase("SemiActiveRadar")) {
                this.semiActiveRadar = this.toBool(data);
            } else if (item.equalsIgnoreCase("EnableDataLink")) {
                this.enableDataLink = this.toBool(data);
                this.hasEnableDataLinkSet = true;
            } else if (item.equalsIgnoreCase("OnlyDataLink")) {
                this.onlyDataLink = this.toBool(data);
            } else if (item.equalsIgnoreCase("EnableHMS")) {
                this.enableHMS = this.toBool(data);
            } else if (item.equalsIgnoreCase("NameOnRWR")) {
                String name = data.trim();
                this.nameOnRWR = "NULL".equals(name) ? "" : name;
            } else if (item.equalsIgnoreCase("RCSFactor")) {
                s = this.splitParam(data);
                if (s.length >= 1) {
                    this.rcsFrontFactor = this.toFloat(s[0], 0.01F, 10.0F);
                }
                if (s.length >= 2) {
                    this.rcsSideFactor = this.toFloat(s[1], 0.01F, 10.0F);
                }
                if (s.length >= 3) {
                    this.rcsRearFactor = this.toFloat(s[2], 0.01F, 10.0F);
                }
                if (s.length >= 4) {
                    this.rcsTimeFactor = this.toFloat(s[3], 0.01F, 10.0F);
                }
            } else if (item.equalsIgnoreCase("RCSTimeFactor")) {
                this.rcsTimeFactor = this.toFloat(data, 0.01F, 10.0F);
            } else if (item.equalsIgnoreCase("ProximityFuseTick")) {
                this.proximityFuseTick = this.toInt(data);
                this.hasProximityFuseTickSet = true;
            } else if (item.equalsIgnoreCase("ProximityFuseDamage")) {
                this.proximityFuseDamage = this.toFloat(data);
            } else if (item.equalsIgnoreCase("ProximityFuseHeight")) {
                this.proximityFuseHeight = this.toInt(data);
                this.hasProximityFuseHeightSet = true;
            } else if (item.equalsIgnoreCase("DamageFactor")) {
                s = this.splitParam(data);
                if (s.length >= 2) {
                    if (this.damageFactor == null) {
                        this.damageFactor = new MCH_DamageFactor();
                    }
                    String eType = s[0].toLowerCase();
                    float damage = toFloat(s[1]);
                    switch (eType) {
                        case "living":
                            damageFactor.damageVsLiving = damage;
                            break;
                        case "player":
                            damageFactor.damageVsPlayer = damage;
                            break;
                        case "plane":
                            damageFactor.damageVsPlane = damage;
                            break;
                        case "tank":
                            damageFactor.damageVsTank = damage;
                            break;
                        case "vehicle":
                            damageFactor.damageVsVehicle = damage;
                            break;
                        case "heli":
                        case "helicopter":
                            damageFactor.damageVsHeli = damage;
                            break;
                        case "ship":
                            damageFactor.damageVsShip = damage;
                            break;
                    }
                }
            } else if (item.compareTo("reloadtime") == 0) {
                this.reloadTime = this.toInt(data, 3, 1000);
            } else if (item.compareTo("round") == 0) {
                this.round = this.toInt(data, 1, 30000);
            } else if (item.equalsIgnoreCase("MaxAmmo")) {
                this.maxAmmo = this.toInt(data, 0, 30000);
            } else if (item.equalsIgnoreCase("SuppliedNum")) {
                this.suppliedNum = this.toInt(data, 1, 30000);
            } else if (item.equalsIgnoreCase("Item")) {
                s = data.split("\\s*,\\s*");
                if (s.length >= 2 && s[1].length() > 0 && this.roundItems.size() < 3) {
                    c = this.toInt(s[0], 1, 64);
                    if (c > 0) {
                        int className = s.length >= 3 ? this.toInt(s[2], 0, 100000000) : 0;
                        this.roundItems.add(new MCH_WeaponInfo.RoundItem(c, s[1].toLowerCase().trim(), className));
                    }
                }
            } else if (item.compareTo("sounddelay") == 0) {
                this.soundDelay = this.toInt(data, 0, 1000);
            } else if (item.compareTo("soundpattern") != 0) {
                if (item.compareTo("soundvolume") == 0) {
                    this.soundVolume = this.toFloat(data, 0.0F, 1000.0F);
                } else if (item.compareTo("soundpitch") == 0) {
                    this.soundPitch = this.toFloat(data, 0.0F, 1.0F);
                } else if (item.equalsIgnoreCase("SoundPitchRandom")) {
                    this.soundPitchRandom = this.toFloat(data, 0.0F, 1.0F);
                } else if (item.compareTo("locktime") == 0) {
                    this.lockTime = this.toInt(data, 0, 1000);
                } else if (item.equalsIgnoreCase("RidableOnly")) {
                    this.ridableOnly = this.toBool(data);
                } else if (item.compareTo("proximityfusedist") == 0) {
                    this.proximityFuseDist = this.toFloat(data, 0.0F, 2000.0F);
                    this.hasProximityFuseDistSet = true;
                } else if (item.equalsIgnoreCase("RigidityTime")) {
                    this.rigidityTime = this.toInt(data, 0, 1000000);
                } else if (item.compareTo("accuracy") == 0) {
                    this.accuracy = this.toFloat(data, 0.0F, 1000.0F);
                } else if (item.compareTo("bomblet") == 0) {
                    this.bomblet = this.toInt(data, 0, 1000);
                } else if (item.compareTo("bombletstime") == 0) {
                    this.bombletSTime = this.toInt(data, 0, 1000);
                } else if (item.equalsIgnoreCase("BombletDiff")) {
                    this.bombletDiff = this.toFloat(data, 0.0F, 1000.0F);
                    this.hasBombletDiffSet = true;
                } else if (item.equalsIgnoreCase("RecoilBufCount")) {
                    s = this.splitParam(data);
                    if (s.length >= 1) {
                        this.recoilBufCount = this.toInt(s[0], 1, 10000);
                    }

                    if (s.length >= 2 && this.recoilBufCount > 2) {
                        this.recoilBufCountSpeed = this.toInt(s[1], 1, 10000) - 1;
                        if (this.recoilBufCountSpeed > this.recoilBufCount / 2) {
                            this.recoilBufCountSpeed = this.recoilBufCount / 2;
                        }
                    }
                } else if (item.compareTo("modenum") == 0) {
                    this.modeNum = this.toInt(data, 0, 1000);
                } else if (item.equalsIgnoreCase("FixMode")) {
                    this.fixMode = this.toInt(data, 0, 10);
                } else if (item.compareTo("piercing") == 0) {
                    this.piercing = this.toInt(data, 0, 100000);
                } else if (item.compareTo("heatcount") == 0) {
                    this.heatCount = this.toInt(data, 0, 100000);
                } else if (item.compareTo("maxheatcount") == 0) {
                    this.maxHeatCount = this.toInt(data, 0, 100000);
                } else if (item.compareTo("modelbullet") == 0) {
                    this.bulletModelName = data.toLowerCase().trim();
                } else if (item.equalsIgnoreCase("ModelBomblet")) {
                    this.bombletModelName = data.toLowerCase().trim();
                } else if (item.compareTo("fae") == 0) {
                    this.isFAE = this.toBool(data);
                } else if (item.compareTo("guidedtorpedo") == 0) {
                    this.isGuidedTorpedo = this.toBool(data);
                } else if (item.compareTo("destruct") == 0) {
                    this.destruct = this.toBool(data);
                } else if (item.equalsIgnoreCase("AddMuzzleFlash")) {
                    s = this.splitParam(data);
                    if (s.length >= 7) {
                        if (this.listMuzzleFlash == null) {
                            this.listMuzzleFlash = new ArrayList();
                        }

                        this.listMuzzleFlash.add(new MCH_WeaponInfo.MuzzleFlash(this.toFloat(s[0]), this.toFloat(s[1]), 0.0F, this.toInt(s[2]), this.toFloat(s[3]) / 255.0F, this.toFloat(s[4]) / 255.0F, this.toFloat(s[5]) / 255.0F, this.toFloat(s[6]) / 255.0F, 1));
                    }
                } else if (item.equalsIgnoreCase("AddMuzzleFlashSmoke")) {
                    s = this.splitParam(data);
                    if (s.length >= 9) {
                        if (this.listMuzzleFlashSmoke == null) {
                            this.listMuzzleFlashSmoke = new ArrayList();
                        }

                        this.listMuzzleFlashSmoke.add(new MCH_WeaponInfo.MuzzleFlash(this.toFloat(s[0]), this.toFloat(s[2]), this.toFloat(s[3]), this.toInt(s[4]), this.toFloat(s[5]) / 255.0F, this.toFloat(s[6]) / 255.0F, this.toFloat(s[7]) / 255.0F, this.toFloat(s[8]) / 255.0F, this.toInt(s[1], 1, 1000)));
                    }
                } else if (item.equalsIgnoreCase("TrajectoryParticle")) {
                    this.trajectoryParticleName = data.toLowerCase().trim();
                    if (this.trajectoryParticleName.equalsIgnoreCase("none")) {
                        this.trajectoryParticleName = "";
                    }
                } else if (item.equalsIgnoreCase("TrajectoryParticleStartTick")) {
                    this.trajectoryParticleStartTick = this.toInt(data, 0, 10000);
                } else if (item.equalsIgnoreCase("TrajectoryParticleEndTick")) {
                    this.trajectoryParticleEndTick = this.toInt(data, -1, 10000);
                } else if (item.equalsIgnoreCase("DisableSmoke")) {
                    this.disableSmoke = this.toBool(data);
                } else {
                    float var10;
                    if (item.equalsIgnoreCase("SetCartridge")) {
                        s = data.split("\\s*,\\s*");
                        if (s.length > 0 && s[0].length() > 0) {
                            var10 = s.length >= 2 ? this.toFloat(s[1]) : 0.0F;
                            float var11 = s.length >= 3 ? this.toFloat(s[2]) : 0.0F;
                            float pt = s.length >= 4 ? this.toFloat(s[3]) : 0.0F;
                            float sc = s.length >= 5 ? this.toFloat(s[4]) : 1.0F;
                            float gr = s.length >= 6 ? this.toFloat(s[5]) : -0.04F;
                            float bo = s.length >= 7 ? this.toFloat(s[6]) : 0.5F;
                            this.cartridge = new MCH_Cartridge(s[0].toLowerCase(), var10, var11, pt, bo, gr, sc);
                        }
                    } else if (item.equalsIgnoreCase("ModelBulletEndTick")) {
                        s = this.splitParam(data);
                        if (s.length >= 2) {
                            this.bulletModelEndTick = this.toInt(s[0], -1, 1000000);
                            this.bulletModelNameEnd = s[1].toLowerCase().trim();
                        }
                    } else if (!item.equalsIgnoreCase("BulletColorInWater") && !item.equalsIgnoreCase("BulletColor") && !item.equalsIgnoreCase("SmokeColor")) {
                        if (item.equalsIgnoreCase("SmokeSize")) {
                            this.smokeSize = this.toFloat(data, 0.0F, 100.0F);
                        } else if (item.equalsIgnoreCase("SmokeNum")) {
                            this.smokeNum = this.toInt(data, 1, 100);
                        } else if (item.equalsIgnoreCase("SmokeMaxAge")) {
                            this.smokeMaxAge = this.toInt(data, 2, 1000);
                        } else if (item.equalsIgnoreCase("DispenseItem")) {
                            s = data.split("\\s*,\\s*");
                            if (s.length >= 2) {
                                this.dispenseDamege = this.toInt(s[1], 0, 100000000);
                            }

                            this.dispenseItem = s[0];
                        } else if (item.equalsIgnoreCase("DispenseRange")) {
                            this.dispenseRange = this.toInt(data, 1, 100);
                        } else if (item.equalsIgnoreCase("Length")) {
                            this.length = this.toFloat(data, 1, 1000);
                        } else if (item.equalsIgnoreCase("Radius")) {
                            this.radius = this.toFloat(data, 0, 1000);
                        } else if (item.equalsIgnoreCase("Target")) {
                            if (data.indexOf("block") >= 0) {
                                this.target = 64;
                            } else {
                                this.target = 0;
                                this.target |= data.indexOf("planes") >= 0 ? 32 : 0;
                                this.target |= data.indexOf("helicopters") >= 0 ? 16 : 0;
                                this.target |= data.indexOf("vehicles") >= 0 ? 8 : 0;
                                this.target |= data.indexOf("tanks") >= 0 ? 8 : 0;
                                this.target |= data.indexOf("players") >= 0 ? 4 : 0;
                                this.target |= data.indexOf("monsters") >= 0 ? 2 : 0;
                                this.target |= data.indexOf("others") >= 0 ? 1 : 0;
                            }
                        } else if (item.equalsIgnoreCase("MarkTime")) {
                            this.markTime = this.toInt(data, 1, 30000) + 1;
                        } else if (item.equalsIgnoreCase("Recoil")) {
                            this.recoil = this.toFloat(data, 0.0F, 100.0F);
                        }
                    } else {
                        s = data.split("\\s*,\\s*");
                        if (s.length >= 4) {
                            var10 = 0.003921569F;
                            MCH_Color var12 = new MCH_Color(0.003921569F * (float) this.toInt(s[0], 0, 255), 0.003921569F * (float) this.toInt(s[1], 0, 255), 0.003921569F * (float) this.toInt(s[2], 0, 255), 0.003921569F * (float) this.toInt(s[3], 0, 255));
                            if (item.equalsIgnoreCase("BulletColorInWater")) {
                                this.colorInWater = var12;
                            } else {
                                this.color = var12;
                            }
                        }
                    }
                }
            }
        }

    }

    public float getDamageFactor(Entity e) {
        return this.damageFactor != null ? this.damageFactor.getDamageFactor(e) : 1.0F;
    }

    public double getEffectiveMaxDegreeOfMissile(int missileTick) {
        if (this.initMaxDegreeTick > 0 && missileTick >= 0 && missileTick <= this.initMaxDegreeTick) {
            return this.initMaxDegreeOfMissile;
        }
        return this.maxDegreeOfMissile;
    }

    public double getEffectiveTurningFactor(int missileTick) {
        if (this.initTurningFactorTick > 0 && missileTick >= 0 && missileTick <= this.initTurningFactorTick) {
            return this.initTurningFactor;
        }
        return this.turningFactor;
    }

    public double getHudPreferredMissileFovDeg() {
        if (this.initMaxDegreeTick > 0) {
            return this.initMaxDegreeOfMissile;
        }
        return this.maxDegreeOfMissile;
    }

    public String getWeaponTypeName() {
        switch (this.type.toLowerCase()) {
            case "machinegun1":
            case "machinegun2":
            case "railgun":
            case "laser":
                return "MachineGun";
            case "torpedo":
                return "Torpedo";
            case "cas":
                return "CAS";
            case "rocket":
                return "Rocket";
            case "asmissile":
                return "AS Missile";
            case "aamissile":
                return "AA Missile";
            case "tvmissile":
                return "TV Missile";
            case "atmissile":
                return "AT Missile";
            case "bomb":
                return "Bomb";
            case "mkrocket":
                return "Mk Rocket";
            case "dummy":
                return "Dummy";
            case "smoke":
                return "Smoke";
            case "dispenser":
                return "Dispenser";
            case "targetingpod":
                return "Targeting Pod";
            default:
                return "";
        }
    }


    public float getRecoilPitch() {
        return this.recoilPitch + (rand.nextFloat() * this.recoilPitchRange);
    }

    public float getRecoilYaw() {
        return this.recoilYaw + ((rand.nextFloat() - 0.5F) * this.recoilYawRange);
    }

    private boolean isCCIPSupportedType(String type) {
        if (type == null) {
            return false;
        }
        return type.equalsIgnoreCase("rocket")
            || type.equalsIgnoreCase("atmissile")
            || type.equalsIgnoreCase("tvmissile");
    }

    public class RoundItem {

        public final int num;
        public final String itemName;
        public final int damage;
        public ItemStack itemStack;


        public RoundItem(int n, String name, int damage) {
            this.num = n;
            this.itemName = name;
            this.damage = damage;
        }
    }

    public class MuzzleFlash {

        public final float dist;
        public final float size;
        public final float range;
        public final int age;
        public final float a;
        public final float r;
        public final float g;
        public final float b;
        public final int num;


        public MuzzleFlash(float dist, float size, float range, int age, float a, float r, float g, float b, int num) {
            this.dist = dist;
            this.size = size;
            this.range = range;
            this.age = age;
            this.a = a;
            this.r = r;
            this.g = g;
            this.b = b;
            this.num = num;
        }
    }
}
