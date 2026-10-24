package battletanks;

// A predefined object that the players can place, made of wall blocks

public class WallShape {

    // Each row is one block: {column, row}.
    // The block is drawn at column * block size, row * block size from the corner of the shape.
    private int[][] blockPositions;

    public WallShape(int[][] blockPositions) {
        this.blockPositions = blockPositions;
    }

    // 3 blocks next to each other: ###
    public static WallShape createHorizontalWall() {
        int[][] positions = {
            {0, 0}, {1, 0}, {2, 0}
        };
        return new WallShape(positions);
    }

    // 3 blocks above each other
    public static WallShape createVerticalWall() {
        int[][] positions = {
            {0, 0}, {0, 1}, {0, 2}
        };
        return new WallShape(positions);
    }

    public int getBlockCount() {
        return blockPositions.length;
    }

    public int getBlockColumn(int blockIndex) {
        return blockPositions[blockIndex][0];
    }

    public int getBlockRow(int blockIndex) {
        return blockPositions[blockIndex][1];
    }

    // Width of the whole shape in pixels
    public int getWidth() {
        int highestColumn = 0;

        for (int blockIndex = 0; blockIndex < blockPositions.length; blockIndex++) {
            if (getBlockColumn(blockIndex) > highestColumn) {
                highestColumn = getBlockColumn(blockIndex);
            }
        }

        return (highestColumn + 1) * WallBlock.DEFAULT_SIZE;
    }

    // Height of the whole shape in pixels
    public int getHeight() {
        int highestRow = 0;

        for (int blockIndex = 0; blockIndex < blockPositions.length; blockIndex++) {
            if (getBlockRow(blockIndex) > highestRow) {
                highestRow = getBlockRow(blockIndex);
            }
        }

        return (highestRow + 1) * WallBlock.DEFAULT_SIZE;
    }
}
