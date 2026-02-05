package org.firstinspires.ftc.teamcode.decode14464.systems;

import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DcMotor;
import org.firstinspires.ftc.teamcode.decode14464.LimelightManager;

/**
 * Turret Test TeleOp
 * DECODE 14464 - FTC 2026
 *
 * Four Test Modes:
 * 1. Counter-Rotation: Turret rotates opposite to robot rotation to maintain its field orientation
 * 2. Field-Relative: Turret adjusts based on robot's field position/angle to point at goal (LIMITED)
 * 3. Combined Goal Tracking: BEST MODE - Tracks goal based on X,Y position AND rotation (UNLIMITED)
 * 4. Manual: Direct control
 *
 * Controls:
 * ========
 * GAMEPAD 1 (Driver):
 *   Left Stick Y  - Forward/Backward drive
 *   Left Stick X  - Strafe left/right
 *   Right Stick X - Robot rotation
 *   Right Stick Y - Manual turret control (in manual mode only)
 *
 *   D-Pad Up      - Mode 1: Counter-Rotation Only
 *   D-Pad Right   - Mode 2: Field-Relative Only (limited ±360°)
 *   D-Pad Down    - Mode 3: Combined Goal Tracking (RECOMMENDED)
 *   D-Pad Left    - Manual Mode (disable auto-turret)
 *
 * GAMEPAD 2 (Testing Features - All Toggleable):
 *   A/Cross       - Toggle LIMELIGHT on/off
 *   B/Circle      - Toggle AUTO-CALIBRATION on/off
 *   X/Square      - MANUAL CALIBRATION (one-shot recalibrate turret position)
 *   Y/Triangle    - LIMELIGHT CALIBRATION (one-shot AprilTag calibration)
 *   Left Bumper   - Reset localization to (0,0,0)
 *   Right Bumper  - Reset turret encoder to 0
 *
 * Supports both Xbox and PS4 controllers!
 */
@TeleOp(name = "Turret Test", group = "Test")
public class TurretTest extends OpMode {

    // Subsystems
    private LocalizationSystem localization;
    private RobotMotors motors;
    private LimelightManager limelight;

    // ========== FEATURE TOGGLES (for isolated testing) ==========
    private boolean limelightEnabled = false;        // Toggle with GP2 A
    private boolean autoCalibrationEnabled = false;  // Toggle with GP2 B
    
    // Calibration state
    private static final double AUTO_CALIBRATE_COOLDOWN_MS = 2000;
    private static final double AUTO_CALIBRATE_TX_THRESHOLD = 5.0;
    private static final double AUTO_CALIBRATE_VELOCITY_THRESHOLD = 0.3;
    private double lastAutoCalibrationTime = 0;
    private String lastCalibrationStatus = "None";

    // GP2 button state tracking
    private boolean prevGP2A = false;
    private boolean prevGP2B = false;
    private boolean prevGP2X = false;
    private boolean prevGP2Y = false;
    private boolean prevGP2LeftBumper = false;
    private boolean prevGP2RightBumper = false;

    // Test modes
    private enum TestMode {
        MANUAL,              // Manual control only
        COUNTER_ROTATION,    // Compensate for robot rotation
        FIELD_RELATIVE,      // Adjust based on field position (point at goal) - LIMITED
        COMBINED             // Combined Goal Tracking: position + rotation (RECOMMENDED)
    }

    private TestMode currentMode = TestMode.COMBINED;  // Start in COMBINED mode (RECOMMENDED)

    // Turret control state
    private double turretTargetPosition = 0;  // Target encoder position
    private double lastHeading = 0;           // Last robot heading (for delta calculation)
    private double totalRobotRotation = 0;    // Total accumulated robot rotation (degrees)
    private double lastTotalRotation = 0;     // Previous total rotation (to detect direction changes)
    private double initialHeading = 0;        // Starting heading to track total rotation
    private boolean hasUnwound = false;       // Prevent repeated unwinding in same reversal
    private double previousHeadingChange = 0; // Track direction of rotation for reversal detection

