package org.firstinspires.ftc.teamcode.decode14464;

import com.qualcomm.hardware.limelightvision.LLResultTypes;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;

import org.firstinspires.ftc.teamcode.decode14464.systems.LimelightManager;

import java.util.List;

/**
 * TagReader - Simple test program to read AprilTags from Limelight
 *
 * This program displays all detected AprilTags and their data
 * Use this to verify Limelight is working and what tags it sees
 *
 * Controls:
 * - Press START to begin reading
 * - Watch telemetry for detected tags
 */
// @TeleOp(name = "Tag Reader", group = "Testing")  // DISABLED
public class TagReader extends OpMode {

    private LimelightManager limelightManager = new LimelightManager();

    @Override
    public void init() {
        // Initialize Limelight
        limelightManager.init(hardwareMap);

        telemetry.addLine("=================================");
        telemetry.addLine("  TAG READER - LIMELIGHT TEST");
        telemetry.addLine("=================================");
        telemetry.addLine();
        telemetry.addData("Limelight Status", limelightManager.isConnected() ? "✓ Connected" : "✗ NOT FOUND");
        telemetry.addLine();
        telemetry.addLine("Press START to begin reading tags");
        telemetry.addLine("=================================");
        telemetry.update();
    }

    @Override
    public void loop() {
        // Update Limelight data
        limelightManager.update();

        // Clear telemetry
        telemetry.clear();
        telemetry.addLine("=================================");
        telemetry.addLine("  TAG READER - LIVE DATA");
        telemetry.addLine("=================================");

        // Check Limelight connection
        if (!limelightManager.isConnected()) {
            telemetry.addLine();
            telemetry.addLine("❌ LIMELIGHT NOT CONNECTED");
            telemetry.addLine();
            telemetry.addLine("Troubleshooting:");
            telemetry.addLine("1. Check USB cable");
            telemetry.addLine("2. Check Robot Configuration");
            telemetry.addLine("3. Verify device name: 'limelight'");
            telemetry.addLine("4. Restart Robot Controller");
            telemetry.addLine("=================================");
            telemetry.update();
            return;
        }

        telemetry.addData("Limelight", "✓ Connected");
        telemetry.addLine();

        // Check if we have valid data
        if (!limelightManager.hasValidTarget()) {
            telemetry.addLine("⚠️ NO TARGETS DETECTED");
            telemetry.addLine();
            telemetry.addLine("Make sure:");
            telemetry.addLine("• Pipeline 0 is AprilTag mode");
            telemetry.addLine("• AprilTag is in camera view");
            telemetry.addLine("• Good lighting conditions");
            telemetry.addLine("• Tag is not too far away");
            telemetry.addLine("• Tag is flat and not damaged");
            telemetry.addLine("=================================");
            telemetry.update();
            return;
        }

        // Get all detected tags
        List<LLResultTypes.FiducialResult> tags = limelightManager.getAprilTags();

        if (tags == null || tags.isEmpty()) {
            telemetry.addLine("⚠️ NO APRILTAGS FOUND");
            telemetry.addLine();
            telemetry.addLine("Valid target detected but");
            telemetry.addLine("no AprilTags in the data.");
            telemetry.addLine();
            telemetry.addLine("Check Limelight pipeline");
            telemetry.addLine("is set to AprilTag mode");
            telemetry.addLine("=================================");
            telemetry.update();
            return;
        }

        // Display number of tags found
        telemetry.addData("Tags Detected", "✓ " + tags.size() + " tag(s)");
        telemetry.addLine();
        telemetry.addLine("---------------------------------");

        // Display each detected tag
        for (int i = 0; i < tags.size(); i++) {
            LLResultTypes.FiducialResult tag = tags.get(i);

            telemetry.addLine();
            telemetry.addData("Tag #" + (i + 1), "ID: " + tag.getFiducialId());

            // Get tag position data
            try {
                // Target pose in robot space (relative to camera)
                org.firstinspires.ftc.robotcore.external.navigation.Pose3D targetPose =
                    tag.getTargetPoseRobotSpace();

                if (targetPose != null) {
                    // Limelight returns position in METERS, convert to INCHES
                    double x = targetPose.getPosition().x * 39.3701; // meters to inches
                    double y = targetPose.getPosition().y * 39.3701;
                    double z = targetPose.getPosition().z * 39.3701;

                    telemetry.addData("  Position X", "%.2f inches", x);
                    telemetry.addData("  Position Y", "%.2f inches", y);
                    telemetry.addData("  Position Z", "%.2f inches", z);

                    // Calculate distance in inches
                    double distance = Math.sqrt(x*x + y*y + z*z);
                    telemetry.addData("  Distance", "%.2f inches", distance);
                }
            } catch (Exception e) {
                telemetry.addLine("  (Position data unavailable)");
            }

            // Get corner data if available
            try {
                List<List<Double>> corners = tag.getTargetCorners();
                if (corners != null && !corners.isEmpty()) {
                    telemetry.addData("  Corners", corners.size() + " detected");
                }
            } catch (Exception e) {
                // Corner data not available
            }
        }

        telemetry.addLine();
        telemetry.addLine("---------------------------------");

        // Additional Limelight data
        telemetry.addLine();
        telemetry.addData("tx (Horizontal)", "%.2f°", limelightManager.getTargetXUnfiltered());
        telemetry.addData("ty (Vertical)", "%.2f°", limelightManager.getTargetYUnfiltered());
        long staleness = limelightManager.getStaleness();
        telemetry.addData("Data Staleness", staleness >= 0 ? staleness + " ms" : "N/A");
        telemetry.addData("Data Fresh", limelightManager.isDataFresh() ? "✓ Yes" : "✗ No");

        telemetry.addLine();
        telemetry.addLine("=================================");

        // Alliance info
        telemetry.addLine();
        String alliance = limelightManager.getAlliance() == LimelightManager.Alliance.BLUE ? "BLUE" : "RED";
        telemetry.addData("Current Alliance", alliance + " (Target Tag: " + limelightManager.getTargetTagId() + ")");
        telemetry.addData("Alliance Tag Visible", limelightManager.isAllianceTagVisible() ? "✓ YES" : "✗ No");
        
        // Tags 20 and 24 status (efficient - uses cached data)
        telemetry.addLine();
        telemetry.addLine("TAG STATUS:");
        telemetry.addData("Tag 20 (BLUE)", limelightManager.isAprilTagVisible(20) ? "✓ VISIBLE" : "✗ Not found");
        telemetry.addData("Tag 24 (RED)", limelightManager.isAprilTagVisible(24) ? "✓ VISIBLE" : "✗ Not found");

        telemetry.addLine("=================================");
        telemetry.update();
    }

    @Override
    public void stop() {
        limelightManager.stop();
    }
}

