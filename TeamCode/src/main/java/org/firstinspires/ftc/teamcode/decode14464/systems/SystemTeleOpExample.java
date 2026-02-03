package org.firstinspires.ftc.teamcode.decode14464.systems;

import com.pedropathing.geometry.Pose;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

/**
 * Example TeleOp using the new modular RobotSystem
 * Demonstrates how to use all features in a clean, simple way
 */
@TeleOp(name = "System TeleOp Example", group = "Systems")
public class SystemTeleOpExample extends OpMode {

    private RobotSystem robot;

    // Button state tracking
    private boolean prevA = false;
    private boolean prevB = false;
    private boolean prevX = false;
    private boolean prevY = false;
    private boolean prevDpadLeft = false;
    private boolean prevDpadRight = false;
    private boolean prevRightBumper = false;
    private boolean prevLeftBumper = false;

    @Override
    public void init() {
        // Initialize robot system with starting pose
        robot = new RobotSystem();
        robot.init(hardwareMap, new Pose(0, 0, 0));

        telemetry.addLine("✅ Robot System Initialized");
        telemetry.addLine();
        telemetry.addLine("Controls:");
        telemetry.addLine("  A: Toggle Intake/Launch Mode");
        telemetry.addLine("  B: Toggle Auto-Turret");
        telemetry.addLine("  X: Toggle Auto-Hood");
        telemetry.addLine("  Y: Toggle Shooter");
        telemetry.addLine("  Right Bumper: Trigger Auto-Shoot (3 balls)");
        telemetry.addLine("  Left Bumper: Enable Full Auto Mode");
        telemetry.addLine("  DPAD Left/Right: Manual turret servo");
        telemetry.update();
    }

    @Override
    public void start() {
        // Start teleop drive
        robot.localization.getFollower().startTeleopDrive();
    }

    @Override
    public void loop() {
        // ========== UPDATE ALL SYSTEMS ==========
        robot.updateAll();

        // ========== DRIVE CONTROL ==========
        robot.localization.getFollower().setTeleOpDrive(
            -gamepad1.left_stick_y,
            -gamepad1.left_stick_x,
            -gamepad1.right_stick_x,
            false // Field-centric (required for accurate localization)
        );

        // ========== BUTTON CONTROLS ==========

        // A: Toggle Intake/Launch Mode
        if (gamepad1.a && !prevA) {
            robot.autoIntake.toggleMode();
        }
        prevA = gamepad1.a;

        // B: Toggle Auto-Turret
        if (gamepad1.b && !prevB) {
            robot.autoTurret.toggle();
        }
        prevB = gamepad1.b;

        // X: Toggle Auto-Hood
        if (gamepad1.x && !prevX) {
            robot.autoHood.toggleAutoAdjust();
        }
        prevX = gamepad1.x;

        // Y: Toggle Shooter
        if (gamepad1.y && !prevY) {
            if (robot.shooting.isShooterActive()) {
                robot.shooting.stopShooter();
            } else {
                robot.shooting.startShooterAuto(); // Distance-based velocity
            }
        }
        prevY = gamepad1.y;

        // Right Bumper: Trigger Auto-Shoot (3 balls)
        if (gamepad1.right_bumper && !prevRightBumper) {
            robot.shooting.triggerAutoShoot();
        }
        prevRightBumper = gamepad1.right_bumper;

        // Left Bumper: Enable Full Auto Mode (turret + hood)
        if (gamepad1.left_bumper && !prevLeftBumper) {
            if (robot.autoTurret.isEnabled() && robot.autoHood.isAutoAdjustEnabled()) {
                robot.disableFullAuto();
            } else {
                robot.enableFullAuto();
            }
        }
        prevLeftBumper = gamepad1.left_bumper;

        // DPAD Left/Right: Manual turret servo control
        if (gamepad1.dpad_left && !prevDpadLeft) {
            robot.autoIntake.moveToPreviousSlot();
        }
        prevDpadLeft = gamepad1.dpad_left;

        if (gamepad1.dpad_right && !prevDpadRight) {
            robot.autoIntake.advanceToNextSlot();
        }
        prevDpadRight = gamepad1.dpad_right;

        // ========== TELEMETRY ==========
        telemetry.addLine("━━━ POSITION ━━━");
        telemetry.addData("X, Y", "%.1f, %.1f", robot.localization.getX(), robot.localization.getY());
        telemetry.addData("Heading", "%.1f°", robot.localization.getHeadingDegrees());
        telemetry.addLine();

        telemetry.addLine("━━━ AUTO SYSTEMS ━━━");
        telemetry.addData("Auto-Turret", robot.autoTurret.isEnabled() ? "✓ ON" : "✗ OFF");
        telemetry.addData("Auto-Hood", robot.autoHood.isAutoAdjustEnabled() ? "✓ ON" : "✗ OFF");
        telemetry.addData("Turret Angle", "%.1f°", robot.autoTurret.getTurretAngle());
        telemetry.addData("On Target", robot.autoTurret.isOnTarget() ? "✓ YES" : "✗ NO");
        telemetry.addLine();

        telemetry.addLine("━━━ SHOOTING ━━━");
        telemetry.addData("Distance to Goal", "%.1f in", robot.getDistanceToGoal());
        telemetry.addData("Shooter", robot.shooting.isShooterActive() ? "✓ ACTIVE" : "✗ OFF");
        telemetry.addData("Velocity", "%.0f", robot.shooting.getCurrentVelocity());
        telemetry.addData("Auto-Shoot", robot.shooting.isAutoShootingInProgress() ? "🎯 SHOOTING" : "Idle");
        telemetry.addData("State", robot.shooting.getCurrentState());
        telemetry.addLine();

        telemetry.addLine("━━━ INTAKE ━━━");
        telemetry.addData("Mode", robot.autoIntake.isIntakeMode() ? "🔵 INTAKE" : "🔴 LAUNCH");
        telemetry.addData("Slot", robot.autoIntake.getCurrentIndex());
        telemetry.addData("Ball Detected", robot.autoIntake.isBallDetected() ? "✓ YES" : "✗ NO");
        telemetry.addData("Color", robot.autoIntake.getDetectedColor());

        telemetry.update();
    }

    @Override
    public void stop() {
        robot.stopAll();
    }
}

