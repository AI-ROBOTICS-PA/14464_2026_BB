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
    private boolean counterRotationEnabled = false;  // Counter-rotation mode

    private double previousError = 0;
    private double integral = 0;

    // Counter-rotation tracking (for field-centric turret positioning)
    private double turretTargetPosition = 0;      // Target encoder position (can go beyond ±360°)
    private double lastHeading = 0;               // Last robot heading
    private double totalRobotRotation = 0;        // Total accumulated rotation
    private double lastTotalRotation = 0;         // For reversal detection
    private double previousHeadingChange = 0;     // For direction-change detection
    private boolean hasUnwound = false;           // Prevent repeated unwinding

    // Local constants (use RobotConstants.Turret for shared values)
    private static final double SOFT_LIMIT_ZONE = 50;         // Slow down within 50 ticks of limit
    private static final double POSITION_TOLERANCE = 12;      // ~4° accuracy
    
    // Shorthand references to RobotConstants.Turret for cleaner code
    private static final double TICKS_PER_DEGREE = RobotConstants.Turret.TICKS_PER_DEGREE;
    private static final int MAX_ENCODER_POSITION = RobotConstants.Turret.MAX_ENCODER_POSITION;
    private static final int MIN_ENCODER_POSITION = RobotConstants.Turret.MIN_ENCODER_POSITION;
    private static final double WIRE_LIMIT = RobotConstants.Turret.WIRE_LIMIT_DEGREES;
    private static final double UNWIND_HYSTERESIS = RobotConstants.Turret.UNWIND_HYSTERESIS;
    private static final double KP = RobotConstants.Turret.KP;
    private static final double KI = RobotConstants.Turret.KI;
    private static final double KD = RobotConstants.Turret.KD;
    private static final double MAX_POWER = RobotConstants.Turret.MAX_POWER;

    /**
     * Initialize turret system with centralized motors
     */
    public void init(HardwareMap hardwareMap, LocalizationSystem localizationSystem, RobotMotors motors) {
        this.motors = motors;
        this.localization = localizationSystem;

        // Ensure turret motor is properly initialized (encoder reset done in RobotMotors.init())
        if (motors != null && motors.turret != null) {
            motors.turret.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
            motors.turret.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        }

        // Initialize counter-rotation tracking
        resetCounterRotationTracking();
    }

    /**
     * Enable auto-tracking (goal-relative mode)
     */
    public void enable() {
        enabled = true;
        counterRotationEnabled = false;
        previousError = 0;
        integral = 0;
    }

    /**
     * Disable auto-tracking
     */
    public void disable() {
        enabled = false;
        counterRotationEnabled = false;
        if (motors != null && motors.turret != null) {
            motors.turret.setPower(0);
        }
        previousError = 0;
        integral = 0;
    }

    /**
     * Enable counter-rotation mode (field-centric turret)
     * Turret maintains its field orientation as robot rotates
     */
    public void enableCounterRotation() {
        counterRotationEnabled = true;
        enabled = false;  // Disable goal-tracking
        resetCounterRotationTracking();
    }

    /**
     * Disable counter-rotation mode
     */
    public void disableCounterRotation() {
        counterRotationEnabled = false;
        if (motors != null && motors.turret != null) {
            motors.turret.setPower(0);
        }
    }

    /**
     * Check if counter-rotation is enabled
     */
    public boolean isCounterRotationEnabled() {
        return counterRotationEnabled;
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
     * Supports two modes:
     * 1. Goal-tracking: Points turret at goal (with ±150° limits)
     * 2. Counter-rotation: Maintains field orientation (unlimited rotation with wire safety)
     */
    public void update() {
        // Null safety check - verify all required components exist
        if (motors == null || motors.turret == null || localization == null) {
            return;
        }

        if (counterRotationEnabled) {
            updateCounterRotation();
        } else if (enabled) {
            updateGoalTracking();
        }
    }

    /**
     * Update goal-tracking mode (original behavior with limits)
     */
    private void updateGoalTracking() {
        // Calculate angle to goal
        double angleToGoal = localization.getAngleToPoint(
            RobotConstants.Field.GOAL_X,
            RobotConstants.Field.GOAL_Y
        );

        // Get robot heading
        double robotHeading = localization.getHeadingDegrees();

        // Calculate turret angle (relative to robot)
        double turretAngle = normalizeAngle(angleToGoal - robotHeading);

        // SAFETY CHECK: Limit turret angle to reasonable range
        if (Math.abs(turretAngle) > 120) {
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
        if (KI != 0) {
            integral += error;
            // Anti-windup
            double integralLimit = 500;
            integral = Math.max(-integralLimit, Math.min(integralLimit, integral));
        }

        double derivative = error - previousError;

        // PID control
        double power = (error * RobotConstants.Turret.KP) +
                      (integral * RobotConstants.Turret.KI) +
                      (derivative * RobotConstants.Turret.KD);

        // Get current encoder position for limit checking
        int currentPosition = motors.turret.getCurrentPosition();

        // Enforce encoder limits (hard stops at ±150°)
        power = enforceTurretLimits(currentPosition, power);

        // Clamp power
        power = Math.max(-RobotConstants.Turret.MAX_POWER,
                        Math.min(RobotConstants.Turret.MAX_POWER, power));

        // Apply minimum power threshold
        if (Math.abs(power) > 0.01 && Math.abs(power) < 0.1) {
            power = Math.signum(power) * 0.1;
        }

        motors.turret.setPower(power);
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
        
        return normalizeAngle(angleToGoal - robotHeading);
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
        counterRotationEnabled = false;
        previousError = 0;
        integral = 0;
    }

    // ==================== COUNTER-ROTATION MODE ====================

    /**
     * Update counter-rotation mode
     * Turret rotates opposite to robot rotation to maintain field orientation
     */
    private void updateCounterRotation() {
        double currentHeading = localization.getHeadingDegrees();
        double rawHeadingChange = currentHeading - lastHeading;

        // Normalize heading change for boundary crossings
        double headingChange = rawHeadingChange;
        if (rawHeadingChange > 180) {
            headingChange = rawHeadingChange - 360;
        } else if (rawHeadingChange < -180) {
            headingChange = rawHeadingChange + 360;
        }

        // Track total rotation
        totalRobotRotation += headingChange;

        // Counter-rotate: if robot turns right (+), turret turns left (-)
        turretTargetPosition -= headingChange * TICKS_PER_DEGREE;

        // REVERSAL DETECTION: Check if direction changed
        boolean directionChanged = (previousHeadingChange != 0) &&
                                   (Math.signum(headingChange) != Math.signum(previousHeadingChange)) &&
                                   (Math.abs(headingChange) > 1.0);

        // Reset unwind flag when we complete another full rotation
        if (Math.abs(totalRobotRotation) > 360) {
            hasUnwound = false;
        }

        // If direction reversed and haven't unwound yet, unwind turret
        if (directionChanged && !hasUnwound && Math.abs(lastTotalRotation) > 300) {
            double currentTurretDegrees = turretTargetPosition / TICKS_PER_DEGREE;

            if (Math.abs(currentTurretDegrees) > 20) {
                if (currentTurretDegrees > 0) {
                    turretTargetPosition -= 360.0 * TICKS_PER_DEGREE;
                } else {
                    turretTargetPosition += 360.0 * TICKS_PER_DEGREE;
                }
                hasUnwound = true;
            }
        }

        // WIRE LIMIT PROTECTION: Unwind at ±370° (with hysteresis)
        double currentTurretDegrees = turretTargetPosition / TICKS_PER_DEGREE;
        if (currentTurretDegrees > WIRE_LIMIT + UNWIND_HYSTERESIS) {
            turretTargetPosition -= 360.0 * TICKS_PER_DEGREE;
        } else if (currentTurretDegrees < -(WIRE_LIMIT + UNWIND_HYSTERESIS)) {
            turretTargetPosition += 360.0 * TICKS_PER_DEGREE;
        }

        // Run PID to target position
        runTurretToPosition();

        // Update tracking variables
        lastHeading = currentHeading;
        lastTotalRotation = totalRobotRotation;
        
        // Only update previous heading change if significant movement (prevents noise)
        if (Math.abs(headingChange) > 0.5) {
            previousHeadingChange = headingChange;
        }
    }

    /**
     * Run turret motor to target position using optimized PID control
     */
    private void runTurretToPosition() {
        double currentPosition = motors.turret.getCurrentPosition();
        double error = turretTargetPosition - currentPosition;

        // Check if at target
        if (Math.abs(error) <= POSITION_TOLERANCE) {
            motors.turret.setPower(0);
            previousError = 0;
            integral = 0;
            return;
        }

        // PID calculation (KI = 0, so skip integral)
        if (KI != 0) {
            integral += error;
            double integralLimit = 500;
            integral = Math.max(-integralLimit, Math.min(integralLimit, integral));
        }

        double derivative = error - previousError;
        double power = (KP * error) + (KI * integral) + (KD * derivative);

        // Clamp power
        power = Math.max(-MAX_POWER, Math.min(MAX_POWER, power));

        // Minimum power threshold
        double minPower = 0.06;
        if (Math.abs(power) > 0.01 && Math.abs(power) < minPower) {
            power = Math.signum(power) * minPower;
        }

        // Apply smooth power changes (prevent jerky motion)
        double maxPowerChange = 0.10;
        double lastPower = motors.turret.getPower();
        double powerChange = power - lastPower;

        if (Math.abs(powerChange) > maxPowerChange) {
            power = lastPower + Math.signum(powerChange) * maxPowerChange;
        }

        motors.turret.setPower(power);
        previousError = error;
    }

    /**
     * Reset counter-rotation tracking
     */
    private void resetCounterRotationTracking() {
        if (localization != null) {
            lastHeading = localization.getHeadingDegrees();
        }
        totalRobotRotation = 0;
        lastTotalRotation = 0;
        previousHeadingChange = 0;
        hasUnwound = false;
        if (motors != null && motors.turret != null) {
            turretTargetPosition = motors.turret.getCurrentPosition();
        }
        previousError = 0;
        integral = 0;
    }

    /**
     * Normalize angle to -180 to +180 range (efficient modulo version)
     */
    private double normalizeAngle(double angle) {
        angle = angle % 360;
        if (angle > 180) {
            angle -= 360;
        } else if (angle < -180) {
            angle += 360;
        }
        return angle;
    }

    /**
     * Get total robot rotation (for telemetry/debugging)
     */
    public double getTotalRobotRotation() {
        return totalRobotRotation;
    }

    /**
     * Get turret target position in degrees (for telemetry/debugging)
     */
    public double getTurretTargetDegrees() {
        return turretTargetPosition / TICKS_PER_DEGREE;
    }

}
