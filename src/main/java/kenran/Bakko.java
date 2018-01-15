package kenran;

import kenran.defense.ShieldDash;
import kenran.offense.WeaponManager;
import kenran.sight.HeroicGaze;
import robocode.*;

import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

public class Bakko extends AdvancedRobot {
    private static final double RADAR_LOCK_MULTIPLIER = 2.0;
    private static Rectangle2D.Double _fieldRect = null;

    private final Point2D.Double _enemyPosition = new Point2D.Double();
    private final Point2D.Double _position = new Point2D.Double();

    private HeroicGaze _heroicGaze;
    private ShieldDash _shieldDash;
    private WeaponManager _weaponManager;
    private boolean _hasWon = false;

    @SuppressWarnings("InfiniteLoopStatement")
    @Override
    public void run() {
        setScanColor(new Color(0, 200, 0));
        setBodyColor(new Color(150, 50, 0));
        setRadarColor(new Color(30, 90, 30));
        setGunColor(Color.DARK_GRAY);
        setAdjustGunForRobotTurn(true);
        setAdjustRadarForGunTurn(true);
        _heroicGaze = new HeroicGaze(this, RADAR_LOCK_MULTIPLIER);
        _shieldDash = new ShieldDash(this);
        _weaponManager = new WeaponManager(this);
        if (_fieldRect == null) {
            _fieldRect = new Rectangle2D.Double(18.0, 18.0, getBattleFieldWidth() - 36.0, getBattleFieldHeight() - 36.0);
        }
        while (true) {
            _heroicGaze.lookForEscapedEnemy();
            execute();
        }
    }

    @Override
    public void onScannedRobot(ScannedRobotEvent e) {
        _position.setLocation(getX(), getY());
        _heroicGaze.onScannedRobot(e);
        _weaponManager.onScannedRobot(e);
        _shieldDash.onScannedRobot(e);
    }

    @Override
    public void onHitByBullet(HitByBulletEvent e) {
        _shieldDash.onHitByBullet(e);
    }

    @Override
    public void onBulletHitBullet(BulletHitBulletEvent e) {
        _shieldDash.onBulletHitBullet(e);
    }

    @Override
    public void onBulletHit(BulletHitEvent e) {
        _shieldDash.onBulletHit(e);
    }

    @Override
    public void onWin(WinEvent event) {
        _hasWon = true;
    }

    @Override
    public void onPaint(Graphics2D g) {
        _weaponManager.onPaint(g);
        _shieldDash.onPaint(g);
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