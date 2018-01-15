package kenran.offense;

import kenran.Bakko;
import kenran.defense.MovementDeque;
import kenran.data.MovementState;
import robocode.Rules;
import robocode.ScannedRobotEvent;

import java.awt.*;
import java.awt.geom.Point2D;
import java.util.ArrayList;

import static kenran.util.Utils.absoluteBearing;
import static kenran.util.Utils.bulletTravelTime;
import static robocode.util.Utils.normalRelativeAngle;

public class WarAxe implements Weapon {
    private static final int MAX_RECORD_LENGTH = 3000;
    private static final int RECENT_PATTERN_LENGTH = 12;
    private static final int MINIMUM_NUMBER_OF_RECORDS = 50;
    private static final double DEFAULT_BULLET_POWER = 1.9;
    private static final ArrayList<MovementState> _record = new ArrayList<>(MAX_RECORD_LENGTH);
    private final MovementDeque _recent = new MovementDeque(RECENT_PATTERN_LENGTH);
    private double _enemyHeading = 0.0;
    private Bakko _bakko;

    WarAxe(Bakko bakko) {
        _bakko = bakko;
        for (int i = 0; i < _recent.getMaxSize(); i++) {
            _recent.add(new MovementState(0.0, Rules.MAX_VELOCITY));
        }
    }

    @Override
    public void onScannedRobot(ScannedRobotEvent e) {
        double _deltaHeading = e.getHeadingRadians() - _enemyHeading;
        _enemyHeading = e.getHeadingRadians();
        record(_deltaHeading, e.getVelocity());
        double power = bulletPower(e.getEnergy(), e.getDistance());
        if (_record.size() > MINIMUM_NUMBER_OF_RECORDS) {
            Point2D.Double predictedPosition = predictPosition(power);
            double predictedHeading = absoluteBearing(_bakko.getPosition(), predictedPosition);
            _bakko.setTurnGunRightRadians(normalRelativeAngle(predictedHeading - _bakko.getGunHeadingRadians()));
        } else {
            double enemyBearing = _bakko.getHeadingRadians() + e.getBearingRadians();
            _bakko.setTurnGunRightRadians(normalRelativeAngle(enemyBearing - _bakko.getGunHeadingRadians()));
        }
        _bakko.setFire(power);
    }

    private double bulletPower(double enemyEnergy, double distance) {
        if (enemyEnergy < 0.1) {
            return Rules.MIN_BULLET_POWER;
        }
        double power = distance < 150 ? Rules.MAX_BULLET_POWER : DEFAULT_BULLET_POWER;
        double powerToKill = enemyEnergy >= 16.0 ? (enemyEnergy + 2.0) / 6.0 : enemyEnergy / 4.0;
        power = Math.min(powerToKill, power);
        power = Math.max(Rules.MIN_BULLET_POWER, power);
        power = Math.min(_bakko.getEnergy(), power);
        return power;
    }

    private void record(double turnRate, double velocity) {
        MovementState ms = new MovementState(turnRate, velocity);
        _recent.add(ms);
        _record.add(ms);
        if (_record.size() >= MAX_RECORD_LENGTH) {
            _record.remove(0);
        }
    }

    private int lastIndexOfMatchingSeries() {
        MovementDeque iterator = new MovementDeque(RECENT_PATTERN_LENGTH);
        for (int i = 0; i < iterator.getMaxSize(); i++) {
            iterator.add(_record.get(i));
        }
        double minimumDistance = iterator.compare(_recent);
        int indexOfMinimumDistance = _recent.getMaxSize() - 1;
        int i = _recent.getMaxSize();
        do {
            MovementState ms = _record.get(i);
            iterator.add(ms);
            double distance = iterator.compare(_recent);
            if (distance <= minimumDistance) {
                minimumDistance = distance;
                indexOfMinimumDistance = i;
            }
            i++;
        } while (i < _record.size() - MINIMUM_NUMBER_OF_RECORDS);
        return indexOfMinimumDistance;
    }

    private Point2D.Double predictPosition(double power) {
        int i = lastIndexOfMatchingSeries();
        Point2D.Double predictedPosition = (Point2D.Double) _bakko.getEnemyPosition().clone();
        double heading = _enemyHeading;
        double travelTime = 0.0;
        double turns = 0.0;
        for (int j = i + 1; j < _record.size() && turns <= travelTime; j++) {
            MovementState ms = _record.get(j);
            predictedPosition.x += Math.sin(heading) * ms.getVelocity();
            predictedPosition.y += Math.cos(heading) * ms.getVelocity();
            heading += ms.getTurnRate();
            travelTime = bulletTravelTime(_bakko.getPosition().distance(predictedPosition), power);
            turns += 1.0;
        }
        return predictedPosition;
    }

    @Override
    public void onPaint(Graphics2D g) {
    }
}