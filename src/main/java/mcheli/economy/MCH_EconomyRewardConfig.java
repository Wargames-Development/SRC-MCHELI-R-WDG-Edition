package mcheli.economy;

import cpw.mods.fml.common.Loader;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class MCH_EconomyRewardConfig {

    private static final Reward DEFAULT_MOB = new Reward(20, 0, 8);
    private static final Reward DEFAULT_BOSS = new Reward(1800, 2, 600);
    private static volatile boolean loaded;
    private static volatile Reward defaultMobReward = DEFAULT_MOB;
    private static volatile Reward defaultBossReward = DEFAULT_BOSS;
    private static final Map<String, Reward> MOB_REWARDS = new HashMap<String, Reward>();
    private static final Map<String, Reward> VEHICLE_REWARDS = new HashMap<String, Reward>();

    private MCH_EconomyRewardConfig() {
    }

    public static synchronized void ensureLoaded() {
        if (loaded) {
            return;
        }
        loaded = true;
        defaultMobReward = DEFAULT_MOB;
        defaultBossReward = DEFAULT_BOSS;
        MOB_REWARDS.clear();
        VEHICLE_REWARDS.clear();
        File baseDir = new File(Loader.instance().getConfigDir(), "mcheli/economy");
        loadMobRewards(new File(baseDir, "mob_rewards.properties"));
        loadVehicleRewards(new File(baseDir, "vehicle_rewards.properties"));
    }

    public static Reward getDefaultMobReward() {
        ensureLoaded();
        return defaultMobReward;
    }

    public static Reward getDefaultBossReward() {
        ensureLoaded();
        return defaultBossReward;
    }

    public static Reward getMobReward(String mobKey) {
        ensureLoaded();
        if (mobKey == null) {
            return null;
        }
        return MOB_REWARDS.get(normalizeKey(mobKey));
    }

    public static Reward getVehicleReward(String vehicleKey) {
        ensureLoaded();
        if (vehicleKey == null) {
            return null;
        }
        return VEHICLE_REWARDS.get(normalizeKey(vehicleKey));
    }

    private static void loadMobRewards(File file) {
        if (file == null || !file.isFile()) {
            return;
        }
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(file));
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                int idx = line.indexOf('=');
                if (idx <= 0 || idx >= line.length() - 1) {
                    continue;
                }
                String key = normalizeKey(line.substring(0, idx));
                Reward reward = parseReward(line.substring(idx + 1));
                if (reward == null) {
                    continue;
                }
                if ("default".equals(key)) {
                    defaultMobReward = reward;
                } else if ("boss".equals(key)) {
                    defaultBossReward = reward;
                } else {
                    MOB_REWARDS.put(key, reward);
                }
            }
        } catch (IOException ignored) {
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    private static void loadVehicleRewards(File file) {
        if (file == null || !file.isFile()) {
            return;
        }
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(file));
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                int idx = line.indexOf('=');
                if (idx <= 0 || idx >= line.length() - 1) {
                    continue;
                }
                String key = normalizeKey(line.substring(0, idx));
                Reward reward = parseReward(line.substring(idx + 1));
                if (reward == null) {
                    continue;
                }
                VEHICLE_REWARDS.put(key, reward);
            }
        } catch (IOException ignored) {
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    private static Reward parseReward(String value) {
        if (value == null) {
            return null;
        }
        String[] arr = value.split(",");
        if (arr.length < 3) {
            return null;
        }
        try {
            int sl = Integer.parseInt(arr[0].trim());
            int ge = Integer.parseInt(arr[1].trim());
            int rp = Integer.parseInt(arr[2].trim());
            return new Reward(sl, ge, rp);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String normalizeKey(String key) {
        if (key == null) {
            return "";
        }
        return key.trim().toLowerCase(Locale.ROOT);
    }

    public static final class Reward {
        public final int sl;
        public final int ge;
        public final int rp;

        public Reward(int sl, int ge, int rp) {
            this.sl = Math.max(0, sl);
            this.ge = Math.max(0, ge);
            this.rp = Math.max(0, rp);
        }
    }
}
