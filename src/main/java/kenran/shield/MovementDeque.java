package kenran.shield;

import kenran.util.MovementState;

public class MovementDeque {
    private final int _maxSize;
    private final MovementState[] _data;

    public MovementDeque(int maxSize) {
        _maxSize = maxSize;
        _data = new MovementState[_maxSize];
    }

    public void add(MovementState ms) {
        System.arraycopy(_data, 1, _data, 0, _maxSize - 1);
        _data[_maxSize - 1] = ms;
    }

    public double compare(MovementDeque other) {
        double distance = 0.0;
        for (int i = 0; i < getMaxSize(); i++) {
            distance += _data[i].compare(other._data[i]);
        }
        return distance;
    }

    public int getMaxSize() {
        return _maxSize;
    }
}