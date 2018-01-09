package kenran.eyes;

import kenran.Bakko;
import robocode.ScannedRobotEvent;
import robocode.util.Utils;

import java.awt.geom.Point2D;

import static kenran.util.Utils.project;

public class HeroicGaze {
    private final Bakko _bakko;
    private final double _lockMultiplier;

    public HeroicGaze(Bakko bakko, double lockMultiplier) {
        _bakko = bakko;
        _lockMultiplier = lockMultiplier;
        _bakko.setTurnRadarRightRadians(Double.POSITIVE_INFINITY);
    }

    public void onScannedRobot(ScannedRobotEvent e) {
        double enemyBearing = _bakko.getHeadingRadians() + e.getBearingRadians();
        double radarAngle = enemyBearing - _bakko.getRadarHeadingRadians();
        Point2D.Double enemyPosition = project(_bakko.getPosition(), enemyBearing, e.getDistance());
        _bakko.setEnemyPosition(enemyPosition);
        _bakko.setTurnRadarRightRadians(_lockMultiplier * Utils.normalRelativeAngle(radarAngle));
    }

    public void lookForEscapedEnemy() {
        if (_bakko.getRadarTurnRemainingRadians() == 0.0 && !_bakko.hasWon()) {
            System.out.println("\tRadar slipped. Readjusting...");
            _bakko.setTurnRadarLeftRadians(Double.POSITIVE_INFINITY);
        }
    }
}