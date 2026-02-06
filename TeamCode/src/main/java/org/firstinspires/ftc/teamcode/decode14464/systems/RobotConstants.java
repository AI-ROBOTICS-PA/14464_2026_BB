package org.firstinspires.ftc.teamcode.decode14464.systems;

/**
 * Unified Robot Constants
 * All robot configuration values in one place for easy tuning
 */
public class RobotConstants {

    // ==================== FIELD CONSTANTS ====================
    public static final class Field {
        public static final double WIDTH = 144.0;  // inches
        public static final double HEIGHT = 144.0; // inches

        // Goal position (shooting target)
        public static final double GOAL_X = 1.0;   // 1 inch from left edge
        public static final double GOAL_Y = 144.0; // Top edge
    }

    // ==================== TURRET MOTOR CONSTANTS ====================
    // Optimized for GoBilda 5203 312 RPM Yellow Jacket motor
    public static final class Turret {
        // PID Constants for auto-tracking (optimized for 312 RPM motor)
        public static final double KP = 0.010;      // Proportional gain
        public static final double KI = 0.0;        // No integral (avoid windup)
        public static final double KD = 0.0015;     // Derivative for damping
        public static final double KF = 0.0;

        // Limits
        public static final double MAX_POWER = 0.45;
        public static final double MIN_POWER = -0.45;
        public static final double DEADBAND_DEGREES = 4.0;  // ~12 ticks at 3.01 ticks/deg

        // Encoder calibration: 3.01 ticks per degree (measured)
        public static final double TICKS_PER_DEGREE = 3.01;
        
        // ========== WIRE MANAGEMENT - MOVEMENT LIMITS ==========
        // Maximum turret rotation from center (CONFIGURABLE for wire safety)
        public static final double MAX_ROTATION_DEGREES = 135.0;  // ±135° = 270° total range
        
        // Encoder limits (calculated from MAX_ROTATION_DEGREES)
        public static final int MAX_ENCODER_POSITION = (int)(MAX_ROTATION_DEGREES * TICKS_PER_DEGREE);  // +135° = ~406 ticks
        public static final int MIN_ENCODER_POSITION = (int)(-MAX_ROTATION_DEGREES * TICKS_PER_DEGREE); // -135° = ~-406 ticks
        
        // Soft limit zone (slow down before hard limit)
        public static final double SOFT_LIMIT_DEGREES = 15.0;  // Start slowing 15° before limit
        
        // Counter-rotation wire limits (legacy - kept for compatibility)
        public static final double WIRE_LIMIT_DEGREES = MAX_ROTATION_DEGREES;
        public static final double UNWIND_HYSTERESIS = 10;

        // AprilTag IDs
        public static final int PRIMARY_TAG_ID = 20;
        public static final int SECONDARY_TAG_ID = 24;
    }

    // ==================== HOOD SERVO CONSTANTS ====================
    public static final class Hood {
        // Servo hardware name
        public static final String SERVO_NAME = "hood_5";
        
        // Servo position range
        public static final double MIN_POSITION = 0.28;   // Highest hood angle
        public static final double MAX_POSITION = 0.82;   // Lowest hood angle
        public static final double HOME_POSITION = 0.80;  // Initial/home position
        public static final double DEFAULT_POSITION = 0.50; // Default when no tag visible

        // Preset positions for manual control
        public static final double LONG_RANGE_POSITION = 0.30;   // Flattest angle
        public static final double MID_RANGE_POSITION = 0.55;    // Mid-range
        public static final double SHORT_RANGE_POSITION = 0.80;  // Steepest angle

        // Distance-based auto-hood mapping
        public static final double MIN_DISTANCE_INCHES = 20.0;   // At this distance -> MAX_POSITION
        public static final double MAX_DISTANCE_INCHES = 100.0;  // At this distance -> MIN_POSITION
        public static final boolean INVERT_DISTANCE_MAPPING = false;

        // Distance-based position lookup (for interpolation)
        public static final double CLOSE_DISTANCE = 36.0;   // inches
        public static final double CLOSE_POSITION = 0.80;

        public static final double MID_DISTANCE = 72.0;     // inches
        public static final double MID_POSITION = 0.65;

        public static final double FAR_DISTANCE = 120.0;    // inches
        public static final double FAR_POSITION = 0.50;
    }

    // ==================== TURRET SERVO CONSTANTS ====================
    public static final class TurretServo {
        // Intake mode positions (3 slots)
        public static final double[] INTAKE_POSITIONS = {0.07, 0.44, 0.81};

        // Launch mode positions (3 slots)
        public static final double[] LAUNCH_POSITIONS = {0.165, 0.535, 0.905};

        public static final double NEUTRAL_POSITION = 0.5;
    }

    // ==================== BALL PUSH SERVO CONSTANTS ====================
    public static final class BallPush {
        public static final double INITIAL_POSITION = 0.49;
        public static final double PUSH_POSITION = 0.77;
        public static final double RETRACT_POSITION = 0.49;

