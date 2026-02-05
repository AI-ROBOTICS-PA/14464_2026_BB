package org.firstinspires.ftc.teamcode.decode14464;

import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.hardware.Servo;

import org.firstinspires.ftc.teamcode.decode14464.systems.LimelightManager;
import org.firstinspires.ftc.teamcode.decode14464.systems.RobotConstants;

/**
 * HoodHeightTuner - Tune hood servo position based on distance from AprilTag
 *
 * Features:
 * - Automatically adjusts hood angle based on distance to AprilTag
 * - Manual override with gamepad controls
 * - Real-time tuning of distance mapping parameters
 * - Tracks AprilTag ID 20 or 24
 *
 * Controls:
 * - Cross (X): Enable/disable auto-adjustment
 * - Triangle: Switch between tag 20 and 24
 * - Circle: Reset hood to home position
 * - Square: Change step size for tuning
 * - Right Stick Y: Manual hood control
 * - D-Pad Up/Down: Fine tune position
 *
 * Tuning Controls (Gamepad 2):
 * - D-Pad Up/Down: Adjust min distance
 * - D-Pad Left/Right: Adjust max distance
 * - Left Bumper/Right Bumper: Adjust servo position offset
 */
// @TeleOp(name = "Hood Height Tuner", group = "Tuning")  // DISABLED
public class HoodHeightTuner extends OpMode {

    // Hardware
    private Servo hoodServo;
    private LimelightManager limelightManager = new LimelightManager();

    // ========== CONFIGURATION FROM CONSTANTS ==========
    private double hoodMinPosition = RobotConstants.Hood.MIN_POSITION;        // 0.28
    private double hoodMaxPosition = RobotConstants.Hood.MAX_POSITION;        // 0.82
    private double minDistance = RobotConstants.Hood.MIN_DISTANCE_INCHES;     // 20.0 inches
    private double maxDistance = RobotConstants.Hood.MAX_DISTANCE_INCHES;     // 100.0 inches
    private int currentTargetTagID = RobotConstants.Turret.PRIMARY_TAG_ID;    // 20

    // ========== TUNING CONTROLS ==========
    // Step sizes for dynamic tuning
    private double[] stepSizes = {10.0, 5.0, 1.0, 0.5, 0.1};
    private int stepIndex = 2; // Start with 1.0 step size

    // Servo position offset for fine tuning
    private double positionOffset = 0.0;

    // ========== CONTROL STATE ==========
    private boolean autoAdjustEnabled = false;
    private boolean tagVisible = false;
    private double currentDistance = 0.0;
    private double calculatedPosition = RobotConstants.Hood.HOME_POSITION;

    // Button state tracking
    private boolean prevCross = false;
    private boolean prevTriangle = false;
    private boolean prevCircle = false;
    private boolean prevSquare = false;
    private boolean prevDpadUp = false;
    private boolean prevDpadDown = false;
    private boolean prevDpadLeft = false;
    private boolean prevDpadRight = false;

    @Override
    public void init() {
        // Initialize hood servo
        hoodServo = hardwareMap.get(Servo.class, RobotConstants.Hood.SERVO_NAME);
        hoodServo.setPosition(RobotConstants.Hood.HOME_POSITION);

        // Initialize Limelight
        limelightManager.init(hardwareMap);

        telemetry.addLine("=================================");
        telemetry.addLine("  HOOD HEIGHT TUNER INITIALIZED");
        telemetry.addLine("=================================");
        telemetry.addLine("Controls:");
        telemetry.addLine("  Cross (X) - Toggle auto-adjust");
        telemetry.addLine("  Triangle - Switch tag (20 ↔ 24)");
        telemetry.addLine("  Circle - Reset to home position");
        telemetry.addLine("  Square - Change step size");
        telemetry.addLine("  Right Stick Y - Manual control");
        telemetry.addLine("");
        telemetry.addLine("TUNING (Gamepad 2):");
        telemetry.addLine("  D-Pad Up/Down - Min Distance");
        telemetry.addLine("  D-Pad Left/Right - Max Distance");
        telemetry.addLine("  Left/Right Bumper - Position Offset");
        telemetry.addLine("=================================");
        telemetry.addData("Limelight", limelightManager.isConnected() ? "Connected" : "NOT FOUND");
        telemetry.addData("Target Tag", currentTargetTagID);
        telemetry.addData("Servo Range", "%.2f to %.2f", hoodMinPosition, hoodMaxPosition);
        telemetry.addData("Distance Range", "%.0f to %.0f inches", minDistance, maxDistance);
        telemetry.update();
    }

