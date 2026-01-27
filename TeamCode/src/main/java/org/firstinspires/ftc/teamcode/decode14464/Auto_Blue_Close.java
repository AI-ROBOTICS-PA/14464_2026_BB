package org.firstinspires.ftc.teamcode.decode14464;

import com.pedropathing.follower.Follower;
import com.pedropathing.geometry.Pose;
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.util.ElapsedTime;
import org.firstinspires.ftc.teamcode.pedroPathing.Constants;

/**
 * Auto_Blue_Close - Autonomous routine for Blue Alliance, Close Starting Position
 *
 * DECODE Team 14464 - FTC 2026
 *
 * Strategy:
 * 1. Start from close blue alliance position (near baskets)
 * 2. Score preloaded sample in high basket (short distance)
 * 3. Collect additional samples from spike marks
 * 4. Score samples in high basket
 * 5. Park in observation zone
 *
 * Features:
 * - Auto-turret aiming using Limelight
 * - Pedro Pathing for smooth trajectories
 * - Integrated intake/shooter system
 * - Autonomous hood angle adjustment
 * - Optimized for close starting position
 */
@Autonomous(name = "Auto Blue Close", group = "DECODE Auto")
public class Auto_Blue_Close extends LinearOpMode {

    // Hardware and systems
    private Follower follower;
    private final RobotMotors robotMotors = new RobotMotors();
    private final RobotServos robotServos = new RobotServos();
    private final RobotSensors robotSensors = new RobotSensors();
    private final LimelightManager limelightManager = new LimelightManager();

    // Auto-turret
    private final DecodeUtils.TurretPIDState turretPidState = new DecodeUtils.TurretPIDState();

    // Timing
    private final ElapsedTime runtime = new ElapsedTime();

    // Field positions (BLUE ALLIANCE - CLOSE STARTING POSITION)
    // All measurements in INCHES
    private static final Pose START_POSE = new Pose(-12, -63, Math.toRadians(90)); // Close blue starting position
    private static final Pose SCORE_BASKET_POSE = new Pose(-56, -56, Math.toRadians(135)); // High basket position
    private static final Pose SPIKE_MARK_1 = new Pose(-24, -48, Math.toRadians(90)); // First spike mark
    private static final Pose SPIKE_MARK_2 = new Pose(-36, -48, Math.toRadians(90)); // Second spike mark
    private static final Pose SPIKE_MARK_3 = new Pose(-48, -48, Math.toRadians(90)); // Third spike mark
    private static final Pose PARK_POSE = new Pose(-60, -12, Math.toRadians(180)); // Park in observation zone

    // Shooting parameters (adjusted for close position)
    private static final double SHOOT_VELOCITY = 1100; // Lower velocity for close shots
    private static final double HOOD_ANGLE_BASKET_CLOSE = 0.50; // Lower hood angle for close basket

    // Timing constants
    private static final int SHOOT_DELAY_MS = 500; // Delay between shots
    private static final int INTAKE_DELAY_MS = 1000; // Time to intake sample
    private static final double AUTO_TRACK_TIMEOUT = 3.0; // Max time to lock onto target

    @Override
    public void runOpMode() {
        // ========== INITIALIZATION ==========
        telemetry.addLine("═══════════════════════════════");
        telemetry.addLine("  AUTO BLUE CLOSE - INITIALIZING");
        telemetry.addLine("═══════════════════════════════");
        telemetry.update();

        initializeSystems();

        telemetry.addLine("✅ All systems initialized");
        telemetry.addLine("Ready to start!");
        telemetry.addData("Starting Position", "Blue Close");
        telemetry.addData("Alliance", "BLUE ALLIANCE");
        telemetry.addData("Target", "High Basket (Close)");
        telemetry.update();

        waitForStart();
        runtime.reset();

        if (opModeIsActive()) {
            // ========== AUTONOMOUS SEQUENCE ==========
            scorePreloadedSample();

            for (int i = 0; i < 2 && opModeIsActive(); i++) {
                collectSampleFromSpikeMark(i);
                scoreSampleInBasket();
            }

            parkInObservationZone();

            // ========== END OF AUTO ==========
            telemetry.addLine("═══════════════════════════════");
            telemetry.addLine("  AUTO COMPLETE!");
            telemetry.addLine("═══════════════════════════════");
            telemetry.addData("Alliance", "BLUE");
            telemetry.addData("Starting Position", "Close");
            telemetry.addData("Total Time", "%.1f sec", runtime.seconds());
            telemetry.update();
        }
    }

