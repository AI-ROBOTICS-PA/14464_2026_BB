package org.firstinspires.ftc.teamcode.decode14464;

import com.qualcomm.hardware.limelightvision.LLResult;
import com.qualcomm.hardware.limelightvision.LLResultTypes;
import com.qualcomm.hardware.limelightvision.Limelight3A;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.pedropathing.geometry.Pose;
import org.firstinspires.ftc.robotcore.external.navigation.Pose3D;
import java.util.List;

/**
 * LimelightManager - Efficient Limelight wrapper with caching
 * 
 * USAGE:
 * 1. Call update() once per loop cycle
 * 2. All getter methods use cached data (no redundant processing)
 * 
 * Alliance-based AprilTag filtering:
 * - BLUE Alliance: Uses Tag ID 20
 * - RED Alliance: Uses Tag ID 24
 */
public class LimelightManager {
    private Limelight3A limelight;
    private LLResult latestResult;

    // ========== ALLIANCE CONFIGURATION ==========
    public enum Alliance {
        BLUE,  // Tag ID 20
        RED    // Tag ID 24
    }
    
    private Alliance currentAlliance = Alliance.BLUE;
    
    // AprilTag IDs for each alliance zone
    public static final int BLUE_TAG_ID = 20;
    public static final int RED_TAG_ID = 24;
    
    // Meters to inches conversion
    private static final double METERS_TO_INCHES = 39.3701;

    // ========== CACHED DATA (updated once per cycle in update()) ==========
    private boolean cacheValid = false;
    private List<LLResultTypes.FiducialResult> cachedTags = null;
    private int cachedTagCount = 0;
    private boolean cachedHasValidTarget = false;
    private boolean cachedAllianceTagVisible = false;
    private LLResultTypes.FiducialResult cachedAllianceTag = null;
    private double cachedTx = 0.0;
    private double cachedTy = 0.0;
    private long cachedStaleness = -1;
    private Pose cachedBotPose = null;
    private String cachedTagIdsString = "None";

    /**
     * Initialize the Limelight camera
     */
    public void init(HardwareMap hardwareMap) {
        try {
            limelight = hardwareMap.get(Limelight3A.class, "limelight");
            limelight.pipelineSwitch(0);
            limelight.start();
        } catch (Exception e) {
            limelight = null;
        }
    }

    // ========== ALLIANCE METHODS ==========
    
    public void setAlliance(Alliance alliance) {
        this.currentAlliance = alliance;
        // Invalidate cache when alliance changes
        cacheValid = false;
    }

    public Alliance getAlliance() {
        return currentAlliance;
    }

    public void toggleAlliance() {
        currentAlliance = (currentAlliance == Alliance.BLUE) ? Alliance.RED : Alliance.BLUE;
        cacheValid = false;
    }

    public int getTargetTagId() {
        return (currentAlliance == Alliance.BLUE) ? BLUE_TAG_ID : RED_TAG_ID;
    }

    // ========== MAIN UPDATE METHOD - CALL ONCE PER LOOP ==========
    
    /**
     * Update all cached data from Limelight.
     * Call this ONCE at the start of your loop.
     * All other methods will use cached data for efficiency.
     */
    public void update() {
        // Reset cache
        cacheValid = false;
        cachedTags = null;
        cachedTagCount = 0;
        cachedHasValidTarget = false;
        cachedAllianceTagVisible = false;
        cachedAllianceTag = null;
        cachedTx = 0.0;
        cachedTy = 0.0;
        cachedStaleness = -1;
        cachedBotPose = null;
        cachedTagIdsString = "None";
        
        if (limelight == null) {
            return;
        }
        
        // Get latest result
        latestResult = limelight.getLatestResult();
        
        if (latestResult == null || !latestResult.isValid()) {
            return;
        }
        
        // Valid result - cache all data
        cachedHasValidTarget = true;
        cachedTx = latestResult.getTx();
        cachedTy = latestResult.getTy();
        cachedStaleness = latestResult.getStaleness();
        
        // Cache tag list and process it ONCE
        cachedTags = latestResult.getFiducialResults();
        if (cachedTags != null) {
            cachedTagCount = cachedTags.size();
            
            // Build tag IDs string and find alliance tag in ONE pass
            int targetId = getTargetTagId();
            StringBuilder sb = new StringBuilder();
            
            for (int i = 0; i < cachedTags.size(); i++) {
                LLResultTypes.FiducialResult tag = cachedTags.get(i);
                int tagId = tag.getFiducialId();
                
                // Build string
                if (i > 0) sb.append(", ");
                sb.append(tagId);
                
                // Check if this is our alliance tag
                if (tagId == targetId) {
                    sb.append("*");
                    cachedAllianceTagVisible = true;
                    cachedAllianceTag = tag;
                }
            }
            
            cachedTagIdsString = sb.length() > 0 ? sb.toString() : "None";
        }
        
        // Cache bot pose
        try {
            Pose3D botpose = latestResult.getBotpose();
            if (botpose != null) {
                double x = botpose.getPosition().x;
                double y = botpose.getPosition().y;
                double heading = Math.toRadians(botpose.getOrientation().getYaw());
                cachedBotPose = new Pose(x, y, heading);
            }
        } catch (Exception e) {
            cachedBotPose = null;
        }
        
        cacheValid = true;
    }

