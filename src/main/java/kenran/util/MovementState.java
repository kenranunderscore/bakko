package kenran.util;

public class MovementState {
    public final double turnRate;
    public final double velocity;

    public MovementState(double turnRate, double velocity) {
        this.turnRate = turnRate;
        this.velocity = velocity;
    }

    public double compare(MovementState other) {
        double turnRateDifference = this.turnRate - other.turnRate;
        double velocityDifference = this.velocity - other.velocity;
        return Math.abs(turnRateDifference) + Math.abs(velocityDifference);
    }
}