    private void initializeSystems() {
        robotMotors.init(hardwareMap);
        robotServos.init(hardwareMap);
        robotSensors.init(hardwareMap);
        limelightManager.init(hardwareMap);
        DecodeUtils.initializeTurretMotor(robotMotors.turret);

        follower = Constants.createFollower(hardwareMap);
        follower.setStartingPose(START_POSE);

        robotServos.hood_5.setPosition(HoodConstants.HOME_POSITION);
        robotServos.turret_1.setPosition(0.17);
        robotServos.colorservo.setPosition(0.38);
    }

    private void scorePreloadedSample() {
        telemetry.addLine("STEP 1: Score Preloaded Sample (CLOSE RANGE)");
        telemetry.update();

        follower.setMaxPower(0.6);
        robotServos.hood_5.setPosition(HOOD_ANGLE_BASKET_CLOSE);
        sleep(300);

        if (autoAimAtTarget(TurretConstants.PRIMARY_TAG_ID, AUTO_TRACK_TIMEOUT)) {
            telemetry.addLine("✅ Locked on target!");
        } else {
            telemetry.addLine("⚠️ Shooting without lock");
        }
        telemetry.update();
        shootSample();
    }

    private void collectSampleFromSpikeMark(int sampleIndex) {
        telemetry.addLine("STEP 2: Collect Sample " + (sampleIndex + 1));
        telemetry.update();

        robotServos.turret_1.setPosition(0.07);
        robotServos.colorservo.setPosition(1.0);
        sleep(300);

        robotMotors.startIntake();
        waitForSampleIntake(INTAKE_DELAY_MS);
        robotMotors.stopIntake();

        robotServos.turret_1.setPosition(0.17);
        robotServos.colorservo.setPosition(0.38);
        sleep(300);

        telemetry.addLine("✅ Sample collected");
        telemetry.update();
    }

    private void scoreSampleInBasket() {
        telemetry.addLine("STEP 3: Score in Basket");
        telemetry.update();

        robotServos.hood_5.setPosition(HOOD_ANGLE_BASKET_CLOSE);
        sleep(300);

        if (autoAimAtTarget(TurretConstants.PRIMARY_TAG_ID, AUTO_TRACK_TIMEOUT)) {
            telemetry.addLine("✅ Locked on basket!");
        } else {
            telemetry.addLine("⚠️ Shooting without lock");
        }
        telemetry.update();
        shootSample();
    }

    private void parkInObservationZone() {
        telemetry.addLine("STEP 4: Park");
        telemetry.update();

        robotServos.hood_5.setPosition(HoodConstants.HOME_POSITION);
        robotServos.turret_1.setPosition(0.17);

        telemetry.addLine("✅ Parked!");
        telemetry.update();
    }

    private boolean autoAimAtTarget(int tagID, double timeoutSeconds) {
        double startTime = runtime.seconds();

        while (opModeIsActive() && (runtime.seconds() - startTime) < timeoutSeconds) {
            limelightManager.update();

            DecodeUtils.TurretTrackingResult result = DecodeUtils.autoTrackTurret(
                limelightManager, tagID, turretPidState);

            double turretPower = DecodeUtils.enforceTurretLimits(
                robotMotors.turret.getCurrentPosition(), result.motorPower);
            robotMotors.turret.setPower(turretPower);

            if (result.isLockedOn) {
                robotMotors.turret.setPower(0);
                return true;
            }
            sleep(20);
        }

        robotMotors.turret.setPower(0);
        return false;
    }

    private void shootSample() {
        robotMotors.startShooter(SHOOT_VELOCITY);
        sleep(500);

        robotServos.ballpush_4.setPosition(robotServos.ballpush_initial_position + 0.27);
        sleep(SHOOT_DELAY_MS);

        robotServos.ballpush_4.setPosition(robotServos.ballpush_initial_position);
        sleep(300);

        robotMotors.stopShooter();
    }

    private void waitForSampleIntake(int maxWaitMs) {
        long startTime = System.currentTimeMillis();

        while (opModeIsActive() && (System.currentTimeMillis() - startTime) < maxWaitMs) {
            String colorDetected = robotSensors.getColorName(robotSensors.color_f);

            if (!colorDetected.equalsIgnoreCase("Black") && !colorDetected.equalsIgnoreCase("None")) {
                sleep(200);
                return;
            }
            sleep(50);
        }
    }
}