    // Combined Goal Tracking state (X,Y position tracking)
    private double lastX = 0;                 // Last robot X position
    private double lastY = 0;                 // Last robot Y position
    private double lastAngleToGoal = 0;       // Last calculated angle to goal

    // PID control
    private double previousError = 0;
    private double integral = 0;

    // Use RobotConstants.Turret for shared calibration values
    private static final double TICKS_PER_DEGREE = RobotConstants.Turret.TICKS_PER_DEGREE;
    
    // TurretTest-specific limits (±360° for counter-rotation testing, not ±150° goal-tracking limits)
    private static final double MAX_ENCODER_POSITION = 1083;  // ~360 degrees (full rotation for testing)
    private static final double MIN_ENCODER_POSITION = -1083; // ~-360 degrees (full rotation for testing)
    private static final double WIRE_LIMIT = RobotConstants.Turret.WIRE_LIMIT_DEGREES;
    private static final double UNWIND_HYSTERESIS = RobotConstants.Turret.UNWIND_HYSTERESIS;

    // Use RobotConstants.Turret for PID parameters (ensures consistency)
    private static final double KP = RobotConstants.Turret.KP;
    private static final double KI = RobotConstants.Turret.KI;
    private static final double KD = RobotConstants.Turret.KD;
    private static final double MAX_POWER = RobotConstants.Turret.MAX_POWER;
    private static final double POSITION_TOLERANCE = 12;      // ticks (~4 degrees) - tighter accuracy for shooting

    // Button state tracking
    private boolean prevDpadUp = false;
    private boolean prevDpadRight = false;
    private boolean prevDpadDown = false;
    private boolean prevDpadLeft = false;

    @Override
    public void init() {
        // Initialize motors (RobotMotors.init() already configures encoder and brake mode)
        motors = new RobotMotors();
        motors.init(hardwareMap);

        // Note: RobotMotors.init() already resets encoder and sets RUN_USING_ENCODER mode
        // Just ensure brake mode is set (redundant but safe)
        motors.turret.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);

        // Initialize localization
        localization = new LocalizationSystem();
        localization.initTeleOp(hardwareMap);

        // Initialize Limelight (but starts disabled for isolated testing)
        limelight = new LimelightManager();
        limelight.init(hardwareMap);

        telemetry.addLine("✅ Turret Test Initialized");
        telemetry.addLine();
        telemetry.addLine("🎯 STARTING IN COMBINED GOAL TRACKING MODE");
        telemetry.addLine("   Drive AND rotate - turret tracks goal!");
        telemetry.addLine();
        telemetry.addLine("====== GP1: MODE CONTROLS ======");
        telemetry.addLine("D-Pad Up:    Counter-Rotation Only");
        telemetry.addLine("D-Pad Right: Field-Relative (limited)");
        telemetry.addLine("D-Pad Down:  Combined Goal Tracking ⭐BEST");
        telemetry.addLine("D-Pad Left:  Manual Mode");
        telemetry.addLine();
        telemetry.addLine("====== GP2: TEST FEATURES ======");
        telemetry.addLine("A: Toggle Limelight (OFF)");
        telemetry.addLine("B: Toggle Auto-Calibrate (OFF)");
        telemetry.addLine("X: Manual Recalibrate");
        telemetry.addLine("Y: Limelight Calibrate");
        telemetry.addLine("LB: Reset Localization");
        telemetry.addLine("RB: Reset Turret Encoder");
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
        lastTotalRotation = 0;
        hasUnwound = false;
        turretTargetPosition = motors.turret.getCurrentPosition();

        // Initialize combined goal tracking variables
        lastX = localization.getX();
        lastY = localization.getY();
        lastAngleToGoal = localization.getAngleToPoint(
            RobotConstants.Field.GOAL_X,
            RobotConstants.Field.GOAL_Y
        );

