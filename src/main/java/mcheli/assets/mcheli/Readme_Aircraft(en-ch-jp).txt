; ★ Important ★
; Configuration files and models can be reloaded without closing Minecraft.
; [ Enter a vehicle → Press R to open the supply interface → MOD Options → Development → Reload Aircraft Settings ]
; Textures and sounds must be reloaded using Minecraft's native function, not via the Helicopter MOD.
; [ Press Esc to open the game menu → Options → Resource Packs → Done ]

;***********************************************************************************
■ Common Settings for Helicopter/Fighter Plane/Ground Vehicle/Tank Configuration Files
;***********************************************************************************

DisplayName = AH-6 Killer Egg
; Display Name ※Do not use full-width characters; only half-width alphanumerics and symbols are allowed.

AddDisplayName = ja_JP, AH-6 キラ－エッグ
; Name displayed when holding the item.
; ※ When using Japanese full-width characters, file encoding must be UTF-8.

ItemID = 28801
; Item ID (will be +256 when actually used in Minecraft)
; ※ ItemID is not used in version 1.7.2+, but must be set for compatibility with 1.6.4 and earlier.

Invulnerable = true
; Vehicle invincibility mode.
; Recommended for base defense weapons in multiplayer games.

CreativeOnly = true
; Only creative mode players can place or retrieve the item.

AddTexture = sh60-us-1
AddTexture = sh60-jp-1
AddTexture = sh60-jp-2
; Additional textures (can add multiple).
; Uses a .png file with the same name as the config file by default.
; Add extra textures here (no file extension).

ThirdPersonDist = 12
; Default third-person view distance.
; PageUp/Down adjusts distance in 4-block increments in the Helicopter MOD; recommended to set as a multiple of 4.

CameraPosition = 0.0, 1.1, 3.7
CameraPosition = 0.0, 1.1, 3.7, false
CameraPosition = 0.0, 1.1, 3.7, true, 30, 45
; Camera coordinates.
; Multiple settings allow switching between different views with the H key.
; Positions 1-3: Coordinates.
; 4th position: If set to true, camera view is always locked.
; 5th position: Horizontal angle.
; 6th position: Vertical angle.

CameraZoom = 3
; Maximum camera zoom multiplier.

HUD = heli, heli_gnr, none, gunner
; HUD configuration file names used for each seat.
; In this example: Pilot seat uses heli.txt, 2nd seat uses heli_gnr.txt, 3rd seat has no HUD, 4th seat uses gunner.txt.
; If fewer settings than seats, unspecified seats use the following defaults:
; Default when not set:
; Helicopter: HUD = heli, heli_gnr, gunner, gunner, gunner, gunner...
; Plane: HUD = plane, plane, gunner, gunner, gunner, gunner...
; Ground Vehicle: HUD = vehicle
;
; ※ Only the pilot seat uses the 2nd seat's HUD configuration in gunner mode.
;    Even for single-seat vehicles, if gunner mode is enabled, the 2nd seat must be set.
;    Example: HUD = heli, heli_gnr

EnableGunnerMode = true
; Whether to enable gunner mode toggle (true=enabled, false=disabled).

EnableNightVision = true
; Whether to enable night vision mode toggle (true=enabled, false=disabled).

EnableEntityRadar = false
; Whether to enable radar (true=enabled, false=disabled).

Speed = 0.6
; Vehicle speed (higher value = faster).

MotionFactor = 0.96
; Vehicle movement deceleration coefficient (range 0.0~1.0, lower value = stronger deceleration).

Gravity = -0.04
; Vehicle gravity setting (negative value means falling).

GravityInWater = -0.04
; Gravity setting in water (negative value means falling).

StepHeight = 2.5
; Maximum block height the vehicle can step over.

MobilityYaw
MobilityYawOnGround
; Horizontal turn sensitivity (higher value = more agile).
; MobilityYawOnGround only affects ground, not water.
MobilityRoll
; Roll sensitivity (higher value = faster roll).
MobilityPitch
; Pitch sensitivity (higher value = more agile; for ground vehicles, indicates pitch angle limits).
MinRotationPitch
MaxRotationPitch
; Range MinRotationPitch -80~0
; Range MaxRotationPitch 0~80
; Pitch angle limits (min/max).
; ※ Enabling this for helicopters/planes also restricts roll.

