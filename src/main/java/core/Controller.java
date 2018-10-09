package core;

import input.CursorHandler;
import input.KeyboardHandler;
import input.MouseButtonHandler;
import org.joml.Vector2f;
import org.joml.Vector3f;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_CONTROL;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_SPACE;

public class Controller {
    private Camera camera;

    private Vector2f prevCursorPos = new Vector2f();
    private Vector2f mouseSensitivity = new Vector2f(1);

    public Controller(Camera camera) {
        this.camera = camera;
    }

    Vector3f translationTemp = new Vector3f();
    public void update(float dt) {
        translationTemp.set(0);
        float factor = 0.1f;
        if (KeyboardHandler.isKeyDown(GLFW_KEY_LEFT_SHIFT))
            factor *= 3.0f;
        if (KeyboardHandler.isKeyDown(GLFW_KEY_LEFT_CONTROL))
            factor /= 3.0f;

        if (KeyboardHandler.isKeyDown(GLFW_KEY_W)) {
            translationTemp.add(0, 0, factor * dt);
        }
        if (KeyboardHandler.isKeyDown(GLFW_KEY_S)) {
            translationTemp.add(0, 0, -factor * dt);
        }
        if (KeyboardHandler.isKeyDown(GLFW_KEY_A)) {
            translationTemp.add(factor * dt, 0, 0);
        }
        if (KeyboardHandler.isKeyDown(GLFW_KEY_D)) {
            translationTemp.add(-factor * dt, 0, 0);
        }
        if (KeyboardHandler.isKeyDown(GLFW_KEY_LEFT_ALT)) {
            translationTemp.add(0, factor * dt, 0);
        }
        if (KeyboardHandler.isKeyDown(GLFW_KEY_SPACE)) {
            translationTemp.add(0, -factor * dt, 0);
        }
        camera.getPosition().add(translationTemp.rotate(camera.getRotation()));


        if (KeyboardHandler.isKeyDown(GLFW_KEY_Q)) {
            camera.getRotation().rotateLocalZ(-factor * dt);
        }
        if (KeyboardHandler.isKeyDown(GLFW_KEY_E)) {
            camera.getRotation().rotateLocalZ(factor * dt);
        }


        if (MouseButtonHandler.isButtonDown(0)) {

            // If mouse is down, compute the camera rotation based on mouse cursor location.
//            currRotationAboutY = rotationAboutY + (mouseX - mouseDownX) * 0.01f;
            camera.getRotation().rotateLocalY((CursorHandler.getCursorPos().x - prevCursorPos.x) * mouseSensitivity.x * dt);
            camera.getRotation().rotateLocalX((CursorHandler.getCursorPos().y - prevCursorPos.y) * mouseSensitivity.y * dt);
        }

        prevCursorPos.set(CursorHandler.getCursorPos());
    }
}
