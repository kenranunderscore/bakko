package kenran.offense;

import kenran.Bakko;
import robocode.Rules;
import robocode.ScannedRobotEvent;

import java.awt.*;
import java.awt.geom.Point2D;

import static kenran.util.Utils.project;
import static robocode.util.Utils.normalAbsoluteAngle;
import static robocode.util.Utils.normalRelativeAngle;

public class CircularPredictor implements Weapon {
    private final Bakko _bakko;
    private double _oldHeading = 0.0;
    private Point2D.Double _predictedPosition = new Point2D.Double();

    CircularPredictor(Bakko bakko) {
        _bakko = bakko;
    }

    @Override
    public void onScannedRobot(ScannedRobotEvent e) {
        double bulletPower = Math.min(2.5, _bakko.getEnergy());
        double enemyBearing = _bakko.getHeadingRadians() + e.getBearingRadians();
        Point2D.Double predictedPosition = project(_bakko.getPosition(), enemyBearing, e.getDistance());
        double enemyHeading = e.getHeadingRadians();
        double enemyHeadingChange = enemyHeading - _oldHeading;
        double enemyVelocity = e.getVelocity();
        _oldHeading = enemyHeading;
        double turnCount = 0.0;
        double battleFieldHeight = _bakko.getBattleFieldHeight();
        double battleFieldWidth = _bakko.getBattleFieldWidth();
        while(++turnCount * Rules.getBulletSpeed(bulletPower) < _bakko.getPosition().distance(predictedPosition)){
            predictedPosition.x += Math.sin(enemyHeading) * enemyVelocity;
            predictedPosition.y += Math.cos(enemyHeading) * enemyVelocity;
            enemyHeading += enemyHeadingChange;
            if(	predictedPosition.x < 18.0
                    || predictedPosition.y < 18.0
                    || predictedPosition.x > battleFieldWidth - 18.0
                    || predictedPosition.y > battleFieldHeight - 18.0){

                predictedPosition.x = Math.min(Math.max(18.0, predictedPosition.x),
                        battleFieldWidth - 18.0);
                predictedPosition.y = Math.min(Math.max(18.0, predictedPosition.y),
                        battleFieldHeight - 18.0);
                break;
            }
        }
        _predictedPosition.setLocation(predictedPosition);
        double theta = normalAbsoluteAngle(Math.atan2(
                predictedPosition.x - _bakko.getX(), predictedPosition.y - _bakko.getY()));
        _bakko.setTurnGunRightRadians(normalRelativeAngle(
                theta - _bakko.getGunHeadingRadians()));
        _bakko.setFire(bulletPower);
    }

    @Override
    public void onPaint(Graphics2D g) {
        g.setColor(Color.CYAN);
        g.drawLine(
                (int)_bakko.getPosition().getX(),
                (int)_bakko.getPosition().getY(),
                (int)_predictedPosition.getX(),
                (int)_predictedPosition.getY());
    }
}