package kenran.util;

import robocode.AdvancedRobot;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import static robocode.util.Utils.normalRelativeAngle;

public class Utils {
    public static double bulletTravelTime(double distance, double bulletPower) {
        return distance / bulletVelocity(bulletPower);
    }

    public static double bulletVelocity(double bulletPower) {
        return 20.0 - 3.0 * bulletPower;
    }

    public static double wallSmoothing(
            Rectangle2D.Double field,
            Point2D.Double position,
            double angle,
            int orientation,
            double stickLength) {
        double targetAngle = angle;
        while (!field.contains(project(position, targetAngle, stickLength)))
        {
            targetAngle += orientation * 0.05;
        }
        return targetAngle;
    }

    public static Point2D.Double project(Point2D.Double position, double angle, double distance) {
        double x = position.getX() + Math.sin(angle) * distance;
        double y = position.getY() + Math.cos(angle) * distance;
        return new Point2D.Double(x, y);
    }

    public static double absoluteBearing(Point2D.Double p1, Point2D.Double p2) {
        return Math.atan2(p2.x - p1.x, p2.y - p1.y);
    }

    public static double limit(double min, double value, double max) {
        return Math.max(min, Math.min(value, max));
    }

    public static double maxEscapeAngle(double velocity) {
        return Math.asin(8.0 / velocity);
    }

    public static void setBackAsFront(AdvancedRobot bot, double targetAngle) {
        double angle = normalRelativeAngle(targetAngle - bot.getHeadingRadians());
        if (Math.abs(angle) > Math.PI / 2.0)
        {
            if (angle < 0)
            {
                bot.setTurnRightRadians(Math.PI + angle);
            }
            else
            {
                bot.setTurnLeftRadians(Math.PI - angle);
            }
            bot.setBack(100.0);
        }
        else
        {
            if (angle < 0)
            {
                bot.setTurnLeftRadians(-angle);
            }
            else
            {
                bot.setTurnRightRadians(angle);
            }
            bot.setAhead(100.0);
        }
    }

    public static double rollingAverage(double currentAverage, double newValue, double n, double weight) {
        return (currentAverage * n + newValue * weight) / (n + weight);
    }
}