MinRotationRoll
MaxRotationRoll
; Roll angle limits (min/max).
; Range MinRotationRoll -80~0
; Range MaxRotationRoll 0~80
; ※ Enabling this for helicopters/planes also restricts pitch.

UnmountPosition = 3.0, 1.0, -2.0
; Coordinates when dismounting.

AddSeat =-0.45,  0.80,  1.20
AddSeat = 0.45, -0.50,  1.20
AddSeat =-0.90, -0.50,  0.20
AddSeat = 0.90, -0.50,  0.20, true
; Add a seat ※At least 1 seat is required except for drones.
; The first one is the pilot seat.
; Parameters are coordinates (X,Y,Z).
; The 4th parameter determines if the seat rotates with the driver's direction (mainly for tank turrets).

AddGunnerSeat = -0.45, 0.80, 1.20,   0.0, 2.00, -1.01,   true
AddGunnerSeat = -0.45, 0.80, 1.20,   0.0, 2.00, -1.01,   true, -60, 78, true
; AddGunnerSeat=Seat X,Y,Z,  Camera X,Y,Z,  Can switch view,  Camera upper limit (-90~0), Camera lower limit (0~90), Seat rotates with turret
; Add a gunner seat.
; Players in this seat default to camera view.
; Parameters include seat coordinates and camera position (camera position can be omitted, defaults to CameraPosition).
; If view switching is false, camera view is locked; if true, H key can switch back to player view.
; The 10th parameter controls if the seat rotates with the driver's direction.

AddFixRotSeat = -0.45,  0.80,  1.20, 0.0,2.00,-1.01,  true,  -50, 40
; AddFixRotSeat=Seat X,Y,Z,  Camera X,Y,Z,  Can switch view,  Fixed horizontal angle, Fixed vertical angle
; Add a fixed-view seat.
; Similar to AddGunnerSeat, but camera angle is fixed and cannot be adjusted.
; Setting fixed angles disables mouse view adjustment (Ctrl key can switch to free view).

; ★★★★★
; Vehicle carrying functionality requires:
; ・Carrying vehicle specifies the carried vehicle (AddRack)
; ・Carried vehicle specifies the rack number of the carrier (RideRack)
; Either condition being met will take effect.

AddRack = container,                 0.0, 1.4, -4.7,  0.0, 1.0, -16.1
AddRack = container / ah-64,         0.0, 1.4, -4.7,  0.0, 1.0, -16.1,  5.0, 20
AddRack = helicopter/vehicle / t-4,  0.0, 1.4, -4.7,  0.0, 1.0, -16.1,  5.0, 100000,  0.0, 0.0
; ■ Carrier settings
; AddRack =
;  Parameter 1: Carryable entity name
;  Parameters 2-4: Rack coordinates X,Y,Z (position on the vehicle)
;  Parameters 5-7: Entrance/exit coordinates X,Y,Z (entity must be near these coordinates to load/unload)
;  Parameter 8: Entrance radius (can be omitted)
;  Parameter 9: Parachute opening height (set very high to disable parachute)
;  Parameter 10: Carried entity's horizontal angle
;  Parameter 11: Carried entity's vertical angle
; Add a rack that can carry containers/helicopters/etc.
;  Entity names can use container/helicopter/plane/vehicle or directly specify a vehicle name (e.g., ah-64), separated by /.

RideRack = c5, 1
; ■ Carried vehicle settings
; RideRack = Carrier vehicle name, Rack number (1~)

ExclusionSeat = 15, 17
; Unlimited number of parameters (must be ≥2)
; Set mutually exclusive seats/racks.
; Example: ExclusionSeat = 3,4,5:
;  If seat 3 is occupied, seats 4/5 cannot hold entities.
;  If seat 4 is occupied, seats 3/5 cannot hold entities.
;  If seat 5 is occupied, seats 3/4 cannot hold entities.
;
; Seat/Rack numbering rule: Number all seat definitions first in order, then number the racks.
; Example configuration order:
;  AddSeat  → No.1
;  AddRack  → No.4
;  AddGunnerSeat → No.2
;  AddRack  → No.5
;  AddSeat  → No.3
;  AddRack  → No.6
; It is recommended to define racks last for clear numbering.

TurretPosition = 0.0, 0.0, 0.25
; Turret rotation center position (not recommended to modify unless necessary).

