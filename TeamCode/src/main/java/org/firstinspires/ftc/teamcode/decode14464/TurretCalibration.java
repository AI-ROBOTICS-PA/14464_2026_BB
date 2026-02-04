package org.firstinspires.ftc.teamcode.decode14464;

import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import org.firstinspires.ftc.teamcode.decode14464.systems.RobotMotors;

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

        // Capture position with A button
        if (gamepad1.a && !captured) {
            captured = true;
            telemetry.addLine("📌 POSITION CAPTURED!");
            telemetry.addData("Delta", delta + " ticks");
            if (delta != 0) {
                double ticksPerDegree = delta / 90.0;
                telemetry.addData("Ticks per Degree", String.format("%.2f", ticksPerDegree));
                telemetry.addLine();
                telemetry.addLine("UPDATE THESE VALUES:");
                telemetry.addData("MAX_ENCODER_POSITION", (int)(ticksPerDegree * 150));
                telemetry.addData("MIN_ENCODER_POSITION", (int)(-ticksPerDegree * 150));
            }
        }

        if (gamepad1.b) {
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
            double degrees = delta / 5.0; // Assuming 5 ticks/degree
            telemetry.addData("Estimated Rotation", String.format("%.1f°", degrees));
            telemetry.addData("Ticks per Degree", String.format("%.2f", delta / degrees));
        }

        telemetry.addLine();
        telemetry.addLine("====== INSTRUCTIONS ======");
        telemetry.addLine("1. Use RIGHT STICK to position turret");
        telemetry.addLine("2. Rotate turret EXACTLY 90 degrees");
        telemetry.addLine("   (use protractor or visual marker)");
        telemetry.addLine("3. Press A button to capture position");
        telemetry.addLine("4. Note 'Ticks per Degree' value");
        telemetry.addLine("5. Update AutoTurretSystem constants");
        telemetry.addLine();
        telemetry.addData("Right Stick Y", "Manual Control");
        telemetry.addData("A Button", "Capture Position");
        telemetry.addData("B Button", "Reset Capture");

        telemetry.update();
    }

    @Override
    public void stop() {
        motors.turret.setPower(0);
    }
}

