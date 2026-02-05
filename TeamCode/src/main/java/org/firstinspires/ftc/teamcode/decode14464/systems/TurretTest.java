package org.firstinspires.ftc.teamcode.decode14464.systems;

import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DcMotor;

/**
 * Turret Test TeleOp
 * DECODE 14464 - FTC 2026
 *
 * Three Test Modes:
 * 1. Counter-Rotation: Turret rotates opposite to robot rotation to maintain its field orientation
 * 2. Field-Relative: Turret adjusts based on robot's field position/angle to point at goal
 * 3. Combined: Both counter-rotation AND field-relative adjustments work together
 *
 * Controls:
 *   Left Stick Y  - Forward/Backward drive
 *   Left Stick X  - Strafe left/right
 *   Right Stick X - Robot rotation
 *
 *   D-Pad Up      - Mode 1: Counter-Rotation Only
 *   D-Pad Right   - Mode 2: Field-Relative Only (point at goal)
 *   D-Pad Down    - Mode 3: Combined Mode
 *   D-Pad Left    - Manual Mode (disable auto-turret)
 *
 *   Right Stick Y - Manual turret control (in manual mode only)
 */
@TeleOp(name = "Turret Test", group = "Test")
public class TurretTest extends OpMode {

    // Subsystems
    private LocalizationSystem localization;
    private RobotMotors motors;

    // Test modes
    private enum TestMode {
        MANUAL,              // Manual control only
        COUNTER_ROTATION,    // Compensate for robot rotation
        FIELD_RELATIVE,      // Adjust based on field position (point at goal)
        COMBINED             // Both counter-rotation and field-relative
    }

    private TestMode currentMode = TestMode.COUNTER_ROTATION;  // Start in counter-rotation mode for easy testing

    // Turret control state
    private double turretTargetPosition = 0;  // Target encoder position
    private double lastHeading = 0;           // Last robot heading (for delta calculation)
    private double totalRobotRotation = 0;    // Total accumulated robot rotation (degrees)
    private double initialHeading = 0;        // Starting heading to track total rotation

    // PID control
    private double previousError = 0;
    private double integral = 0;

    // Constants (calibrated)
    private static final double TICKS_PER_DEGREE = 3.01;  // Measured from TurretCalibration
    private static final double MAX_ENCODER_POSITION = 1083;  // ~360 degrees (full rotation)
    private static final double MIN_ENCODER_POSITION = -1083; // ~-360 degrees (full rotation)
    private static final double WIRE_LIMIT = 360;  // Auto-unwind at exactly 360° (one full rotation)

    // Control parameters - Tuned for VERY smooth motion
    private static final double KP = 0.006;                   // Proportional gain (further reduced)
    private static final double KI = 0.0;                     // Integral gain
    private static final double KD = 0.0005;                  // Derivative gain (further reduced)
    private static final double MAX_POWER = 0.35;             // Further reduced max power
    private static final double POSITION_TOLERANCE = 25;      // ticks (~8.3 degrees) - even wider tolerance

    // Button state tracking
    private boolean prevDpadUp = false;
    private boolean prevDpadRight = false;
    private boolean prevDpadDown = false;
    private boolean prevDpadLeft = false;

    @Override
    public void init() {
        // Initialize motors
        motors = new RobotMotors();
        motors.init(hardwareMap);

        // Reset and configure turret motor
        motors.turret.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        motors.turret.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        motors.turret.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);

        // Initialize localization
        localization = new LocalizationSystem();
        localization.initTeleOp(hardwareMap);

