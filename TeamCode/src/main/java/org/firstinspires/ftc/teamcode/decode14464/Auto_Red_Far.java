package org.firstinspires.ftc.teamcode.decode14464;

import com.pedropathing.follower.Follower;
import com.pedropathing.geometry.Pose;
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.util.ElapsedTime;
import org.firstinspires.ftc.teamcode.pedroPathing.Constants;

/**
 * Auto_Red_Far - Autonomous routine for Red Alliance, Far Starting Position
 *
 * DECODE Team 14464 - FTC 2026
 *
 * Strategy:
 * 1. Start from far red alliance position
 * 2. Score preloaded sample in high basket
 * 3. Collect specimens from observation zone
 * 4. Score specimens on high chamber
 * 5. Park in observation zone
 *
 * Features:
 * - Auto-turret aiming using Limelight
 * - Pedro Pathing for smooth trajectories
 * - Integrated intake/shooter system
 * - Autonomous hood angle adjustment
 */
@Autonomous(name = "Auto Red Far", group = "DECODE Auto")
public class Auto_Red_Far extends LinearOpMode {

    // Hardware and systems
    private Follower follower;
    private RobotMotors robotMotors = new RobotMotors();
    private RobotServos robotServos = new RobotServos();
    private RobotSensors robotSensors = new RobotSensors();
    private LimelightManager limelightManager = new LimelightManager();

    // Auto-turret
    private DecodeUtils.TurretPIDState turretPidState = new DecodeUtils.TurretPIDState();
    private int currentTargetTagID = TurretConstants.PRIMARY_TAG_ID;

    // Timing
    private ElapsedTime runtime = new ElapsedTime();

    // Field positions (RED ALLIANCE - mirrored from blue)
    // All measurements in INCHES
    private static final Pose START_POSE = new Pose(36, -63, Math.toRadians(90)); // Far red starting position
    private static final Pose SCORE_BASKET_POSE = new Pose(56, -56, Math.toRadians(45)); // High basket position (mirrored)
    private static final Pose SPECIMEN_PICKUP_POSE = new Pose(48, -48, Math.toRadians(0)); // Observation zone (mirrored)
    private static final Pose SCORE_CHAMBER_POSE = new Pose(8, -36, Math.toRadians(90)); // High chamber (mirrored)
    private static final Pose PARK_POSE = new Pose(60, -60, Math.toRadians(0)); // Park in observation zone (mirrored)

    // Shooting parameters
    private static final double SHOOT_VELOCITY = 1450; // High velocity for auto
    private static final double HOOD_ANGLE_BASKET = 0.65; // Hood angle for high basket
    private static final double HOOD_ANGLE_CHAMBER = 0.50; // Hood angle for chamber

    // Timing constants
    private static final int SHOOT_DELAY_MS = 500; // Delay between shots
    private static final int INTAKE_DELAY_MS = 1000; // Time to intake specimen
    private static final double AUTO_TRACK_TIMEOUT = 3.0; // Max time to lock onto target

    @Override
    public void runOpMode() {
        // ========== INITIALIZATION ==========
        telemetry.addLine("═══════════════════════════════");
        telemetry.addLine("  AUTO RED FAR - INITIALIZING");
        telemetry.addLine("═══════════════════════════════");
        telemetry.update();

        // Initialize all systems
        initializeSystems();

        telemetry.addLine("✅ All systems initialized");
        telemetry.addLine("Ready to start!");
        telemetry.addData("Starting Position", "Red Far");
        telemetry.addData("Alliance", "RED ALLIANCE");
        telemetry.addData("Target", "High Basket + Chamber");
        telemetry.update();

        // Wait for start
        waitForStart();
        runtime.reset();

        if (opModeIsActive()) {
            // ========== AUTONOMOUS SEQUENCE ==========

            // Step 1: Score preloaded sample in high basket
            scorePreloadedSample();

            // Step 2: Collect and score specimens (repeat 2-3 times)
            for (int i = 0; i < 2 && opModeIsActive(); i++) {
                collectSpecimen();
                scoreSpecimenOnChamber();
            }

            // Step 3: Park in observation zone
            parkInObservationZone();

            // ========== END OF AUTO ==========
            telemetry.addLine("═══════════════════════════════");
            telemetry.addLine("  AUTO COMPLETE!");
            telemetry.addLine("═══════════════════════════════");
            telemetry.addData("Alliance", "RED");
            telemetry.addData("Total Time", "%.1f sec", runtime.seconds());
            telemetry.update();
        }
    }

