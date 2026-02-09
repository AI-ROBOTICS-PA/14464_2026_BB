package org.firstinspires.ftc.teamcode.decode14464.systems;

import com.qualcomm.robotcore.hardware.HardwareMap;

/**
 * Feature 4: Auto-Intake System
 * Controls intake motor, turret servo, and automatically advances when ball is detected
 * Uses centralized RobotSensors, RobotMotors, and RobotServos
 *
 * IMPORTANT: ballsLoaded tracks actual balls detected by color sensor (0-3).
 * This is SEPARATE from currentIndex which tracks the current servo slot position (0-2).
 * Manual D-Pad slot changes affect currentIndex only, NOT ballsLoaded.
 * Auto-shoot uses ballsLoaded to know how many balls to shoot.
 */
public class AutoIntakeSystem {
    // Centralized hardware references
    private RobotSensors sensors;
    private RobotMotors motors;
    private RobotServos servos;

    private boolean isIntakeMode = false;
    private boolean isIntakeMotorRunning = true; // Auto-start intake motor
    private int currentIndex = 0;       // Current servo slot position (0-2)
    private int ballsLoaded = 0;        // Actual balls detected by sensor (0-3), independent of currentIndex
    private String[] ballColors = {"None", "None", "None"}; // Color of ball in each slot (0-2)
    private String previousColor = RobotConstants.ColorSensor.NO_BALL_COLOR_1;
    private boolean transitionLocked = false;
    private long lastBallDetectedTime = 0; // Timestamp of last ball detection (for debounce)

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
            // Ball detected! Record color, increment count, then advance servo
            int slotFilled = Math.min(ballsLoaded, 2); // Which slot this ball went into (0-2)
            ballColors[slotFilled] = currentColor;
            ballsLoaded = Math.min(ballsLoaded + 1, 3);
            advanceToNextSlot(); // Moves servo only (ballsLoaded already incremented above)
            transitionLocked = true;
            lastBallDetectedTime = System.currentTimeMillis(); // Record when we detected this ball
        }

        // Update previous color and lock state
        // DEBOUNCE: Only unlock after COLOR_DEBOUNCE_MS has passed since last detection.
        // This prevents sensor flicker (ball → no ball → ball) from double-counting.
        long timeSinceLastDetection = System.currentTimeMillis() - lastBallDetectedTime;
        if (isNoBall && timeSinceLastDetection > RobotConstants.ColorSensor.COLOR_DEBOUNCE_MS) {
            // No ball AND enough time has passed - safe to unlock
            transitionLocked = false;
            previousColor = RobotConstants.ColorSensor.NO_BALL_COLOR_1;
        } else if (!isNoBall) {
            // Ball present - remember color (keep locked if locked)
            previousColor = currentColor;
        }
        // If isNoBall but within debounce window: do nothing (stay locked, keep previousColor)
    }

    /**
     * Switch to intake mode
     * Positions turret at the next EMPTY slot based on ballsLoaded.
     * - 0 balls loaded → slot 0 (ready for first ball)
     * - 1 ball loaded  → slot 1 (ready for second ball)
     * - 2 balls loaded → slot 2 (ready for third ball)
     * - 3 balls loaded → slot 2 (all full)
     */
    public void setIntakeMode() {
        isIntakeMode = true;
        // Position at the next empty slot (ballsLoaded index), capped at max slot
        currentIndex = Math.min(ballsLoaded, RobotConstants.TurretServo.INTAKE_POSITIONS.length - 1);
        if (servos != null && servos.turret_1 != null) {
            servos.turret_1.setPosition(RobotConstants.TurretServo.INTAKE_POSITIONS[currentIndex]);
        }

        // Reset color tracking
        previousColor = RobotConstants.ColorSensor.NO_BALL_COLOR_1;
        transitionLocked = false;
    }

    /**
     * Switch to launch mode
     * Positions turret at the HIGHEST loaded slot based on ballsLoaded.
     * - 0 balls loaded → slot 0 (nothing to shoot, but park at slot 0)
     * - 1 ball loaded  → slot 0 (ball at slot 0)
     * - 2 balls loaded → slot 1 (balls at slots 0 and 1)
     * - 3 balls loaded → slot 2 (balls at slots 0, 1, and 2)
     */
    public void setLaunchMode() {
        isIntakeMode = false;

        if (servos != null && servos.turret_1 != null) {
            // Position at the highest loaded slot (ballsLoaded - 1), minimum 0
            currentIndex = Math.max(ballsLoaded - 1, 0);
            servos.turret_1.setPosition(RobotConstants.TurretServo.LAUNCH_POSITIONS[currentIndex]);
        }

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
     * Advance to next slot (servo position only).
     * Does NOT change ballsLoaded. Used by:
     * - Auto-advance in update() (ballsLoaded incremented separately before this call)
     * - Manual D-Pad repositioning (no ball count change)
     * - Legacy ShootingSystem
     */
    public void advanceToNextSlot() {
        if (servos == null || servos.turret_1 == null) return;
        
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
     * Move to previous slot (servo position only).
     * Does NOT change ballsLoaded. Used by:
     * - Manual D-Pad repositioning (no ball count change)
     * - AutoShootSystem to step backward through loaded balls during shooting
     */
    public void moveToPreviousSlot() {
        if (servos == null || servos.turret_1 == null) return;
        
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
     * Also resets ball count since this implies a fresh start
     */
    public void resetToFirstLaunchPosition() {
        if (!isIntakeMode && servos != null && servos.turret_1 != null) {
            currentIndex = 0;
            ballsLoaded = 0;
            ballColors[0] = "None";
            ballColors[1] = "None";
            ballColors[2] = "None";
            servos.turret_1.setPosition(RobotConstants.TurretServo.LAUNCH_POSITIONS[0]);
        }
    }

    /**
     * Move turret to the highest loaded slot in launch mode.
     * Call this before auto-shoot to ensure we start shooting from the correct position,
     * even if manual D-Pad changed the currentIndex.
     */
    public void prepareForShooting() {
        if (!isIntakeMode && servos != null && servos.turret_1 != null && ballsLoaded > 0) {
            currentIndex = ballsLoaded - 1;
            servos.turret_1.setPosition(RobotConstants.TurretServo.LAUNCH_POSITIONS[currentIndex]);
        }
    }

    // ========== MANUAL INTAKE OVERRIDE (Sensor Broken Fallback) ==========
    // Use these when the color sensor is not working.
    // These deliberately change ballsLoaded + move servo, unlike regular D-Pad
    // which only repositions the servo without changing ball count.

    /**
     * MANUAL FALLBACK: Advance intake slot AND increment ball count.
     * Use when color sensor is broken and operator visually confirms a ball loaded.
     * Records color as "Unknown" since sensor isn't available.
     * Only works in intake mode.
     * @return true if advanced, false if already at max or not in intake mode
     */
    public boolean manualAdvanceIntake() {
        if (!isIntakeMode) return false;
        if (ballsLoaded >= 3) return false;
        if (servos == null || servos.turret_1 == null) return false;

        // Record color as Unknown (sensor broken)
        int slotFilled = Math.min(ballsLoaded, 2);
        ballColors[slotFilled] = "Unknown";

        // Increment ball count
        ballsLoaded = Math.min(ballsLoaded + 1, 3);

        // Advance servo to next slot (if not already at max)
        if (currentIndex < RobotConstants.TurretServo.INTAKE_POSITIONS.length - 1) {
            currentIndex++;
            servos.turret_1.setPosition(RobotConstants.TurretServo.INTAKE_POSITIONS[currentIndex]);
        }
        return true;
    }

    /**
     * MANUAL FALLBACK: Retreat intake slot AND decrement ball count.
     * Use when operator made a mistake with manual advance (no ball was actually loaded).
     * Clears the color of the vacated slot.
     * Only works in intake mode.
     * @return true if retreated, false if already at 0 or not in intake mode
     */
    public boolean manualRetreatIntake() {
        if (!isIntakeMode) return false;
        if (ballsLoaded <= 0) return false;
        if (servos == null || servos.turret_1 == null) return false;

        // Decrement ball count and clear the last loaded slot's color
        ballsLoaded = Math.max(ballsLoaded - 1, 0);
        ballColors[Math.min(ballsLoaded, 2)] = "None";

        // Retreat servo (if not already at 0)
        if (currentIndex > 0) {
            currentIndex--;
            servos.turret_1.setPosition(RobotConstants.TurretServo.INTAKE_POSITIONS[currentIndex]);
        }
        return true;
    }

    // ========== BALL COUNT TRACKING ==========

    /**
     * Get the actual number of balls loaded (detected by color sensor).
     * This is independent of currentIndex (servo position).
     * @return Number of balls loaded (0-3)
     */
    public int getBallsLoaded() {
        return ballsLoaded;
    }

    /**
     * Manually set ball count (for auto programs with pre-loaded balls)
     * @param count Number of balls (0-3)
     */
    public void setBallsLoaded(int count) {
        ballsLoaded = Math.max(0, Math.min(3, count));
    }

    /**
     * Reset ball count to 0 (e.g., after shooting all balls)
     * Also clears recorded ball colors
     */
    public void resetBallCount() {
        ballsLoaded = 0;
        ballColors[0] = "None";
        ballColors[1] = "None";
        ballColors[2] = "None";
    }

    // ========== BALL COLOR TRACKING ==========
    // Ball colors can only be Blue or Green (defined in RobotConstants.ColorSensor)

    /**
     * Get the color of the ball in a specific slot.
     * @param slot Slot index (0-2)
     * @return "Blue", "Green", or "None" if empty
     */
    public String getBallColor(int slot) {
        if (slot < 0 || slot > 2) return "None";
        return ballColors[slot];
    }

    /**
     * Get colors of all loaded balls as an array.
     * Index 0 = first ball loaded, index 1 = second, index 2 = third.
     * Unloaded slots contain "None".
     * @return Copy of the ball colors array (safe to modify)
     */
    public String[] getAllBallColors() {
        return new String[]{ballColors[0], ballColors[1], ballColors[2]};
    }

    /**
     * Get a summary string of loaded ball colors for telemetry.
     * Example: "[Blue, Green, Blue]" or "[Blue, Green, -]" if only 2 loaded.
     */
    public String getBallColorSummary() {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < 3; i++) {
            if (i > 0) sb.append(", ");
            if (i < ballsLoaded) {
                sb.append(ballColors[i]);
            } else {
                sb.append("-");
            }
        }
        sb.append("]");
        return sb.toString();
    }

    /**
     * Get number of Blue balls loaded.
     */
    public int getBlueCount() {
        int count = 0;
        for (int i = 0; i < ballsLoaded && i < 3; i++) {
            if (ballColors[i].equalsIgnoreCase(RobotConstants.ColorSensor.BALL_COLOR_BLUE)) {
                count++;
            }
        }
        return count;
    }

    /**
     * Get number of Green balls loaded.
     */
    public int getGreenCount() {
        int count = 0;
        for (int i = 0; i < ballsLoaded && i < 3; i++) {
            if (ballColors[i].equalsIgnoreCase(RobotConstants.ColorSensor.BALL_COLOR_GREEN)) {
                count++;
            }
        }
        return count;
    }

    /**
     * Check if any Blue ball is loaded.
     */
    public boolean hasBlueBall() {
        return getBlueCount() > 0;
    }

    /**
     * Check if any Green ball is loaded.
     */
    public boolean hasGreenBall() {
        return getGreenCount() > 0;
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

