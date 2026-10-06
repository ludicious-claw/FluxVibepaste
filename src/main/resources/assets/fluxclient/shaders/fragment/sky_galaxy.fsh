#version 150

in vec3 worldPos;
in vec2 texCoord;
out vec4 fragColor;

uniform float time;
uniform vec2 screenSize;
uniform vec4 baseColor;
uniform float alpha;

float hash(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453123);
}

void main() {
    vec3 rd = normalize(worldPos);
    if (rd.y <= 0.005) {
        fragColor = vec4(0.0);
        return;
    }

    vec2 uv = rd.xz / (rd.y + 0.22) * 0.8;

    // Вращение галактики
    float ang = atan(uv.y, uv.x);
    float dist = length(uv);
    float rot = ang + dist * 3.5 - time * 0.15;
    
    // Спиральные рукава
    float spiral = sin(rot * 2.0) * 0.5 + 0.5;
    spiral = pow(spiral, 3.0) * exp(-dist * 1.8);

    // Звёздная россыпь
    vec2 grid = floor(uv * 45.0);
    float star = hash(grid);
    float starGlow = 0.0;
    if (star > 0.94) {
        float twinkle = sin(time * 3.0 + star * 6.28) * 0.5 + 0.5;
        starGlow = (star - 0.94) * 16.0 * twinkle;
    }

    vec3 coreColor = vec3(0.95, 0.85, 1.0);
    vec3 armColor = baseColor.rgb * 1.8;
    vec3 finalCol = mix(coreColor, armColor, clamp(dist * 1.2, 0.0, 1.0)) * spiral * 2.5 + vec3(starGlow);

    fragColor = vec4(finalCol, clamp((spiral * 1.5 + starGlow) * alpha, 0.0, 1.0));
}
