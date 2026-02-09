from pathlib import Path

# ---------------------------
# ORDERED NON-PART / NON-WEAPON PARAMETERS
# (Weapon-related lines are now handled by ;Weapons order-preserved section)
# ---------------------------
ORDER_GROUPS = [
    ["DisplayName", "AddDisplayName", "MaxHp"],

    ["Category", "ItemID", "HUD", "ThirdPersonDist", "Speed",
     "MobilityPitch", "MobilityRoll", "MobilityYaw", "ThrottleUpDown",
     "MinRotationPitch", "MaxRotationPitch",
     "PivotTurnThrottle",
     "throttledownfactor", "StepHeight",
     "MobilityYawOnGround",
     "RotationPitchMin", "RotationPitchMax",
     "CanMoveOnGround", "DefaultFreelook"],

    ["Sound", "Float", "FloatOffset", "CanRotOnGround"],

    ["Gravity", "GravityInWater", "motionfactor", "MaxFuel",
     "FuelConsumption", "VariableSweepWing", "SweepWingSpeed"],

    ["CameraPosition", "CameraRotationSpeed", "CameraZoom",
     "EnableGunnerMode", "AutoPilotRot", "EnableNightVision",
     "EnableEntityRadar", "EnableEjectionSeat",
     "AddSeat", "AddGunnerSeat"],

    ["ParticlesScale", "EnableSeaSurfaceParticle",
     "AddParticleSplash", "AddPartNozzle", "AddExhaustFlame"],

    ["AddPartSlideweaponbay", "AddPartWeaponBay"],

    ["ArmorMinDamage", "DamageFactor", "engineShutdownThreshold",
     "explosionsizebycrash", "BoundingBox",
     "EntityWidth", "EntityHeight", "SubmergedDamageHeight"],

    ["FlareType", "hasDIRCM", "HasChaff", "ChaffUseTime",
     "ChaffWaitTime", "hasecmJammer", "ecmJammerType",
     "ecmJammerUseTime", "ecmJammerWaitTime",
     "HasPhotoelectricJammer",
     "hasaps", "apsUseTime", "apsWaitTime", "apsRange"],

    ["RadarType", "NameOnAdvancedAARadar",
     "NameOnModernAARadar", "NameOnEarlyAARadar",
     "EnableMortarRadar", "MortarRadarMaxDist",
     "NameOnModernASRadar", "NameOnEarlyASRadar",
     "enablerwr"],

    ["hasMaintenance", "MaintenceUsetime", "MaintenceWaittime"],

    ["MobilityYawOnGround", "OnGroundPitchFactor", "OnGroundRollFactor"],
]

# ---------------------------
# PARTS (ORDER MUST BE PRESERVED)
# ---------------------------
PART_KEYS = {
    "addpartlg",
    "addpartlgrev",
    "addpartsteeringwheel",
    "addpartwing",
    "addpartthrottle",
    "addpartcanopy",
    "addtrackroller",
    "addcrawlertrack",
    "trackrollerrot",
    "setwheelpos",
    "addrotor",
}

# ---------------------------
# WEAPONS (ORDER MUST BE PRESERVED to keep barrels/turrets paired)
# ---------------------------
WEAPON_KEYS = {
    "weapongrouptype",
    "addweapon",
    "addturretweapon",
    "addpartweapon",
    "addpartweaponchild",
    "addpartrotweapon",
    "addpartturretweapon",
    "addpartturretrotweapon",
    "addpartweaponmissile",
}

# ---------------------------
# DUPLICATE-ALLOWED (case-insensitive)
# (Duplicates section is still for non-Parts/non-Weapons params only.)
# ---------------------------
DUPLICATE_ALLOWED = {
    "adddisplayname",
    "boundingbox",
    "addseat",
    "addgunnerseat",

    # parts commonly repeat (but these are moved to ;Parts anyway)
    "addpartlg", "addpartlgrev", "addpartsteeringwheel", "addpartthrottle",
    "addexhaustflame", "addpartwing",

    # weapons commonly repeat (but these are moved to ;Weapons anyway)
    "addpartweapon", "addpartweaponchild", "addturretweapon",
    "addpartturretweapon", "addpartturretrotweapon",
    "addweapon", "addpartweaponmissile",
}

