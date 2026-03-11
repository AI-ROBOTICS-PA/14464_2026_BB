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
    private boolean holdPositionEnabled = false;  // Hold turret at current encoder position using PID
    private boolean paused = false;  // When true, update() skips motor control (for manual override)
    
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
    
    // Manual override offset (in encoder ticks)
    // When operator manually adjusts turret, the difference between manual position
    // and auto-calculated position is saved here. Applied every update so the
    // correction persists while auto-tracking continues.
    // Includes BOTH the initial program offset AND any manual D-pad adjustments.
    private double manualOffsetTicks = 0;
    
    // Initial offset set by auto program (encoder 0 ≠ physical 0)
    // Used for limit calculations and telemetry
    private double initialOffsetDegrees = 0;

    // ========== ALLIANCE-BASED GOAL TRACKING ==========
    // Goal position changes based on alliance (Blue vs Red)
    private double goalX = RobotConstants.Field.BLUE_GOAL_X;  // Default to Blue
    private double goalY = RobotConstants.Field.BLUE_GOAL_Y;

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
     * Set goal position for Blue Alliance
     */
    public void setBlueAlliance() {
        goalX = RobotConstants.Field.BLUE_GOAL_X;
        goalY = RobotConstants.Field.BLUE_GOAL_Y;
    }

    /**
     * Set goal position for Red Alliance
     */
    public void setRedAlliance() {
        goalX = RobotConstants.Field.RED_GOAL_X;
        goalY = RobotConstants.Field.RED_GOAL_Y;
    }

    /**
     * Set goal position directly (for custom targets)
     */
    public void setGoalPosition(double x, double y) {
        goalX = x;
        goalY = y;
    }

    /**
     * Get current goal X coordinate
     */
    public double getGoalX() { return goalX; }

    /**
     * Get current goal Y coordinate
     */
    public double getGoalY() { return goalY; }

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
        // Switch motor back to software-PID mode.
        // moveToAngle() (used in Auto INIT) leaves the motor in RUN_TO_POSITION,
        // which causes the hardware controller to ignore setPower() calls and keep
        // driving to encoder 0. This must be cleared before any software PID runs.
        if (motors != null && motors.turret != null) {
            motors.turret.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        }
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
     * Hold turret at its current encoder position using PID.
     * Use during auto when no tracking mode is active but the turret must not drift.
     * The turret will actively resist movement from robot acceleration/deceleration.
     */
    public void holdCurrentPosition() {
        holdPositionEnabled = true;
        // Target = current encoder position (should be 0 after init reset)
        if (motors != null && motors.turret != null) {
            turretTargetPosition = motors.turret.getCurrentPosition();
        }
        previousError = 0;
        integral = 0;
    }

    /**
     * Stop holding position
     */
    public void stopHoldPosition() {
        holdPositionEnabled = false;
    }

    /**
     * Pause auto-turret updates without changing the enabled/tracking state.
     * Use for manual override - auto-turret stays "enabled" but stops controlling the motor.
     * Call resume() when manual control is done to continue tracking from the new position.
     */
    public void pause() {
        paused = true;
    }

    /**
     * Resume auto-turret updates after a pause.
     * Automatically recalibrates to current position so tracking continues smoothly.
     */
    public void resume() {
        if (paused) {
            paused = false;
            // Recalibrate: set target to current position so tracking resumes from here
            recalibratePosition();
        }
    }

    /**
     * Check if auto-turret is paused (for manual override)
     */
    public boolean isPaused() {
        return paused;
    }

    /**
     * Get the current manual offset in degrees
     * Positive = turret is manually shifted right of auto-target
     */
    public double getManualOffsetDegrees() {
        return manualOffsetTicks / TICKS_PER_DEGREE;
    }

    /**
     * Clear MANUAL adjustments but keep initial program offset.
     * Use this after Limelight corrects robot position - the auto-tracking
     * calculation should now be accurate, so manual D-pad corrections are no longer needed.
     * The initial program offset (e.g., 42° for Blue Far) is preserved.
     */
    public void clearManualOffset() {
        // Reset to initial offset only (remove any D-pad or Limelight adjustments)
        manualOffsetTicks = -initialOffsetDegrees * TICKS_PER_DEGREE;
        previousError = 0;
        integral = 0;
    }

    /**
     * Set initial turret offset for manual pre-positioning.
     * User manually aims turret before init. Encoder resets to 0 but turret
     * is physically at this angle. This offset is preserved through auto-tracking.
     *
     * Call this AFTER enableCombinedGoalTracking() — it overrides the cleared offset.
     *
     * @param angleDegrees Physical angle turret is at (e.g., 42 for Blue Far)
     */
    public void setInitialOffset(double angleDegrees) {
        initialOffsetDegrees = angleDegrees;
        // Offset makes auto-tracking produce encoder target = 0 when goal needs angleDegrees
        // Formula: target = turretAngleNeeded * TPD + manualOffsetTicks
        // When turretAngleNeeded = angleDegrees and target should = 0:
        //   0 = angleDegrees * TPD + manualOffsetTicks
        //   manualOffsetTicks = -angleDegrees * TPD
        manualOffsetTicks = -angleDegrees * TICKS_PER_DEGREE;
        turretTargetPosition = 0; // Current encoder position
        previousError = 0;
        integral = 0;
    }
    
    /**
     * Get the initial offset in degrees (set by auto program)
     */
    public double getInitialOffsetDegrees() {
        return initialOffsetDegrees;
    }
    
    /**
     * Get the current manual offset in ticks (for saving between OpModes)
     */
    public double getManualOffsetTicks() {
        return manualOffsetTicks;
    }
    
    /**
     * Restore offset from previous OpMode (Auto → TeleOp).
     * Sets both manualOffsetTicks and initialOffsetDegrees from saved values.
     */
    public void restoreOffset(double offsetTicks, double initialDegrees) {
        this.manualOffsetTicks = offsetTicks;
        this.initialOffsetDegrees = initialDegrees;
        turretTargetPosition = (motors != null && motors.turret != null)
            ? motors.turret.getCurrentPosition() : 0;
        previousError = 0;
        integral = 0;
    }
    
    /**
     * Get the PHYSICAL turret angle (encoder + offset).
     * This is the true angle the turret is pointing at, accounting for
     * the initial offset from manual pre-positioning.
     */
    public double getPhysicalAngleDegrees() {
        if (motors == null || motors.turret == null) return initialOffsetDegrees;
        double encoderDegrees = motors.turret.getCurrentPosition() / TICKS_PER_DEGREE;
        return encoderDegrees + initialOffsetDegrees;
    }
    
    /**
     * Hard reset: User physically moves turret to center, then calls this.
     * Resets encoder to 0, clears ALL offsets, resets PID.
     * After this, encoder 0 = physical center (0°).
     */
    public void hardReset() {
        if (motors == null || motors.turret == null) return;
        
        // Reset encoder to 0
        motors.turret.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        motors.turret.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        motors.turret.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        
        // Clear ALL offsets
        manualOffsetTicks = 0;
        initialOffsetDegrees = 0;
        turretTargetPosition = 0;
        
        // Reset PID
        previousError = 0;
        integral = 0;
        
        // Reset tracking state
        totalRobotRotation = 0;
        lastTotalRotation = 0;
        previousHeadingChange = 0;
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
                goalX,
                goalY
            );
        }
        totalRobotRotation = 0;
        lastTotalRotation = 0;
        previousHeadingChange = 0;
        hasUnwound = false;
        manualOffsetTicks = 0;  // Clear offset on fresh enable (setInitialOffset() sets it after)
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
        if (motors != null && motors.turret != null && localization != null) {
            // Calculate where auto-turret THINKS the target should be (goal angle)
            double currentHeading = localization.getHeadingDegrees();
            double fieldAngleToGoal = localization.getAngleToPoint(
                goalX,
                goalY
            );
            double autoTargetAngle = normalizeAngle(fieldAngleToGoal - currentHeading);
            
            // Clamp to limits (same as updateCombinedGoalTracking)
            double maxRotation = RobotConstants.Turret.MAX_ROTATION_DEGREES;
            autoTargetAngle = Math.max(-maxRotation, Math.min(maxRotation, autoTargetAngle));
            
            double autoTargetTicks = autoTargetAngle * TICKS_PER_DEGREE;
            
            // Calculate offset: difference between where turret IS vs where auto wants it
            double currentEncoderPosition = motors.turret.getCurrentPosition();
            manualOffsetTicks = currentEncoderPosition - autoTargetTicks;
            
            // Set target to current position (zero out PID error)
            turretTargetPosition = currentEncoderPosition;
        }
        // Reset tracking references to current state
        if (localization != null) {
            lastHeading = localization.getHeadingDegrees();
            lastX = localization.getX();
            lastY = localization.getY();
            lastAngleToGoal = localization.getAngleToPoint(
                goalX,
                goalY
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
     * Calibrate turret from Limelight AprilTag detection.
     * Converts the tx offset into a persistent manualOffsetTicks value so the
     * correction survives auto-tracking recalculations every loop.
     * 
     * IMPORTANT: Call this AFTER position correction (calibratePositionOnly) so that
     * the auto-tracking base calculation uses the corrected robot position.
     * 
     * @param limelightTx Horizontal angle to target from Limelight (degrees, + = right, - = left)
     * @return true if calibration was applied
     */
    public boolean calibrateFromLimelight(double limelightTx) {
        if (motors == null || motors.turret == null || localization == null) {
            return false;
        }
        
        // Calculate where auto-tracking THINKS turret should be (using now-corrected position)
        double currentHeading = localization.getHeadingDegrees();
        double fieldAngleToGoal = localization.getAngleToPoint(goalX, goalY);
        double autoTargetAngle = normalizeAngle(fieldAngleToGoal - currentHeading);
        double maxRotation = RobotConstants.Turret.MAX_ROTATION_DEGREES;
        autoTargetAngle = Math.max(-maxRotation, Math.min(maxRotation, autoTargetAngle));
        double autoTargetTicks = autoTargetAngle * TICKS_PER_DEGREE;
        
        // The "ideal" position = where turret IS now + tx correction
        // This is where the turret SHOULD be pointing to hit the tag
        double currentEncoder = motors.turret.getCurrentPosition();
        double idealPosition = currentEncoder + limelightTx * TICKS_PER_DEGREE;
        
        // Store as manualOffset: difference between ideal and auto-calculated target
        // This persists through every updateCombinedGoalTracking() call
        manualOffsetTicks = idealPosition - autoTargetTicks;
        
        // Set immediate target so PID starts driving there now
        turretTargetPosition = idealPosition;
        
        // Reset PID state for smooth transition
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

        // Skip motor control when paused (manual override active)
        // Tracking state is preserved so we can resume smoothly
        if (paused) {
            return;
        }

        if (combinedGoalTrackingEnabled) {
            updateCombinedGoalTracking();
        } else if (counterRotationEnabled) {
            updateCounterRotation();
        } else if (enabled) {
            updateGoalTracking();
        } else if (holdPositionEnabled) {
            // Actively hold turret at target position using PID (prevents drift during driving)
            runTurretToPosition();
        }
    }

    /**
     * Update goal-tracking mode (original behavior with limits)
     */
    private void updateGoalTracking() {
        // Calculate angle to goal
        double angleToGoal = localization.getAngleToPoint(
            goalX,
            goalY
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
     * Uses PHYSICAL angle (encoder + initialOffset) so limits are correct
     * regardless of where the encoder was zeroed during auto init.
     * (Matches DecodeTeleOp.enforceTurretLimitsForManual approach)
     */
    private double enforceTurretLimits(double currentEncoderPosition, double requestedPower) {
        // Convert raw encoder to physical degrees (accounts for initial offset)
        double physicalDegrees = currentEncoderPosition / TICKS_PER_DEGREE + initialOffsetDegrees;
        double maxRotation = RobotConstants.Turret.MAX_ROTATION_DEGREES;
        double softLimitDegrees = RobotConstants.Turret.SOFT_LIMIT_DEGREES;

        // Hard limit - stop at +MAX physical rotation
        if (physicalDegrees >= maxRotation && requestedPower > 0) {
            return 0.0;
        }

        // Hard limit - stop at -MAX physical rotation
        if (physicalDegrees <= -maxRotation && requestedPower < 0) {
            return 0.0;
        }

        // Soft limit - slow down approaching +MAX
        if (physicalDegrees > (maxRotation - softLimitDegrees) && requestedPower > 0) {
            double reduction = (maxRotation - physicalDegrees) / softLimitDegrees;
            requestedPower *= Math.max(0.1, reduction);
        }

        // Soft limit - slow down approaching -MAX
        if (physicalDegrees < (-maxRotation + softLimitDegrees) && requestedPower < 0) {
            double reduction = (physicalDegrees - (-maxRotation)) / softLimitDegrees;
            requestedPower *= Math.max(0.1, reduction);
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
            goalX,
            goalY
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
            goalX,
            goalY
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
     * Uses PHYSICAL angle (accounts for initial offset)
     */
    public boolean isAtLimit() {
        if (motors == null || motors.turret == null) return false;
        double physicalDegrees = getPhysicalAngleDegrees();
        double maxRotation = RobotConstants.Turret.MAX_ROTATION_DEGREES;
        // Check if at either limit (within 5 degrees)
        return Math.abs(Math.abs(physicalDegrees) - maxRotation) < 5.0;
    }
    
    /**
     * Check if turret is near rotation limits
     * Uses PHYSICAL angle (accounts for initial offset)
     */
    public boolean isNearLimits() {
        if (motors == null || motors.turret == null) return false;
        double physicalDegrees = getPhysicalAngleDegrees();
        double maxRotation = RobotConstants.Turret.MAX_ROTATION_DEGREES;
        return Math.abs(physicalDegrees) > (maxRotation - 10.0);
    }

    /**
     * Reset turret to center position (0 encoder ticks)
     */
    public void resetToCenter() {
        if (motors == null || motors.turret == null) return;

        disable(); // Disable auto-tracking

        motors.turret.setTargetPosition(0); // MUST set target before switching mode
        motors.turret.setMode(DcMotor.RunMode.RUN_TO_POSITION);
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
        
        // Move to position (MUST set target before switching to RUN_TO_POSITION)
        motors.turret.setTargetPosition(targetTicks);
        motors.turret.setMode(DcMotor.RunMode.RUN_TO_POSITION);
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
     * Check if encoder reading is within expected bounds
     * Use this to detect potential encoder drift or issues
     * @return true if encoder is within safe limits, false if suspicious
     */
    public boolean isEncoderHealthy() {
        if (motors == null || motors.turret == null) return true;
        
        double currentAngle = getCurrentAngleDegrees();
        double maxAngle = RobotConstants.Turret.MAX_ROTATION_DEGREES;
        
        // Allow 20% overshoot buffer for encoder reading anomalies
        double safetyBuffer = maxAngle * 1.2;
        
        return Math.abs(currentAngle) <= safetyBuffer;
    }
    
    /**
     * Get encoder health status string for telemetry
     */
    public String getEncoderHealthStatus() {
        if (!isEncoderHealthy()) {
            return String.format("⚠️ ENCODER WARNING: %.0f° (expect ±%.0f°)", 
                getCurrentAngleDegrees(), RobotConstants.Turret.MAX_ROTATION_DEGREES);
        }
        return "OK";
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
            goalX,
            goalY
        );

        // ========== CALCULATE TURRET ANGLE RELATIVE TO ROBOT ==========
        // This is the angle the turret needs to point, relative to robot's heading
        double turretAngleNeeded = fieldAngleToGoal - currentHeading;
        
        // Normalize to -180 to +180
        turretAngleNeeded = normalizeAngle(turretAngleNeeded);
        
        // ========== APPLY ROTATION LIMITS (WIRE SAFETY) ==========
        // Clamp to configured limits (default ±135°)
        double maxRotation = RobotConstants.Turret.MAX_ROTATION_DEGREES;
        
        if (Math.abs(turretAngleNeeded) > maxRotation) {
            // Goal is in the dead zone behind the robot.
            // Hold at whichever limit the turret is ALREADY closest to.
            // Use PHYSICAL angle (accounts for initial offset) for correct snap decision.
            double physicalDegrees = getPhysicalAngleDegrees();
            turretAngleNeeded = (physicalDegrees >= 0) ? maxRotation : -maxRotation;
        }
        
        // Convert angle to encoder ticks + apply manual offset
        turretTargetPosition = turretAngleNeeded * TICKS_PER_DEGREE + manualOffsetTicks;
        
        // Clamp target to OFFSET-AWARE encoder limits (wire safety)
        // Physical limits are ±MAX_ROTATION_DEGREES. Encoder limits shift by offset.
        double effectiveMaxEncoder = maxRotation * TICKS_PER_DEGREE + manualOffsetTicks;
        double effectiveMinEncoder = -maxRotation * TICKS_PER_DEGREE + manualOffsetTicks;
        turretTargetPosition = Math.max(effectiveMinEncoder, 
                                        Math.min(effectiveMaxEncoder, turretTargetPosition));
        
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

        // Enforce encoder limits (hard stops + soft limits near edges)
        power = enforceTurretLimits(currentPosition, power);

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
