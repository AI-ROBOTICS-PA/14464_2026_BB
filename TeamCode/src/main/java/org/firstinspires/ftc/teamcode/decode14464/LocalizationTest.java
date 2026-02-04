package org.firstinspires.ftc.teamcode.decode14464;

import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.pedropathing.follower.Follower;
import com.pedropathing.geometry.Pose;
import org.firstinspires.ftc.teamcode.pedroPathing.Constants;
import org.firstinspires.ftc.teamcode.decode14464.systems.RobotMotors;

/**
 * LocalizationTest - TeleOp program to test Pedro Pathing localization
 *
 * COORDINATE SYSTEM (Per Pedro Pathing Documentation):
 * - Origin (0,0) is at BOTTOM-LEFT corner of field
 * - X-axis: Horizontal (0 to 144 inches, left to right)
 * - Y-axis: Vertical (0 to 144 inches, bottom to top)
 * - Heading 0° points UP (positive Y direction)
 * - Heading increases COUNTER-CLOCKWISE: 0°=Up, 90°=Left, 180°=Down, 270°=Right
 *
 * Features:
 * 1. Real-time position tracking (X, Y coordinates in inches)
 * 2. Real-time heading/angle tracking (in degrees)
 * 3. Field position visualization with quadrant detection
 * 4. Direction indicator based on Pedro Pathing coordinate system
 * 5. Distance from field center
 * 6. Visual field map representation
 *
 * Controls:
 * - Left Stick Y: Forward/Backward
 * - Left Stick X: Strafe Left/Right
 * - Right Stick X: Rotate
 * - DPAD UP: Reset position to origin (0, 0, 0°)
 * - DPAD DOWN: Set position to field center (72, 72, 0°)
 * - DPAD LEFT: Set to Blue Alliance corner (0, 72, 0°)
 * - DPAD RIGHT: Set to Red Alliance corner (144, 72, 0°)
 * - X/Square: Toggle field-centric vs robot-centric drive
 */
@TeleOp(name = "Localization Test", group = "Testing")
public class LocalizationTest extends OpMode {

    private Follower follower;
    private RobotMotors robotMotors = new RobotMotors();
    private boolean fieldCentric = true;
    private boolean prevDpadUp = false;
    private boolean prevDpadDown = false;
    private boolean prevDpadLeft = false;
    private boolean prevDpadRight = false;
    private boolean prevX = false;
    private boolean autoTurretEnabled = true; // Default: Auto-turret ON
    private boolean prevCircle = false;

    // Field dimensions (FTC field is 144" x 144")
    private static final double FIELD_WIDTH = 144.0;  // inches
    private static final double FIELD_HEIGHT = 144.0; // inches

    // Target position for turret to always point at (top-left corner)
    private static final double TARGET_X = 1.0;   // 1 inch from left edge
    private static final double TARGET_Y = 144.0; // Top edge

    // Starting positions for alliances (Pedro Pathing coordinate system)
    // Origin is at bottom-left, 0° heading points UP (positive Y)
    private static final Pose ORIGIN = new Pose(0, 0, Math.toRadians(0));
    private static final Pose FIELD_CENTER = new Pose(72, 72, Math.toRadians(0));
    private static final Pose BLUE_ALLIANCE = new Pose(0, 72, Math.toRadians(0));  // Left side, middle
    private static final Pose RED_ALLIANCE = new Pose(144, 72, Math.toRadians(0)); // Right side, middle

