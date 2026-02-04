package org.firstinspires.ftc.teamcode.decode14464.systems;

import com.pedropathing.follower.Follower;
import com.pedropathing.geometry.Pose;
import com.qualcomm.robotcore.hardware.HardwareMap;
import org.firstinspires.ftc.teamcode.pedroPathing.Constants;

/**
 * Feature 1: Localization System
 * Manages robot position and heading tracking using Pedro Pathing
 */
public class LocalizationSystem {
    private Follower follower;

    /**
     * Initialize the localization system for Auto programs
     * Sets a specific starting position
     */
    public void init(HardwareMap hardwareMap, Pose startPose) {
        follower = Constants.createFollower(hardwareMap);
        follower.setStartingPose(startPose);
    }

    /**
     * Initialize for TeleOp - preserves position from Auto
     * The Pedro Pathing follower will continue from where Auto left off
     * This maintains position continuity throughout the entire 2min 30sec match
     */
    public void initTeleOp(HardwareMap hardwareMap) {
        // Create follower WITHOUT setting starting pose
        // This preserves the position from the Auto period
        follower = Constants.createFollower(hardwareMap);
        // Note: Pedro Pathing automatically maintains position continuity
        // No need to reset pose - it continues from last known position
    }

    /**
     * Update localization - call this every loop
     */
    public void update() {
        if (follower != null) {
            follower.update();
        }
    }

    /**
     * Get current robot pose (position + heading)
     */
    public Pose getPose() {
        return follower != null ? follower.getPose() : new Pose(0, 0, 0);
    }

    /**
     * Get current X position in inches
     */
    public double getX() {
        return getPose().getX();
    }

    /**
     * Get current Y position in inches
     */
    public double getY() {
        return getPose().getY();
    }

    /**
     * Get current heading in radians
     */
    public double getHeadingRadians() {
        return getPose().getHeading();
    }

    /**
     * Get current heading in degrees (0-360)
     */
    public double getHeadingDegrees() {
        double degrees = Math.toDegrees(getHeadingRadians());
        while (degrees < 0) degrees += 360;
        while (degrees >= 360) degrees -= 360;
        return degrees;
    }

    /**
     * Calculate distance to a target point
     */
    public double getDistanceToPoint(double targetX, double targetY) {
        double dx = targetX - getX();
        double dy = targetY - getY();
        return Math.sqrt(dx * dx + dy * dy);
    }

    /**
     * Calculate angle to a target point (in degrees, 0-360)
     * Uses Pedro Pathing coordinate system (0° = UP)
     */
    public double getAngleToPoint(double targetX, double targetY) {
        double dx = targetX - getX();
        double dy = targetY - getY();
        double angleRad = Math.atan2(dx, dy); // Note: dx, dy for Pedro Pathing
        double angleDeg = Math.toDegrees(angleRad);
        while (angleDeg < 0) angleDeg += 360;
        while (angleDeg >= 360) angleDeg -= 360;
        return angleDeg;
    }

    /**
     * Set robot pose (for resetting/calibration)
     */
    public void setPose(Pose pose) {
        if (follower != null) {
            follower.setPose(pose);
        }
    }

    /**
     * Get follower instance for advanced usage
     */
    public Follower getFollower() {
        return follower;
    }
}

