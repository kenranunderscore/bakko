package kenran.offense;

import kenran.Bakko;
import robocode.ScannedRobotEvent;

import java.awt.*;

public class WeaponManager {
    private final WarAxe _warAxe;
    private final CircularPredictor _circularPredictor;
    private Weapon _activeWeapon;

    public WeaponManager(Bakko bakko) {
        _warAxe = new WarAxe(bakko);
        _circularPredictor = new CircularPredictor(bakko);
        _activeWeapon = _warAxe;
        _warAxe.setActive(true);
    }

    public void onScannedRobot(ScannedRobotEvent e) {
        _activeWeapon.onScannedRobot(e);
    }

    public void onPaint(Graphics2D g) {
        _activeWeapon.onPaint(g);
    }
}