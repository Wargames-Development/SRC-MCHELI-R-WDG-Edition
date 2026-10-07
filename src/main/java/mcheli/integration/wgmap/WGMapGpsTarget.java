package mcheli.integration.wgmap;

/** An immutable GPS fire choice; never a live WGMap record. */
public final class WGMapGpsTarget {
    public final double x, y, z;
    public final boolean shared;
    public final String name;

    public WGMapGpsTarget(double x, double y, double z, boolean shared) {
        this(x, y, z, shared, "");
    }

    public WGMapGpsTarget(double x, double y, double z, boolean shared, String name) {
        this.x = x; this.y = y; this.z = z; this.shared = shared;
        this.name = name != null ? name : "";
    }
}
