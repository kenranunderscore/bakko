package kenran;

import kenran.util.MovementState;
import kenran.util.Utils;
import kenran.util.Wave;
import robocode.AdvancedRobot;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;

import static kenran.util.Utils.*;
import static robocode.util.Utils.normalAbsoluteAngle;
import static robocode.util.Utils.normalRelativeAngle;

public class Bakko extends AdvancedRobot {
    private static final int PM_LENGTH = 3000;
    private static final int RECENT_PATTERN_LENGTH = 12;
    private static final double WALL_STICK = 160.0D;
    private static Rectangle2D.Double _fieldRect;
    private static Point2D.Double _enemyPosition = new Point2D.Double();
    private static Point2D.Double _robotPosition = new Point2D.Double();

    private double _enemyHeading = 0.0;
    private static double _enemyBearing;
    private static double _enemyVelocity;
    private static double _enemyDistance;
    private static double _deltaHeading;
    private double _enemyEnergy = 100.0;
    private static ArrayList<MovementState> _record = new ArrayList<>(3000);
    private static MovementDeque _recent = new MovementDeque();
    private static MovementDeque _iterator = new MovementDeque();
    private static int _recordSize = 0;
    private static boolean _recordIsFull = false;

    private static boolean _aimed = false;

    private static final int BINS = 47;
    private static double[] _surfStats = new double[BINS];
    private static ArrayList<Wave> _enemyWaves = new ArrayList<>();
    private static ArrayList<Integer> _surfDirections = new ArrayList<>();
    private static ArrayList<Double> _surfAbsBearings = new ArrayList<>();

    @SuppressWarnings("InfiniteLoopStatement")
    @Override
    public void run() {
        setAdjustGunForRobotTurn(true);
        setAdjustRadarForGunTurn(true);
        _aimed = false;
        _fieldRect = new Rectangle2D.Double(18.0, 18.0, getBattleFieldWidth() - 36.0, getBattleFieldHeight() - 36.0);
        if (getRoundNum() == 0) {
            for (int i = 0; i < RECENT_PATTERN_LENGTH; i++) {
                _recent.add(0.0D, 8.0D);
            }
        }
        turnRadarRightRadians(Double.POSITIVE_INFINITY);
        while (true) {
            execute();
        }
    }

