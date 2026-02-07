package org.firstinspires.ftc.teamcode.decode14464;

/**
 * TurretConstants - Configuration constants for Auto Turret System
 * DECODE 14464 - FTC 2026 Season
 *
dation  * Updated with Production Calibration from Feb 4, 2026
 */
public class TurretConstants {

    // ========== MOTOR CONFIGURATION ==========
    public static final String TURRET_MOTOR_NAME = "turret";
    public static final String LIMELIGHT_NAME = "limelight";

    /**
     * CALIBRATED VALUE (Feb 4, 2026)
     * 271 ticks for 90 degrees = 3.01 ticks/degree
     */
    public static double ENCODER_TICKS_PER_DEGREE = 3.01;

    /**
     * Center/neutral position is 0 ticks in AutoTurretSystem
     */
    public static double CENTER_POSITION_DEGREES = 0.0;

    /**
     * Max rotation (Left/CCW) from center
     */
    public static double MAX_ROTATION_DEGREES = 150.0;

    /**
     * Min rotation (Right/CW) from center
     */
    public static double MIN_ROTATION_DEGREES = -150.0;

    public static double SOFT_LIMIT_ZONE_DEGREES = 15.0; // Increased slighty for safety

    // ========== PID CONTROL PARAMETERS (Optimized Feb 5) ==========
    /**
     * Optimized for GoBilda 5203 (312 RPM)
     */
    public static double AUTO_TRACK_KP = 0.010;  // Was 0.035
    public static double AUTO_TRACK_KI = 0.0;
    public static double AUTO_TRACK_KD = 0.0015; // Was 0.030

    public static double MAX_TURRET_POWER = 0.45; // Optimized for torque
    public static double DEADBAND = 2.0;
    public static double MIN_POWER_THRESHOLD = 0.06; // Lower friction threshold

    public static boolean INVERT_TURRET_DIRECTION = true;

    // ========== APRILTAG CONFIGURATION ==========
    public static int PRIMARY_TAG_ID = 20;
    public static int SECONDARY_TAG_ID = 24;

    // ========== MANUAL CONTROL PARAMETERS ==========
    public static double MANUAL_CONTROL_POWER = 0.4;
    public static double FINE_ADJUST_POWER = 0.15;
    public static double JOYSTICK_DEADZONE = 0.05;

    // ========== HELPER METHODS ==========

    public static double getMaxEncoderPosition() {
        return MAX_ROTATION_DEGREES * ENCODER_TICKS_PER_DEGREE;
    }

    public static double getMinEncoderPosition() {
        return MIN_ROTATION_DEGREES * ENCODER_TICKS_PER_DEGREE;
    }

    public static double getCenterEncoderPosition() {
        return CENTER_POSITION_DEGREES * ENCODER_TICKS_PER_DEGREE;
    }

    public static double getSoftLimitZoneTicks() {
        return SOFT_LIMIT_ZONE_DEGREES * ENCODER_TICKS_PER_DEGREE;
    }

    public static double encoderToDegrees(double encoderPosition) {
        return encoderPosition / ENCODER_TICKS_PER_DEGREE;
    }

    public static double degreesToEncoder(double degrees) {
        return degrees * ENCODER_TICKS_PER_DEGREE;
    }
}

