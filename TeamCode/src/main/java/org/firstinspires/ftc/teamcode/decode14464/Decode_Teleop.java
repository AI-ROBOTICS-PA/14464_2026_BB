package org.firstinspires.ftc.teamcode.decode14464;

import com.bylazar.configurables.annotations.Configurable;
import com.bylazar.telemetry.PanelsTelemetry;
import com.bylazar.telemetry.TelemetryManager;
import com.pedropathing.follower.Follower;
import com.pedropathing.geometry.Pose;
import com.pedropathing.paths.PathChain;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import org.firstinspires.ftc.teamcode.pedroPathing.Constants;
import java.util.function.Supplier;

@Configurable
@TeleOp
public class Decode_Teleop extends OpMode {
    private Follower follower;
    public static Pose startingPose; //See ExampleAuto to understand how to use this
    private boolean automatedDrive;
    private Supplier<PathChain> pathChain;
    private TelemetryManager telemetryM;
    private boolean slowMode = false;
    private double slowModeMultiplier = 0.5;

    private RobotMotors robotMotors = new RobotMotors();
    private RobotServos robotServos = new RobotServos();
    private LimelightManager limelightManager = new LimelightManager();
    private RobotSensors robotSensors = new RobotSensors();

    // AutoTurret variables
    private DecodeUtils.TurretPIDState turretPidState = new DecodeUtils.TurretPIDState();
    private boolean autoTrackEnabled = true; // Default to auto-tracking enabled
    private int currentTargetTagID = TurretConstants.PRIMARY_TAG_ID; // Start with tag 20
    private boolean prevDpadUpPressed  = false;
    private boolean prevDpadDownPressed  = false;
    private boolean prevDpadLeftPressed  = false;
    private boolean prevDpadRightPressed  = false;
    private boolean prevDpadLeftPressed2  = false;
    private boolean prevDpadRightPressed2  = false;
    private boolean prevAPressed  = false;

    // Hood servo control
    private final double HOOD_INCREMENT = 0.01; // Amount to increment/decrement
    private double hood_servoPosition = HoodConstants.HOME_POSITION; // Initial servo position
    private final double BI_INCREMENT = 0.1; // Amount to increment/decrement
    private double BI_servoPosition = 0.67; // Initial servo position
    private final double BI_MIN_POSITION = 0.37;
    private final double BI_MAX_POSITION = 0.67;

    private boolean intakeMode = false;

    // Define discrete positions for each mode
    private final double[] INTAKE_POSITIONS = {0.07, 0.44, 0.81};
    private final double[] LAUNCH_POSITIONS = {0.17, 0.54, 0.91};

    // Current position index in the respective array
    private int currentTurretPositionIndex = 0;

    // Current actual servo position
    private double currentTurretServoPosition = 0.17; // Start in launch mode at first position

    private boolean leftBumped;
    private boolean isintakeActive = false;
    private boolean rightBumped;

    private boolean isshooterActive = false;
    private double targetShooterhighVelocity = 1450; // High velocity for long distance
    private double targetShooterlowVelocity = 1100; // Low velocity for short distance
    private double currentShooterVelocity = 1450; // Start with high velocity
    private boolean isHighVelocityMode = true; // Track current mode
    private boolean prevYPressed = false; // For Y button toggle

    // Auto-advance on color detection (intake mode only)
    private String previousColor = "Black";
    private boolean colorTransitionDetected = false;

    @Override
    public void init() {
        robotMotors.init(hardwareMap);
        robotServos.init(hardwareMap);
        robotSensors.init(hardwareMap); // <-- Ensure color sensors are initialized

        // Initialize turret for auto-tracking
        DecodeUtils.initializeTurretMotor(robotMotors.turret);

        // Initialize Limelight
        limelightManager.init(hardwareMap);

        // Initialize hood servo to home position
        if (robotServos.hood_5 != null) {
            robotServos.hood_5.setPosition(hood_servoPosition);
        }

        // Initialize turret servo to starting position
        if (robotServos.turret_1 != null) {
            robotServos.turret_1.setPosition(currentTurretServoPosition);
        }

        follower = Constants.createFollower(hardwareMap);
        follower.setStartingPose(startingPose == null ? new Pose() : startingPose);
        follower.update();
        telemetryM = PanelsTelemetry.INSTANCE.getTelemetry();
    }