        telemetry.addLine("✅ Turret Test Initialized");
        telemetry.addLine();
        telemetry.addLine("🔄 STARTING IN COUNTER-ROTATION MODE");
        telemetry.addLine("   Rotate robot to test!");
        telemetry.addLine();
        telemetry.addLine("====== MODE CONTROLS ======");
        telemetry.addLine("D-Pad Up:    Counter-Rotation Test");
        telemetry.addLine("D-Pad Right: Field-Relative Test");
        telemetry.addLine("D-Pad Down:  Combined Test");
        telemetry.addLine("D-Pad Left:  Manual Mode");
        telemetry.addLine();
        telemetry.addLine("====== DRIVE CONTROLS ======");
        telemetry.addLine("Left Stick:  Drive (Y/X)");
        telemetry.addLine("Right Stick X: Rotate Robot ⭐");
        telemetry.addLine("Right Stick Y: Manual Turret (manual mode)");
        telemetry.update();
    }

    @Override
    public void start() {
        // Start teleop drive
        localization.getFollower().startTeleopDrive();

        // Initialize tracking variables
        initialHeading = localization.getHeadingDegrees();
        lastHeading = initialHeading;
        totalRobotRotation = 0;
        turretTargetPosition = motors.turret.getCurrentPosition();

        telemetry.addLine("🚀 Turret Test Started!");
        telemetry.update();
    }

    @Override
    public void loop() {
        // Update localization
        localization.update();

        // ========== MODE SELECTION ==========
        if (gamepad1.dpad_up && !prevDpadUp) {
            currentMode = TestMode.COUNTER_ROTATION;
            resetTurretTracking();
        }
        prevDpadUp = gamepad1.dpad_up;

        if (gamepad1.dpad_right && !prevDpadRight) {
            currentMode = TestMode.FIELD_RELATIVE;
            resetTurretTracking();
        }
        prevDpadRight = gamepad1.dpad_right;

        if (gamepad1.dpad_down && !prevDpadDown) {
            currentMode = TestMode.COMBINED;
            resetTurretTracking();
        }
        prevDpadDown = gamepad1.dpad_down;

        if (gamepad1.dpad_left && !prevDpadLeft) {
            currentMode = TestMode.MANUAL;
            motors.turret.setPower(0);
            previousError = 0;
                        integral = 0;
        }
        prevDpadLeft = gamepad1.dpad_left;

        // ========== DRIVE CONTROL ==========
        localization.getFollower().setTeleOpDrive(
            -gamepad1.left_stick_y,
            -gamepad1.left_stick_x,
            -gamepad1.right_stick_x
        );

        // ========== TURRET CONTROL BASED ON MODE ==========
        switch (currentMode) {
            case MANUAL:
                // Manual turret control with right stick Y
                double manualPower = -gamepad1.right_stick_y * 0.3;
                motors.turret.setPower(manualPower);
                break;

            case COUNTER_ROTATION:
                updateCounterRotation();
                runTurretToPosition();
                break;

            case FIELD_RELATIVE:
                updateFieldRelative();
                runTurretToPositionWithLimits();
                break;

            case COMBINED:
                updateCombined();
                runTurretToPositionWithLimits();
                break;
        }

        // ========== TELEMETRY ==========
        displayTelemetry();
    }

    /**
     * MODE 1: Counter-Rotation
     * Turret rotates opposite to robot rotation to maintain its field orientation
     * When robot turns right (+), turret turns left (-) relative to robot
     *
     * SIMPLE LOGIC: Only fix large jumps (boundary crossings), otherwise follow heading directly
     */
    private void updateCounterRotation() {
        double currentHeading = localization.getHeadingDegrees();
        double rawHeadingChange = currentHeading - lastHeading;

        double headingChange = rawHeadingChange;

        // ONLY fix if there's a LARGE single-step jump (boundary crossing)
        // Example: Robot jumps from 357° to 7° in ONE step → raw change is -350°
        if (rawHeadingChange > 180) {
            headingChange = rawHeadingChange - 360;
            telemetry.log().add("Boundary: " + String.format("%.0f", rawHeadingChange) + "° → " + String.format("%.0f", headingChange) + "°");
        } else if (rawHeadingChange < -180) {
            headingChange = rawHeadingChange + 360;
            telemetry.log().add("Boundary: " + String.format("%.0f", rawHeadingChange) + "° → " + String.format("%.0f", headingChange) + "°");
        }
        // Otherwise: use raw change directly (allows smooth 0→100→200→360 tracking)

        // Track total rotation
        totalRobotRotation += headingChange;

        // Counter-rotate: if robot turns right (+), turret turns left (-)
        turretTargetPosition -= headingChange * TICKS_PER_DEGREE;

        // WIRE LIMIT: Only unwind at ±360° (safety backup)
        double currentTurretDegrees = turretTargetPosition / TICKS_PER_DEGREE;
        if (currentTurretDegrees > WIRE_LIMIT) {
            turretTargetPosition -= 360.0 * TICKS_PER_DEGREE;
            telemetry.log().add("WIRE LIMIT: Unwinding -360°");
        } else if (currentTurretDegrees < -WIRE_LIMIT) {
            turretTargetPosition += 360.0 * TICKS_PER_DEGREE;
            telemetry.log().add("WIRE LIMIT: Unwinding +360°");
        }

        lastHeading = currentHeading;
    }

    /**
     * MODE 2: Field-Relative
     * Turret adjusts based on robot's position and heading to always point at goal
     */
    private void updateFieldRelative() {
        // Calculate angle from robot to goal
        double angleToGoal = localization.getAngleToPoint(
            RobotConstants.Field.GOAL_X,
            RobotConstants.Field.GOAL_Y
        );

        // Get robot heading
        double robotHeading = localization.getHeadingDegrees();

        // Track total rotation
        double headingChange = robotHeading - lastHeading;
        while (headingChange > 180) headingChange -= 360;
        while (headingChange < -180) headingChange += 360;
        totalRobotRotation += headingChange;

        // Calculate turret angle needed (relative to robot)
        double turretAngle = angleToGoal - robotHeading;

        // Normalize to -180 to 180
        while (turretAngle > 180) turretAngle -= 360;
        while (turretAngle < -180) turretAngle += 360;

        // Convert to encoder position
        turretTargetPosition = turretAngle * TICKS_PER_DEGREE;

        // Clamp to limits
        turretTargetPosition = Math.max(MIN_ENCODER_POSITION,
                                       Math.min(MAX_ENCODER_POSITION, turretTargetPosition));

        lastHeading = robotHeading;
    }

    /**
     * MODE 3: Combined
     * Both counter-rotation compensation AND field-relative targeting
     * This maintains goal tracking even as the robot rotates
     */
    private void updateCombined() {
        // Simply recalculate field-relative angle each loop
        // This naturally includes counter-rotation because we're always
        // calculating the fresh angle to goal based on current position and heading
        updateFieldRelative();
    }

    /**
     * Run turret motor to target position using PID control (unlimited rotation)
     * Used for counter-rotation mode
     */
    private void runTurretToPosition() {
        double currentPosition = motors.turret.getCurrentPosition();
        double error = turretTargetPosition - currentPosition;

        // Check if at target
        if (Math.abs(error) <= POSITION_TOLERANCE) {
            motors.turret.setPower(0);
            previousError = 0;
            integral = 0;
            return;
        }

        // PID calculation
        integral += error;

        // Anti-windup: limit integral to prevent excessive buildup
        double integralLimit = 500;  // Reduced for smoother response
        integral = Math.max(-integralLimit, Math.min(integralLimit, integral));

        double derivative = error - previousError;

        double power = (KP * error) + (KI * integral) + (KD * derivative);

        // Clamp power
        power = Math.max(-MAX_POWER, Math.min(MAX_POWER, power));

        // Smoother minimum power threshold - very gradual ramp up
        double minPower = 0.06; // Even lower minimum power
        if (Math.abs(power) > 0.01 && Math.abs(power) < minPower) {
            power = Math.signum(power) * minPower;
        }

        // Apply VERY SMOOTH power changes (reduced from 0.15 to 0.10)
        double maxPowerChange = 0.10; // Maximum power change per loop - SMOOTHER
        double lastPower = motors.turret.getPower();
        double powerChange = power - lastPower;

        if (Math.abs(powerChange) > maxPowerChange) {
            power = lastPower + Math.signum(powerChange) * maxPowerChange;
        }

        // Apply power (no limits for counter-rotation)
        motors.turret.setPower(power);
        previousError = error;
    }

    /**
     * Run turret motor to target position with encoder limits
     * Used for field-relative and combined modes
     */
    private void runTurretToPositionWithLimits() {
        double currentPosition = motors.turret.getCurrentPosition();
        double error = turretTargetPosition - currentPosition;

        // Check if at target
        if (Math.abs(error) <= POSITION_TOLERANCE) {
            motors.turret.setPower(0);
            previousError = 0;
            integral = 0;
            return;
        }

        // PID calculation
        integral += error;

        // Anti-windup: limit integral to prevent excessive buildup
        double integralLimit = 500;  // Reduced for smoother response
        integral = Math.max(-integralLimit, Math.min(integralLimit, integral));

        double derivative = error - previousError;

        double power = (KP * error) + (KI * integral) + (KD * derivative);

        // Clamp power
        power = Math.max(-MAX_POWER, Math.min(MAX_POWER, power));

        // Smoother minimum power threshold - very gradual ramp up
        double minPower = 0.06; // Even lower minimum power
        if (Math.abs(power) > 0.01 && Math.abs(power) < minPower) {
            power = Math.signum(power) * minPower;
        }

        // Apply VERY SMOOTH power changes (reduced from 0.15 to 0.10)
        double maxPowerChange = 0.10; // Maximum power change per loop - SMOOTHER
        double lastPower = motors.turret.getPower();
        double powerChange = power - lastPower;

        if (Math.abs(powerChange) > maxPowerChange) {
            power = lastPower + Math.signum(powerChange) * maxPowerChange;
        }

        // Enforce encoder limits for field-relative modes
        power = enforceTurretLimits(currentPosition, power);

        // Apply power
        motors.turret.setPower(power);
        previousError = error;
    }

    /**
     * Enforce turret encoder limits (safety feature)
     */
    private double enforceTurretLimits(double currentPosition, double power) {
        // Hard stop at limits
        if (currentPosition >= MAX_ENCODER_POSITION && power > 0) {
            return 0;
        }
        if (currentPosition <= MIN_ENCODER_POSITION && power < 0) {
            return 0;
        }

        // Soft limits - reduce power near edges
        double softLimitZone = 50; // ticks
        if (currentPosition > MAX_ENCODER_POSITION - softLimitZone && power > 0) {
            double reduction = (MAX_ENCODER_POSITION - currentPosition) / softLimitZone;
            power *= reduction;
        }
        if (currentPosition < MIN_ENCODER_POSITION + softLimitZone && power < 0) {
            double reduction = (currentPosition - MIN_ENCODER_POSITION) / softLimitZone;
            power *= reduction;
        }

        return power;
    }

    /**
     * Reset turret tracking when changing modes
     */
    private void resetTurretTracking() {
        lastHeading = localization.getHeadingDegrees();
        totalRobotRotation = 0;  // Reset rotation counter when changing modes
        turretTargetPosition = motors.turret.getCurrentPosition();
        previousError = 0;
        integral = 0;
    }

    /**
     * Display comprehensive telemetry
     */
    private void displayTelemetry() {
        double currentPosition = motors.turret.getCurrentPosition();
        double error = turretTargetPosition - currentPosition;

        telemetry.addLine("====== TURRET TEST ======");
        telemetry.addData("🎮 Active Mode", currentMode);
        telemetry.addLine();

        telemetry.addLine("====== ROBOT STATE ======");
        telemetry.addData("Position (X, Y)", "(%.1f, %.1f)",
            localization.getX(), localization.getY());
        telemetry.addData("Robot Heading", "%.1f°", localization.getHeadingDegrees());

        // Show total robot rotation
        int fullRotations = (int)(totalRobotRotation / 360.0);
        double remainingDegrees = totalRobotRotation % 360.0;
        telemetry.addData("🔄 Total Rotation", "%.1f° (%d × 360° + %.1f°)",
            totalRobotRotation, fullRotations, remainingDegrees);

        // Show heading change in counter-rotation mode
        if (currentMode == TestMode.COUNTER_ROTATION || currentMode == TestMode.COMBINED) {
            double currentHeading = localization.getHeadingDegrees();
            double headingChange = currentHeading - lastHeading;
            while (headingChange > 180) headingChange -= 360;
            while (headingChange < -180) headingChange += 360;
            telemetry.addData("⚡ Heading Change", "%.2f° (turret compensating)", headingChange);
        }

        if (currentMode == TestMode.FIELD_RELATIVE || currentMode == TestMode.COMBINED) {
            double angleToGoal = localization.getAngleToPoint(
                RobotConstants.Field.GOAL_X,
                RobotConstants.Field.GOAL_Y
            );
            telemetry.addData("🎯 Angle to Goal", "%.1f°", angleToGoal);
        }

        telemetry.addLine();
        telemetry.addLine("====== TURRET STATE ======");
        telemetry.addData("Current Position", "%.0f ticks", currentPosition);
        telemetry.addData("Target Position", "%.0f ticks", turretTargetPosition);
        telemetry.addData("Error", "%.0f ticks (%.1f°)", error, error / TICKS_PER_DEGREE);

        // Show turret angle and wire status
        double turretDegrees = currentPosition / TICKS_PER_DEGREE;
        telemetry.addData("Turret Angle", "%.1f°", turretDegrees);

        // Wire safety indicator
        double wireUsage = Math.abs(turretDegrees) / WIRE_LIMIT * 100.0;
        String wireStatus;
        if (wireUsage < 50) {
            wireStatus = "✅ Safe (" + String.format("%.0f", wireUsage) + "%)";
        } else if (wireUsage < 80) {
            wireStatus = "⚠️ Moderate (" + String.format("%.0f", wireUsage) + "%)";
        } else if (wireUsage < 100) {
            wireStatus = "⚠️ HIGH (" + String.format("%.0f", wireUsage) + "%)";
        } else {
            wireStatus = "🔄 UNWINDING!";
        }
        telemetry.addData("Wire Status", wireStatus);

        telemetry.addData("Motor Power", "%.2f", motors.turret.getPower());
        telemetry.addData("At Target?", Math.abs(error) <= POSITION_TOLERANCE ? "YES ✓" : "NO");

        telemetry.addLine();
        telemetry.addLine("====== LIMITS & SAFETY ======");
        if (currentMode == TestMode.COUNTER_ROTATION) {
            telemetry.addLine("🔄 AUTO WIRE UNWINDING ACTIVE");
            telemetry.addData("Wire Limit", "±%.0f°", WIRE_LIMIT);
            telemetry.addData("Current Angle", "%.1f°", currentPosition / TICKS_PER_DEGREE);
            telemetry.addLine("Turret will auto-unwind at limit");
        } else {
            telemetry.addData("Max Position", "%.0f ticks", MAX_ENCODER_POSITION);
            telemetry.addData("Min Position", "%.0f ticks", MIN_ENCODER_POSITION);
            telemetry.addData("Limit Status", getLimitStatus(currentPosition));
        }

        telemetry.addLine();
        telemetry.addLine("====== INSTRUCTIONS ======");
        if (currentMode == TestMode.COUNTER_ROTATION) {
            telemetry.addLine("🔄 ROTATE ROBOT with Right Stick X");
            telemetry.addLine("👀 Watch turret rotate OPPOSITE direction");
            telemetry.addLine("   to maintain field orientation");
        } else if (currentMode == TestMode.FIELD_RELATIVE) {
            telemetry.addLine("🚗 DRIVE AROUND the field");
            telemetry.addLine("👀 Watch turret track the goal");
        } else if (currentMode == TestMode.COMBINED) {
            telemetry.addLine("🔄 ROTATE and DRIVE together");
            telemetry.addLine("👀 Turret maintains goal lock");
        } else {
            telemetry.addLine("Use Right Stick Y for manual control");
        }

        telemetry.update();
    }

    /**
     * Get limit status string
     */
    private String getLimitStatus(double position) {
        if (position >= MAX_ENCODER_POSITION) {
            return "AT MAX LIMIT!";
        } else if (position <= MIN_ENCODER_POSITION) {
            return "AT MIN LIMIT!";
        } else if (position > MAX_ENCODER_POSITION - 50) {
            return "Near Max";
        } else if (position < MIN_ENCODER_POSITION + 50) {
            return "Near Min";
        } else {
            return "OK";
        }
    }

    @Override
    public void stop() {
        motors.turret.setPower(0);
        telemetry.addLine("Turret Test Stopped");
        telemetry.update();
    }
}

