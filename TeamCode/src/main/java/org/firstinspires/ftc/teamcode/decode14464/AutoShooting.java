package org.firstinspires.ftc.teamcode.decode14464;

import com.bylazar.configurables.annotations.Configurable;
import com.bylazar.telemetry.PanelsTelemetry;
import com.bylazar.telemetry.TelemetryManager;
import com.pedropathing.follower.Follower;
import com.pedropathing.geometry.Pose;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;

import org.firstinspires.ftc.robotcore.external.navigation.Pose3D;
import org.firstinspires.ftc.teamcode.decode14464.systems.LimelightManager;
import org.firstinspires.ftc.teamcode.pedroPathing.Constants;
import org.firstinspires.ftc.teamcode.decode14464.systems.RobotMotors;
import org.firstinspires.ftc.teamcode.decode14464.systems.RobotServos;
import org.firstinspires.ftc.teamcode.decode14464.systems.RobotSensors;

@Configurable
// @TeleOp(name = "Auto Shooting", group = "Competition")  // DISABLED
public class AutoShooting extends OpMode {
    // ========== ROBOT COMPONENTS ==========
    private Follower follower;
    private RobotMotors robotMotors = new RobotMotors();
    private RobotServos robotServos = new RobotServos();
    private RobotSensors robotSensors = new RobotSensors();
    private LimelightManager limelightManager = new LimelightManager();
    private TelemetryManager telemetryM;

    // ========== APRILTAG CONFIGURATION ==========
    // AprilTag IDs are managed by LimelightManager alliance system:
    // - BLUE Alliance: Tag ID 20
    // - RED Alliance: Tag ID 24
    // Use limelightManager.toggleAlliance() to switch, getTargetTagId() to get current

    // ========== GOAL CONFIGURATION ==========
    // Dynamic goal position (updated from AprilTag detection)
    private double goalX = 72.0; // inches (updated by Limelight)
    private double goalY = 72.0; // inches (updated by Limelight)

    // Fallback goal position if AprilTag not visible
    public static double FALLBACK_GOAL_X = 72.0; // inches (manual fallback)
    public static double FALLBACK_GOAL_Y = 72.0; // inches (manual fallback)

    // AprilTag offset from basket center (adjust based on your tag placement)
    public static double TAG_OFFSET_X = 0.0; // inches (if tag is offset from basket center)
    public static double TAG_OFFSET_Y = 0.0; // inches (if tag is offset from basket center)

    // Goal selection
    private boolean useAprilTagDetection = true;
    private boolean aprilTagDetected = false;

    // ========== SHOOTER TUNING PARAMETERS ==========
    // These define the trajectory pass-through point
    public static double SCORE_HEIGHT = 24.0; // Height of pass-through point above launcher (inches)
    public static double PASS_THROUGH_RADIUS = 20.0; // Distance from goal to pass-through point (inches)
    public static double PASS_THROUGH_ANGLE = -30.0; // Angle at pass-through point (degrees, negative)

    // Physical constraints
    public static double LAUNCHER_HEIGHT = 12.0; // Height of launcher off ground (inches)
    public static double MIN_HOOD_ANGLE = 15.0; // Minimum achievable hood angle (degrees)
    public static double MAX_HOOD_ANGLE = 75.0; // Maximum achievable hood angle (degrees)
    public static double MIN_FLYWHEEL_SPEED = 800.0; // Minimum flywheel speed (ticks/sec)
    public static double MAX_FLYWHEEL_SPEED = 2000.0; // Maximum flywheel speed (ticks/sec)

    // Servo conversion parameters (tune these based on your robot)
    public static double HOOD_SERVO_MIN = 0.17; // Servo position at MIN_HOOD_ANGLE
    public static double HOOD_SERVO_MAX = 0.91; // Servo position at MAX_HOOD_ANGLE
    public static double TURRET_SERVO_CENTER = 0.5; // Servo position pointing forward
    public static double TURRET_DEGREES_PER_SERVO_UNIT = 180.0; // How many degrees per servo unit (0-1)

