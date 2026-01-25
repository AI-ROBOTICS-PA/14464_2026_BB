package org.firstinspires.ftc.teamcode.decode14464;

import com.qualcomm.robotcore.hardware.DcMotorEx;

/**
 * Utility class for framework-level helper functions for Decode14464 package.
 * Add static methods here for code reuse across OpModes and subsystems.
 */
public class DecodeUtils {
    // Example utility method: clamp a value between min and max
    public static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    /**
     * Points the turret to the barcode using Limelight horizontal offset (tx).
     * @param limelight LimelightManager instance
     * @param turret DcMotorEx controlling the turret
     * @param targetTagId The AprilTag/barcode ID to track
     * @param kP Proportional gain for turret control (suggest 0.01-0.05)
     * @param minPower Minimum power to overcome turret stiction (suggest 0.05)
     * @param maxPower Maximum allowed power (suggest 0.3-0.5)
     * @return true if barcode is detected and turret is being controlled, false otherwise
     */
    public static boolean pointTurretToBarcode(
            LimelightManager limelight,
            DcMotorEx turret,
            int targetTagId,
            double kP,
            double minPower,
            double maxPower
    ) {
        if (limelight == null || turret == null) return false;
        limelight.update();
        if (!limelight.isConnected() || !limelight.isAprilTagVisible(targetTagId)) {
            turret.setPower(0);
            return false;
        }
        double tx = limelight.getTargetX(); // Horizontal offset in degrees
        double power = kP * tx;
        // Clamp power and apply minimum threshold
        if (Math.abs(power) < minPower) {
            power = 0;
        } else {
            power = Math.max(-maxPower, Math.min(maxPower, power));
        }
        turret.setPower(power);
        return true;
    }

    // Add more utility methods as needed
}
