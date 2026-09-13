#version 330
#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:fog.glsl>

uniform sampler2D Sampler0;
uniform sampler2D Sampler1;
in vec2 coatingUv;
in vec2 sourceUv;
in vec4 lightColor;
in float sourceCutout;
in float sphereDistance;
in float cylinderDistance;
out vec4 fragColor;

void main() {
    vec4 pigment = texture(Sampler0, coatingUv);
    if (pigment.a < 0.5 || (sourceCutout > 0.5 && texture(Sampler1, sourceUv).a < 0.5)) discard;
    vec4 color = vec4(pigment.rgb * lightColor.rgb * ColorModulator.rgb, 1.0);
    fragColor = apply_fog(color, sphereDistance, cylinderDistance, FogEnvironmentalStart, FogEnvironmentalEnd,
                         FogRenderDistanceStart, FogRenderDistanceEnd, FogColor);
}
