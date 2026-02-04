package org.firstinspires.ftc.teamcode.decode14464.systems;

import com.qualcomm.robotcore.hardware.HardwareMap;

/**
 * Feature 4: Auto-Intake System
 * Controls intake motor, turret servo, and automatically advances when ball is detected
 * Uses centralized RobotSensors, RobotMotors, and RobotServos
 */
public class AutoIntakeSystem {
    // Centralized hardware references
    private RobotSensors sensors;
    private RobotMotors motors;
    private RobotServos servos;

    private boolean isIntakeMode = false;
    private boolean isIntakeMotorRunning = true; // Auto-start intake motor
    private int currentIndex = 0;
    private String previousColor = RobotConstants.ColorSensor.NO_BALL_COLOR_1;
    private boolean transitionLocked = false;

    /**
     * Initialize intake system with centralized hardware
     */
    public void init(HardwareMap hardwareMap, RobotSensors sensors, RobotMotors motors, RobotServos servos) {
        this.sensors = sensors;
        this.motors = motors;
        this.servos = servos;

        // Start in launch mode at first position
        setLaunchMode();

        // DON'T auto-start intake motor - wait for start() method
    }

    /**
     * Update auto-advance logic - call this every loop
     */
    public void update() {
        if (!isIntakeMode) {
            // Not in intake mode, don't auto-advance
            return;
        }

        // Get current color
        String currentColor = getColorName();

        // Check if color represents "no ball"
        boolean isNoBall = currentColor.equalsIgnoreCase(RobotConstants.ColorSensor.NO_BALL_COLOR_1) ||
                          currentColor.equalsIgnoreCase(RobotConstants.ColorSensor.NO_BALL_COLOR_2);
        boolean wasPreviousNoBall = previousColor.equalsIgnoreCase(RobotConstants.ColorSensor.NO_BALL_COLOR_1) ||
                                   previousColor.equalsIgnoreCase(RobotConstants.ColorSensor.NO_BALL_COLOR_2);

        // Detect transition from "no ball" to "ball detected"
        if (wasPreviousNoBall && !isNoBall && !transitionLocked) {
            // Ball detected! Advance to next position
            advanceToNextSlot();
            transitionLocked = true;
        }

        // Update previous color and lock state
        if (isNoBall) {
            // No ball - unlock and reset
            transitionLocked = false;
            previousColor = RobotConstants.ColorSensor.NO_BALL_COLOR_1;
        } else {
            // Ball present - remember color
            previousColor = currentColor;
        }
    }

    /**
     * Switch to intake mode
     */
    public void setIntakeMode() {
        isIntakeMode = true;
        currentIndex = 0; // Always start at first slot
        servos.turret_1.setPosition(RobotConstants.TurretServo.INTAKE_POSITIONS[currentIndex]);

        // Reset color tracking
        previousColor = RobotConstants.ColorSensor.NO_BALL_COLOR_1;
        transitionLocked = false;
    }

    /**
     * Switch to launch mode
     */
    public void setLaunchMode() {
        isIntakeMode = false;

        // Find closest launch position to current position
        double currentPos = servos.turret_1.getPosition();
        currentIndex = findClosestIndex(currentPos, RobotConstants.TurretServo.LAUNCH_POSITIONS);
        servos.turret_1.setPosition(RobotConstants.TurretServo.LAUNCH_POSITIONS[currentIndex]);

        // Reset color tracking
        previousColor = RobotConstants.ColorSensor.NO_BALL_COLOR_1;
        transitionLocked = false;
    }

    /**
     * Toggle between intake and launch mode
     */
    public void toggleMode() {
        if (isIntakeMode) {
            setLaunchMode();
        } else {
            setIntakeMode();
        }
    }

    /**
     * Advance to next slot (manual control)
     */
    public void advanceToNextSlot() {
        if (isIntakeMode) {
            if (currentIndex < RobotConstants.TurretServo.INTAKE_POSITIONS.length - 1) {
                currentIndex++;
                servos.turret_1.setPosition(RobotConstants.TurretServo.INTAKE_POSITIONS[currentIndex]);
            }
        } else {
            if (currentIndex < RobotConstants.TurretServo.LAUNCH_POSITIONS.length - 1) {
                currentIndex++;
                servos.turret_1.setPosition(RobotConstants.TurretServo.LAUNCH_POSITIONS[currentIndex]);
            }
        }
    }