    @Override
    public void onScannedRobot(robocode.ScannedRobotEvent paramScannedRobotEvent)
    {
        _robotPosition.setLocation(getX(), getY());
        _enemyDistance = paramScannedRobotEvent.getDistance();
        _deltaHeading = paramScannedRobotEvent.getHeadingRadians() - _enemyHeading;
        _enemyHeading = paramScannedRobotEvent.getHeadingRadians();
        _enemyBearing = getHeadingRadians() + paramScannedRobotEvent.getBearingRadians();
        _enemyVelocity = paramScannedRobotEvent.getVelocity();
        _enemyPosition.setLocation(project(_robotPosition, _enemyBearing, _enemyDistance));

        double d1 = getVelocity() * Math.sin(paramScannedRobotEvent.getBearingRadians());
        _surfDirections.add(0, d1 >= 0.0D ? 1 : -1);
        _surfAbsBearings.add(0, _enemyBearing + 3.141592653589793D);
        double d2 = _enemyEnergy - paramScannedRobotEvent.getEnergy();
        _enemyEnergy = paramScannedRobotEvent.getEnergy();
        if (_enemyEnergy < 0.1D)
        {
            stop();
            setTurnGunRightRadians(_enemyBearing);
            setFire(0.1D);
        }
        if ((d2 < 3.01D) && (d2 > 0.09D) && (_surfDirections.size() > 2))
        {
            Wave w = new Wave();
            w.fireTime = (getTime() - 1L);
            w.bulletVelocity = bulletVelocity(d2);
            w.traveledDistance = bulletVelocity(d2);
            w.direction = _surfDirections.get(2);
            w.angle = _surfAbsBearings.get(2);
            w.firePosition = (Point2D.Double)_enemyPosition.clone();
            _enemyWaves.add(w);
        }
        updateWaves();
        doSurfing();

        record(_deltaHeading, _enemyVelocity);
        _recent.add(_deltaHeading, _enemyVelocity);
        double d3 = bulletPower();
        if (getRoundNum() != 0)
        {
            java.awt.geom.Point2D.Double localDouble = new java.awt.geom.Point2D.Double();
            localDouble.setLocation(predictPosition(d3));
            double d5 = absoluteBearing(_robotPosition, localDouble);
            setTurnGunRightRadians(normalRelativeAngle(d5 - getGunHeadingRadians()));
            _aimed = true;
        }
        else
        {
            double d4 = 0.0D;
            double d6 = _enemyPosition.getX();
            double d7 = _enemyPosition.getY();
            double d8 = _enemyHeading;
            double d9 = getBattleFieldWidth();
            double d10 = getBattleFieldHeight();
            while (++d4 * bulletVelocity(d3) < _robotPosition.distance(d6, d7))
            {
                d6 += Math.sin(d8) * _enemyVelocity;
                d7 += Math.cos(d8) * _enemyVelocity;
                d8 += _deltaHeading;
                if ((d6 < 17.9D) || (d7 < 17.9D) || (d6 > d9 - 17.9D) || (d7 > d10 - 17.9D))
                {
                    d6 = Math.min(Math.max(18.0D, d6), d9 - 18.0D);
                    d7 = Math.min(Math.max(18.0D, d7), d10 - 18.0D);
                }
            }

            double d11 = normalAbsoluteAngle(Utils.absoluteBearing(_robotPosition, new Point2D.Double(d6, d7)));
            setTurnGunRightRadians(normalRelativeAngle(d11 - getGunHeadingRadians()));
            _aimed = true;
        }
        if (_aimed)
        {
            setFire(bulletPower());
            _aimed = false;
        }
        double d4 = _enemyBearing - getRadarHeadingRadians();
        setTurnRadarRightRadians(2.1D * normalRelativeAngle(d4));
    }


    public void updateWaves()
    {
        for (int i = 0; i < _enemyWaves.size(); i++)
        {
            Wave w = _enemyWaves.get(i);
            w.traveledDistance = ((getTime() - w.fireTime) * w.bulletVelocity);
            if (w.traveledDistance > _robotPosition.distance(w.firePosition) + 50.0D)
            {
                _enemyWaves.remove(i);
                i--;
            }
        }
    }

    public Wave getClosestSurfableWave()
    {
        double d1 = 50000.0D;
        Wave localObject = null;
        for (int i = 0; i < _enemyWaves.size(); i++)
        {
            Wave localEnemyWave = _enemyWaves.get(i);
            double d2 = _robotPosition.distance(localEnemyWave.firePosition) - localEnemyWave.traveledDistance;
            if ((d2 > localEnemyWave.bulletVelocity) && (d2 < d1))
            {
                localObject = localEnemyWave;
                d1 = d2;
            }
        }
        return localObject;
    }

    public static int getFactorIndex(Wave paramEnemyWave, Point2D.Double paramPoint2D)
    {
        double d1 = absoluteBearing(paramEnemyWave.firePosition, paramPoint2D) - paramEnemyWave.angle;
        double d2 = normalRelativeAngle(d1) / maxEscapeAngle(paramEnemyWave.bulletVelocity) * paramEnemyWave.direction;
        return (int)Utils.limit(0.0D, d2 * 23.0D + 23.0D, 46.0D);
    }

    public void logHit(Wave paramEnemyWave, Point2D.Double paramDouble)
    {
        int i = getFactorIndex(paramEnemyWave, paramDouble);
        for (int j = 0; j < 47; j++) {
            _surfStats[j] += 1.0D / (Math.pow(i - j, 2.0D) + 1.0D);
        }
    }

