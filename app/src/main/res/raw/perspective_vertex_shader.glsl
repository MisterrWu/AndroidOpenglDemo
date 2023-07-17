#version 300 es
layout (location = 0) in vec4 aPosition;
layout (location = 1) in vec4 aTexCoord;

out vec2 vTexCoord;
uniform mat4 uMatrix;
uniform mat4 uSTMatrix;
uniform mat4 uProjectionMatrix;

void main() {
    vTexCoord = (uSTMatrix * aTexCoord).xy;
    gl_Position = uProjectionMatrix*uMatrix*aPosition;
}
