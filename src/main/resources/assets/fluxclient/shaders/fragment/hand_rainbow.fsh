#version 150

in vec2 texCoord;
out vec4 fragColor;

uniform float time;
uniform vec2 screenSize;
uniform vec4 baseColor;
uniform float alpha;

vec3 hsv2rgb(vec3 c) {
    vec4 K = vec4(1.0, 2.0 / 3.0, 1.0 / 3.0, 3.0);
    vec3 p = abs(fract(c.xxx + K.xyz) * 6.0 - K.www);
    return c.z * mix(K.xxx, clamp(p - K.xxx, 0.0, 1.0), c.y);
}

void main() {
    vec2 uv = gl_FragCoord.xy / screenSize.xy;
    float hue = fract(uv.x * 1.5 + uv.y * 0.8 + time * 0.6);
    vec3 rgb = hsv2rgb(vec3(hue, 0.85, 1.0));
    
    // Мягкий неоновый блик
    float sheen = sin((uv.x + uv.y * 0.5) * 20.0 + time * 4.0) * 0.2 + 0.8;
    rgb *= sheen;

    fragColor = vec4(rgb, alpha);
}
