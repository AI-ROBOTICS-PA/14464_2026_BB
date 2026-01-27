package org.firstinspires.ftc.teamcode.decode14464;

import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.hardware.Servo;

public class RobotServos {
    // Pushing the ball to the flywheel shooter
    public Servo ballpush_4 = null;
    public Servo turret_1 = null;
    public Servo hood_5 = null;

    public Servo colorservo = null;
    public final double ballpush_initial_position = 0.50;
    public final double ballpush_final_position = 0.47;

    public void init(HardwareMap hardwareMap) {
        ballpush_4 = hardwareMap.get(Servo.class, "ballpush_2");
        hood_5 = hardwareMap.get(Servo.class, "hood_5");
        turret_1 = hardwareMap.get(Servo.class, "turret_1");
        colorservo = hardwareMap.get(Servo.class, "colorservo");
        // Initialize additional servos here
        if (ballpush_4 != null) {
            ballpush_4.setPosition(ballpush_initial_position);
        }
    }
}