        public static final long PUSH_DELAY_MS = 200;  // Time to push ball

        // Touch sensor - detects when ball push is fully retracted
        public static final String TOUCH_SENSOR_NAME = "ballpush_sensor";
    }

    // ==================== COLOR SERVO (PWM LIGHT) CONSTANTS ====================
    public static final class ColorServo {
        public static final double INTAKE_MODE_COLOR = 1.0;    // Color for intake mode
        public static final double LAUNCH_MODE_COLOR = 0.38;   // Color for launch mode
        public static final double NEUTRAL_COLOR = 0.5;
        public static final double OFF = 0.0;
    }

    // ==================== SHOOTER/FLYWHEEL CONSTANTS ====================
    public static final class Shooter {
        // Velocity targets (ticks per second)
        public static final double HIGH_VELOCITY = 1450.0;  // Long distance
        public static final double LOW_VELOCITY = 1100.0;   // Short distance

        // PIDF Coefficients
        public static final double KP = 300.0;
        public static final double KI = 0.0;
        public static final double KD = 0.0;
        public static final double KF = 13.65;

        // Velocity tolerance
        public static final double VELOCITY_TOLERANCE = 50.0; // ticks/sec

        // Distance-based velocity mapping
        public static final double CLOSE_DISTANCE = 36.0;
        public static final double CLOSE_VELOCITY = 1100.0;

        public static final double MID_DISTANCE = 72.0;
        public static final double MID_VELOCITY = 1300.0;

        public static final double FAR_DISTANCE = 120.0;
        public static final double FAR_VELOCITY = 1450.0;
    }

    // ==================== INTAKE CONSTANTS ====================
    public static final class Intake {
        public static final double INTAKE_POWER = 1.0;
        public static final double OUTTAKE_POWER = -1.0;
        public static final double STOP_POWER = 0.0;
    }

    // ==================== COLOR SENSOR CONSTANTS ====================
    public static final class ColorSensor {
        // Color detection thresholds
        public static final String NO_BALL_COLOR_1 = "Black";
        public static final String NO_BALL_COLOR_2 = "None";

        // Auto-advance settings
        public static final long COLOR_DEBOUNCE_MS = 100; // Debounce time
    }

    // ==================== AUTO SHOOTING CONSTANTS ====================
    public static final class AutoShooting {
        // Pass-through point for trajectory calculation
        public static final double SCORE_HEIGHT = 24.0;           // Height of pass-through point
        public static final double SCORE_ANGLE = -15.0;           // Angle at pass-through (degrees)
        public static final double PASS_THROUGH_RADIUS = 12.0;    // Distance from goal to pass-through point

        // Physics constants
        public static final double GRAVITY = 386.0; // inches/sec^2 (32.2 ft/s^2)

        // Compensation factors
        public static final double VELOCITY_COMPENSATION_FACTOR = 1.0;
    }

    // ==================== SEQUENCE TIMING ====================
    public static final class Timing {
        public static final long BALL_PUSH_DURATION_MS = 200;
        public static final long BALL_FEED_INTERVAL_MS = 300;
        public static final long SHOOTER_SPINUP_MS = 500;
    }
    
    // ==================== AUTONOMOUS TIMING ====================
    public static final class Auto {
        // Shooter/turret timing
        public static final double SHOOTER_SPINUP_SECONDS = 1.5;    // Time to spin up flywheel
        public static final double TURRET_AIM_TIMEOUT = 2.0;        // Max time to wait for turret to aim
        
        // Shooting sequence
        public static final double SHOOT_SEQUENCE_TIMEOUT = 4.0;    // Max time for 3-ball sequence
        
        // Intake timing
        public static final double INTAKE_DURATION = 3.0;           // Time to intake balls
        
        // Path timing
        public static final double PATH_TIMEOUT = 5.0;              // Max time for path completion
        
        // Number of balls
        public static final int BALLS_PER_CYCLE = 3;                // Balls loaded/shot per cycle
        
        // ========== INITIAL TURRET POSITION (TUNABLE) ==========
        // Turret angle to move to at start of autonomous (degrees from center)
        // Positive = right, Negative = left
        // TUNE THIS: Adjust until first shot hits the goal reliably
        public static final double BLUE_FAR_INITIAL_TURRET_ANGLE = 45.0;   // Blue far zone
        public static final double RED_FAR_INITIAL_TURRET_ANGLE = -45.0;   // Red far zone (mirror)
        public static final double BLUE_NEAR_INITIAL_TURRET_ANGLE = 30.0;  // Blue near zone
        public static final double RED_NEAR_INITIAL_TURRET_ANGLE = -30.0;  // Red near zone (mirror)
        
        // Time to wait for turret to reach initial position
        public static final double INITIAL_TURRET_MOVE_TIMEOUT = 1.0;      // seconds
        
        // Settling time after turret reaches position before enabling auto-tracking
        public static final double TURRET_SETTLING_TIME = 0.2;             // seconds (200ms)
    }
}

