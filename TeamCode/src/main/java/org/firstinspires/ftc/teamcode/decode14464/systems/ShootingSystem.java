package org.firstinspires.ftc.teamcode.decode14464.systems;

import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.hardware.PIDFCoefficients;
import com.qualcomm.robotcore.hardware.Servo;
import com.qualcomm.robotcore.hardware.TouchSensor;
import com.qualcomm.robotcore.util.ElapsedTime;

/**
 * Feature 5 & 7: Shooting System
 * Manages shooter motor velocity and automatic shooting sequence
 * Also handles distance-based velocity adjustment
 */
public class ShootingSystem {
    private DcMotorEx flywheelMotor;
    private DcMotor intakeMotor;
    private Servo ballPushServo;
    private TouchSensor ballPushSensor; // Detects when ball push is retracted
    private AutoIntakeSystem intakeSystem;
    private LocalizationSystem localization;

    private boolean isShooterActive = false;
    private boolean isAutoShootingInProgress = false;
    private double targetVelocity = 0; // Track target velocity for accurate checks
    private ElapsedTime shootTimer = new ElapsedTime();

    private enum ShootingState {
        IDLE,
        SPIN_UP,
        PUSH_BALL_1,
        RETRACT_1,
        WAIT_RETRACT_1,      // Wait for ball push to fully retract
        ADVANCE_TURRET_1,    // Move turret to next position
        WAIT_TURRET_1,       // Wait for turret to reach position
        PUSH_BALL_2,
        RETRACT_2,
        WAIT_RETRACT_2,      // Wait for ball push to fully retract
        ADVANCE_TURRET_2,    // Move turret to next position
        WAIT_TURRET_2,       // Wait for turret to reach position
        PUSH_BALL_3,
        RETRACT_3,
        COMPLETE
    }

    private ShootingState currentState = ShootingState.IDLE;

    /**
     * Initialize shooting system
     */
    public void init(HardwareMap hardwareMap, AutoIntakeSystem intakeSystem, LocalizationSystem localization) {
        flywheelMotor = hardwareMap.get(DcMotorEx.class, "shooter");
        intakeMotor = hardwareMap.get(DcMotor.class, "intake");
        ballPushServo = hardwareMap.get(Servo.class, "ballpush_2");
        ballPushSensor = hardwareMap.get(TouchSensor.class, RobotConstants.BallPush.TOUCH_SENSOR_NAME);

        this.intakeSystem = intakeSystem;
        this.localization = localization;

        // Configure flywheel motor
        flywheelMotor.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        flywheelMotor.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        flywheelMotor.setDirection(DcMotor.Direction.FORWARD);
        flywheelMotor.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);

        // Set PIDF coefficients
        PIDFCoefficients pidfCoefficients = new PIDFCoefficients(
            RobotConstants.Shooter.KP,
            RobotConstants.Shooter.KI,
            RobotConstants.Shooter.KD,
            RobotConstants.Shooter.KF
        );
        flywheelMotor.setPIDFCoefficients(DcMotor.RunMode.RUN_USING_ENCODER, pidfCoefficients);