AddWeapon = m230,     0.00, 0.90, 2.54,   0.0, 0.0, true, 2
AddWeapon = hydra70,  0.00, 0.90, 2.54,   0.0, 0.0, true, 1, 0,-60,60, 0,25
AddWeapon = m134,     1.48, 0.40, 1.54,   1.0, 0.0
AddWeapon = m134,    -1.48, 0.40, 1.54,  -1.0, 0.0
AddTurretWeapon = hydra70,  0.00, 0.90, 2.54,   0.0, 0.0, true, 1, 0,-60,60, 0,25
; Add a weapon (filename must match the extensionless file in the weapons folder).
; Consecutively adding the same weapon (e.g., m134) is treated as a single weapon with multiple firing points.
; Parameter order: Weapon config name, Position (X,Y,Z), Rotation angles (Horizontal, Vertical), Usable by driver, Seat, Default horizontal angle, Min horizontal angle, Max horizontal angle, Min pitch angle, Max pitch angle.
;
; Seat: 1=Seat 1, 2=Seat 2, and so on.
; Parameter combination explanation:
;  true,2 → Usable by player in seat 2, usable by driver if seat 2 is empty.
;  false,2 → Only usable by player in seat 2.
;  false,1 → Only usable by driver (not recommended).
;  true,1 → Only usable by driver.
; Defaults to true,1 if omitted.
;
; AddTurretWeapon differs from AddWeapon only in that the firing point rotates with the turret.

