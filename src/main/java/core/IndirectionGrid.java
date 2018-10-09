package core;

import lombok.Getter;
import org.joml.Vector3f;
import org.joml.Vector3i;
import org.lwjgl.BufferUtils;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

public class IndirectionGrid {

    private static Vector3i[] CHILD_TEXTURE_OFFSETS = {
            new Vector3i(0, 0, 0),
            new Vector3i(1, 0, 0),
            new Vector3i(0, 1, 0),
            new Vector3i(1, 1, 0),
            new Vector3i(0, 0, 1),
            new Vector3i(1, 0, 1),
            new Vector3i(0, 1, 1),
            new Vector3i(1, 1, 1),
    };

    @Getter
    private Cell[] children;

    @Getter
    private int depth = 0;

    @Getter
    private int index = 0;

    public IndirectionGrid() {
        children = new Cell[8];
        for (int i = 0; i < 8; i++)
            children[i] = Cell.createEmpty();
    }

    public void setNode(int x, int y, int z, Cell node) {
        int index = x + 2 * y + 4 * z;
        setNode(index, node);
    }

    public void setNode(int index, Cell node) {
        if (index < 0 || index >= 8) {
            System.err.println("Index out of bounds for core.IndirectionGrid insertion: " + index);
            return;
        }
        children[index] = node;
    }

    /**
     * Inserts the cells of this indirection grid in a 2x2x2 cube in a bytebuffer texture
     * @param textureSize
     * @param x
     * @param y
     * @param z
     * @param buffer
     */
    public void get(int textureSize, int x, int y, int z, ByteBuffer buffer) {
        for (int i = 0; i < 8; i++) {
            Cell cell = children[i];
            Vector3i of = CHILD_TEXTURE_OFFSETS[i];
            int textureIndex = (x + of.x) + (y + of.y) * textureSize + (z + of.z) * textureSize * textureSize;
//            System.out.println("textureIndex = " + textureIndex + " (=" + textureIndex * 4 + ")");
            cell.getData(textureIndex * 4, buffer);
        }
    }
}
