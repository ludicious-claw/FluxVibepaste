#version 150

in vec2 texCoord;
out vec4 fragColor;

uniform float time;
uniform vec2 screenSize;
uniform vec4 baseColor;
uniform float alpha;

float noise(vec2 p) {
    return sin(p.x * 2.0) * cos(p.y * 2.0);
}

void main() {
    vec2 uv = gl_FragCoord.xy / screenSize.xy;
    float t = time * 0.8;
    
    // Космический вихрь
    float n = noise(uv * 8.0 + vec2(t * 0.3, t * 0.2));
    float n2 = noise(uv * 16.0 - vec2(t * 0.2, -t * 0.4));
    float val = (n + n2) * 0.5 + 0.5;

    vec3 col1 = baseColor.rgb;
    vec3 col2 = vec3(0.2, 0.8, 1.0); // Неоновый циан
    vec3 mixed = mix(col1, col2, val);

    // Добавляем звездные искры
    float spark = fract(sin(dot(floor(uv * 120.0), vec2(12.9898, 78.233))) * 43758.5453);
    if (spark > 0.985) {
        mixed += vec3(1.0);
    }

    fragColor = vec4(mixed, alpha);
}