    // Physics constants
    private static final double GRAVITY = 386.22; // inches/sec^2 (9.8 m/s^2)

    // ========== AUTO-TURN CONFIGURATION ==========
    public static double AUTO_TURN_KP = 0.022; // Proportional gain for auto-turning
    public static double MAX_TURN_POWER = 0.4; // Maximum turn power during auto-aim

    // ========== CONTROL VARIABLES ==========
    private boolean runAutoAim = false;
    private boolean runAutoShoot = false;
    private double headingSetpoint = 0.0;
    private double calculatedHoodAngle = 45.0;
    private double calculatedFlywheelSpeed = 1500.0;
    private double calculatedTurretOffset = 0.0;

    // Button state tracking
    private boolean prevCrossPressed = false;
    private boolean prevCirclePressed = false;
    private boolean prevTrianglePressed = false;
    private boolean prevSquarePressed = false;
    private boolean prevLeftBumper = false;
    private boolean prevRightBumper = false;
    private boolean prevAPressed = false;

    // Robot state
    private boolean isShooterActive = false;
    private boolean slowMode = false;
    private double slowModeMultiplier = 0.5;

    @Override
    public void init() {
        // Initialize hardware
        robotMotors.init(hardwareMap);
        robotServos.init(hardwareMap);
        robotSensors.init(hardwareMap);
        limelightManager.init(hardwareMap);

        follower = Constants.createFollower(hardwareMap);
        follower.setStartingPose(new Pose(0, 0, 0));
        follower.update();

        telemetryM = PanelsTelemetry.INSTANCE.getTelemetry();

        telemetry.addLine("=================================");
        telemetry.addLine("  AUTO SHOOTING INITIALIZED");
        telemetry.addLine("=================================");
        telemetry.addLine("Controls:");
        telemetry.addLine("  Cross (X) - Auto aim at goal");
        telemetry.addLine("  Circle (O) - Toggle shooter");
        telemetry.addLine("  Triangle - Switch target tag");
        telemetry.addLine("  Square - Manual aim forward");
        telemetry.addLine("  Right Trigger - Launch ball");
        telemetry.addLine("  D-Pad Left - Toggle AprilTag mode");
        telemetry.addLine("=================================");
        telemetry.addData("Limelight", limelightManager.isConnected() ? "Connected" : "NOT FOUND");
        String alliance = limelightManager.getAlliance() == LimelightManager.Alliance.BLUE ? "BLUE" : "RED";
        telemetry.addData("Alliance", alliance + " (Tag " + limelightManager.getTargetTagId() + ")");
        telemetry.update();
    }

    @Override
    public void start() {
        follower.startTeleopDrive();
    }

