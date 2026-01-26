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
    private double targetShooterVelocity = 1450; // Target velocity for shooter

    public double lowVelocity = 1100;

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
                robotMotors.startShooter(targetShooterVelocity);
                isshooterActive = true;
            }
        }
        rightBumped = gamepad1.right_bumper;

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
        // Toggle auto-tracking with gamepad2 Cross (X)
        if (gamepad2.cross) {
            autoTrackEnabled = true;
        }
        if (gamepad2.circle) {
            autoTrackEnabled = false;
        }

        // Switch between tag 20 and 24 with gamepad2 Triangle
        if (gamepad2.triangle) {
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
            // MANUAL CONTROL MODE - Use gamepad2 right stick
            turretPower = gamepad2.right_stick_y;
            turretPidState.reset(); // Reset PID when not tracking

            telemetry.addData("🎯 Auto-Track", "Manual");
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
        if (gamepad1.a && !prevAPressed) {
            if (!intakeMode) {
                // Switching from LAUNCH to INTAKE mode
                currentTurretPositionIndex = findClosestPositionIndex(currentTurretServoPosition, INTAKE_POSITIONS);
                currentTurretServoPosition = INTAKE_POSITIONS[currentTurretPositionIndex];

                robotServos.turret_1.setPosition(currentTurretServoPosition);
                robotServos.colorservo.setPosition(1);
                intakeMode = true;
            } else {
                // Switching from INTAKE to LAUNCH mode
                currentTurretPositionIndex = findClosestPositionIndex(currentTurretServoPosition, LAUNCH_POSITIONS);
                currentTurretServoPosition = LAUNCH_POSITIONS[currentTurretPositionIndex];

                robotServos.turret_1.setPosition(currentTurretServoPosition);
                robotServos.colorservo.setPosition(0.38);
                intakeMode = false;
            }
        }
        prevAPressed = gamepad1.a;

        // ========== HOOD SERVO CONTROL (gamepad1 D-Pad Up/Down) ==========
        // Increment hood position with D-Pad Up
        if (gamepad1.dpad_up && !prevDpadUpPressed) {
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
        prevDpadUpPressed = gamepad1.dpad_up;

        // Decrement hood position with D-Pad Down
        if (gamepad1.dpad_down && !prevDpadDownPressed) {
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
        prevDpadDownPressed = gamepad1.dpad_down;


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

        // Show available positions for both modes
        telemetry.addLine("--- TURRET POSITIONS ---");
        telemetry.addData("INTAKE Positions",
            String.format("[0]=%.2f, [1]=%.2f, [2]=%.2f",
                INTAKE_POSITIONS[0], INTAKE_POSITIONS[1], INTAKE_POSITIONS[2]));
        telemetry.addData("LAUNCH Positions",
            String.format("[0]=%.2f, [1]=%.2f, [2]=%.2f",
                LAUNCH_POSITIONS[0], LAUNCH_POSITIONS[1], LAUNCH_POSITIONS[2]));

        telemetry.addData("Shooter Target Velocity", targetShooterVelocity);
        telemetry.addData("Shooter Current Velocity", robotMotors.getShooterVelocity());

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
