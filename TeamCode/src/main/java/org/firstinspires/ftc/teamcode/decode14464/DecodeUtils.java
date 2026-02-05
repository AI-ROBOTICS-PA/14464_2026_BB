package org.firstinspires.ftc.teamcode.decode14464;

import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;

/**
 * Utility class for framework-level helper functions for Decode14464 package.
 * Add static methods here for code reuse across OpModes and subsystems.
 */
public class DecodeUtils {

    // ========== AUTO TURRET UTILITIES ==========

    /**
     * TurretTrackingResult - Result of auto-tracking calculation
     * Contains all data needed to control the turret
     */
    public static class TurretTrackingResult {
        public double motorPower;           // Calculated motor power
        public boolean isTracking;          // Is actively tracking a target
        public boolean isLockedOn;          // Is locked on target (within deadband)
        public double error;                // Current error in degrees
        public double targetOffset;         // Target horizontal offset (tx)
        public boolean limitReached;        // Has turret reached rotation limit

        public TurretTrackingResult(double motorPower, boolean isTracking, boolean isLockedOn,
                                   double error, double targetOffset, boolean limitReached) {
            this.motorPower = motorPower;
            this.isTracking = isTracking;
            this.isLockedOn = isLockedOn;
            this.error = error;
            this.targetOffset = targetOffset;
            this.limitReached = limitReached;
        }
    }

    /**
     * TurretPIDState - Maintains PID controller state between calls
     * Store one instance and pass it to calculateTurretTracking() each loop
     */
    public static class TurretPIDState {
        public double previousError = 0.0;
        public double integral = 0.0;

        public void reset() {
            previousError = 0.0;
            integral = 0.0;
        }
    }

    /**
     * Initialize turret motor with standard configuration
     * Call this AFTER RobotMotors.init() if you want to reconfigure the turret
     *
     * Note: If RobotMotors already initializes the turret correctly, you may not need this.
     *
     * @param turretMotor The turret motor to initialize (e.g., robotMotors.turret)
     */
    public static void initializeTurretMotor(DcMotorEx turretMotor) {
        turretMotor.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        turretMotor.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        turretMotor.setDirection(DcMotor.Direction.FORWARD);
        turretMotor.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
    }

    /**
     * Calculate auto-tracking power for turret using PID control
     *
     * This is the core auto-tracking logic that can be used in any OpMode.
     *
     * @param limelightManager LimelightManager instance for tag detection
     * @param targetTagID AprilTag ID to track (e.g., 20 or 24)
     * @param pidState PID state object (maintains state between calls)
     * @param kp Proportional gain (use TurretConstants.AUTO_TRACK_KP or custom value)
     * @param kd Derivative gain (use TurretConstants.AUTO_TRACK_KD or custom value)
     * @param maxPower Maximum motor power (use TurretConstants.MAX_TURRET_POWER or custom)
     * @return TurretTrackingResult containing motor power and tracking status
     */
    public static TurretTrackingResult calculateTurretTracking(
            LimelightManager limelightManager,
            int targetTagID,
            TurretPIDState pidState,
            double kp,
            double kd,
            double maxPower) {

        // Check if Limelight is connected
        if (!limelightManager.isConnected()) {
            pidState.reset();
            return new TurretTrackingResult(0.0, false, false, 0.0, 0.0, false);
        }

        // Check if target tag is visible
        if (!limelightManager.isAprilTagVisible(targetTagID)) {
            pidState.reset();
            return new TurretTrackingResult(0.0, false, false, 0.0, 0.0, false);
        }

        // Get horizontal offset (tx) from Limelight
        // tx is in degrees - positive means target is to the right of center
        // Use unfiltered since we already did our own tag check above
        double tx = limelightManager.getTargetXUnfiltered();

        // Error is how far off we are (want tx to be 0)
        double error = tx;

        // Check if we're within deadband (locked on)
        if (Math.abs(error) < TurretConstants.DEADBAND) {
            pidState.reset();
            return new TurretTrackingResult(0.0, true, true, error, tx, false);
        }

        // PID calculations
        pidState.integral += error;
        double derivative = error - pidState.previousError;

        // Calculate power using PID formula
        double power = (kp * error) +
                      (TurretConstants.AUTO_TRACK_KI * pidState.integral) +
                      (kd * derivative);

        // Apply direction inversion if needed
        if (TurretConstants.INVERT_TURRET_DIRECTION) {
            power = -power;
        }

        // Clamp power to max limit
        power = Math.max(-maxPower, Math.min(maxPower, power));

        // Add minimum power to overcome friction (prevents jerky slow movements)
        if (Math.abs(power) > 0.01 && Math.abs(power) < TurretConstants.MIN_POWER_THRESHOLD) {
            power = Math.signum(power) * TurretConstants.MIN_POWER_THRESHOLD;
        }

        // Update for next iteration
        pidState.previousError = error;

        return new TurretTrackingResult(power, true, false, error, tx, false);
    }

