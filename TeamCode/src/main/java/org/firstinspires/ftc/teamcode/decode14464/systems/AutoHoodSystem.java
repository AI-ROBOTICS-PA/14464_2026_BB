package org.firstinspires.ftc.teamcode.decode14464.systems;

import com.qualcomm.robotcore.hardware.HardwareMap;
import org.firstinspires.ftc.teamcode.decode14464.RobotServos;

/**
 * Feature 3: Auto-Hood System
 * Automatically adjusts hood angle based on distance to goal
 * Uses centralized RobotServos
 */
public class AutoHoodSystem {
    private RobotServos servos;
    private LocalizationSystem localization;
    private boolean autoAdjustEnabled = false;

    /**
     * Initialize hood system with centralized servos
     */
    public void init(HardwareMap hardwareMap, LocalizationSystem localizationSystem, RobotServos servos) {
        this.servos = servos;
        this.localization = localizationSystem;

        // Set to home position
        setPosition(RobotConstants.Hood.HOME_POSITION);
    }

    /**
     * Enable auto-adjustment based on distance
     */
    public void enableAutoAdjust() {
        autoAdjustEnabled = true;
    }

    /**
     * Disable auto-adjustment
     */
    public void disableAutoAdjust() {
        autoAdjustEnabled = false;
    }

    /**
     * Toggle auto-adjustment
     */
    public void toggleAutoAdjust() {
        autoAdjustEnabled = !autoAdjustEnabled;
    }

    /**
     * Check if auto-adjust is enabled
     */
    public boolean isAutoAdjustEnabled() {
        return autoAdjustEnabled;
    }

    /**
     * Update hood position - call this every loop
     */
    public void update() {
        if (!autoAdjustEnabled || servos == null || servos.hood_5 == null || localization == null) {
            return;
        }

        // Get distance to goal
        double distance = localization.getDistanceToPoint(
            RobotConstants.Field.GOAL_X,
            RobotConstants.Field.GOAL_Y
        );

        // Calculate servo position based on distance
        double position = calculatePositionForDistance(distance);

        // Set hood position
        setPosition(position);
    }

    /**
     * Calculate servo position based on distance using linear interpolation
     */
    private double calculatePositionForDistance(double distance) {
        // Clamp distance to range
        if (distance <= RobotConstants.Hood.CLOSE_DISTANCE) {
            return RobotConstants.Hood.CLOSE_POSITION;
        } else if (distance >= RobotConstants.Hood.FAR_DISTANCE) {
            return RobotConstants.Hood.FAR_POSITION;
        } else if (distance <= RobotConstants.Hood.MID_DISTANCE) {
            // Interpolate between close and mid
            double ratio = (distance - RobotConstants.Hood.CLOSE_DISTANCE) /
                          (RobotConstants.Hood.MID_DISTANCE - RobotConstants.Hood.CLOSE_DISTANCE);
            return RobotConstants.Hood.CLOSE_POSITION +
                   ratio * (RobotConstants.Hood.MID_POSITION - RobotConstants.Hood.CLOSE_POSITION);
        } else {
            // Interpolate between mid and far
            double ratio = (distance - RobotConstants.Hood.MID_DISTANCE) /
                          (RobotConstants.Hood.FAR_DISTANCE - RobotConstants.Hood.MID_DISTANCE);
            return RobotConstants.Hood.MID_POSITION +
                   ratio * (RobotConstants.Hood.FAR_POSITION - RobotConstants.Hood.MID_POSITION);
        }
    }

    /**
     * Set hood to long range position
     */
    public void setLongRange() {
        setPosition(RobotConstants.Hood.LONG_RANGE_POSITION);
        autoAdjustEnabled = false;
    }

    /**
     * Set hood to short range position
     */
    public void setShortRange() {
        setPosition(RobotConstants.Hood.SHORT_RANGE_POSITION);
        autoAdjustEnabled = false;
    }

    /**
     * Set hood to home position
     */
    public void setHome() {
        setPosition(RobotConstants.Hood.HOME_POSITION);
        autoAdjustEnabled = false;
    }

    /**
     * Set hood position manually (0.0 to 1.0)
     */
    public void setPosition(double position) {
        if (servos != null && servos.hood_5 != null) {
            // Clamp to safe range
            position = Math.max(RobotConstants.Hood.MIN_POSITION,
                               Math.min(RobotConstants.Hood.MAX_POSITION, position));
            servos.hood_5.setPosition(position);
        }
    }

    /**
     * Increment hood position
     */
    public void increment(double amount) {
        if (servos != null && servos.hood_5 != null) {
            double newPosition = servos.hood_5.getPosition() + amount;
            setPosition(newPosition);
            autoAdjustEnabled = false; // Manual control
        }
    }

    /**
     * Decrement hood position
     */
    public void decrement(double amount) {
        increment(-amount);
    }

    /**
     * Get current hood position
     */
    public double getPosition() {
        return (servos != null && servos.hood_5 != null) ? servos.hood_5.getPosition() : 0;
    }

    /**
     * Get distance to goal
     */
    public double getDistanceToGoal() {
        if (localization == null) return 0;
        return localization.getDistanceToPoint(
            RobotConstants.Field.GOAL_X,
            RobotConstants.Field.GOAL_Y
        );
    }
}
