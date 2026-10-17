package f1_game.base;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

/**
 * KeyHandler = listens to the keyboard.
 *
 * Java tells us "a key went DOWN" (keyPressed) and "a key went UP" (keyReleased).
 * We just remember for every key we care about if it is held right now (true/false).
 * The game then asks these booleans every frame, e.g. keys.isUpPressed().
 *
 * We support BOTH WASD and the arrow keys.
 */
public class KeyHandler extends KeyAdapter {

    private boolean upPressed;
    private boolean downPressed;
    private boolean leftPressed;
    private boolean rightPressed;
    private boolean restartPressed;

    @Override
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_F3) {
            PerfMonitor.toggle(); // dev overlay: frame timing numbers
        }
        setKey(e.getKeyCode(), true);
    }

    @Override
    public void keyReleased(KeyEvent e) {
        setKey(e.getKeyCode(), false);
    }

    /** Sets the right boolean to true (pressed) or false (released). */
    private void setKey(int keyCode, boolean pressed) {
        switch (keyCode) {
            case KeyEvent.VK_W, KeyEvent.VK_UP -> upPressed = pressed;
            case KeyEvent.VK_S, KeyEvent.VK_DOWN -> downPressed = pressed;
            case KeyEvent.VK_A, KeyEvent.VK_LEFT -> leftPressed = pressed;
            case KeyEvent.VK_D, KeyEvent.VK_RIGHT -> rightPressed = pressed;
            case KeyEvent.VK_R -> restartPressed = pressed;
            default -> {
                // any other key: we don't care
            }
        }
    }

    public boolean isUpPressed() {
        return upPressed;
    }

    public boolean isDownPressed() {
        return downPressed;
    }

    public boolean isLeftPressed() {
        return leftPressed;
    }

    public boolean isRightPressed() {
        return rightPressed;
    }

    public boolean isRestartPressed() {
        return restartPressed;
    }
}
