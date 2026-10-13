package minigames;

import java.awt.Rectangle;

public class Bullet {

    private int positionX;
    private int positionY;
    private int speedX;
    private int speedY;
    private int size;
    private int bounceCount;   // used in step 5, counts the bounces of this bullet
    private Tank owner;        // the tank that fired this bullet

    public Bullet(Tank owner, int startX, int startY, int speedX, int speedY) {
        this.owner = owner;
        this.positionX = startX;
        this.positionY = startY;
        this.speedX = speedX;
        this.speedY = speedY;
        this.size = 6;
        this.bounceCount = 0;
    }

    public Rectangle getBounds() {
        return new Rectangle(positionX, positionY, size, size);
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

    public int getSpeedX() {
        return speedX;
    }

    public void setSpeedX(int speedX) {
        this.speedX = speedX;
    }

    public int getSpeedY() {
        return speedY;
    }

    public void setSpeedY(int speedY) {
        this.speedY = speedY;
    }

    public int getSize() {
        return size;
    }

    public int getBounceCount() {
        return bounceCount;
    }

    public void setBounceCount(int bounceCount) {
        this.bounceCount = bounceCount;
    }

    public Tank getOwner() {
        return owner;
    }
}
