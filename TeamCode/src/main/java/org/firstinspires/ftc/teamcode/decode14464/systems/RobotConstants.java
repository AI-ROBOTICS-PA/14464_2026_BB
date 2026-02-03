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
    public static final class Turret {
        // PID Constants for auto-tracking
        public static final double KP = 0.015;
        public static final double KI = 0.0;
        public static final double KD = 0.001;
        public static final double KF = 0.0;

        // Limits
        public static final double MAX_POWER = 0.5;
        public static final double MIN_POWER = -0.5;
        public static final double DEADBAND_DEGREES = 2.0;

        // Encoder limits (if using)
        public static final int MAX_ENCODER_POSITION = 1000;
        public static final int MIN_ENCODER_POSITION = -1000;

        // AprilTag IDs
        public static final int PRIMARY_TAG_ID = 20;
        public static final int SECONDARY_TAG_ID = 24;
    }

    // ==================== HOOD SERVO CONSTANTS ====================
    public static final class Hood {
        // Servo position range
        public static final double MIN_POSITION = 0.28;
        public static final double MAX_POSITION = 0.80;
        public static final double HOME_POSITION = 0.50;

        // Preset positions
        public static final double LONG_RANGE_POSITION = 0.50;
        public static final double SHORT_RANGE_POSITION = 0.80;

        // Distance-based mapping (distance in inches -> servo position)
        // Tune these based on testing
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
        public static final double[] LAUNCH_POSITIONS = {0.17, 0.54, 0.91};

        public static final double NEUTRAL_POSITION = 0.5;
    }

    // ==================== BALL PUSH SERVO CONSTANTS ====================
    public static final class BallPush {
        public static final double INITIAL_POSITION = 0.50;
        public static final double PUSH_POSITION = 0.77;
        public static final double RETRACT_POSITION = 0.50;

        public static final long PUSH_DELAY_MS = 200;  // Time to push ball
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
}

