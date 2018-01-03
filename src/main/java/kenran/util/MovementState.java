package kenran.util;

public class MovementState {
    private final double _turnRate;
    private final double _velocity;

    public MovementState(double turnRate, double velocity) {
        this._turnRate = turnRate;
        this._velocity = velocity;
    }

    public double compare(MovementState other) {
        double turnRateDifference = this._turnRate - other._turnRate;
        double velocityDifference = this._velocity - other._velocity;
        return Math.abs(turnRateDifference) + Math.abs(velocityDifference);
    }

    public double getTurnRate() {
        return _turnRate;
    }

    public double getVelocity() {
        return _velocity;
    }
}