in vec2 vTexCoord;
out vec4 fragColor;

float uBendFactor = 0.5;
void main() {

    // 将纹理坐标的y值映射到一个曲线方程上
    float curve = uBendFactor * (vTexCoord.y - 0.5);
    float bend = curve * curve * sign(curve);

    // 将纹理坐标应用曲线变形
    vec2 texCoord = vec2(vTexCoord.x, vTexCoord.y + bend);

    // 根据条件判断是否丢弃片元
    if (texCoord.y < 0.0 || texCoord.y > 1.0) {
        discard;
    }
    // 从纹理中获取颜色并绘制
    fragColor = texture(texture_0, texCoord);
}
