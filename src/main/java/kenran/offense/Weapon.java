package kenran.offense;

import kenran.data.ShotInfo;
import robocode.ScannedRobotEvent;

import java.awt.*;

public interface Weapon {
    ShotInfo onScannedRobot(ScannedRobotEvent e);
    void onPaint(Graphics2D g);
    void setActive(boolean isActive);
}