    @Override
    public void init() {
        // Initialize Pedro Pathing follower
        follower = Constants.createFollower(hardwareMap);

        // Initialize robot motors (for turret)
        robotMotors.init(hardwareMap);

        // Set starting pose to origin (bottom-left corner)
        follower.setStartingPose(new Pose(0, 0, 0));

        telemetry.addLine("╔════════════════════════════════╗");
        telemetry.addLine("║   LOCALIZATION TEST READY      ║");
        telemetry.addLine("╚════════════════════════════════╝");
        telemetry.addLine();
        telemetry.addLine("📐 PEDRO PATHING COORDINATES:");
        telemetry.addLine("  Origin (0,0) = Bottom-Left");
        telemetry.addLine("  0° Heading = UP ↑");
        telemetry.addLine("  90° = LEFT ←, 270° = RIGHT →");
        telemetry.addLine();
        telemetry.addLine("🎯 AUTO-TURRET:");
        telemetry.addLine("  Target: (1, 144) - Top-Left");
        telemetry.addLine("  Circle/B: Toggle auto-turret");
        telemetry.addLine();
        telemetry.addLine("Controls:");
        telemetry.addLine("  Left Stick: Drive");
        telemetry.addLine("  Right Stick X: Rotate");
        telemetry.addLine("  DPAD UP: Reset to origin");
        telemetry.addLine("  DPAD DOWN: Go to center");
        telemetry.addLine("  DPAD LEFT: Blue alliance");
        telemetry.addLine("  DPAD RIGHT: Red alliance");
        telemetry.addLine("  X/Square: Toggle drive mode");
        telemetry.addLine();
        telemetry.addData("Drive Mode", fieldCentric ? "FIELD-CENTRIC" : "ROBOT-CENTRIC");
        telemetry.addData("Auto-Turret", "ENABLED");
        telemetry.update();
    }

    @Override
    public void start() {
        follower.startTeleopDrive();
    }

