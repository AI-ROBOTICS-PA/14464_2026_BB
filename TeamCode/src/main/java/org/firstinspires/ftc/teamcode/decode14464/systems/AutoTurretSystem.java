package org.firstinspires.ftc.teamcode.decode14464.systems;

import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.HardwareMap;

/**
 * Feature 2: Auto-Turret System
 * Automatically points turret motor toward the goal using localization
 */
public class AutoTurretSystem {
    private DcMotorEx turretMotor;
    private LocalizationSystem localization;
    private boolean enabled = false;

    private double previousError = 0;

    /**
     * Initialize turret system
     */
    public void init(HardwareMap hardwareMap, LocalizationSystem localizationSystem) {
        turretMotor = hardwareMap.get(DcMotorEx.class, "turret");
        turretMotor.setMode(DcMotorEx.RunMode.RUN_WITHOUT_ENCODER);
        turretMotor.setZeroPowerBehavior(DcMotorEx.ZeroPowerBehavior.BRAKE);

        this.localization = localizationSystem;
    }

    /**
     * Enable auto-tracking
     */
    public void enable() {
        enabled = true;
        previousError = 0;
    }

    /**
     * Disable auto-tracking
     */
    public void disable() {
        enabled = false;
        if (turretMotor != null) {
            turretMotor.setPower(0);
        }
        previousError = 0;
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
     */
    public void update() {
        if (!enabled || turretMotor == null || localization == null) {
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

        // Apply PID control
        double error = turretAngle;

        // Deadband - stop if close enough
        if (Math.abs(error) < RobotConstants.Turret.DEADBAND_DEGREES) {
            turretMotor.setPower(0);
            previousError = 0;
            return;
        }

        // Calculate derivative
        double derivative = error - previousError;

        // PD control
        double power = (error * RobotConstants.Turret.KP) + (derivative * RobotConstants.Turret.KD);

        // Clamp power
        power = Math.max(RobotConstants.Turret.MIN_POWER,
                        Math.min(RobotConstants.Turret.MAX_POWER, power));

        // Apply power
        turretMotor.setPower(power);

        // Save error for next iteration
        previousError = error;
    }

    /**
     * Set turret power manually (disables auto-tracking)
     */
    public void setManualPower(double power) {
        enabled = false;
        if (turretMotor != null) {
            turretMotor.setPower(power);
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
     * Stop turret motor
     */
    public void stop() {
        if (turretMotor != null) {
            turretMotor.setPower(0);
        }
        enabled = false;
        previousError = 0;
    }
}