    @Override
    public void loop() {
        follower.update();
        telemetryM.update();

        // Update Limelight data
        limelightManager.update();

        // Get current robot pose
        Pose currentPose = follower.getPose();

        // Get robot velocity components (inches/sec)
        double velocityX = follower.poseTracker.getLocalizer().getVelocity().getX();
        double velocityY = follower.poseTracker.getLocalizer().getVelocity().getY();

        // ========== APRILTAG GOAL DETECTION ==========
        if (useAprilTagDetection) {
            updateGoalPositionFromAprilTag(currentPose);
        } else {
            // Use fallback coordinates
            goalX = FALLBACK_GOAL_X;
            goalY = FALLBACK_GOAL_Y;
            aprilTagDetected = false;
        }

        // ========== ALLIANCE SWITCHING ==========
        // Triangle button to toggle between BLUE (Tag 20) and RED (Tag 24) alliance
        if (gamepad1.triangle && !prevTrianglePressed) {
            limelightManager.toggleAlliance();
        }
        prevTrianglePressed = gamepad1.triangle;

        // D-Pad Left to toggle AprilTag detection mode
        if (gamepad1.dpad_left) {
            useAprilTagDetection = !useAprilTagDetection;
        }

        // ========== AUTO-AIM LOGIC ==========
        // Press and hold Cross (X) to auto-aim at goal
        if (gamepad1.cross) {
            runAutoAim = true;
            calculateShooterParameters(currentPose, velocityX, velocityY);
        } else {
            runAutoAim = false;
        }

        // ========== DRIVE CONTROL ==========
        double rx; // Rotation input

        if (runAutoAim) {
            // Auto-turn to face goal
            double botHeading = Math.toDegrees(currentPose.getHeading());
            double error = headingSetpoint - botHeading;

            // Normalize error to -180 to 180
            while (error > 180) error -= 360;
            while (error < -180) error += 360;

            // Apply proportional control
            rx = error * AUTO_TURN_KP;

            // Clamp turn power
            rx = Math.max(-MAX_TURN_POWER, Math.min(MAX_TURN_POWER, rx));
        } else {
            // Manual rotation control
            rx = -gamepad1.right_stick_x;
        }

        // Apply drive controls
        double driveMultiplier = slowMode ? slowModeMultiplier : 1.0;
        follower.setTeleOpDrive(
            -gamepad1.left_stick_y * driveMultiplier,
            -gamepad1.left_stick_x * driveMultiplier,
            rx,
            true // Field-centric
        );

        // ========== SHOOTER CONTROL ==========
        // Toggle shooter with Circle (O)
        if (gamepad1.circle && !prevCirclePressed) {
            if (isShooterActive) {
                robotMotors.stopShooter();
                isShooterActive = false;
            } else {
                robotMotors.startShooter(calculatedFlywheelSpeed);
                isShooterActive = true;
            }
        }
        prevCirclePressed = gamepad1.circle;

        // Update shooter speed if auto-aiming and shooter is active
        if (runAutoAim && isShooterActive) {
            robotMotors.startShooter(calculatedFlywheelSpeed);
        }

        // ========== HOOD CONTROL ==========
        if (runAutoAim) {
            // Set hood to calculated angle
            double hoodServoPosition = hoodAngleToServoPosition(calculatedHoodAngle);
            robotServos.hood_5.setPosition(hoodServoPosition);
        } else {
            // Manual hood control with D-pad
            if (gamepad1.dpad_up) {
                robotServos.hood_5.setPosition(
                    Math.min(HOOD_SERVO_MAX, robotServos.hood_5.getPosition() + 0.01)
                );
            }
            if (gamepad1.dpad_down) {
                robotServos.hood_5.setPosition(
                    Math.max(HOOD_SERVO_MIN, robotServos.hood_5.getPosition() - 0.01)
                );
            }
        }

        // ========== TURRET ROTATION CONTROL ==========
        if (runAutoAim) {
            // Auto-control turret based on calculated offset
            // This compensates for robot movement perpendicular to goal
            double turretServoPosition = TURRET_SERVO_CENTER + (calculatedTurretOffset / TURRET_DEGREES_PER_SERVO_UNIT);
            // Clamp turret position
            turretServoPosition = Math.max(0, Math.min(1, turretServoPosition));
            // Note: Apply to your turret rotation servo if you have one
            // For now, this is a placeholder - adjust based on your turret design
        } else {
            // Manual turret control
            if (gamepad2.right_stick_y != 0) {
                robotMotors.turret.setPower(gamepad2.right_stick_y);
            }
        }

        // ========== BALL LAUNCH CONTROL ==========
        // Right trigger to push ball into flywheel
        if (gamepad1.right_trigger > 0.1) {
            robotServos.ballpush_4.setPosition(
                robotServos.ballpush_initial_position + (gamepad1.right_trigger * 0.27)
            );
        } else {
            robotServos.ballpush_4.setPosition(robotServos.ballpush_initial_position);
        }

        // ========== PRESET ANGLES ==========
        // Square button - aim straight ahead (0 degrees)
        if (gamepad1.square && !prevSquarePressed) {
            headingSetpoint = 0.0;
            runAutoAim = true;
        }
        prevSquarePressed = gamepad1.square;

        // ========== INTAKE CONTROL ==========
        if (gamepad1.left_bumper && !prevLeftBumper) {
            // Toggle intake (reuse your existing intake logic if needed)
        }
        prevLeftBumper = gamepad1.left_bumper;

        // ========== TELEMETRY ==========
        telemetry.addLine("=================================");
        telemetry.addLine("  AUTO SHOOTING STATUS");
        telemetry.addLine("=================================");
        telemetry.addData("Auto-Aim Active", runAutoAim);
        telemetry.addData("Shooter Active", isShooterActive);
        telemetry.addLine("---------------------------------");
        telemetry.addData("AprilTag Mode", useAprilTagDetection ? "ENABLED" : "Manual");
        String alliance = limelightManager.getAlliance() == LimelightManager.Alliance.BLUE ? "BLUE" : "RED";
        telemetry.addData("Alliance", alliance + " (Tag " + limelightManager.getTargetTagId() + ")");
        telemetry.addData("Tag Detected", aprilTagDetected ? "YES" : "NO");
        telemetry.addData("Limelight", limelightManager.isConnected() ? "Connected" : "Disconnected");
        telemetry.addData("Visible Tags", limelightManager.getVisibleTagCount());
        telemetry.addLine("---------------------------------");
        telemetry.addData("Goal X", "%.1f in", goalX);
        telemetry.addData("Goal Y", "%.1f in", goalY);
        telemetry.addData("Robot X", "%.1f in", currentPose.getX());
        telemetry.addData("Robot Y", "%.1f in", currentPose.getY());
        telemetry.addData("Robot Heading", "%.1f°", Math.toDegrees(currentPose.getHeading()));
        telemetry.addLine("---------------------------------");
        telemetry.addData("Distance to Goal", "%.1f in", getDistanceToGoal(currentPose));
        telemetry.addData("Heading Setpoint", "%.1f°", headingSetpoint);
        telemetry.addData("Heading Error", "%.1f°",
            headingSetpoint - Math.toDegrees(currentPose.getHeading()));
        telemetry.addLine("---------------------------------");
        telemetry.addData("Hood Angle", "%.1f°", calculatedHoodAngle);
        telemetry.addData("Flywheel Speed", "%.0f ticks/sec", calculatedFlywheelSpeed);
        telemetry.addData("Current Flywheel", "%.0f ticks/sec", robotMotors.getShooterVelocity());
        telemetry.addData("Turret Offset", "%.1f°", calculatedTurretOffset);
        telemetry.addLine("=================================");
        telemetry.update();
    }

