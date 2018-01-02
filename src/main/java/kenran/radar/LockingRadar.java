package kenran.radar;

import kenran.Bakko;
import robocode.ScannedRobotEvent;
import robocode.util.Utils;

public class LockingRadar {
    private final Bakko _bot;
    private final double _lockMultiplier;

    public LockingRadar(Bakko bot, double lockMultiplier) {
        _bot = bot;
        _lockMultiplier = lockMultiplier;
    }

    public void onScannedRobot(ScannedRobotEvent e) {
        double enemyBearing = _bot.getHeadingRadians() + e.getBearingRadians();
        double angle = enemyBearing - _bot.getRadarHeadingRadians();
        _bot.setTurnRadarRightRadians(_lockMultiplier * Utils.normalRelativeAngle(angle));
    }
}