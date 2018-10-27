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
    private SVO svo;

    private Vector2f prevCursorPos = new Vector2f();
    private Vector2f mouseSensitivity = new Vector2f(1);

    public Controller(Camera camera, SVO svo) {
        this.camera = camera;
        this.svo = svo;
    }

    private Vector3f localTranslationTemp = new Vector3f();
    private Vector3f globalTranslationTemp = new Vector3f();

    public void update(float dt) {
        localTranslationTemp.set(0);
        globalTranslationTemp.set(0);
        float factor = 0.1f;
        if (KeyboardHandler.isKeyDown(GLFW_KEY_LEFT_SHIFT))
            factor *= 3.0f;
        if (KeyboardHandler.isKeyDown(GLFW_KEY_LEFT_CONTROL))
            factor /= 10.0f;

        if (KeyboardHandler.isKeyDown(GLFW_KEY_W)) {
            localTranslationTemp.add(0, 0, factor * dt);
        }
        if (KeyboardHandler.isKeyDown(GLFW_KEY_S)) {
            localTranslationTemp.add(0, 0, -factor * dt);
        }
        if (KeyboardHandler.isKeyDown(GLFW_KEY_A)) {
            localTranslationTemp.add(factor * dt, 0, 0);
        }
        if (KeyboardHandler.isKeyDown(GLFW_KEY_D)) {
            localTranslationTemp.add(-factor * dt, 0, 0);
        }
        if (KeyboardHandler.isKeyDown(GLFW_KEY_LEFT_ALT)) {
            globalTranslationTemp.add(0, -factor * dt, 0);
        }
        if (KeyboardHandler.isKeyDown(GLFW_KEY_SPACE)) {
            globalTranslationTemp.add(0, factor * dt, 0);
        }
        camera.getPosition().add(localTranslationTemp.rotate(camera.getRotation()));
        camera.getPosition().add(globalTranslationTemp);


//        if (KeyboardHandler.isKeyDown(GLFW_KEY_Q)) {
//            camera.getRotation().rotateZ(-factor * dt);
//        }
//        if (KeyboardHandler.isKeyDown(GLFW_KEY_E)) {
//            camera.getRotation().rotateZ(factor * dt);
//        }


        if (MouseButtonHandler.isButtonDown(0)) {

            // If mouse is down, compute the camera rotation based on mouse cursor location.
//            currRotationAboutY = rotationAboutY + (mouseX - mouseDownX) * 0.01f;
            camera.getRotation().rotateLocalY(-(CursorHandler.getCursorPos().x - prevCursorPos.x) * mouseSensitivity.x * dt);
            camera.getRotation().rotateX((CursorHandler.getCursorPos().y - prevCursorPos.y) * mouseSensitivity.y * dt);
        }

        prevCursorPos.set(CursorHandler.getCursorPos());





        // Gravity: check voxel below camera
//        Vector3f m = camera.getPosition().sub(0, 1/(float) svo.getMaxTextureSize(), 0, new Vector3f());
//        IndirectionGrid lookup = svo.getIndirectionPool().get(0);
//        for (int i = 0; i < svo.getMaxDepth(); i++) {
//
//            lookup = svo.getIndirectionPool().get()
//        }
    }
}