    @Override
    public void loop() {
        // Update Limelight data
        limelightManager.update();

        // Check tag visibility and get distance
        if (limelightManager.isConnected()) {
            tagVisible = limelightManager.isAprilTagVisible(currentTargetTagID);
            if (tagVisible) {
                currentDistance = limelightManager.getDistanceToTag(currentTargetTagID);
            } else {
                currentDistance = 0.0;
            }
        } else {
            tagVisible = false;
            currentDistance = 0.0;
        }

        // ========== BUTTON CONTROLS ==========

        // Toggle auto-adjustment with Cross (X)
        if (gamepad1.cross && !prevCross) {
            autoAdjustEnabled = !autoAdjustEnabled;
        }
        prevCross = gamepad1.cross;

        // Switch target tag with Triangle (now uses alliance toggle)
        if (gamepad1.triangle && !prevTriangle) {
            limelightManager.toggleAlliance();
        }
        prevTriangle = gamepad1.triangle;

        // Reset to home with Circle
        if (gamepad1.circle && !prevCircle) {
            hoodServo.setPosition(RobotConstants.Hood.HOME_POSITION);
            autoAdjustEnabled = false;
        }
        prevCircle = gamepad1.circle;

        // Change step size with Square
        if (gamepad1.square && !prevSquare) {
            stepIndex = (stepIndex + 1) % stepSizes.length;
        }
        prevSquare = gamepad1.square;

        // ========== TUNING CONTROLS (GAMEPAD 2) ==========

        // Adjust min distance with D-Pad Up/Down
        if (gamepad2.dpad_up && !prevDpadUp) {
            minDistance += stepSizes[stepIndex];
        }
        prevDpadUp = gamepad2.dpad_up;

        if (gamepad2.dpad_down && !prevDpadDown) {
            minDistance -= stepSizes[stepIndex];
            if (minDistance < 5.0) minDistance = 5.0; // Don't go below 5 inches
        }
        prevDpadDown = gamepad2.dpad_down;

        // Adjust max distance with D-Pad Left/Right
        if (gamepad2.dpad_right && !prevDpadRight) {
            maxDistance += stepSizes[stepIndex];
        }
        prevDpadRight = gamepad2.dpad_right;

        if (gamepad2.dpad_left && !prevDpadLeft) {
            maxDistance -= stepSizes[stepIndex];
            if (maxDistance < minDistance + 10.0) {
                maxDistance = minDistance + 10.0; // Keep at least 10 inch range
            }
        }
        prevDpadLeft = gamepad2.dpad_left;

        // Adjust position offset with Bumpers
        if (gamepad2.right_bumper) {
            positionOffset += 0.01;
            if (positionOffset > 0.20) positionOffset = 0.20; // Max offset
        }
        if (gamepad2.left_bumper) {
            positionOffset -= 0.01;
            if (positionOffset < -0.20) positionOffset = -0.20; // Max offset
        }

        // ========== HOOD CONTROL ==========
        double targetPosition;

        if (autoAdjustEnabled && tagVisible) {
            // AUTO-ADJUSTMENT MODE
            targetPosition = calculateHoodPosition(currentDistance);
            targetPosition += positionOffset; // Apply offset
        } else {
            // MANUAL CONTROL MODE

            // Get current position
            targetPosition = hoodServo.getPosition();

            // Right stick for continuous manual control
            if (Math.abs(gamepad1.right_stick_y) > 0.05) {
                targetPosition -= gamepad1.right_stick_y * 0.01; // Invert if needed
            }

            // D-Pad for fine adjustments
            if (gamepad1.dpad_up) {
                targetPosition += 0.01;
            } else if (gamepad1.dpad_down) {
                targetPosition -= 0.01;
            }
        }

        // Clamp position to valid range
        targetPosition = Math.max(hoodMinPosition, Math.min(hoodMaxPosition, targetPosition));
        calculatedPosition = targetPosition;

        // Apply position to servo
        hoodServo.setPosition(targetPosition);

        // ========== TELEMETRY ==========
        updateTelemetry();
    }

