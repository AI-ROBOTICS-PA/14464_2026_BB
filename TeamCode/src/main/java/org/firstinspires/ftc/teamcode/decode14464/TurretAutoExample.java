package org.firstinspires.ftc.teamcode.decode14464;

import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;

/**
 * Example: Using Turret in Autonomous
 * DECODE 14464 - FTC 2026
 */
// DISABLED: Example only — not for competition
// @Autonomous(name = "Turret Auto Example", group = "DECODE")
public class TurretAutoExample extends LinearOpMode {

    private Turret turret;

    @Override
    public void runOpMode() {
        // Initialize
        turret = new Turret();
        turret.init(hardwareMap);

        telemetry.addData("Status", "Ready");
        telemetry.update();

        waitForStart();

        // Example: Align to target with timeout
        alignWithTimeout(3.0);  // 3 second timeout

        // Continue with rest of autonomous...
        telemetry.addData("Status", "Alignment complete!");
        telemetry.update();
        sleep(2000);
    }

    /**
     * Align turret to target with timeout
     * @param timeoutSeconds Max time to try alignment
     * @return true if locked on target
     */
    private boolean alignWithTimeout(double timeoutSeconds) {
        double startTime = getRuntime();

        while (opModeIsActive() && (getRuntime() - startTime) < timeoutSeconds) {
            boolean locked = turret.track();

            telemetry.addData("Aligning...", "%.1fs", getRuntime() - startTime);
            telemetry.addData("Locked", locked);
            telemetry.addData("Offset", "%.1f°", turret.getTargetOffset());
            telemetry.update();

            if (locked) {
                return true;
            }
        }

        turret.stop();
        return false;
    }
}
