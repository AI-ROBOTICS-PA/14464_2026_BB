package org.firstinspires.ftc.teamcode.decode14464;

import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;

/**
 * AutoTurret - Automatically aims turret (with Limelight) at AprilTag
 *
 * Features:
 * - Tracks AprilTag ID 20 or 24
 * - Limits rotation to 270 degrees using encoder
 * - PID control for smooth aiming
 * - Manual override available
 *
 * Controls:
 * - Cross (X): Enable auto-tracking
 * - Triangle: Switch between tag 20 and 24
 * - Circle: Reset turret to center (0 degrees)
 * - Right Stick X: Manual turret control
 * - D-Pad Up/Down: Fine tune position
 */
@TeleOp(name = "Auto Turret", group = "Competition")
public class AutoTurret extends OpMode {

    // Hardware
    private DcMotorEx turretMotor;
    private LimelightManager limelightManager = new LimelightManager();

    // ========== TURRET CONFIGURATION ==========
    // Motor encoder positions (adjust based on your turret setup)
    public static double ENCODER_TICKS_PER_DEGREE = 5.0; // TUNE THIS - ticks per degree of rotation
    public static double MAX_ROTATION_DEGREES = 270.0;   // Maximum rotation allowed
    public static double MIN_ROTATION_DEGREES = 0.0;     // Minimum rotation (typically 0)

    // Calculate encoder limits
    private double maxEncoderPosition = MAX_ROTATION_DEGREES * ENCODER_TICKS_PER_DEGREE;
    private double minEncoderPosition = MIN_ROTATION_DEGREES * ENCODER_TICKS_PER_DEGREE;
    private double centerEncoderPosition = (MAX_ROTATION_DEGREES / 2.0) * ENCODER_TICKS_PER_DEGREE;

    // ========== PID CONTROL PARAMETERS ==========
    public static double AUTO_TRACK_KP = 0.025;  // Proportional gain - REDUCED to stop oscillation
    public static double AUTO_TRACK_KI = 0.0;    // Integral gain (usually 0)
    public static double AUTO_TRACK_KD = 0.010;  // Derivative gain - INCREASED for damping
    public static double MAX_TURRET_POWER = 0.5; // Maximum motor power - REDUCED for control
    public static double DEADBAND = 2.0;         // Degrees - stop if within this range
    public static double MIN_POWER_THRESHOLD = 0.08; // Minimum power to overcome friction

    // Motor direction correction
    public static boolean INVERT_TURRET_DIRECTION = true; // CHANGED TO TRUE - fixes unidirectional movement

    // PID variables
    private double previousError = 0.0;
    private double integral = 0.0;

    // ========== APRILTAG CONFIGURATION ==========
    public static int PRIMARY_TAG_ID = 20;
    public static int SECONDARY_TAG_ID = 24;
    private int currentTargetTagID = PRIMARY_TAG_ID;

    // ========== CONTROL STATE ==========
    private boolean autoTrackEnabled = false;
    private boolean tagVisible = false;

    // Button state tracking
    private boolean prevCross = false;
    private boolean prevTriangle = false;
    private boolean prevCircle = false;

    @Override
    public void init() {
        // Initialize turret motor
        turretMotor = hardwareMap.get(DcMotorEx.class, "turret");
        turretMotor.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        turretMotor.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        turretMotor.setDirection(DcMotor.Direction.FORWARD); // Change to REVERSE if needed
        turretMotor.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);

        // Initialize Limelight
        limelightManager.init(hardwareMap);

