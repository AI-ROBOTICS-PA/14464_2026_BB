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
     * NEVER returns null - returns default pose if not initialized
     */
    public Pose getPose() {
        if (follower != null) {
            Pose pose = follower.getPose();
            if (pose != null) {
                return pose;
            }
        }
        return new Pose(0, 0, 0);
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
     * Uses standard math convention: 0° = +X direction (matches Pedro Pathing heading)
     * Forward = +X, Sideways = +Y
     */
    public double getAngleToPoint(double targetX, double targetY) {
        double dx = targetX - getX();
        double dy = targetY - getY();
        double angleRad = Math.atan2(dy, dx); // atan2(dy, dx) → 0° = +X direction
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
     * Calibrate full pose from Limelight AprilTag detection (X, Y, and heading)
     * @param limelightPose The pose from Limelight (can be null if no valid target)
     * @return true if calibration was applied, false if no valid data
     */
    public boolean calibrateFromLimelight(Pose limelightPose) {
        if (limelightPose == null || follower == null) {
            return false;
        }
        
        // Apply Limelight pose to correct odometry
        follower.setPose(limelightPose);
        return true;
    }

    /**
     * Calibrate only position (X, Y) from Limelight - keeps heading from odometry.
     * Use this during TeleOp: odometry heading is reliable, but X/Y can drift.
     * Limelight corrects position without disturbing the heading.
     * @param limelightPose The pose from Limelight (only X, Y are used)
     * @return true if calibration was applied
     */
    public boolean calibratePositionOnly(Pose limelightPose) {
        if (limelightPose == null || follower == null) {
            return false;
        }
        
        // Keep current heading from odometry, only update X and Y from Limelight
        double currentHeading = getHeadingRadians();
        Pose positionOnlyPose = new Pose(limelightPose.getX(), limelightPose.getY(), currentHeading);
        follower.setPose(positionOnlyPose);
        return true;
    }

    /**
     * Calibrate only heading from Limelight (keeps X,Y from odometry)
     * Useful when odometry X,Y is reliable but heading has drifted
     * @param headingRadians New heading in radians from Limelight
     * @return true if calibration was applied
     */
    public boolean calibrateHeadingOnly(double headingRadians) {
        if (follower == null) return false;
        
        Pose currentPose = getPose();
        Pose newPose = new Pose(currentPose.getX(), currentPose.getY(), headingRadians);
        follower.setPose(newPose);
        return true;
    }

    /**
     * Get follower instance for advanced usage
     */
    public Follower getFollower() {
        return follower;
    }
}