    /**
     * Update goal position from AprilTag detection using Limelight
     * This calculates the goal's field position based on the detected AprilTag
     */
    private void updateGoalPositionFromAprilTag(Pose robotPose) {
        if (!limelightManager.isConnected()) {
            // Limelight not available, use fallback
            goalX = FALLBACK_GOAL_X;
            goalY = FALLBACK_GOAL_Y;
            aprilTagDetected = false;
            return;
        }

        // Check if target AprilTag is visible
        if (limelightManager.hasAllianceTarget()) {
            com.qualcomm.hardware.limelightvision.LLResultTypes.FiducialResult targetTag =
                limelightManager.getAllianceTag();

            if (targetTag != null) {
                // Get target pose in robot-relative coordinates from Limelight
                // Limelight provides tx (horizontal offset) and ty (vertical offset) in degrees
                // and also provides distance to target

                // Method 1: Use Limelight's botpose if available (most accurate)
                Pose robotPoseFromLimelight = limelightManager.getRobotPoseFromAprilTag();

                if (robotPoseFromLimelight != null) {
                    // Limelight gave us robot position, so we can calculate tag position
                    // Get target pose from the fiducial result
                    try {
                        // Get the target's pose relative to the robot
                        Pose3D targetPose3D = targetTag.getTargetPoseRobotSpace();

                        if (targetPose3D != null) {
                            // Convert robot-relative position to field coordinates
                            double tagRelativeX = targetPose3D.getPosition().x;
                            double tagRelativeY = targetPose3D.getPosition().y;

                            // Convert to field coordinates
                            // Rotate by robot heading and add robot position
                            double robotHeading = robotPose.getHeading();
                            double cosHeading = Math.cos(robotHeading);
                            double sinHeading = Math.sin(robotHeading);

                            // Transform robot-relative to field coordinates
                            double tagFieldX = robotPose.getX() + (tagRelativeX * cosHeading - tagRelativeY * sinHeading);
                            double tagFieldY = robotPose.getY() + (tagRelativeX * sinHeading + tagRelativeY * cosHeading);

                            // Apply offset if tag is not at basket center
                            goalX = tagFieldX + TAG_OFFSET_X;
                            goalY = tagFieldY + TAG_OFFSET_Y;

                            aprilTagDetected = true;
                            return;
                        }
                    } catch (Exception e) {
                        // Fall through to backup method
                    }
                }

                // Method 2: Calculate from tx, ty, and distance (backup method)
                double tx = limelightManager.getTargetX(); // Horizontal offset in degrees
                double ty = limelightManager.getTargetY(); // Vertical offset in degrees

                // Estimate distance using tag size and pixel height (if available)
                // This is a simplified calculation - adjust based on your setup
                // For now, we'll use a reasonable estimate

                // If we can't get accurate position, use fallback
                goalX = FALLBACK_GOAL_X;
                goalY = FALLBACK_GOAL_Y;
                aprilTagDetected = true; // Tag is visible, but using fallback position

            } else {
                // Tag ID matches but no data
                goalX = FALLBACK_GOAL_X;
                goalY = FALLBACK_GOAL_Y;
                aprilTagDetected = false;
            }
        } else {
            // Target tag not visible, use fallback
            goalX = FALLBACK_GOAL_X;
            goalY = FALLBACK_GOAL_Y;
            aprilTagDetected = false;
        }
    }

