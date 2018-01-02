package kenran;

import kenran.movement.MovementDeque;
import kenran.radar.LockingRadar;
import kenran.util.MovementState;
import kenran.util.Wave;
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
    private static final double WALL_STICK = 160.0;
    private static final int BINS = 47;
    private static final double RADAR_LOCK_MULTIPLIER = 2.1;
    private static final ArrayList<MovementState> _record = new ArrayList<>(3000);
    private static final double[] _surfStats = new double[BINS];
    private static Rectangle2D.Double _fieldRect = null;

    private final Point2D.Double _enemyPosition = new Point2D.Double();
    private final Point2D.Double _position = new Point2D.Double();
    private final MovementDeque _recent = new MovementDeque(RECENT_PATTERN_LENGTH);
    private final ArrayList<Wave> _enemyWaves = new ArrayList<>();
    private final ArrayList<Integer> _surfDirections = new ArrayList<>();
    private final ArrayList<Double> _surfAbsBearings = new ArrayList<>();
    private double _enemyHeading = 0.0;
    private double _enemyEnergy = 100.0;
    private LockingRadar _radar;

    @SuppressWarnings("InfiniteLoopStatement")
    @Override
    public void run() {
        setAdjustGunForRobotTurn(true);
        setAdjustRadarForGunTurn(true);
        _radar = new LockingRadar(this, RADAR_LOCK_MULTIPLIER);
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
        double d1 = getVelocity() * Math.sin(e.getBearingRadians());
        _surfDirections.add(0, d1 >= 0.0 ? 1 : -1);
        _surfAbsBearings.add(0, enemyBearing + Math.PI);
        double deltaEnergy = _enemyEnergy - e.getEnergy();
        _enemyEnergy = e.getEnergy();
        if (_enemyEnergy < 0.1) {
            stop();
            setTurnGunRightRadians(enemyBearing);
            setFire(0.1);
        }
        if (deltaEnergy < 3.01 && deltaEnergy > 0.09 && _surfDirections.size() > 2) {
            Wave w = new Wave();
            w.fireTime = getTime() - 1;
            w.bulletVelocity = bulletVelocity(deltaEnergy);
            w.traveledDistance = bulletVelocity(deltaEnergy);
            w.direction = _surfDirections.get(2);
            w.angle = _surfAbsBearings.get(2);
            w.firePosition = (Point2D.Double)_enemyPosition.clone();
            _enemyWaves.add(w);
        }
        updateWaves();
        surf();
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

    private void updateWaves() {
        for (int i = 0; i < _enemyWaves.size(); i++) {
            Wave w = _enemyWaves.get(i);
            w.traveledDistance = ((getTime() - w.fireTime) * w.bulletVelocity);
            if (w.traveledDistance > _position.distance(w.firePosition) + 50.0D) {
                _enemyWaves.remove(i);
                i--;
            }
        }
    }

    private Wave getClosestSurfableWave() {
        double minimumDistance = Double.POSITIVE_INFINITY;
        Wave surfWave = null;
        for (Wave wave : _enemyWaves) {
            double distance = _position.distance(wave.firePosition) - wave.traveledDistance;
            if (distance > wave.bulletVelocity && distance < minimumDistance) {
                surfWave = wave;
                minimumDistance = distance;
            }
        }
        return surfWave;
    }

    private static int getFactorIndex(Wave wave, Point2D.Double position) {
        double d1 = absoluteBearing(wave.firePosition, position) - wave.angle;
        double d2 = normalRelativeAngle(d1) / maxEscapeAngle(wave.bulletVelocity) * wave.direction;
        return (int)limit(0.0, d2 * 23.0 + 23.0, 46.0);
    }

    private void logHit(Wave wave, Point2D.Double position) {
        int i = getFactorIndex(wave, position);
        for (int j = 0; j < BINS; j++) {
            _surfStats[j] += 1.0 / (Math.pow(i - j, 2.0) + 1.0);
        }
    }

    @Override
    public void onHitByBullet(HitByBulletEvent e) {
        if (_enemyWaves.isEmpty()) {
            return;
        }

        Bullet bullet = e.getBullet();
        Point2D.Double hitPosition = new Point2D.Double(bullet.getX(), bullet.getY());
        Wave hitWave = null;
        for (Wave wave : _enemyWaves) {
            if ((Math.abs(wave.traveledDistance - _position.distance(wave.firePosition)) < 50.0D) && (Math.round(bulletVelocity(e.getBullet().getPower()) * 10.0D) == Math.round(wave.bulletVelocity * 10.0D))) {
                hitWave = wave;
                break;
            }
        }
        if (hitWave != null) {
            logHit(hitWave, hitPosition);
            _enemyWaves.remove(_enemyWaves.lastIndexOf(hitWave));
        }
    }

    @Override
    public void onBulletHitBullet(BulletHitBulletEvent e) {
        if (_enemyWaves.isEmpty()) {
            return;
        }

        Point2D.Double hitPosition = new Point2D.Double(e.getBullet().getX(), e.getBullet().getY());
        Wave hitWave = null;
        for (Wave wave : _enemyWaves) {
            if ((Math.abs(wave.traveledDistance - wave.firePosition.distance(hitPosition)) < 50.0D) && (Math.round(bulletVelocity(e.getHitBullet().getPower()) * 10.0D) == Math.round(wave.bulletVelocity * 10.0D))) {
                hitWave = wave;
                break;
            }
        }
        if (hitWave != null) {
            logHit(hitWave, hitPosition);
            _enemyWaves.remove(_enemyWaves.lastIndexOf(hitWave));
        }
    }

    private Point2D.Double predictPosition(Wave wave, int direction) {
        Point2D.Double impactPosition = (Point2D.Double)_position.clone();
        double velocity = getVelocity();
        double heading = getHeadingRadians();
        int i = 0;
        int j = 0;
        do {
            double d4 = wallSmoothing(_fieldRect, impactPosition, absoluteBearing(wave.firePosition, impactPosition) + direction * 1.5707963267948966D, direction, WALL_STICK) - heading;
            double d5 = 1.0D;
            if (Math.cos(d4) < 0.0D) {
                d4 += Math.PI;
                d5 = -1.0D;
            }
            d4 = normalRelativeAngle(d4);
            double d3 = 0.004363323129985824D * (40.0D - 3.0D * Math.abs(velocity));
            heading = normalRelativeAngle(heading + limit(-d3, d4, d3));
            velocity += (velocity * d5 < 0.0D ? 2.0D * d5 : d5);
            velocity = limit(-8.0D, velocity, 8.0D);
            impactPosition = project(impactPosition, heading, velocity);
            i++;
            if (impactPosition.distance(wave.firePosition) < wave.traveledDistance + i * wave.bulletVelocity + wave.bulletVelocity) {
                j = 1;
            }
        } while (j == 0 && (i < 500));
        return impactPosition;
    }

    private double checkDanger(Wave wave, int direction) {
        int i = getFactorIndex(wave, predictPosition(wave, direction));
        return _surfStats[i];
    }

    private void surf() {
        Wave surfWave = getClosestSurfableWave();
        if (surfWave == null) return;
        double dangerLeft = checkDanger(surfWave, -1);
        double dangerRight = checkDanger(surfWave, 1);
        double angle = absoluteBearing(surfWave.firePosition, _position);
        if (dangerLeft < dangerRight) {
            angle = wallSmoothing(_fieldRect, _position, angle - 1.5707963267948966D, -1, WALL_STICK);
        } else {
            angle = wallSmoothing(_fieldRect, _position, angle + 1.5707963267948966D, 1, WALL_STICK);
        }
        setBackAsFront(this, angle);
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