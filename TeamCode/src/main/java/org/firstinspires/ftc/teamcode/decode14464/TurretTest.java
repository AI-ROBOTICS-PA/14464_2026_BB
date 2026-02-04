package org.firstinspires.ftc.teamcode.decode14464;

import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import org.firstinspires.ftc.teamcode.decode14464.systems.RobotSystem;
import com.pedropathing.geometry.Pose;

/**
 * Turret Test OpMode
 * Simple test program to verify turret tracking is working correctly
 *
 * CONTROLS:
 * - A Button: Enable/Disable auto-tracking
 * - Right Stick Y: Manual turret control (when auto disabled)
 * - Left Stick: Drive robot
 *
 * WHAT TO WATCH:
 * - Encoder position should change smoothly (not jump hundreds of ticks)
 * - Target angle should be reasonable (<120 degrees)
 * - Turret should rotate toward goal, not spin wildly
 */
@TeleOp(name = "Turret Test", group = "Testing")
public class TurretTest extends OpMode {
    private RobotSystem robot;
    private boolean prevA = false;

    @Override
    public void init() {
        // Initialize robot at center of field facing forward
        robot = new RobotSystem();
        robot.init(hardwareMap, new Pose(72, 72, 0)); // Center of field

        telemetry.addLine("✅ Turret Test Ready");
        telemetry.addLine("Press A to enable auto-tracking");
        telemetry.update();
    }

    @Override
    public void start() {
        // Start motors
        robot.motors.startShooter(1460);
        robot.autoIntake.startIntakeMotor();
    }

    @Override
    public void loop() {
        // Update all systems
        robot.updateAll();

        // Toggle auto-turret with A button
        boolean aPressed = gamepad1.a || gamepad1.cross;
        if (aPressed && !prevA) {
            robot.autoTurret.toggle();
        }
        prevA = aPressed;

        // Manual turret control when auto disabled (for testing)
        if (!robot.autoTurret.isEnabled()) {
            double manualPower = -gamepad1.right_stick_y * 0.4;
            robot.autoTurret.setManualPower(manualPower);
        }

        // Drive control
        robot.localization.getFollower().setTeleOpDrive(
            -gamepad1.left_stick_y,
            -gamepad1.left_stick_x,
            -gamepad1.right_stick_x
        );

        // Display comprehensive turret info
        displayTelemetry();
    }

    private void displayTelemetry() {
        telemetry.addLine("====== TURRET TEST ======");
        telemetry.addLine();

        // Auto-tracking status
        telemetry.addData("Auto-Tracking", robot.autoTurret.isEnabled() ? "✅ ENABLED" : "❌ DISABLED");

        // Encoder position
        int encoderPos = robot.autoTurret.getTurretEncoderPosition();
        telemetry.addData("Encoder Position", encoderPos + " ticks");

        // Target angle
        double targetAngle = robot.autoTurret.getTurretAngle();
        String angleStatus = Math.abs(targetAngle) > 120 ? "⚠️ TOO LARGE!" : "✅ OK";
        telemetry.addData("Target Angle", String.format("%.1f° %s", targetAngle, angleStatus));

        // On target status
        telemetry.addData("On Target", robot.autoTurret.isOnTarget() ? "✅ YES" : "❌ NO");

        // Limit warnings
        boolean nearLimits = robot.autoTurret.isNearLimits();
        telemetry.addData("Near Limits", nearLimits ? "⚠️ YES" : "✅ NO");

        // Distance to goal
        double distance = robot.autoTurret.getDistanceToGoal();
        telemetry.addData("Distance to Goal", String.format("%.1f inches", distance));

        // Robot position
        Pose pose = robot.localization.getPose();
        telemetry.addLine();
        telemetry.addLine("====== ROBOT POSITION ======");
        telemetry.addData("X", String.format("%.1f in", pose.getX()));
        telemetry.addData("Y", String.format("%.1f in", pose.getY()));
        telemetry.addData("Heading", String.format("%.1f°", Math.toDegrees(pose.getHeading())));

        // Controls reminder
        telemetry.addLine();
        telemetry.addLine("====== CONTROLS ======");
        telemetry.addData("A Button", "Toggle Auto-Tracking");
        telemetry.addData("Right Stick Y", robot.autoTurret.isEnabled() ? "Disabled (Auto)" : "Manual Control");
        telemetry.addData("Left Stick", "Drive");

        // Diagnostics
        telemetry.addLine();
        telemetry.addLine("====== DIAGNOSTICS ======");

        if (Math.abs(targetAngle) > 120) {
            telemetry.addLine("⚠️ WARNING: Target angle too large!");
            telemetry.addLine("   Turret safety stop activated");
        }

        if (nearLimits) {
            telemetry.addLine("⚠️ WARNING: Near rotation limit!");
            telemetry.addLine("   Turret slowing down");
        }

        if (encoderPos == 0 && robot.autoTurret.isEnabled()) {
            telemetry.addLine("ℹ️ NOTE: Encoder at 0 (centered)");
        }

        if (Math.abs(encoderPos) > 700) {
            telemetry.addLine("⚠️ CAUTION: Near maximum rotation");
            telemetry.addData("Max allowed", "±750 ticks");
        }

        telemetry.update();
    }

    @Override
    public void stop() {
        robot.autoTurret.stop();
        robot.motors.stopShooter();
        robot.autoIntake.stopIntakeMotor();
    }
}

