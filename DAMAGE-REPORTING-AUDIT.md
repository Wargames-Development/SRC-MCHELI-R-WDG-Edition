# Vehicle damage reporting audit

Scope: the middle-right damage-dealt HUD, including handheld launcher hits on vehicles.

## Findings and corrections

- B1: Direct damage was reported only when `lastBBName` was non-null. A missile could remove hull HP without a report, while its blast produced a smaller visible report. Unnamed hits now report as "Direct hit".
- B2: Reports used floating-point requested damage before integer HP application and lethal-hit clamping. Both direct and explosion reports now measure HP immediately before and after the server applies damage, then convert the difference to a percentage of maximum HP.
- B3: The client ignored the packet's target ID and added percentages from different vehicles. Switching targets now starts a new total. A new report also resets the total after the old display expires.
- B4: The total truncated fractions and omitted units. The HUD now shows one decimal place and `% HP`, with a "Recent damage dealt" caption and fixed pixel spacing between detail rows.
- B5: Damage reports could use a previous attacker, and the direct-hit path cast an arbitrary player to `EntityPlayerMP`. Reports now use the current resolved attacker and resolve dummy players before checking for a multiplayer recipient.

The total describes recent damage dealt to the last reported target, not the shooter's health or the target's remaining health. Blast and direct-hit entries remain separate. A blast hitting multiple vehicles switches the display to the last reported vehicle instead of adding incompatible percentages.

## Review

ACCEPTABLE on source review. Damage amounts, armor, missile behavior, and HP application order are unchanged. Reports remain server-generated. No packet fields, IDs, field order, percentage units, NBT, or configuration formats changed. Existing unrelated launcher/RWR edits were left intact.

The disabled 3D damage-marker renderer and its separate packet are outside this HUD fix. No client or dedicated server was launched; runtime verification remains T1 below.

## T1: Manual regression checklist

Use a server and client running the updated code. Record target HP and maximum HP before and after each shot; compare the report with `100 * (HP before - HP after) / maximum HP`, rounded to one decimal place.

1. Fire a Stinger at a fresh helicopter and a Javelin at a fresh tank. Repeat against the main body and named extra hitboxes. Every HP loss from direct impact/blast should be reported; the recent total should match the combined percentage lost when only one target is involved.
2. Detonate nearby without a direct hit. The blast entry should match actual HP loss after explosion armor multipliers.
3. Hit an almost-destroyed vehicle. Report only the remaining HP removed, not excess damage beyond destruction.
4. Test armor-negated damage and ricochets. No positive damage should be reported for zero HP loss; ricochet text should remain visible.
5. Hit two vehicles in quick succession. The second vehicle starts a fresh total. Wait until the display disappears and hit again; no previous total should carry over.
6. Try repeated hits on one target, low resolution, and different GUI scales. Percentage totals should fit the right side and detail rows should not overlap.
7. Repeat launcher hits with a second player on a dedicated server. The attacking player receives reports; environmental damage must not credit a stale attacker.
