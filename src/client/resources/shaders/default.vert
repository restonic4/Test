#version 330 core

// Input vertex data
layout (location=0) in vec3 position;
layout (location=1) in vec2 baseTexCoord;
layout (location=2) in vec3 normal;
layout (location=3) in mat4 instanceModelMatrix;
layout (location=4) in vec2 instanceUvOffset;
layout (location=5) in vec2 instanceUvScale;

// Uniforms
uniform mat4 viewMatrix;
uniform mat4 projectionMatrix;

// Outputs sent to the fragment shader
out vec2 outTexCoord;
out vec3 outNormal;

void main()
{
    // Calculate the final vertex position in clip space
    gl_Position = projectionMatrix * viewMatrix * instanceModelMatrix * vec4(position, 1.0);

    // Pass other attributes to fragment shader
    outTexCoord = instanceUvOffset + baseTexCoord * instanceUvScale;
    //outNormal = mat3(transpose(inverse(modelMatrix))) * normal; // For lighting
    outNormal = normal;
}