    /**
     * Enforce rotation limits with soft limits (slows down near edges)
     *
     * @param currentEncoderPosition Current turret encoder position
     * @param requestedPower Desired motor power
     * @return Safe motor power (may be reduced or blocked by limits)
     */
    public static double enforceTurretLimits(double currentEncoderPosition, double requestedPower) {
        double maxPosition = TurretConstants.getMaxEncoderPosition();
        double minPosition = TurretConstants.getMinEncoderPosition();
        double softLimitZone = TurretConstants.getSoftLimitZoneTicks();

        // Check if at max limit (hard stop)
        if (currentEncoderPosition >= maxPosition && requestedPower > 0) {
            return 0.0; // Stop - can't go further positive
        }

        // Check if at min limit (hard stop)
        if (currentEncoderPosition <= minPosition && requestedPower < 0) {
            return 0.0; // Stop - can't go further negative
        }

        // Soft limit - slow down near max
        if (currentEncoderPosition > (maxPosition - softLimitZone) && requestedPower > 0) {
            double reduction = (maxPosition - currentEncoderPosition) / softLimitZone;
            requestedPower *= reduction;
        }

        // Soft limit - slow down near min
        if (currentEncoderPosition < (minPosition + softLimitZone) && requestedPower < 0) {
            double reduction = (currentEncoderPosition - minPosition) / softLimitZone;
            requestedPower *= reduction;
        }

        return requestedPower;
    }

    /**
     * Check if turret is near rotation limits
     *
     * @param currentEncoderPosition Current turret encoder position
     * @param warningZoneDegrees Warning zone in degrees (default 20)
     * @return true if near limits, false otherwise
     */
    public static boolean isTurretNearLimits(double currentEncoderPosition, double warningZoneDegrees) {
        double currentDegrees = TurretConstants.encoderToDegrees(currentEncoderPosition);
        double maxDegrees = TurretConstants.MAX_ROTATION_DEGREES;
        double minDegrees = TurretConstants.MIN_ROTATION_DEGREES;

        return (currentDegrees > maxDegrees - warningZoneDegrees) ||
               (currentDegrees < minDegrees + warningZoneDegrees);
    }

    /**
     * Reset turret to center position using RUN_TO_POSITION mode
     *
     * @param turretMotor The turret motor
     * @param power Power to use for centering (typically 0.3)
     */
    public static void resetTurretToCenter(DcMotorEx turretMotor, double power) {
        turretMotor.setMode(DcMotor.RunMode.RUN_TO_POSITION);
        turretMotor.setTargetPosition((int) TurretConstants.getCenterEncoderPosition());
        turretMotor.setPower(Math.abs(power));
    }

    /**
     * Check if turret has reached target position (for RUN_TO_POSITION mode)
     *
     * @param turretMotor The turret motor
     * @param toleranceDegrees Position tolerance in degrees (default 2.0)
     * @return true if at target, false otherwise
     */
    public static boolean isTurretAtTarget(DcMotorEx turretMotor, double toleranceDegrees) {
        if (turretMotor.getMode() != DcMotor.RunMode.RUN_TO_POSITION) {
            return false;
        }

        double toleranceTicks = TurretConstants.degreesToEncoder(toleranceDegrees);
        int currentPosition = turretMotor.getCurrentPosition();
        int targetPosition = turretMotor.getTargetPosition();

        return Math.abs(currentPosition - targetPosition) <= toleranceTicks;
    }

