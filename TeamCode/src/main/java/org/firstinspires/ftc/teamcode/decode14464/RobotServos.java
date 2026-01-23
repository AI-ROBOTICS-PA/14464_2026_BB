package org.firstinspires.ftc.teamcode.decode14464;

import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.hardware.Servo;

public class RobotServos {
    // Pushing the ball to the flywheel shooter
    public Servo ballpush_0 = null;
    // Adjusting the angle itake arm with the wheels
    public Servo intake_1 = null;
    //adjust the flywheel shooter height
    public Servo shooterHeight_2 = null;
    // servo attached to the catapult launcher with rubberband
    public Servo catLauncher_3 = null;
    // servo to oush the green ball into the catapult
    public Servo scanner_5 = null;
    // servo to hold the catapult in place
    public Servo catholding_4 = null;

    public final double catHoldPosition = 0.05;
    public final double catReleasePosition = 1;

    public final double catLaunchPosition = 0.5;
    public final double catLockPosition = .42;

    public final double stopballtotheFlywheelposition = 0;
    public final double clearballtotheFlywheelposition = .25;

    public final double feedballtotheFlywheelposition = .37;

    public final double resetballtotheFlywheelposition = 0;

    public final double longShootHeightPosition = .45;

    public final double shortShootHeightPosition = 0.67;

    // Add more servos as needed
    public Servo ballstop_0c = null;
    public void init(HardwareMap hardwareMap) {
        ballpush_0 = hardwareMap.get(Servo.class, "0_ballpush");
        intake_1 = hardwareMap.get(Servo.class, "1_intake");
        shooterHeight_2 = hardwareMap.get(Servo.class, "2_shootheight");
        catLauncher_3 = hardwareMap.get(Servo.class, "3_catlaunch");
        scanner_5 = hardwareMap.get(Servo.class, "5_scan");
        catholding_4 = hardwareMap.get(Servo.class, "4_cathold");
        ballstop_0c = hardwareMap.get(Servo.class, "ballstop_0c");
        // Set initial position for shooterHeight_2 servo
        if (shooterHeight_2 != null) {
            shooterHeight_2.setPosition(.45);
        }
        if (intake_1 != null) {
            intake_1.setPosition(.57);
          //  intake_1.setPosition(.5);
        }
        if (catLauncher_3 != null) {
            catLauncher_3.setPosition(.52);
        }

        if (catholding_4 != null) {
            catholding_4.setPosition(.025);
        }

        if (ballpush_0 != null) {
            ballpush_0.setPosition(.2);
        }

        if (ballstop_0c != null) {
            ballstop_0c.setPosition(0);
        }

        if (scanner_5 != null) {
            scanner_5.setPosition(.42);
        }

        // Initialize additional servos here
    }

    // Catapult Servos catLauncher_3 and catholding_4
    public void holdCatapult() {
        catholding_4.setPosition(catHoldPosition);
    }
    public void releaseCatapult() {
        catholding_4.setPosition(catReleasePosition);
    }

    public void launchCatapult() {
        catLauncher_3.setPosition(catLaunchPosition);
    }

    public void lockCatapult() {
        catLauncher_3.setPosition(catLockPosition);
    }

    //Intake servos scanner_5 and intake_1
    public void scanballload() {
        scanner_5.setPosition(.57);
    }
    public void scanballresetscan() {
        scanner_5.setPosition(.23);
    }

    //Flywheel shooter servos shooterHeight_2,ballpush_0 and, ballstop_0c
    public void stopballtotheFlywheel() {
        ballstop_0c.setPosition(stopballtotheFlywheelposition);
    }

    public void clearballtotheFlywheel() {
        ballstop_0c.setPosition(clearballtotheFlywheelposition);
    }

    public void feedballtotheFlywheel() {
        ballpush_0.setPosition(feedballtotheFlywheelposition);
    }

    public void resetballtotheFlywheel() {
        ballpush_0.setPosition(resetballtotheFlywheelposition);
    }

    public void longShootHeight() {
        shooterHeight_2.setPosition(longShootHeightPosition);
    }

    public void shortShootheight() {
        shooterHeight_2.setPosition(shortShootHeightPosition);
    }

}

