package kenran.gfx;

import java.awt.*;
import java.awt.geom.Point2D;

public class GfxUtils {
    public static void drawCircle(Graphics2D g, Point2D center, double radius) {
        int x = (int)(center.getX() - radius);
        int y = (int)(center.getY() - radius);
        int diameter = (int)(2.0 * radius);
        g.drawOval(x, y, diameter, diameter);
    }
}