    @Override
    public void onHitByBullet(robocode.HitByBulletEvent paramHitByBulletEvent)
    {
        if (!_enemyWaves.isEmpty())
        {
            java.awt.geom.Point2D.Double localDouble = new java.awt.geom.Point2D.Double(paramHitByBulletEvent.getBullet().getX(), paramHitByBulletEvent.getBullet().getY());
            Wave localObject = null;

            for (int i = 0; i < _enemyWaves.size(); i++)
            {
                Wave localEnemyWave = _enemyWaves.get(i);
                if ((Math.abs(localEnemyWave.traveledDistance - _robotPosition.distance(localEnemyWave.firePosition)) < 50.0D) && (Math.round(Utils.bulletVelocity(paramHitByBulletEvent.getBullet().getPower()) * 10.0D) == Math.round(localEnemyWave.bulletVelocity * 10.0D)))
                {
                    localObject = localEnemyWave;
                    break;
                }
            }
            if (localObject != null)
            {
                logHit(localObject, localDouble);

                _enemyWaves.remove(_enemyWaves.lastIndexOf(localObject));
            }
        }
    }

    @Override
    public void onBulletHitBullet(robocode.BulletHitBulletEvent paramBulletHitBulletEvent)
    {
        if (!_enemyWaves.isEmpty())
        {
            java.awt.geom.Point2D.Double localDouble = new java.awt.geom.Point2D.Double(paramBulletHitBulletEvent.getBullet().getX(), paramBulletHitBulletEvent.getBullet().getY());
            Wave localObject = null;
            for (int i = 0; i < _enemyWaves.size(); i++)
            {
                Wave localEnemyWave = _enemyWaves.get(i);
                if ((Math.abs(localEnemyWave.traveledDistance - localEnemyWave.firePosition.distance(localDouble)) < 50.0D) && (Math.round(Utils.bulletVelocity(paramBulletHitBulletEvent.getHitBullet().getPower()) * 10.0D) == Math.round(localEnemyWave.bulletVelocity * 10.0D)))
                {

                    localObject = localEnemyWave;
                    break;
                }
            }
            if (localObject != null)
            {
                logHit(localObject, localDouble);
                _enemyWaves.remove(_enemyWaves.lastIndexOf(localObject));
            }
        }
    }

    public Point2D.Double predictPosition(Wave paramEnemyWave, int paramInt)
    {
        Point2D.Double localPoint2D = (Point2D.Double)_robotPosition.clone();
        double d1 = getVelocity();
        double d2 = getHeadingRadians();

        int i = 0;
        int j = 0;
        do {
            double d4 = wallSmoothing(_fieldRect, localPoint2D, absoluteBearing(paramEnemyWave.firePosition, localPoint2D) + paramInt * 1.5707963267948966D, paramInt, WALL_STICK) - d2;
            double d5 = 1.0D;
            if (Math.cos(d4) < 0.0D)
            {
                d4 += 3.141592653589793D;
                d5 = -1.0D;
            }
            d4 = normalRelativeAngle(d4);

            double d3 = 0.004363323129985824D * (40.0D - 3.0D * Math.abs(d1));
            d2 = normalRelativeAngle(d2 + Utils.limit(-d3, d4, d3));



            d1 += (d1 * d5 < 0.0D ? 2.0D * d5 : d5);
            d1 = Utils.limit(-8.0D, d1, 8.0D);

            localPoint2D = Utils.project(localPoint2D, d2, d1);
            i++;
            if (localPoint2D.distance(paramEnemyWave.firePosition) < paramEnemyWave.traveledDistance + i * paramEnemyWave.bulletVelocity + paramEnemyWave.bulletVelocity)
            {
                j = 1;
            }
        } while (j == 0 && (i < 500));
        return localPoint2D;
    }

    public double checkDanger(Wave paramEnemyWave, int paramInt)
    {
        int i = getFactorIndex(paramEnemyWave, predictPosition(paramEnemyWave, paramInt));
        return _surfStats[i];
    }