        telemetry.addLine("🚀 Turret Test Started!");
        telemetry.addLine("🎯 Combined Goal Tracking Active");
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
            -gamepad1.right_stick_x,
            false  // Robot-centric drive (set to true for field-centric)
        );

        // ========== GP2: FEATURE TOGGLES ==========
        // A/Cross Button: Toggle Limelight on/off
        boolean gp2APressed = gamepad2.a || gamepad2.cross; // Support both Xbox and PS4
        if (gp2APressed && !prevGP2A) {
            limelightEnabled = !limelightEnabled;
            gamepad2.rumble(limelightEnabled ? 200 : 100);
            telemetry.log().add(limelightEnabled ? "📷 Limelight ENABLED" : "📷 Limelight DISABLED");
        }
        prevGP2A = gp2APressed;

        // B/Circle Button: Toggle Auto-Calibration on/off
        boolean gp2BPressed = gamepad2.b || gamepad2.circle; // Support both Xbox and PS4
        if (gp2BPressed && !prevGP2B) {
            autoCalibrationEnabled = !autoCalibrationEnabled;
            gamepad2.rumble(autoCalibrationEnabled ? 200 : 100);
            telemetry.log().add(autoCalibrationEnabled ? "🔄 Auto-Calibration ENABLED" : "🔄 Auto-Calibration DISABLED");
        }
        prevGP2B = gp2BPressed;

        // X/Square Button: Manual Recalibrate (one-shot)
        boolean gp2XPressed = gamepad2.x || gamepad2.square; // Support both Xbox and PS4
        if (gp2XPressed && !prevGP2X) {
            performManualRecalibration();
        }
        prevGP2X = gp2XPressed;

        // Y/Triangle Button: Limelight Calibration (one-shot)
        boolean gp2YPressed = gamepad2.y || gamepad2.triangle; // Support both Xbox and PS4
        if (gp2YPressed && !prevGP2Y) {
            performLimelightCalibration(true);
        }
        prevGP2Y = gp2YPressed;

        // Left Bumper: Reset localization to (0, 0, 0)
        if (gamepad2.left_bumper && !prevGP2LeftBumper) {
            localization.getFollower().setPose(new com.pedropathing.geometry.Pose(0, 0, 0));
            resetTurretTracking();
            gamepad2.rumble(300);
            telemetry.log().add("📍 Localization RESET to (0, 0, 0°)");
        }
        prevGP2LeftBumper = gamepad2.left_bumper;

        // Right Bumper: Reset turret encoder to 0
        if (gamepad2.right_bumper && !prevGP2RightBumper) {
            motors.turret.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
            motors.turret.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
            turretTargetPosition = 0;
            previousError = 0;
            integral = 0;
            gamepad2.rumble(300);
            telemetry.log().add("🎯 Turret encoder RESET to 0");
        }
        prevGP2RightBumper = gamepad2.right_bumper;

        // ========== LIMELIGHT UPDATE (if enabled) ==========
        if (limelightEnabled && limelight != null) {
            limelight.update();
            
            // Auto-calibration (if enabled and conditions are met)
            if (autoCalibrationEnabled) {
                performAutoCalibration();
            }
        }

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
                // Combined Goal Tracking: handles BOTH position AND rotation
                updateCombinedGoalTracking();
                runTurretToPosition();  // Use unlimited rotation (no encoder limits)
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
     * SMART: Detects direction reversal using total rotation tracking
     */
    private void updateCounterRotation() {
        double currentHeading = localization.getHeadingDegrees();
        double rawHeadingChange = currentHeading - lastHeading;

        double headingChange = rawHeadingChange;

        // ONLY fix if there's a LARGE single-step jump (boundary crossing)
        // Example: Robot jumps from 357° to 7° in ONE step → raw change is -350°
        if (rawHeadingChange > 180) {
            headingChange = rawHeadingChange - 360;
            telemetry.log().add("Boundary jump: " + String.format("%.0f", rawHeadingChange) + "° → " + String.format("%.0f", headingChange) + "°");
        } else if (rawHeadingChange < -180) {
            headingChange = rawHeadingChange + 360;
            telemetry.log().add("Boundary jump: " + String.format("%.0f", rawHeadingChange) + "° → " + String.format("%.0f", headingChange) + "°");
        }

        // Track total rotation BEFORE applying heading change
        double previousTotalRotation = totalRobotRotation;
        totalRobotRotation += headingChange;

        // Counter-rotate FIRST: if robot turns right (+), turret turns left (-)
        turretTargetPosition -= headingChange * TICKS_PER_DEGREE;

        // REVERSAL DETECTION: Detect when robot changes rotation direction after completing 360°
        // Uses direction change detection instead of threshold-based approach
        boolean reversalDetected = false;

        // Detect direction change: sign of heading change flipped
        boolean directionChanged = (previousHeadingChange != 0) && 
                                   (Math.signum(headingChange) != Math.signum(previousHeadingChange)) &&
                                   (Math.abs(headingChange) > 1.0);  // Ignore tiny oscillations

        // Reset the unwind flag when we complete another full rotation in same direction
        if (Math.abs(totalRobotRotation) > 360 && !directionChanged) {
            hasUnwound = false;  // Reset flag, ready for next reversal
        }

        // Detect reversal: direction changed AND we've rotated past 360° AND haven't unwound yet
        if (!hasUnwound && directionChanged && Math.abs(lastTotalRotation) > 300) {
            reversalDetected = true;
            hasUnwound = true;  // SET FLAG to prevent repeated unwinding
            telemetry.log().add("🔄 REVERSAL! Direction changed at " + String.format("%.0f", totalRobotRotation) + "°");
        }

        // Update previous heading change for next iteration
        if (Math.abs(headingChange) > 0.5) {  // Only update if significant movement
            previousHeadingChange = headingChange;
        }

        // If reversal detected, unwind turret ONCE to prevent wire tangling
        if (reversalDetected) {
            double currentTurretDegrees = turretTargetPosition / TICKS_PER_DEGREE;

            // Force turret to unwind by ±360° to take shorter path
            if (Math.abs(currentTurretDegrees) > 20) {
                if (currentTurretDegrees > 0) {
                    turretTargetPosition -= 360.0 * TICKS_PER_DEGREE;
                    telemetry.log().add("⚡ UNWIND: Turret " + String.format("%.0f", currentTurretDegrees) + "° → " + String.format("%.0f", currentTurretDegrees - 360) + "°");
                } else {
                    turretTargetPosition += 360.0 * TICKS_PER_DEGREE;
                    telemetry.log().add("⚡ UNWIND: Turret " + String.format("%.0f", currentTurretDegrees) + "° → " + String.format("%.0f", currentTurretDegrees + 360) + "°");
                }
            }
        }


        // WIRE LIMIT: Safety at ±360° with hysteresis to prevent oscillation
        double currentTurretDegrees = turretTargetPosition / TICKS_PER_DEGREE;
        if (currentTurretDegrees > WIRE_LIMIT + UNWIND_HYSTERESIS) {
            turretTargetPosition -= 360.0 * TICKS_PER_DEGREE;
            telemetry.log().add("⚠️ WIRE LIMIT +360°: Unwinding -360°");
        } else if (currentTurretDegrees < -(WIRE_LIMIT + UNWIND_HYSTERESIS)) {
            turretTargetPosition += 360.0 * TICKS_PER_DEGREE;
            telemetry.log().add("⚠️ WIRE LIMIT -360°: Unwinding +360°");
        }

        lastHeading = currentHeading;
        lastTotalRotation = totalRobotRotation;
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

        // Track total rotation - use efficient normalization
        double headingChange = robotHeading - lastHeading;
        headingChange = normalizeAngle(headingChange);
        totalRobotRotation += headingChange;

        // Calculate turret angle needed (relative to robot)
        double turretAngle = angleToGoal - robotHeading;

        // Normalize to -180 to 180
        turretAngle = normalizeAngle(turretAngle);

        // Convert to encoder position
        turretTargetPosition = turretAngle * TICKS_PER_DEGREE;

        // Clamp to limits
        turretTargetPosition = Math.max(MIN_ENCODER_POSITION,
                                       Math.min(MAX_ENCODER_POSITION, turretTargetPosition));

        lastHeading = robotHeading;
    }

    /**
     * MODE 3: Combined Goal Tracking (RECOMMENDED)
     * Tracks goal based on BOTH:
     * 1. Robot X,Y position changes (when robot drives around)
     * 2. Robot rotation changes (counter-rotation to compensate)
     * 
     * Uses UNLIMITED rotation with automatic wire unwinding
     * This is the BEST mode for competition - turret always points at goal!
     */
    private void updateCombinedGoalTracking() {
        // Get current robot state
        double currentX = localization.getX();
        double currentY = localization.getY();
        double currentHeading = localization.getHeadingDegrees();

        // Calculate current angle to goal (field-relative, in degrees)
        double currentAngleToGoal = localization.getAngleToPoint(
            RobotConstants.Field.GOAL_X,
            RobotConstants.Field.GOAL_Y
        );

        // ========== HANDLE ROBOT ROTATION (Counter-Rotation Logic) ==========
        double rawHeadingChange = currentHeading - lastHeading;

        // Normalize heading change for boundary crossings (e.g., 359° to 1°)
        double headingChange = rawHeadingChange;
        if (rawHeadingChange > 180) {
            headingChange = rawHeadingChange - 360;
        } else if (rawHeadingChange < -180) {
            headingChange = rawHeadingChange + 360;
        }

        // Track total robot rotation (for wire management)
        totalRobotRotation += headingChange;

        // Counter-rotate for robot heading change: if robot turns right (+), turret turns left (-)
        turretTargetPosition -= headingChange * TICKS_PER_DEGREE;

        // ========== HANDLE ROBOT POSITION CHANGE (X,Y Movement) ==========
        // When robot moves, the angle to goal changes
        // We need to adjust the turret to track this change
        double angleToGoalChange = currentAngleToGoal - lastAngleToGoal;

        // Normalize angle change for boundary crossings
        if (angleToGoalChange > 180) {
            angleToGoalChange -= 360;
        } else if (angleToGoalChange < -180) {
            angleToGoalChange += 360;
        }

        // Adjust turret for position-based angle change
        // When angle to goal increases (goal moves "right" relative to robot), turret turns right (+)
        turretTargetPosition += angleToGoalChange * TICKS_PER_DEGREE;

        // ========== REVERSAL DETECTION (Smart Wire Unwinding) ==========
        boolean directionChanged = (previousHeadingChange != 0) &&
                                   (Math.signum(headingChange) != Math.signum(previousHeadingChange)) &&
                                   (Math.abs(headingChange) > 1.0);

        // Reset unwind flag when we complete another full rotation
        if (Math.abs(totalRobotRotation) > 360) {
            hasUnwound = false;
        }

        // If direction reversed and haven't unwound yet, unwind turret
        if (directionChanged && !hasUnwound && Math.abs(lastTotalRotation) > 300) {
            double currentTurretDegrees = turretTargetPosition / TICKS_PER_DEGREE;

            if (Math.abs(currentTurretDegrees) > 20) {
                if (currentTurretDegrees > 0) {
                    turretTargetPosition -= 360.0 * TICKS_PER_DEGREE;
                    telemetry.log().add("⚡ UNWIND: " + String.format("%.0f", currentTurretDegrees) + "° → " + 
                                       String.format("%.0f", currentTurretDegrees - 360) + "°");
                } else {
                    turretTargetPosition += 360.0 * TICKS_PER_DEGREE;
                    telemetry.log().add("⚡ UNWIND: " + String.format("%.0f", currentTurretDegrees) + "° → " + 
                                       String.format("%.0f", currentTurretDegrees + 360) + "°");
                }
                hasUnwound = true;
            }
        }

        // ========== WIRE LIMIT PROTECTION ==========
        double currentTurretDegrees = turretTargetPosition / TICKS_PER_DEGREE;
        if (currentTurretDegrees > WIRE_LIMIT + UNWIND_HYSTERESIS) {
            turretTargetPosition -= 360.0 * TICKS_PER_DEGREE;
            telemetry.log().add("⚠️ WIRE LIMIT: Auto-unwinding -360°");
        } else if (currentTurretDegrees < -(WIRE_LIMIT + UNWIND_HYSTERESIS)) {
            turretTargetPosition += 360.0 * TICKS_PER_DEGREE;
            telemetry.log().add("⚠️ WIRE LIMIT: Auto-unwinding +360°");
        }

        // ========== UPDATE TRACKING VARIABLES ==========
        lastHeading = currentHeading;
        lastX = currentX;
        lastY = currentY;
        lastAngleToGoal = currentAngleToGoal;
        lastTotalRotation = totalRobotRotation;

        // Only update previous heading change if significant movement (prevents noise)
        if (Math.abs(headingChange) > 0.5) {
            previousHeadingChange = headingChange;
        }
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

        // PID calculation - only accumulate integral if KI is non-zero
        if (KI != 0) {
            integral += error;
            // Anti-windup: limit integral to prevent excessive buildup
            double integralLimit = 500;
            integral = Math.max(-integralLimit, Math.min(integralLimit, integral));
        }

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

        // PID calculation - only accumulate integral if KI is non-zero
        if (KI != 0) {
            integral += error;
            // Anti-windup: limit integral to prevent excessive buildup
            double integralLimit = 500;
            integral = Math.max(-integralLimit, Math.min(integralLimit, integral));
        }

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
        lastTotalRotation = 0;   // Reset last total rotation tracker
        hasUnwound = false;      // Reset unwind flag
        previousHeadingChange = 0;  // Reset direction tracking
        turretTargetPosition = motors.turret.getCurrentPosition();
        previousError = 0;
        integral = 0;

        // Reset combined goal tracking variables
        lastX = localization.getX();
        lastY = localization.getY();
        lastAngleToGoal = localization.getAngleToPoint(
            RobotConstants.Field.GOAL_X,
            RobotConstants.Field.GOAL_Y
        );
    }

    /**
     * Normalize angle to range -180 to 180 degrees
     * More efficient than while loops for large angles
     */
    private double normalizeAngle(double angle) {
        angle = angle % 360;
        if (angle > 180) {
            angle -= 360;
        } else if (angle < -180) {
            angle += 360;
        }
        return angle;
    }

    // ========== CALIBRATION METHODS ==========

    /**
     * Manual Recalibration - sets current turret position as the "correct" reference
     * Use when you've manually aimed the turret correctly and want to sync tracking
     */
    private void performManualRecalibration() {
        // Set target to current position (zero out error)
        turretTargetPosition = motors.turret.getCurrentPosition();
        
        // Reset PID state
        previousError = 0;
        integral = 0;
        
        // Reset tracking references to current state
        lastHeading = localization.getHeadingDegrees();
        lastX = localization.getX();
        lastY = localization.getY();
        lastAngleToGoal = localization.getAngleToPoint(
            RobotConstants.Field.GOAL_X,
            RobotConstants.Field.GOAL_Y
        );
        
        lastCalibrationStatus = "Manual Recal OK";
        gamepad2.rumble(300);
        telemetry.log().add("✅ MANUAL RECALIBRATE: Position locked as reference!");
    }

    /**
     * Limelight-based calibration using AprilTag detection
     * Calibrates turret pointing based on tx (horizontal angle to target)
     * @param isManual true if triggered by button, false if auto-triggered
     */
    private void performLimelightCalibration(boolean isManual) {
        // Check if Limelight is available
        if (limelight == null) {
            if (isManual) {
                lastCalibrationStatus = "FAILED: No Limelight";
                telemetry.log().add("❌ Limelight not initialized!");
            }
            return;
        }

        if (!limelight.isConnected()) {
            if (isManual) {
                lastCalibrationStatus = "FAILED: Not connected";
                telemetry.log().add("❌ Limelight not connected!");
            }
            return;
        }

        if (!limelight.hasValidTarget()) {
            if (isManual) {
                lastCalibrationStatus = "FAILED: No AprilTag";
                telemetry.log().add("❌ No AprilTag visible!");
            }
            return;
        }

        // Get Limelight data
        double tx = limelight.getTargetX(); // Horizontal angle to target
        com.pedropathing.geometry.Pose limelightPose = limelight.getRobotPoseFromAprilTag();

        // === TURRET CALIBRATION ===
        // tx tells us how many degrees the turret is off from pointing at the target
        // Positive tx = target is to the RIGHT, turret needs to turn RIGHT (positive)
        if (Math.abs(tx) > 0.5) { // Only calibrate if there's meaningful offset
            // Adjust turret target position based on tx error
            turretTargetPosition += tx * TICKS_PER_DEGREE;
            
            // Reset PID to avoid integral windup after position jump
            previousError = 0;
            integral = 0;
        }

        // === LOCALIZATION CALIBRATION (if pose available) ===
        if (limelightPose != null) {
            localization.calibrateFromLimelight(limelightPose);
            
            // Update tracking references after position change
            lastX = localization.getX();
            lastY = localization.getY();
            lastAngleToGoal = localization.getAngleToPoint(
                RobotConstants.Field.GOAL_X,
                RobotConstants.Field.GOAL_Y
            );
        }

        // Update status
        if (isManual) {
            lastCalibrationStatus = String.format("Manual OK (tx=%.1f°)", tx);
            gamepad2.rumble(300);
            telemetry.log().add(String.format("✅ LIMELIGHT CALIBRATE: tx=%.1f°", tx));
            if (limelightPose != null) {
                telemetry.log().add(String.format("   Position: (%.1f, %.1f)", limelightPose.getX(), limelightPose.getY()));
            }
        } else {
            lastAutoCalibrationTime = System.currentTimeMillis();
            lastCalibrationStatus = String.format("Auto OK (tx=%.1f°)", tx);
            gamepad2.rumble(50); // Light rumble for auto
        }
    }

    /**
     * Automatic calibration - triggers when conditions are optimal
     * Only runs if autoCalibrationEnabled and limelightEnabled are true
     */
    private void performAutoCalibration() {
        // Check cooldown
        double currentTime = System.currentTimeMillis();
        if (currentTime - lastAutoCalibrationTime < AUTO_CALIBRATE_COOLDOWN_MS) {
            return;
        }

        // Check if Limelight has valid target
        if (limelight == null || !limelight.hasValidTarget()) {
            return;
        }

        // Check if robot is relatively stationary (using gamepad inputs as proxy)
        double driveInput = Math.abs(gamepad1.left_stick_x) + Math.abs(gamepad1.left_stick_y) + Math.abs(gamepad1.right_stick_x);
        if (driveInput > AUTO_CALIBRATE_VELOCITY_THRESHOLD) {
            return; // Robot is moving
        }

        // Check tx error - only auto-calibrate if error is small enough
        double tx = limelight.getTargetX();
        if (Math.abs(tx) > AUTO_CALIBRATE_TX_THRESHOLD) {
            return; // Error too large (might be wrong target)
        }

        // All conditions met
        performLimelightCalibration(false);
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
            double headingChange = normalizeAngle(currentHeading - lastHeading);
            telemetry.addData("⚡ Heading Change", "%.2f° (turret compensating)", headingChange);
        }

        if (currentMode == TestMode.FIELD_RELATIVE || currentMode == TestMode.COMBINED) {
            double angleToGoal = localization.getAngleToPoint(
                RobotConstants.Field.GOAL_X,
                RobotConstants.Field.GOAL_Y
            );
            telemetry.addData("🎯 Angle to Goal", "%.1f°", angleToGoal);
            
            // Show distance to goal
            double distanceToGoal = localization.getDistanceToPoint(
                RobotConstants.Field.GOAL_X,
                RobotConstants.Field.GOAL_Y
            );
            telemetry.addData("📏 Distance to Goal", "%.1f in", distanceToGoal);
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
        if (currentMode == TestMode.COUNTER_ROTATION || currentMode == TestMode.COMBINED) {
            telemetry.addLine("🔄 AUTO WIRE UNWINDING ACTIVE");
            telemetry.addData("Wire Limit", "±%.0f°", WIRE_LIMIT);
            telemetry.addData("Current Angle", "%.1f°", currentPosition / TICKS_PER_DEGREE);
            telemetry.addLine("Turret will auto-unwind at limit");
        } else {
            telemetry.addData("Max Position", "%.0f ticks", MAX_ENCODER_POSITION);
            telemetry.addData("Min Position", "%.0f ticks", MIN_ENCODER_POSITION);
            telemetry.addData("Limit Status", getLimitStatus(currentPosition));
        }

        // ===== LIMELIGHT & CALIBRATION STATUS =====
        telemetry.addLine();
        telemetry.addLine("====== TEST FEATURES ======");
        telemetry.addData("📷 Limelight", limelightEnabled ? "✓ ON" : "✗ OFF");
        telemetry.addData("🔄 Auto-Calibrate", autoCalibrationEnabled ? "✓ ON" : "✗ OFF");
        
        if (limelightEnabled && limelight != null) {
            telemetry.addLine();
            telemetry.addLine("===== LIMELIGHT =====");
            if (limelight.isConnected()) {
                telemetry.addData("Status", "✓ Connected");
                if (limelight.hasValidTarget()) {
                    telemetry.addData("AprilTag", "✓ VISIBLE");
                    telemetry.addData("tx (offset)", "%.1f°", limelight.getTargetX());
                    telemetry.addData("ty (vertical)", "%.1f°", limelight.getTargetY());
                    telemetry.addData("Tags Seen", limelight.getVisibleTagCount());
                } else {
                    telemetry.addData("AprilTag", "✗ Not visible");
                }
            } else {
                telemetry.addData("Status", "✗ Not connected");
            }
            telemetry.addData("Last Calibration", lastCalibrationStatus);
        }

        telemetry.addLine();
        telemetry.addLine("====== GP2 CONTROLS ======");
        telemetry.addData("A/Cross", "Limelight: " + (limelightEnabled ? "ON" : "OFF"));
        telemetry.addData("B/Circle", "Auto-Cal: " + (autoCalibrationEnabled ? "ON" : "OFF"));
        telemetry.addData("X/Square", "Manual Recalibrate");
        telemetry.addData("Y/Triangle", "Limelight Calibrate");
        telemetry.addData("LB", "Reset Localization");
        telemetry.addData("RB", "Reset Turret Encoder");

        telemetry.addLine();
        telemetry.addLine("====== GP1 INSTRUCTIONS ======");
        if (currentMode == TestMode.COUNTER_ROTATION) {
            telemetry.addLine("🔄 ROTATE ROBOT with Right Stick X");
            telemetry.addLine("👀 Watch turret rotate OPPOSITE direction");
        } else if (currentMode == TestMode.FIELD_RELATIVE) {
            telemetry.addLine("🚗 DRIVE AROUND the field");
            telemetry.addLine("👀 Watch turret track the goal");
            telemetry.addLine("⚠️ LIMITED to ±360° encoder range");
        } else if (currentMode == TestMode.COMBINED) {
            telemetry.addLine("⭐ COMBINED GOAL TRACKING (BEST)");
            telemetry.addLine("🚗 DRIVE + 🔄 ROTATE freely");
            telemetry.addLine("🎯 Turret tracks goal automatically");
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
        if (limelight != null) {
            limelight.stop();
        }
        telemetry.addLine("Turret Test Stopped");
        telemetry.update();
    }
}

