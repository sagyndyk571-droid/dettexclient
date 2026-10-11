#version 150

uniform sampler2D AfterSampler;
uniform sampler2D BeforeSampler;

layout(std140) uniform HandShaderData {
    vec4 resolution;
    vec4 tintColor;
    vec4 settings;
};

in vec2 texCoord;
out vec4 fragColor;

float blurRadius() {
    return resolution.z;
}

float threshold() {
    return resolution.w;
}

int shaderMode() {
    return int(settings.x + 0.5);
}

float shaderTime() {
    return settings.y;
}

float handMask(vec2 uv) {
    vec3 diff = abs(texture(AfterSampler, uv).rgb - texture(BeforeSampler, uv).rgb);
    return smoothstep(threshold(), threshold() + 0.08, max(diff.r, max(diff.g, diff.b)) * 8.0);
}

float hash(vec2 p) {
    p = fract(p * vec2(123.34, 345.45));
    p += dot(p, p + 34.345);
    return fract(p.x * p.y);
}

float noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    return mix(
        mix(hash(i), hash(i + vec2(1.0, 0.0)), f.x),
        mix(hash(i + vec2(0.0, 1.0)), hash(i + vec2(1.0, 1.0)), f.x),
        f.y
    );
}

float fbm(vec2 p) {
    float v = 0.0;
    float a = 0.5;
    for (int i = 0; i < 5; i++) {
        v += noise(p) * a;
        p = p * 2.02 + vec2(8.4, 5.7);
        a *= 0.5;
    }
    return v;
}

float ridged(vec2 p) {
    float v = 0.0;
    float a = 0.55;
    for (int i = 0; i < 4; i++) {
        float r = 1.0 - abs(noise(p) * 2.0 - 1.0);
        v += r * a;
        p = p * 2.18 + vec2(3.1, 9.2);
        a *= 0.52;
    }
    return v;
}

vec3 hsv2rgb(vec3 c) {
    vec4 K = vec4(1.0, 2.0 / 3.0, 1.0 / 3.0, 3.0);
    vec3 p = abs(fract(c.xxx + K.xyz) * 6.0 - K.www);
    return c.z * mix(K.xxx, clamp(p - K.xxx, 0.0, 1.0), c.y);
}

float outerGlow(vec2 uv, float radius) {
    vec2 texel = radius / vec2(textureSize(BeforeSampler, 0));
    float glow = 0.0;
    for (int i = 0; i < 8; i++) {
        float a = float(i) * 0.7854;
        vec2 dir = vec2(cos(a), sin(a));
        glow += handMask(uv + dir * texel * 0.5) + handMask(uv + dir * texel);
    }
    return (glow / 16.0) * (1.0 - handMask(uv));
}

