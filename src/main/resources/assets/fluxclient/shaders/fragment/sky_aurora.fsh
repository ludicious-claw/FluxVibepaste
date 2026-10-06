#version 150

in vec3 worldPos;
in vec2 texCoord;
out vec4 fragColor;

uniform vec2 resolution;
uniform float time;
uniform float cameraYaw;
uniform float cameraPitch;
uniform float fov;

// Настраиваемые цвета из конфига клиента
uniform vec4 color1;      // Нижний цвет сияния (Изумрудный/Циан)
uniform vec4 color2;      // Верхний цвет сияния (Фиолетовый/Маджента)
uniform vec4 zenithCol;   // Цвет макушки неба (Зенит)
uniform vec4 horizonCol;  // Цвет полосы горизонта
uniform float speed;
uniform float brightness;
uniform float starDensity;
uniform int skyMode;       // 0: Сияние, 1: Галактика, 2: Плазма, 3: Закат, 4: Полный

// Хеш-функция для псевдослучайных звёзд
float hash(vec3 p) {
    p = fract(p * 0.3183099 + vec3(0.1));
    p *= 17.0;
    return fract(p.x * p.y * p.z * (p.x + p.y + p.z));
}

// Генерация звёздного скопления
float getStars(vec3 rd, float t) {
    if (rd.y < 0.005) return 0.0;
    vec3 grid = floor(rd * 320.0);
    float h = hash(grid);
    float threshold = 1.0 - (0.012 * clamp(starDensity, 0.2, 3.0));
    if (h > threshold) {
        float twinkle = sin(t * 3.2 + h * 62.8318) * 0.5 + 0.5;
        float intensity = pow((h - threshold) / (1.0 - threshold), 3.0);
        return intensity * twinkle * 2.8;
    }
    return 0.0;
}

// Процедурное кинематографичное Северное Сияние (Aurora Borealis)
vec3 getAurora(vec3 rd, float t, vec3 c1, vec3 c2) {
    if (rd.y <= 0.001) return vec3(0.0);

    // Проекция луча на атмосферную плоскость
    float d = 1.0 / (rd.y + 0.05);
    vec2 p = rd.xz * d * 0.16;

    vec3 accum = vec3(0.0);

    // 3 волновых слоя струящихся занавесей
    for (int i = 0; i < 3; i++) {
        float fi = float(i);
        vec2 waveP = p + vec2(t * 0.06 * (fi * 0.35 + 0.75), fi * 2.6);

        // Мягкие гармонические складки полярного ветра
        float fold = sin(waveP.x * 2.1 + t * 0.22) * 0.52
                   + sin(waveP.x * 4.7 - t * 0.16) * 0.26
                   + sin(waveP.x * 10.2 + t * 0.28) * 0.13;

        // Вертикальные лучи-струи (шелковая драпировка)
        float rays = sin(waveP.x * 38.0 + fold * 10.0) * 0.5 + 0.5;
        rays = pow(rays, 1.7);

        // Мягкое экспоненциальное затухание по краям полосы (БЕЗ жестких границ!)
        float dist = abs(waveP.y - fold);
        float curtain = exp(-dist * 2.6) * (0.5 + 0.5 * rays);

        // Градиент высоты занавеси: низ изумрудный, верх неоново-фиолетовый
        float hProg = clamp(dist * 1.4, 0.0, 1.0);
        vec3 col = mix(c1, c2, hProg);

        accum += col * curtain * (1.15 / (fi * 0.45 + 1.0));
    }

    // Плавное мягкое растворение в туман горизонта (устраняет любые срезы)
    float horizonFade = smoothstep(0.015, 0.22, rd.y);
    return accum * horizonFade;
}

// Космическая туманность и галактический рукав
vec3 getGalaxy(vec3 rd, float t, vec3 c1, vec3 c2) {
    if (rd.y <= 0.005) return vec3(0.0);
    vec2 guv = rd.xz / (rd.y + 0.22) * 0.45;
    float dist = length(guv);
    float angle = atan(guv.y, guv.x) + dist * 2.2 - t * 0.07;
    float spiral = sin(angle * 2.0) * 0.5 + 0.5;
    spiral = pow(spiral, 2.6) * exp(-dist * 1.1);

    vec3 col = mix(c1 * 0.7, c2 * 1.5, spiral);
    return col * spiral * 2.2 * smoothstep(0.02, 0.2, rd.y);
}

// Энергетическая плазма
vec3 getPlasma(vec3 rd, float t, vec3 c1, vec3 c2) {
    if (rd.y <= 0.005) return vec3(0.0);
    vec2 p = rd.xz / (rd.y + 0.12) * 0.7;
    float v1 = sin(p.x * 3.8 + t * 1.2);
    float v2 = sin(p.y * 3.8 + t * 1.4);
    float v3 = sin((p.x + p.y) * 3.2 + t * 0.9);
    float plas = (v1 + v2 + v3) * 0.333 * 0.5 + 0.5;
    vec3 col = mix(c1, c2, plas);
    return col * pow(plas, 1.6) * 1.8 * smoothstep(0.02, 0.2, rd.y);
}

void main() {
    // 1. Мировой 3D вектор направления луча (интерполирован из вершин купола)
    vec3 rd = normalize(worldPos);

    float t = time * speed;

    // 3. Базовый градиент неба (Зенит -> Горизонт -> Надир)
    vec3 skyBase;
    if (rd.y >= 0.0) {
        float skyGrad = clamp(pow(rd.y, 0.65), 0.0, 1.0);
        skyBase = mix(horizonCol.rgb, zenithCol.rgb, skyGrad);
    } else {
        // Ниже горизонта плавно уходит в цвет тумана горизонта
        skyBase = horizonCol.rgb * max(0.0, 1.0 + rd.y * 2.0);
    }

    vec3 finalColor = skyBase;

    // 4. Звёзды
    if (starDensity > 0.05) {
        float stars = getStars(rd, t);
        finalColor += vec3(stars);
    }

    // 5. Визуальные эффекты в зависимости от выбранного режима
    vec3 c1 = color1.rgb;
    vec3 c2 = color2.rgb;

    if (skyMode == 0) {
        // Северное сияние
        vec3 aurora = getAurora(rd, t, c1, c2);
        finalColor += aurora * brightness;
    } else if (skyMode == 1) {
        // Космос и Галактика
        vec3 galaxy = getGalaxy(rd, t, c1, c2);
        finalColor += galaxy * brightness;
    } else if (skyMode == 2) {
        // Плазма
        vec3 plasma = getPlasma(rd, t, c1, c2);
        finalColor += plasma * brightness;
    } else if (skyMode == 3) {
        // Неоновый закат
        if (rd.y > 0.0) {
            float sunset = exp(-abs(rd.y - 0.07) * 7.5);
            vec3 sunsetCol = mix(vec3(0.98, 0.35, 0.15), vec3(0.85, 0.15, 0.65), rd.x * 0.5 + 0.5);
            finalColor += sunsetCol * sunset * 1.8;
            vec3 aurora = getAurora(rd, t, c1, c2);
            finalColor += aurora * (brightness * 0.45);
        }
    } else {
        // Полный (Кинематограф): Галактика + Северное сияние
        vec3 galaxy = getGalaxy(rd, t, c1, c2);
        vec3 aurora = getAurora(rd, t, c1, c2);
        finalColor += galaxy * 0.75 + aurora * brightness;
    }

    fragColor = vec4(finalColor, 1.0);
}
