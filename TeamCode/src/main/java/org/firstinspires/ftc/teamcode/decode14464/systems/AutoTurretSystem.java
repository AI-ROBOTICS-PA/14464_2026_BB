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
    private AutoIntakeSystem autoIntake;  // Reference to check launch/intake mode
    
    private boolean enabled = false;
    private boolean counterRotationEnabled = false;  // Counter-rotation mode
    private boolean combinedGoalTrackingEnabled = false;  // Combined: position + rotation tracking
    
    // ========== LAUNCH MODE ONLY CONTROL ==========
    // When true, turret tracks target but only MOVES when in launch mode
    // This prevents wire tangling during intake (turret stays still)
    private boolean launchModeOnly = true;  // DEFAULT: Only move in launch mode

    private double previousError = 0;
    private double integral = 0;

    // Counter-rotation tracking (for field-centric turret positioning)
    private double turretTargetPosition = 0;      // Target encoder position (can go beyond ±360°)
    private double lastHeading = 0;               // Last robot heading
    private double totalRobotRotation = 0;        // Total accumulated rotation
    private double lastTotalRotation = 0;         // For reversal detection
    private double previousHeadingChange = 0;     // For direction-change detection
    private boolean hasUnwound = false;           // Prevent repeated unwinding

    // Combined goal tracking state (position + rotation)
    private double lastX = 0;                     // Last robot X position
    private double lastY = 0;                     // Last robot Y position
    private double lastAngleToGoal = 0;           // Last calculated angle to goal

    // Local constants (use RobotConstants.Turret for shared values)
    private static final double SOFT_LIMIT_ZONE = RobotConstants.Turret.SOFT_LIMIT_DEGREES * RobotConstants.Turret.TICKS_PER_DEGREE;
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
        init(hardwareMap, localizationSystem, motors, null);
    }
    
    /**
     * Initialize turret system with AutoIntakeSystem reference
     * This allows turret to only move in LAUNCH mode (wire management)
     */
    public void init(HardwareMap hardwareMap, LocalizationSystem localizationSystem, 
                     RobotMotors motors, AutoIntakeSystem autoIntake) {
        this.motors = motors;
        this.localization = localizationSystem;
        this.autoIntake = autoIntake;

        // Ensure turret motor is properly initialized (encoder reset done in RobotMotors.init())
        if (motors != null && motors.turret != null) {
            motors.turret.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
            motors.turret.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        }

        // Initialize counter-rotation tracking
        resetCounterRotationTracking();
    }
    
    /**
     * Set reference to AutoIntakeSystem (for launch mode checking)
     * Call this after init if autoIntake wasn't available during init
     */
    public void setAutoIntakeSystem(AutoIntakeSystem autoIntake) {
        this.autoIntake = autoIntake;
    }
    
    /**
     * Enable/disable launch-mode-only movement
     * When true: Turret tracks target but only MOVES when in launch mode
     * When false: Turret moves regardless of intake/launch mode
     */
    public void setLaunchModeOnly(boolean launchModeOnly) {
        this.launchModeOnly = launchModeOnly;
    }
    
    /**
     * Check if launch-mode-only is enabled
     */
    public boolean isLaunchModeOnly() {
        return launchModeOnly;
    }
    
    /**
     * Check if turret movement is currently allowed
     * @return true if movement allowed (launch mode or launchModeOnly disabled)
     */
    public boolean isMovementAllowed() {
        if (!launchModeOnly) return true;  // Always allowed if feature disabled
        if (autoIntake == null) return true;  // Allow if no intake system reference
        return !autoIntake.isIntakeMode();  // Only allow in launch mode
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
     * Disable auto-tracking (all modes)
     */
    public void disable() {
        enabled = false;
        counterRotationEnabled = false;
        combinedGoalTrackingEnabled = false;
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

    // ==================== COMBINED GOAL TRACKING MODE ====================
    // This mode tracks the goal based on robot's X,Y position AND handles rotation
    // Turret will always point at the goal regardless of where robot moves or rotates

    /**
     * Enable combined goal tracking mode
     * This is the RECOMMENDED mode for competition - turret always points at goal
     * Handles both:
     * - Position changes (robot moving on field)
     * - Rotation changes (robot spinning)
     * Uses unlimited rotation with wire protection
     */
    public void enableCombinedGoalTracking() {
        combinedGoalTrackingEnabled = true;
        counterRotationEnabled = false;
        enabled = false;
        resetCombinedGoalTracking();
    }

    /**
     * Disable combined goal tracking mode
     */
    public void disableCombinedGoalTracking() {
        combinedGoalTrackingEnabled = false;
        if (motors != null && motors.turret != null) {
            motors.turret.setPower(0);
        }
    }

    /**
     * Check if combined goal tracking is enabled
     */
    public boolean isCombinedGoalTrackingEnabled() {
        return combinedGoalTrackingEnabled;
    }

    /**
     * Reset combined goal tracking state
     */
    private void resetCombinedGoalTracking() {
        if (localization != null) {
            lastHeading = localization.getHeadingDegrees();
            lastX = localization.getX();
            lastY = localization.getY();
            lastAngleToGoal = localization.getAngleToPoint(
                RobotConstants.Field.GOAL_X,
                RobotConstants.Field.GOAL_Y
            );
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
     * Recalibrate turret position - call this when manually adjusted to correct angle
     * This tells the auto-tracking system "current position is the correct target"
     * Use this to correct for any tracking errors during a match
     */
    public void recalibratePosition() {
        if (motors != null && motors.turret != null) {
            // Set target position to current actual position (zero out error)
            turretTargetPosition = motors.turret.getCurrentPosition();
        }
        // Reset tracking references to current state
        if (localization != null) {
            lastHeading = localization.getHeadingDegrees();
            lastX = localization.getX();
            lastY = localization.getY();
            lastAngleToGoal = localization.getAngleToPoint(
                RobotConstants.Field.GOAL_X,
                RobotConstants.Field.GOAL_Y
            );
        }
        // Reset PID state
        previousError = 0;
        integral = 0;
        // Reset rotation tracking
        totalRobotRotation = 0;
        lastTotalRotation = 0;
        previousHeadingChange = 0;
    }

    /**
     * Calibrate turret from Limelight AprilTag detection
     * Uses tx (horizontal angle) to correct turret pointing
     * @param limelightTx Horizontal angle to target from Limelight (degrees, + = right, - = left)
     * @return true if calibration was applied
     */
    public boolean calibrateFromLimelight(double limelightTx) {
        if (motors == null || motors.turret == null) {
            return false;
        }
        
        // If Limelight sees target at tx degrees offset, turret needs to move by that amount
        // Positive tx = target is to the right = turret needs to turn right (+ encoder)
        // Negative tx = target is to the left = turret needs to turn left (- encoder)
        
        // Convert tx angle to encoder ticks offset
        double offsetTicks = limelightTx * TICKS_PER_DEGREE;
        
        // Adjust target position to account for the offset
        // Current position + offset = where we should be pointing
        turretTargetPosition = motors.turret.getCurrentPosition() + offsetTicks;
        
        // Reset PID state
        previousError = 0;
        integral = 0;
        
        return true;
    }

    /**
     * Get turret error from Limelight (how far off we are)
     * @param limelightTx Horizontal angle to target from Limelight
     * @return Absolute error in degrees
     */
    public double getLimelightError(double limelightTx) {
        return Math.abs(limelightTx);
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
     * Supports three modes:
     * 1. Goal-tracking: Points turret at goal (with ±150° limits) - SIMPLE
     * 2. Counter-rotation: Maintains field orientation (unlimited rotation with wire safety)
     * 3. Combined Goal Tracking: Always points at goal based on X,Y position AND rotation - RECOMMENDED
     */
    public void update() {
        // Null safety check - verify all required components exist
        if (motors == null || motors.turret == null || localization == null) {
            return;
        }

        if (combinedGoalTrackingEnabled) {
            updateCombinedGoalTracking();
        } else if (counterRotationEnabled) {
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
     * Check if turret is AT its rotation limit (goal is behind robot)
     * This means the turret can't reach the goal angle and is at max/min position
     */
    public boolean isAtLimit() {
        if (motors == null || motors.turret == null) return false;
        double currentDegrees = motors.turret.getCurrentPosition() / TICKS_PER_DEGREE;
        double maxRotation = RobotConstants.Turret.MAX_ROTATION_DEGREES;
        // Check if at either limit (within 5 degrees)
        return Math.abs(Math.abs(currentDegrees) - maxRotation) < 5.0;
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
     * Move turret to a specific angle (degrees from center)
     * Use this at start of autonomous to pre-position turret
     * 
     * @param angleDegrees Target angle in degrees (+ = right, - = left)
     *                     Will be clamped to ±MAX_ROTATION_DEGREES
     */
    public void moveToAngle(double angleDegrees) {
        if (motors == null || motors.turret == null) return;
        
        // Clamp to safe limits
        double maxRotation = RobotConstants.Turret.MAX_ROTATION_DEGREES;
        angleDegrees = Math.max(-maxRotation, Math.min(maxRotation, angleDegrees));
        
        // Convert to encoder ticks
        int targetTicks = (int)(angleDegrees * TICKS_PER_DEGREE);
        
        // Move to position
        motors.turret.setMode(DcMotor.RunMode.RUN_TO_POSITION);
        motors.turret.setTargetPosition(targetTicks);
        motors.turret.setPower(0.5); // Faster than reset for auto
    }
    
    /**
     * Check if turret has reached target position (for moveToAngle)
     */
    public boolean isAtTargetPosition() {
        if (motors == null || motors.turret == null) return true;
        return !motors.turret.isBusy();
    }
    
    /**
     * Get current turret position in degrees (from encoder)
     */
    public double getCurrentAngleDegrees() {
        if (motors == null || motors.turret == null) return 0;
        return motors.turret.getCurrentPosition() / TICKS_PER_DEGREE;
    }

    /**
     * Check if turret reset is complete
     */
    public boolean isResetComplete() {
        if (motors == null || motors.turret == null) return true;
        return !motors.turret.isBusy();
    }

    /**
     * Resume normal operation after reset or moveToAngle
     * Call this before enabling auto-tracking
     */
    public void resumeFromReset() {
        if (motors == null || motors.turret == null) return;
        motors.turret.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        motors.turret.setPower(0);
    }

    /**
     * Stop turret motor (disables all modes)
     */
    public void stop() {
        if (motors != null && motors.turret != null) {
            motors.turret.setPower(0);
        }
        enabled = false;
        counterRotationEnabled = false;
        combinedGoalTrackingEnabled = false;
        previousError = 0;
        integral = 0;
    }

    // ==================== COMBINED GOAL TRACKING UPDATE ====================

    /**
     * Update combined goal tracking mode
     * 
     * WIRE-SAFE IMPLEMENTATION:
     * Instead of accumulating rotation (which fails with hard limits), this calculates
     * the DIRECT angle to goal and clamps it within the configured rotation limits.
     * 
     * This means:
     * - Robot can spin unlimited times (360° x N) and turret always finds the goal
     * - Turret never exceeds ±MAX_ROTATION_DEGREES (configurable, default ±135° = 270° total)
     * - If goal is directly behind robot (outside ±135°), turret goes to closest limit
     */
    private void updateCombinedGoalTracking() {
        // Get current robot state
        double currentX = localization.getX();
        double currentY = localization.getY();
        double currentHeading = localization.getHeadingDegrees();

        // Calculate angle to goal (field-relative, in degrees)
        double fieldAngleToGoal = localization.getAngleToPoint(
            RobotConstants.Field.GOAL_X,
            RobotConstants.Field.GOAL_Y
        );

        // ========== CALCULATE TURRET ANGLE RELATIVE TO ROBOT ==========
        // This is the angle the turret needs to point, relative to robot's heading
        double turretAngleNeeded = fieldAngleToGoal - currentHeading;
        
        // Normalize to -180 to +180
        turretAngleNeeded = normalizeAngle(turretAngleNeeded);
        
        // ========== APPLY ROTATION LIMITS (WIRE SAFETY) ==========
        // Clamp to configured limits (default ±135°)
        double maxRotation = RobotConstants.Turret.MAX_ROTATION_DEGREES;
        
        if (turretAngleNeeded > maxRotation) {
            // Goal is too far right - go to right limit
            turretAngleNeeded = maxRotation;
            // Mark as "at limit" for telemetry
        } else if (turretAngleNeeded < -maxRotation) {
            // Goal is too far left - go to left limit  
            turretAngleNeeded = -maxRotation;
        }
        
        // Convert angle to encoder ticks
        turretTargetPosition = turretAngleNeeded * TICKS_PER_DEGREE;
        
        // Track for telemetry
        lastAngleToGoal = fieldAngleToGoal;
        lastHeading = currentHeading;
        lastX = currentX;
        lastY = currentY;
        
        // ========== RUN PID TO TARGET POSITION ==========
        runTurretToPosition();
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

        // ========== LAUNCH MODE ONLY CHECK ==========
        // If launchModeOnly is enabled and we're in intake mode, don't move motor
        // But tracking calculation above already updated turretTargetPosition
        if (!isMovementAllowed()) {
            motors.turret.setPower(0);
            // Keep PID state for smooth transition when switching to launch mode
            previousError = error;
            return;
        }

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

    /**
     * Get the last calculated angle to goal (for telemetry/debugging)
     */
    public double getLastAngleToGoal() {
        return lastAngleToGoal;
    }

    /**
     * Get robot position for debugging
     */
    public double[] getRobotPosition() {
        if (localization == null) return new double[]{0, 0};
        return new double[]{localization.getX(), localization.getY()};
    }

    /**
     * Get current tracking mode as string (for telemetry)
     */
    public String getCurrentMode() {
        if (combinedGoalTrackingEnabled) return "COMBINED_GOAL_TRACKING";
        if (counterRotationEnabled) return "COUNTER_ROTATION";
        if (enabled) return "GOAL_TRACKING";
        return "DISABLED";
    }

    /**
     * Check if any auto mode is active
     */
    public boolean isAnyModeEnabled() {
        return enabled || counterRotationEnabled || combinedGoalTrackingEnabled;
    }
    
    /**
     * Get tracking status for telemetry
     * Shows mode, tracking vs moving state, target angle
     */
    public String getTrackingStatus() {
        if (!isAnyModeEnabled()) return "DISABLED";
        
        String mode = getCurrentMode();
        boolean moving = isMovementAllowed();
        double targetDeg = getTurretTargetDegrees();
        double currentDeg = getTurretEncoderPosition() / TICKS_PER_DEGREE;
        double error = targetDeg - currentDeg;
        
        if (moving) {
            return String.format("%s: MOVING (err=%.1f°)", mode, error);
        } else {
            return String.format("%s: TRACKING (target=%.1f°, wait for LAUNCH)", mode, targetDeg);
        }
    }
    
    /**
     * Get pending angle that turret will move to when switching to launch mode
     * Useful for telemetry to show "turret will snap to X° when entering LAUNCH"
     */
    public double getPendingTargetDegrees() {
        return turretTargetPosition / TICKS_PER_DEGREE;
    }
    
    /**
     * Get error between current position and tracked target (degrees)
     * This shows how far turret needs to move when switching to launch mode
     */
    public double getPendingMovementDegrees() {
        if (motors == null || motors.turret == null) return 0;
        double currentDeg = motors.turret.getCurrentPosition() / TICKS_PER_DEGREE;
        double targetDeg = turretTargetPosition / TICKS_PER_DEGREE;
        return targetDeg - currentDeg;
    }

}
