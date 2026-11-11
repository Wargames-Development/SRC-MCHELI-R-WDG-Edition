package mcheli.render;

import java.nio.FloatBuffer;

/** Camera-relative marker math; no camera policy or gameplay state. */
public final class MCH_MarkerProjection {
    private MCH_MarkerProjection() { }
    public static boolean eye(FloatBuffer m,double[] out) {
        double a=m.get(0),b=m.get(4),c=m.get(8),d=m.get(1),e=m.get(5),f=m.get(9),g=m.get(2),h=m.get(6),i=m.get(10);
        double det=a*(e*i-f*h)-b*(d*i-f*g)+c*(d*h-e*g);
        if(!Double.isFinite(det) || Math.abs(det)<1e-12) return false;
        double x=m.get(12),y=m.get(13),z=m.get(14);
        out[0]=-((e*i-f*h)*x+(c*h-b*i)*y+(b*f-c*e)*z)/det;
        out[1]=-((f*g-d*i)*x+(a*i-c*g)*y+(c*d-a*f)*z)/det;
        out[2]=-((d*h-e*g)*x+(b*g-a*h)*y+(a*e-b*d)*z)/det;
        return Double.isFinite(out[0]+out[1]+out[2]);
    }
    public static double scaled(double point,double eye,double scale) { return eye+(point-eye)*scale; }
    static double depth(FloatBuffer m,double x,double y,double z) { return -(m.get(2)*x+m.get(6)*y+m.get(10)*z+m.get(14)); }
}
