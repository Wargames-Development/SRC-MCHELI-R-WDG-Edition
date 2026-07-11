#version 120

uniform sampler2D DiffuseSampler;

varying vec2 texCoord;
varying vec2 oneTexel;

void main() {
    vec4 centerColor = texture2D(DiffuseSampler, texCoord);
    float brightness = (centerColor.r + centerColor.g + centerColor.b) / 3.0;
    float maxChannel = max(max(centerColor.r, centerColor.g), centerColor.b);
    float minChannel = min(min(centerColor.r, centerColor.g), centerColor.b);
    float saturation = maxChannel - minChannel;
    bool isTargetViolet = centerColor.r > 0.8 && centerColor.b > 0.8 && 
                         centerColor.b > centerColor.g * 1.5 && 
                         centerColor.r > centerColor.g * 1.5;
    bool isNearWhite = brightness > 0.9 && saturation < 0.2;
    if (isTargetViolet || isNearWhite) {
        gl_FragColor = vec4(vec3(1.0), centerColor.a);
    } else {
        float gray = dot(centerColor.rgb, vec3(0.299, 0.587, 0.114));
        gray = gray * 0.45;
        gl_FragColor = vec4(vec3(gray), centerColor.a);
    }
}