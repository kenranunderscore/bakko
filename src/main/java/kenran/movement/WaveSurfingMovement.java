package kenran.movement;

import kenran.Bakko;
import kenran.gfx.GfxUtils;
import kenran.util.FixedSizeQueue;
import kenran.util.Wave;
import robocode.*;

import java.awt.*;
import java.awt.geom.Point2D;
import java.util.ArrayList;

import static kenran.util.Utils.*;
import static kenran.util.Utils.setBackAsFront;
import static kenran.util.Utils.wallSmoothing;
import static robocode.util.Utils.normalRelativeAngle;

public class WaveSurfingMovement {
    private static final double WALL_STICK = 160.0;
    private static final int BINS = 47;
    private static final int MIDDLE_BIN = (BINS - 1) / 2;
    private static final int DISTANCE_SEGMENT_COUNT = 3;
    private static final int ACCELERATION_SEGMENT_COUNT = 3;
    private static final double DISTANCE_KEEPING_ANGLE = Math.PI / 2.0 - 0.2;
    private static final double[][][] _surfStats = new double[DISTANCE_SEGMENT_COUNT][ACCELERATION_SEGMENT_COUNT][BINS];

    private final ArrayList<Wave> _enemyWaves = new ArrayList<>();
    private final FixedSizeQueue<Integer> _surfDirections = new FixedSizeQueue<>(3);
    private final FixedSizeQueue<Double> _surfAbsBearings = new FixedSizeQueue<>(3);
    private final Point2D.Double _enemyPosition = new Point2D.Double();
    private final Bakko _bot;

    private double _enemyEnergy = 100.0;
    private double _lastVelocity = 0.0;
    private Wave _surfWave = null;

    public WaveSurfingMovement(Bakko bot) {
        _bot = bot;
    }

    public void onScannedRobot(ScannedRobotEvent e) {
        double enemyBearing = _bot.getHeadingRadians() + e.getBearingRadians();
        double lateralVelocity = _bot.getVelocity() * Math.sin(e.getBearingRadians());
        _surfDirections.push(lateralVelocity >= 0.0 ? 1 : -1);
        _surfAbsBearings.push(enemyBearing + Math.PI);
        double deltaEnergy = _enemyEnergy - e.getEnergy();
        _enemyEnergy = e.getEnergy();
        if (deltaEnergy <= Rules.MAX_BULLET_POWER && deltaEnergy >= Rules.MIN_BULLET_POWER && _surfDirections.isFull()) {
            double bulletVelocity = Rules.getBulletSpeed(deltaEnergy);
            Wave w = new Wave();
            w.fireTime = _bot.getTime() - 1;
            w.bulletVelocity = bulletVelocity;
            w.traveledDistance = bulletVelocity;
            w.direction = _surfDirections.peek();
            w.angle = _surfAbsBearings.peek();
            w.firePosition = (Point2D.Double)_enemyPosition.clone();
            w.distanceSegment = distanceSegment(e.getDistance());
            w.accelerationSegment = accelerationSegment();
            _enemyWaves.add(w);
        }
        _enemyPosition.setLocation(_bot.getEnemyPosition());
        _lastVelocity = _bot.getVelocity();
        updateWaves();
        surf();
    }

    public void onHitByBullet(HitByBulletEvent e) {
        Bullet bullet = e.getBullet();
        _enemyEnergy += 3.0 * bullet.getPower();
        if (_enemyWaves.isEmpty()) {
            return;
        }

        Point2D.Double hitPosition = new Point2D.Double(bullet.getX(), bullet.getY());
        Wave hitWave = null;
        for (Wave wave : _enemyWaves) {
            if ((Math.abs(wave.traveledDistance - _bot.getPosition().distance(wave.firePosition)) < 50.0D) && (Math.round(Rules.getBulletSpeed(e.getBullet().getPower()) * 10.0D) == Math.round(wave.bulletVelocity * 10.0D))) {
                hitWave = wave;
                break;
            }
        }
        if (hitWave != null) {
            logHit(hitWave, hitPosition);
            _enemyWaves.remove(hitWave);
        }
    }

    public void onBulletHitBullet(BulletHitBulletEvent e) {
        if (_enemyWaves.isEmpty()) {
            return;
        }

        Point2D.Double hitPosition = new Point2D.Double(e.getBullet().getX(), e.getBullet().getY());
        Wave hitWave = null;
        for (Wave wave : _enemyWaves) {
            if ((Math.abs(wave.traveledDistance - wave.firePosition.distance(hitPosition)) < 50.0D) && (Math.round(Rules.getBulletSpeed(e.getHitBullet().getPower()) * 10.0D) == Math.round(wave.bulletVelocity * 10.0D))) {
                hitWave = wave;
                break;
            }
        }
        if (hitWave != null) {
            logHit(hitWave, hitPosition);
            _enemyWaves.remove(hitWave);
        }
    }

