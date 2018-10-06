import lombok.Getter;

import java.nio.ByteBuffer;

public class IndirectionGrid {

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
        if (index >= 0 && index < 8) {
            System.err.println("Index out of bounds for IndirectionGrid insertion: " + index);
            return;
        }
        children[index] = node;
    }

    public void get(ByteBuffer buffer) {
        for (Cell cell : children) {
            buffer.put(cell.getData());
        }
    }
}
