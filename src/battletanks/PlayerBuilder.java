package battletanks;

public class PlayerBuilder {

    private WallShape currentShape;      // the shape the player is moving right now
    private int positionX;               // top left corner of the shape
    private int positionY;
    private int objectsLeftToPlace;

    public PlayerBuilder() {
        this.currentShape = null;
        this.positionX = 0;
        this.positionY = 0;
        this.objectsLeftToPlace = 0;
    }

    // A player is finished when there is nothing left to place
    public boolean isFinished() {
        return objectsLeftToPlace <= 0;
    }

    public WallShape getCurrentShape() {
        return currentShape;
    }

    public void setCurrentShape(WallShape currentShape) {
        this.currentShape = currentShape;
    }

    public int getPositionX() {
        return positionX;
    }

    public void setPositionX(int positionX) {
        this.positionX = positionX;
    }

    public int getPositionY() {
        return positionY;
    }

    public void setPositionY(int positionY) {
        this.positionY = positionY;
    }

    public int getObjectsLeftToPlace() {
        return objectsLeftToPlace;
    }

    public void setObjectsLeftToPlace(int objectsLeftToPlace) {
        this.objectsLeftToPlace = objectsLeftToPlace;
    }
}
