#version 150

in vec2 texCoord;

out vec4 fragColor;

uniform sampler2D Sampler0;
uniform sampler2D Sampler1;

layout(std140) uniform PostData {
    vec4 uResolutionSaturation;
    vec4 uBlurData;
    vec4 uSnowParams;
    mat4 uProjInv;
    mat4 uViewInv;
    vec4 uUpView;
};

vec3 adjustSaturation(vec3 color, float saturation) {
    float gray = dot(color, vec3(0.299, 0.587, 0.114));
    return mix(vec3(gray), color, saturation);
}

vec4 blurScene(vec2 uv, float radius) {
    vec2 resolution = max(uResolutionSaturation.xy, vec2(1.0));
    vec2 texel = 1.0 / resolution;

    int samples = clamp(int(ceil(radius)), 1, 10);
    float sigma = max(radius * 0.45, 0.1);
    float twoSigma2 = 2.0 * sigma * sigma;
    vec4 color = vec4(0.0);
    float total = 0.0;

    for (int x = -samples; x <= samples; x++) {
        for (int y = -samples; y <= samples; y++) {
            float d = float(x * x + y * y);
            float weight = exp(-d / twoSigma2);
            vec2 offset = vec2(float(x), float(y)) * texel;
            color += texture(Sampler0, clamp(uv + offset, vec2(0.001), vec2(0.999))) * weight;
            total += weight;
        }
    }

    return color / max(total, 0.0001);
}

float hash(vec2 p) {
    return fract(sin(dot(p, vec2(12.9898, 78.233))) * 43758.5453);
}

void main() {
    vec4 scene = texture(Sampler0, texCoord);
    float saturation = max(uResolutionSaturation.z, 0.0);
    float blurRadius = max(uBlurData.x, 0.0);
    float blurStrength = clamp(uBlurData.y, 0.0, 1.0);
    float fogBlur = step(0.5, uBlurData.z);

    vec3 color = scene.rgb;
    if (fogBlur > 0.5 && blurRadius > 0.01 && blurStrength > 0.001) {
        vec4 blurred = blurScene(texCoord, blurRadius);
        float horizonMask = smoothstep(0.18, 0.82, texCoord.y);
        float edgeMask = smoothstep(0.18, 0.78, length(texCoord - vec2(0.5)) * 1.35);
        float fogMask = clamp(0.25 + horizonMask * 0.55 + edgeMask * 0.20, 0.0, 1.0);
        color = mix(color, blurred.rgb, blurStrength * fogMask);
    }

    if (uSnowParams.w > 0.5) {
        vec2 ndcXY = texCoord * 2.0 - 1.0;
        float depth = texture(Sampler1, texCoord).r;
        float ndcZ = depth * 2.0 - 1.0;
        vec4 view = uProjInv * vec4(ndcXY, ndcZ, 1.0);
        vec3 viewPos = view.xyz / max(view.w, 1.0e-5);

        vec3 dx = dFdx(viewPos);
        vec3 dy = dFdy(viewPos);
        vec3 normal = normalize(cross(dx, dy));
        if (dot(normal, -viewPos) < 0.0) {
            normal = -normal;
        }

        float up = dot(normal, normalize(uUpView.xyz));
        float amount = smoothstep(uSnowParams.y, uSnowParams.z, up) * uSnowParams.x;
        amount *= step(depth, 0.9999);

        vec3 worldPos = (uViewInv * vec4(viewPos, 1.0)).xyz;
        float shade = 0.9 + 0.1 * hash(floor(worldPos.xz * 0.5));
        vec3 snow = vec3(0.94, 0.96, 1.0) * shade;

        color = mix(color, snow, clamp(amount, 0.0, 1.0));

        float sparkle = step(0.9975, hash(floor(worldPos * 5.0) + floor(worldPos.zx * 3.0)));
        float near = 1.0 - smoothstep(40.0, 90.0, length(viewPos));
        color += snow * sparkle * 0.4 * amount * near;
    }

    color = adjustSaturation(color, saturation);
    fragColor = vec4(clamp(color, 0.0, 1.0), scene.a);
}
