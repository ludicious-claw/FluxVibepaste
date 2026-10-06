#version 150

in vec2 texCoord;
out vec4 fragColor;

uniform float time;
uniform vec2 screenSize;
uniform vec4 baseColor;
uniform float alpha;

// НОВЫЕ UNIFORMS: передаем углы поворота головы для фиксации неба в 3D пространстве
uniform float cameraYaw;
uniform float cameraPitch;

float noise(in vec2 p) {
    return sin(p.x * 0.5) * sin(p.y * 0.5);
}

float fbm(in vec2 p) {
    float v = 0.0;
    float a = 0.5;
    vec2 shift = vec2(100.0);
    mat2 rot = mat2(cos(0.5), sin(0.5), -sin(0.5), cos(0.5));
    for (int i = 0; i < 5; ++i) {
        v += a * noise(p);
        p = rot * p * 2.0 + shift;
        a *= 0.5;
    }
    return v;
}

void main() {
    // Базовые координаты пикселя экрана
    vec2 uv = (gl_FragCoord.xy * 2.0 - screenSize.xy) / screenSize.y;

    // МАТЕМАТИЧЕСКИЙ ФИКС: Смещаем uv-координаты шейдера на основе углов камеры Майнкрафта.
    // Теперь при повороте мышки картинка будет двигаться в противоход, создавая эффект статичного 3D-неба!
    uv.x += cameraYaw * 0.01745329251; // Перевод градусов в радианы со смещением
    uv.y -= cameraPitch * 0.01745329251;

    float t = time * 0.4;

    vec2 q = vec2(fbm(uv + vec2(0.0, 0.0)), fbm(uv + vec2(5.2, 1.3)));
    vec2 r = vec2(fbm(uv + 4.0 * q + vec2(1.7, 9.2) + t), fbm(uv + 4.0 * q + vec2(8.3, 2.8) + t));
    float f = fbm(uv + 4.0 * r);

    // Сделали цвета туманности значительно ярче и насыщеннее, чтобы убрать блёклость
    vec3 spaceBackground = baseColor.rgb * 0.3; // Подняли яркость фона космоса
    vec3 fireClouds = baseColor.rgb * 2.5;       // Накрутили контраст огненных туч (было 1.2)
    vec3 highlights = vec3(0.5, 0.1, 0.45);      // Сделали неоновые фиолетовые вкрапления сочнее

    vec3 color = mix(spaceBackground, fireClouds, clamp(f * f * 4.0, 0.0, 1.0));
    color = mix(color, highlights, clamp(length(q), 0.0, 1.0));
    color = color * f * f * (3.5 - 2.0 * f);

    color = clamp(color, 0.0, 1.0);

    // Форсируем максимальную видимость без выбеливания ванилой
    fragColor = vec4(color, alpha * 1.5);
}