AddSearchLight      = 0.71,  -0.02,  0.02,   0x50FFFFFF,   0x10FFFFC0,    60.0, 20.0,       0,   0
AddFixedSearchLight = 0.71,  -0.02,  0.02,   0x50FFFFFF,   0x10FFFFC0,    60.0, 20.0,       0,   0
AddSteeringSearchLight = -0.52,0.90, 1.76,   0x50FFFFFF,   0x00FFFFC0,    27.0, 15.0,       5,   0,     45
;AddSearchLight     = Coord X,Y,Z,   Start color, End color,  Distance, End radius, Horizontal angle, Pitch angle, Rudder angle
; AddSearchLight      : Dynamic searchlight (rotates with occupant's view).
; AddFixedSearchLight : Fixed-direction searchlight.
; AddSteeringSearchLight : Fixed light that rotates with wheel direction (rudder angle should match wheel turn angle).

AddPartLightHatch =  0.32, 0.23, 1.83,   -1,0,-0.024, 90
;AddPartLightHatch= Coord X,Y,Z, Rotation axis X,Y,Z, Rotation angle -1800~1800
; Add a part that only extends when the searchlight is turned on.
; ★Important: AddSearchLight or AddFixedSearchLight must be set first.

AddRecipe = " Y ",  "YXY",  " YD",  X, iron_block, Y, iron_ingot, D,dye,2
AddRecipe ="YXY", X, mcheli:ah-6, Y, redstone
AddShapelessRecipe = iron_block, iron_ingot, dye,2
; Add crafting recipe (multiple AddRecipe lines can add recipes).
; 3 characters inside "" correspond to horizontal arrangement on the crafting table.
; (Format same as Forge's GameRegistry.addRecipe)
; Detailed example:
; X = iron block name
; Y = iron ingot item name
; D = green dye item name (items with damage value need the damage value appended to the name)
; Item name reference: http://minecraft.gamepedia.com/Data_values
; Vanilla items can omit the "minecraft:" prefix.
; MOD items need to specify the MOD name (e.g., mcheli:ah-6).
; AddShapelessRecipe adds a shapeless crafting recipe.

FlareType = 1
; Flare/Decoy type:
; 0=None
; 1=Normal
; 2=For large aircraft
; 3=Sideways ejection
; 4=Forward ejection
; 5=Downward ejection
; 10=Tank smoke grenade

Float  = true
; Enable floating.

FloatOffset = -1.0
; Floating height offset (can be negative).

SubmergedDamageHeight = 2
; Water contact below this height does not cause damage (unit: blocks).

MaxHP = 100
; Hit Points / Durability.

ArmorDamageFactor = 0.5
; Vehicle damage coefficient (1.0=100%, 0.5=50%).

ArmorMinDamage = 5
; Minimum damage threshold (damage below this value is ignored).

ArmorMaxDamage = 500
; Maximum damage cap (damage exceeding this is calculated as this value).

InventorySize = 18
; Vehicle inventory size (must be a multiple of 9).

DamageFactor = 0.2
; Player damage coefficient (0.2=takes 20% damage).
; Note: Vehicle takes damage simultaneously when the player is damaged.

Sound = heli
; Sound file when throttle is increased (corresponds to sounds/heli.ogg).

UAV = true
SmallUAV = true
; true=Drone (cannot enter pilot seat).
; UAV=true: Large drone (cannot be controlled by handheld terminal).
; SmallUAV=true: Small drone (can be controlled by handheld terminal).
; Note: Drone control station can control all types, handheld terminal only for small drones.

TargetDrone = true
; Only for planes: true=Unmanned target drone (cannot enter pilot seat).
; Can only be spawned by a drone control station, automatically flies low altitude circles after spawning.

OnGroundPitch = angle
; Pitch angle when parked on ground (e.g., Zero fighter nose up on ground).

AddPartHatch = Position X,Y,Z, Rotation axis X,Y,Z, Rotation angle 0~180
; Add a hatch that opens/closes with Z key.
; Model naming: vehiclename_hatch?.obj (? starts from 0).
; If model is not found, it won't be displayed (model is optional if no display is needed).

AddPartSlideHatch = Movement X,Y,Z
; Add a sliding hatch (model naming rule same as AddPartHatch).

AddPartCamera = Coord X,Y,Z, Horizontal link, Pitch link
; Add a part that always faces the player.
; Model naming: vehiclename_camera?.obj.

AddPartRotation = 0.00, 9.00, -31.17,  0,-1,0,       1.3,      false
; AddPartRotation = Position X,Y,Z,        Rotation axis X,Y,Z,   Rotation speed,  Continuous rotation
; Add a periodically rotating part.

AddPartWeapon        = m230,       false, true, true,  -2.51,  1.29,  -1.51
AddPartWeapon        = m102_105mm, false, true, true,  -2.51,  1.29,  -1.51, 1.00
AddPartWeapon        = rehinmetall_apfsds / rehinmetall_he, false, true, false,  0.00, 2.10, 0.00, 0
AddPartTurretWeapon  = mg7_62mm,   false, true, true,  -0.83,  3.39,  -0.57, 0
AddPartRotWeapon     = m134_r50,   false, true, true,  -1.825, 1.475, -0.25, 1,0,0
AddPartWeaponChild   = false, true, 0.00, 0.5, 3.00
AddPartWeaponMissile = aim120,     false, false,false, -2.51,  1.29,  -1.51
; Helicopter/Plane weapon part settings.
; AddPartWeapon = Linked weapon name (none for none), Hidden in gunner mode?, Horizontal link, Pitch link, Rotation coord X,Y,Z, Recoil distance
; AddPartRotWeapon = Linked weapon name, Hidden in gunner mode?, Horizontal link, Pitch link, Rotation coord X,Y,Z, Rotation axis X,Y,Z
; AddPartWeaponChild = Horizontal link, Pitch link, Rotation coord X,Y,Z
; Changes with the weapon angle of AddWeapon (weapon names separated by /).
; Recoil distance is the backward movement for cannons.
; AddPartRotWeapon is for rotary barrel guns (rotates when firing).
; Model naming: vehiclename_weapon?.obj
;
; AddPartWeaponChild is added as a child part of AddPartWeapon.
; Must be defined immediately after AddPartWeapon.
; Model naming: vehiclename_weapon?_0.obj (? is the parent part number).
;
; AddPartWeaponMissile is hidden when the weapon is not ready (e.g., missiles/bombs).

AddPartWeaponBay = Weapon name, Position X,Y,Z, Rotation axis X,Y,Z, Rotation angle 0~180
; Add a rotating weapon bay.
AddPartSlideWeaponBay = Weapon name, Movement X,Y,Z
; Add a sliding weapon bay.
; Model naming: vehiclename_wb?.obj.

AddPartCanopy = Position X,Y,Z, Rotation axis X,Y,Z, Rotation angle 0~180
; Add a rotating canopy.
AddPartSlideCanopy = Movement X,Y,Z
; Add a sliding canopy.
; Model naming: vehiclename_canopy?.obj (can add multiple).
; Compatibility note: Omitting the number defaults to _canopy0.obj.

AddPartThrottle = Position X,Y,Z,  Rotation axis X,Y,Z,  Rotation angle 0~180,  Movement X,Y,Z
; Add a part that rotates/moves linked to the throttle.
; Items before rotation angle are required.

AddPartLG = Position X,Y,Z, Rotation axis X,Y,Z, Rotation angle 0~180 [, Rotation axis X,Y,Z, Rotation angle 0~180]
AddPartLGRev = Position X,Y,Z, Rotation axis X,Y,Z, Rotation angle 0~180 [, Rotation axis X,Y,Z, Rotation angle 0~180]
AddPartSlideRotLG = Movement X,Y,Z,  Position X,Y,Z, Rotation axis X,Y,Z, Rotation angle 0~180
AddPartLGHatch = Position X,Y,Z, Rotation axis X,Y,Z, Rotation angle 0~180 [, Rotation axis X,Y,Z, Rotation angle 0~180]
; Add landing gear (automatically retracts when taking off).
; Model naming: vehiclename_lg?.obj.
; AddPartLGRev has the opposite action of AddPartLG.
; AddPartLGHatch only opens when the landing gear is folding/unfolding.
;
; Action explanation:
; AddPartLG      Retract: 0°→90°.
; AddPartLGRev   Retract: 90°→0°.
; AddPartSlideRotLG Retract: 0°→90°.
; AddPartLGHatch Retract: 0°→90°→0°.

TrackRollerRot = 30
; Track roller rotation speed (negative value reverses, but not recommended).

AddTrackRoller = -1.72,  0.77,  5.04
; Add a track roller (only coordinates needed, X negative=right side, positive=left side).
; Can be set independently of tracks.

AddCrawlerTrack = false, 0.37, -2.09,  1.03/-3.41, 0.72/-3.57, 0.37/-3.42, -0.15/-2.55, -0.25/-2.16, -0.25/3.88, -0.13/4.21, 0.52/5.29, 0.78/5.39, 1.03/5.28, 1.10/5.04, 1.15/-3.12
;AddCrawlerTrack = Track direction,  Segment spacing, Track X pos, Rotation point Y/Z, ...
; Adjust the direction parameter if track movement is abnormal.
; Test mode in the game shows set positions with red/blue points.

PartWheelRot = 40
; Wheel rotation speed (higher value = faster).

AddPartWheel     = -1.05, 0.157, 1.965,  30
; Add wheel     X,Y,Z coord,  Max turn angle.
AddPartWheel     =  0.68,  0.19,  1.20,  30,   0.0, 1.0, 0.2,   0.68, 0.19, 0.70
; Add wheel     X,Y,Z coord,  Turn angle, Rotation axis X,Y,Z,    Rotation position X,Y,Z.
; Default rotation axis is (0,1,0) if omitted.

AddPartSteeringWheel =  -0.54, 0.88,  0.48,   0.0,     1.0, -1.7,  130
; Add steering wheel        X,Y,Z coord,   Rotation axis X,Y,Z,   Max rotation angle.

ThrottleUpDown = 1.0
ThrottleUpDownOnEntity = 2.0
; Throttle response coefficient (lower value = slower takeoff).
;
; ThrottleUpDownOnEntity is the response coefficient when the vehicle is carried on another entity (default 2.0).
; Calculation formula:
; ThrottleUpDown * Carrying entity speed * ThrottleUpDownOnEntity → Throttle sensitivity.
; Example: When ThrottleUpDownOnEntity=2.0 and carried on a minecart (max speed≈1.7)
;       1.7 * 2.0=3.4 → Only 1/3 the distance is needed to take off.

AutoPilotRot = -0.4
; Auto-turn angle (higher value = smaller turn radius).
; 0=Straight.
; Negative value=Left turn, Positive value=Right turn.

ConcurrentGunnerMode = true
; Allow entering gunner mode even when the 2nd seat is occupied.

Regeneration = true
; Occupants in the 2nd seat and beyond automatically regenerate health.

ParticlesScale = 0.1
; Size of particle effects like dust (higher value = more noticeable effect).

FuelSupplyRange = 25
; Range for supplying fuel to other vehicles (unit: meters).
; Own fuel is not consumed when supplying.
; Cannot supply itself.

AmmoSupplyRange = 35
; Range for supplying ammo to other vehicles (unit: meters).
; Own ammo is not consumed when supplying.
; Cannot supply itself.

MaxFuel         = 600
; Maximum fuel capacity.
FuelConsumption = 0.5
; Fuel consumption per second.
; Endurance (seconds) = Max fuel capacity / Consumption per second.
; 600 / 0.5 = 1200 seconds.

Stealth = 0.5
; Stealth (0.0~1.0, default 0.0).
; Higher value = harder to lock onto by missiles (increases lock time, shortens lock range).

SmoothShading = false
; Smooth shading toggle.
; false=Flat shading (sharp edges).
; true=Smooth shading (softened edges).
; Note: SmoothShading=false in mcheli.cfg disables smooth shading globally.

HideEntity = false
; Whether to hide occupant models.
; true=Hide.
; false=Show.

EntityWidth  = 0.9
EntityHeight = 0.9
; Occupant model render size (width/height, range -100.0~100.0).
; 0.5=Half size.

EntityPitch = 45
EntityRoll  = 20
; Occupant model render angle (-360~360).

CanRide = false
; Whether riding is allowed.
; true=Allowed (default).
; false=Prohibited.

BoundingBox =  Collision box center X,Y,Z,  Width, Height, Damage multiplier
; Add a collision box.
; Only affected by this MOD's machine guns/missiles.
; Does not collide with blocks/entities.
; Enable TestMode in MOD options to display.
; Damage multiplier defaults to 1.0 (0.5=half damage, 3.0=triple damage).

Category = W.A
; Vehicle category (only used for creative mode inventory sorting).

CanMoveOnGround = false
CanRotOnGround  = false
; Ground movement/rotation prohibition.
;  CanMoveOnGround: Prohibit ground movement.
;  CanRotOnGround: Prohibit ground rotation.

EnableParachuting = true
; Enable parachuting (only for players in seat 3 and beyond, press Space to parachute).
MobDropOption  = 0.0, 0.0, -11.5,  10
; Occupant drop settings = Drop point X,Y,Z, Drop interval (1/20 second).

RotorSpeed = 50.0
; Rotor speed (higher value = faster, negative value reverses but not recommended).

;***********************************************************************************
■ Helicopter Exclusive Settings
;***********************************************************************************

;Requires four files (all lowercase):
;  helicopters folder: Configuration file.
;  models/helicopters: Model.
;  textures/helicopters: Texture.
;  textures/items: Item texture.

EnableFoldBlade = false
; Rotor blade folding function (true=enabled).

AddRotor= 6, 60,  0.00,  3.35,  0.00,  0.0, 1.0, 0.0, true
AddRotor= 2, 60,  0.50,  1.90, -6.55,  1.0, 0.0, 0.0
; Add rotor (unlimited number).
; First one in this example is main rotor, second is tail rotor.
; Only the first rotor can be folded.
; Parameters: Number of blades, Angle between blades, Position X,Y,Z, Rotation axis X,Y,Z, Foldable.
; Model naming: vehiclename_rotor?.obj.
;
; ※ Legacy AddRotorOld is deprecated.

AddRepellingHook =  0.60, 2.75, -14.21, 30
; Rappelling hook settings = Hook coordinates X,Y,Z, Deployment interval.

;***********************************************************************************
■ Fighter Plane Exclusive Settings
;***********************************************************************************

;Requires four files (all lowercase):
;  planes folder: Configuration file.
;  models/planes: Model.
;  textures/planes: Texture.
;  textures/items: Item texture.

AddPartRotor = Position X,Y,Z, Rotation axis X,Y,Z, Rotation angle (-180~180)
; Add rotor (rotates during VTOL).
; Model naming: vehiclename_rotor?.obj.
AddBlade = Number of blades, Angle between blades, Position X,Y,Z, Rotation axis X,Y,Z
; Must be added after AddPartRotor.
; Model naming: vehiclename_blade?.obj.

AddPartWing = Position X,Y,Z, Rotation axis X,Y,Z, Rotation angle 0~180
; Add foldable main wing.
; Model naming: vehiclename_wing?.obj.
AddPartPylon = Position X,Y,Z, Rotation axis X,Y,Z, Rotation angle 0~180
; Add foldable pylon.
; Model naming: vehiclename_wing?_pylon?.obj.
; Must be added after AddPartWing.
; Example:
; AddPartWing  → Model: vehiclename_wing0.obj
; AddPartPylon → Model: vehiclename_wing0_pylon0.obj / wing0_pylon1.obj

PivotTurnThrottle = 0.0
; Movement amount during ground turning.
; 0=Pivot turn, >0=Turn while moving.
; Tank setting suggestions:
;  Neutral steer=0
;  Skid steer>0

EnableBack = true
; Allow reversing.

VariableSweepWing = true
SweepWingSpeed = 1.2
; Variable-sweep wing settings (requires AddPartWing).
; VariableSweepWing=true: Wing can be adjusted in air.
; SweepWingSpeed=1.2: Speed when wings are folded.

AddPartNozzle = Position X,Y,Z, Rotation axis X,Y,Z, Rotation angle 0~180
; Add engine nozzle (rotates during VTOL).
; Model naming: vehiclename_nozzle?.obj.
; Particle size controlled by ParticlesScale.

EnableVtol = true
; Whether to enable VTOL functionality.
DefaultVtol = true
; Default state when VTOL is enabled (true=VTOL automatically enabled on ground).
VtolYaw = 0.3
; Horizontal turn amount in VTOL mode.
VtolPitch = 0.3
; Pitch turn amount in VTOL mode.

EnableEjectionSeat = true
; Ejection seat toggle.
; true=Adds ejection seat button to GUI.
; 1-seat vehicles support 1, 2-seat vehicles support 2.

AddParticleSplash  =  1.0,  0.97,   13.19,      3,     9.0,   1.1,        20, 0.30, -0.03
;AddParticleSplash = Coord X,Y,Z,  Particle count,  Size,  Speed,  Duration, Rise speed, Gravity
; Generate splash particles when moving on water.
; Unrelated to EnableSeaSurfaceParticle.

EnableSeaSurfaceParticle = true
; Whether to generate splashes when flying over sea surface.
; Size affected by ParticlesScale (recommended 0.7).
; Note: Unrelated to AddParticleSplash.

;***********************************************************************************
■ Ground Vehicle Exclusive Settings
;***********************************************************************************

;Requires four files (all lowercase):
;  vehicles folder: Configuration file.
;  models/vehicles: Model.
;  textures/vehicles: Texture.
;  textures/items: Item texture.

AddPart = Parameter 1, Parameter 2, Parameter 3, Parameter 4, Position X,Y,Z
; Add a part that rotates with the player.
; Parameter 1: Hide in first-person view? (true=show).
; Parameter 2: Link horizontally? (true=link).
; Parameter 3: Link with pitch? (true=link).
; Parameter 4: Part type (0=Normal,1=Rotate when firing,2=Recoil when firing).
; Model naming: vehiclename_part?.obj.
AddChildPart = Parameter 1, Parameter 2, Parameter 3, Parameter 4, Position X,Y,Z
; Add a child part (must be after AddPart).
; Model naming: vehiclename_part?_#.obj (# starts from 0).
; Example:
; AddPart     → vehiclename_part0.obj
; AddChildPart → vehiclename_part0_0.obj / part0_1.obj

; RotationPitchMax/Min are legacy parameters, do not use.

;***********************************************************************************
■ Tank Exclusive Settings
;***********************************************************************************

;Requires four files (all lowercase):
;  tanks folder: Configuration file.
;  models/tanks: Model.
;  textures/tanks: Texture.
;  textures/items: Item texture.

DefaultFreelook = true
; Enable free look immediately after entering vehicle (mainly for tanks).

OnGroundPitchFactor = 2.0
OnGroundRollFactor  = 1.3
; Terrain adaptation tilt speed.
; Higher value = faster tilting.
; Recommended higher for fast vehicles, lower for slow vehicles.
; Too high causes screen shake, too low gets stuck in blocks.

CameraRotationSpeed = 25
; Camera rotation speed (for tanks, can be used to limit turret rotation speed).

WeightType = Tank
; Weight type: Tank / Car / Unknown.
; Tank: No self-damage when hitting mobs, destroys more blocks.
; Car: Self-damage when hitting mobs, destroys fewer blocks.
; Block destruction rules set in mcheli.cfg.

WeightedCenterZ = 0.0
; Center of gravity Z coordinate (affects terrain adaptation tilt).
; ※ Effect is unstable, disable if unsuitable.

SetWheelPos =  1.75,  -0.24,  4.85, 3.02, 1.44, -1.54, -2.91
;SetWheelPos =  X coord, Y coord,  Z coord1, Z coord2...
; Set ground contact points (vehicle tilts based on these).
; Negative X values need not be set.
; ★ Y coordinate strongly recommended to be fixed at -0.24.

======================================Updated 2025.10.8, parameters below are added by MCHeli-Reforged=====================================
    /**
     * Radar Type
     */
radarType = MODERN_AA
;Modern Anti-Air Radar
radarType = EARLY_AA
;Early Anti-Air Radar
radarType = MODERN_AS
;Modern Air-to-Surface Radar
radarType = EARLY_AS
;Early Air-to-Surface Radar
;Default=None

The above radar parameters are case-sensitive.

nameOnModernAARadar = "?"
;Name displayed for this vehicle on Modern Anti-Air Radar.
;Default=?

nameOnEarlyAARadar = "?"
;Name displayed for this vehicle on Early Anti-Air Radar.
;Default=?

nameOnModernASRadar = "?"
;Name displayed for this vehicle on Modern Air-to-Surface Radar.
;Default=?

nameOnEarlyASRadar = "?"
;Name displayed for this vehicle on Early Air-to-Surface Radar.
;Default=?

explosionSizeByCrash = 5
;Explosion radius when the vehicle is destroyed.
;Default=5

throttleDownFactor = 1
;Reverse speed multiplier (Recommended value 3, so reverse speed is about half of forward speed. But reverse speed is also affected by motionfactor).
;Default=1

haschaff = false
;Whether it has chaff.
;Default=false

chaffUseTime = 100
;Chaff effect duration.
;Default=None

chaffWaitTime = 400
;Chaff cooldown duration.
;Default=None

hasmaintenance = false
;Whether it has a maintenance system.
;Default=false

maintenanceUseTime = 20
;Maintenance system effect duration (Duration is the health regeneration percentage).
;Default=None

maintenanceWaitTime = 300
;Maintenance system cooldown duration.
;Default=None

engineShutdownThreshold = 20
;Vehicle crippling threshold, engine shuts down if health falls below this percentage.
;Default=None

hasaps = false
;Whether it has an Active Protection System.
;Default=false

apsUseTime = 100
;APS effect duration (When active, can intercept Rocket and Missile type weapons).
;Default=100

apsWaitTime = 400
;APS cooldown duration.
;Default=400

apsRange = 8
;APS range.
;Default=8

enableRWR = false
;Whether it has a Radar Warning Receiver.
;Default=false

hudType = 0
;HUD custom field, used to indicate the vehicle's HUD.
;Default=None

weaponGroupType = 0
;HUD custom field, used to indicate the vehicle's weaponGroupType.
;Default=None

armorExplosionDamageMultiplier = 1.0
;Vehicle explosion damage multiplier, final explosion damage = explosion damage * explosion multiplier.
;Default=1

;Currently, MCH-R supports the original MCH collision boxes and adds two new types. The syntax is as follows:
;The first type is BoundingBox = {center_x}, {center_y}, {center_z}, {width}, {height}, {length}, {multiplier}, DEFAULT, {name}
;It generates a DEFAULT type collision box centered at center_x,center_y,center_z, with width, height, length as specified, damage multiplier, and hit display name.
;DEFAULT type collision boxes do not rotate with the turret. Example:
BoundingBox = 0.0, 1.21, 3.6, 4.684, 0.8871, 1.5, 0.4, DEFAULT, Upper Glacis
;This example creates a normal collision box at 0.0, 1.21, 3.6, with width/height/length 4.684, 0.8871, 1.5, damage multiplier 0.4, showing hit location as 'Upper Glacis'.

;The second type is BoundingBox = {center_x}, {center_y}, {center_z}, {width}, {height}, {length}, {multiplier}, TURRET, {name}
;It generates a TURRET type collision box centered at center_x,center_y,center_z, with width, height, length, damage multiplier, and hit display name.
;TURRET type collision boxes rotate with the turret around 0,y,0 (we recommend setting the turret rotation position as 0,y,0). Example:
BoundingBox = 0.0, 2.16, -0.44, 4.1, 1, 5.4, 0.4, TURRET, Turret Front
;This example creates a turret collision box at 0.0, 2.16, -0.44, with width/height/length 4.1, 1, 5.4, damage multiplier 0.4, showing hit location as 'Turret Front', which rotates with the turret.