    /**
     * Calculate all shooter parameters based on robot position and velocity
     */
    private void calculateShooterParameters(Pose robotPose, double velocityX, double velocityY) {
        // Calculate distance and angle to goal
        double dx = goalX - robotPose.getX();  // Use dynamic goalX from AprilTag
        double dy = goalY - robotPose.getY();  // Use dynamic goalY from AprilTag
        double distanceToGoal = Math.sqrt(dx * dx + dy * dy);
        double angleToGoal = Math.atan2(dy, dx);

        // Set heading setpoint to face goal
        headingSetpoint = Math.toDegrees(angleToGoal);

        // Calculate distance to pass-through point
        double d = distanceToGoal - PASS_THROUGH_RADIUS;
        double h = SCORE_HEIGHT;
        double theta = Math.toRadians(PASS_THROUGH_ANGLE);

        // ========== CALCULATE HOOD ANGLE (assuming no robot movement) ==========
        // Using trajectory equations and pass-through point constraints
        double tanTheta = Math.tan(theta);
        double hoodAngleRad = Math.atan((h + d * tanTheta) / d);
        calculatedHoodAngle = Math.toDegrees(hoodAngleRad);

        // Clamp hood angle
        calculatedHoodAngle = Math.max(MIN_HOOD_ANGLE, Math.min(MAX_HOOD_ANGLE, calculatedHoodAngle));

        // ========== CALCULATE FLYWHEEL SPEED (assuming no robot movement) ==========
        double sinHood = Math.sin(hoodAngleRad);
        double cosHood = Math.cos(hoodAngleRad);

        // Initial velocity needed to reach pass-through point
        double v0_squared = (GRAVITY * d * d) / (2 * cosHood * cosHood * (d * Math.tan(hoodAngleRad) - h));
        double v0 = Math.sqrt(Math.abs(v0_squared));

        // ========== COMPENSATE FOR ROBOT VELOCITY ==========
        // Get robot velocity magnitude and direction
        double robotSpeed = Math.sqrt(velocityX * velocityX + velocityY * velocityY);
        double robotVelAngle = Math.atan2(velocityY, velocityX);

        // Calculate angle difference between robot velocity and robot-to-goal vector
        double coordTheta = robotVelAngle - angleToGoal;

        // Decompose robot velocity into parallel and perpendicular components
        double parallelVelocity = robotSpeed * Math.cos(coordTheta);
        double perpendicularVelocity = robotSpeed * Math.sin(coordTheta);

        // Calculate ball's vertical velocity component
        double vz = v0 * sinHood;

        // Calculate airtime
        double airtime = (vz + Math.sqrt(vz * vz + 2 * GRAVITY * LAUNCHER_HEIGHT)) / GRAVITY;

        // Adjust radial velocity for robot motion
        double initialRadialVelocity = (d / airtime) + parallelVelocity;

        // Compensate for perpendicular motion using Pythagorean theorem
        double perpendicularDistance = perpendicularVelocity * airtime;
        double adjustedDistance = Math.sqrt(d * d + perpendicularDistance * perpendicularDistance);

        // Recalculate hood angle with adjusted distance
        hoodAngleRad = Math.atan((vz * airtime - 0.5 * GRAVITY * airtime * airtime) / adjustedDistance);
        calculatedHoodAngle = Math.toDegrees(hoodAngleRad);
        calculatedHoodAngle = Math.max(MIN_HOOD_ANGLE, Math.min(MAX_HOOD_ANGLE, calculatedHoodAngle));

        // Recalculate flywheel speed
        v0 = initialRadialVelocity / Math.cos(hoodAngleRad);

        // Convert velocity to flywheel ticks per second
        // This conversion depends on your specific flywheel setup
        // For now, using a linear approximation - TUNE THIS based on your robot
        calculatedFlywheelSpeed = velocityToFlywheelTicks(v0);
        calculatedFlywheelSpeed = Math.max(MIN_FLYWHEEL_SPEED, Math.min(MAX_FLYWHEEL_SPEED, calculatedFlywheelSpeed));

        // ========== CALCULATE TURRET OFFSET ==========
        // Calculate offset angle to compensate for perpendicular motion
        calculatedTurretOffset = Math.toDegrees(Math.atan2(perpendicularDistance, d));
    }

