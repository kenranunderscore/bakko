package kenran.data;

import org.junit.Test;
import robocode.Bullet;

import static org.junit.Assert.*;

public class ShotInfoTest {
    @Test
    public void isVirtualWhenBulletIsNull() {
        ShotInfo info = new ShotInfo(5.0, null);
        assertTrue(info.isVirtual());
    }

    @Test
    public void isNotVirtualWhenBulletExists() {
        ShotInfo info = new ShotInfo(5.0, new Bullet(
                1.0,
                1.0,
                1.0,
                1.0,
                "foo",
                "bar",
                true,
                1));
        assertFalse(info.isVirtual());
    }

    @Test
    public void constructorValueForAngleIsReturned() {
        ShotInfo info = new ShotInfo(5.0, null);
        assertEquals(5.0, info.getAngle(), .0001);
    }
}