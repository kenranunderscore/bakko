package kenran.data;

import robocode.Bullet;

public class ShotInfo {
    private final double _angle;
    private final Bullet _bullet;

    public ShotInfo(double angle, Bullet bullet) {
        _angle = angle;
        _bullet = bullet;
    }

    public double getAngle() {
        return _angle;
    }

    public boolean isVirtual() {
        return _bullet == null;
    }
}