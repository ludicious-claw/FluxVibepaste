#version 150

in vec3 Position;
in vec2 UV0;

uniform mat4 ProjMat;
uniform mat4 ModelViewMat;

out vec3 worldPos;
out vec2 texCoord;

void main() {
    worldPos = Position;
    vec4 pos = ProjMat * ModelViewMat * vec4(Position, 1.0);
    // Привязываем купол к дальней плоскости отсечения (z = w * 0.99999), чтобы он гарантированно
    // находился позади всех блоков мира и не клипался при любой дальности прорисовки
    gl_Position = vec4(pos.xy, pos.w * 0.99999, pos.w);
    texCoord = UV0;
}
