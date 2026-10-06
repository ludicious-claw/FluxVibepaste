#version 150

in vec2 texCoord;
out vec4 fragColor;

uniform sampler2D u_msdf_atlas;  // Текстурный атлас шрифта от msdf-atlas-gen
uniform vec4 u_text_color;       // Цвет текста RGBA
uniform float u_px_range;        // Диапазон пикселей расстояния (обычно 3.0 - 4.0)

// Медианный фильтр трёх цветовых каналов MSDF
float median(float r, float g, float b) {
    return max(min(r, g), min(max(r, g), b));
}

void main() {
    vec3 msd = texture(u_msdf_atlas, texCoord).rgb;
    float sd = median(msd.r, msd.g, msd.b);
    
    // Преобразование расстояния знакового поля в экранные пиксели
    vec2 unitRange = vec2(u_px_range) / vec2(textureSize(u_msdf_atlas, 0));
    vec2 screenTexSize = vec2(1.0) / fwidth(texCoord);
    float screenPxRange = max(0.5 * dot(unitRange, screenTexSize), 1.0);
    float screenPxDistance = screenPxRange * (sd - 0.5);
    
    float opacity = clamp(screenPxDistance + 0.5, 0.0, 1.0);
    
    fragColor = vec4(u_text_color.rgb, u_text_color.a * opacity);
}
