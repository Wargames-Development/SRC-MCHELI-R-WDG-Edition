package mcheli.economy;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ChatComponentText;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TimeZone;

public final class MCH_EconomyService {

    private static final String ROOT_TAG = "MCH_Economy";
    private static final String KEY_SL = "SL";
    private static final String KEY_GE = "GE";
    private static final String KEY_RP = "RP";
    private static final String KEY_DAILY_DAY = "DailyDay";
    private static final String KEY_DAILY_SL = "DailySL";
    private static final String KEY_DAILY_GE = "DailyGE";
    private static final String KEY_DAILY_RP = "DailyRP";
    private static final float MIN_DAMAGE_SHARE = 0.25F;
    private static final int REPEAT_KILL_DECAY_WINDOW_TICKS = 600;
    private static final float REPEAT_KILL_DECAY_FACTOR = 0.5F;
    private static final int DAILY_SOFT_CAP_SL = 120000;
    private static final int DAILY_SOFT_CAP_GE = 20;
    private static final int DAILY_SOFT_CAP_RP = 60000;
    private static final int DAMAGE_TRACKER_EXPIRE_TICKS = 1200;
    private static final Map<String, DamageTracker> DAMAGE_TRACKERS = new HashMap<String, DamageTracker>();
    private static final Map<String, Long> LAST_KILL_TICK = new HashMap<String, Long>();

    private MCH_EconomyService() {
    }

    private static NBTTagCompound getOrCreateEconomyTag(EntityPlayer player) {
        NBTTagCompound root = player.getEntityData();
        if (!root.hasKey(ROOT_TAG)) {
            root.setTag(ROOT_TAG, new NBTTagCompound());
        }
        return root.getCompoundTag(ROOT_TAG);
    }

