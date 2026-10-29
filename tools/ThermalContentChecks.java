import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.net.URLClassLoader;
import java.util.*;
import java.lang.reflect.*;
import mcheli.MCH_ThermalProfiles;

/** Optional content migration check using both mods' real profile parsers. */
public final class ThermalContentChecks {
    private static int checks;
    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
        checks++;
    }
    public static void main(String[] args) throws Exception {
        Path root = Paths.get(args[0]);
        Path assets = root.resolve("mods/MCHELI-R-WDG-Edition-1.1.8_X190/assets/mcheli");
        int vehicles = 0, enabled = 0;
        Map<String, MCH_ThermalProfiles> devices = new HashMap<>();
        try (URLClassLoader fmur = new URLClassLoader(new java.net.URL[]{Paths.get(args[1]).toUri().toURL()},
                ThermalContentChecks.class.getClassLoader())) {
            Class<?> profile = fmur.loadClass("com.flansmod.common.guns.ThermalProfile");
            Constructor<?> parse = profile.getConstructor(Map.class);
            for (String category : new String[]{"helicopters", "planes", "tanks", "vehicles"}) {
                try (java.util.stream.Stream<Path> files = Files.walk(assets.resolve(category))) {
                    for (Path file : (Iterable<Path>)files.filter(p -> p.toString().endsWith(".txt"))::iterator) {
                        MCH_ThermalProfiles device = new MCH_ThermalProfiles();
                        List<Boolean> access = new ArrayList<>();
                        boolean night = false;
                        for (String line : new String(Files.readAllBytes(file), StandardCharsets.UTF_8).split("\\r?\\n")) {
                            String[] pair = line.trim().split("\\s*=\\s*", 2);
                            if (pair.length != 2 || pair[0].startsWith(";")) continue;
                            if (pair[0].equalsIgnoreCase("EnableNightVision")) night = Boolean.parseBoolean(pair[1]);
                            if (pair[0].equalsIgnoreCase("SeatThermalEnabled")) access.add(Boolean.parseBoolean(pair[1].split(",")[1]));
                            device.read(pair[0], pair[1]);
                        }
                        vehicles++;
                        boolean available = device.isEnabled(0, -1);
                        if (available) enabled++;
                        check(!night, "Vehicle night vision is disabled: " + file);
                        for (int seat = -1; seat < Math.max(1, access.size()); seat++) {
                            Map<String, String> values = device.resolve(0, seat);
                            check(!values.containsKey("ThermalEnabled"), "Capability flag is not passed to FMUR");
                            parse.newInstance(values);
                            check(device.isEnabled(0, seat) == (seat >= 0 && seat < access.size() ? access.get(seat) : available),
                                    "Seat capability resolves correctly: " + file + " seat=" + seat);
                            if (device.isEnabled(0, seat)) check(values.size() == 8, "Enabled device has explicit imaging fields: " + file);
                        }
                        devices.put(file.getFileName().toString(), device);
                    }
                }
            }
            check(vehicles == 64 && enabled == 40, "Complete reviewed 64-vehicle inventory");
            check(devices.get("ea18g.txt").isEnabled(0, 0), "ATFLIR-equipped Growler has thermals");
            check(!devices.get("j16d.txt").isEnabled(0, 0), "J-16D has no unconfirmed thermal payload");
            check(!devices.get("m1161itv.txt").isEnabled(0, 0), "M1161 never gains thermal vision");
            check(!devices.get("m1a1.txt").isEnabled(0, 1), "M1A1 open commander station is not a thermal sensor");
            check(devices.get("m1a2.txt").isEnabled(0, 1), "M1A2 commander sensor is available");
            Map<String, String> a1 = devices.get("m1a1.txt").resolve(0, 0), a2 = devices.get("m1a2.txt").resolve(0, 0);
            check(Integer.parseInt(a2.get("ThermalMaxFPS")) > Integer.parseInt(a1.get("ThermalMaxFPS")), "M1A2 has higher sensor cadence");
            check(Integer.parseInt(a2.get("ThermalResolution").split(" ")[0]) > Integer.parseInt(a1.get("ThermalResolution").split(" ")[0]), "M1A2 has greater sensor detail");
            check(Float.parseFloat(a2.get("ThermalGrain")) < Float.parseFloat(a1.get("ThermalGrain")), "M1A2 has less noise");
            for (String item : new String[]{"attachments/SCOPE_CNVD-T[3x].txt", "guns/GEN_Tactical Visor.txt"}) {
                Map<String, String> values = new HashMap<>();
                for (String line : Files.readAllLines(root.resolve("Flan/BFMC Gun Pack/" + item), StandardCharsets.UTF_8)) {
                    String[] pair = line.trim().split("\\s+", 2);
                    if (pair.length == 2 && pair[0].matches("Thermal(MaxFPS|Resolution|Grain|Palette|Contrast|Sensitivity|Bloom|Gamma)"))
                        values.put(pair[0], pair[1]);
                }
                parse.newInstance(values);
                check(values.size() == 8, "FMUR device has all eight thermal fields: " + item);
                if (item.startsWith("attachments/")) check("640 480".equals(values.get("ThermalResolution")), "Scope resolution is doubled");
            }
        }
        System.out.println("PASS: " + checks + " real-parser content checks, 64 vehicles (40 enabled), 2 FMUR devices.");
    }
}