    @Override
    public void loop() {
        // Update follower (this updates localization)
        follower.update();

        // Get current pose
        Pose currentPose = follower.getPose();
        double x = currentPose.getX();
        double y = currentPose.getY();
        double headingRadians = currentPose.getHeading();
        double headingDegrees = Math.toDegrees(headingRadians);

        // Normalize heading to 0-360 range
        while (headingDegrees < 0) headingDegrees += 360;
        while (headingDegrees >= 360) headingDegrees -= 360;

        // Handle position reset controls
        handlePositionResets();

        // Handle drive mode toggle
        if ((gamepad1.x || gamepad1.square) && !prevX) {
            fieldCentric = !fieldCentric;
        }
        prevX = gamepad1.x || gamepad1.square;

        // Toggle auto-turret with Circle/B button
        if ((gamepad1.circle || gamepad1.b) && !prevCircle) {
            autoTurretEnabled = !autoTurretEnabled;
        }
        prevCircle = gamepad1.circle || gamepad1.b;

        // ========== AUTO-TURRET CALCULATION ==========
        // Calculate angle from robot to target (1, 144)
        double dx = TARGET_X - x;
        double dy = TARGET_Y - y;

        // Calculate absolute angle to target in field coordinates (atan2 gives angle from positive X-axis)
        // We need to adjust for Pedro Pathing where 0° = UP (positive Y)
        double angleToTargetRad = Math.atan2(dx, dy); // Note: dx, dy order for Pedro Pathing (0° = UP)
        double angleToTargetDeg = Math.toDegrees(angleToTargetRad);

        // Normalize to 0-360 range
        while (angleToTargetDeg < 0) angleToTargetDeg += 360;
        while (angleToTargetDeg >= 360) angleToTargetDeg -= 360;

        // Calculate turret angle relative to robot heading
        // Turret needs to compensate for robot's heading
        double turretAngleDeg = angleToTargetDeg - headingDegrees;

        // Normalize to -180 to 180 range for shortest rotation
        while (turretAngleDeg > 180) turretAngleDeg -= 360;
        while (turretAngleDeg <= -180) turretAngleDeg += 360;

        // Calculate distance to target
        double distanceToTarget = Math.sqrt(dx * dx + dy * dy);

        // Apply turret control if enabled
        if (autoTurretEnabled && robotMotors.turret != null) {
            // Simple proportional control for turret
            double kp = 0.015; // Tuning constant
            double turretPower = turretAngleDeg * kp;

            // Clamp power
            turretPower = Math.max(-0.5, Math.min(0.5, turretPower));

            // Apply deadband
            if (Math.abs(turretAngleDeg) < 2.0) {
                turretPower = 0;
            }

            robotMotors.turret.setPower(turretPower);
        } else if (robotMotors.turret != null) {
            robotMotors.turret.setPower(0);
        }

        // Drive control
        follower.setTeleOpDrive(
            -gamepad1.left_stick_y,
            -gamepad1.left_stick_x,
            -gamepad1.right_stick_x,
            !fieldCentric  // Robot-centric when fieldCentric is false
        );

        // ========== TELEMETRY OUTPUT ==========

        telemetry.addLine("╔════════════════════════════════════════╗");
        telemetry.addLine("║      ROBOT LOCALIZATION STATUS         ║");
        telemetry.addLine("╚════════════════════════════════════════╝");
        telemetry.addLine();

        // === POSITION DATA ===
        telemetry.addLine("━━━ POSITION ━━━");
        telemetry.addData("X Position", "%.2f inches", x);
        telemetry.addData("Y Position", "%.2f inches", y);
        telemetry.addData("Coordinates", "(%.1f, %.1f)", x, y);
        telemetry.addLine();

        // === HEADING/ANGLE DATA ===
        telemetry.addLine("━━━ HEADING/ORIENTATION ━━━");
        telemetry.addData("Heading", "%.1f°", headingDegrees);
        telemetry.addData("Radians", "%.3f rad", headingRadians);
        telemetry.addData("Direction", getCompassDirection(headingDegrees));
        telemetry.addData("Arrow", getDirectionArrow(headingDegrees));
        telemetry.addLine();

        // === AUTO-TURRET INFO ===
        telemetry.addLine("━━━ AUTO-TURRET ━━━");
        telemetry.addData("Status", autoTurretEnabled ? "🎯 ACTIVE" : "⭕ DISABLED");
        telemetry.addData("Target Position", "(%.0f, %.0f)", TARGET_X, TARGET_Y);
        telemetry.addData("Distance to Target", "%.1f inches", distanceToTarget);
        telemetry.addData("Field Angle to Target", "%.1f°", angleToTargetDeg);
        telemetry.addData("Turret Angle (Relative)", "%.1f°", turretAngleDeg);
        telemetry.addData("Turret Direction", getTurretDirection(turretAngleDeg));
        telemetry.addLine();

        // === FIELD LOCATION ===
        telemetry.addLine("━━━ FIELD LOCATION ━━━");
        telemetry.addData("Quadrant", getFieldQuadrant(x, y));
        telemetry.addData("Distance from Center", "%.1f inches", getDistanceFromCenter(x, y));
        telemetry.addData("Field Position", getFieldPositionDescription(x, y));
        telemetry.addLine();

        // === VISUAL FIELD MAP ===
        telemetry.addLine("━━━ FIELD MAP (Top View) ━━━");
        telemetry.addLine(getFieldMap(x, y, headingDegrees));
        telemetry.addLine();

        // === DRIVE MODE ===
        telemetry.addLine("━━━ CONTROLS ━━━");
        telemetry.addData("Drive Mode", fieldCentric ? "🌍 FIELD-CENTRIC" : "🤖 ROBOT-CENTRIC");
        telemetry.addData("Gamepad Input", "LY:%.2f LX:%.2f RX:%.2f",
            -gamepad1.left_stick_y, -gamepad1.left_stick_x, -gamepad1.right_stick_x);

        telemetry.update();
    }

