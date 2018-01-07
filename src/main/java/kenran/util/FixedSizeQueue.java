package kenran.util;

import java.util.LinkedList;

public class FixedSizeQueue<E> {
    private final LinkedList<E> _items = new LinkedList<>();
    private final int _maxSize;

    public FixedSizeQueue(int maxSize) {
        if (maxSize <= 0) {
            throw new IllegalArgumentException();
        }
        _maxSize = maxSize;
    }

    public void push(E item) {
        _items.addLast(item);
        if (_items.size() > _maxSize) {
            _items.removeFirst();
        }
    }

    public E peek() {
        return _items.peekFirst();
    }

    public boolean isFull() {
        return _items.size() == _maxSize;
    }

    public int maxSize() {
        return _maxSize;
    }
}