    /**
     * Convert hood angle (degrees) to servo position (0-1)
     */
    private double hoodAngleToServoPosition(double angleDegrees) {
        double normalized = (angleDegrees - MIN_HOOD_ANGLE) / (MAX_HOOD_ANGLE - MIN_HOOD_ANGLE);
        return HOOD_SERVO_MIN + normalized * (HOOD_SERVO_MAX - HOOD_SERVO_MIN);
    }

    /**
     * Convert artifact exit velocity (inches/sec) to flywheel speed (ticks/sec)
     * TUNE THIS FUNCTION based on your specific flywheel design
     */
    private double velocityToFlywheelTicks(double velocityInchesPerSec) {
        // This is a placeholder linear conversion
        // You need to calibrate this based on your flywheel diameter and gear ratio
        // Example: if 1500 ticks/sec gives you 120 inches/sec exit velocity
        // then the conversion factor is 1500/120 = 12.5
        double conversionFactor = 12.5; // TUNE THIS VALUE
        return velocityInchesPerSec * conversionFactor;
    }

    /**
     * Calculate distance from robot to goal
     */
    private double getDistanceToGoal(Pose robotPose) {
        double dx = goalX - robotPose.getX();  // Use dynamic goalX from AprilTag
        double dy = goalY - robotPose.getY();  // Use dynamic goalY from AprilTag
        return Math.sqrt(dx * dx + dy * dy);
    }

    @Override
    public void stop() {
        robotMotors.stopShooter();
        robotServos.ballpush_4.setPosition(robotServos.ballpush_initial_position);
        limelightManager.stop();
    }
}