    /**
     * Get turret position as a human-readable status string
     *
     * @param currentEncoderPosition Current encoder position
     * @return Status string (e.g., "Center", "145.2° (5° left of center)")
     */
    public static String getTurretPositionStatus(double currentEncoderPosition) {
        double currentDegrees = TurretConstants.encoderToDegrees(currentEncoderPosition);
        double centerDegrees = TurretConstants.CENTER_POSITION_DEGREES;

        if (Math.abs(currentDegrees - centerDegrees) < 5.0) {
            return "Center (" + String.format("%.1f°", currentDegrees) + ")";
        }

        double offsetFromCenter = currentDegrees - centerDegrees;
        String direction = (offsetFromCenter > 0) ? "anticlockwise" : "clockwise";

        return String.format("%.1f° (%.1f° %s from center)",
                           currentDegrees, Math.abs(offsetFromCenter), direction);
    }

    /**
     * Simple auto-tracking wrapper for basic use cases
     * Uses default PID values from TurretConstants
     *
     * @param limelightManager LimelightManager instance
     * @param targetTagID AprilTag ID to track
     * @param pidState PID state object
     * @return TurretTrackingResult with calculated motor power
     */
    public static TurretTrackingResult autoTrackTurret(
            LimelightManager limelightManager,
            int targetTagID,
            TurretPIDState pidState) {
        return calculateTurretTracking(
            limelightManager,
            targetTagID,
            pidState,
            TurretConstants.AUTO_TRACK_KP,
            TurretConstants.AUTO_TRACK_KD,
            TurretConstants.MAX_TURRET_POWER
        );
    }

    /**
     * Auto-track turret using ALLIANCE-BASED tag filtering
     * Automatically uses the correct tag (20 for BLUE, 24 for RED) based on
     * the alliance set in LimelightManager.
     *
     * @param limelightManager LimelightManager instance (must have alliance set)
     * @param pidState PID state object
     * @return TurretTrackingResult with calculated motor power
     */
    public static TurretTrackingResult autoTrackTurretAlliance(
            LimelightManager limelightManager,
            TurretPIDState pidState) {
        
        // Check if Limelight is connected
        if (!limelightManager.isConnected()) {
            pidState.reset();
            return new TurretTrackingResult(0.0, false, false, 0.0, 0.0, false);
        }

        // Check if ALLIANCE tag is visible (uses cached data - efficient)
        if (!limelightManager.hasAllianceTarget()) {
            pidState.reset();
            return new TurretTrackingResult(0.0, false, false, 0.0, 0.0, false);
        }

        // Get horizontal offset (tx) - alliance filtered
        double tx = limelightManager.getTargetX();
        double error = tx;

        // Check if we're within deadband (locked on)
        if (Math.abs(error) < TurretConstants.DEADBAND) {
            pidState.reset();
            return new TurretTrackingResult(0.0, true, true, error, tx, false);
        }

        // PID calculations
        pidState.integral += error;
        double derivative = error - pidState.previousError;

        double power = (TurretConstants.AUTO_TRACK_KP * error) +
                      (TurretConstants.AUTO_TRACK_KI * pidState.integral) +
                      (TurretConstants.AUTO_TRACK_KD * derivative);

        if (TurretConstants.INVERT_TURRET_DIRECTION) {
            power = -power;
        }

        power = Math.max(-TurretConstants.MAX_TURRET_POWER, 
                        Math.min(TurretConstants.MAX_TURRET_POWER, power));

        if (Math.abs(power) < TurretConstants.MIN_TURRET_POWER && Math.abs(error) > TurretConstants.DEADBAND) {
            power = Math.signum(power) * TurretConstants.MIN_TURRET_POWER;
        }

        pidState.previousError = error;

        return new TurretTrackingResult(power, true, false, error, tx, false);
    }
}



