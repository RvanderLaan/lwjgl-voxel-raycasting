/*
 * Copyright LWJGL. All rights reserved.
 * License terms: https://www.lwjgl.org/license
 */
#version 430 core

/**
 * The framebuffer image we write to.
 */
layout(binding = 0, rgba32f) uniform image2D framebufferImage;

uniform sampler3D voxelTexture;

/**
 * Describes the view frustum of the camera via its world-space corner
 * edge vectors which we perform bilinear interpolation on to get the
 * world-space direction vector of a work item's framebuffer pixel.
 * See function main().
 */
uniform vec3 eye, ray00, ray01, ray10, ray11;

uniform float invNumberOfIndGrids;

#define LARGE_FLOAT 1E+10
#define NUM_BOXES 10
#define EPSILON 0.0001
#define HRDWTREE_MAX_DEPTH 16


/**
 * Describes an axis-aligned box by its minimum and maximum corner
 * oordinates.
 */
struct box {
  vec3 min, max;
};

/**
 * Our scene description is very simple. We just use a static array
 * of boxes, each defined by its minimum and maximum corner coordinates.
 */
const box boxes[NUM_BOXES] = {
  {vec3(-5.0, -0.1, -5.0), vec3(5.0, 0.0, 5.0)}, // <- bottom
  {vec3(-5.1, 0.0, -5.0), vec3(-5.0, 5.0, 5.0)}, // <- left
  {vec3(5.0, 0.0, -5.0), vec3(5.1, 5.0, 5.0)},   // <- right
  {vec3(-5.0, 0.0, -5.1), vec3(5.0, 5.0, -5.0)}, // <- back
  {vec3(-5.0, 0.0, 5.0), vec3(5.0, 5.0, 5.1)},   // <- front
  {vec3(-1.0, 1.0, -1.0), vec3(1.0, 1.1, 1.0)},   // <- table top
  {vec3(-1.0, 0.0, -1.0), vec3(-0.8, 1.0, -0.8)},   // <- table foot
  {vec3(-1.0, 0.0,  0.8), vec3(-0.8, 1.0, 1.0)},   // <- table foot
  {vec3(0.8, 0.0, -1.0), vec3(1.0, 1.0, -0.8)},   // <- table foot
  {vec3(0.8, 0.0,  0.8), vec3(1.0, 1.0, 1.0)}   // <- table foot
};

/**
 * Describes the first intersection of a ray with a box.
 */
struct hitinfo {
  /*
   * The value of the parameter 't' in the ray equation
   * `p = origin + dir * t` at which p is a point on one of the boxes
   * intersected by the ray.
   */
  float near;
  /*
   * The index of the box into the 'boxes' array.
   */
  int i;
};

/**
 * Compute whether the given ray `origin + t * dir` intersects the given
 * box 'b' and return the values of the parameter 't' at which the ray
 * enters and exists the box, called (tNear, tFar). If there is no
 * intersection then tNear > tFar or tFar < 0.
 */
vec2 intersectBox(vec3 origin, vec3 dir, const box b) {
  vec3 tMin = (b.min - origin) / dir;
  vec3 tMax = (b.max - origin) / dir;
  vec3 t1 = min(tMin, tMax);
  vec3 t2 = max(tMin, tMax);
  float tNear = max(max(t1.x, t1.y), t1.z);
  float tFar = min(min(t2.x, t2.y), t2.z);
  return vec2(tNear, tFar);
}

/**
 * Compute the closest intersection of the given ray `origin + t * dir`
 * with all boxes and return whether there was any intersection and
 * store the value of 't' at the intersection as well as the index of
 * the intersected box into the out-parameter 'info'.
 */
bool intersectBoxes(vec3 origin, vec3 dir, out hitinfo info) {
  float smallest = LARGE_FLOAT;
  bool found = false;
  for (int i = 0; i < NUM_BOXES; i++) {
    vec2 lambda = intersectBox(origin, dir, boxes[i]);
    if (lambda.y >= 0.0 && lambda.x < lambda.y && lambda.x < smallest) {
      info.near = lambda.x;
      info.i = i;
      smallest = lambda.x;
      found = true;
    }
  }
  return found;
}

uint hash3(uint x, uint y, uint z) {
  x += x >> 11;
  x ^= x << 7;
  x += y;
  x ^= x << 3;
  x += z ^ (x >> 14);
  x ^= x << 6;
  x += x >> 15;
  x ^= x << 5;
  x += x >> 12;
  x ^= x << 9;
  return x;
}
float random(vec3 f) {
  uint mantissaMask = 0x007FFFFFu;
  uint one = 0x3F800000u;
  uvec3 u = floatBitsToUint(f);
  uint h = hash3(u.x, u.y, u.z);
  return uintBitsToFloat((h & mantissaMask) | one) - 1.0;
}


vec4 treeLookup(vec3 m) {
    vec4 cell = vec4(0.0, 0.0, 0.0, 0.0);
    vec3 mnd = m;
    vec3 p;
    float pow2 = 1;

    for (float i = 0; i < HRDWTREE_MAX_DEPTH; i++) { // fixed # of iterations
        // already in a leaf?
        if (cell.w < 0.9) {
            // compute lookup coords. within current node
            p = cell.xyz + fract(m * pow2)* invNumberOfIndGrids;
//            p = cell.xyz + mnd * invNumberOfIndGrids;
            // continue to next depth
            cell = texture3D(voxelTexture, p); // maybe offset slightly? + vec3(0.05));
        }

        if (cell.w > 0.9)    // a leaf has been reached
            break;

        if (cell.w < 0.1) // empty cell
            return vec4(0);

        // compute pos within next depth grid
        mnd = mnd * 2;
        pow2 *= 2;
    }
    return cell;
}

