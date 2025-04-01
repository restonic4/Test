#version 330 core

// Input vertex data
layout (location=0) in vec3 position;
layout (location=1) in vec2 baseTexCoord;
layout (location=2) in vec3 normal;

// Uniforms
uniform mat4 modelMatrix;
uniform mat4 viewMatrix;
uniform mat4 projectionMatrix;

// Uniforms for atlas mapping
uniform vec2 uvOffset;
uniform vec2 uvScale;

// Outputs sent to the fragment shader
out vec2 outTexCoord;
out vec3 outNormal;

void main()
{
    // Calculate the final vertex position in clip space
    gl_Position = projectionMatrix * viewMatrix * modelMatrix * vec4(position, 1.0);

    // Pass other attributes to fragment shader
    outTexCoord = uvOffset + baseTexCoord * uvScale;
    //outNormal = mat3(transpose(inverse(modelMatrix))) * normal; // For lighting
    outNormal = normal;
}