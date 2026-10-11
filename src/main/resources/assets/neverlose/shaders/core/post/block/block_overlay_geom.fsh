#version 150

in vec4 vertexColor;
in vec2 texCoord0;

out vec4 fragColor;

float waterPattern(vec2 uv, float time) {
    float grid = sin(uv.x * 26.0 + gl_FragCoord.y * 0.045 + time * 2.15)
               * cos(uv.y * 22.0 - gl_FragCoord.x * 0.035 - time * 1.85);
    float bands = sin((uv.x + uv.y) * 34.0 + gl_FragCoord.x * 0.018 + time * 2.7);
    float ripple = sin(length(uv - 0.5) * 18.0 - time * 3.1);
    return clamp(0.45 + grid * 0.28 + bands * 0.16 + ripple * 0.12, 0.0, 1.0);
}

float causticPattern(vec2 uv, float time) {
    vec2 p = uv * 2.0 - 1.0;
    float a = sin((p.x * p.x - p.y) * 18.0 + gl_FragCoord.y * 0.055 + time * 2.4);
    float b = cos((p.y * p.y + p.x) * 22.0 - gl_FragCoord.x * 0.045 - time * 2.0);
    float c = sin((p.x + p.y) * 28.0 + gl_FragCoord.x * 0.018 + gl_FragCoord.y * 0.02 + time * 3.2);
    return clamp(0.35 + abs(a * b) * 0.45 + c * 0.16, 0.0, 1.0);
}

void main() {
    float mode = step(7.5, texCoord0.x);
    vec2 uv = fract(texCoord0);
    float rawBlue = floor(vertexColor.b * 255.0 + 0.5);
    float time = mod(rawBlue, 32.0) / 31.0 * 6.2831853;
    float opacity = vertexColor.a;
    vec3 tint = vec3(vertexColor.r, vertexColor.g, (floor(rawBlue / 32.0) * 32.0 + 16.0) / 255.0);
    float shade = mix(waterPattern(uv, time), causticPattern(uv, time), mode);
    float mask = 0.28 + shade * 0.72;

    if (opacity <= 0.01 || mask <= 0.02) {
        discard;
    }

    vec3 color = tint * (0.20 + mask * 1.25);
    fragColor = vec4(clamp(color, 0.0, 1.0), opacity * (0.72 + mask * 0.28));
}
