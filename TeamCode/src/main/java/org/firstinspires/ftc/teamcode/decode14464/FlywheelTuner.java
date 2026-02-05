package org.firstinspires.ftc.teamcode.decode14464;

import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.PIDFCoefficients;

// @TeleOp(name = "Flywheel Tuner", group = "Tuning")  // DISABLED
public class FlywheelTuner extends OpMode {
    // Motor
    private DcMotorEx flywheelMotor;

    // Target velocities (ticks per second)
    public double highVelocity = 1450;
    public double lowVelocity = 1100;
    public double curTargetVelocity = highVelocity;

    // PIDF values (we're only tuning P and F, I and D stay at 0)
    private double p = 300.0;
    private double f = 13.65;

    // Step sizes for fine and coarse tuning
    private final double[] stepSizes = {10.0, 1.0, 0.1, 0.01, 0.001};
    private int stepIndex = 1; // Start at 1.0 step size

    // Button state tracking for edge detection
    private boolean prevY = false;
    private boolean prevB = false;
    private boolean prevDpadUp = false;
    private boolean prevDpadDown = false;
    private boolean prevDpadLeft = false;
    private boolean prevDpadRight = false;

    @Override
    public void init() {
        // Initialize the flywheel motor (using your "shooter" motor)
        flywheelMotor = hardwareMap.get(DcMotorEx.class, "shooter");
        flywheelMotor.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        flywheelMotor.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        flywheelMotor.setDirection(DcMotor.Direction.FORWARD); // Change to REVERSE if needed
        flywheelMotor.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);

        // Set initial PIDF coefficients (P, I, D, F)
        PIDFCoefficients pidfCoefficients = new PIDFCoefficients(p, 0, 0, f);
        flywheelMotor.setPIDFCoefficients(DcMotor.RunMode.RUN_USING_ENCODER, pidfCoefficients);

        telemetry.addLine("=================================");
        telemetry.addLine("  FLYWHEEL TUNER INITIALIZED");
        telemetry.addLine("=================================");
        telemetry.addLine("Controls:");
        telemetry.addLine("  Y - Toggle velocity (900/1500)");
        telemetry.addLine("  B - Change step size");
        telemetry.addLine("  D-Pad Up/Down - Adjust P");
        telemetry.addLine("  D-Pad Right/Left - Adjust F");
        telemetry.addLine("=================================");
        telemetry.update();
    }

    @Override
    public void loop() {
        // Toggle between high and low velocity with Y button
        if (gamepad1.y && !prevY) {
            if (curTargetVelocity == highVelocity) {
                curTargetVelocity = lowVelocity;
            } else {
                curTargetVelocity = highVelocity;
            }
        }
        prevY = gamepad1.y;



        // Cycle through step sizes with B button
        if (gamepad1.b && !prevB) {
            stepIndex = (stepIndex + 1) % stepSizes.length;
        }
        prevB = gamepad1.b;

        // Adjust P value with D-pad Up/Down
        if (gamepad1.dpad_up && !prevDpadUp) {
            p += stepSizes[stepIndex];
        }
        if (gamepad1.dpad_down && !prevDpadDown) {
            p = Math.max(0, p - stepSizes[stepIndex]); // Don't go below 0
        }
        prevDpadUp = gamepad1.dpad_up;
        prevDpadDown = gamepad1.dpad_down;

        // Adjust F value with D-pad Right/Left
        if (gamepad1.dpad_right && !prevDpadRight) {
            f += stepSizes[stepIndex];
        }
        if (gamepad1.dpad_left && !prevDpadLeft) {
            f = Math.max(0, f - stepSizes[stepIndex]); // Don't go below 0
        }
        prevDpadRight = gamepad1.dpad_right;
        prevDpadLeft = gamepad1.dpad_left;

        // Apply new PIDF coefficients every loop
        PIDFCoefficients pidfCoefficients = new PIDFCoefficients(p, 0, 0, f);
        flywheelMotor.setPIDFCoefficients(DcMotor.RunMode.RUN_USING_ENCODER, pidfCoefficients);

        // Set the target velocity
        flywheelMotor.setVelocity(curTargetVelocity);

        // Get current velocity and calculate error
        double currentVelocity = flywheelMotor.getVelocity();
        double error = curTargetVelocity - currentVelocity;

        // Update telemetry with all tuning data
        telemetry.addLine("=================================");
        telemetry.addLine("  FLYWHEEL TUNER");
        telemetry.addLine("=================================");
        telemetry.addData("Target Velocity", "%.1f ticks/sec", curTargetVelocity);
        telemetry.addData("Current Velocity", "%.1f ticks/sec", currentVelocity);
        telemetry.addData("Error", "%.1f ticks/sec", error);
        telemetry.addLine("---------------------------------");
        telemetry.addData("P Value", "%.4f", p);
        telemetry.addData("F Value", "%.4f", f);
        telemetry.addData("Step Size", "%.4f", stepSizes[stepIndex]);
        telemetry.addLine("=================================");
        telemetry.addLine("Controls:");
        telemetry.addLine("  Y - Toggle velocity");
        telemetry.addLine("  B - Change step size");
        telemetry.addLine("  D-Pad Up/Down - Adjust P");
        telemetry.addLine("  D-Pad Right/Left - Adjust F");
        telemetry.addLine("=================================");
        telemetry.update();
    }

    @Override
    public void stop() {
        // Stop the flywheel when OpMode ends
        if (flywheelMotor != null) {
            flywheelMotor.setVelocity(0);
        }
    }
}

