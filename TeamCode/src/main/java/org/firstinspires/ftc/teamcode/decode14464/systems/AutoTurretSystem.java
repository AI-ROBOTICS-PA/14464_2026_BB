package org.firstinspires.ftc.teamcode.decode14464.systems;

import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.HardwareMap;

/**
 * Feature 2: Auto-Turret System
 * Automatically points turret motor toward the goal using localization
 * Uses centralized RobotMotors
 *
 * FIXED: Now uses proper encoder-based control with limits to prevent excessive rotation
 */
public class AutoTurretSystem {
    private RobotMotors motors;
    private LocalizationSystem localization;
    private boolean enabled = false;

    private double previousError = 0;
    private double integral = 0;

    // Encoder-based rotation limits (prevents runaway rotation)
    // CALIBRATED: 3.01 ticks per degree (measured with TurretCalibration on Feb 4, 2026)
    // 271 ticks for 90 degrees → 3.01 ticks/degree
    // Max rotation: ±150 degrees → ±451 ticks
    private static final double MAX_ENCODER_POSITION = 451;   // ~150 degrees (3.01 ticks/degree)
    private static final double MIN_ENCODER_POSITION = -451;  // ~-150 degrees
    private static final double SOFT_LIMIT_ZONE = 50;         // Slow down within 50 ticks of limit

    /**
     * Initialize turret system with centralized motors
     */
    public void init(HardwareMap hardwareMap, LocalizationSystem localizationSystem, RobotMotors motors) {
        this.motors = motors;
        this.localization = localizationSystem;

        // Ensure turret motor is properly initialized
        if (motors != null && motors.turret != null) {
            motors.turret.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
            motors.turret.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
            motors.turret.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        }
    }

    /**
     * Enable auto-tracking
     */
    public void enable() {
        enabled = true;
        previousError = 0;
        integral = 0;
    }

    /**
     * Disable auto-tracking
     */
    public void disable() {
        enabled = false;
        if (motors != null && motors.turret != null) {
            motors.turret.setPower(0);
        }
        previousError = 0;
        integral = 0;
    }

    /**
     * Toggle auto-tracking
     */
    public void toggle() {
        if (enabled) {
            disable();
        } else {
            enable();
        }
    }

    /**
     * Check if auto-tracking is enabled
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Update turret tracking - call this every loop
     * FIXED: Now includes encoder limits to prevent excessive rotation
     */
    public void update() {
        if (!enabled || motors == null || localization == null) {
            return;
        }

        // Calculate angle to goal
        double angleToGoal = localization.getAngleToPoint(
            RobotConstants.Field.GOAL_X,
            RobotConstants.Field.GOAL_Y
        );

        // Get robot heading
        double robotHeading = localization.getHeadingDegrees();

        // Calculate turret angle (relative to robot)
        double turretAngle = angleToGoal - robotHeading;

        // Normalize to -180 to 180
        while (turretAngle > 180) turretAngle -= 360;
        while (turretAngle <= -180) turretAngle += 360;

        // SAFETY CHECK: Limit turret angle to reasonable range
        // This prevents trying to rotate 300+ degrees
        if (Math.abs(turretAngle) > 120) {
            // If angle is too large, stop and alert
            motors.turret.setPower(0);
            previousError = 0;
            integral = 0;
            return;
        }

        // Apply PID control
        double error = turretAngle;

        // Deadband - stop if close enough
        if (Math.abs(error) < RobotConstants.Turret.DEADBAND_DEGREES) {
            motors.turret.setPower(0);
            previousError = 0;
            integral = 0;
            return;
        }

        // Calculate PID terms
        integral += error;
        double derivative = error - previousError;

        // PID control (using KP and KD)
        double power = (error * RobotConstants.Turret.KP) +
                      (integral * RobotConstants.Turret.KI) +
                      (derivative * RobotConstants.Turret.KD);

        // Get current encoder position for limit checking
        int currentPosition = motors.turret.getCurrentPosition();

        // Enforce encoder limits (hard stops)
        power = enforceTurretLimits(currentPosition, power);

        // Clamp power
        power = Math.max(-RobotConstants.Turret.MAX_POWER,
                        Math.min(RobotConstants.Turret.MAX_POWER, power));

        // Apply minimum power threshold to overcome friction
        if (Math.abs(power) > 0.01 && Math.abs(power) < 0.1) {
            power = Math.signum(power) * 0.1;
        }

        // Apply power
        motors.turret.setPower(power);

        // Save error for next iteration
        previousError = error;
    }

