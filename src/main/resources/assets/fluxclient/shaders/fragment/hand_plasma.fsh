#version 150

in vec2 texCoord;
out vec4 fragColor;

uniform float time;
uniform vec2 screenSize;
uniform vec4 baseColor;
uniform float alpha;

void main() {
    vec2 uv = gl_FragCoord.xy / screenSize.xy;
    float t = time * 2.0;

    float v1 = sin(uv.x * 12.0 + t);
    float v2 = sin(uv.y * 12.0 + t * 1.5);
    float v3 = sin((uv.x + uv.y) * 10.0 + t);
    float v4 = sin(sqrt(uv.x * uv.x + uv.y * uv.y) * 16.0 + 1.0 + t);

    float plasma = (v1 + v2 + v3 + v4) * 0.25;
    plasma = sin(plasma * 3.14159) * 0.5 + 0.5;

    vec3 colA = baseColor.rgb;
    vec3 colB = vec3(0.9, 0.2, 0.4);
    vec3 res = mix(colA, colB, plasma);

    fragColor = vec4(res * 1.4, alpha);
}
