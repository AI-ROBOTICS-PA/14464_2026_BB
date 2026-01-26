package org.firstinspires.ftc.teamcode.decode14464;

/**
 * TurretConstants - Configuration constants for Auto Turret System
 *
 * All turret-related configuration in one place for easy tuning and reuse
 * across TeleOp and Autonomous programs.
 */
public class TurretConstants {

    // ========== MOTOR CONFIGURATION ==========
    /**
     * Motor hardware name in Robot Configuration
     */
    public static final String TURRET_MOTOR_NAME = "turret";

    /**
     * Encoder ticks per degree of turret rotation
     * IMPORTANT: Calibrate this for your specific turret setup!
     *
     * To calibrate:
     * 1. Manually rotate turret exactly 90 degrees
     * 2. Note encoder position change
     * 3. Calculate: ticks / 90 = ticks per degree
     */
    public static double ENCODER_TICKS_PER_DEGREE = 5.0;

    /**
     * Center/neutral position of turret in degrees
     * This is the forward-facing position
     */
    public static double CENTER_POSITION_DEGREES = 150.0;

    /**
     * Maximum rotation limit (anticlockwise from center)
     * Default: 300° (150° anticlockwise from 150° center)
     */
    public static double MAX_ROTATION_DEGREES = 300.0;

    /**
     * Minimum rotation limit (clockwise from center)
     * Default: 0° (150° clockwise from 150° center)
     */
    public static double MIN_ROTATION_DEGREES = 0.0;

    /**
     * Soft limit zone in degrees
     * Turret slows down when within this distance from limits
     */
    public static double SOFT_LIMIT_ZONE_DEGREES = 10.0;

    // ========== PID CONTROL PARAMETERS ==========
    /**
     * Proportional gain - controls response speed
     * Higher = faster response, but may oscillate
     * Lower = slower response, but more stable
     *
     * Typical range: 0.01 to 0.10
     */
    public static double AUTO_TRACK_KP = 0.035;

    /**
     * Integral gain - accumulates error over time
     * Usually set to 0 for turret control
     */
    public static double AUTO_TRACK_KI = 0.0;

    /**
     * Derivative gain - provides damping
     * Higher = smoother motion, less overshoot
     * Lower = may oscillate or overshoot
     *
     * Typical range: 0.005 to 0.030
     */
    public static double AUTO_TRACK_KD = 0.030;

    /**
     * Maximum motor power limit (0.0 to 1.0)
     * Limits how fast the turret can move
     *
     * Recommended: 0.5 to 0.8 for good control
     */
    public static double MAX_TURRET_POWER = 0.6;

    /**
     * Deadband in degrees
     * Stop tracking if error is within this range
     * Prevents jitter when locked on target
     */
    public static double DEADBAND = 2.0;

    /**
     * Minimum power threshold
     * Ensures motor always has enough power to overcome friction
     * Prevents jerky movements at low speeds
     */
    public static double MIN_POWER_THRESHOLD = 0.12;

    /**
     * Invert turret direction
     * Set to true if turret goes opposite direction from expected
     */
    public static boolean INVERT_TURRET_DIRECTION = true;

    // ========== APRILTAG CONFIGURATION ==========
    /**
     * Primary AprilTag ID to track
     * From DECODE_Printable_AT_USLetter - V1
     */
    public static int PRIMARY_TAG_ID = 20;

    /**
     * Secondary AprilTag ID to track
     * From DECODE_Printable_AT_USLetter - V1
     */
    public static int SECONDARY_TAG_ID = 24;

    // ========== MANUAL CONTROL PARAMETERS ==========
    /**
     * Manual control power for joystick
     */
    public static double MANUAL_CONTROL_POWER = 0.4;

    /**
     * Fine adjustment power for D-Pad
     */
    public static double FINE_ADJUST_POWER = 0.15;

    /**
     * Joystick deadzone threshold
     */
    public static double JOYSTICK_DEADZONE = 0.05;

    // ========== HELPER METHODS ==========

    /**
     * Get maximum encoder position based on max degrees
     */
    public static double getMaxEncoderPosition() {
        return MAX_ROTATION_DEGREES * ENCODER_TICKS_PER_DEGREE;
    }

    /**
     * Get minimum encoder position based on min degrees
     */
    public static double getMinEncoderPosition() {
        return MIN_ROTATION_DEGREES * ENCODER_TICKS_PER_DEGREE;
    }

    /**
     * Get center encoder position
     */
    public static double getCenterEncoderPosition() {
        return CENTER_POSITION_DEGREES * ENCODER_TICKS_PER_DEGREE;
    }

    /**
     * Get soft limit zone in encoder ticks
     */
    public static double getSoftLimitZoneTicks() {
        return SOFT_LIMIT_ZONE_DEGREES * ENCODER_TICKS_PER_DEGREE;
    }

    /**
     * Convert encoder position to degrees
     */
    public static double encoderToDegrees(double encoderPosition) {
        return encoderPosition / ENCODER_TICKS_PER_DEGREE;
    }

    /**
     * Convert degrees to encoder position
     */
    public static double degreesToEncoder(double degrees) {
        return degrees * ENCODER_TICKS_PER_DEGREE;
    }
}