        // Initialize ball push servo
        ballPushServo.setPosition(RobotConstants.BallPush.INITIAL_POSITION);
    }

    /**
     * Update shooting sequence - call this every loop
     */
    public void update() {
        if (!isAutoShootingInProgress) {
            return;
        }

        double elapsed = shootTimer.milliseconds();

        switch (currentState) {
            case IDLE:
                // Should not happen
                break;

            case SPIN_UP:
                if (isShooterAtTargetVelocity()) {
                    // Shooter ready, start pushing balls
                    currentState = ShootingState.PUSH_BALL_1;
                    intakeSystem.setLaunchMode(); // Ensure in launch mode
                    shootTimer.reset();
                }
                break;

            // ========== BALL 1 SEQUENCE ==========
            case PUSH_BALL_1:
                ballPushServo.setPosition(RobotConstants.BallPush.PUSH_POSITION);
                if (elapsed > RobotConstants.Timing.BALL_PUSH_DURATION_MS) {
                    currentState = ShootingState.RETRACT_1;
                    shootTimer.reset();
                }
                break;

            case RETRACT_1:
                // Start retracting ball push servo
                ballPushServo.setPosition(RobotConstants.BallPush.RETRACT_POSITION);
                currentState = ShootingState.WAIT_RETRACT_1;
                shootTimer.reset();
                break;

            case WAIT_RETRACT_1:
                // Wait for ball push to fully retract before moving turret
                if (elapsed > RobotConstants.Timing.BALL_PUSH_DURATION_MS) {
                    currentState = ShootingState.ADVANCE_TURRET_1;
                    shootTimer.reset();
                }
                break;

            case ADVANCE_TURRET_1:
                // Now safe to move turret - ball push is fully retracted
                intakeSystem.advanceToNextSlot();
                currentState = ShootingState.WAIT_TURRET_1;
                shootTimer.reset();
                break;

            case WAIT_TURRET_1:
                // Wait for turret to reach position before pushing next ball
                if (elapsed > RobotConstants.Timing.BALL_FEED_INTERVAL_MS) {
                    currentState = ShootingState.PUSH_BALL_2;
                    shootTimer.reset();
                }
                break;

            // ========== BALL 2 SEQUENCE ==========
            case PUSH_BALL_2:
                ballPushServo.setPosition(RobotConstants.BallPush.PUSH_POSITION);
                if (elapsed > RobotConstants.Timing.BALL_PUSH_DURATION_MS) {
                    currentState = ShootingState.RETRACT_2;
                    shootTimer.reset();
                }
                break;

            case RETRACT_2:
                // Start retracting ball push servo
                ballPushServo.setPosition(RobotConstants.BallPush.RETRACT_POSITION);
                currentState = ShootingState.WAIT_RETRACT_2;
                shootTimer.reset();
                break;

            case WAIT_RETRACT_2:
                // Wait for ball push to fully retract before moving turret
                if (elapsed > RobotConstants.Timing.BALL_PUSH_DURATION_MS) {
                    currentState = ShootingState.ADVANCE_TURRET_2;
                    shootTimer.reset();
                }
                break;

            case ADVANCE_TURRET_2:
                // Now safe to move turret - ball push is fully retracted
                intakeSystem.advanceToNextSlot();
                currentState = ShootingState.WAIT_TURRET_2;
                shootTimer.reset();
                break;

            case WAIT_TURRET_2:
                // Wait for turret to reach position before pushing next ball
                if (elapsed > RobotConstants.Timing.BALL_FEED_INTERVAL_MS) {
                    currentState = ShootingState.PUSH_BALL_3;
                    shootTimer.reset();
                }
                break;

            // ========== BALL 3 SEQUENCE ==========
            case PUSH_BALL_3:
                ballPushServo.setPosition(RobotConstants.BallPush.PUSH_POSITION);
                if (elapsed > RobotConstants.Timing.BALL_PUSH_DURATION_MS) {
                    currentState = ShootingState.RETRACT_3;
                    shootTimer.reset();
                }
                break;

            case RETRACT_3:
                // Final retract - no more balls to shoot
                ballPushServo.setPosition(RobotConstants.BallPush.RETRACT_POSITION);
                if (elapsed > RobotConstants.Timing.BALL_PUSH_DURATION_MS) {
                    currentState = ShootingState.COMPLETE;
                    isAutoShootingInProgress = false;
                }
                break;

            case COMPLETE:
                isAutoShootingInProgress = false;
                break;
        }
    }

    /**
     * Start shooter at specific velocity
     */
    public void startShooter(double velocity) {
        this.targetVelocity = velocity; // Store target for accuracy check
        if (flywheelMotor != null) {
            flywheelMotor.setVelocity(velocity);
            isShooterActive = true;
        }
    }

    /**
     * Start shooter with distance-based velocity
     */
    public void startShooterAuto() {
        double distance = getDistanceToGoal();
        double velocity = calculateVelocityForDistance(distance);
        startShooter(velocity);
    }

    /**
     * Stop shooter
     */
    public void stopShooter() {
        if (flywheelMotor != null) {
            flywheelMotor.setVelocity(0);
            isShooterActive = false;
            targetVelocity = 0; // Reset target
        }
    }

    /**
     * Trigger automatic shooting sequence (3 balls)
     * Safety: Only works in launch mode (balls should be loaded)
     */
    public void triggerAutoShoot() {
        if (isAutoShootingInProgress) {
            return; // Already shooting
        }

        // Safety check: Must be in launch mode with balls loaded
        if (intakeSystem != null && intakeSystem.isIntakeMode()) {
            // Still in intake mode, balls not ready to launch
            return;
        }

        isAutoShootingInProgress = true;
        currentState = ShootingState.SPIN_UP;
        shootTimer.reset();

        // Ensure shooter is running
        if (!isShooterActive) {
            startShooterAuto();
        }
    }

    /**
     * Stop auto-shooting sequence
     */
    public void stopAutoShoot() {
        isAutoShootingInProgress = false;
        currentState = ShootingState.IDLE;
        ballPushServo.setPosition(RobotConstants.BallPush.RETRACT_POSITION);
    }

    /**
     * Calculate velocity based on distance (linear interpolation)
     */
    private double calculateVelocityForDistance(double distance) {
        if (distance <= RobotConstants.Shooter.CLOSE_DISTANCE) {
            return RobotConstants.Shooter.CLOSE_VELOCITY;
        } else if (distance >= RobotConstants.Shooter.FAR_DISTANCE) {
            return RobotConstants.Shooter.FAR_VELOCITY;
        } else if (distance <= RobotConstants.Shooter.MID_DISTANCE) {
            // Interpolate between close and mid
            double ratio = (distance - RobotConstants.Shooter.CLOSE_DISTANCE) /
                          (RobotConstants.Shooter.MID_DISTANCE - RobotConstants.Shooter.CLOSE_DISTANCE);
            return RobotConstants.Shooter.CLOSE_VELOCITY +
                   ratio * (RobotConstants.Shooter.MID_VELOCITY - RobotConstants.Shooter.CLOSE_VELOCITY);
        } else {
            // Interpolate between mid and far
            double ratio = (distance - RobotConstants.Shooter.MID_DISTANCE) /
                          (RobotConstants.Shooter.FAR_DISTANCE - RobotConstants.Shooter.MID_DISTANCE);
            return RobotConstants.Shooter.MID_VELOCITY +
                   ratio * (RobotConstants.Shooter.FAR_VELOCITY - RobotConstants.Shooter.MID_VELOCITY);
        }
    }

    /**
     * Check if shooter is at target velocity
     */
    public boolean isShooterAtTargetVelocity() {
        if (flywheelMotor == null || !isShooterActive || targetVelocity == 0) {
            return false;
        }
        double currentVel = flywheelMotor.getVelocity();
        return Math.abs(currentVel - targetVelocity) <= RobotConstants.Shooter.VELOCITY_TOLERANCE;
    }

    /**
     * Get current shooter velocity
     */
    public double getCurrentVelocity() {
        return flywheelMotor != null ? flywheelMotor.getVelocity() : 0;
    }

    /**
     * Get distance to goal
     */
    public double getDistanceToGoal() {
        if (localization == null) return 72.0; // Default mid-distance
        return localization.getDistanceToPoint(
            RobotConstants.Field.GOAL_X,
            RobotConstants.Field.GOAL_Y
        );
    }

    /**
     * Check if auto-shooting is in progress
     */
    public boolean isAutoShootingInProgress() {
        return isAutoShootingInProgress;
    }

    /**
     * Check if shooter is active
     */
    public boolean isShooterActive() {
        return isShooterActive;
    }

    /**
     * Get current shooting state (for debugging)
     */
    public String getCurrentState() {
        return currentState.toString();
    }

    /**
     * Manual ball push (for testing)
     */
    public void pushBall() {
        ballPushServo.setPosition(RobotConstants.BallPush.PUSH_POSITION);
    }

    /**
     * Manual ball retract (for testing)
     */
    public void retractBall() {
        ballPushServo.setPosition(RobotConstants.BallPush.RETRACT_POSITION);
    }

    /**
     * Set ball push servo to specific position (for auto-shoot control)
     * @param position Servo position (0.0 to 1.0)
     */
    public void setBallPushPosition(double position) {
        if (ballPushServo != null) {
            ballPushServo.setPosition(position);
        }
    }

    /**
     * Get current ball push servo position
     * @return Current servo position (0.0 to 1.0)
     */
    public double getBallPushPosition() {
        if (ballPushServo != null) {
            return ballPushServo.getPosition();
        }
        return 0.0;
    }

    /**
     * Check if ball push servo is fully retracted using touch sensor
     * @return true if ball push is retracted (sensor pressed), false otherwise
     */
    public boolean isBallPushRetracted() {
        if (ballPushSensor != null) {
            return ballPushSensor.isPressed();
        }
        return false; // If no sensor, assume not retracted
    }
}