    /**
     * Handle position reset button presses
     */
    private void handlePositionResets() {
        // Reset to origin (0, 0, 0°) - Bottom-left corner
        if (gamepad1.dpad_up && !prevDpadUp) {
            follower.setPose(ORIGIN);
            telemetry.addLine("✅ Position reset to ORIGIN (0, 0, 0°)");
        }
        prevDpadUp = gamepad1.dpad_up;

        // Set to field center (72, 72, 0°)
        if (gamepad1.dpad_down && !prevDpadDown) {
            follower.setPose(FIELD_CENTER);
            telemetry.addLine("✅ Position set to FIELD CENTER (72, 72, 0°)");
        }
        prevDpadDown = gamepad1.dpad_down;

        // Set to Blue Alliance position (left side, middle)
        if (gamepad1.dpad_left && !prevDpadLeft) {
            follower.setPose(BLUE_ALLIANCE);
            telemetry.addLine("✅ Position set to BLUE ALLIANCE (0, 72, 0°)");
        }
        prevDpadLeft = gamepad1.dpad_left;

        // Set to Red Alliance position (right side, middle)
        if (gamepad1.dpad_right && !prevDpadRight) {
            follower.setPose(RED_ALLIANCE);
            telemetry.addLine("✅ Position set to RED ALLIANCE (144, 72, 0°)");
        }
        prevDpadRight = gamepad1.dpad_right;
    }

    /**
     * Get compass direction from heading angle
     * Pedro Pathing: 0° = UP, 90° = LEFT, 180° = DOWN, 270° = RIGHT (counter-clockwise)
     */
    private String getCompassDirection(double degrees) {
        if (degrees >= 337.5 || degrees < 22.5) return "UP ↑ (0°)";
        if (degrees >= 22.5 && degrees < 67.5) return "UP-LEFT ↖ (45°)";
        if (degrees >= 67.5 && degrees < 112.5) return "LEFT ← (90°)";
        if (degrees >= 112.5 && degrees < 157.5) return "DOWN-LEFT ↙ (135°)";
        if (degrees >= 157.5 && degrees < 202.5) return "DOWN ↓ (180°)";
        if (degrees >= 202.5 && degrees < 247.5) return "DOWN-RIGHT ↘ (225°)";
        if (degrees >= 247.5 && degrees < 292.5) return "RIGHT → (270°)";
        return "UP-RIGHT ↗ (315°)";
    }

    /**
     * Get directional arrow for heading
     * Pedro Pathing: 0° = UP, counter-clockwise rotation
     */
    private String getDirectionArrow(double degrees) {
        if (degrees >= 337.5 || degrees < 22.5) return "↑";
        if (degrees >= 22.5 && degrees < 67.5) return "↖";
        if (degrees >= 67.5 && degrees < 112.5) return "←";
        if (degrees >= 112.5 && degrees < 157.5) return "↙";
        if (degrees >= 157.5 && degrees < 202.5) return "↓";
        if (degrees >= 202.5 && degrees < 247.5) return "↘";
        if (degrees >= 247.5 && degrees < 292.5) return "→";
        return "↗";
    }

    /**
     * Get field quadrant (1-4) based on position
     * Pedro Pathing: Origin (0,0) at bottom-left
     * Quadrant 1: Top-Right (X > 72, Y > 72)
     * Quadrant 2: Top-Left (X < 72, Y > 72)
     * Quadrant 3: Bottom-Left (X < 72, Y < 72)
     * Quadrant 4: Bottom-Right (X > 72, Y < 72)
     */
    private String getFieldQuadrant(double x, double y) {
        double centerX = FIELD_WIDTH / 2;  // 72
        double centerY = FIELD_HEIGHT / 2; // 72

        if (x >= centerX && y >= centerY) return "Q1 (Top-Right)";
        if (x < centerX && y >= centerY) return "Q2 (Top-Left)";
        if (x < centerX && y < centerY) return "Q3 (Bottom-Left - Origin)";
        return "Q4 (Bottom-Right)";
    }

    /**
     * Calculate distance from field center
     */
    private double getDistanceFromCenter(double x, double y) {
        double centerX = FIELD_WIDTH / 2;
        double centerY = FIELD_HEIGHT / 2;
        double dx = x - centerX;
        double dy = y - centerY;
        return Math.sqrt(dx * dx + dy * dy);
    }