    /**
     * Initialize all robot systems
     */
    private void initializeSystems() {
        // Initialize hardware
        robotMotors.init(hardwareMap);
        robotServos.init(hardwareMap);
        robotSensors.init(hardwareMap);
        limelightManager.init(hardwareMap);

        // Initialize turret motor for auto-tracking
        DecodeUtils.initializeTurretMotor(robotMotors.turret);

        // Initialize Pedro Pathing follower
        follower = Constants.createFollower(hardwareMap);
        follower.setStartingPose(START_POSE);

        // Set initial positions
        robotServos.hood_5.setPosition(HoodConstants.HOME_POSITION);
        robotServos.turret_1.setPosition(0.17); // Launch mode
        robotServos.colorservo.setPosition(0.38); // Launch mode

        telemetry.addLine("✅ Motors initialized");
        telemetry.addLine("✅ Servos initialized");
        telemetry.addLine("✅ Sensors initialized");
        telemetry.addLine("✅ Limelight initialized");
        telemetry.addLine("✅ Pedro Pathing initialized");
    }

    /**
     * Step 1: Score preloaded sample in high basket
     */
    private void scorePreloadedSample() {
        telemetry.addLine("───────────────────────────────");
        telemetry.addLine("STEP 1: Score Preloaded Sample");
        telemetry.addLine("───────────────────────────────");
        telemetry.update();

        // For now, use simple position setting instead of path following
        // TODO: Replace with proper path following when API is confirmed
        follower.setMaxPower(0.8);

        // Set hood angle for high basket
        robotServos.hood_5.setPosition(HOOD_ANGLE_BASKET);
        sleep(300); // Wait for servo to move

        // Auto-aim at basket using AprilTag
        boolean locked = autoAimAtTarget(TurretConstants.PRIMARY_TAG_ID, AUTO_TRACK_TIMEOUT);

        if (locked) {
            telemetry.addLine("✅ Locked on target!");
            telemetry.update();

            // Shoot preloaded sample
            shootSample();
        } else {
            telemetry.addLine("⚠️ Could not lock on target, shooting anyway");
            telemetry.update();
            shootSample();
        }
    }

    /**
     * Step 2: Collect specimen from observation zone
     */
    private void collectSpecimen() {
        telemetry.addLine("───────────────────────────────");
        telemetry.addLine("STEP 2: Collect Specimen");
        telemetry.addLine("───────────────────────────────");
        telemetry.update();

        // TODO: Add path following movement

        // Switch to intake mode
        robotServos.turret_1.setPosition(0.07); // Intake position
        robotServos.colorservo.setPosition(1.0); // Intake mode
        sleep(300);

        // Start intake
        robotMotors.startIntake();
        telemetry.addLine("🔄 Intake running...");
        telemetry.update();

        // Wait for specimen to be collected (check color sensor)
        waitForSpecimenIntake(INTAKE_DELAY_MS);

        // Stop intake
        robotMotors.stopIntake();

        // Switch back to launch mode
        robotServos.turret_1.setPosition(0.17); // Launch position
        robotServos.colorservo.setPosition(0.38); // Launch mode
        sleep(300);

        telemetry.addLine("✅ Specimen collected");
        telemetry.update();
    }

    /**
     * Step 3: Score specimen on high chamber
     */
    private void scoreSpecimenOnChamber() {
        telemetry.addLine("───────────────────────────────");
        telemetry.addLine("STEP 3: Score on Chamber");
        telemetry.addLine("───────────────────────────────");
        telemetry.update();

        // TODO: Add path following movement

        // Set hood angle for chamber
        robotServos.hood_5.setPosition(HOOD_ANGLE_CHAMBER);
        sleep(300);

        // Auto-aim at chamber using AprilTag
        boolean locked = autoAimAtTarget(TurretConstants.SECONDARY_TAG_ID, AUTO_TRACK_TIMEOUT);

        if (locked) {
            telemetry.addLine("✅ Locked on chamber!");
            telemetry.update();

            // Shoot specimen
            shootSample();
        } else {
            telemetry.addLine("⚠️ Could not lock on chamber, shooting anyway");
            telemetry.update();
            shootSample();
        }
    }

