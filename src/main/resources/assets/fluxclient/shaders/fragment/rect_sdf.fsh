#version 150

in vec2 texCoord;
out vec4 fragColor;

uniform vec2 u_size;             // Размеры прямоугольника в пикселях (w, h)
uniform vec4 u_radius;           // Радиусы углов: (top-left, top-right, bottom-right, bottom-left)
uniform vec4 u_color1;           // Верхний/основной цвет RGBA
uniform vec4 u_color2;           // Нижний цвет RGBA (для вертикального градиента)
uniform vec4 u_border_color;     // Цвет обводки
uniform float u_border_width;    // Толщина обводки
uniform float u_shadow_softness; // Размытие тени/свечения
uniform vec4 u_shadow_color;     // Цвет тени/свечения

// Математическое знакопеременное поле расстояний (Signed Distance Field) для скругленного прямоугольника
float roundedBoxSDF(vec2 centerPos, vec2 size, vec4 radius) {
    radius.xy = (centerPos.x > 0.0) ? radius.yz : radius.xw;
    float r = (centerPos.y > 0.0) ? radius.y : radius.x;
    vec2 q = abs(centerPos) - size + r;
    return min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - r;
}

void main() {
    vec2 pixelPos = texCoord * u_size;
    vec2 halfSize = u_size * 0.5;
    vec2 centerPos = pixelPos - halfSize;

    float dist = roundedBoxSDF(centerPos, halfSize - vec2(u_border_width), u_radius);
    
    // Субпиксельное субдискретное антиалиасинг-сглаживание
    float edgeSoftness = 1.0;
    float fillAlpha = 1.0 - smoothstep(-edgeSoftness, 0.0, dist);

    // Вертикальный градиент
    vec4 fillColor = mix(u_color1, u_color2, texCoord.y);
    vec4 result = fillColor * fillAlpha;

    // Внутренняя/внешняя обводка
    if (u_border_width > 0.0) {
        float borderDist = abs(dist + u_border_width * 0.5) - u_border_width * 0.5;
        float borderAlpha = 1.0 - smoothstep(-edgeSoftness, 0.0, borderDist);
        result = mix(result, u_border_color, borderAlpha * u_border_color.a);
    }

    // Мягкое свечение / рассеянная тень
    if (u_shadow_softness > 0.0 && u_shadow_color.a > 0.0) {
        float shadowAlpha = 1.0 - smoothstep(-u_shadow_softness, u_shadow_softness, dist);
        result = mix(u_shadow_color * shadowAlpha, result, fillAlpha);
    }

    fragColor = result;
}
