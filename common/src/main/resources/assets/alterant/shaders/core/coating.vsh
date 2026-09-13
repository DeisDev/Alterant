#version 330
#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:projection.glsl>
#moj_import <minecraft:fog.glsl>
#moj_import <minecraft:sample_lightmap.glsl>

in vec3 Position;
in vec4 Color;
in vec2 UV0;
in ivec2 UV1;
in ivec2 UV2;
in vec3 Normal;
uniform sampler2D Sampler2;
out vec2 coatingUv;
out vec2 sourceUv;
out vec4 lightColor;
out float sourceCutout;
out float sphereDistance;
out float cylinderDistance;

void main() {
    vec3 position = Position + ModelOffset;
    gl_Position = ProjMat * ModelViewMat * vec4(position, 1.0);
    coatingUv = UV0;
    sourceUv = vec2(UV1 & ivec2(65535)) / 65535.0;
    sourceCutout = Normal.x;
    lightColor = Color * sample_lightmap(Sampler2, UV2);
    sphereDistance = fog_spherical_distance(position);
    cylinderDistance = fog_cylindrical_distance(position);
}
