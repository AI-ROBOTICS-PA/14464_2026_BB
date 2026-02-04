package org.firstinspires.ftc.teamcode.decode14464.systems;

import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

/**
 * Turret Calibration OpMode
 * Use this to calibrate your turret's encoder ticks per degree
 *
 * INSTRUCTIONS:
 * 1. Run this program
 * 2. Note the starting encoder value (should be 0)
 * 3. Manually rotate turret EXACTLY 90 degrees (use a protractor)
 * 4. Note the ending encoder value
 * 5. Calculate: (ending - starting) / 90 = ticks per degree
 * 6. Update AutoTurretSystem.java MAX/MIN_ENCODER_POSITION constants
 */
@TeleOp(name = "Turret Calibration", group = "Calibration")
public class TurretCalibration extends OpMode {
    private RobotMotors motors;
    private int initialPosition = 0;
    private boolean captured = false;

    @Override
    public void init() {
        motors = new RobotMotors();
        motors.init(hardwareMap);

        // Capture initial position
        initialPosition = motors.turret.getCurrentPosition();

        telemetry.addLine("✅ Turret Calibration Ready");
        telemetry.addData("Initial Position", initialPosition);
        telemetry.update();
    }

    @Override
    public void loop() {
        int currentPosition = motors.turret.getCurrentPosition();
        int delta = currentPosition - initialPosition;

        // Manual control with gamepad
        double power = -gamepad1.right_stick_y * 0.3; // Slow speed for precise positioning
        motors.turret.setPower(power);

        // Capture position with A/Cross button (supports both Xbox and PS4)
        boolean capturePressed = gamepad1.a || gamepad1.cross;
        if (capturePressed && !captured) {
            captured = true;
            telemetry.addLine("📌 POSITION CAPTURED!");
            telemetry.addData("Delta", delta + " ticks");
            if (delta != 0) {
                double ticksPerDegree = delta / 90.0;
                telemetry.addData("Ticks per Degree", String.format("%.2f", ticksPerDegree));
                telemetry.addLine();
                telemetry.addLine("UPDATE THESE VALUES IN AutoTurretSystem.java:");
                telemetry.addData("MAX_ENCODER_POSITION", (int)(ticksPerDegree * 150));
                telemetry.addData("MIN_ENCODER_POSITION", (int)(-ticksPerDegree * 150));
            }
        }

        // Reset capture with B/Circle button (supports both Xbox and PS4)
        boolean resetPressed = gamepad1.b || gamepad1.circle;
        if (resetPressed) {
            captured = false;
        }

        // Display
        telemetry.addLine("====== TURRET CALIBRATION ======");
        telemetry.addLine();
        telemetry.addData("Current Position", currentPosition);
        telemetry.addData("Initial Position", initialPosition);
        telemetry.addData("Delta (Change)", delta + " ticks");
        telemetry.addLine();

        if (delta != 0) {
            // Calculate based on 90 degree rotation (as per instructions)
            double ticksPerDegree = delta / 90.0;
            telemetry.addData("For 90° Rotation", delta + " ticks");
            telemetry.addData("Ticks per Degree", String.format("%.2f", ticksPerDegree));
            telemetry.addData("Calculate MAX_LIMIT", (int)(ticksPerDegree * 150));
            telemetry.addData("Calculate MIN_LIMIT", (int)(-ticksPerDegree * 150));
        }

        telemetry.addLine();
        telemetry.addLine("====== INSTRUCTIONS ======");
        telemetry.addLine("1. Use RIGHT STICK to position turret");
        telemetry.addLine("2. Rotate turret EXACTLY 90 degrees");
        telemetry.addLine("   (use protractor or visual marker)");
        telemetry.addLine("3. Press A/Cross button to capture");
        telemetry.addLine("4. Note 'Ticks per Degree' value");
        telemetry.addLine("5. Update AutoTurretSystem constants");
        telemetry.addLine();
        telemetry.addData("Right Stick Y", "Manual Control");
        telemetry.addData("A/Cross Button", "Capture Position");
        telemetry.addData("B/Circle Button", "Reset Capture");

        telemetry.update();
    }

    @Override
    public void stop() {
        motors.turret.setPower(0);
    }
}