    /**
     * Enforce rotation limits with soft limits (slows down near edges)
     * FIXED: Prevents turret from rotating beyond safe limits
     */
    private double enforceTurretLimits(double currentEncoderPosition, double requestedPower) {
        // Check if at max limit (hard stop)
        if (currentEncoderPosition >= MAX_ENCODER_POSITION && requestedPower > 0) {
            return 0.0; // Stop - can't go further positive
        }

        // Check if at min limit (hard stop)
        if (currentEncoderPosition <= MIN_ENCODER_POSITION && requestedPower < 0) {
            return 0.0; // Stop - can't go further negative
        }

        // Soft limit - slow down near max
        if (currentEncoderPosition > (MAX_ENCODER_POSITION - SOFT_LIMIT_ZONE) && requestedPower > 0) {
            double reduction = (MAX_ENCODER_POSITION - currentEncoderPosition) / SOFT_LIMIT_ZONE;
            requestedPower *= Math.max(0.1, reduction); // At least 10% power
        }

        // Soft limit - slow down near min
        if (currentEncoderPosition < (MIN_ENCODER_POSITION + SOFT_LIMIT_ZONE) && requestedPower < 0) {
            double reduction = (currentEncoderPosition - MIN_ENCODER_POSITION) / SOFT_LIMIT_ZONE;
            requestedPower *= Math.max(0.1, reduction); // At least 10% power
        }

        return requestedPower;
    }

    /**
     * Set turret power manually (disables auto-tracking)
     * FIXED: Now includes limit checking for manual control
     */
    public void setManualPower(double power) {
        enabled = false;
        if (motors != null && motors.turret != null) {
            // Apply limits even for manual control
            int currentPosition = motors.turret.getCurrentPosition();
            power = enforceTurretLimits(currentPosition, power);
            motors.turret.setPower(power);
        }
    }

    /**
     * Get current turret angle relative to robot (degrees)
     */
    public double getTurretAngle() {
        if (localization == null) return 0;

        double angleToGoal = localization.getAngleToPoint(
            RobotConstants.Field.GOAL_X,
            RobotConstants.Field.GOAL_Y
        );
        double robotHeading = localization.getHeadingDegrees();
        double turretAngle = angleToGoal - robotHeading;

        while (turretAngle > 180) turretAngle -= 360;
        while (turretAngle <= -180) turretAngle += 360;

        return turretAngle;
    }

    /**
     * Get current turret encoder position
     */
    public int getTurretEncoderPosition() {
        if (motors != null && motors.turret != null) {
            return motors.turret.getCurrentPosition();
        }
        return 0;
    }

    /**
     * Get distance to goal in inches
     */
    public double getDistanceToGoal() {
        if (localization == null) return 0;
        return localization.getDistanceToPoint(
            RobotConstants.Field.GOAL_X,
            RobotConstants.Field.GOAL_Y
        );
    }

    /**
     * Check if turret is on target
     */
    public boolean isOnTarget() {
        return Math.abs(getTurretAngle()) < RobotConstants.Turret.DEADBAND_DEGREES;
    }

    /**
     * Check if turret is near rotation limits
     */
    public boolean isNearLimits() {
        if (motors == null || motors.turret == null) return false;
        int currentPosition = motors.turret.getCurrentPosition();
        return (currentPosition > MAX_ENCODER_POSITION - 100) ||
               (currentPosition < MIN_ENCODER_POSITION + 100);
    }

    /**
     * Reset turret to center position (0 encoder ticks)
     */
    public void resetToCenter() {
        if (motors == null || motors.turret == null) return;

        disable(); // Disable auto-tracking

        motors.turret.setMode(DcMotor.RunMode.RUN_TO_POSITION);
        motors.turret.setTargetPosition(0);
        motors.turret.setPower(0.3);
    }

    /**
     * Check if turret reset is complete
     */
    public boolean isResetComplete() {
        if (motors == null || motors.turret == null) return true;
        return !motors.turret.isBusy();
    }

    /**
     * Resume normal operation after reset
     */
    public void resumeFromReset() {
        if (motors == null || motors.turret == null) return;
        motors.turret.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        motors.turret.setPower(0);
    }

    /**
     * Stop turret motor
     */
    public void stop() {
        if (motors != null && motors.turret != null) {
            motors.turret.setPower(0);
        }
        enabled = false;
        previousError = 0;
        integral = 0;
    }
}