    /**
     * Move to previous slot (manual control)
     */
    public void moveToPreviousSlot() {
        if (currentIndex > 0) {
            currentIndex--;
            if (isIntakeMode) {
                servos.turret_1.setPosition(RobotConstants.TurretServo.INTAKE_POSITIONS[currentIndex]);
            } else {
                servos.turret_1.setPosition(RobotConstants.TurretServo.LAUNCH_POSITIONS[currentIndex]);
            }
        }
    }

    /**
     * Get color name from sensor
     */
    private String getColorName() {
        if (sensors == null || sensors.color_f == null) return "None";

        // Use RobotSensors color detection method
        return sensors.getColorName(sensors.color_f);
    }

    /**
     * Find closest index in position array
     */
    private int findClosestIndex(double currentPos, double[] positions) {
        int closestIndex = 0;
        double minDistance = Math.abs(currentPos - positions[0]);

        for (int i = 1; i < positions.length; i++) {
            double distance = Math.abs(currentPos - positions[i]);
            if (distance < minDistance) {
                minDistance = distance;
                closestIndex = i;
            }
        }

        return closestIndex;
    }

    /**
     * Check if in intake mode
     */
    public boolean isIntakeMode() {
        return isIntakeMode;
    }

    /**
     * Get current slot index
     */
    public int getCurrentIndex() {
        return currentIndex;
    }

    /**
     * Get current turret servo position
     */
    public double getCurrentPosition() {
        return servos != null && servos.turret_1 != null ? servos.turret_1.getPosition() : 0;
    }

    /**
     * Check if ball is detected by color sensor
     */
    public boolean isBallDetected() {
        String color = getColorName();
        return !color.equalsIgnoreCase(RobotConstants.ColorSensor.NO_BALL_COLOR_1) &&
               !color.equalsIgnoreCase(RobotConstants.ColorSensor.NO_BALL_COLOR_2);
    }

    /**
     * Get detected color name
     */
    public String getDetectedColor() {
        return getColorName();
    }

    /**
     * Reset to first launch position (for auto-shoot)
     * Call this before starting auto-shoot to ensure we start from position 0
     */
    public void resetToFirstLaunchPosition() {
        if (!isIntakeMode) {
            currentIndex = 0;
            servos.turret_1.setPosition(RobotConstants.TurretServo.LAUNCH_POSITIONS[0]);
        }
    }

    // ========== INTAKE MOTOR CONTROL ==========

    /**
     * Start intake motor
     */
    public void startIntakeMotor() {
        if (motors != null && motors.intake != null) {
            motors.intake.setPower(1.0);
            isIntakeMotorRunning = true;
        }
    }

    /**
     * Stop intake motor
     */
    public void stopIntakeMotor() {
        if (motors != null && motors.intake != null) {
            motors.intake.setPower(0);
            isIntakeMotorRunning = false;
        }
    }

    /**
     * Set intake motor power (for variable speed control)
     * @param power Motor power (-1.0 to 1.0, positive = intake, negative = outtake)
     */
    public void setIntakeMotorPower(double power) {
        if (motors != null && motors.intake != null) {
            motors.intake.setPower(power);
            isIntakeMotorRunning = (Math.abs(power) > 0.01);
        }
    }

    /**
     * Toggle intake motor on/off
     */
    public void toggleIntakeMotor() {
        if (isIntakeMotorRunning) {
            stopIntakeMotor();
        } else {
            startIntakeMotor();
        }
    }

    /**
     * Check if intake motor is running
     */
    public boolean isIntakeMotorRunning() {
        return isIntakeMotorRunning;
    }

    /**
     * Stop all intake system components (for emergency stop)
     */
    public void stopAll() {
        stopIntakeMotor();
    }
}

