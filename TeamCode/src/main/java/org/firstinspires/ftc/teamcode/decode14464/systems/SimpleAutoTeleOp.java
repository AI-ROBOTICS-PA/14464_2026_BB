package org.firstinspires.ftc.teamcode.decode14464.systems;

import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

/**
 * Simple TeleOp - Focus on Auto-Intake and Auto-Shooting
 *
 * Controls:
 * ========
 * GAMEPAD 1:
 * - A/Cross: Toggle Intake/Launch Mode (works with both PS4 and Xbox controllers)
 * - Right Bumper: Auto-Shoot (shoots all balls in turret)
 * - Left Stick: Drive (X/Y movement)
 * - Right Stick X: Rotation
 *
 * Features:
 * =========
 * - Auto-Intake: Automatically advances turret when ball detected
 * - Auto-Shoot: Automatically shoots all loaded balls
 * - Simple mode switching between Intake and Launch
 * - Supports both PS4 (Cross/Circle) and Xbox (A/B) controllers
 */
@TeleOp(name = "Simple Auto TeleOp", group = "Main")
public class SimpleAutoTeleOp extends OpMode {

    private RobotSystem robot;

    // Button state tracking for edge detection
    private boolean prevA = false;
    private boolean prevRightBumper = false;

    // Shooter velocity (high velocity only for simplicity)
    private static final double SHOOTER_VELOCITY = 1460;

    @Override
    public void init() {
        // Initialize robot - preserves position from Auto
        robot = new RobotSystem();
        robot.initTeleOp(hardwareMap);

        telemetry.addLine("✅ Simple Auto TeleOp Initialized");
        telemetry.addLine();
        telemetry.addLine("Controls:");
        telemetry.addLine("  A/Cross: Toggle Intake ↔ Launch Mode");
        telemetry.addLine("  Right Bumper: Auto-Shoot All Balls");
        telemetry.addLine();
        telemetry.addData("Current Mode", "LAUNCH (ready to shoot)");
        telemetry.update();
    }

    @Override
    public void start() {
        // Start TeleOp driving
        robot.localization.getFollower().startTeleopDrive();

        // Start shooter motor at high velocity
        robot.shooting.startShooter(SHOOTER_VELOCITY);

        // Start intake motor (always running)
        robot.autoIntake.startIntakeMotor();

        telemetry.addLine("🚀 TeleOp Started!");
        telemetry.addLine("✅ Shooter Running at " + SHOOTER_VELOCITY);
        telemetry.addLine("✅ Intake Motor Running");
        telemetry.update();
    }

    @Override
    public void loop() {
        // ========== UPDATE ALL SYSTEMS ==========
        robot.updateAll();

        // ========== DRIVE CONTROL ==========
        robot.localization.getFollower().setTeleOpDrive(
            -gamepad1.left_stick_y,
            -gamepad1.left_stick_x,
            -gamepad1.right_stick_x
        );

        // ========== MODE SWITCHING ==========
        // A/Cross: Toggle between Intake and Launch modes
        // Support both controller types: PS4 (cross) and Xbox (a)
        boolean aPressed = gamepad1.a || gamepad1.cross;
        if (aPressed && !prevA) {
            robot.autoIntake.toggleMode();

            // Sync LED color with mode
            if (robot.autoIntake.isIntakeMode()) {
                robot.led.setIntakeModeColor();
            } else {
                robot.led.setLaunchModeColor();
            }
        }
        prevA = aPressed;

        // ========== AUTO-SHOOT ==========
        // Right Bumper: Start auto-shooting sequence
        // Support both controller types
        boolean rightBumperPressed = gamepad1.right_bumper;
        if (rightBumperPressed && !prevRightBumper) {
            // Only shoot if in launch mode
            if (!robot.autoIntake.isIntakeMode() && !robot.autoShoot.isInProgress()) {
                // Determine number of balls based on current turret position
                int ballsToShoot = robot.autoIntake.getCurrentIndex() + 1;
                robot.autoShoot.start(ballsToShoot);
            }
        }
        prevRightBumper = rightBumperPressed;

        // ========== TELEMETRY ==========
        updateTelemetry();
    }

    private void updateTelemetry() {
        // ===== MODE STATUS =====
        telemetry.addLine("========== MODE ==========");
        String modeText = robot.autoIntake.isIntakeMode() ? "🔵 INTAKE" : "🔴 LAUNCH";
        telemetry.addData("Current Mode", modeText);
        telemetry.addData("Turret Slot", robot.autoIntake.getCurrentIndex());

        // ===== AUTO-INTAKE STATUS =====
        if (robot.autoIntake.isIntakeMode()) {
            telemetry.addLine();
            telemetry.addLine("===== AUTO-INTAKE =====");
            telemetry.addData("Ball Detected", robot.autoIntake.isBallDetected() ? "✓ YES" : "✗ NO");
            telemetry.addData("Color", robot.autoIntake.getDetectedColor());
        }

        // ===== AUTO-SHOOT STATUS =====
        if (robot.autoShoot.isInProgress()) {
            telemetry.addLine();
            telemetry.addLine("===== AUTO-SHOOT =====");
            telemetry.addData("Status", "🎯 SHOOTING!");
            telemetry.addData("Balls Shot", robot.autoShoot.getBallsShot() + "/" + robot.autoShoot.getBallsToShoot());
            telemetry.addData("State", robot.autoShoot.getCurrentState());
        }

        // ===== SHOOTER STATUS =====
        telemetry.addLine();
        telemetry.addLine("====== SHOOTER ======");
        telemetry.addData("Velocity", "%.0f", robot.motors.getShooterVelocity());
        telemetry.addData("Target", "%.0f", SHOOTER_VELOCITY);
        telemetry.addData("Ready", robot.shooting.isShooterAtTargetVelocity() ? "✓ YES" : "⏳ Spinning Up");

        // ===== POSITION INFO =====
        telemetry.addLine();
        telemetry.addLine("====== POSITION ======");
        telemetry.addData("X, Y", "%.1f, %.1f", robot.localization.getX(), robot.localization.getY());
        telemetry.addData("Heading", "%.1f°", robot.localization.getHeadingDegrees());

        // ===== CONTROLS REMINDER =====
        telemetry.addLine();
        telemetry.addLine("====== CONTROLS ======");
        telemetry.addData("A/Cross", "Toggle Mode");
        telemetry.addData("Right Bumper", "Auto-Shoot");

        telemetry.update();
    }

    @Override
    public void stop() {
        robot.stopAll();
        telemetry.addLine("⏹️ TeleOp Stopped");
        telemetry.update();
    }
}

