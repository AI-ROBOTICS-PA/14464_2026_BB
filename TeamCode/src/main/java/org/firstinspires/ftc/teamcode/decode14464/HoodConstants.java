package org.firstinspires.ftc.teamcode.decode14464;

/**
 * HoodConstants - Configuration constants for Hood Height System
 *
 * All hood-related configuration in one place for easy tuning and reuse
 * across TeleOp and Autonomous programs.
 */
public class HoodConstants {

    // ========== SERVO CONFIGURATION ==========
    /**
     * Servo hardware name in Robot Configuration
     */
    public static final String HOOD_SERVO_NAME = "hood_5";

    /**
     * Minimum servo position (highest hood angle)
     * When servo is at 0.28, hood is at maximum angle
     */
    public static double HOOD_MIN_POSITION = 0.28;

    /**
     * Maximum servo position (lowest hood angle)
     * When servo is at 0.80, hood is at minimum angle
     */
    public static double HOOD_MAX_POSITION = 0.80;

    // ========== DISTANCE MAPPING ==========
    /**
     * Minimum distance to AprilTag (in inches)
     * At this distance or closer, hood will be at HOOD_MAX_POSITION (0.80)
     */
    public static double MIN_DISTANCE_INCHES = 20.0;

    /**
     * Maximum distance to AprilTag (in inches)
     * At this distance or farther, hood will be at HOOD_MIN_POSITION (0.28)
     */
    public static double MAX_DISTANCE_INCHES = 100.0;

    /**
     * Whether to invert the distance mapping
     * If true: closer = lower position (0.28), farther = higher position (0.80)
     * If false: closer = higher position (0.80), farther = lower position (0.28)
     */
    public static boolean INVERT_DISTANCE_MAPPING = false;

    // ========== APRILTAG CONFIGURATION ==========
    /**
     * Primary AprilTag ID to track
     */
    public static int PRIMARY_TAG_ID = 20;

    /**
     * Secondary AprilTag ID to track
     */
    public static int SECONDARY_TAG_ID = 24;

    // ========== DEFAULT POSITIONS ==========
    /**
     * Default hood position when no tag is visible
     */
    public static double DEFAULT_POSITION = 0.50;

    /**
     * Initial/home hood position
     */
    public static double HOME_POSITION = 0.80;
}

