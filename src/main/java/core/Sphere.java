package core;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.joml.Vector3f;

@RequiredArgsConstructor
public class Sphere extends Geometry {

    @NonNull private Vector3f origin;
    @NonNull private float radius;

    public Sphere(Vector3f origin, float radius, Vector3f color) {
        this.origin = origin;
        this.radius = radius;
        this.color = color;
    }

    private float squared(float x) { return x * x; }

    @Override
    public boolean intersects(Vector3f boxStart, Vector3f boxEnd) {
        // https://stackoverflow.com/questions/4578967/cube-sphere-intersection-test
        float R = radius;
        Vector3f S = origin;

        float dist_squared = R * R;
        /* assume C1 and C2 are element-wise sorted, if not, do that now */
        if (S.x < boxStart.x) dist_squared -= squared(S.x - boxStart.x);
        else if (S.x > boxEnd.x) dist_squared -= squared(S.x - boxEnd.x);
        if (S.y < boxStart.y) dist_squared -= squared(S.y - boxStart.y);
        else if (S.y > boxEnd.y) dist_squared -= squared(S.y - boxEnd.y);
        if (S.z < boxStart.z) dist_squared -= squared(S.z - boxStart.z);
        else if (S.z > boxEnd.z) dist_squared -= squared(S.z - boxEnd.z);
        return dist_squared > 0;
    }
}
