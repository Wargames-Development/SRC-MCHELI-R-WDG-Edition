package mcheli;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/** Content data only. FMUR owns sensor rendering and consumes immutable resolved maps. */
public final class MCH_ThermalProfiles {
    private static final String[] FIELDS = {"ThermalMaxFPS", "ThermalResolution", "ThermalGrain",
            "ThermalContrast", "ThermalSensitivity", "ThermalBloom", "ThermalGamma", "ThermalPalette", "ThermalEnabled"};
    private final Map<String, String> vehicle = new HashMap<>();
    private final Map<Integer, Map<String, String>> cameras = new HashMap<>();
    private final Map<Integer, Map<String, String>> seats = new HashMap<>();
    private final Map<Long, Map<String, String>> resolved = new HashMap<>();
    private final Map<Long, Boolean> availability = new HashMap<>();

    public boolean read(String item, String data) {
        boolean camera = item.regionMatches(true, 0, "CameraThermal", 0, 13);
        boolean seat = item.regionMatches(true, 0, "SeatThermal", 0, 11);
        String field = camera ? item.substring(6) : seat ? item.substring(4) : item;
        String key = null;
        for (String candidate : FIELDS) if (candidate.equalsIgnoreCase(field)) key = candidate;
        if (key == null) return false;
        Map<String, String> destination = vehicle;
        if (camera || seat) {
            int comma = data.indexOf(',');
            if (comma < 0) throw new IllegalArgumentException(item + " requires index, value");
            int index = Integer.parseInt(data.substring(0, comma).trim());
            if (index < 0 || index > 255) throw new IllegalArgumentException("Thermal camera/seat index must be 0 to 255");
            data = data.substring(comma + 1).trim();
            Map<Integer, Map<String, String>> overrides = camera ? cameras : seats;
            destination = overrides.get(index);
            if (destination == null) {
                destination = new HashMap<>();
                overrides.put(index, destination);
            }
        }
        String value = data.trim().replace(',', ' ');
        validate(key, value);
        destination.put(key, value);
        resolved.clear();
        availability.clear();
        return true;
    }

    public Map<String, String> resolve(int camera, int seat) {
        long identity = ((long)camera << 32) ^ (seat & 0xffffffffL);
        Map<String, String> profile = resolved.get(identity);
        if (profile == null) {
            Map<String, String> values = new HashMap<>(vehicle);
            if (cameras.containsKey(camera)) values.putAll(cameras.get(camera));
            if (seats.containsKey(seat)) values.putAll(seats.get(seat));
            // Capability belongs to MCHR; the optional FMUR sensor API receives only imaging fields.
            String enabled = values.remove("ThermalEnabled");
            availability.put(identity, enabled == null || Boolean.parseBoolean(enabled));
            profile = Collections.unmodifiableMap(values);
            resolved.put(identity, profile);
        }
        return profile;
    }

    public boolean isEnabled(int camera, int seat) {
        resolve(camera, seat);
        return availability.get(((long)camera << 32) ^ (seat & 0xffffffffL));
    }

    public void clear() { vehicle.clear(); cameras.clear(); seats.clear(); resolved.clear(); availability.clear(); }

    private static void validate(String key, String value) {
        if ("ThermalEnabled".equals(key)) {
            if (!"true".equalsIgnoreCase(value) && !"false".equalsIgnoreCase(value))
                throw new IllegalArgumentException("ThermalEnabled requires true or false");
        } else if ("ThermalMaxFPS".equals(key)) integer(value, 1, 240);
        else if ("ThermalResolution".equals(key)) {
            String[] size = value.split("\\s+");
            if (size.length != 2) throw new IllegalArgumentException("ThermalResolution requires width, height");
            integer(size[0], 64, 2048); integer(size[1], 64, 2048);
        } else if ("ThermalPalette".equals(key)) {
            if (!value.equalsIgnoreCase("WHITE_HOT") && !value.equalsIgnoreCase("BLACK_HOT")
                    && !value.equalsIgnoreCase("IRONBOW") && !value.equalsIgnoreCase("GREEN"))
                throw new IllegalArgumentException("Unknown thermal palette: " + value);
        } else {
            float number = Float.parseFloat(value);
            float min = key.equals("ThermalGrain") || key.equals("ThermalBloom") ? 0F : 0.1F;
            float max = key.equals("ThermalGrain") ? 1F : 4F;
            if (!Float.isFinite(number) || number < min || number > max)
                throw new IllegalArgumentException("Invalid " + key + ": " + value);
        }
    }

    private static void integer(String value, int min, int max) {
        int number = Integer.parseInt(value);
        if (number < min || number > max) throw new IllegalArgumentException("Thermal value outside " + min + " to " + max);
    }
}
