package kenran.util;

import org.junit.Test;

import static org.junit.Assert.*;

public class FixedSizeQueueTest {
    @Test(expected = IllegalArgumentException.class)
    public void throwsWhenMaxSizeIsNegative() {
        FixedSizeQueue<Integer> queue = new FixedSizeQueue<>(-1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void throwsWhenMaxSizeIsZero() {
        FixedSizeQueue<Integer> queue = new FixedSizeQueue<>(0);
    }

    @Test
    public void firstPushedItemIsFirstPeeked() {
        FixedSizeQueue<Object> queue = new FixedSizeQueue<>(5);
        Object foo = new Object();
        queue.push(foo);
        assertEquals(foo, queue.peek());
    }

    @Test
    public void queueIsFullWhenEnoughItemsArePushed() {
        FixedSizeQueue<Integer> queue = new FixedSizeQueue<>(3);
        queue.push(1);
        queue.push(2);
        queue.push(3);
        assertTrue(queue.isFull());
    }

    @Test
    public void firstPushedItemIsGoneWhenTooManyItemsArePushed() {
        FixedSizeQueue<Integer> queue = new FixedSizeQueue<>(2);
        queue.push(1);
        queue.push(2);
        queue.push(3);
        assertEquals(queue.peek(), (Integer)2);
    }

    @Test
    public void maxSizeReturnsCorrectValue() {
        FixedSizeQueue<Integer> queue = new FixedSizeQueue<>(1789);
        assertEquals(1789, queue.maxSize());
    }
}