    public void onBulletHit(BulletHitEvent e) {
        _enemyEnergy -= Rules.getBulletDamage(e.getBullet().getPower());
    }

    private int distanceSegment(double distance) {
        if (distance <= 300) {
            return 0;
        } else if (distance <= 600) {
            return 1;
        } else {
            return 2;
        }
    }

    private int accelerationSegment() {
        double velocityDifference = _bot.getVelocity() - _lastVelocity;
        if (velocityDifference == 0.0) {
            return 1;
        }
        return velocityDifference < 0 ? 0 : 2;
    }

    private void updateWaves() {
        for (int i = 0; i < _enemyWaves.size(); i++) {
            Wave w = _enemyWaves.get(i);
            w.traveledDistance = (_bot.getTime() - w.fireTime) * w.bulletVelocity;
            if (w.traveledDistance > _bot.getPosition().distance(w.firePosition) + 50.0D) {
                _enemyWaves.remove(i);
                i--;
            }
        }
    }

    private Wave getClosestSurfableWave() {
        double minimumDistance = Double.POSITIVE_INFINITY;
        Wave surfWave = null;
        for (Wave wave : _enemyWaves) {
            double distance = _bot.getPosition().distance(wave.firePosition) - wave.traveledDistance;
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
        return (int)limit(0, d2 * MIDDLE_BIN + MIDDLE_BIN, BINS - 1);
    }

    private void logHit(Wave wave, Point2D.Double position) {
        int i = getFactorIndex(wave, position);
        for (int j = 0; j < BINS; j++) {
            _surfStats[wave.distanceSegment][wave.accelerationSegment][j] += 1.0 / (Math.pow(i - j, 2.0) + 1.0);
        }
    }

    private Point2D.Double predictPosition(Wave wave, int direction) {
        Point2D.Double impactPosition = (Point2D.Double)_bot.getPosition().clone();
        double velocity = _bot.getVelocity();
        double heading = _bot.getHeadingRadians();
        int turnCount = 0;
        boolean intercepted = false;
        do {
            double unsmoothedAngle = absoluteBearing(wave.firePosition, impactPosition) + direction * DISTANCE_KEEPING_ANGLE;
            double goAngle = wallSmoothing(
                    _bot.getBattleField(),
                    impactPosition,
                    unsmoothedAngle,
                    direction,
                    WALL_STICK) - heading;
            double goDirection = 1.0;
            if (Math.cos(goAngle) < 0.0) {
                goAngle += Math.PI;
                goDirection = -1.0;
            }
            goAngle = normalRelativeAngle(goAngle);
            double maxTurnRate = Rules.getTurnRateRadians(velocity);
            heading = normalRelativeAngle(heading + limit(-maxTurnRate, goAngle, maxTurnRate));
            velocity += (velocity * goDirection < 0.0 ? 2.0 * goDirection : goDirection);
            velocity = limit(-Rules.MAX_VELOCITY, velocity, Rules.MAX_VELOCITY);
            impactPosition = project(impactPosition, heading, velocity);
            turnCount++;
            if (impactPosition.distance(wave.firePosition) < wave.traveledDistance + turnCount * wave.bulletVelocity + wave.bulletVelocity) {
                intercepted = true;
            }
        } while (!intercepted && turnCount < 500);
        return impactPosition;
    }

    private double checkDanger(Wave wave, int direction) {
        int i = getFactorIndex(wave, predictPosition(wave, direction));
        return _surfStats[wave.distanceSegment][wave.accelerationSegment][i];
    }

    private void surf() {
        _surfWave = getClosestSurfableWave();
        if (_surfWave == null) {
            return;
        }
        double dangerLeft = checkDanger(_surfWave, -1);
        double dangerRight = checkDanger(_surfWave, 1);
        double angle = absoluteBearing(_surfWave.firePosition, _bot.getPosition());
        if (dangerLeft < dangerRight) {
            angle = wallSmoothing(_bot.getBattleField(), _bot.getPosition(), angle - DISTANCE_KEEPING_ANGLE, -1, WALL_STICK);
        } else {
            angle = wallSmoothing(_bot.getBattleField(), _bot.getPosition(), angle + DISTANCE_KEEPING_ANGLE, 1, WALL_STICK);
        }
        setBackAsFront(_bot, angle);
    }

    public void onPaint(Graphics2D g) {
        for (Wave w : _enemyWaves) {
            if (w == _surfWave) {
                g.setColor(Color.ORANGE);
            } else {
                g.setColor(Color.GRAY);
            }
            GfxUtils.drawCircle(g, w.firePosition, w.traveledDistance);
        }
    }
}