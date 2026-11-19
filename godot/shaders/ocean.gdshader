shader_type spatial;
render_mode cull_disabled, diffuse_burley, specular_schlick_ggx;
uniform vec4 deep_color : source_color = vec4(0.035,0.24,0.33,1.0);
uniform vec4 shallow_color : source_color = vec4(0.09,0.58,0.59,1.0);
uniform vec4 foam_color : source_color = vec4(0.78,0.91,0.81,1.0);
uniform float wave_speed = 0.48;
uniform float wave_height = 0.09;
varying vec3 world_position;
void vertex() {
    float a = sin(VERTEX.x * 0.53 + TIME * wave_speed);
    float b = cos(VERTEX.z * 0.69 - TIME * wave_speed * 0.77);
    VERTEX.y += (a + b) * wave_height;
    world_position = (MODEL_MATRIX * vec4(VERTEX,1.0)).xyz;
}
void fragment() {
    float wave = sin(world_position.x * 0.17 + TIME * 0.3) *
                 cos(world_position.z * 0.23 - TIME * 0.2);
    float current = smoothstep(-0.4,0.75,wave);
    vec3 water = mix(deep_color.rgb, shallow_color.rgb, current * 0.35);
    float foam = smoothstep(0.82,0.99, sin(world_position.x * 1.2 + world_position.z * 0.7 + TIME * 0.5));
    ALBEDO = mix(water,foam_color.rgb,foam*0.018);
    ROUGHNESS = 0.4;
    METALLIC = 0.0;
    SPECULAR = 0.18;
}
