package geometry;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.joml.Vector3f;

@RequiredArgsConstructor
public class Sphere extends Geometry {

    @NonNull @Getter
    private Vector3f origin;
    @NonNull @Getter
    private float radius;

    public Sphere(Vector3f origin, float radius, Vector3f color) {
        this.origin = origin;
        this.radius = radius;
        this.color = color;
    }

    private float squared(float x) { return x * x; }

    @Override
    public boolean intersects(Vector3f boxStart, Vector3f boxEnd) {
        // Only the surface of the sphere should produce intersections, not the whole volume
        // The surface is where the box intersects the volume but is not fully inside it
        boolean intersectsVolume = intersectsVolume(boxStart, boxEnd, radius);
        boolean insideVolume = insideVolume(boxStart, boxEnd, radius);
        return intersectsVolume && !insideVolume;
    }

    public boolean intersectsVolume(Vector3f boxStart, Vector3f boxEnd, float R) {
        // https://stackoverflow.com/questions/4578967/cube-sphere-intersection-test
        Vector3f S = origin;
        float dist_squared = 0;

        // Calculate the closest distance from the cube to the sphere
        if      (S.x < boxStart.x)  dist_squared    += squared(S.x - boxStart.x);
        else if (S.x > boxEnd.x)    dist_squared    += squared(S.x - boxEnd.x);
        if      (S.y < boxStart.y)  dist_squared    += squared(S.y - boxStart.y);
        else if (S.y > boxEnd.y)    dist_squared    += squared(S.y - boxEnd.y);
        if      (S.z < boxStart.z)  dist_squared    += squared(S.z - boxStart.z);
        else if (S.z > boxEnd.z)    dist_squared    += squared(S.z - boxEnd.z);

        // If the closest distance is within the radius, it intersects the sphere's volume
        return dist_squared <= R * R;
    }

    public boolean insideVolume(Vector3f boxStart, Vector3f boxEnd, float R) {
        // Based on https://stackoverflow.com/questions/4578967/cube-sphere-intersection-test
        Vector3f S = origin;
        float dist_squared = 0;

        // Calculate the furthest distance from the cube to the sphere
        if      (S.x < boxStart.x)  dist_squared    += squared(S.x - boxEnd.x);
        else if (S.x > boxEnd.x)    dist_squared    += squared(S.x - boxStart.x);
        else                        dist_squared    += Math.max(squared(S.x - boxEnd.x), squared(S.x - boxStart.x));

        if      (S.y < boxStart.y)  dist_squared    += squared(S.y - boxEnd.y);
        else if (S.y > boxEnd.y)    dist_squared    += squared(S.y - boxStart.y);
        else                        dist_squared    += Math.max(squared(S.y - boxEnd.y), squared(S.y - boxStart.y));

        if      (S.z < boxStart.z)  dist_squared    += squared(S.z - boxEnd.z);
        else if (S.z > boxEnd.z)    dist_squared    += squared(S.z - boxStart.z);
        else                        dist_squared    += Math.max(squared(S.z - boxEnd.z), squared(S.z - boxStart.z));

        // If the furthest distance is within the radius, the box is fully inside the sphere's volume
        return dist_squared <= R * R;
    }
}
