#version 150

in vec3 Position;
in vec2 UV0;

uniform mat4 ProjMat;
uniform mat4 ModelViewMat;

out vec3 worldPos;
out vec2 texCoord;

void main() {
    worldPos = Position;
    vec4 viewPos = ModelViewMat * vec4(Position, 1.0);
    // КРИТИЧЕСКИЙ ФИКС: предотвращаем инверсию геометрии и улет в угол экрана
    if (viewPos.z > -0.05) {
        viewPos.z = -0.05;
    }
    gl_Position = ProjMat * viewPos;
    texCoord = UV0;
}
