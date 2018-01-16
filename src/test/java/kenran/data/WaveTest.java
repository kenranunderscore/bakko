package kenran.data;

import org.junit.Test;
import robocode.Rules;

import java.awt.geom.Point2D;

import static org.junit.Assert.*;

public class WaveTest {
    @Test(expected = IllegalArgumentException.class)
    public void throwsWhenBulletPowerIsTooLow() {
        Wave wave = new Wave(new Point2D.Double(), 0.05);
    }

    @Test(expected = IllegalArgumentException.class)
    public void throwsWhenBulletPowerIsTooHigh() {
        Wave wave = new Wave(new Point2D.Double(), 3.05);
    }

    @Test
    public void speedIsCalculatedCorrectly() {
        double power = 1.5;
        Wave wave = new Wave(new Point2D.Double(), power);
        assertEquals(Rules.getBulletSpeed(power), wave.getSpeed(), .0001);
    }

    @Test
    public void traveledDistanceIsInitiallyZero() {
        Wave wave = new Wave(new Point2D.Double(), 1.5);
        assertEquals(wave.getTraveledDistance(), 0.0, .0001);
    }

    @Test
    public void advanceIncreasesTraveledDistanceByCorrectAmount() {
        Wave wave = new Wave(new Point2D.Double(), 0.5);
        wave.advance(3);
        double expectedDistance = 3.0 * Rules.getBulletSpeed(0.5);
        assertEquals(expectedDistance, wave.getTraveledDistance(), .0001);
    }

    @Test
    public void singleTickAdvanceIncreasesTraveledDistanceBySpeed() {
        Wave wave = new Wave(new Point2D.Double(), 0.5);
        wave.advance();
        double expectedDistance = Rules.getBulletSpeed(0.5);
        assertEquals(expectedDistance, wave.getTraveledDistance(), .0001);
    }

    @Test
    public void getPowerReturnsInitiallySetValue() {
        Wave wave = new Wave(new Point2D.Double(), 2.95);
        assertEquals(2.95, wave.getPower(), .0001);
    }

    @Test
    public void getOriginReturnsInitiallySetPoint() {
        Point2D.Double origin = new Point2D.Double(100.0, 150.0);
        Wave wave = new Wave(origin, 1.0);
        assertEquals(origin, wave.getOrigin());
    }
}