    /**
     * Calculate hood servo position based on distance to AprilTag
     */
    private double calculateHoodPosition(double distance) {
        // Map distance to servo position
        // Closer distance -> higher position (0.80)
        // Farther distance -> lower position (0.28)

        // Clamp distance to range
        distance = Math.max(minDistance, Math.min(maxDistance, distance));

        // Linear interpolation
        double t = (distance - minDistance) / (maxDistance - minDistance);

        // Map to servo position
        if (RobotConstants.Hood.INVERT_DISTANCE_MAPPING) {
            // Closer = lower position, farther = higher position
            return hoodMinPosition + (hoodMaxPosition - hoodMinPosition) * t;
        } else {
            // Closer = higher position, farther = lower position
            return hoodMaxPosition - (hoodMaxPosition - hoodMinPosition) * t;
        }
    }

    /**
     * Update telemetry display
     */
    private void updateTelemetry() {
        telemetry.addLine("=================================");
        telemetry.addLine("  HOOD HEIGHT TUNER STATUS");
        telemetry.addLine("=================================");
        telemetry.addData("Mode", autoAdjustEnabled ? "AUTO-ADJUST" : "MANUAL");
        telemetry.addLine("---------------------------------");
        telemetry.addData("Target Tag ID", currentTargetTagID);
        telemetry.addData("Tag Visible", tagVisible ? "YES ✓" : "NO ✗");
        telemetry.addData("Limelight", limelightManager.isConnected() ? "Connected" : "Disconnected");

        if (tagVisible) {
            telemetry.addData("Distance to Tag", "%.1f inches", currentDistance);
        } else {
            telemetry.addData("Distance to Tag", "N/A");
        }

        telemetry.addLine("---------------------------------");
        telemetry.addData("Current Position", "%.3f", hoodServo.getPosition());
        telemetry.addData("Calculated Position", "%.3f", calculatedPosition);
        telemetry.addData("Position Offset", "%.3f", positionOffset);

        // Show position in percentage
        double currentPos = hoodServo.getPosition();
        double percentage = ((currentPos - hoodMinPosition) / (hoodMaxPosition - hoodMinPosition)) * 100.0;
        telemetry.addData("Position %", "%.1f%%", percentage);

        telemetry.addLine("---------------------------------");
        telemetry.addLine("TUNING PARAMETERS:");
        telemetry.addData("Min Distance", "%.1f inches", minDistance);
        telemetry.addData("Max Distance", "%.1f inches", maxDistance);
        telemetry.addData("Servo Min Pos", "%.3f", hoodMinPosition);
        telemetry.addData("Servo Max Pos", "%.3f", hoodMaxPosition);
        telemetry.addData("Step Size", "%.1f", stepSizes[stepIndex]);

        telemetry.addLine("---------------------------------");
        telemetry.addLine("MAPPING (at current distance):");
        if (tagVisible && currentDistance > 0) {
            double mappedPos = calculateHoodPosition(currentDistance);
            telemetry.addData("Distance", "%.1f in → Position %.3f", currentDistance, mappedPos);

            // Show what angle range this represents
            String range = "";
            if (mappedPos > 0.65) {
                range = "LOW angle (close shot)";
            } else if (mappedPos > 0.45) {
                range = "MID angle";
            } else {
                range = "HIGH angle (far shot)";
            }
            telemetry.addData("Hood Angle", range);
        } else {
            telemetry.addData("No tag visible", "Move tag into view");
        }

        telemetry.update();
    }

    @Override
    public void stop() {
        // Return to home position
        hoodServo.setPosition(RobotConstants.Hood.HOME_POSITION);
        limelightManager.stop();
    }
}