void main() {
    vec2 uv = texCoord;
    float mask = handMask(uv);
    int mode = shaderMode();
    float time = shaderTime();
    vec4 tint = tintColor;
    vec2 samplerSize = vec2(textureSize(BeforeSampler, 0));

    if (mode == 0) {
        if (mask < 0.01) discard;
        vec2 stepSize = blurRadius() / samplerSize;
        vec3 blurred = vec3(0.0);
        for (int i = 0; i < 8; i++) {
            float a = float(i) * 0.7854;
            vec2 dir = vec2(cos(a), sin(a));
            blurred += texture(BeforeSampler, uv + dir * stepSize * 0.33).rgb
                     + texture(BeforeSampler, uv + dir * stepSize * 0.66).rgb
                     + texture(BeforeSampler, uv + dir * stepSize).rgb;
        }
        blurred /= 24.0;
        fragColor = vec4(mix(blurred, tint.rgb, tint.a), mask);
    } else if (mode == 1) {
        if (mask > 0.99) discard;
        vec2 glowStep = (blurRadius() * 1.5) / samplerSize;
        float glow = 0.0;
        for (int i = 0; i < 8; i++) {
            float a = float(i) * 0.7854;
            vec2 dir = vec2(cos(a), sin(a));
            glow += handMask(uv + dir * glowStep * 0.4)
                  + handMask(uv + dir * glowStep * 0.7)
                  + handMask(uv + dir * glowStep);
        }
        glow = (glow / 24.0) * (1.0 - mask);
        if (glow < 0.005) discard;
        fragColor = vec4(tint.rgb, clamp(pow(glow, 0.5) * tint.a * 1.6, 0.0, 1.0));
    } else if (mode == 2) {
        if (mask < 0.01) discard;
        vec2 flow = uv * 2.5;
        vec2 drift = vec2(time * 0.20, -time * 0.15);
        vec2 warp = vec2(
            fbm(flow * 0.90 + drift * 0.75 + vec2(0.0, 4.1)),
            fbm(flow * 0.78 - drift * 0.48 + vec2(3.7, 1.8))
        );
        vec2 q = flow + (warp - 0.5) * 1.8;
        float mist = fbm(q * 0.72 - drift * 0.24 + vec2(4.2, 8.1));
        float veins = pow(clamp(ridged(q * 1.85 + vec2(mist * 2.5, mist * 1.6) - drift * 0.55), 0.0, 1.0), 2.4);
        float stripeA = pow(clamp(1.0 - abs(sin((q.x * 1.08 + q.y * 0.42) * 1.7 + time * 0.85 + mist * 4.3)), 0.0, 1.0), 4.8);
        float stripeB = pow(clamp(1.0 - abs(sin((q.x * -0.58 + q.y * 1.12) * 1.45 - time * 0.65 - mist * 2.9)), 0.0, 1.0), 5.4);
        float energy = clamp(mist * 0.22 + veins * 0.88 + stripeA * 0.55 + stripeB * 0.32, 0.0, 1.0);
        float core = smoothstep(0.18, 0.98, energy);
        float accent = pow(clamp(max(veins, stripeA), 0.0, 1.0), 1.25);
        vec3 color = mix(tint.rgb, mix(tint.rgb, vec3(1.0), 0.4), clamp(core * 0.75 + stripeB * 0.25, 0.0, 1.0));
        float fill = mask * (0.26 + core * 0.82 + accent * 0.28);
        float alpha = clamp(tint.a * fill * 0.92 * mask, 0.0, 1.0);
        if (alpha <= 0.001) discard;
        fragColor = vec4(color * fill, alpha);
    } else if (mode == 3) {
        float waveA = fbm(uv * 3.5 + vec2(time * 0.22, time * 0.15));
        float waveB = fbm(uv * 2.8 - vec2(time * 0.18, -time * 0.12));
        float hue = uv.x * 0.7 + uv.y * 0.5 + time * 0.10 + waveA * 0.3;
        vec3 rainbow = hsv2rgb(vec3(fract(hue), 0.95, 1.0));
        vec2 auraStep = (blurRadius() * 3.0) / samplerSize;
        float auraSum = 0.0;
        float auraHue = 0.0;
        float total = 0.0;
        for (int i = 0; i < 12; i++) {
            float a = float(i) * 0.5236;
            vec2 dir = vec2(cos(a), sin(a));
            for (float r = 0.3; r <= 1.0; r += 0.233) {
                vec2 sampleUv = uv + dir * auraStep * r;
                float sampleMask = handMask(sampleUv);
                auraSum += sampleMask;
                auraHue += (sampleUv.x * 0.7 + sampleUv.y * 0.5 + time * 0.10 + waveB * 0.3) * sampleMask;
                total += 1.0;
            }
        }
        float outer = (auraSum / total) * (1.0 - mask);
        float glowHue = auraSum > 0.001 ? auraHue / auraSum : hue;
        vec3 glowColor = hsv2rgb(vec3(fract(glowHue), 0.9, 1.0));
        if (mask > 0.01) {
            fragColor = vec4(rainbow, mask * tint.a + clamp(pow(outer, 0.45) * tint.a * 1.8, 0.0, 1.0) * 0.3);
        } else if (outer > 0.005) {
            fragColor = vec4(glowColor, clamp(pow(outer, 0.45) * tint.a * 1.8, 0.0, 1.0));
        } else {
            discard;
        }
    } else if (mode == 4) {
        float glow = outerGlow(uv, blurRadius() * 1.8);
        if (mask < 0.01) {
            if (glow < 0.005) discard;
            float pulse = sin(uv.x * 8.0 + time * 2.0) * 0.5 + 0.5;
            vec3 color = mix(tint.rgb, mix(tint.rgb, vec3(1.0), 0.6), pulse);
            fragColor = vec4(color, clamp(pow(glow, 0.5) * tint.a * 1.5, 0.0, 1.0));
        } else {
            float p1 = sin(uv.x * 8.0 + time * 2.0);
            float p2 = sin(uv.y * 6.0 + time * 1.5);
            float p3 = sin((uv.x + uv.y) * 5.0 + time * 1.8);
            float p4 = sin(sqrt(uv.x * uv.x + uv.y * uv.y) * 7.0 - time * 2.5);
            float n = (p1 + p2 + p3 + p4) * 0.125 + 0.5;
            vec3 c1 = tint.rgb;
            vec3 c2 = mix(tint.rgb, vec3(1.0), 0.6);
            vec3 color = n < 0.5 ? mix(c1, c2, n * 2.0) : mix(c2, vec3(1.0), (n - 0.5) * 2.0);
            fragColor = vec4(color, mask * tint.a + clamp(pow(glow, 0.5) * tint.a * 1.5, 0.0, 1.0) * 0.2);
        }
    } else {
        float row = floor(uv.y * 32.0);
        float glitchRandom = hash(vec2(row, floor(time * 8.0)));
        float glitchAmount = step(0.85, glitchRandom) * (glitchRandom - 0.85) * 4.0;
        vec2 glitchUv = uv + vec2(glitchAmount * 0.08, 0.0);
        float stripe = step(0.92, hash(vec2(row, floor(time * 4.0))));
        vec3 baseColor = tint.rgb;
        vec3 glitchColor = mix(baseColor, vec3(1.0, 0.0, 0.5), stripe * 0.8);
        float redMask = handMask(glitchUv + vec2(0.005, 0.0));
        float greenMask = handMask(glitchUv);
        float blueMask = handMask(glitchUv - vec2(0.005, 0.0));
        vec3 color = vec3(
            mix(baseColor.r, glitchColor.r, redMask),
            mix(baseColor.g, glitchColor.g, greenMask),
            mix(baseColor.b, glitchColor.b, blueMask)
        ) * (0.85 + 0.15 * sin(uv.y * 120.0));
        float glow = outerGlow(uv, blurRadius() * 1.5);
        if (mask > 0.01) {
            fragColor = vec4(color, mask * tint.a + clamp(pow(glow, 0.5) * tint.a * 1.4, 0.0, 1.0) * 0.15);
        } else if (glow > 0.005) {
            fragColor = vec4(tint.rgb, clamp(pow(glow, 0.5) * tint.a * 1.4, 0.0, 1.0));
        } else {
            discard;
        }
    }
}
