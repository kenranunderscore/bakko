package kenran.movement;

import kenran.Bakko;
import kenran.util.Wave;
import robocode.Bullet;
import robocode.BulletHitBulletEvent;
import robocode.HitByBulletEvent;
import robocode.ScannedRobotEvent;

import java.awt.geom.Point2D;
import java.util.ArrayList;

import static kenran.util.Utils.*;
import static kenran.util.Utils.setBackAsFront;
import static kenran.util.Utils.wallSmoothing;
import static robocode.util.Utils.normalRelativeAngle;

public class WaveSurfingMovement {
    private static final double WALL_STICK = 160.0;
    private static final int BINS = 47;
    private static final double HALF_PI = Math.PI / 2.0;
    private static final double[] _surfStats = new double[BINS];

    private final ArrayList<Wave> _enemyWaves = new ArrayList<>();
    private final ArrayList<Integer> _surfDirections = new ArrayList<>();
    private final ArrayList<Double> _surfAbsBearings = new ArrayList<>();
    private final Bakko _bot;

    private double _enemyEnergy = 100.0;

    public WaveSurfingMovement(Bakko bot) {
        _bot = bot;
    }

    public void onScannedRobot(ScannedRobotEvent e) {
        double enemyBearing = _bot.getHeadingRadians() + e.getBearingRadians();
        double lateralVelocity = _bot.getVelocity() * Math.sin(e.getBearingRadians());
        _surfDirections.add(0, lateralVelocity >= 0.0 ? 1 : -1);
        _surfAbsBearings.add(0, enemyBearing + Math.PI);
        double deltaEnergy = _enemyEnergy - e.getEnergy();
        _enemyEnergy = e.getEnergy();
        if (deltaEnergy < 3.01 && deltaEnergy > 0.09 && _surfDirections.size() > 2) {
            Wave w = new Wave();
            w.fireTime = _bot.getTime() - 1;
            w.bulletVelocity = bulletVelocity(deltaEnergy);
            w.traveledDistance = bulletVelocity(deltaEnergy);
            w.direction = _surfDirections.get(2);
            w.angle = _surfAbsBearings.get(2);
            w.firePosition = (Point2D.Double)_bot.getEnemyPosition().clone();
            _enemyWaves.add(w);
        }
        updateWaves();
        surf();
    }

    public void onHitByBullet(HitByBulletEvent e) {
        if (_enemyWaves.isEmpty()) {
            return;
        }

        Bullet bullet = e.getBullet();
        Point2D.Double hitPosition = new Point2D.Double(bullet.getX(), bullet.getY());
        Wave hitWave = null;
        for (Wave wave : _enemyWaves) {
            if ((Math.abs(wave.traveledDistance - _bot.getPosition().distance(wave.firePosition)) < 50.0D) && (Math.round(bulletVelocity(e.getBullet().getPower()) * 10.0D) == Math.round(wave.bulletVelocity * 10.0D))) {
                hitWave = wave;
                break;
            }
        }
        if (hitWave != null) {
            logHit(hitWave, hitPosition);
            _enemyWaves.remove(_enemyWaves.lastIndexOf(hitWave));
        }
    }

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
        return (int)limit(0.0, d2 * 23.0 + 23.0, 46.0);
    }

    private void logHit(Wave wave, Point2D.Double position) {
        int i = getFactorIndex(wave, position);
        for (int j = 0; j < BINS; j++) {
            _surfStats[j] += 1.0 / (Math.pow(i - j, 2.0) + 1.0);
        }
    }

    private Point2D.Double predictPosition(Wave wave, int direction) {
        Point2D.Double impactPosition = (Point2D.Double)_bot.getPosition().clone();
        double velocity = _bot.getVelocity();
        double heading = _bot.getHeadingRadians();
        int turnCount = 0;
        boolean intercepted = false;
        do {
            double unsmoothedAngle = absoluteBearing(wave.firePosition, impactPosition) + direction * HALF_PI;
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
            double maxTurnRate = Math.PI / 720.0 * (40.0 - 3.0 * Math.abs(velocity));
            heading = normalRelativeAngle(heading + limit(-maxTurnRate, goAngle, maxTurnRate));
            velocity += (velocity * goDirection < 0.0 ? 2.0 * goDirection : goDirection);
            velocity = limit(-8.0, velocity, 8.0);
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
        return _surfStats[i];
    }

    private void surf() {
        Wave surfWave = getClosestSurfableWave();
        if (surfWave == null) return;
        double dangerLeft = checkDanger(surfWave, -1);
        double dangerRight = checkDanger(surfWave, 1);
        double angle = absoluteBearing(surfWave.firePosition, _bot.getPosition());
        if (dangerLeft < dangerRight) {
            angle = wallSmoothing(_bot.getBattleField(), _bot.getPosition(), angle - HALF_PI, -1, WALL_STICK);
        } else {
            angle = wallSmoothing(_bot.getBattleField(), _bot.getPosition(), angle + HALF_PI, 1, WALL_STICK);
        }
        setBackAsFront(_bot, angle);
    }
}