# ---------------------------
# HEADERS
# ---------------------------
MISC_HEADER = [";MISC", ";Place misc paramters here"]
PARTS_HEADER = ["", ";Parts"]
WEAPONS_HEADER = ["", ";Weapons"]
DUP_HEADER = ["", ";DUPLICATES"]  # no "Place duplicates here"

# Strip BOM / zero-width chars (fixes DisplayName -> MISC bugs)
STRIP_WEIRD_PREFIX = ("\ufeff", "\u200b", "\u200c", "\u200d", "\u2060")


def clean_key(name: str) -> str:
    n = name.strip()
    while n.startswith(STRIP_WEIRD_PREFIX):
        n = n[1:].lstrip()
    return n


def is_param_line(line: str) -> bool:
    s = line.strip()
    return bool(s) and not s.startswith(";") and "=" in s


def parse_param(line: str):
    left, right = line.split("=", 1)
    return clean_key(left), right.rstrip()


def process_file(path: Path) -> bool:
    original = path.read_text(encoding="utf-8", errors="replace").splitlines()

    occurrences = {}
    parts_lines = []
    weapon_lines = []
    misc_lines = []

    last_bucket = None  # "parts" | "weapons" | None  (for preserving blank lines within those blocks)

    for line in original:
        if is_param_line(line):
            name, value = parse_param(line)
            key = name.lower()
            formatted = f"{name} = {value.strip()}"

            if key in PART_KEYS:
                parts_lines.append(formatted)
                last_bucket = "parts"
            elif key in WEAPON_KEYS:
                weapon_lines.append(formatted)
                last_bucket = "weapons"
            else:
                occurrences.setdefault(key, []).append(formatted)
                last_bucket = None
        else:
            # Preserve blank lines only inside Parts/Weapons blocks to keep grouping readable
            if line.strip() == "":
                if last_bucket == "parts":
                    if parts_lines and parts_lines[-1] != "":
                        parts_lines.append("")
                elif last_bucket == "weapons":
                    if weapon_lines and weapon_lines[-1] != "":
                        weapon_lines.append("")
                continue

            # Non-empty comment/other lines -> go to misc bucket
            misc_lines.append(line)
            last_bucket = None

    # Build ordered output (non-parts, non-weapons)
    ordered_out = []
    duplicates_out = []

    for group in ORDER_GROUPS:
        group_used = False
        for key in group:
            kl = key.lower()
            vals = occurrences.get(kl, [])
            if not vals:
                continue

            if kl in DUPLICATE_ALLOWED:
                ordered_out.extend(vals)
            else:
                ordered_out.append(vals[0])
                if len(vals) > 1:
                    duplicates_out.extend(vals[1:])

            occurrences[kl] = []
            group_used = True

        if group_used:
            ordered_out.append("")

    # Trim trailing blank lines in ordered section
    while ordered_out and ordered_out[-1] == "":
        ordered_out.pop()

    # Anything not matched in ORDER_GROUPS goes to MISC
    unmatched = []
    for vals in occurrences.values():
        unmatched.extend(vals)

    # Compose final output
    out = []
    out.extend(ordered_out)

    if parts_lines:
        # Clean trailing blank lines inside parts block
        while parts_lines and parts_lines[-1] == "":
            parts_lines.pop()
        out.extend(PARTS_HEADER)
        out.extend(parts_lines)

    if weapon_lines:
        while weapon_lines and weapon_lines[-1] == "":
            weapon_lines.pop()
        out.extend(WEAPONS_HEADER)
        out.extend(weapon_lines)

    if out:
        out.append("")

    out.extend(MISC_HEADER)
    if unmatched:
        out.extend(unmatched)

    if misc_lines:
        out.append("")
        out.extend(misc_lines)

    out.extend(DUP_HEADER)
    if duplicates_out:
        out.extend(duplicates_out)

    new_text = "\n".join(out).rstrip() + "\n"
    old_text = "\n".join(original).rstrip() + "\n"

    if new_text == old_text:
        return False

    path.write_text(new_text, encoding="utf-8")
    print(f"[OK] {path}")
    return True


def main():
    root = Path(__file__).resolve().parent
    total = changed = 0

    for p in root.rglob("*.txt"):
        total += 1
        try:
            if process_file(p):
                changed += 1
        except Exception as e:
            print(f"[ERROR] {p}: {e}")

    print(f"Done. Processed {total}, changed {changed}.")


if __name__ == "__main__":
    main()