        telemetry.addLine("=================================");
        telemetry.addLine("  AUTO TURRET INITIALIZED");
        telemetry.addLine("=================================");
        telemetry.addLine("Controls:");
        telemetry.addLine("  Cross (X) - Toggle auto-tracking");
        telemetry.addLine("  Triangle - Switch tag (20 ↔ 24)");
        telemetry.addLine("  Circle - Reset to center");
        telemetry.addLine("  Right Stick X - Manual control");
        telemetry.addLine("  D-Pad Up/Down - Fine adjust");
        telemetry.addLine("=================================");
        telemetry.addData("Limelight", limelightManager.isConnected() ? "Connected" : "NOT FOUND");
        telemetry.addData("Target Tag", currentTargetTagID);
        telemetry.addData("Max Rotation", MAX_ROTATION_DEGREES + "°");
        telemetry.update();
    }

    @Override
    public void loop() {
        // Update Limelight data
        limelightManager.update();

        // Check tag visibility (always check, not just when auto-tracking)
        if (limelightManager.isConnected()) {
            tagVisible = limelightManager.isAprilTagVisible(currentTargetTagID);
        } else {
            tagVisible = false;
        }

        // Get current turret position
        double currentEncoderPosition = turretMotor.getCurrentPosition();
        double currentDegrees = currentEncoderPosition / ENCODER_TICKS_PER_DEGREE;

        // ========== BUTTON CONTROLS ==========

        // Toggle auto-tracking with Cross (X)
        if (gamepad1.cross && !prevCross) {
            autoTrackEnabled = !autoTrackEnabled;
            if (autoTrackEnabled) {
                // Reset PID when enabling
                previousError = 0.0;
                integral = 0.0;
            }
        }
        prevCross = gamepad1.cross;

        // Switch target tag with Triangle
        if (gamepad1.triangle && !prevTriangle) {
            if (currentTargetTagID == PRIMARY_TAG_ID) {
                currentTargetTagID = SECONDARY_TAG_ID;
            } else {
                currentTargetTagID = PRIMARY_TAG_ID;
            }
        }
        prevTriangle = gamepad1.triangle;

        // Reset to center with Circle
        if (gamepad1.circle && !prevCircle) {
            resetTurretToCenter();
            autoTrackEnabled = false; // Disable auto-tracking when resetting
        }
        prevCircle = gamepad1.circle;

        // ========== TURRET CONTROL ==========
        double turretPower = 0.0;

        if (autoTrackEnabled) {
            // AUTO-TRACKING MODE
            turretPower = calculateAutoTrackPower();
        } else {
            // MANUAL CONTROL MODE

            // Right stick for continuous manual control
            if (Math.abs(gamepad1.right_stick_x) > 0.05) {
                turretPower = -gamepad1.right_stick_x * 0.4; // Invert if needed
            }

            // D-Pad for fine adjustments
            if (gamepad1.dpad_up) {
                turretPower = 0.15;
            } else if (gamepad1.dpad_down) {
                turretPower = -0.15;
            }

            // Reset PID when in manual mode
            previousError = 0.0;
            integral = 0.0;
        }

        // ========== ENFORCE ROTATION LIMITS ==========
        turretPower = enforceLimits(currentEncoderPosition, turretPower);

        // ========== APPLY POWER TO MOTOR ==========
        turretMotor.setPower(turretPower);

        // ========== TELEMETRY ==========
        updateTelemetry(currentDegrees, turretPower);
    }

    /**
     * Calculate turret power using PID control to track AprilTag
     */
    private double calculateAutoTrackPower() {
        if (!limelightManager.isConnected()) {
            tagVisible = false;
            return 0.0;
        }

        // Check if target tag is visible
        if (limelightManager.isAprilTagVisible(currentTargetTagID)) {
            tagVisible = true;

            // Get horizontal offset (tx) from Limelight
            // tx is in degrees - positive means target is to the right of center
            double tx = limelightManager.getTargetX();

            // Error is how far off we are (want tx to be 0)
            double error = tx;

            // Check if we're within deadband
            if (Math.abs(error) < DEADBAND) {
                // Close enough - stop
                previousError = 0.0;
                integral = 0.0;
                return 0.0;
            }

            // PID calculations
            integral += error;
            double derivative = error - previousError;

            // Calculate power using PID formula
            double power = (AUTO_TRACK_KP * error) +
                          (AUTO_TRACK_KI * integral) +
                          (AUTO_TRACK_KD * derivative);

            // Apply direction inversion if needed
            if (INVERT_TURRET_DIRECTION) {
                power = -power;
            }

            // Clamp power
            power = Math.max(-MAX_TURRET_POWER, Math.min(MAX_TURRET_POWER, power));

            // Add minimum power to overcome friction (prevents jerky slow movements)
            if (Math.abs(power) > 0.01 && Math.abs(power) < MIN_POWER_THRESHOLD) {
                power = Math.signum(power) * MIN_POWER_THRESHOLD;
            }

            // Update for next iteration
            previousError = error;

            return power;

        } else {
            // Tag not visible
            tagVisible = false;
            previousError = 0.0;
            integral = 0.0;
            return 0.0;
        }
    }

    /**
     * Enforce rotation limits to prevent turret from exceeding 270 degrees
     */
    private double enforceLimits(double currentPosition, double requestedPower) {
        // Check if at max limit
        if (currentPosition >= maxEncoderPosition && requestedPower > 0) {
            return 0.0; // Stop - can't go further positive
        }

        // Check if at min limit
        if (currentPosition <= minEncoderPosition && requestedPower < 0) {
            return 0.0; // Stop - can't go further negative
        }

        // Slow down near limits (soft limits)
        double slowdownZone = 10.0 * ENCODER_TICKS_PER_DEGREE; // 10 degrees before limit

        if (currentPosition > (maxEncoderPosition - slowdownZone) && requestedPower > 0) {
            // Near max limit, moving toward it - reduce power
            double reduction = (maxEncoderPosition - currentPosition) / slowdownZone;
            requestedPower *= reduction;
        }

        if (currentPosition < (minEncoderPosition + slowdownZone) && requestedPower < 0) {
            // Near min limit, moving toward it - reduce power
            double reduction = (currentPosition - minEncoderPosition) / slowdownZone;
            requestedPower *= reduction;
        }

        return requestedPower;
    }

    /**
     * Reset turret to center position (135 degrees if max is 270)
     */
    private void resetTurretToCenter() {
        // This is a simple approach - just set power toward center
        // For more precise positioning, you could use RUN_TO_POSITION mode
        turretMotor.setMode(DcMotor.RunMode.RUN_TO_POSITION);
        turretMotor.setTargetPosition((int)centerEncoderPosition);
        turretMotor.setPower(0.3);

        // Wait briefly for movement to start
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            // Ignore
        }

        // Switch back to RUN_USING_ENCODER after a moment
        // (This will happen on next loop when mode is checked)
    }

    /**
     * Update telemetry display
     */
    private void updateTelemetry(double currentDegrees, double turretPower) {
        telemetry.addLine("=================================");
        telemetry.addLine("  AUTO TURRET STATUS");
        telemetry.addLine("=================================");
        telemetry.addData("Mode", autoTrackEnabled ? "AUTO-TRACKING" : "MANUAL");
        telemetry.addLine("---------------------------------");
        telemetry.addData("Target Tag ID", currentTargetTagID);
        telemetry.addData("Tag Visible", tagVisible ? "YES ✓" : "NO ✗");
        telemetry.addData("Limelight", limelightManager.isConnected() ? "Connected" : "Disconnected");
        telemetry.addData("Total Tags Seen", limelightManager.getVisibleTagCount());
        telemetry.addData("Has Valid Target", limelightManager.hasValidTarget() ? "Yes" : "No");
        telemetry.addLine("---------------------------------");
        telemetry.addData("Turret Position", "%.1f°", currentDegrees);
        telemetry.addData("Encoder Ticks", turretMotor.getCurrentPosition());
        telemetry.addData("Motor Power", "%.3f", turretPower);

        // Show direction indicator
        String direction = "";
        if (turretPower > 0.01) {
            direction = "→→→ (Positive)";
        } else if (turretPower < -0.01) {
            direction = "←←← (Negative)";
        } else {
            direction = "■ (Stopped)";
        }
        telemetry.addData("Direction", direction);
        telemetry.addLine("---------------------------------");

        if (autoTrackEnabled && tagVisible) {
            double tx = limelightManager.getTargetX();
            telemetry.addData("Horizontal Offset (tx)", "%.2f°", tx);

            // Show what tx means
            String txMeaning = "";
            if (tx > 1.0) {
                txMeaning = "Tag is RIGHT of center";
            } else if (tx < -1.0) {
                txMeaning = "Tag is LEFT of center";
            } else {
                txMeaning = "Tag is CENTERED";
            }
            telemetry.addData("tx Meaning", txMeaning);
            telemetry.addData("Error", "%.2f°", tx);
            telemetry.addData("Status", Math.abs(tx) < DEADBAND ? "LOCKED ON ✓" : "TRACKING...");
        } else if (autoTrackEnabled && !tagVisible) {
            telemetry.addLine("⚠️ AUTO-TRACKING ENABLED");
            telemetry.addLine("   but tag not visible!");
        }

        telemetry.addLine("---------------------------------");
        telemetry.addData("Rotation Range", "%.0f° - %.0f°", MIN_ROTATION_DEGREES, MAX_ROTATION_DEGREES);

        // Warning if near limits
        if (currentDegrees > MAX_ROTATION_DEGREES - 20) {
            telemetry.addLine("⚠️ WARNING: Near MAX limit!");
        }
        if (currentDegrees < MIN_ROTATION_DEGREES + 20) {
            telemetry.addLine("⚠️ WARNING: Near MIN limit!");
        }

        telemetry.addLine("=================================");
        telemetry.update();
    }

    @Override
    public void stop() {
        // Stop turret motor
        if (turretMotor != null) {
            turretMotor.setPower(0.0);
        }

        // Stop Limelight
        if (limelightManager != null) {
            limelightManager.stop();
        }
    }
}

