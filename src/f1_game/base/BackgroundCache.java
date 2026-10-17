package f1_game.base;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Transparency;
import java.awt.image.BufferedImage;

/**
 * BackgroundCache = the grass on the left and right of the road, painted ONCE.
 *
 * WHY?
 * The grass never moves, but in fullscreen it is dozens of scaled tiles. Painting all of
 * them again every frame took ~15 ms (a frame only has ~16 ms!) and made the game stutter.
 * So we paint them once into a picture the size of the panel and only copy that picture
 * every frame, which is very fast. When the window changes size we paint it again.
 */
public class BackgroundCache {

    private BufferedImage cached; // the finished grass picture (null = not painted yet)

    /** Copies the grass onto the screen. Paints it first if the panel size changed. */
    public void draw(Graphics2D g, Road road, int panelWidth, int panelHeight) {
        if (cached == null || cached.getWidth() != panelWidth || cached.getHeight() != panelHeight) {
            cached = paint(road, panelWidth, panelHeight);
        }
        g.drawImage(cached, 0, 0, null);
    }

    /** Paints the grass tiles into a new picture (only on the first frame or after a resize). */
    private BufferedImage paint(Road road, int panelWidth, int panelHeight) {
        BufferedImage image = ImageLoader.createImage(panelWidth, panelHeight, Transparency.OPAQUE);
        Graphics2D g = image.createGraphics();
        try {
            g.setColor(Color.BLACK);
            g.fillRect(0, 0, panelWidth, panelHeight);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

            ScreenFit fit = ScreenFit.of(panelWidth, panelHeight);
            road.drawBackground(g, panelWidth, panelHeight, fit.roadLeftX(), fit.roadRightX(), fit.tileSize());
        } finally {
            g.dispose(); // free the "pen"
        }
        return image;
    }
}