    public void doSurfing()
    {
        Wave localEnemyWave = getClosestSurfableWave();
        if (localEnemyWave == null) return;
        double d1 = checkDanger(localEnemyWave, -1);
        double d2 = checkDanger(localEnemyWave, 1);
        double d3 = Utils.absoluteBearing(localEnemyWave.firePosition, _robotPosition);
        if (d1 < d2)
        {
            d3 = Utils.wallSmoothing(_fieldRect, _robotPosition, d3 - 1.5707963267948966D, -1, WALL_STICK);
        }
        else
        {
            d3 = Utils.wallSmoothing(_fieldRect, _robotPosition, d3 + 1.5707963267948966D, 1, WALL_STICK);
        }
        Utils.setBackAsFront(this, d3);
    }

    private double bulletPower()
    {
        double d;
        if (_enemyEnergy <= 1.0D) {
            d = Math.min(0.3D, getEnergy());
        } else if ((_enemyEnergy <= 2.0D) && (_enemyEnergy > 1.0D)) {
            d = Math.min(0.6D, getEnergy());
        } else if ((_enemyEnergy <= 3.0D) && (_enemyEnergy > 2.0D)) {
            d = Math.min(0.8D, getEnergy());
        } else if ((_enemyEnergy <= 4.0D) && (_enemyEnergy > 3.0D)) {
            d = Math.min(1.0D, getEnergy());
        } else
            d = Math.min(2.0D, getEnergy());
        return d;
    }

    private void record(double paramDouble1, double paramDouble2)
    {
        _record.add(new MovementState(paramDouble1, paramDouble2));
        if (_recordIsFull) {
            _record.remove(0);
        }
        else {
            _recordSize += 1;
            _recordIsFull = _recordSize >= 3000;
        }
    }

    private double compare(MovementDeque paramMovementDeque1, MovementDeque paramMovementDeque2)
    {
        double d = 0.0D;
        for (int i = 0; i < 12; i++)
        {
            d += Math.abs(paramMovementDeque1.get(i).turnRate - paramMovementDeque2.get(i).turnRate) + Math.abs(paramMovementDeque1.get(i).velocity - paramMovementDeque2.get(i).velocity);
        }
        return d;
    }

    private int lastIndexOfMatchingSeries()
    {
        for (int i = 0; i < 12; i++)
            _iterator.add((MovementState)_record.get(i));
        double d1 = compare(_iterator, _recent);
        int j = 11;
        int k = 12;
        do
        {
            MovementState localMovementState = (MovementState)_record.get(k);
            _iterator.add(localMovementState.turnRate, localMovementState.velocity);
            double d2 = compare(_iterator, _recent);
            if (d2 < d1)
            {
                d1 = d2;
                j = k;
            }
            k++;
        } while (k < _recordSize - 50);
        return j;
    }

    private Point2D.Double predictPosition(double paramDouble)
    {
        int i = lastIndexOfMatchingSeries();
        double d1 = _enemyPosition.getX();
        double d2 = _enemyPosition.getY();
        double d3 = _enemyHeading;
        double d4 = 0.0D;
        double d5 = 0.0D;
        for (int j = i + 1; (j < _recordSize) && (d5 <= d4); j++)
        {
            MovementState localMovementState = _record.get(j);
            d1 += Math.sin(d3) * localMovementState.velocity;
            d2 += Math.cos(d3) * localMovementState.velocity;
            d3 += localMovementState.turnRate;
            d4 = (int)Utils.bulletTravelTime(_robotPosition.distance(d1, d2), paramDouble);
            d5 += 1.0D;
        }
        return new java.awt.geom.Point2D.Double(d1, d2);
    }

    static class MovementDeque
    {
        private MovementState[] msArray = new MovementState[12];

        void add(double paramDouble1, double paramDouble2)
        {
            for (int i = 0; i < 11; i++)
                msArray[i] = msArray[(i + 1)];
            msArray[11] = new MovementState(paramDouble1, paramDouble2);
        }

        void add(MovementState paramMovementState)
        {
            add(paramMovementState.turnRate, paramMovementState.velocity);
        }

        MovementState get(int paramInt)
        {
            if ((0 <= paramInt) && (paramInt < 12)) {
                return msArray[paramInt];
            }
            return null;
        }
    }
}