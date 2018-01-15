package kenran.offense;

import robocode.ScannedRobotEvent;

import java.awt.*;

public interface Weapon {
    void onScannedRobot(ScannedRobotEvent e);
    void onPaint(Graphics2D g);
}