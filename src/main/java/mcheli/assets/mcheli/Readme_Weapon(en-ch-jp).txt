​​2016/04/17​​
;​​​​​​​​​​​​​​​​​​​​​​​​​​​​​​​​​​​​​​​​​​​​​​​​​​​​​​​​​​​​​​​​​​​​​​​​​​​​​​​​​*​
;■ Weapon configuration file: weapons/​​.txt, sound/​​_snd.ogg
;​​​​​​​​​​​​​​​​​​​​​​​​​​​​​​​​​​​​​​​​​​​​​​​​​​​​​​​​​​​​​​​​​​​​​​​​​​​​​​​​​*​
;★ Important ★
;Weapon configuration files can be reloaded without closing Minecraft.
;Enter a vehicle → Press R to open the supply interface → MOD Options → Development → Reload All Weapons
;This action reloads all weapons (including portable weapons).
;Adding a new weapon requires two files (all lowercase):
; ・Add a weapon configuration file (.txt) in the weapons folder
; ・Add a sound file with the same name in the sound folder (format: weaponname_snd.ogg)
; For example, weapon 'abc' requires weapons/abc.txt and sound/abc_snd.ogg
;※ Sound file is no longer mandatory in version 0.9.4+
;Some numerical parameters have upper and lower limits.
DisplayName = M134 Minigun
;Display Name ※Do not use full-width characters; only half-width alphanumerics and symbols are allowed.
Type = MachineGun1
;Weapon Type (choose one from the following):
; MachineGun1 Fixed-direction Machine Gun (e.g., M134)
; MachineGun2 Player-aimed Machine Gun (e.g., M230)
; Torpedo Torpedo (auto-locks on targets after entering water, e.g., Mk46)
; CAS Close Air Support (e.g., A-10)
; Rocket Fixed-direction unguided rocket (e.g., Hydra70 / SNEB68mm)
; ASMissile Air-to-Surface Missile (strikes ground coordinates, e.g., AGM119)
; AAMissile Air-to-Air Missile (tracks airborne entities, e.g., AIM92)
; TVMissile Player-controlled missile after launch (e.g., AGM114[TV])
; ATMissile Anti-Tank Missile (tracks ground entities, e.g., AGM114)
; Bomb Vertically dropped bomb (e.g., CBU-100)
; MkRocket Marker Rocket (calls artillery strike on impact point, e.g., Hydra 70mm M264RP)
; Dummy Dummy weapon (displays text only, unusable)
; Smoke Smoke grenade (generates contrail, e.g., White Smoke)
; Dispenser Dispenser (uses item at impact point, e.g., Water Dispenser)
; TargetingPod Targeting Pod (marks entities/players/blocks, e.g., targeting_pod_block)
; Railgun Railgun (requires charging to fire)
Power = 8
;Base damage value, 1 damage = 1 heart (half a heart icon)
DamageFactor = tank, 2.0
;Damage multiplier:
; Parameter 1: Target type (player=player, heli/helicopter=helicopter, plane=plane, tank=tank/car, vehicle=ground vehicle)
; Parameter 2: Damage multiplier (e.g., Power=10 + multiplier 3.4 = 34 damage)
;Multiple lines can be added to configure multipliers for different targets.
Acceleration = 4.0
;Projectile velocity (Most weapons max 4.0, MachineGun1/2 and Rocket can go up to 100.0. Too high may cause projectile jitter)
AccelerationInWater = 4.0
;Torpedo speed in water (max 4.0)
VelocityInWater = 0.5
;Acceleration in water (Speed adjusted by multiplying this value per tick)
Explosion = 0
;Impact explosion power (0=no explosion, 1=equivalent to fireball power)
ExplosionInWater = 0
;Underwater explosion power
ExplosionBlock = 0
;Explosion block destruction power (0=does not destroy blocks)
ExplosionAltitude = 10
;Minimum arming height (explodes when ≤10 meters above ground)
DelayFuse = 30
;Delay fuse: Projectile lifetime in ticks (timer starts after impact)
;If Explosion is not 0, explodes when projectile disappears.
Bound = 0.4
;Bounce strength (requires DelayFuse to be set, otherwise explodes on impact)
TimeFuse = 30
;Time fuse: Projectile lifetime in ticks (timer starts after firing)
;If Explosion is not 0, explodes when projectile disappears.
Flaming = false
;Whether to spread fire at the impact point (only effective if Explosion>0)
Sight = MoveSight
;Reticle type:
; MoveSight Reticle moves with the vehicle
; MissileSight Target lock reticle (Required for AAMissile/ATMissile)
; None No reticle
Zoom = 4.2, 9.2
;For portable weapons only: Default zoom level(s) (comma-separated for multiple levels, Z key to cycle)
Group = MainGun
;Weapon Group:
; Firing any weapon in the same group triggers reload for the entire group.
; Example: A tank's main gun has three configs: APFSDS, HE, Canister, all set to Group = MainGun.
; Firing any shell type puts the others on cooldown (ammo count remains unchanged).
Delay = 5
;Firing interval (unit: 1/20 second. Lower value = higher rate of fire)
ReloadTime = 80
;Reload time (unit: 1/20 second. Lower value = faster reload)
;※ If reload time is set to 0, ensure Round > 0.
Round = 100
;Ammo capacity per load (Set to 0 or leave empty for infinite)
SoundVolume = 3
;Firing volume (Values ≥1.0 play at max volume. Set <1.0 to reduce volume)
SoundPitch = 1.0
;Sound pitch (0.0~1.0)
SoundPitchRandom = 0.1
;Random pitch variation range (e.g., SoundPitch=0.8 + this value 0.2 → actual pitch 0.6~0.8)
SoundDelay = 1
;Rapid-fire sound delay (prevents sounds from fast-firing weapons like M134 from overlapping others)
Sound = rocket_snd
;Sound filename (no extension. If not specified, defaults to weaponname_snd.ogg)
LockTime = 20
;Missile lock-on time (Higher value = slower lock)
RidableOnly = true
;Can only lock onto targets when the player is riding a vehicle.
ProximityFuseDist = 1.0
;Missile proximity fuse distance (1.0 = explodes when target is within 1 meter)
RigidityTime = 0
;Number of ticks delay after missile launch before homing starts (default 7)
Accuracy = 1
;Unguided projectile spread error (Higher value = less accurate)
Bomblet = 25
;Number of submunitions released (for cluster bombs, etc.)
BombletSTime = 5
;Submunition release delay (ticks)
BombletDiff = 0.7
;Submunition spread range
ModeNum = 2
;Number of weapon modes (X key to cycle, only effective for these types):
; MachineGun2 → Switch to High Explosive (requires Explosion>0)
; TVMissile → Switch to regular guidance (non-missile camera view)
; ATMissile → Switch to Top Attack mode
; Rocket → Switch to Airburst (releases sub-bombs in air)
; Currently only supports 1 or 2 modes.
Piercing = 2
;Maximum number of blocks the projectile can pierce through.
;Current version causes multiple explosions; each number represents one explosion.
HeatCount = 20
;Heat generated per shot.
MaxHeatCount = 150
;Maximum heat capacity.
FAE = true
;Fuel-Air Explosive (Thermobaric) switch (true enables)
;Note: This type of bomb does not destroy blocks.
ModelBullet = bullet
ModelBomblet = cbc
;Projectile model files:
; Example: loads models/bullets/bullet.obj + textures/bullets/bullet.png
; Submunition model: models/bullets/cbc.obj + textures/bullets/cbc.png
Destruct = true
;Vehicle self-destructs after use (Only effective when Type=Bomb and the vehicle is a drone helicopter)
Gravity = -0.04
GravityInWater = 0.0
;Projectile gravity acceleration (Positive=up, Negative=down. Larger absolute value = faster fall)
;GravityInWater is gravity in water.
GuidedTorpedo = true
;Torpedo guidance switch:
; true=Guided (flies to designated coordinates)
; false=Straight-running (moves straight after entering water)
TrajectoryParticle = flame
;Projectile trail particle effect type (missile trail, etc.):
; none None
; explode Explosion particles
; flame Flame particles
; hugeexplosion Large explosion particles
; largeexplode Big explosion particles
; largesmoke Large smoke particles
; smoke Smoke particles
;※ Particle parameter deprecated in 1.0.0, use AddMuzzleFlash / AddMuzzleFlashSmoke instead.
TrajectoryParticleStartTick = 10
;Delay in ticks before trajectory particles start generating.
DisableSmoke = true
;Disable weapon movement dust effect (not firing effect).
AddMuzzleFlash = 0.5, 0.20, 1, 150,254,219,184
;Parameters: Distance from source, Size, Duration, A,R,G,B (RGBA color)
;★Note: May not display correctly if firing interval (Delay) ≈ 5!
AddMuzzleFlashSmoke = 2.2, 1, 5.0, 2.0, 15, 180,250,245,240
;Parameters: Distance from source, Count, Size, Range, Duration, A,R,G,B
;★Note: May not display correctly if firing interval (Delay) ≈ 5!
SetCartridge = cartridge, 0.0, 0, 0, 2.00, -0.04, 0.40
;Cartridge ejection settings:
; SetCartridge = Model name, Initial velocity, Yaw offset, Pitch offset, Model scale, Gravity, Bounce
; Model name: all lowercase, half-width characters.
; Initial velocity: 0 = vertical drop.
; Yaw offset: Positive = eject left, Negative = eject right.
; Pitch offset: Positive = eject down, Negative = eject up.
; Model scale: Display scale.
; Gravity: Falling speed.
; Bounce: Collision bounce strength.
MaxAmmo = 40
;Maximum ammo capacity for the vehicle.
SuppliedNum = 10
;Amount of ammo received per supply action.
Item = 3, iron_ingot
Item = 4, gunpowder
Item = 2, redstone
;Items required for supply (only vanilla items supported).
;Example explanation:
;MaxAmmo=40, SuppliedNum=10
;One supply consumes Iron Ingot x3 + Gunpowder x4 + Redstone x2 → yields 10 ammo.
;To fully resupply 40 ammo, total consumption required: Iron Ingot x12 + Gunpowder x16 + Redstone x8.
BulletColor = 255, 255, 255, 255 ;RGBA Projectile color (in air)
BulletColorInWater = 255, 25, 25, 75 ;RGBA Projectile color (in water)
SmokeColor = 230, 200, 20, 80 ;RGBA Smoke color
SmokeSize = 2.0 ;Smoke size
SmokeMaxAge = 500 ;Smoke duration (ticks)
DisplayMortarDistance = true
;Display projectile impact point distance.
FixCameraPitch = true
;Lock camera vertical angle to 0 degrees.
CameraRotationSpeedPitch = 0.3
;Camera pitch rotation speed multiplier (Lower value = finer control).
DispenseItem = flint_and_steel
;Use item at impact point (Example: Flint and Steel)
;Valid items: water_bucket can extinguish fire/lava.
DispenseRange = 4
;Item effect range (unit: blocks).
Recoil = 1.1
;Recoil intensity.
RecoilBufCount = 40, 5
;Recoil parameters = Total recoil duration (ticks), Recoil process multiplier.
; Longer total duration = more sustained recoil.
; Higher multiplier = more intense recoil.
Target = monsters/others
;Targetable entities (supports multiple, / separated):
; planes Planes
; helicopters Helicopters
; vehicles Ground vehicles
; players Players
; monsters Monsters
; others Other entities
; block Block marking (This mode overrides other targets).
Length = 100
;Marking distance (unit: blocks).
Radius = 45
;Marking cone angle (45 = 45° cone).
MarkTime = 10
;Marking duration (seconds).
====================================== Updated 2025.10.8, parameters below are added by MCHeli-Reforged =====================================
flakParticlesCrack = 10
;Number of block break particles generated.
;Default=10
numParticlesFlak = 3
;Number of white smoke particles generated.
;Default=3
flakParticlesDiff = 0.3
;Spread of generated block break particles, recommended values 0.1 (rifle bullet) ~ 0.6 (anti-tank rifle).
;Default=0.3
hitSound = ""
;Sound effect when projectile hits. Add the corresponding audio filename after the = sign.
;Default=none
hitSoundIron = "hit_metal"
;Sound effect when projectile hits metal. Add the corresponding audio filename after the = sign.
;Default=hit_metal
railgunSound = "railgun"
;Charging sound effect for railgun type weapons.
;Default=railgun
;*Railgun charge time parameter: locktime = 20
hitSoundRange = 100
;Range for hit sound propagation.
;Default=100
isHeatSeekerMissile = true
;Whether it is an infrared missile, can be distracted by flares.
;Default=true
isRadarMissile = false
;Whether it is a radar missile, can be distracted by chaff.
;Default=false
maxDegreeOfMissile = 60
;Maximum seeker gimbal limit for the missile. Beyond this angle, the missile loses guidance.
;Default=60
tickEndHoming = -1
;Number of ticks after the missile loses lock before it stops homing. -1 means permanent lock.
;Default=-1
maxLockOnRange = 300
;Maximum lock-on range. Affected by vehicle's Stealth parameter. Actual lock range ≈ maxLockOnRange*(1-Stealth).
;Default=300
maxLockOnAngle = 10
;Aircraft radar maximum lock angle, can be thought of as FoV. Conversion formula with field of view radius r and distance d: r = d * tan(FoV / 2). maxLockOnAngle corresponds to FoV in the formula.
;Default=10
pdHDNMaxDegree = 180
;Velocity Gate Radar maximum angle. Beyond this angle, lock is lost. (Can also simulate rear-aspect IR missile attacks) (Bug suspected, only works for IR and semi-active radar missiles).
;Default=1000
pdHDNMaxDegreeLockOutCount = 10
;Velocity Gate Radar lockout count. After exceeding max angle, missile loses lock after this many ticks. (Bug suspected, not working).
;Default=10
antiFlareCount = -1
;Missile flare resistance duration. -1 means no resistance.
;Default=-1
lockMinHeight = 25
;Radar missile multipath clutter detection height. Target below this height causes radar missile to lose lock. (Only for AAMissile weapon type).
;Default=25
passiveRadar = false
;Whether it is a semi-active radar missile, requires continuous illumination (Left-click to fire, Right-click to lock and maintain illumination on target).
passiveRadarLockOutCount = 20
;Semi-active radar missile lock loss countdown after losing illumination. (Bug suspected, not working).
;Default=20
laserGuidance = false
;Enable laser guidance for TV missiles.
;Default=false
hasLaserGuidancePod = true
;Whether a laser targeting pod is present. true=can guide laser in free look, false=laser guidance direction is tied to aircraft nose.
;Default=true
enableOffAxis = true
;Allow off-boresight launch for AAMissile and ATMissile.
;Default=true
turningFactor = 0.1
;Missile maneuverability parameter. Smaller value = smoother maneuver. Value of 1 = original MCH missile maneuverability. Recommended value 0.25. (Note: Value should not be too small, or missile may fail to hit).
;Default=0.5
enableChunkLoader = false
;Whether to enable chunk loader. Ammunition can load chunks autonomously. (Experimental, suspected not to work on Uranium core servers).
;Default=false
activeRadar = false
;Whether it is an active radar missile, automatically tracks target after launch.
;Default=false
scanInterval = 20
;Active radar missile scan interval (Scans for targets within lock range every X ticks. If an enemy is found, it will track).
;Default=20
weaponSwitchCount = 0
;Weapon switch cooldown time.
;Default=0
weaponSwitchSound = ""
;Weapon switch sound effect.
;Default=none
recoilPitch = 0.0
;Weapon vertical recoil.
;Default=0.0
recoilYaw = 0.0
;Weapon horizontal recoil (fixed direction).
;Default=0.0
recoilPitchRange = 0.0
;Weapon random vertical recoil (Recoil 2 + rndRecoil 0.5 == 1.5-2.5 Recoil range).
;Default=0.0
recoilYawRange = 0.0
;Weapon random horizontal recoil.
;Default=0.0
recoilRecoverFactor = 0.8
;Weapon recoil recovery speed.
;Default=0.8
speedFactor = 0
;Speed change per tick. <0 decelerates, >0 accelerates.
;Default=0
speedFactorStartTick = 0
;Tick count after launch when the speed multiplier takes effect. speedFactorStartTick = 20 means speed change starts 20 ticks after launch.
;Default=0
speedFactorEndTick = 0
;Tick count after launch when the speed multiplier effect ends. speedFactorEndTick = 80 means speed change ends 80 ticks after launch.
;Default=0
speedDependsAircraft = false
;Whether speed depends on the launching aircraft. Final speed = aircraft current speed + projectile initial Acceleration + speedFactor*(speedFactorEndTick - speedFactorStartTick).
;Default=false
canLockMissile = false
;Whether it can lock onto missile entities.
;Default=false
enableBVR = false
;Enable Beyond Visual Range radar (requires server-side usage).
;Default=false
minRangeBVR = 300
;Minimum range for BVR target acquisition functionality.
;Default=300
predictTargetPos = true
;Whether the missile can predict entity position. (Not recommended, not very effective).
;Default=true
numLockedChaffMax = 2
;Maximum number of chaff locks. Exceeding this causes missile to fly straight. (Currently bugged, seems to only work when aircraft faces missile and deploys chaff).
;Default=2
explosionDamageVsLiving = 1
explosionDamageVsPlayer = 1
explosionDamageVsPlane = 1
explosionDamageVsVehicle = 1
explosionDamageVsTank = 1
explosionDamageVsHeli = 1
explosionDamageVsShip = 1
;Explosion damage multiplier against different entity types. Value of 2 = 2x explosion damage.
;Default=1
canBeIntercepted = false
;Whether the projectile entity can be intercepted. Recommended to set true for rocket and missile type weapons, false for conventional weapons.
;Default=false
canAirburst = false
;Whether programmable airburst functionality can be set (Right-click on a block to set airburst distance).
;Default=false
explosionAirburst
;Explosion radius when airburst is triggered, unit same as 'explosion'.
;If not set separately, defaults to the value of 'explosion'.
crossType = 0
;Custom HUD field for indicating crosshair/reticle HUD.
;Default=none
hasMortarRadar = false
;Whether it has artillery/mortar radar.
;Default=false
mortarRadarMaxDist = -1
;Artillery radar maximum display radius. Should be greater than the maximum range of the indirect fire weapon. (For indirect fire weapons, first measure max range at 45° elevation, then set the radar display radius).
;Default=-1
markerRocketSpawnNum = 5
;Number of bombs spawned by markerRocket.
;Default=5
markerRocketSpawnDiff = 15
;Spread of bombs spawned by markerRocket.
;Default=15
markerRocketSpawnHeight = 200
;Spawn height of bombs spawned by markerRocket.
;Default=200
markerRocketSpawnSpeed = 5
;Initial speed of bombs spawned by markerRocket.
;Default=5
nukeyield = 100
;Whether the weapon is an HBM nuclear weapon. =100 means nuclear blast radius is 100.
;Default=none
ExplosionType=hbmNT_Bomb
;Whether the weapon uses HBM bomb explosion effects.
;Default = none
ExplosionType=hbmNT_Shell
;Whether the weapon uses HBM artillery shell explosion effects.
;Default = none
effectyield = 10
;Blast radius for HBM conventional explosion effects (requires using ExplosionType=hbmNT_Shell or ExplosionType=hbmNT_Bomb).
;Value 1-4: small effect, 5-8: medium effect, ≥9: large effect.
;Default = none
NukeEffectOnly=true
;Enable HBM nuclear explosion effect without block destruction (requires nukeyield).
;Default = false
DisableDestroyBlock=true
;Enable HBM explosion effects without block destruction (requires using ExplosionType=hbmNT_Shell or ExplosionType=hbmNT_Bomb).
;Default = true