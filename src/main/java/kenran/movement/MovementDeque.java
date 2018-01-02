package kenran.movement;

import kenran.util.MovementState;

public class MovementDeque {
    private final int _maxSize;
    private final MovementState[] _data;

    public MovementDeque(int maxSize) {
        _maxSize = maxSize;
        _data = new MovementState[_maxSize];
    }

    public void add(double turnRate, double velocity) {
        System.arraycopy(_data, 1, _data, 0, _maxSize - 1);
        _data[_maxSize - 1] = new MovementState(turnRate, velocity);
    }

    public void add(MovementState ms) {
        add(ms.turnRate, ms.velocity);
    }

    public MovementState get(int index) {
        if (index < 0 || index >= _maxSize) {
            throw new IndexOutOfBoundsException();
        }
        return _data[index];
    }

    public int getMaxSize() {
        return _maxSize;
    }
}