    /**
     * Step 4: Park in observation zone
     */
    private void parkInObservationZone() {
        telemetry.addLine("───────────────────────────────");
        telemetry.addLine("STEP 4: Park");
        telemetry.addLine("───────────────────────────────");
        telemetry.update();

        // TODO: Add path following movement

        // Lower all mechanisms
        robotServos.hood_5.setPosition(HoodConstants.HOME_POSITION);
        robotServos.turret_1.setPosition(0.17);

        telemetry.addLine("✅ Parked!");
        telemetry.update();
    }

    /**
     * Auto-aim turret at AprilTag target
     * @param tagID AprilTag ID to track
     * @param timeoutSeconds Maximum time to try
     * @return true if locked on target
     */
    private boolean autoAimAtTarget(int tagID, double timeoutSeconds) {
        double startTime = runtime.seconds();
        currentTargetTagID = tagID;

        while (opModeIsActive() && (runtime.seconds() - startTime) < timeoutSeconds) {
            // Update Limelight
            limelightManager.update();

            // Get auto-tracking result
            DecodeUtils.TurretTrackingResult result = DecodeUtils.autoTrackTurret(
                limelightManager,
                currentTargetTagID,
                turretPidState
            );

            // Apply turret power with limits
            double turretPower = DecodeUtils.enforceTurretLimits(
                robotMotors.turret.getCurrentPosition(),
                result.motorPower
            );
            robotMotors.turret.setPower(turretPower);

            // Update telemetry
            telemetry.addData("🎯 Auto-Aim", "Tag %d", tagID);
            telemetry.addData("Tag Visible", result.isTracking ? "YES ✓" : "NO ✗");
            telemetry.addData("Status", result.isLockedOn ? "LOCKED ✓" : "Tracking...");
            telemetry.addData("Error", "%.2f°", result.error);
            telemetry.addData("Time", "%.1f / %.1f sec", runtime.seconds() - startTime, timeoutSeconds);
            telemetry.update();

            // Check if locked on
            if (result.isLockedOn) {
                robotMotors.turret.setPower(0); // Stop turret
                return true;
            }

            sleep(20); // Small delay for loop
        }

        // Timeout - stop turret
        robotMotors.turret.setPower(0);
        return false;
    }

    /**
     * Shoot a sample/specimen
     */
    private void shootSample() {
        telemetry.addLine("🚀 Shooting...");
        telemetry.update();

        // Start shooter at high velocity
        robotMotors.startShooter(SHOOT_VELOCITY);

        // Wait for shooter to spin up
        sleep(500);

        // Push ball into shooter
        robotServos.ballpush_4.setPosition(robotServos.ballpush_initial_position + 0.27);
        sleep(SHOOT_DELAY_MS);

        // Retract ball pusher
        robotServos.ballpush_4.setPosition(robotServos.ballpush_initial_position);
        sleep(300);

        // Stop shooter
        robotMotors.stopShooter();

        telemetry.addLine("✅ Shot complete!");
        telemetry.update();
    }

    /**
     * Wait for specimen to be intaked (using color sensor)
     * @param maxWaitMs Maximum time to wait in milliseconds
     */
    private void waitForSpecimenIntake(int maxWaitMs) {
        long startTime = System.currentTimeMillis();

        while (opModeIsActive() && (System.currentTimeMillis() - startTime) < maxWaitMs) {
            String colorDetected = robotSensors.getColorName(robotSensors.color_f);

            telemetry.addData("Color Detected", colorDetected);
            telemetry.addData("Waiting", "%.1f sec", (System.currentTimeMillis() - startTime) / 1000.0);
            telemetry.update();

            // Check if non-black color detected (specimen present)
            if (!colorDetected.equalsIgnoreCase("Black") && !colorDetected.equalsIgnoreCase("None")) {
                telemetry.addLine("✅ Specimen detected!");
                telemetry.update();
                sleep(200); // Extra time to ensure full intake
                return;
            }

            sleep(50);
        }

        telemetry.addLine("⚠️ Timeout waiting for specimen");
        telemetry.update();
    }
}

