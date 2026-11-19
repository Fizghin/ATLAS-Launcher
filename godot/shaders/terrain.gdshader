shader_type spatial;
render_mode cull_back, diffuse_burley;
uniform vec4 sand_color : source_color = vec4(0.80,0.68,0.44,1.0);
uniform vec4 grass_color : source_color = vec4(0.33,0.48,0.28,1.0);
uniform float texture_scale = 2.0;
varying vec3 world_position;
void vertex() {
    world_position = (MODEL_MATRIX * vec4(VERTEX,1.0)).xyz;
}
void fragment() {
    float noise = sin(world_position.x * 3.4) * sin(world_position.z * 2.8);
    float grains = sin(world_position.x * 13.0 + world_position.z * 9.0);
    float mix_amount = smoothstep(0.1,0.85,noise*0.25+0.55);
    ALBEDO = mix(sand_color.rgb,grass_color.rgb,mix_amount)* (0.93+0.07*grains);
    ROUGHNESS = 0.94;
}
