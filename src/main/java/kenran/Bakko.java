package kenran;

import kenran.gun.PatternMatcher;
import kenran.movement.WaveSurfingMovement;
import kenran.radar.LockingRadar;
import robocode.*;

import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

public class Bakko extends AdvancedRobot {
    private static final double RADAR_LOCK_MULTIPLIER = 2.0;
    private static Rectangle2D.Double _fieldRect = null;

    private final Point2D.Double _enemyPosition = new Point2D.Double();
    private final Point2D.Double _position = new Point2D.Double();

    private LockingRadar _radar;
    private WaveSurfingMovement _movement;
    private PatternMatcher _gun;
    private boolean _hasWon = false;

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
            _radar.checkForSlip();
            execute();
        }
    }

    @Override
    public void onScannedRobot(ScannedRobotEvent e) {
        _position.setLocation(getX(), getY());
        _radar.onScannedRobot(e);
        _gun.onScannedRobot(e);
        _movement.onScannedRobot(e);
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

    @Override
    public void onWin(WinEvent event) {
        _hasWon = true;
    }

    @Override
    public void onPaint(Graphics2D g) {
        _movement.onPaint(g);
    }

    public Point2D.Double getPosition() {
        return _position;
    }

    public Point2D.Double getEnemyPosition() {
        return _enemyPosition;
    }

    public void setEnemyPosition(Point2D.Double position) {
        _enemyPosition.setLocation(position);
    }

    public Rectangle2D.Double getBattleField() {
        return _fieldRect;
    }

    public boolean hasWon() {
        return _hasWon;
    }
}