    @Override
    public void start() {
        follower.startTeleopDrive();

    }

    public void stop() {
        robotServos.colorservo.setPosition(0);
    }

    @Override
    public void loop() {
        follower.update();
        telemetryM.update();

        // Update Limelight data
        limelightManager.update();

        // Intake control For player 1 with left bumper and player 2 right stick
        if (gamepad1.left_bumper && !leftBumped) {
            if (isintakeActive){
               robotMotors.stopIntake();
                isintakeActive = false;
            }
            else{
                robotMotors.startIntake();
                isintakeActive = true;
           }
        }
        leftBumped = gamepad1.left_bumper;


        // Shooter control with right bumper
        if (gamepad1.right_bumper && !rightBumped) {
            if (isshooterActive){
                robotMotors.stopShooter();
                isshooterActive = false;
            }
            else{
                robotMotors.startShooter(currentShooterVelocity); // Use current velocity mode
                isshooterActive = true;
            }
        }
        rightBumped = gamepad1.right_bumper;

        // Toggle between high and low shooter velocity with Y/Triangle button
        // Support both PlayStation (triangle) and Xbox (y) button styles
        boolean velocityTogglePressed = gamepad1.y || gamepad1.triangle;

        if (velocityTogglePressed && !prevYPressed) {
            isHighVelocityMode = !isHighVelocityMode;

            if (isHighVelocityMode) {
                currentShooterVelocity = targetShooterhighVelocity;
                telemetry.addLine("🚀 Switched to HIGH velocity (long distance)");
            } else {
                currentShooterVelocity = targetShooterlowVelocity;
                telemetry.addLine("🎯 Switched to LOW velocity (short distance)");
            }

            // If shooter is already active, update its velocity
            if (isshooterActive) {
                robotMotors.startShooter(currentShooterVelocity);
                telemetry.addLine("✅ Shooter velocity updated");
            }
        }
        prevYPressed = velocityTogglePressed;

        // Drive control
        if (!automatedDrive) {
            if (!slowMode) follower.setTeleOpDrive(
                    -gamepad1.left_stick_y,
                    -gamepad1.left_stick_x,
                    -gamepad1.right_stick_x,
                    true // Robot Centric
            );
            else follower.setTeleOpDrive(
                    -gamepad1.left_stick_y * slowModeMultiplier,
                    -gamepad1.left_stick_x * slowModeMultiplier,
                    -gamepad1.right_stick_x * slowModeMultiplier,
                    true // Robot Centric
            );
        }

        // ========== AUTO TURRET CONTROL ==========
        // Toggle auto-tracking with gamepad2 Cross (X) / Circle (O)
        if (gamepad2.cross || gamepad2.a) {
            autoTrackEnabled = true;
        }
        if (gamepad2.circle || gamepad2.b) {
            autoTrackEnabled = false;
        }

        // Switch between tag 20 and 24 with gamepad2 Triangle/Y
        if (gamepad2.triangle || gamepad2.y) {
            currentTargetTagID = (currentTargetTagID == TurretConstants.PRIMARY_TAG_ID)
                ? TurretConstants.SECONDARY_TAG_ID
                : TurretConstants.PRIMARY_TAG_ID;
        }

        double turretPower = 0.0;

        if (autoTrackEnabled) {
            // AUTO-TRACKING MODE
            DecodeUtils.TurretTrackingResult result = DecodeUtils.autoTrackTurret(
                limelightManager,
                currentTargetTagID,
                turretPidState
            );

            turretPower = result.motorPower;

            // Add telemetry for auto-tracking
            telemetry.addData("🎯 Auto-Track", "ENABLED");
            telemetry.addData("Target Tag", currentTargetTagID);
            telemetry.addData("Tag Visible", result.isTracking ? "YES ✓" : "NO ✗");
            if (result.isTracking) {
                telemetry.addData("Status", result.isLockedOn ? "LOCKED ON ✓" : "TRACKING...");
                telemetry.addData("Error", "%.2f°", result.error);
            }

        } else {
            // ========== MANUAL TURRET CONTROL MODE (Gamepad 2) ==========

            // Continuous control with right stick Y
            double stickControl = -gamepad2.right_stick_y * 0.6; // Inverted, 60% max speed

            // Fine adjustment with D-Pad
            double dpadControl = 0.0;
            if (gamepad2.dpad_up) {
                dpadControl = 0.2; // Fine control positive
            } else if (gamepad2.dpad_down) {
                dpadControl = -0.2; // Fine control negative
            }

            // Left stick X for alternative control
            double leftStickControl = gamepad2.left_stick_x * 0.4; // 40% max speed

            // Combine controls (priority: D-Pad > Right Stick > Left Stick)
            if (Math.abs(dpadControl) > 0.01) {
                turretPower = dpadControl;
            } else if (Math.abs(stickControl) > 0.05) {
                turretPower = stickControl;
            } else {
                turretPower = leftStickControl;
            }

            turretPidState.reset(); // Reset PID when not tracking

            telemetry.addData("🎯 Auto-Track", "MANUAL MODE");
            telemetry.addData("Control Source",
                Math.abs(dpadControl) > 0.01 ? "D-Pad" :
                Math.abs(stickControl) > 0.05 ? "Right Stick" :
                Math.abs(leftStickControl) > 0.05 ? "Left Stick" : "Idle");
            telemetry.addData("Turret Power", "%.2f", turretPower);
        }

        // Enforce rotation limits for safety
        turretPower = DecodeUtils.enforceTurretLimits(
            robotMotors.turret.getCurrentPosition(),
            turretPower
        );

        // Apply power to turret motor
        robotMotors.turret.setPower(turretPower);

        // Show turret position
        telemetry.addData("Turret Position",
            DecodeUtils.getTurretPositionStatus(robotMotors.turret.getCurrentPosition()));

        // Warning if near limits
        if (DecodeUtils.isTurretNearLimits(robotMotors.turret.getCurrentPosition(), 20.0)) {
            telemetry.addLine("⚠️ WARNING: Turret near rotation limit!");
        }
        if(!intakeMode) {
            robotServos.ballpush_4.setPosition(robotServos.ballpush_initial_position + (gamepad1.right_trigger * 0.27));
        }
        // Support both PlayStation (cross) and Xbox (a) button styles
        boolean modeSwitchPressed = gamepad1.a || gamepad1.cross;

        if (modeSwitchPressed && !prevAPressed) {
            if (!intakeMode) {
                // Switching from LAUNCH to INTAKE mode
                // ALWAYS go to index 0 (first position) in INTAKE mode
                currentTurretPositionIndex = 0;
                currentTurretServoPosition = INTAKE_POSITIONS[0]; // 0.07

                robotServos.turret_1.setPosition(currentTurretServoPosition);
                robotServos.colorservo.setPosition(1);
                intakeMode = true;

                // ALWAYS reset to "Black" so auto-advance is ready immediately
                // This allows it to detect the FIRST ball even if sensor currently sees color
                previousColor = "Black";
                colorTransitionDetected = false;

                telemetry.addLine("✅ Switched to INTAKE mode - Reset to Index 0");
                telemetry.addData("Ready for auto-advance", "YES");
            } else {
                // Switching from INTAKE to LAUNCH mode
                // Find closest LAUNCH position
                currentTurretPositionIndex = findClosestPositionIndex(currentTurretServoPosition, LAUNCH_POSITIONS);
                currentTurretServoPosition = LAUNCH_POSITIONS[currentTurretPositionIndex];

                robotServos.turret_1.setPosition(currentTurretServoPosition);
                robotServos.colorservo.setPosition(0.38);
                intakeMode = false;

                // Reset color sensor state
                previousColor = "Black";
                colorTransitionDetected = false;

                telemetry.addLine("✅ Switched to LAUNCH mode");
            }
        }
        prevAPressed = modeSwitchPressed;

        // ========== HOOD SERVO CONTROL ==========
        // Gamepad 1: D-Pad Up/Down (primary control)
        // Gamepad 2: D-Pad Left/Right (secondary control for operator)

        boolean hoodIncrementPressed = gamepad1.dpad_up || gamepad2.dpad_right;
        boolean hoodDecrementPressed = gamepad1.dpad_down || gamepad2.dpad_left;

        // Increment hood position with D-Pad Up (GP1) or D-Pad Right (GP2)
        if (hoodIncrementPressed && !prevDpadUpPressed) {
            hood_servoPosition += HOOD_INCREMENT;
            if (hood_servoPosition > HoodConstants.HOOD_MAX_POSITION) {
                hood_servoPosition = HoodConstants.HOOD_MAX_POSITION;
            }
            if (robotServos.hood_5 != null) {
                robotServos.hood_5.setPosition(hood_servoPosition);
            } else {
                telemetry.addData("ERROR", "Servo 'hood_5' not found. Check hardwareMap name and configuration.");
            }
        }
        prevDpadUpPressed = hoodIncrementPressed;

        // Decrement hood position with D-Pad Down (GP1) or D-Pad Left (GP2)
        if (hoodDecrementPressed && !prevDpadDownPressed) {
            hood_servoPosition -= HOOD_INCREMENT;
            if (hood_servoPosition < HoodConstants.HOOD_MIN_POSITION) {
                hood_servoPosition = HoodConstants.HOOD_MIN_POSITION;
            }
            if (robotServos.hood_5 != null) {
                robotServos.hood_5.setPosition(hood_servoPosition);
            } else {
                telemetry.addData("ERROR", "Servo 'hood_5' not found. Check hardwareMap name and configuration.");
            }
        }
        prevDpadDownPressed = hoodDecrementPressed;


        // ========== TURRET SERVO CONTROL (gamepad1 D-Pad Left/Right) ==========
        // Cycle through defined positions based on current mode
        if (!intakeMode) {
            // LAUNCH MODE - cycle through LAUNCH_POSITIONS
            if (gamepad1.dpad_left && !prevDpadLeftPressed) {
                // Move to previous position (decrease index)
                currentTurretPositionIndex--;
                if (currentTurretPositionIndex < 0) {
                    currentTurretPositionIndex = 0; // Stay at first position
                }
                currentTurretServoPosition = LAUNCH_POSITIONS[currentTurretPositionIndex];

                if (robotServos.turret_1 != null) {
                    robotServos.turret_1.setPosition(currentTurretServoPosition);
                } else {
                    telemetry.addData("ERROR", "Servo 'turret_1' not found. Check hardwareMap name and configuration.");
                }
            }
            prevDpadLeftPressed = gamepad1.dpad_left;

            if (gamepad1.dpad_right && !prevDpadRightPressed) {
                // Move to next position (increase index)
                currentTurretPositionIndex++;
                if (currentTurretPositionIndex >= LAUNCH_POSITIONS.length) {
                    currentTurretPositionIndex = LAUNCH_POSITIONS.length - 1; // Stay at last position
                }
                currentTurretServoPosition = LAUNCH_POSITIONS[currentTurretPositionIndex];

                if (robotServos.turret_1 != null) {
                    robotServos.turret_1.setPosition(currentTurretServoPosition);
                } else {
                    telemetry.addData("ERROR", "Servo 'turret_1' not found. Check hardwareMap name and configuration.");
                }
            }
            prevDpadRightPressed = gamepad1.dpad_right;

        } else {
            // INTAKE MODE - cycle through INTAKE_POSITIONS
            if (gamepad1.dpad_left && !prevDpadLeftPressed) {
                // Move to previous position (decrease index)
                currentTurretPositionIndex--;
                if (currentTurretPositionIndex < 0) {
                    currentTurretPositionIndex = 0; // Stay at first position
                }
                currentTurretServoPosition = INTAKE_POSITIONS[currentTurretPositionIndex];

                if (robotServos.turret_1 != null) {
                    robotServos.turret_1.setPosition(currentTurretServoPosition);
                } else {
                    telemetry.addData("ERROR", "Servo 'turret_1' not found. Check hardwareMap name and configuration.");
                }
            }
            prevDpadLeftPressed = gamepad1.dpad_left;

            if (gamepad1.dpad_right && !prevDpadRightPressed) {
                // Move to next position (increase index)
                currentTurretPositionIndex++;
                if (currentTurretPositionIndex >= INTAKE_POSITIONS.length) {
                    currentTurretPositionIndex = INTAKE_POSITIONS.length - 1; // Stay at last position
                }
                currentTurretServoPosition = INTAKE_POSITIONS[currentTurretPositionIndex];

                if (robotServos.turret_1 != null) {
                    robotServos.turret_1.setPosition(currentTurretServoPosition);
                } else {
                    telemetry.addData("ERROR", "Servo 'turret_1' not found. Check hardwareMap name and configuration.");
                }
            }
            prevDpadRightPressed = gamepad1.dpad_right;
        }

        // ========== AUTO-ADVANCE IN INTAKE MODE (Color Sensor) ==========
        if (intakeMode) {
            String currentColor = robotSensors.getColorName(robotSensors.color_f);

            telemetry.addLine("═══ COLOR SENSOR STATE ═══");
            telemetry.addData("📸 Current Sensor Reading", currentColor);
            telemetry.addData("📋 Previous Color Stored", previousColor);
            telemetry.addData("🔒 Transition Locked", colorTransitionDetected ? "YES" : "NO");

            // Helper method to check if color represents "no ball" state
            // Sensor can return "Black" or "None" when no ball is present
            boolean isNoBall = currentColor.equalsIgnoreCase("Black") || currentColor.equalsIgnoreCase("None");
            boolean wasPreviousNoBall = previousColor.equalsIgnoreCase("Black") || previousColor.equalsIgnoreCase("None");

            // Debug: Show transition detection logic
            boolean isPreviousBlack = wasPreviousNoBall;
            boolean isCurrentNotBlack = !isNoBall;
            boolean isNotLocked = !colorTransitionDetected;
            boolean willTrigger = isPreviousBlack && isCurrentNotBlack && isNotLocked;

            telemetry.addLine("🔍 TRANSITION CHECK:");
            telemetry.addData("  Previous was Black?", isPreviousBlack ? "YES ✓" : "NO ✗ (was: " + previousColor + ")");
            telemetry.addData("  Current is NOT Black?", isCurrentNotBlack ? "YES ✓ (is: " + currentColor + ")" : "NO ✗ (is: " + currentColor + ")");
            telemetry.addData("  NOT Locked?", isNotLocked ? "YES ✓" : "NO ✗ (LOCKED)");
            telemetry.addData("  → Will Trigger?", willTrigger ? "YES! 🚀" : "NO");

            // Detect color change from Black to any other color
            if (willTrigger) {
                // Non-black color detected - advance to next position
                colorTransitionDetected = true; // Lock to prevent multiple advances

                telemetry.addLine("⚡ TRIGGERING AUTO-ADVANCE!");
                telemetry.addData("From Index", currentTurretPositionIndex);

                if (currentTurretPositionIndex < INTAKE_POSITIONS.length - 1) {
                    // Can advance to next position
                    int oldIndex = currentTurretPositionIndex;
                    currentTurretPositionIndex++;
                    currentTurretServoPosition = INTAKE_POSITIONS[currentTurretPositionIndex];

                    if (robotServos.turret_1 != null) {
                        robotServos.turret_1.setPosition(currentTurretServoPosition);
                        telemetry.addLine("🎨 AUTO-ADVANCE SUCCESS!");
                        telemetry.addData("Color Detected", currentColor);
                        telemetry.addData("Moved", "%d → %d", oldIndex, currentTurretPositionIndex);
                        telemetry.addData("Position", "%.2f → %.2f", INTAKE_POSITIONS[oldIndex], currentTurretServoPosition);
                    }
                } else {
                    telemetry.addLine("⚠️ Already at last position - cannot auto-advance");
                    telemetry.addData("Current Index", currentTurretPositionIndex);
                }
            }

            // Update previousColor based on current sensor reading
            telemetry.addLine("📝 UPDATING PREVIOUS COLOR:");
            telemetry.addData("  Sensor reads", currentColor);

            // Check if sensor shows "no ball" state (Black or None)
            boolean currentIsNoBall = currentColor.equalsIgnoreCase("Black") || currentColor.equalsIgnoreCase("None");

            if (currentIsNoBall) {
                // Color returned to "no ball" state - unlock and reset
                if (colorTransitionDetected) {
                    telemetry.addLine("  🔓 UNLOCKING - No ball detected (sensor: " + currentColor + ")");
                }
                colorTransitionDetected = false;
                previousColor = "Black"; // Always use "Black" as the standard "no ball" reference
                telemetry.addData("  → Setting previousColor to", "Black");
            } else {
                // Ball detected - remember the color
                previousColor = currentColor;
                telemetry.addData("  → Setting previousColor to", currentColor);
            }

            telemetry.addData("✅ Final previousColor", previousColor);

        } else {
            // Reset when not in intake mode
            previousColor = "Black";
            colorTransitionDetected = false;
        }


        // Color sensor detection with comprehensive info (FTC best practices)
        telemetry.addData("Color BR", robotSensors.getColorName(robotSensors.color_br));
        telemetry.addData("BR Info", robotSensors.getSensorInfo(robotSensors.color_br));

        telemetry.addData("Color BL", robotSensors.getColorName(robotSensors.color_bl));
        telemetry.addData("BL Info", robotSensors.getSensorInfo(robotSensors.color_bl));

        telemetry.addData("Color F", robotSensors.getColorName(robotSensors.color_f));
        telemetry.addData("F Info", robotSensors.getSensorInfo(robotSensors.color_f));

        telemetry.addData("Turret Servo Position", currentTurretServoPosition);
        telemetry.addData("Turret Position Index", currentTurretPositionIndex);
        telemetry.addData("Hood Servo Position", hood_servoPosition);
        telemetry.addData("Mode", intakeMode ? "INTAKE" : "LAUNCH");

        // Show auto-advance debug info when in INTAKE mode
        if (intakeMode) {
            telemetry.addLine("--- AUTO-ADVANCE DEBUG ---");
            telemetry.addData("Current Color", robotSensors.getColorName(robotSensors.color_f));
            telemetry.addData("Previous Color", previousColor);
            telemetry.addData("Transition Locked", colorTransitionDetected ? "YES (waiting for black)" : "NO (ready)");
            telemetry.addData("Can Advance", currentTurretPositionIndex < INTAKE_POSITIONS.length - 1 ? "YES" : "NO (at last position)");
        }

        // Show available positions for both modes
        telemetry.addLine("--- TURRET POSITIONS ---");
        telemetry.addData("INTAKE Positions",
            String.format("[0]=%.2f, [1]=%.2f, [2]=%.2f",
                INTAKE_POSITIONS[0], INTAKE_POSITIONS[1], INTAKE_POSITIONS[2]));
        telemetry.addData("LAUNCH Positions",
            String.format("[0]=%.2f, [1]=%.2f, [2]=%.2f",
                LAUNCH_POSITIONS[0], LAUNCH_POSITIONS[1], LAUNCH_POSITIONS[2]));

        // Shooter velocity information
        telemetry.addLine("--- SHOOTER INFO ---");
        telemetry.addData("Velocity Mode", isHighVelocityMode ? "HIGH (Long Distance)" : "LOW (Short Distance)");
        telemetry.addData("Target Velocity", currentShooterVelocity);
        telemetry.addData("Current Velocity", robotMotors.getShooterVelocity());
        telemetry.addData("Shooter Active", isshooterActive ? "YES ✓" : "NO");

        telemetry.update();
    }

    /**
     * Helper method to find the closest position index in an array to a given value
     * @param currentPosition The current servo position
     * @param positions Array of defined positions to search
     * @return Index of the closest position in the array
     */
    private int findClosestPositionIndex(double currentPosition, double[] positions) {
        int closestIndex = 0;
        double minDistance = Math.abs(currentPosition - positions[0]);

        for (int i = 1; i < positions.length; i++) {
            double distance = Math.abs(currentPosition - positions[i]);
            if (distance < minDistance) {
                minDistance = distance;
                closestIndex = i;
            }
        }

        return closestIndex;
    }
}
