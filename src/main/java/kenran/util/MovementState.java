package kenran.util;

public class MovementState {
    public final double turnRate;
    public final double velocity;

    public MovementState(double turnRate, double velocity) {
        this.turnRate = turnRate;
        this.velocity = velocity;
    }
}