#version 150

in vec2 texCoord;
out vec4 fragColor;

uniform float time;
uniform vec2 screenSize;
uniform vec4 baseColor;
uniform float alpha;

void main() {
    vec2 uv = gl_FragCoord.xy / screenSize.xy;
    
    // Голографические скан-линии
    float scanline = sin(uv.y * screenSize.y * 1.5 + time * 10.0) * 0.5 + 0.5;
    scanline = pow(scanline, 2.0) * 0.4 + 0.6;

    // Горизонтальный глитч-сдвиг
    float band = sin(uv.y * 10.0 - time * 4.0);
    float glow = smoothstep(0.2, 0.8, band);

    vec3 holoColor = baseColor.rgb * scanline;
    holoColor += vec3(0.1, 0.4, 0.6) * glow;

    fragColor = vec4(holoColor * 1.5, alpha * scanline);
}
