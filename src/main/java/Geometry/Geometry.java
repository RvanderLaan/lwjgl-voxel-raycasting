package Geometry;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.joml.Vector3f;

public abstract class Geometry {

    @NonNull @Getter @Setter
    protected Vector3f color;

    /** Whether the surface of the geometry intersects with a box */
    public abstract boolean intersects(Vector3f boxStart, Vector3f boxEnd);
}


