package kenran.util;

import org.junit.Test;

import static org.junit.Assert.*;

public class UtilsTest {
    @Test
    public void limitChoosesMinWhenValueIsLessThanMin() {
        double value = 0.5;
        double limit = Utils.limit(1.0, value, 2.0);
        assertEquals(1.0, limit, .0001);
    }

    @Test
    public void limitChoosesMaxWhenValueIsGreaterThanMax() {
        double value = 3.7;
        double limit = Utils.limit(1.0, value, 2.5);
        assertEquals(2.5, limit, .0001);
    }

    @Test
    public void limitChoosesValueWhenInRange() {
        double value = 3.7;
        double limit = Utils.limit(1.0, value, 5.0);
        assertEquals(value, limit, .0001);
    }
}