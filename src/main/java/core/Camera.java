package core;

import lombok.Getter;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class Camera {

    @Getter
    protected Vector3f position;
    @Getter
    protected Quaternionf rotation;

    public static Vector3f UP = new Vector3f(0, 1, 0);

    public Camera(Vector3f pos) {
        this.position = pos;
        // Look towards negative Z by default (Y is up)
        rotation = new Quaternionf().lookAlong(new Vector3f(0, 0, -1), UP);
    }

    public void lookAt(Vector3f dir) {
        rotation.lookAlong(dir, UP);
    }
}
