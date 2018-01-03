package kenran;

import kenran.gun.PatternMatcher;
import kenran.movement.WaveSurfingMovement;
import kenran.radar.LockingRadar;
import robocode.*;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import static kenran.util.Utils.*;

public class Bakko extends AdvancedRobot {
    private static final double RADAR_LOCK_MULTIPLIER = 2.1;
    private static Rectangle2D.Double _fieldRect = null;

    private final Point2D.Double _enemyPosition = new Point2D.Double();
    private final Point2D.Double _position = new Point2D.Double();

    private LockingRadar _radar;
    private WaveSurfingMovement _movement;
    private PatternMatcher _gun;

    @SuppressWarnings("InfiniteLoopStatement")
    @Override
    public void run() {
        setAdjustGunForRobotTurn(true);
        setAdjustRadarForGunTurn(true);
        _radar = new LockingRadar(this, RADAR_LOCK_MULTIPLIER);
        _movement = new WaveSurfingMovement(this);
        _gun = new PatternMatcher(this);
        if (_fieldRect == null) {
            _fieldRect = new Rectangle2D.Double(18.0, 18.0, getBattleFieldWidth() - 36.0, getBattleFieldHeight() - 36.0);
        }
        while (true) {
            execute();
        }
    }

    @Override
    public void onScannedRobot(ScannedRobotEvent e) {
        _position.setLocation(getX(), getY());
        double enemyBearing = getHeadingRadians() + e.getBearingRadians();
        _enemyPosition.setLocation(project(_position, enemyBearing, e.getDistance()));
        _gun.onScannedRobot(e);
        _movement.onScannedRobot(e);
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

    @Override
    public void onBulletHit(BulletHitEvent e) {
        _movement.onBulletHit(e);
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
}