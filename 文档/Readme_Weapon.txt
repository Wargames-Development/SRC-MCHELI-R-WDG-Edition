
2016/04/17

;***********************************************************************************
;■ 武器配置文件 weapons/***.txt, sound/***_snd.ogg
;***********************************************************************************

;★ 重要 ★
;武器配置文件可在不关闭Minecraft的情况下重新加载。
;进入载具 → 按R键打开补给界面 → MOD选项 → 开发 → 重载所有武器
;此操作会重新加载所有武器（包括便携武器）。

;添加新武器需两个文件（全小写）：
; ・在weapons文件夹添加武器配置文件（.txt）
; ・在sound文件夹添加同名音效文件（格式：武器名_snd.ogg）
; 例如武器 abc 需要 weapons/abc.txt 和 sound/abc_snd.ogg
;※0.9.4+版本音效文件不再必须

;部分数值参数有上下限约束

DisplayName = M134 米尼岗机枪
;显示名称 ※请勿使用全角字符，仅限半角英数字及符号

Type = MachineGun1
;武器类型（可选以下其一）：
;	MachineGun1  固定朝向机枪（如 M134）
;	MachineGun2  随玩家视角转向机枪（如 M230）
;	Torpedo      鱼雷（入水后自动追踪目标，如 Mk46）
;	CAS          近距空中支援（如 A-10）
;	Rocket       固定朝向无制导火箭（如 Hydra70 / SNEB68mm）
;	ASMissile    空地导弹（打击地面坐标，如 AGM119）
;	AAMissile    空对空导弹（追踪空中生物，如 AIM92）
;	TVMissile    发射后玩家操控导弹（如 AGM114[TV]）
;	ATMissile    反坦克导弹（追踪地面生物，如 AGM114）
;	Bomb         垂直投放炸弹（如 CBU-100）
;	MkRocket     标记火箭（引导炮火打击落点，如 Hydra 70mm M264RP）
;	Dummy        虚拟武器（仅显示文字，不可用）
;	Smoke        烟幕弹（生成航迹云，如 White Smoke）
;	Dispenser    投射器（在落点使用物品，如 Water Dispenser）
;	TargetingPod 目标指示器（标记生物/玩家/方块，如 targeting_pod_block）
;	Railgun 电磁炮（需要蓄力发射）
；	Laser 激光

Power = 8
;基础伤害值，1伤害=1滴血=半颗心

DamageFactor = tank, 2.0
;伤害倍率：
; 参数1：目标类型（player=玩家，heli/helicopter=直升机，plane=固定翼，tank=坦克/汽车，vehicle=地面载具）
; 参数2：伤害倍率（例：Power=10 + 倍率3.4 = 34点伤害）
;可添加多行配置不同目标的倍率

Acceleration = 4.0
;弹体速度（大部分武器上限4.0，MachineGun1/2和Rocket可至100.0，速度过高会导致弹体震颤）

AccelerationInWater = 4.0
;鱼雷水下速度（上限4.0）

VelocityInWater = 0.5
;水下加速度（每Tick乘此值调整速度）

Explosion = 0
;落地爆炸威力（0=无爆炸，1=等同于火焰弹威力）
ExplosionInWater = 0
;水下爆炸威力
ExplosionBlock = 0
;爆炸方块破坏力（0=不破坏方块）
ExplosionAltitude = 10
;最低起爆高度（距离地面≤10米时爆炸）

DelayFuse = 30
;延时引信：弹体存活Tick数（落地后计时）
;若Explosion非0，弹体消失时爆炸

Bound = 0.4
;弹跳强度（需配合DelayFuse使用，否则落地即爆）

TimeFuse = 30
;定时引信：弹体存活Tick数（发射后计时）
;若Explosion非0，弹体消失时爆炸

Flaming = false
;是否在落点散布火焰（仅当Explosion>0时生效）

Sight = MoveSight
;屏幕准星类型：
;	MoveSight   随载具移动的准星
;	MissileSight 目标锁定准星（AAMissile/ATMissile必须使用此类型）
;	None        无准星

Zoom = 4.2, 9.2
;仅限便携武器：默认倍率（逗号分隔可设多档，Z键切换）

Group = MainGun
;武器分组：
; 同组武器中任一件开火会触发全组装填
; 例：坦克主炮分设穿甲弹(APFSDS)、高爆弹(HE)、霰弹(Canister)三个配置文件均设 Group = MainGun
;     发射任意弹种后，其他弹种同步进入装填（弹药数不减）

Delay = 5
;开火间隔（单位：1/20秒，值越小射速越高）

ReloadTime = 80
;装填耗时（单位：1/20秒，值越小越快）
;※ 装填时间设为0时需确保装弹数>0

Round = 100
;装弹量（设为0或不填表示无限）

SoundVolume = 3
;开火音量（≥1.0时按最大音量播放，如需降低请设<1.0）

SoundPitch = 1.0
;音调（0.0~1.0）

SoundPitchRandom = 0.1
;音调随机浮动范围（例：SoundPitch=0.8 + 此值0.2 → 实际音调0.6~0.8）

SoundDelay = 1
;连发音效延迟（防M134等速射武器覆盖其他声音）

Sound = rocket_snd
;音效文件名（无需扩展名，不指定时默认加载 武器名_snd.ogg）

LockTime = 20
;导弹锁定耗时（值越大锁定越慢）

RidableOnly = true
;仅玩家乘坐载具时可锁定目标

ProximityFuseDist = 1.0
;导弹近炸引信距离（1.0=目标进入1米范围时爆炸）

RigidityTime = 0
;导弹发射后开始追踪的延迟Tick数（默认7）

Accuracy = 1
;非制导弹药散布误差（值越大越不准）

Bomblet = 25
;子母弹展开数量（用于集束炸弹等）
BombletSTime = 5
;子弹展开延迟时间（Tick）
BombletDiff = 0.7
;子弹散布范围

ModeNum = 2
;武器模式数（X键切换，仅以下类型生效）：
; MachineGun2 → 切换高爆弹（需Explosion>0）
; TVMissile   → 切换常规制导（非导弹视角）
; ATMissile   → 切换攻顶模式（Top Attack）
; Rocket      → 切换空爆弹（空中释放子炸弹）
; 当前仅支持1或2种模式

Piercing = 2
;方块穿透次数上限
;现版本会导致多次爆炸，每个数值代表1次爆炸

HeatCount = 20
;单次开火增加的热量值
MaxHeatCount = 150
;热量上限

FAE = true
;燃料空气炸弹开关（true启用）
;注：此类炸弹不破坏方块

ModelBullet = bullet
ModelBomblet = cbc
;弹体模型文件：
; 示例：加载 models/bullets/bullet.obj + textures/bullets/bullet.png
; 子弹模型：models/bullets/cbc.obj + textures/bullets/cbc.png

Destruct = true
;使用后载具自毁（仅限Type=Bomb且为无人机直升机时生效）

Gravity = -0.04
GravityInWater = 0.0
;弹体重力加速度（正值向上，负值向下，绝对值越大下落越快）
;GravityInWater为水下重力

GuidedTorpedo = true
;鱼雷制导开关：
; true=制导（飞向指定坐标）
; false=直航（入水后直线前进）

TrajectoryParticle = flame
;弹道粒子特效类型（导弹尾迹等）：
; none          无
; explode       爆炸粒子
; flame         火焰粒子
; hugeexplosion 大型爆炸粒子
; largeexplode  大爆炸粒子
; largesmoke    大烟雾粒子
; smoke         烟雾粒子
;※ Particle参数在1.0.0弃用，改用 AddMuzzleFlash / AddMuzzleFlashSmoke

TrajectoryParticleStartTick = 10
;弹道粒子开始生成的延迟Tick数

DisableSmoke = true
;禁用武器移动扬尘特效（非开火特效）

AddMuzzleFlash  =  0.5,       0.20,  1,  150,254,219,184
;参数：距发射源距离, 尺寸, 持续时间, A,R,G,B（颜色RGBA）
;★注意：开火间隔≈5时可能无法正常显示！

AddMuzzleFlashSmoke  =  2.2,       1,    5.0,   2.0,  15,  180,250,245,240
;参数：距发射源距离, 数量, 尺寸, 范围, 持续时间, A,R,G,B
;★注意：开火间隔≈5时可能无法正常显示！

SetCartridge = cartridge, 0.0, 0, 0, 2.00, -0.04, 0.40
;抛壳设置：
; SetCartridge = 模型名, 初速, 偏航角, 俯仰角, 模型缩放, 重力, 弹跳
; 模型名：全小写半角名称
; 初速：0=垂直下落
; 偏航角：正值左抛，负值右抛
; 俯仰角：正值下抛，负值上抛
; 模型缩放：显示比例
; 重力：下坠速度
; 弹跳：碰撞反弹强度

MaxAmmo = 40
;载具最大携弹量
SuppliedNum = 10
;单次补给获得弹药数
Item =  3, iron_ingot
Item =  4, gunpowder
Item =  2, redstone
;补给所需物品及数量（仅支持原版物品）

;示例说明：
;MaxAmmo=40, SuppliedNum=10
;单次补给消耗铁锭x3+火药x4+红石x2 → 获得10发弹药
;补满40发需累计消耗：铁锭x12+火药x16+红石x8

BulletColor        = 255, 255, 255, 255 ;RGBA弹体颜色（空中）
BulletColorInWater = 255,  25,  25,  75 ;RGBA弹体颜色（水下）

SmokeColor  = 230, 200, 20, 80 ;RGBA烟雾颜色
SmokeSize   = 2.0   ;烟雾尺寸
SmokeMaxAge = 500   ;烟雾存续时间（Tick）

DisplayMortarDistance = true
;显示弹道落点距离

FixCameraPitch = true
;锁定摄像机垂直角度为0度

CameraRotationSpeedPitch = 0.3
;摄像机俯仰角转速倍率（值越小区分度越高）

DispenseItem = flint_and_steel
;在落点使用物品（示例：打火石）
;有效物品：水桶(water_bucket)可扑灭火焰/岩浆

DispenseRange = 4
;物品作用范围（单位：方块）

Recoil = 1.1
;后坐力强度

RecoilBufCount = 40, 5
;后坐参数 = 后坐总时长(Tick), 后坐过程倍率
; 总时长越长后坐越持久
; 倍率越高后坐越剧烈

Target = monsters/others
;可标记目标（支持多选，/分隔）：
; planes       固定翼
; helicopters  直升机
; vehicles     地面载具
; players      玩家
; monsters     怪物
; others       其他生物
; block        方块标记（此模式会覆盖其他目标）

Length = 100
;标记距离（单位：方块）

Radius = 45
;标记范围角度（45=45°锥形范围）

MarkTime = 10
;标记持续时间（秒）

======================================2025.10.8更新，以下为MCHeli-Reforged新增的参数=====================================
flakParticlesCrack = 10
;生成的方块破碎粒子数量
;默认值=10

numParticlesFlak = 3
;生成的白色烟雾粒子数量
;默认值=3
 
flakParticlesDiff = 0.3;
;生成的方块破碎粒子扩散，推荐值0.1(步枪子弹) ~ 0.6(反坦克步枪)
;默认值=0.3

hitSound = ""
;弹体命中时的音效，=号后添加相应的音频文件名
;默认值=无

hitSoundIron = "hit_metal"
;弹体击中金属物体时的音效，=号后添加相应音频文件名
;默认=hit_metal

railgunSound = "railgun"
;railgun类型武器的蓄力音效
;默认值=railgun
;*电磁炮蓄力时间参数：locktime = 20

hitSoundRange = 100
命中音效的传播距离;
;默认值=100
 
isHeatSeekerMissile = true
;是否为红外弹，会受到热焰弹干扰
;默认值=true
 
isRadarMissile = false
;是否为雷达弹，会受到箔条干扰
;默认值=false
 
maxDegreeOfMissile = 60
;弹药导引头最大导引角度，超过该角度导弹不制导。
;默认值=60
 
tickEndHoming = -1
;导弹脱离锁定后在多少时间（tick）会脱锁，-1为永远锁定
;默认值=-1
 
maxLockOnRange = 300
;最大锁定距离。会受载具Stealth参数影响，实际锁定距离约等于maxLockOnRange*(1-Stealth)。超视距导弹除外
;默认值=300
 
maxLockOnAngle = 10
;机载雷达最大锁定角度，可理解为FoV。与视场半径r，距离d的换算公式为r = d * tan(FoV / 2)。maxLockOnAngle就相当于公式中的FoV
;默认值=10
;在半主动雷达弹中还担任着机载雷达照射角度的功能
 
pdHDNMaxDegree = 180
;速度门雷达最大角度，超过此角度将脱锁 (也可用于模拟红外弹尾后攻击)(疑似存在bug，只对红外弹和半主动雷达弹生效)
;默认值=1000
 
pdHDNMaxDegreeLockOutCount = 10
;速度门雷达脱锁间隔，超过最大角度后，在该tick后导弹脱锁(疑似存在bug，不生效)
;默认值=10
 
antiFlareCount = -1
;导弹抗干扰时长，-1为不抗干扰
;默认值=-1
 
lockMinHeight = 25
;雷达导弹多径杂波检测高度，目标低于这个高度将使雷达弹脱锁(仅对AAmissile武器类型有用)
;默认值=25
 
passiveRadar = false
;是否为被动雷达制导导弹，需要持续引导(左键发射，右键锁定敌机持续引导)
;
 
passiveRadarLockOutCount = 20
;半主动雷达弹脱离引导后脱离锁定倒计时(疑似有bug，不生效)
;默认值=20
 
laserGuidance = false
;对TV导弹启用激光制导
;默认值=false

hasLaserGuidancePod = true
;是否有激光吊舱，true=可以自由视角引导激光，false=激光引导方向和机头指向绑定
;默认值=true

enableOffAxis = true
;允许离轴射击AAmissile和ATmissile
;默认值=true
 
turningFactor = 0.1
;导弹机动参数，数值越小机动越平滑，值设为1时为原版MCH导弹机动性，推荐值为0.25(注意，该数值不宜过小，否则可能会使得导弹打不中人)
;默认值=0.5
 
enableChunkLoader = false
;是否启用区块加载器，弹药可自主加载区块(试验功能，疑似Uranium核心服务端不生效)
;默认值=false
 
activeRadar = false
;是否为主动雷达弹，发射后自动追踪目标
;默认值=false

scanInterval = 20
;主动雷达弹 扫描间隔(每隔一定tick，对锁定范围内的目标扫描一次，若有敌机，则会追踪敌机)
;默认值=20
 
weaponSwitchCount = 0
;武器切换冷却时间
;默认值=0
 
weaponSwitchSound = ""
;武器切换音效
;默认值=无
 
recoilPitch = 0.0
;武器垂直后坐力
;默认值=0
 
recoilYaw = 0.0
;武器水平后坐力（固定方向）
;默认值=0
 
recoilPitchRange = 0.0
;武器随机垂直后坐力 (Recoil 2 + rndRecoil 0.5 == 1.5-2.5 Recoil range)
;默认值=0
 
recoilYawRange = 0.0
;武器随机水平后坐力
;默认值=0
 
recoilRecoverFactor = 0.8
;武器后坐力恢复速度
;默认值=0.8
 
speedFactor = 0 
;每tick速度增加数值，小于0减速，大于0加速
;默认值=0
 
speedFactorStartTick = 0
;每tick的速度乘数生效时长，speedFactorStartTick = 20=在发射20tick后开始速度变化
;默认值=0
 
speedFactorEndTick = 0
;每tick的速度乘数结束时间,speedFactorEndTick = 80=在发射80tick后结束速度变化
;默认值=0
 
speedDependsAircraft = false
;速度是否跟随载机，最终速度 = 载机当前speed + 弹药初始Acceleration+speedFactor*(speedFactorEndTick-speedFactorStartTick)
;默认值=false
 
canLockMissile = false
;是否可以锁定导弹实体
;默认值=false
 
enableBVR = false
;启用超视距雷达(需要在服务端使用)
;默认值=false
 
minRangeBVR = 300
;超视距索敌功能最小启用距离
;默认值=300
 
predictTargetPos = true
;导弹是否可以预测实体位置(不好用，不推荐)
;默认值=true
 
numLockedChaffMax = 2
;锁定箔条的最大次数，超过此次数导弹将变为直射状态（目前有bug，疑似只有战机正面对准导弹抛洒箔条时才能生效）
;默认值=2
 
explosionDamageVsLiving = 1
explosionDamageVsPlayer = 1
explosionDamageVsPlane = 1
explosionDamageVsVehicle = 1
explosionDamageVsTank = 1
explosionDamageVsHeli = 1
explosionDamageVsShip = 1
;对不同实体类型的爆炸伤害倍率,数值为2=2倍爆炸伤害
;默认值=1


canBeIntercepted = false
;弹药实体是否可以被拦截，推荐rocket和missile类型武器设置为true，常规武器设置为false
;默认=false

canAirburst = false
;是否可以设定可编程空爆功能(对准方块右键装订空爆距离)
;默认=false

explosionAirburst
;触发空爆时的爆炸范围，单位等同于explosion
;如不单独设定默认=explosion的数值

crossType = 0
;hud自定义字段，用于指示准星hud
;默认=无
 
hasMortarRadar = false
;是否有炮兵雷达
;默认=false
 
mortarRadarMaxDist = -1
;炮兵雷达最大显示半径，应大于曲射武器的最大射程(对于曲射武器建议先在45度仰角情况下测量最大射程，然后再设置炮兵雷达的显示半径)
;默认=-1

markerRocketSpawnNum = 5
;markerRocket召唤的炸弹数量
;默认值=5

markerRocketSpawnDiff = 15
;markerRocket召唤的炸弹散布
;默认值=15

markerRocketSpawnHeight = 200
;markerRocket召唤的炸弹的生成高度
;默认值=200

markerRocketSpawnSpeed = 5
;markerRocket召唤的炸弹生成时的速度
;默认值=5

nukeyield = 100
;武器是否为HBM核弹，=100时核弹破坏半径为100
;默认值=无

ExplosionType=hbmNT_Bomb
;武器爆炸特效是否采用HBM炸弹爆炸特效
;默认值 = 无

ExplosionType=hbmNT_Shell
;武器爆炸特效是否采用HBM火炮爆炸特效
;默认值 = 无

effectyield = 10
;启用HBM常规爆炸特效时武器的破坏半径（需搭配ExplosionType=hbmNT_Shell/ExplosionType=hbmNT_Bomb使用）
;值=1-4时为小规模特效，5-8时为中型特效，≥9时为大型特效
;默认值 = 无

NukeEffectOnly=true
;启用HBM核爆炸特效时不破坏方块（需搭配nukeyield使用）
;默认值 = false

DisableDestroyBlock=true
;启用HBM爆炸特效时不破坏方块（需搭配ExplosionType=hbmNT_Shell/ExplosionType=hbmNT_Bomb使用）
;默认值 = true

======================================2026.3.11更新，以下为MCHeli-Reforged 2.0新增的参数=====================================
spawnBulletInAir=true
;启用集束航空布撒器功能
;默认值=false

spawnBulletMaxNum = 1
;航空布撒器最大发射子弹药次数
;默认值=1

spawnBulletIntervalTick = 20
;航空布撒器的发射子弹药的间隔，即发射后20tick后发射子弹药，若spawnBulletMaxNum>1，则在发射完子弹药的下一秒继续发射下一发子弹药
;默认值=20

spawnBulletPerNum = 1
;每轮发射子弹药的弹丸数，注意总发射弹丸数=spawnBulletMaxNum*spawnBulletPerNum,共发射spawnBulletMaxNum轮，每轮弹药数为spawnBulletPerNum
;默认值为1

spawnBulletInheritSpeed=false
;子弹药继承母弹药的速度
;默认false，建议true

destructAfterSpawnBullet=false
;发射完所有子弹药销毁母弹药
;默认值＝false

ModelBomblet = weapon_name
;发射的子弹药的文件，注意，这里和mcheli原版的ModelBomblet参数不同，这个是直接调用weapons文件夹里面的武器文件，具体案例可以参考官方包F15E MLU的SPICE红外炸弹（当然可能这个文档会发的比官方包要早）
;如这个例子是在weapons文件夹内找寻atkjet_f15e_gpsbomb_bullet.txt并将其设置为子弹药
;注意调用的子弹药不能是当前武器文件，否则会无限递归

;以上集束弹药相关参数仅用于Rocket或者ASmissile类型武器

BulletDecay = Segmented ,距离1,倍率1,距离2,倍率2,距离3,倍率3........距离N,倍率N
;分段伤害倍率，在距离1和距离2之间的伤害等于power*倍率1，在距离2和距离3之间的伤害等于power*倍率2。用于模拟动能弹药的远距离伤害衰减
;例如BulletDecay = Segmented ,0, 1.3,  25, 1.2, 50, 1.1, 100, 1, 150 , 0.9, 200, 0.85, 250, 0.8, 300, 0.7表示在0-25米距离内伤害为1.3倍，25-50米距离为1.1倍随后距离越远伤害越低
;注意距离N一定要大于距离N-1


AddPotionEffect = <ID>, <DurationTick>, <Amplifier> (, <MinDistance>, <MaxDistance>)
;该参数只能用于machinegun与laser类型武器。对命中的目标添加药水效果ID为药水ID，DurationTick为持续时间，Amplifier为效果强度；<MinDistance>, <MaxDistance>表示最小和最大生效距离
; Syntax: AddPotionEffect = <ID>, <DurationTick>, <Amplifier> (, <MinDistance>, <MaxDistance>)
; IDs are 	1:MoveSpeed		2:MoveSlowdown		3:DigSpeed			4:DigSlowdown		5:DamageBoost	6:Heal			7:Harm		8:Jump			9:Confusion		10:Regeneration
;			11:Resistance	12:FireResistance	13:WaterBreathing	14:Invisibility		15:Blindness	16:NightVision	17:Hunger	18:Weakness		19:Poison		20:Wither

;例子如下
;AddPotionEffect = 15, 130, 2, 0, 150
;AddPotionEffect = 15, 80, 2, 150, 300
;AddPotionEffect = 15, 80, 1, 300, 600
;这三个参数分别代表命中0-150米内目标时，对其造成失明2的效果130tick;对150-300米的目标造成失明2效果80tick;对300-600米目标造成失明效果80tick。我用其模拟ZTZ99A的激光压制系统

isGPSMissile = false
;是否为GPS导弹，用于ASmissile类型武器，右键标记GPS导航点，左键发射弹药，导弹会向导航点移动
;默认值=false

canister = 40
;霰弹数量 (现在只支持MachineGun1)
;默认值=-1（？？？？我也不知道，反正代码是这样写的)

CanisterType = 1
;霰弹散布参数
;0为位置散布(不管距离多远，散布都相同)
;1为角度散布（距离越远散布越大)
;2为时间散布（用于模拟多连发弹药，同时支持角度散布）


BombletDiff = 2
;霰弹角度散布角度参数, 推荐值>0.5

BombletSTime = 0
;霰弹时间散布参数 推荐值>3

dragInAir = 0
;空气阻力，在x和z方向，正值减速，负值加速

lockEntity = false
;激光/GPS导弹可以锁定实体

cameraFollowLockEntity = false
;让视角跟随锁定实体，可以理解为战争雷霆的激光瞄准稳定器。但是目前效果不好用，除了激光制导防空导弹不推荐使用

cameraFollowStrength = 0.3
;吸附强度

AntiRadiationMissile = false
;是否为反辐射导弹，其前置条件为当前导弹必须为主动雷达制导导弹。在主动雷达制导导弹的基础上筛选EnableRWR=true的目标（MCHR目前的RWR系统和雷达系统基本是一体的)


semiActiveRadar = false
;是否为半主动雷达弹，其前置条件为当前导弹必须为主动雷达制导导弹。在主动雷达制导导弹的基础上，筛选机头指向一定范围的目标追踪
;半主动雷达弹中MaxLockOnAngle = 15决定机体雷达波束范围度数，该例子为15度，MaxDegreeOfMissile = 55决定导弹导引头雷达波束范围度数，该例子为55度。
;目标必须处于机载机雷达波束和导弹导引头雷达波束的重合区域才能够正常追踪，换言之就是导弹击中目标前机头必须对准目标

enableHMS = true
;是否拥有头瞄，半主动弹机体雷达波束随鼠标离轴转动，车载半主动雷达制导导弹必须要用该参数

nameOnRWR = "MSL"
;导弹在RWR/雷达上显示的名称，可以留空模拟隐私巡航导弹

proximityFuseTick=-1
;弹药出膛后多少tick启动近炸引信，-1表示永不启动

proximityFuseDamage=0
;近炸伤害，给在近炸范围内的目标追加一次固定伤害，都到DamageVsPlane等伤害参数增幅

proximityFuseHeight=20
;近炸激活高度，目标离地高度高于这个数值才能激活近炸


; 激光武器参数(前置条件为Type=Laser)
BulletColor        = 160, 160, 160, 255
;激光束的ARGB

Radius = 0.2
; 线宽

Length = 100
; 射程(m)


TimeFuse = 2
; 激光持续tick 最小设置为2

LaserStartDistance=2
;激光起始渲染距离
