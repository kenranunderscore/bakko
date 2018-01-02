package kenran;

import kenran.movement.MovementDeque;
import kenran.movement.WaveSurfingMovement;
import kenran.radar.LockingRadar;
import kenran.util.MovementState;
import robocode.*;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import static kenran.util.Utils.*;
import static robocode.util.Utils.normalAbsoluteAngle;
import static robocode.util.Utils.normalRelativeAngle;

public class Bakko extends AdvancedRobot {
    private static final int PM_LENGTH = 3000;
    private static final int RECENT_PATTERN_LENGTH = 12;
    private static final double RADAR_LOCK_MULTIPLIER = 2.1;
    private static final ArrayList<MovementState> _record = new ArrayList<>(3000);
    private static Rectangle2D.Double _fieldRect = null;

    private final Point2D.Double _enemyPosition = new Point2D.Double();
    private final Point2D.Double _position = new Point2D.Double();
    private final MovementDeque _recent = new MovementDeque(RECENT_PATTERN_LENGTH);
    private double _enemyHeading = 0.0;
    private double _enemyEnergy = 100.0;

    private LockingRadar _radar;
    private WaveSurfingMovement _movement;

    @SuppressWarnings("InfiniteLoopStatement")
    @Override
    public void run() {
        setAdjustGunForRobotTurn(true);
        setAdjustRadarForGunTurn(true);
        _radar = new LockingRadar(this, RADAR_LOCK_MULTIPLIER);
        _movement = new WaveSurfingMovement(this);
        if (_fieldRect == null) {
            _fieldRect = new Rectangle2D.Double(18.0, 18.0, getBattleFieldWidth() - 36.0, getBattleFieldHeight() - 36.0);
        }
        for (int i = 0; i < _recent.getMaxSize(); i++) {
            _recent.add(0.0, 8.0);
        }
        while (true) {
            execute();
        }
    }

    @Override
    public void onScannedRobot(ScannedRobotEvent e) {
        _position.setLocation(getX(), getY());
        double _deltaHeading = e.getHeadingRadians() - _enemyHeading;
        _enemyHeading = e.getHeadingRadians();
        double enemyBearing = getHeadingRadians() + e.getBearingRadians();
        _enemyPosition.setLocation(project(_position, enemyBearing, e.getDistance()));
        _enemyEnergy = e.getEnergy();
        if (_enemyEnergy < 0.1) {
            stop();
            setTurnGunRightRadians(enemyBearing);
            setFire(0.1);
        }
        _movement.onScannedRobot(e);
        record(_deltaHeading, e.getVelocity());
        double power = bulletPower();
        if (getRoundNum() != 0) {
            Point2D.Double predictedPosition = predictPosition(power);
            double d5 = absoluteBearing(_position, predictedPosition);
            setTurnGunRightRadians(normalRelativeAngle(d5 - getGunHeadingRadians()));
        } else {
            double bulletVelocity = bulletVelocity(power);
            double turns = 0.0D;
            double enemyX = _enemyPosition.getX();
            double enemyY = _enemyPosition.getY();
            double heading = _enemyHeading;
            double battleFieldWidth = getBattleFieldWidth();
            double battleFieldHeight = getBattleFieldHeight();
            while (++turns * bulletVelocity < _position.distance(enemyX, enemyY)) {
                enemyX += Math.sin(heading) * e.getVelocity();
                enemyY += Math.cos(heading) * e.getVelocity();
                heading += _deltaHeading;
                if (enemyX < 17.9 || enemyY < 17.9 || enemyX > battleFieldWidth - 17.9 || enemyY > battleFieldHeight - 17.9) {
                    enemyX = Math.min(Math.max(18.0, enemyX), battleFieldWidth - 18.0);
                    enemyY = Math.min(Math.max(18.0, enemyY), battleFieldHeight - 18.0);
                }
            }

            double theta = normalAbsoluteAngle(absoluteBearing(_position, new Point2D.Double(enemyX, enemyY)));
            setTurnGunRightRadians(normalRelativeAngle(theta - getGunHeadingRadians()));
        }
        setFire(power);
        _radar.onScannedRobot(e);
    }

    @Override
    public void onHitByBullet(HitByBulletEvent e) {
        _movement.onHitByBullet(e);
    }

    @Override
    public void onBulletHitBullet(BulletHitBulletEvent e) {
        _movement.onBulletHitBullet(e);
    }

    public Point2D.Double getPosition() {
        return _position;
    }

    public Point2D.Double getEnemyPosition() {
        return _enemyPosition;
    }

    public Rectangle2D.Double getBattleField() {
        return _fieldRect;
    }

    private double bulletPower() {
        if (_enemyEnergy <= 1.0) {
            return Math.min(0.3, getEnergy());
        } else if (_enemyEnergy <= 2.0 && _enemyEnergy > 1.0) {
            return Math.min(0.6, getEnergy());
        } else if (_enemyEnergy <= 3.0 && _enemyEnergy > 2.0) {
            return Math.min(0.8, getEnergy());
        } else if (_enemyEnergy <= 4.0 && _enemyEnergy > 3.0) {
            return Math.min(1.0, getEnergy());
        } else {
            return Math.min(2.0, getEnergy());
        }
    }

    private void record(double turnRate, double velocity) {
        MovementState ms = new MovementState(turnRate, velocity);
        _recent.add(ms);
        _record.add(ms);
        if (_record.size() >= PM_LENGTH) {
            _record.remove(0);
        }
    }

    private double compare(MovementDeque d1, MovementDeque d2) {
        double distance = 0.0;
        for (int i = 0; i < d1.getMaxSize(); i++) {
            MovementState ms1 = d1.get(i);
            MovementState ms2 = d2.get(i);
            double turnRateDifference = ms1.turnRate - ms2.turnRate;
            double velocityDifference = ms1.velocity - ms2.velocity;
            distance += Math.abs(turnRateDifference) + Math.abs(velocityDifference);
        }
        return distance;
    }

    private int lastIndexOfMatchingSeries() {
        MovementDeque iterator = new MovementDeque(RECENT_PATTERN_LENGTH);
        for (int i = 0; i < 12; i++) {
            iterator.add(_record.get(i));
        }
        double minimumDistance = compare(iterator, _recent);
        int indexOfMinimumDistance = _recent.getMaxSize() - 1;
        int i = _recent.getMaxSize();
        do {
            MovementState ms = _record.get(i);
            iterator.add(ms.turnRate, ms.velocity);
            double distance = compare(iterator, _recent);
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
        Point2D.Double predictedPosition = (Point2D.Double)_enemyPosition.clone();
        double heading = _enemyHeading;
        double travelTime = 0.0;
        double turns = 0.0;
        for (int j = i + 1; j < _record.size() && turns <= travelTime; j++) {
            MovementState ms = _record.get(j);
            predictedPosition.x += Math.sin(heading) * ms.velocity;
            predictedPosition.y += Math.cos(heading) * ms.velocity;
            heading += ms.turnRate;
            travelTime = (int)bulletTravelTime(_position.distance(predictedPosition), power);
            turns += 1.0;
        }
        return predictedPosition;
    }
}