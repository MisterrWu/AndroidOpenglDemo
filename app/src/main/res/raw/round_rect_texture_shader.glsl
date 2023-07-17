in vec2 vTexCoord;
out vec4 fragColor;
vec2 dimensions = vec2(100.0,100.0);
float cornerRadius = 0.6;
void main() {

    vec2 uv = vTexCoord.xy;

    // 这会创建一个原点在(0.5, 0.5)的标准化坐标系统
    vec2 normCoord = 2.0*uv - 1.0;

    float radius = cornerRadius;

    // 计算uv到最近角的距离
    float dist = distance(normCoord, vec2(clamp(normCoord.x, -0.5, 0.5), clamp(normCoord.y, -0.5, 0.5)));

    // 如果距离大于半径，则丢弃这个片段
    if(dist > radius)
    discard;
    else
    fragColor=texture(texture_0,vTexCoord);
}
