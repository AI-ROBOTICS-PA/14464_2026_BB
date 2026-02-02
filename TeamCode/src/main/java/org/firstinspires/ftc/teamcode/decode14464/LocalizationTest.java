package org.firstinspires.ftc.teamcode.decode14464;

import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.pedropathing.follower.Follower;
import com.pedropathing.geometry.Pose;
import org.firstinspires.ftc.teamcode.pedroPathing.Constants;

/**
 * LocalizationTest - TeleOp program to test Pedro Pathing localization
 *
 * Features:
 * 1. Real-time position tracking (X, Y coordinates in inches)
 * 2. Real-time heading/angle tracking (in degrees)
 * 3. Field position visualization with quadrant detection
 * 4. Direction indicator (North, South, East, West, etc.)
 * 5. Distance from field center
 * 6. Visual field map representation
 *
 * Controls:
 * - Left Stick Y: Forward/Backward
 * - Left Stick X: Strafe Left/Right
 * - Right Stick X: Rotate
 * - DPAD UP: Reset position to (0, 0, 0°)
 * - DPAD DOWN: Set position to field center
 * - DPAD LEFT: Set to Blue Alliance starting position
 * - DPAD RIGHT: Set to Red Alliance starting position
 * - X/Square: Toggle field-centric vs robot-centric drive
 */
@TeleOp(name = "Localization Test", group = "Testing")
public class LocalizationTest extends OpMode {

    private Follower follower;
    private boolean fieldCentric = true;
    private boolean prevDpadUp = false;
    private boolean prevDpadDown = false;
    private boolean prevDpadLeft = false;
    private boolean prevDpadRight = false;
    private boolean prevX = false;

    // Field dimensions (FTC field is 144" x 144")
    private static final double FIELD_WIDTH = 144.0;  // inches
    private static final double FIELD_HEIGHT = 144.0; // inches

    // Starting positions for alliances
    private static final Pose BLUE_START = new Pose(0, 0, Math.toRadians(0));
    private static final Pose RED_START = new Pose(144, 0, Math.toRadians(180));
    private static final Pose FIELD_CENTER = new Pose(72, 72, Math.toRadians(0));

    @Override
    public void init() {
        // Initialize Pedro Pathing follower
        follower = Constants.createFollower(hardwareMap);

        // Set starting pose to origin
        follower.setStartingPose(new Pose(0, 0, 0));

        telemetry.addLine("╔════════════════════════════════╗");
        telemetry.addLine("║   LOCALIZATION TEST READY      ║");
        telemetry.addLine("╚════════════════════════════════╝");
        telemetry.addLine();
        telemetry.addLine("Controls:");
        telemetry.addLine("  Left Stick: Drive");
        telemetry.addLine("  Right Stick X: Rotate");
        telemetry.addLine("  DPAD UP: Reset to origin");
        telemetry.addLine("  DPAD DOWN: Go to center");
        telemetry.addLine("  DPAD LEFT: Blue start");
        telemetry.addLine("  DPAD RIGHT: Red start");
        telemetry.addLine("  X/Square: Toggle drive mode");
        telemetry.addLine();
        telemetry.addData("Drive Mode", fieldCentric ? "FIELD-CENTRIC" : "ROBOT-CENTRIC");
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
        // Reset to origin (0, 0, 0°)
        if (gamepad1.dpad_up && !prevDpadUp) {
            follower.setPose(new Pose(0, 0, 0));
            telemetry.addLine("✅ Position reset to ORIGIN");
        }
        prevDpadUp = gamepad1.dpad_up;

        // Set to field center
        if (gamepad1.dpad_down && !prevDpadDown) {
            follower.setPose(FIELD_CENTER);
            telemetry.addLine("✅ Position set to FIELD CENTER");
        }
        prevDpadDown = gamepad1.dpad_down;

        // Set to Blue Alliance start
        if (gamepad1.dpad_left && !prevDpadLeft) {
            follower.setPose(BLUE_START);
            telemetry.addLine("✅ Position set to BLUE START");
        }
        prevDpadLeft = gamepad1.dpad_left;

        // Set to Red Alliance start
        if (gamepad1.dpad_right && !prevDpadRight) {
            follower.setPose(RED_START);
            telemetry.addLine("✅ Position set to RED START");
        }
        prevDpadRight = gamepad1.dpad_right;
    }

    /**
     * Get compass direction from heading angle
     */
    private String getCompassDirection(double degrees) {
        if (degrees >= 337.5 || degrees < 22.5) return "NORTH (0°)";
        if (degrees >= 22.5 && degrees < 67.5) return "NORTHEAST (45°)";
        if (degrees >= 67.5 && degrees < 112.5) return "EAST (90°)";
        if (degrees >= 112.5 && degrees < 157.5) return "SOUTHEAST (135°)";
        if (degrees >= 157.5 && degrees < 202.5) return "SOUTH (180°)";
        if (degrees >= 202.5 && degrees < 247.5) return "SOUTHWEST (225°)";
        if (degrees >= 247.5 && degrees < 292.5) return "WEST (270°)";
        return "NORTHWEST (315°)";
    }

    /**
     * Get directional arrow for heading
     */
    private String getDirectionArrow(double degrees) {
        if (degrees >= 337.5 || degrees < 22.5) return "↑";
        if (degrees >= 22.5 && degrees < 67.5) return "↗";
        if (degrees >= 67.5 && degrees < 112.5) return "→";
        if (degrees >= 112.5 && degrees < 157.5) return "↘";
        if (degrees >= 157.5 && degrees < 202.5) return "↓";
        if (degrees >= 202.5 && degrees < 247.5) return "↙";
        if (degrees >= 247.5 && degrees < 292.5) return "←";
        return "↖";
    }

    /**
     * Get field quadrant (1-4) based on position
     * Quadrant 1: Top-Right (positive X, positive Y)
     * Quadrant 2: Top-Left (negative X, positive Y)
     * Quadrant 3: Bottom-Left (negative X, negative Y)
     * Quadrant 4: Bottom-Right (positive X, negative Y)
     */
    private String getFieldQuadrant(double x, double y) {
        double centerX = FIELD_WIDTH / 2;
        double centerY = FIELD_HEIGHT / 2;

        if (x >= centerX && y >= centerY) return "Q1 (Top-Right)";
        if (x < centerX && y >= centerY) return "Q2 (Top-Left)";
        if (x < centerX && y < centerY) return "Q3 (Bottom-Left)";
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
     */
    private String getFieldPositionDescription(double x, double y) {
        double centerX = FIELD_WIDTH / 2;
        double centerY = FIELD_HEIGHT / 2;

        // Check if near center
        if (Math.abs(x - centerX) < 12 && Math.abs(y - centerY) < 12) {
            return "FIELD CENTER";
        }

        // Check if near corners
        if (x < 24 && y < 24) return "BOTTOM-LEFT CORNER";
        if (x > 120 && y < 24) return "BOTTOM-RIGHT CORNER";
        if (x < 24 && y > 120) return "TOP-LEFT CORNER";
        if (x > 120 && y > 120) return "TOP-RIGHT CORNER";

        // Check edges
        if (x < 24) return "LEFT EDGE";
        if (x > 120) return "RIGHT EDGE";
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

    @Override
    public void stop() {
        // Stop all motors when program ends
        if (follower != null) {
            follower.breakFollowing();
        }
    }
}

