package kenran.data;

import robocode.Rules;

import java.awt.geom.Point2D;

public class Wave {
    private final Point2D.Double _origin;
    private final double _power;
    private final double _speed;
    private double _traveledDistance = 0.0;

    public Wave(Point2D.Double origin, double power) {
        if (power < Rules.MIN_BULLET_POWER || power > Rules.MAX_BULLET_POWER) {
            throw new IllegalArgumentException(power + " is no valid bullet power value.");
        }
        _origin = origin;
        _power = power;
        _speed = Rules.getBulletSpeed(power);
    }

    public void advance() {
        advance(1);
    }

    public void advance(int ticks) {
        _traveledDistance += ticks * _speed;
    }

    public Point2D.Double getOrigin() {
        return _origin;
    }

    public double getPower() {
        return _power;
    }

    public double getSpeed() {
        return _speed;
    }

    public double getTraveledDistance() {
        return _traveledDistance;
    }
}