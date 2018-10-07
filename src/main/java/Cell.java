import lombok.Getter;
import org.joml.Vector3f;

import javax.xml.soap.Node;

/**
 * SVO Node
 * Designed for 4 bytes per node
 * First 3 are RGB, last one is node type:
 * EMPTY = 0, DATA = 0.5, INDEX = 1
 */
public class Cell {

    private static byte[] EMPTY_DATA = new byte[] {
            Byte.MIN_VALUE, Byte.MIN_VALUE, Byte.MIN_VALUE, Byte.MIN_VALUE
    };

    private Cell(byte[] data) {
        this.data = data;
    }

    public enum NodeType {
        EMPTY(Byte.MIN_VALUE),
        DATA((byte) 0),
        INDEX(Byte.MAX_VALUE);

        private byte value;
        NodeType(byte value) {
            this.value = value;
        }
    }

    private byte[] data;

    public NodeType getNodeType() {
        if (data == null) return NodeType.EMPTY;

        byte typeByte = data[3];
        if (typeByte == NodeType.EMPTY.value) return NodeType.EMPTY;
        if (typeByte == NodeType.INDEX.value) return NodeType.INDEX;
        return NodeType.DATA;
    }

    public byte[] getData() {
        if (getNodeType() == NodeType.EMPTY) return EMPTY_DATA;
        return data;
    }

    public Vector3f getRgb() {
        return new Vector3f(
                byteToFloat8(data[0]),
                byteToFloat8(data[1]),
                byteToFloat8(data[2])
        );
    }

    public int getIndex() {
        return ((data[0] << 8 & data[1]) << 8 & data[2]);
    }

    static float byteToFloat8(byte b) {
        return (b + Byte.MIN_VALUE) / 255f;
    }

    static byte float8ToByte(float f) {
        return (byte) ((int) (f * 255f) - Byte.MIN_VALUE);
    }

    public static Cell createEmpty() {
        return new Cell(null);
    }
    public static Cell createData(Vector3f rgb) {
        byte[] data = {
                float8ToByte(rgb.x),
                float8ToByte(rgb.y),
                float8ToByte(rgb.z),
                NodeType.DATA.value
        };
        return new Cell(data);
    }

    /**
     *
     * @param index Index in the 3D texture
     * @return
     */
    public static Cell createIndex(Vector3f index) {
        byte[] data = {
                float8ToByte(index.x),
                float8ToByte(index.y),
                float8ToByte(index.z),
                NodeType.INDEX.value
        };
        return new Cell(data);
    }
}
