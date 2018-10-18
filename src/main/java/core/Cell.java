package core;

import org.joml.Vector3f;
import org.joml.Vector4f;

import java.nio.ByteBuffer;

/**
 * core.SVO Node
 * Designed for 4 bytes per node
 * First 3 are RGB, last one is node type:
 * EMPTY = 0, DATA = 0.5, INDEX = 1
 */
public class Cell {

    private Vector4f data;

    private static Vector4f EMPTY_DATA = new Vector4f(0);

    private Cell(Vector4f data) {
        this.data = data;
    }

    public enum NodeType {
        EMPTY(0),
        INDEX(0.5f),
        DATA(1);

        private float value;
        NodeType(float value) {
            this.value = value;
        }
    }



    public NodeType getNodeType() {
        if (data == null) return NodeType.EMPTY;

        float type = data.w;
        if (type < 0.1) return NodeType.EMPTY;
        if (type < 0.6) return NodeType.INDEX;
        return NodeType.DATA;
    }

    public void getData(int position, ByteBuffer buffer) {
        Vector4f realData = data;
        if (getNodeType() == NodeType.EMPTY) {
            realData = EMPTY_DATA;
        }
//        realData.get(position, buffer);
        buffer.put(position + 0, (byte) Math.floor(realData.x * 255f));
        buffer.put(position + 1, (byte) Math.floor(realData.y * 255f));
        buffer.put(position + 2, (byte) Math.floor(realData.z * 255f));
        buffer.put(position + 3, (byte) Math.floor(realData.w * 255f));
    }

    public static Cell createEmpty() {
        return new Cell(null);
    }
    public static Cell createData(Vector3f rgb) {
        return new Cell(new Vector4f(rgb, NodeType.DATA.value));
    }

    /**
     *
     * @param index Index in the 3D texture
     * @return
     */
    public static Cell createIndex(Vector3f index) {
        return new Cell(new Vector4f(index, NodeType.INDEX.value));
    }
}
