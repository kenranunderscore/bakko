package kenran.gun;

import kenran.Bakko;
import kenran.movement.MovementDeque;
import kenran.util.MovementState;
import robocode.Rules;
import robocode.ScannedRobotEvent;

import java.awt.geom.Point2D;
import java.util.ArrayList;

import static kenran.util.Utils.absoluteBearing;
import static kenran.util.Utils.bulletTravelTime;
import static robocode.util.Utils.normalAbsoluteAngle;
import static robocode.util.Utils.normalRelativeAngle;

public class PatternMatcher {
    private static final int MAX_RECORD_LENGTH = 3000;
    private static final int RECENT_PATTERN_LENGTH = 12;
    private static final ArrayList<MovementState> _record = new ArrayList<>(3000);
    private final MovementDeque _recent = new MovementDeque(RECENT_PATTERN_LENGTH);
    private double _enemyHeading = 0.0;
    private double _enemyEnergy = 100.0;
    private Bakko _bot;

    public PatternMatcher(Bakko bot) {
        _bot = bot;
        for (int i = 0; i < _recent.getMaxSize(); i++) {
            _recent.add(new MovementState(0.0, Rules.MAX_VELOCITY));
        }
    }

    public void onScannedRobot(ScannedRobotEvent e) {
        double _deltaHeading = e.getHeadingRadians() - _enemyHeading;
        _enemyHeading = e.getHeadingRadians();
        double enemyBearing = _bot.getHeadingRadians() + e.getBearingRadians();
        _enemyEnergy = e.getEnergy();
        if (_enemyEnergy < 0.1) {
            //TODO: Check if this even works if the enemy heals.
            //TODO: return after?
            _bot.stop();
            _bot.setTurnGunRightRadians(enemyBearing);
            _bot.setFire(0.1);
        }
        record(_deltaHeading, e.getVelocity());
        double power = bulletPower();
        if (_bot.getRoundNum() != 0) {
            Point2D.Double predictedPosition = predictPosition(power);
            double predictedHeading = absoluteBearing(_bot.getPosition(), predictedPosition);
            _bot.setTurnGunRightRadians(normalRelativeAngle(predictedHeading - _bot.getGunHeadingRadians()));
        } else {
            double bulletVelocity = Rules.getBulletSpeed(power);
            double turns = 0.0;
            Point2D.Double predictedPosition = (Point2D.Double)_bot.getEnemyPosition().clone();
            double heading = _enemyHeading;
            double battleFieldWidth = _bot.getBattleFieldWidth();
            double battleFieldHeight = _bot.getBattleFieldHeight();
            while (++turns * bulletVelocity < _bot.getPosition().distance(predictedPosition)) {
                predictedPosition.x += Math.sin(heading) * e.getVelocity();
                predictedPosition.y += Math.cos(heading) * e.getVelocity();
                heading += _deltaHeading;
                if (predictedPosition.getX() < 17.9 || predictedPosition.getY() < 17.9 || predictedPosition.getX() > battleFieldWidth - 17.9 || predictedPosition.getY() > battleFieldHeight - 17.9) {
                    predictedPosition.x = Math.min(Math.max(18.0, predictedPosition.x), battleFieldWidth - 18.0);
                    predictedPosition.y = Math.min(Math.max(18.0, predictedPosition.y), battleFieldHeight - 18.0);
                }
            }
            double theta = normalAbsoluteAngle(absoluteBearing(_bot.getPosition(), predictedPosition));
            _bot.setTurnGunRightRadians(normalRelativeAngle(theta - _bot.getGunHeadingRadians()));
        }
        //TODO: gun heat?
        _bot.setFire(power);
    }

    private double bulletPower() {
        double energy = _bot.getEnergy();
        if (_enemyEnergy <= 1.0) {
            return Math.min(0.3, energy);
        } else if (_enemyEnergy <= 2.0 && _enemyEnergy > 1.0) {
            return Math.min(0.6, energy);
        } else if (_enemyEnergy <= 3.0 && _enemyEnergy > 2.0) {
            return Math.min(0.8, energy);
        } else if (_enemyEnergy <= 4.0 && _enemyEnergy > 3.0) {
            return Math.min(1.0, energy);
        } else {
            return Math.min(2.0, energy);
        }
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
            if (distance < minimumDistance) {
                minimumDistance = distance;
                indexOfMinimumDistance = i;
            }
            i++;
        } while (i < _record.size() - 50);
        return indexOfMinimumDistance;
    }

    private Point2D.Double predictPosition(double power) {
        int i = lastIndexOfMatchingSeries();
        Point2D.Double predictedPosition = (Point2D.Double)_bot.getEnemyPosition().clone();
        double heading = _enemyHeading;
        double travelTime = 0.0;
        double turns = 0.0;
        for (int j = i + 1; j < _record.size() && turns <= travelTime; j++) {
            MovementState ms = _record.get(j);
            predictedPosition.x += Math.sin(heading) * ms.getVelocity();
            predictedPosition.y += Math.cos(heading) * ms.getVelocity();
            heading += ms.getTurnRate();
            travelTime = bulletTravelTime(_bot.getPosition().distance(predictedPosition), power);
            turns += 1.0;
        }
        return predictedPosition;
    }
}