    /**
     * Get descriptive position on field
     * Pedro Pathing: (0,0) is bottom-left, (144,144) is top-right
     */
    private String getFieldPositionDescription(double x, double y) {
        double centerX = FIELD_WIDTH / 2;  // 72
        double centerY = FIELD_HEIGHT / 2; // 72

        // Check if near center
        if (Math.abs(x - centerX) < 12 && Math.abs(y - centerY) < 12) {
            return "FIELD CENTER";
        }

        // Check if near corners
        if (x < 24 && y < 24) return "BOTTOM-LEFT CORNER (Origin)";
        if (x > 120 && y < 24) return "BOTTOM-RIGHT CORNER";
        if (x < 24 && y > 120) return "TOP-LEFT CORNER";
        if (x > 120 && y > 120) return "TOP-RIGHT CORNER";

        // Check edges
        if (x < 24) return "LEFT EDGE (Blue Alliance)";
        if (x > 120) return "RIGHT EDGE (Red Alliance)";
        if (y < 24) return "BOTTOM EDGE";
        if (y > 120) return "TOP EDGE";

        return "FIELD INTERIOR";
    }

    /**
     * Generate ASCII field map showing robot position
     * Field is 144" x 144", scaled to fit telemetry
     */
    private String getFieldMap(double x, double y, double heading) {
        StringBuilder map = new StringBuilder();

        // Map dimensions (characters)
        int mapWidth = 25;
        int mapHeight = 9;

        // Calculate robot position on map
        int robotMapX = (int) ((x / FIELD_WIDTH) * (mapWidth - 2)) + 1;
        int robotMapY = mapHeight - 1 - (int) ((y / FIELD_HEIGHT) * (mapHeight - 2));

        // Clamp to map bounds
        robotMapX = Math.max(1, Math.min(mapWidth - 2, robotMapX));
        robotMapY = Math.max(1, Math.min(mapHeight - 2, robotMapY));

        // Get robot direction symbol
        String robotSymbol = getDirectionArrow(heading);

        // Build map
        for (int row = 0; row < mapHeight; row++) {
            for (int col = 0; col < mapWidth; col++) {
                // Draw borders
                if (row == 0 || row == mapHeight - 1) {
                    map.append(col == 0 || col == mapWidth - 1 ? "+" : "-");
                } else if (col == 0 || col == mapWidth - 1) {
                    map.append("|");
                }
                // Draw center point
                else if (row == mapHeight / 2 && col == mapWidth / 2) {
                    if (row == robotMapY && col == robotMapX) {
                        map.append(robotSymbol);
                    } else {
                        map.append("⊕"); // Center marker
                    }
                }
                // Draw robot
                else if (row == robotMapY && col == robotMapX) {
                    map.append(robotSymbol);
                }
                // Empty space
                else {
                    map.append(" ");
                }
            }
            if (row < mapHeight - 1) map.append("\n");
        }

        // Add legend
        map.append("\n");
        map.append(String.format("%s = Robot  ⊕ = Center", robotSymbol));

        return map.toString();
    }

    /**
     * Get turret direction description from relative angle
     * @param turretAngle Angle in degrees relative to robot heading (-180 to 180)
     * @return Description of which way turret should rotate
     */
    private String getTurretDirection(double turretAngle) {
        if (Math.abs(turretAngle) < 2.0) {
            return "✓ ON TARGET";
        } else if (turretAngle > 0) {
            return String.format("← Turn LEFT %.1f°", turretAngle);
        } else {
            return String.format("→ Turn RIGHT %.1f°", Math.abs(turretAngle));
        }
    }

    @Override
    public void stop() {
        // Stop turret motor
        if (robotMotors.turret != null) {
            robotMotors.turret.setPower(0);
        }

        // Stop all motors when program ends
        if (follower != null) {
            follower.breakFollowing();
        }
    }
}