/**
 * Given the ray `origin + t * dir` trace it through the scene,
 * checking for the nearest collision with any of the above defined
 * boxes, and return a computed color.
 *
 * @param origin the origin of the ray
 * @param dir the direction vector of the ray
 * @returns the computed color
 */
vec3 trace(vec3 origin, vec3 dir) {
    vec3 lookup = origin;

    // Todo: Intersect with cell borders at deepest depth instead of brute forcing samples

    // Start lookup always at bounds of unit cube
    // (this is brute force, should just clamp the ray between vec3(0) and vec3(1) along the dir
//    float distToUnitCube =
//    lookup += dir *
    for (float i = 0.0005;
        i < 1;
        i *= 1.05) {
//        lookup += dir * i;
        lookup += dir * i * random(lookup);

        if ((all(lessThan(lookup, vec3(1))) && all(greaterThanEqual(lookup, vec3(0)))))
            break;
    }

    // Do several lookups along rays from the camera viewpoint
    for (float i = 0.0005;
            all(lessThan(lookup, vec3(1))) && all(greaterThanEqual(lookup, vec3(0)));
            i *= 1.05) {

        vec4 cell = treeLookup(lookup);

        // Use this to look at the 3d volume texture directly
//        vec4 cell = texture3D(voxelTexture, lookup);

        // Use this to look up a color and use it as a lookup
//        vec3 lookup2 = texture3D(voxelTexture, lookup).rgb;
//        vec3 color = texture3D(voxelTexture, lookup2).rgb;

        if (cell.w != 0)
            return cell.rgb;
//        lookup += dir * i; // Larger steps further from the camera
        lookup += dir * i * random(lookup); // noisy borders
    }
    // If no lookup succeeds, return a background color
    return vec3(0);

  hitinfo hinfo;
  /* Intersect the ray with all boxes */
  if (!intersectBoxes(origin, dir, hinfo))
    return vec3(0.0); // <- nothing hit, return black
  /*
   * hitinfo will give use the index of the box.
   * So, get the actual box with that index.
   */
  box b = boxes[hinfo.i];
  /*
   * And compute some gray scale color based on the index to
   * just allow us to visually differentiate the boxes.
   */
  return vec3(float(hinfo.i+1) / NUM_BOXES);
//    return texture3D(voxelTexture, origin * 10).rgb;
}




layout (local_size_x = 16, local_size_y = 16) in;

/**
 * Entry point of this GLSL compute shader.
 */
void main(void) {
  /*
   * Obtain the 2D index of the current compute shader work item via
   * the built-in gl_GlobalInvocationID variable and store it in a 'px'
   * variable because we need it later.
   */
  ivec2 px = ivec2(gl_GlobalInvocationID.xy);
  /*
   * Also obtain the size of the framebuffer image. We could have used
   * a custom uniform for that as well. But GLSL already provides it as
   * a built-in function.
   */
  ivec2 size = imageSize(framebufferImage);
  /*
   * Because we have to execute our compute shader with a global work
   * size that is a power of two, we need to check whether the current
   * work item is still within our actual framebuffer dimension so that
   * we do not accidentally write to or read from unallocated memory
   * later.
   */
  if (any(greaterThanEqual(px, size)))
    return; // <- no work to do, return.
  /*
   * Now we take our rayNN uniforms declared above to determine the
   * world-space direction from the eye position through the current
   * work item's pixel's center in the framebuffer image. We use the
   * 'px' variable, cast it to a floating-point vector, offset it by
   * half a pixel's width (in whole pixel units) and then transform that
   * position relative to our framebuffer size to get values in the
   * interval [(0, 0), (1, 1)] for all work items covering our
   * framebuffer.
   */
  vec2 p = (vec2(px) + vec2(0.5)) / vec2(size);
  /*
   * Use bilinear interpolation based on the X and Y fraction
   * (within 0..1) with our rayNN vectors defining the world-space
   * vectors along the corner edges of the camera's view frustum. The
   * result is the world-space direction of the ray originating from the
   * camera/eye center through the work item's framebuffer pixel center.
   */
  vec3 dir = mix(mix(ray00, ray01, p.y), mix(ray10, ray11, p.y), p.x);
  /*
   * Now, trace the list of boxes with the ray `eye + t * dir`.
   * The result is a computed color which we will write at the work
   * item's framebuffer pixel.
   */
  vec3 color = trace(eye, normalize(dir));

//    vec3 color = texture3D(voxelTexture, vec3(p, eye.x)).rgb;

//    vec3 lookup = texture3D(voxelTexture, vec3(p, eye.x)).rgb;
//    vec3 color = texture3D(voxelTexture, lookup.xyz + vec3(0.05)).rgb;



  /*
   * Store the final color in the framebuffer's pixel of the current
   * work item.
   */
  imageStore(framebufferImage, px, vec4(color, 1.0));
}
