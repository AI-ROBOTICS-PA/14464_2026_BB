package org.firstinspires.ftc.teamcode.decode14464;

import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

/**
 * Simple Turret Auto-Alignment TeleOp
 * DECODE 14464 - FTC 2026
 *
 * Controls:
 *   Cross (X)     - Toggle auto-tracking
 *   Right Stick X - Manual turret control
 */
@TeleOp(name = "Turret TeleOp", group = "DECODE")
public class TurretTeleOp extends OpMode {

    private Turret turret;
    private boolean autoTrack = false;
    private boolean prevCross = false;

    @Override
    public void init() {
        turret = new Turret();
        turret.init(hardwareMap);

        // Uncomment if turret moves wrong direction:
         turret.setReversed(true);

        telemetry.addData("Status", "Ready");
        telemetry.addData("Controls", "X = Toggle Auto | Right Stick = Manual");
        telemetry.update();
    }

    @Override
    public void loop() {
        // Toggle auto-tracking with Cross button
        if (gamepad1.cross && !prevCross) {
            autoTrack = !autoTrack;
        }
        prevCross = gamepad1.cross;

        // Run turret control
        boolean locked = false;
        if (autoTrack) {
            locked = turret.track();
        } else {
            // Manual control
            turret.setManualPower(-gamepad1.right_stick_x * 0.5);
        }

        // Telemetry
        telemetry.addData("Mode", autoTrack ? "AUTO" : "MANUAL");
        telemetry.addData("Limelight", turret.isConnected() ? "Connected" : "NOT CONNECTED");
        telemetry.addData("Target", turret.hasTarget() ? "VISIBLE" : "--- not seen ---");
        telemetry.addData("Locked", locked ? "YES" : "no");
        telemetry.addLine("---");
        telemetry.addData("TX (horizontal)", "%.1f°", turret.getTargetOffset());
        telemetry.addData("TY (vertical)", "%.1f°", turret.getTargetY());
        telemetry.addData("Area", "%.3f", turret.getTargetArea());
        telemetry.addData("Motor Power", "%.2f", turret.getMotorPower());
        telemetry.addLine("---");
        telemetry.addLine("If tag not seen from far:");
        telemetry.addLine("1. Check Limelight web UI exposure");
        telemetry.addLine("2. Use larger printed tags (6\"+)");
        telemetry.addLine("3. Ensure good lighting");
        telemetry.update();
    }

    @Override
    public void stop() {
        turret.stop();
    }
}