    private static int safeAdd(int current, int delta) {
        long result = (long) current + (long) delta;
        if (result < 0L) {
            return 0;
        }
        if (result > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return (int) result;
    }

    public static int getSL(EntityPlayer player) {
        return getOrCreateEconomyTag(player).getInteger(KEY_SL);
    }

    public static int getGE(EntityPlayer player) {
        return getOrCreateEconomyTag(player).getInteger(KEY_GE);
    }

    public static int getRP(EntityPlayer player) {
        return getOrCreateEconomyTag(player).getInteger(KEY_RP);
    }

    public static void syncToClient(EntityPlayerMP player) {
        if (player == null) {
            return;
        }
        NBTTagCompound tag = getOrCreateEconomyTag(player);
        MCH_PacketNotifyEconomySync.sendToPlayer(
            player,
            tag.getInteger(KEY_SL),
            tag.getInteger(KEY_GE),
            tag.getInteger(KEY_RP),
            MCH_EconomyTechService.getUnlockedNodesRaw(player)
        );
    }

    public static void recordDamage(EntityPlayerMP player, String targetKey, float damage, float targetMaxDamage, long worldTick) {
        if (player == null || targetKey == null || targetKey.isEmpty()) {
            return;
        }
        float appliedDamage = Math.max(0.0F, damage);
        if (appliedDamage <= 0.0F) {
            return;
        }

        cleanupExpiredDamageTracker(worldTick);
        DamageTracker tracker = DAMAGE_TRACKERS.get(targetKey);
        if (tracker == null) {
            tracker = new DamageTracker();
            DAMAGE_TRACKERS.put(targetKey, tracker);
        }
        tracker.lastUpdateTick = worldTick;
        tracker.totalDamage += appliedDamage;
        tracker.maxDamage = Math.max(tracker.maxDamage, Math.max(0.0F, targetMaxDamage));
        String uuid = player.getUniqueID().toString();
        Float current = tracker.playerDamage.get(uuid);
        tracker.playerDamage.put(uuid, current == null ? appliedDamage : current + appliedDamage);
    }

    public static boolean passDamageShare(EntityPlayerMP player, String targetKey, boolean consumeTracker) {
        if (player == null || targetKey == null || targetKey.isEmpty()) {
            return true;
        }
        DamageTracker tracker = consumeTracker ? DAMAGE_TRACKERS.remove(targetKey) : DAMAGE_TRACKERS.get(targetKey);
        if (tracker == null) {
            return true;
        }
        String uuid = player.getUniqueID().toString();
        Float playerDamage = tracker.playerDamage.get(uuid);
        if (playerDamage == null || playerDamage.floatValue() <= 0.0F) {
            return false;
        }
        float denominator = tracker.totalDamage;
        if (tracker.maxDamage > denominator) {
            denominator = tracker.maxDamage;
        }
        if (denominator <= 0.0F) {
            return true;
        }
        float share = playerDamage.floatValue() / denominator;
        return share >= MIN_DAMAGE_SHARE;
    }

    public static void clearDamageTracker(String targetKey) {
        if (targetKey == null || targetKey.isEmpty()) {
            return;
        }
        DAMAGE_TRACKERS.remove(targetKey);
    }

    public static GrantResult grantWithPolicy(EntityPlayerMP player, int sl, int ge, int rp, String reason, String repeatKey, long worldTick) {
        return grantWithPolicy(player, sl, ge, rp, reason, repeatKey, worldTick, true);
    }

    public static GrantResult grantWithPolicy(EntityPlayerMP player, int sl, int ge, int rp, String reason, String repeatKey, long worldTick, boolean sendChatMessage) {
        int addSL = Math.max(0, sl);
        int addGE = Math.max(0, ge);
        int addRP = Math.max(0, rp);
        if (addSL == 0 && addGE == 0 && addRP == 0) {
            return GrantResult.ZERO;
        }

        float repeatFactor = getRepeatDecayFactor(player, repeatKey, worldTick);
        if (repeatFactor < 1.0F) {
            addSL = Math.max(0, Math.round((float) addSL * repeatFactor));
            addGE = Math.max(0, Math.round((float) addGE * repeatFactor));
            addRP = Math.max(0, Math.round((float) addRP * repeatFactor));
        }

        NBTTagCompound tag = getOrCreateEconomyTag(player);
        resetDailyCounterIfNeeded(tag);
        addSL = applyDailyCap(tag, KEY_DAILY_SL, addSL, DAILY_SOFT_CAP_SL);
        addGE = applyDailyCap(tag, KEY_DAILY_GE, addGE, DAILY_SOFT_CAP_GE);
        addRP = applyDailyCap(tag, KEY_DAILY_RP, addRP, DAILY_SOFT_CAP_RP);
        if (addSL == 0 && addGE == 0 && addRP == 0) {
            return GrantResult.ZERO;
        }

        String finalReason = reason;
        if (repeatFactor < 1.0F) {
            finalReason = (reason == null ? "" : reason) + " x" + String.format(java.util.Locale.ROOT, "%.2f", repeatFactor);
        }
        return grant(player, addSL, addGE, addRP, finalReason, sendChatMessage);
    }

    public static GrantResult grant(EntityPlayerMP player, int sl, int ge, int rp, String reason) {
        return grant(player, sl, ge, rp, reason, true);
    }

    public static GrantResult grant(EntityPlayerMP player, int sl, int ge, int rp, String reason, boolean sendChatMessage) {
        int addSL = Math.max(0, sl);
        int addGE = Math.max(0, ge);
        int addRP = Math.max(0, rp);
        if (addSL == 0 && addGE == 0 && addRP == 0) {
            return GrantResult.ZERO;
        }

        NBTTagCompound tag = getOrCreateEconomyTag(player);
        int nextSL = safeAdd(tag.getInteger(KEY_SL), addSL);
        int nextGE = safeAdd(tag.getInteger(KEY_GE), addGE);
        int nextRP = safeAdd(tag.getInteger(KEY_RP), addRP);
        tag.setInteger(KEY_SL, nextSL);
        tag.setInteger(KEY_GE, nextGE);
        tag.setInteger(KEY_RP, nextRP);

        if (sendChatMessage) {
            StringBuilder sb = new StringBuilder("[Economy] ");
            boolean appended = false;
            if (addSL > 0) {
                sb.append("+").append(addSL).append(" SL");
                appended = true;
            }
            if (addGE > 0) {
                if (appended) sb.append("  ");
                sb.append("+").append(addGE).append(" GE");
                appended = true;
            }
            if (addRP > 0) {
                if (appended) sb.append("  ");
                sb.append("+").append(addRP).append(" RP");
                appended = true;
            }
            if (reason != null && !reason.isEmpty()) {
                sb.append("  (").append(reason).append(")");
            }
            player.addChatMessage(new ChatComponentText(sb.toString()));
        }
        syncToClient(player);
        return new GrantResult(addSL, addGE, addRP);
    }

    public static boolean trySpend(EntityPlayerMP player, int sl, int ge, int rp, String reason) {
        if (player == null) {
            return false;
        }
        int needSL = Math.max(0, sl);
        int needGE = Math.max(0, ge);
        int needRP = Math.max(0, rp);
        if (needSL == 0 && needGE == 0 && needRP == 0) {
            return true;
        }
        NBTTagCompound tag = getOrCreateEconomyTag(player);
        int curSL = Math.max(0, tag.getInteger(KEY_SL));
        int curGE = Math.max(0, tag.getInteger(KEY_GE));
        int curRP = Math.max(0, tag.getInteger(KEY_RP));
        if (curSL < needSL || curGE < needGE || curRP < needRP) {
            return false;
        }
        tag.setInteger(KEY_SL, curSL - needSL);
        tag.setInteger(KEY_GE, curGE - needGE);
        tag.setInteger(KEY_RP, curRP - needRP);

        StringBuilder sb = new StringBuilder("[Economy] ");
        boolean appended = false;
        if (needSL > 0) {
            sb.append("-").append(needSL).append(" SL");
            appended = true;
        }
        if (needGE > 0) {
            if (appended) sb.append("  ");
            sb.append("-").append(needGE).append(" GE");
            appended = true;
        }
        if (needRP > 0) {
            if (appended) sb.append("  ");
            sb.append("-").append(needRP).append(" RP");
            appended = true;
        }
        if (reason != null && !reason.isEmpty()) {
            sb.append("  (").append(reason).append(")");
        }
        player.addChatMessage(new ChatComponentText(sb.toString()));
        syncToClient(player);
        return true;
    }

    private static int getCurrentUtcDay() {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        return cal.get(Calendar.YEAR) * 1000 + cal.get(Calendar.DAY_OF_YEAR);
    }

    private static void resetDailyCounterIfNeeded(NBTTagCompound tag) {
        int today = getCurrentUtcDay();
        if (tag.getInteger(KEY_DAILY_DAY) != today) {
            tag.setInteger(KEY_DAILY_DAY, today);
            tag.setInteger(KEY_DAILY_SL, 0);
            tag.setInteger(KEY_DAILY_GE, 0);
            tag.setInteger(KEY_DAILY_RP, 0);
        }
    }

    private static int applyDailyCap(NBTTagCompound tag, String dailyKey, int delta, int cap) {
        if (delta <= 0) {
            return 0;
        }
        if (cap <= 0) {
            int next = safeAdd(tag.getInteger(dailyKey), delta);
            tag.setInteger(dailyKey, next);
            return delta;
        }
        int current = Math.max(0, tag.getInteger(dailyKey));
        int remain = cap - current;
        if (remain <= 0) {
            return 0;
        }
        int granted = Math.min(delta, remain);
        tag.setInteger(dailyKey, safeAdd(current, granted));
        return granted;
    }

    private static float getRepeatDecayFactor(EntityPlayerMP player, String repeatKey, long worldTick) {
        if (player == null || repeatKey == null || repeatKey.isEmpty()) {
            return 1.0F;
        }
        String key = player.getUniqueID().toString() + "|" + repeatKey;
        Long last = LAST_KILL_TICK.get(key);
        LAST_KILL_TICK.put(key, worldTick);
        if (last == null) {
            return 1.0F;
        }
        return worldTick - last.longValue() <= REPEAT_KILL_DECAY_WINDOW_TICKS ? REPEAT_KILL_DECAY_FACTOR : 1.0F;
    }

    private static void cleanupExpiredDamageTracker(long worldTick) {
        Iterator<Map.Entry<String, DamageTracker>> it = DAMAGE_TRACKERS.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, DamageTracker> entry = it.next();
            if (worldTick - entry.getValue().lastUpdateTick > DAMAGE_TRACKER_EXPIRE_TICKS) {
                it.remove();
            }
        }
    }

    private static class DamageTracker {
        private final Map<String, Float> playerDamage = new HashMap<String, Float>();
        private float totalDamage = 0.0F;
        private float maxDamage = 0.0F;
        private long lastUpdateTick = 0L;
    }

    public static final class GrantResult {
        public static final GrantResult ZERO = new GrantResult(0, 0, 0);
        public final int sl;
        public final int ge;
        public final int rp;

        public GrantResult(int sl, int ge, int rp) {
            this.sl = Math.max(0, sl);
            this.ge = Math.max(0, ge);
            this.rp = Math.max(0, rp);
        }

        public boolean isZero() {
            return this.sl <= 0 && this.ge <= 0 && this.rp <= 0;
        }
    }
}
