package kenran.radar;

import kenran.Bakko;
import robocode.ScannedRobotEvent;
import robocode.util.Utils;

import java.awt.geom.Point2D;

import static kenran.util.Utils.project;

public class LockingRadar {
    private final Bakko _bot;
    private final double _lockMultiplier;

    public LockingRadar(Bakko bot, double lockMultiplier) {
        _bot = bot;
        _lockMultiplier = lockMultiplier;
        _bot.setTurnRadarRightRadians(Double.POSITIVE_INFINITY);
    }

    public void onScannedRobot(ScannedRobotEvent e) {
        double enemyBearing = _bot.getHeadingRadians() + e.getBearingRadians();
        double radarAngle = enemyBearing - _bot.getRadarHeadingRadians();
        Point2D.Double enemyPosition = project(_bot.getPosition(), enemyBearing, e.getDistance());
        _bot.setEnemyPosition(enemyPosition);
        _bot.setTurnRadarRightRadians(_lockMultiplier * Utils.normalRelativeAngle(radarAngle));
    }

    public void checkForSlip() {
        if (_bot.getRadarTurnRemainingRadians() == 0.0 && !_bot.hasWon()) {
            System.out.println("\tRadar slipped. Readjusting...");
            _bot.setTurnRadarLeftRadians(Double.POSITIVE_INFINITY);
        }
    }
}