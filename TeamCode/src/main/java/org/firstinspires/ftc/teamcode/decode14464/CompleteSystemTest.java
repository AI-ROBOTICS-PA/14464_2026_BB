package org.firstinspires.ftc.teamcode.decode14464;

import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import org.firstinspires.ftc.teamcode.decode14464.systems.RobotSystem;
import com.pedropathing.geometry.Pose;

/**
 * Complete System Test - Turret, Auto-Intake, and Manual Override Integration
 *
 * This program tests ALL fixed systems working together:
 * - Fixed AutoTurret with encoder limits
 * - Auto-Intake with color detection
 * - Manual Override controls (D-Pad and Trigger)
 * - Full RobotSystem integration
 *
 * CONTROLS:
 * ========
 * GAMEPAD 1:
 * - A/Cross: Toggle Intake/Launch Mode
 * - B/Circle: Toggle Auto-Turret Tracking
 * - Right Bumper: Auto-Shoot All Balls
 * - D-Pad Left/Right: Manual Turret Servo Control (Override)
 * - Right Trigger: Manual Ball Push (Override)
 * - Left Stick: Drive
 * - Right Stick X: Rotate
 *
 * WHAT TO WATCH:
 * ===============
 * 1. Turret encoder position (should be smooth, not jumping)
 * 2. Auto-turret target angle (should be <120°)
 * 3. Auto-intake ball detection
 * 4. Manual override working alongside auto systems
 * 5. No excessive rotations or oscillations
 */
// @TeleOp(name = "Complete System Test", group = "Testing")  // DISABLED
public class CompleteSystemTest extends OpMode {
    private RobotSystem robot;

    // Button state tracking
    private boolean prevA = false;
    private boolean prevB = false;
    private boolean prevRightBumper = false;
    private boolean prevDpadLeft = false;
    private boolean prevDpadRight = false;

    private static final double SHOOTER_VELOCITY = 1460;

    @Override
    public void init() {
        // Initialize robot at center of field
        robot = new RobotSystem();
        robot.init(hardwareMap, new Pose(72, 72, 0));

        telemetry.addLine("✅ Complete System Test Initialized");
        telemetry.addLine();
        telemetry.addLine("Systems Ready:");
        telemetry.addLine("  ✅ Turret (Fixed with encoder limits)");
        telemetry.addLine("  ✅ Auto-Intake (Color detection)");
        telemetry.addLine("  ✅ Manual Override (D-Pad + Trigger)");
        telemetry.addLine("  ✅ Auto-Shoot");
        telemetry.update();
    }

    @Override
    public void start() {
        // Start all motors
        robot.motors.startShooter(SHOOTER_VELOCITY);
        robot.autoIntake.startIntakeMotor();
        robot.localization.getFollower().startTeleopDrive();

        telemetry.addLine("🚀 System Test Started!");
        telemetry.update();
    }

    @Override
    public void loop() {
        // Update all systems
        robot.updateAll();

        // ========== MODE SWITCHING ==========
        boolean aPressed = gamepad1.a || gamepad1.cross;
        if (aPressed && !prevA) {
            robot.autoIntake.toggleMode();
            if (robot.autoIntake.isIntakeMode()) {
                robot.led.setIntakeModeColor();
            } else {
                robot.led.setLaunchModeColor();
            }
        }
        prevA = aPressed;

        // ========== AUTO-TURRET TOGGLE ==========
        boolean bPressed = gamepad1.b || gamepad1.circle;
        if (bPressed && !prevB) {
            robot.autoTurret.toggle();
        }
        prevB = bPressed;

        // ========== MANUAL TURRET SERVO OVERRIDE ==========
        if (gamepad1.dpad_left && !prevDpadLeft) {
            robot.autoIntake.moveToPreviousSlot();
        }
        prevDpadLeft = gamepad1.dpad_left;

        if (gamepad1.dpad_right && !prevDpadRight) {
            robot.autoIntake.advanceToNextSlot();
        }
        prevDpadRight = gamepad1.dpad_right;

        // ========== MANUAL BALL PUSH OVERRIDE ==========
        if (!robot.autoIntake.isIntakeMode()) {
            double triggerValue = gamepad1.right_trigger;
            double servoOffset = triggerValue * 0.27;
            robot.servos.ballpush_4.setPosition(
                robot.servos.ballpush_initial_position + servoOffset
            );
        } else {
            robot.servos.ballpush_4.setPosition(robot.servos.ballpush_initial_position);
        }

        // ========== AUTO-SHOOT ==========
        boolean rightBumperPressed = gamepad1.right_bumper;
        if (rightBumperPressed && !prevRightBumper) {
            if (!robot.autoIntake.isIntakeMode() && !robot.autoShoot.isInProgress()) {
                int ballsToShoot = robot.autoIntake.getBallsLoaded();
                if (ballsToShoot > 0) {
                    robot.autoShoot.start(ballsToShoot);
                }
            }
        }
        prevRightBumper = rightBumperPressed;

        // ========== DRIVE CONTROL ==========
        robot.localization.getFollower().setTeleOpDrive(
            -gamepad1.left_stick_y,
            -gamepad1.left_stick_x,
            -gamepad1.right_stick_x
        );

        // ========== TELEMETRY ==========
        displayComprehensiveTelemetry();
    }

