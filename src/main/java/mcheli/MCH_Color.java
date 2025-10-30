package mcheli;


public class MCH_Color {

    public float a;
    public float r;
    public float g;
    public float b;


    public MCH_Color(float aa, float rr, float gg, float bb) {
        this.a = this.round(aa);
        this.r = this.round(rr);
        this.g = this.round(gg);
        this.b = this.round(bb);
    }

    public MCH_Color(float rr, float gg, float bb) {
        this(1.0F, rr, gg, bb);
    }

    public MCH_Color() {
        this(1.0F, 1.0F, 1.0F, 1.0F);
    }

    public static MCH_Color fromARGB(int argb) {
        float a = ((argb >>> 24) & 0xFF) / 255.0f;
        float r = ((argb >>> 16) & 0xFF) / 255.0f;
        float g = ((argb >>> 8) & 0xFF) / 255.0f;
        float b = ((argb) & 0xFF) / 255.0f;
        return new MCH_Color(a, r, g, b);
    }

    public float round(float f) {
        return f < 0.0F ? 0.0F : (Math.min(f, 1.0F));
    }

    public int toARGB() {
        int aInt = Math.round(this.a * 255);
        int rInt = Math.round(this.r * 255);
        int gInt = Math.round(this.g * 255);
        int bInt = Math.round(this.b * 255);

        return (aInt << 24) | (rInt << 16) | (gInt << 8) | bInt;
    }

    public int toRGB() {
        int rInt = Math.round(this.r * 255);
        int gInt = Math.round(this.g * 255);
        int bInt = Math.round(this.b * 255);

        return 0xFF000000 | (rInt << 16) | (gInt << 8) | bInt;
    }
}