    // ========== FAST CACHED GETTERS (O(1) - no iteration) ==========

    public boolean isConnected() {
        return limelight != null;
    }

    public boolean hasValidTarget() {
        return cachedHasValidTarget;
    }

    public boolean isAllianceTagVisible() {
        return cachedAllianceTagVisible;
    }
    
    public boolean hasAllianceTarget() {
        return cachedAllianceTagVisible;
    }

    public LLResultTypes.FiducialResult getAllianceTag() {
        return cachedAllianceTag;
    }

    public List<LLResultTypes.FiducialResult> getAprilTags() {
        return cachedTags;
    }

    public int getVisibleTagCount() {
        return cachedTagCount;
    }

    public String getVisibleTagIdsString() {
        return cachedTagIdsString;
    }

    public long getStaleness() {
        return cachedStaleness;
    }

    public boolean isDataFresh() {
        return cachedStaleness >= 0 && cachedStaleness < 100;
    }

    public Pose getRobotPoseFromAprilTag() {
        return cachedBotPose;
    }

    // ========== TX/TY METHODS ==========

    /**
     * Get tx (horizontal angle) - ALLIANCE FILTERED
     * Only returns tx if our alliance's AprilTag is visible
     */
    public double getTargetX() {
        return cachedAllianceTagVisible ? cachedTx : 0.0;
    }

    /**
     * Get ty (vertical angle) - ALLIANCE FILTERED
     * Only returns ty if our alliance's AprilTag is visible
     */
    public double getTargetY() {
        return cachedAllianceTagVisible ? cachedTy : 0.0;
    }

    /**
     * Get tx - UNFILTERED (any tag)
     */
    public double getTargetXUnfiltered() {
        return cachedHasValidTarget ? cachedTx : 0.0;
    }

    /**
     * Get ty - UNFILTERED (any tag)
     */
    public double getTargetYUnfiltered() {
        return cachedHasValidTarget ? cachedTy : 0.0;
    }

    // ========== TAG LOOKUP METHODS (requires iteration) ==========

    /**
     * Check if a specific AprilTag is visible by ID
     * Note: For alliance tag, use isAllianceTagVisible() which is O(1)
     */
    public boolean isAprilTagVisible(int tagId) {
        // Fast path for alliance tag
        if (tagId == getTargetTagId()) {
            return cachedAllianceTagVisible;
        }
        // Slow path for other tags
        if (cachedTags == null) return false;
        for (LLResultTypes.FiducialResult tag : cachedTags) {
            if (tag.getFiducialId() == tagId) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get a specific AprilTag by ID
     * Note: For alliance tag, use getAllianceTag() which is O(1)
     */
    public LLResultTypes.FiducialResult getAprilTag(int tagId) {
        // Fast path for alliance tag
        if (tagId == getTargetTagId()) {
            return cachedAllianceTag;
        }
        // Slow path for other tags
        if (cachedTags == null) return null;
        for (LLResultTypes.FiducialResult tag : cachedTags) {
            if (tag.getFiducialId() == tagId) {
                return tag;
            }
        }
        return null;
    }

    /**
     * Get ID of first visible tag (for debugging)
     */
    public int getFirstVisibleTagId() {
        if (cachedTags != null && !cachedTags.isEmpty()) {
            return cachedTags.get(0).getFiducialId();
        }
        return -1;
    }

    // ========== DISTANCE METHODS ==========

    /**
     * Get distance to alliance tag in inches
     */
    public double getDistanceToAllianceTag() {
        return getDistanceFromTag(cachedAllianceTag);
    }

    /**
     * Get distance to a specific tag by ID
     */
    public double getDistanceToTag(int tagId) {
        return getDistanceFromTag(getAprilTag(tagId));
    }

    /**
     * Get distance to the first visible tag
     */
    public double getDistanceToFirstTag() {
        if (cachedTags != null && !cachedTags.isEmpty()) {
            return getDistanceFromTag(cachedTags.get(0));
        }
        return 0.0;
    }

    /**
     * Internal helper to calculate distance from a FiducialResult
     */
    private double getDistanceFromTag(LLResultTypes.FiducialResult tag) {
        if (tag == null) return 0.0;
        try {
            Pose3D targetPose = tag.getTargetPoseRobotSpace();
            if (targetPose != null) {
                double x = targetPose.getPosition().x * METERS_TO_INCHES;
                double y = targetPose.getPosition().y * METERS_TO_INCHES;
                double z = targetPose.getPosition().z * METERS_TO_INCHES;
                return Math.sqrt(x*x + y*y + z*z);
            }
        } catch (Exception e) {
            // Ignore
        }
        return 0.0;
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
