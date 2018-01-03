package kenran.util;

import robocode.Rules;

public class MovementState {
    private final double _turnRate;
    private final double _velocity;
    private final double _normalizedTurnRate;
    private final double _normalizedVelocity;

    public MovementState(double turnRate, double velocity) {
        this._turnRate = turnRate;
        this._velocity = velocity;
        _normalizedTurnRate = turnRate / Rules.MAX_TURN_RATE;
        _normalizedVelocity = velocity / Rules.MAX_VELOCITY;
    }

    public double compare(MovementState other) {
        double turnRateDifference = this._normalizedTurnRate - other._normalizedTurnRate;
        double velocityDifference = this._normalizedVelocity - other._normalizedVelocity;
        return Math.abs(turnRateDifference) + Math.abs(velocityDifference);
    }

    public double getTurnRate() {
        return _turnRate;
    }

    public double getVelocity() {
        return _velocity;
    }
}