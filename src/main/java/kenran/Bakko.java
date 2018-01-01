package kenran;

import robocode.AdvancedRobot;

public class Bakko extends AdvancedRobot {
    @SuppressWarnings("InfiniteLoopStatement")
    @Override
    public void run() {
        while (true) {
            execute();
        }
    }
}