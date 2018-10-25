package Geometry;

import lombok.AllArgsConstructor;
import org.joml.Vector3f;

@AllArgsConstructor
public class Line extends Geometry {

    private Vector3f lineStart, lineEnd;

    @Override
    /**
     * Based on https://stackoverflow.com/questions/3106666/intersection-of-line-segment-with-axis-aligned-box-in-c-sharp
     */
    public boolean intersects(Vector3f boxStart, Vector3f boxEnd) {
        Vector3f beginToEnd = lineEnd.sub(lineStart, new Vector3f());
        Vector3f beginToMin = boxStart.sub(lineStart, new Vector3f());
        Vector3f beginToMax = boxEnd.sub(lineStart, new Vector3f());
        float tNear = -Float.MAX_VALUE;
        float tFar = Float.MAX_VALUE;

        for (int axis = 0; axis < 3; axis++) {
            if (beginToEnd.get(axis) == 0) { // parallel
                if (beginToMin.get(axis) > 0 || beginToMax.get(axis) < 0)
                    return false; // segment is not between planes
            } else {
                float t1 = beginToMin.get(axis) / beginToEnd.get(axis);
                float t2 = beginToMax.get(axis) / beginToEnd.get(axis);
                float tMin = Math.min(t1, t2);
                float tMax = Math.max(t1, t2);
                if (tMin > tNear) tNear = tMin;
                if (tMax < tFar) tFar = tMax;
                if (tNear > tFar || tFar < 0) return false;
            }
        }
        return (tNear >= 0 && tNear <= 1) || (tFar >= 0 && tFar <= 1);
    }
}
