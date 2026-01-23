package org.firstinspires.ftc.teamcode.decode14464;

import com.qualcomm.hardware.limelightvision.LLResult;
import com.qualcomm.hardware.limelightvision.LLResultTypes;
import com.qualcomm.hardware.limelightvision.Limelight3A;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.pedropathing.geometry.Pose;
import org.firstinspires.ftc.robotcore.external.navigation.Pose3D;
import java.util.List;

/**
 * LimelightManager - Handles Limelight camera for AprilTag detection and robot localization
 */
public class LimelightManager {
    private Limelight3A limelight;
    private LLResult latestResult;

    // Field dimensions and AprilTag positions (adjust these based on your field setup)
    // These are example values - you need to configure based on your actual field
    private static final double FIELD_WIDTH = 144.0;  // inches
    private static final double FIELD_LENGTH = 144.0; // inches

    /**
     * Initialize the Limelight camera
     */
    public void init(HardwareMap hardwareMap) {
        try {
            limelight = hardwareMap.get(Limelight3A.class, "limelight");
            // Set pipeline to AprilTag detection (adjust pipeline index as needed)
            limelight.pipelineSwitch(0);
            // Start polling for data
            limelight.start();
        } catch (Exception e) {
            // Limelight not found - will handle gracefully
            limelight = null;
        }
    }

    /**
     * Update the latest result from Limelight
     * Call this in your loop() method
     */
    public void update() {
        if (limelight != null) {
            latestResult = limelight.getLatestResult();
        }
    }

    /**
     * Check if Limelight is connected and working
     */
    public boolean isConnected() {
        return limelight != null;
    }

    /**
     * Check if we have valid AprilTag data
     */
    public boolean hasValidTarget() {
        return latestResult != null && latestResult.isValid();
    }

    /**
     * Get robot pose from AprilTag detection (botpose)
     * Returns robot position on the field based on AprilTag localization
     *
     * @return Pose object with X, Y, and heading, or null if no valid data
     */
    public Pose getRobotPoseFromAprilTag() {
        if (!hasValidTarget()) {
            return null;
        }
        try {
            // Get botpose from Limelight (robot position relative to field)
            Pose3D botpose = latestResult.getBotpose();
            if (botpose != null) {
                // Convert from Limelight coordinates to Pedro Pathing coordinates
                // Limelight uses different coordinate system, adjust as needed
                double x = botpose.getPosition().x;
                double y = botpose.getPosition().y;
                // Extract heading from orientation (yaw)
                double heading = Math.toRadians(botpose.getOrientation().getYaw());
                return new Pose(x, y, heading);
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }

    /**
     * Get list of detected AprilTags
     */
    public List<LLResultTypes.FiducialResult> getAprilTags() {
        if (hasValidTarget()) {
            return latestResult.getFiducialResults();
        }
        return null;
    }

    /**
     * Check if a specific AprilTag is visible
     * @param tagId The AprilTag ID to check for
     * @return true if the tag is visible
     */
    public boolean isAprilTagVisible(int tagId) {
        List<LLResultTypes.FiducialResult> tags = getAprilTags();
        if (tags == null) return false;
        for (LLResultTypes.FiducialResult tag : tags) {
            if (tag.getFiducialId() == tagId) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get a specific AprilTag by ID
     * @param tagId The AprilTag ID to find
     * @return FiducialResult for the tag, or null if not found
     */
    public LLResultTypes.FiducialResult getAprilTag(int tagId) {
        List<LLResultTypes.FiducialResult> tags = getAprilTags();
        if (tags == null) return null;
        for (LLResultTypes.FiducialResult tag : tags) {
            if (tag.getFiducialId() == tagId) {
                return tag;
            }
        }
        return null;
    }

    /**
     * Get horizontal angle to target (tx)
     */
    public double getTargetX() {
        if (hasValidTarget()) {
            return latestResult.getTx();
        }
        return 0.0;
    }

    /**
     * Get vertical angle to target (ty)
     */
    public double getTargetY() {
        if (hasValidTarget()) {
            return latestResult.getTy();
        }
        return 0.0;
    }

    /**
     * Get targeting latency in milliseconds
     */
    public double getLatency() {
        if (hasValidTarget()) {
            return latestResult.getCaptureLatency() + latestResult.getTargetingLatency();
        }
        return 0.0;
    }

    /**
     * Get number of visible AprilTags
     */
    public int getVisibleTagCount() {
        List<LLResultTypes.FiducialResult> tags = getAprilTags();
        return (tags != null) ? tags.size() : 0;
    }

    /**
     * Shutdown Limelight
     */
    public void stop() {
        if (limelight != null) {
            limelight.stop();
        }
    }
}

