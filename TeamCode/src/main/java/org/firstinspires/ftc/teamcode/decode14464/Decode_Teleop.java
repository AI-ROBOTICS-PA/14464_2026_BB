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
import org.firstinspires.ftc.teamcode.decode14464.systems.RobotMotors;
import org.firstinspires.ftc.teamcode.decode14464.systems.RobotServos;
import org.firstinspires.ftc.teamcode.decode14464.systems.RobotSensors;
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
    private boolean autoTrackEnabled = false; // Default to MANUAL mode - Player 2 enables auto-tracking with GP2 A/Cross
    private int currentTargetTagID = TurretConstants.PRIMARY_TAG_ID; // Start with tag 20
    private boolean prevDpadUpPressed  = false;
    private boolean prevDpadDownPressed  = false;
    private boolean prevDpadLeftPressed  = false;  // For gamepad1 turret control
    private boolean prevDpadRightPressed  = false; // For gamepad1 turret control
    private boolean prevDpadLeftPressed2  = false; // For gamepad2 hood fine control
    private boolean prevDpadRightPressed2  = false; // For gamepad2 hood fine control
    private boolean prevAPressed  = false;

    // Hood servo control
    private final double HOOD_INCREMENT = 0.02; // Amount to increment/decrement for fine control
    private double hood_servoPosition = HoodConstants.HOME_POSITION; // Initial servo position

    // Hood preset positions for gamepad1 toggle
    private static final double HOOD_LONG_RANGE = 0.5;  // Long distance shooting
    private static final double HOOD_SHORT_RANGE = 0.8; // Short distance shooting
    private boolean isHoodLongRange = true; // Track current preset mode

    private final double BI_INCREMENT = 0.1; // Amount to increment/decrement
    private double BI_servoPosition = 0.67; // Initial servo position
    private final double BI_MIN_POSITION = 0.37;
    private final double BI_MAX_POSITION = 0.67;

    private boolean intakeMode = false;

    // Define discrete positions for each mode
    private final double[] INTAKE_POSITIONS = {0.07, 0.44, 0.81};
    private final double[] LAUNCH_POSITIONS = {0.165, 0.535, 0.905};

    // Current position index in the respective array
    private int currentTurretPositionIndex = 0;

    // Current actual servo position
    private double currentTurretServoPosition = 0.165; // Start in launch mode at first position

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

    // Position Lock feature (X/Square button)
    private boolean positionLockEnabled = false;
    private Pose lockedPose = null;
    private boolean prevXPressed = false;

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

    @Override
    public void stop() {
        // Stop all motors immediately
        if (robotMotors.turret != null) {
            robotMotors.turret.setPower(0);
        }
        if (robotMotors.intake != null) {
            robotMotors.stopIntake();
        }
        if (robotMotors.flywheelMotor != null) {
            robotMotors.stopShooter();
        }

        // STEP 1: Move all servos to safe neutral positions FIRST
        if (robotServos.colorservo != null) {
            robotServos.colorservo.setPosition(0.5); // Neutral position
        }
        if (robotServos.turret_1 != null) {
            robotServos.turret_1.setPosition(0.5); // Mid-range neutral position
        }
        if (robotServos.hood_5 != null) {
            robotServos.hood_5.setPosition(HoodConstants.HOME_POSITION); // Safe home position
        }
        if (robotServos.ballpush_4 != null) {
            robotServos.ballpush_4.setPosition(robotServos.ballpush_initial_position);
        }

        // STEP 2: Small delay to let servos reach position before disabling PWM
        try {
            Thread.sleep(100); // 100ms for servos to settle
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // STEP 3: Disable PWM on ALL servos to prevent flickering
        // This completely cuts power and prevents electrical interference
        if (robotServos.turret_1 != null) {
            robotServos.turret_1.getController().pwmDisable();
        }
        if (robotServos.hood_5 != null) {
            robotServos.hood_5.getController().pwmDisable();
        }
        if (robotServos.colorservo != null) {
            robotServos.colorservo.getController().pwmDisable();
        }
        if (robotServos.ballpush_4 != null) {
            robotServos.ballpush_4.getController().pwmDisable();
        }

        // Stop follower
        if (follower != null) {
            follower.breakFollowing();
        }
    }

    @Override
    public void loop() {
        follower.update();
        telemetryM.update();

        // Update Limelight data
        limelightManager.update();

        // ========== POSITION LOCK (Gamepad2 Right Bumper Hold) ==========
        // Lock position only while right bumper is held
        boolean wasLocked = positionLockEnabled;
        positionLockEnabled = gamepad2.right_bumper;

        if (positionLockEnabled && !wasLocked) {
            // ENABLE: Just started locking - capture position and break following
            lockedPose = follower.getPose();
            follower.breakFollowing();
        } else if (!positionLockEnabled && wasLocked) {
            // DISABLE: Just released lock - resume normal teleop drive
            follower.startTeleopDrive();
            lockedPose = null;
        }

        // Update position lock if currently enabled
        if (positionLockEnabled && lockedPose != null) {
            // Continuously command robot to hold position
            follower.holdPoint(
                new com.pedropathing.geometry.BezierPoint(lockedPose),
                Math.toRadians(5) // 5 degree heading tolerance
            );
        }

        // Intake control For player 1 with left bumper (toggle on/off)
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

        // Gamepad 2: Variable intake power control with left stick Y
        // Allows fine control and outtake (reverse) capability
        double intakePowerFromGamepad2 = -gamepad2.left_stick_y; // Invert: Up = intake, Down = outtake

        if (Math.abs(intakePowerFromGamepad2) > 0.05) { // Deadzone to prevent drift
            // Manual control takes over - set the power directly
            robotMotors.setIntakePower(intakePowerFromGamepad2);
            // Update toggle state based on direction
            isintakeActive = intakePowerFromGamepad2 > 0;
        } else if (!isintakeActive) {
            // Only stop if toggle is off AND stick is neutral
            robotMotors.setIntakePower(0);
        }
        // If isintakeActive is true and stick is neutral, keep running at toggle speed


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

        // Drive control - disable when position lock is active
        if (!automatedDrive && !positionLockEnabled) {
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

        } else {
            // ========== MANUAL TURRET CONTROL MODE (Gamepad 2) ==========

            // Continuous control with right stick Y (primary)
            double stickControl = -gamepad2.right_stick_y * 0.6; // Inverted, 60% max speed

            // Fine adjustment with D-Pad Left/Right (secondary)
            double dpadControl = 0.0;
            if (gamepad2.dpad_right) {
                dpadControl = 0.2; // Fine control positive
            } else if (gamepad2.dpad_left) {
                dpadControl = -0.2; // Fine control negative
            }

            // Combine controls (priority: D-Pad > Right Stick)
            // Note: Left stick X removed to avoid interference with left stick Y (intake control)
            if (Math.abs(dpadControl) > 0.01) {
                turretPower = dpadControl;
            } else if (Math.abs(stickControl) > 0.05) {
                turretPower = stickControl;
            } else {
                turretPower = 0.0; // No input
            }

            turretPidState.reset(); // Reset PID when not tracking
        }

        // Enforce rotation limits ONLY in auto-tracking mode
        // In manual mode, operator has full control without encoder limits
        if (autoTrackEnabled) {
            turretPower = DecodeUtils.enforceTurretLimits(
                robotMotors.turret.getCurrentPosition(),
                turretPower
            );
        }

        // Apply power to turret motor
        robotMotors.turret.setPower(turretPower);

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
        // Gamepad 1: D-Pad Up/Down - Toggle between preset positions (0.5 long range, 0.8 short range)
        // Gamepad 2: D-Pad Left/Right - Fine control for custom adjustments

        // GAMEPAD 1: PRESET TOGGLE (UP = Long Range, DOWN = Short Range)
        if (gamepad1.dpad_up && !prevDpadUpPressed) {
            // Set to LONG RANGE preset
            isHoodLongRange = true;
            hood_servoPosition = HOOD_LONG_RANGE;
            if (robotServos.hood_5 != null) {
                robotServos.hood_5.setPosition(hood_servoPosition);
                telemetry.addLine("🎯 Hood: LONG RANGE (0.5)");
            }
        }
        prevDpadUpPressed = gamepad1.dpad_up;

        if (gamepad1.dpad_down && !prevDpadDownPressed) {
            // Set to SHORT RANGE preset
            isHoodLongRange = false;
            hood_servoPosition = HOOD_SHORT_RANGE;
            if (robotServos.hood_5 != null) {
                robotServos.hood_5.setPosition(hood_servoPosition);
                telemetry.addLine("🎯 Hood: SHORT RANGE (0.8)");
            }
        }
        prevDpadDownPressed = gamepad1.dpad_down;

        // GAMEPAD 2: FINE CONTROL (UP/DOWN for custom positions)
        if (gamepad2.dpad_up && !prevDpadRightPressed2) {
            // Increment hood position (increase angle)
            hood_servoPosition += HOOD_INCREMENT;
            if (hood_servoPosition > HoodConstants.HOOD_MAX_POSITION) {
                hood_servoPosition = HoodConstants.HOOD_MAX_POSITION;
            }
            if (robotServos.hood_5 != null) {
                robotServos.hood_5.setPosition(hood_servoPosition);
            }
            // Mark as custom (not preset)
            isHoodLongRange = false; // Could use a third state "custom" if needed
        }
        prevDpadRightPressed2 = gamepad2.dpad_up;

        if (gamepad2.dpad_down && !prevDpadLeftPressed2) {
            // Decrement hood position (decrease angle)
            hood_servoPosition -= HOOD_INCREMENT;
            if (hood_servoPosition < HoodConstants.HOOD_MIN_POSITION) {
                hood_servoPosition = HoodConstants.HOOD_MIN_POSITION;
            }
            if (robotServos.hood_5 != null) {
                robotServos.hood_5.setPosition(hood_servoPosition);
            }
            // Mark as custom (not preset)
            isHoodLongRange = false;
        }
        prevDpadLeftPressed2 = gamepad2.dpad_down;


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

            // Helper method to check if color represents "no ball" state
            // Sensor can return "Black" or "None" when no ball is present
            boolean isNoBall = currentColor.equalsIgnoreCase("Black") || currentColor.equalsIgnoreCase("None");
            boolean wasPreviousNoBall = previousColor.equalsIgnoreCase("Black") || previousColor.equalsIgnoreCase("None");

            // Debug: Show transition detection logic
            boolean isPreviousBlack = wasPreviousNoBall;
            boolean isCurrentNotBlack = !isNoBall;
            boolean isNotLocked = !colorTransitionDetected;
            boolean willTrigger = isPreviousBlack && isCurrentNotBlack && isNotLocked;

            // Detect color change from Black to any other color
            if (willTrigger) {
                // Non-black color detected - advance to next position
                colorTransitionDetected = true; // Lock to prevent multiple advances

                if (currentTurretPositionIndex < INTAKE_POSITIONS.length - 1) {
                    // Can advance to next position
                    int oldIndex = currentTurretPositionIndex;
                    currentTurretPositionIndex++;
                    currentTurretServoPosition = INTAKE_POSITIONS[currentTurretPositionIndex];

                    if (robotServos.turret_1 != null) {
                        robotServos.turret_1.setPosition(currentTurretServoPosition);
                    }
                }
            }

            // Update previousColor based on current sensor reading
            // Check if sensor shows "no ball" state (Black or None)
            boolean currentIsNoBall = currentColor.equalsIgnoreCase("Black") || currentColor.equalsIgnoreCase("None");

            if (currentIsNoBall) {
                // Color returned to "no ball" state - unlock and reset
                colorTransitionDetected = false;
                previousColor = "Black"; // Always use "Black" as the standard "no ball" reference
            } else {
                // Ball detected - remember the color
                previousColor = currentColor;
            }

        } else {
            // Reset when not in intake mode
            previousColor = "Black";
            colorTransitionDetected = false;
        }

        // ========== TELEMETRY (Essential Info Only) ==========

        // Color sensor detection (commented out for screen space - uncomment for debugging)
        /*
        telemetry.addData("Color BR", robotSensors.getColorName(robotSensors.color_br));
        telemetry.addData("BR Info", robotSensors.getSensorInfo(robotSensors.color_br));
        telemetry.addData("Color BL", robotSensors.getColorName(robotSensors.color_bl));
        telemetry.addData("BL Info", robotSensors.getSensorInfo(robotSensors.color_bl));
        telemetry.addData("Color F", robotSensors.getColorName(robotSensors.color_f));
        telemetry.addData("F Info", robotSensors.getSensorInfo(robotSensors.color_f));
        */

        // === SYSTEM STATUS ===
        telemetry.addLine("━━━ ROBOT STATUS ━━━");
        telemetry.addData("Mode", intakeMode ? "🔵 INTAKE" : "🔴 LAUNCH");

        // Position Lock status
        telemetry.addData("Position Lock", positionLockEnabled ? "🔒 LOCKED" : "🔓 FREE");
        if (positionLockEnabled && lockedPose != null) {
            telemetry.addData("Locked at", "X:%.1f Y:%.1f H:%.0f°",
                lockedPose.getX(), lockedPose.getY(), Math.toDegrees(lockedPose.getHeading()));
        }

        telemetry.addData("Turret Index", currentTurretPositionIndex);
        telemetry.addData("Turret Pos", "%.2f", currentTurretServoPosition);

        // Auto-advance status (only in intake mode)
        if (intakeMode) {
            String ballColor = robotSensors.getColorName(robotSensors.color_f);
            telemetry.addData("Ball Detected", !ballColor.equalsIgnoreCase("Black") && !ballColor.equalsIgnoreCase("None") ? "✓ " + ballColor : "✗ Empty");
            telemetry.addData("Auto-Advance", colorTransitionDetected ? "🔒 Locked" : "✓ Ready");
        }

        // Auto-Turret Motor tracking status
        telemetry.addLine("━━━ AUTO TURRET ━━━");
        telemetry.addData("Mode", autoTrackEnabled ? "🤖 AUTO" : "🎮 MANUAL");
        if (autoTrackEnabled) {
            telemetry.addData("Target Tag", currentTargetTagID);
            telemetry.addData("Tracking", limelightManager.isAprilTagVisible(currentTargetTagID) ? "✓ LOCKED" : "✗ SEARCHING");
        } else {
            telemetry.addData("Control", "GP2 Sticks");
        }

        // Hood control information
        telemetry.addLine("━━━ HOOD ━━━");
        String hoodMode = "CUSTOM";
        if (Math.abs(hood_servoPosition - HOOD_LONG_RANGE) < 0.01) {
            hoodMode = "🎯 LONG";
        } else if (Math.abs(hood_servoPosition - HOOD_SHORT_RANGE) < 0.01) {
            hoodMode = "🎯 SHORT";
        }
        telemetry.addData("Hood Mode", hoodMode);
        telemetry.addData("Hood Pos", "%.2f", hood_servoPosition);

        // Shooter velocity information
        telemetry.addLine("━━━ SHOOTER ━━━");
        telemetry.addData("Shooter", isshooterActive ? "✓ ACTIVE" : "✗ OFF");
        telemetry.addData("Mode", isHighVelocityMode ? "🚀 HIGH" : "🎯 LOW");
        telemetry.addData("Target", (int)currentShooterVelocity);
        telemetry.addData("Current", "%.0f", robotMotors.getShooterVelocity());

        // Intake control information
        telemetry.addLine("━━━ INTAKE ━━━");
        telemetry.addData("Intake", isintakeActive ? "✓ ACTIVE" : "✗ OFF");
        double currentIntakePower = -gamepad2.left_stick_y;
        if (Math.abs(currentIntakePower) > 0.05) {
            telemetry.addData("Control", currentIntakePower > 0 ? "GP2 IN" : "GP2 OUT");
            telemetry.addData("Power", "%.2f", currentIntakePower);
        } else if (isintakeActive) {
            telemetry.addData("Control", "GP1 Toggle");
        }

        // Commented out for screen space - uncomment for debugging turret positions
        /*
        telemetry.addLine("--- TURRET POSITIONS ---");
        telemetry.addData("INTAKE Positions",
            String.format("[0]=%.2f, [1]=%.2f, [2]=%.2f",
                INTAKE_POSITIONS[0], INTAKE_POSITIONS[1], INTAKE_POSITIONS[2]));
        telemetry.addData("LAUNCH Positions",
            String.format("[0]=%.2f, [1]=%.2f, [2]=%.2f",
                LAUNCH_POSITIONS[0], LAUNCH_POSITIONS[1], LAUNCH_POSITIONS[2]));
        */

        // Commented out for screen space - uncomment for debugging auto-advance
        /*
        if (intakeMode) {
            telemetry.addLine("--- AUTO-ADVANCE DEBUG ---");
            telemetry.addData("Current Color", robotSensors.getColorName(robotSensors.color_f));
            telemetry.addData("Previous Color", previousColor);
            telemetry.addData("Transition Locked", colorTransitionDetected ? "YES (waiting for black)" : "NO (ready)");
            telemetry.addData("Can Advance", currentTurretPositionIndex < INTAKE_POSITIONS.length - 1 ? "YES" : "NO (at last position)");
        }
        */

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
