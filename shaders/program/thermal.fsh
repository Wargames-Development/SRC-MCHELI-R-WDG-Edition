#version 120

uniform sampler2D DiffuseSampler;

varying vec2 texCoord;
varying vec2 oneTexel;

void main() {
    vec4 centerColor = texture2D(DiffuseSampler, texCoord);
    float markerFloor = min(centerColor.r, centerColor.b);
    bool isThermalMarker = markerFloor > 0.55 &&
                           centerColor.g < markerFloor * 0.35;
    if (isThermalMarker) {
        gl_FragColor = vec4(vec3(1.0), centerColor.a);
    } else {
        float gray = dot(centerColor.rgb, vec3(0.299, 0.587, 0.114));
        gray = clamp(gray, 0.0, 1.0) * 0.4;
        gl_FragColor = vec4(vec3(gray), centerColor.a);
    }
}