    private void displayComprehensiveTelemetry() {
        telemetry.addLine("====== COMPLETE SYSTEM TEST ======");
        telemetry.addLine();

        // ===== AUTO-TURRET STATUS =====
        telemetry.addLine("===== AUTO-TURRET (FIXED) =====");
        telemetry.addData("Auto-Tracking", robot.autoTurret.isEnabled() ? "✅ ON" : "❌ OFF");

        int encoderPos = robot.autoTurret.getTurretEncoderPosition();
        String encoderStatus = (Math.abs(encoderPos) > 700) ? "⚠️" : "✅";
        telemetry.addData("Encoder Position", encoderPos + " ticks " + encoderStatus);

        double targetAngle = robot.autoTurret.getTurretAngle();
        String angleStatus = (Math.abs(targetAngle) > 120) ? "⚠️ TOO LARGE!" : "✅ OK";
        telemetry.addData("Target Angle", String.format("%.1f° %s", targetAngle, angleStatus));

        telemetry.addData("On Target", robot.autoTurret.isOnTarget() ? "✅ YES" : "❌ NO");
        telemetry.addData("Near Limits", robot.autoTurret.isNearLimits() ? "⚠️ YES" : "✅ NO");
        telemetry.addData("Distance to Goal", String.format("%.1f in", robot.autoTurret.getDistanceToGoal()));

        // ===== MODE & INTAKE =====
        telemetry.addLine();
        telemetry.addLine("===== MODE & AUTO-INTAKE =====");
        String modeText = robot.autoIntake.isIntakeMode() ? "🔵 INTAKE" : "🔴 LAUNCH";
        telemetry.addData("Current Mode", modeText);
        telemetry.addData("Turret Servo Slot", robot.autoIntake.getCurrentIndex());
        telemetry.addData("Balls Loaded", robot.autoIntake.getBallsLoaded());

        if (robot.autoIntake.isIntakeMode()) {
            telemetry.addData("Ball Detected", robot.autoIntake.isBallDetected() ? "✓ YES" : "✗ NO");
            telemetry.addData("Color", robot.autoIntake.getDetectedColor());
        }

        // ===== AUTO-SHOOT STATUS =====
        if (robot.autoShoot.isInProgress()) {
            telemetry.addLine();
            telemetry.addLine("===== AUTO-SHOOT =====");
            telemetry.addData("Status", "🎯 SHOOTING!");
            telemetry.addData("Progress", robot.autoShoot.getBallsShot() + "/" + robot.autoShoot.getBallsToShoot());
        }

        // ===== MANUAL OVERRIDE =====
        telemetry.addLine();
        telemetry.addLine("===== MANUAL OVERRIDE =====");
        telemetry.addData("D-Pad L/R", "Turret Servo Control");
        telemetry.addData("Right Trigger",
            robot.autoIntake.isIntakeMode() ? "Disabled" :
            String.format("Ball Push (%.0f%%)", gamepad1.right_trigger * 100));

        // ===== SHOOTER =====
        telemetry.addLine();
        telemetry.addLine("===== SHOOTER =====");
        telemetry.addData("Velocity", String.format("%.0f / %.0f",
            robot.motors.getShooterVelocity(), SHOOTER_VELOCITY));
        telemetry.addData("Ready", robot.shooting.isShooterAtTargetVelocity() ? "✅ YES" : "⏳ Spinning Up");

        // ===== POSITION =====
        telemetry.addLine();
        telemetry.addLine("===== ROBOT POSITION =====");
        Pose pose = robot.localization.getPose();
        telemetry.addData("X, Y", String.format("%.1f, %.1f in", pose.getX(), pose.getY()));
        telemetry.addData("Heading", String.format("%.1f°", Math.toDegrees(pose.getHeading())));

        // ===== CONTROLS =====
        telemetry.addLine();
        telemetry.addLine("===== CONTROLS =====");
        telemetry.addData("A/Cross", "Toggle Intake/Launch Mode");
        telemetry.addData("B/Circle", "Toggle Auto-Turret");
        telemetry.addData("Right Bumper", "Auto-Shoot All");
        telemetry.addData("D-Pad L/R", "Manual Turret Servo");
        telemetry.addData("Right Trigger", "Manual Ball Push");

        // ===== DIAGNOSTICS =====
        telemetry.addLine();
        telemetry.addLine("===== DIAGNOSTICS =====");

        if (Math.abs(targetAngle) > 120) {
            telemetry.addLine("⚠️ WARNING: Turret target angle too large!");
            telemetry.addLine("   Safety stop activated");
        }

        if (robot.autoTurret.isNearLimits()) {
            telemetry.addLine("⚠️ WARNING: Turret near rotation limit!");
            telemetry.addLine("   Soft limit active");
        }

        if (Math.abs(encoderPos) > 700) {
            telemetry.addLine("⚠️ CAUTION: Near max rotation (±750)");
        }

        if (encoderPos == 0 && robot.autoTurret.isEnabled()) {
            telemetry.addLine("ℹ️ Turret encoder at center (0)");
        }

        // Success indicators
        if (robot.autoTurret.isEnabled() && robot.autoTurret.isOnTarget()) {
            telemetry.addLine("🎯 Turret locked on target!");
        }

        if (robot.autoIntake.isBallDetected() && robot.autoIntake.isIntakeMode()) {
            telemetry.addLine("🎾 Ball detected - auto-advancing!");
        }

        telemetry.update();
    }

    @Override
    public void stop() {
        robot.stopAll();
        telemetry.addLine("⏹️ Complete System Test Stopped");
        telemetry.update();
    }
}

