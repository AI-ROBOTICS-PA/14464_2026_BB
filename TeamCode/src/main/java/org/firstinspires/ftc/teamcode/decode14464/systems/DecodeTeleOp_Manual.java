package org.firstinspires.ftc.teamcode.decode14464.systems;

import com.pedropathing.geometry.Pose;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

/**
 * DECODE TeleOp - Main Competition TeleOp
 * Focus on Auto-Intake and Auto-Shooting with Manual Override
 *
 * Controls:
 * ========
 * GAMEPAD 1 (Driver):
 * - A/Cross: Toggle Intake/Launch Mode (works with both PS4 and Xbox controllers)
 * - Right Bumper: Auto-Shoot (shoots all balls in turret)
 * - D-Pad Left: Manual turret retreat (previous slot) - OVERRIDE
 * - D-Pad Right: Manual turret advance (next slot) - OVERRIDE
 * - D-Pad Up: Hood preset LONGER range (flatter angle)
 * - D-Pad Down: Hood preset SHORTER range (steeper angle)
 * - Left Trigger: Toggle HIGH/LOW flywheel velocity
 * - Right Trigger: Manual ball push (launch individual ball) - OVERRIDE
 * - Left Stick: Drive (X/Y movement)
 * - Right Stick X: Rotation
 *
 * GAMEPAD 2 (Operator):
 * - A/Cross: Toggle Alliance BEFORE START only | During match: FULL LIMELIGHT RESET (position + clear offset)
 * - X/Square: Toggle Auto-Turret tracking ON/OFF
 * - Y/Triangle: Toggle Auto-Calibration ON/OFF
 * - Left Stick Y: Manual intake control (Up = Intake, Down = Outtake)
 * - Right Stick X: Turret manual override (variable speed)
 * - D-Pad Left/Right: Turret manual override (fine control)
 * - D-Pad Up/Down: Hood fine control (+/- 0.02)
 * - Left Bumper: Manual RECALIBRATE (locks current position as correct reference)
 * - Right Bumper: LIMELIGHT CALIBRATE (uses AprilTag for precision calibration)
 * - Left Trigger (HOLD): POSITION LOCK - Resists being pushed by other robots
 *
 * Auto-Turret Behavior:
 * =====================
 * - Auto-tracking is ALWAYS running (tracks goal automatically)
 * - GP2 controls provide MANUAL OVERRIDE when you need to adjust
 * - When you've manually aimed correctly, press LEFT BUMPER to RECALIBRATE
 * - Recalibrate tells the system "this is the correct position" and fixes drift
 *
 * Limelight Calibration:
 * ======================
 * - AUTOMATIC: Calibrates continuously when AprilTag visible and robot stationary
 * - GP2 RIGHT BUMPER: Calibrates turret angle (tx) + robot position (keeps manual offset)
 * - GP2 A/Cross: FULL RESET — corrects position AND clears manual offset (trust auto-tracking)
 * - Uses AprilTag detection to fix any odometry drift or turret offset
 *
 * Hood Presets:
 * =============
 * - LONG (0.30): Flattest angle for long-distance shots
 * - MEDIUM (0.55): Mid-range shots
 * - SHORT (0.80): Steepest angle for close shots (default)
 *
 * Flywheel Velocity:
 * ==================
 * - HIGH (1450): Long-distance shots (default)
 * - LOW (1100): Short-distance/controlled shots
 *
 * Features:
 * =========
 * - Auto-Intake: Automatically advances turret when ball detected
 * - Auto-Shoot: Automatically shoots all loaded balls
 * - Manual Override: D-Pad controls work even when auto is active
 * - Manual Launch: Right trigger for individual ball launching
 * - Hood Presets: Quick adjustment for different shot distances
 * - Velocity Toggle: Switch between high/low power modes
 * - Supports both PS4 (Cross/Circle) and Xbox (A/B) controllers
 *
 * Fallback Mechanism:
 * ===================
 * If auto-intake malfunctions, driver can manually control turret:
 * - Use D-Pad Left/Right to manually advance/retreat turret positions
 * - Use Right Trigger to manually push balls (instead of auto-shoot)
 * - Manual controls work in both INTAKE and LAUNCH modes
 * - System respects current mode (uses INTAKE or LAUNCH positions)
 */
@TeleOp(name = "DECODE TeleOp Manual (BLUE)", group = "Manual")
public class DecodeTeleOp_Manual extends OpMode {

    protected RobotSystem robot;

    /**
     * Get the default alliance for this TeleOp program.
     * Override in subclass to change default (e.g., DecodeTeleOp_Red returns RED).
     * User can still toggle with GP2 A before pressing START.
     */
    protected LimelightManager.Alliance getDefaultAlliance() {
        return LimelightManager.Alliance.BLUE;
    }

    // Button state tracking for edge detection
    private boolean prevA = false;
    private boolean prevRightBumper = false;
    private boolean prevDpadLeft = false;
    private boolean prevDpadRight = false;
    private boolean prevDpadUp = false;
    private boolean prevDpadDown = false;
    private boolean manualPushActive = false; // Track manual trigger push-release cycle

    // ========== SHOOTER VELOCITY SETTINGS ==========
    private static final double SHOOTER_HIGH_VELOCITY = 1450;   // Long distance (trigger > 50%)
    private static final double SHOOTER_MID_VELOCITY = 1160;    // Mid-point shooting (trigger < 50%)
    private static final double SHOOTER_LOW_VELOCITY = 1100;    // Short distance (default, no trigger)
    private double currentShooterVelocity = SHOOTER_LOW_VELOCITY; // Start at LOW
    private String velocityModeName = "LOW"; // For telemetry

    // ========== HOOD PRESET POSITIONS ==========
    // 3 preset positions: LONG (flat), MEDIUM, SHORT (angled up)
    private static final double[] HOOD_POSITIONS = {0.30, 0.55, 0.80};
    private static final String[] HOOD_POSITION_NAMES = {"LONG", "MEDIUM", "SHORT"};
    private int currentHoodIndex = 2; // Start at SHORT (0.80) - ready for close shots

    // ========== HOOD FINE CONTROL (Gamepad 2) ==========
    private static final double HOOD_INCREMENT = 0.02; // Fine adjustment step
    private double currentHoodPosition = 0.80; // Actual servo position (starts at SHORT)
    private boolean isHoodCustomPosition = false; // Track if using custom vs preset
    private boolean prevGP2DpadUp = false;
    private boolean prevGP2DpadDown = false;

    // ========== TURRET MOTOR CONTROL (Gamepad 2) ==========
    private static final double TURRET_MANUAL_POWER = 0.3; // Manual control power level
    private boolean turretManualOverride = false; // True when GP2 is actively controlling turret
    private boolean autoTurretEnabled = false; // Manual-only mode: keep auto turret disabled
    private boolean prevGP2X = false; // For auto-turret toggle button
    private boolean prevGP2LeftBumper = false; // For recalibrate button detection
    private boolean prevGP2RightBumper = false; // For Limelight calibration button

    // ========== TELEOP MODE ==========
    // Standalone TeleOp: uses default alliance-based starting position + goal tracking
    // Post-Auto TeleOp: preserves position from Auto + goal tracking
    private boolean isStandaloneTeleOp = false;

    // ========== LIMELIGHT AUTO-CALIBRATION ==========
    private static final double AUTO_CALIBRATE_COOLDOWN_MS = 2000; // Minimum time between auto-calibrations
    private static final double AUTO_CALIBRATE_TX_THRESHOLD = 5.0; // Max tx error (degrees) for auto-calibrate
    private static final double AUTO_CALIBRATE_VELOCITY_THRESHOLD = 0.3; // Max robot velocity for auto-calibrate
    private double lastAutoCalibrationTime = 0;
    private boolean autoCalibrationEnabled = true; // Can be toggled with GP2 Y button
    private String lastCalibrationStatus = "None"; // For telemetry
    private boolean prevGP2Y = false; // For auto-calibration toggle button
    private boolean prevGP2A = false; // For alliance toggle button
    private boolean prevGP1B = false; // For manual ball advance (sensor broken fallback)
    private boolean prevGP1Y = false; // For hard turret reset button

    // ========== RUMBLE FEEDBACK ==========
    private boolean wasReadyToShoot = false; // Track state change for rumble
    private boolean wasTurretAtLimit = false; // Track turret limit for warning rumble

    // ========== POSITION LOCK (GP2 Left Trigger HOLD) ==========
    // Locks robot position to resist being pushed by other robots during shooting
    private boolean positionLockEnabled = false;
    private com.pedropathing.geometry.Pose lockedPose = null;
    private com.pedropathing.follower.Follower follower = null; // Cached follower reference

    // ========== TELEMETRY PAGES ==========
    private int telemetryPage = 0; // 0=Essential, 1=Details, 2=Controls
    private static final int TELEMETRY_PAGE_COUNT = 3;
    private boolean prevGP2Back = false;

    @Override
    public void init() {
        // Initialize robot - preserves position from Auto
        robot = new RobotSystem();
        robot.initTeleOp(hardwareMap);

        // Set default alliance based on which program was selected
        if (robot.limelight != null) {
            robot.limelight.setAlliance(getDefaultAlliance());
        }
        if (robot.autoTurret != null) {
            if (getDefaultAlliance() == LimelightManager.Alliance.RED) {
                robot.autoTurret.setRedAlliance();
            } else {
                robot.autoTurret.setBlueAlliance();
            }
            robot.autoTurret.disableCombinedGoalTracking();
        }

        // Initialize hood servo to starting position
        currentHoodPosition = HOOD_POSITIONS[currentHoodIndex];
        if (robot.servos != null && robot.servos.hood_5 != null) {
            robot.servos.hood_5.setPosition(currentHoodPosition);
        }

        String defaultAllianceStr = getDefaultAlliance() == LimelightManager.Alliance.RED ? "RED" : "BLUE";
        telemetry.addLine("✅ DECODE TeleOp Initialized (Default: " + defaultAllianceStr + ")");
        telemetry.addLine();
        
        // Detect standalone vs post-Auto
        isStandaloneTeleOp = !RobotMotors.hasSavedTurretPosition();
        
        if (!isStandaloneTeleOp) {
            // Auto ran first → position preserved from Auto
            double savedAngle = RobotMotors.getSavedTurretPosition() / RobotConstants.Turret.TICKS_PER_DEGREE;
            telemetry.addLine("✅ TURRET POSITION PRESERVED from Auto!");
            telemetry.addData("   Turret Angle", "%.1f° (continuing from Auto)", savedAngle);
            telemetry.addLine("   Turret Mode: GOAL TRACKING (position from Auto)");
        } else {
            // Standalone TeleOp → use default starting position based on alliance
            telemetry.addLine("ℹ️ Standalone TeleOp (no Auto ran)");
            telemetry.addLine("   Make sure turret is physically centered!");
            telemetry.addLine();
            telemetry.addLine(">>> GP2 A/Cross: Toggle BLUE/RED Alliance <<<");
            telemetry.addLine("   (sets starting position + goal target)");
        }
        telemetry.addLine();
        telemetry.addData("Current Mode", "LAUNCH (ready to shoot)");
        telemetry.addData("Hood", HOOD_POSITION_NAMES[currentHoodIndex]);
        telemetry.addData("Velocity", velocityModeName);
        telemetry.update();
    }

    @Override
    public void init_loop() {
        // Allow alliance selection BEFORE pressing START
        // GP2 A/Cross toggles alliance (same button as in loop)
        boolean gp2APressed = gamepad2.a || gamepad2.cross;
        if (gp2APressed && !prevGP2A && robot.limelight != null) {
            robot.limelight.toggleAlliance();
            // Sync turret goal with new alliance
            if (robot.autoTurret != null) {
                if (robot.limelight.getAlliance() == LimelightManager.Alliance.RED) {
                    robot.autoTurret.setRedAlliance();
                } else {
                    robot.autoTurret.setBlueAlliance();
                }
            }
        }
        prevGP2A = gp2APressed;

        // Show current alliance and starting position
        boolean isRed = robot.limelight != null && robot.limelight.getAlliance() == LimelightManager.Alliance.RED;
        telemetry.addLine("✅ DECODE TeleOp - WAITING FOR START");
        telemetry.addLine();

        if (isRed) {
            telemetry.addLine("🔴 RED ALLIANCE");
            telemetry.addData("   Start Position", "(%.0f, %.0f) heading %.0f°",
                RobotConstants.Field.RED_TELEOP_START_X, RobotConstants.Field.RED_TELEOP_START_Y,
                RobotConstants.Field.RED_TELEOP_START_HEADING);
            telemetry.addData("   Goal", "(%.0f, %.0f)", 
                RobotConstants.Field.RED_GOAL_X, RobotConstants.Field.RED_GOAL_Y);
        } else {
            telemetry.addLine("🔵 BLUE ALLIANCE");
            telemetry.addData("   Start Position", "(%.0f, %.0f) heading %.0f°",
                RobotConstants.Field.BLUE_TELEOP_START_X, RobotConstants.Field.BLUE_TELEOP_START_Y,
                RobotConstants.Field.BLUE_TELEOP_START_HEADING);
            telemetry.addData("   Goal", "(%.0f, %.0f)", 
                RobotConstants.Field.BLUE_GOAL_X, RobotConstants.Field.BLUE_GOAL_Y);
        }

        telemetry.addLine();
        telemetry.addLine(">>> GP2 A/Cross: Toggle Alliance <<<");
        telemetry.addLine(">>> Press START when ready <<<");
        telemetry.addLine();
        telemetry.addData("Hood", HOOD_POSITION_NAMES[currentHoodIndex]);
        telemetry.addData("Velocity", velocityModeName);
        telemetry.update();
    }

    @Override
    public void start() {
        // ========== SET STARTING POSITION FOR STANDALONE TELEOP ==========
        boolean isRed = robot.limelight != null && robot.limelight.getAlliance() == LimelightManager.Alliance.RED;
        
        if (isStandaloneTeleOp && robot.localization != null) {
            // Set default starting position based on selected alliance
            double startX, startY, startHeadingDeg;
            if (isRed) {
                startX = RobotConstants.Field.RED_TELEOP_START_X;
                startY = RobotConstants.Field.RED_TELEOP_START_Y;
                startHeadingDeg = RobotConstants.Field.RED_TELEOP_START_HEADING;
            } else {
                startX = RobotConstants.Field.BLUE_TELEOP_START_X;
                startY = RobotConstants.Field.BLUE_TELEOP_START_Y;
                startHeadingDeg = RobotConstants.Field.BLUE_TELEOP_START_HEADING;
            }
            robot.localization.setPose(new Pose(startX, startY, Math.toRadians(startHeadingDeg)));
        }

        // Cache follower reference once (avoid repeated getFollower() calls)
        if (robot.localization != null) {
            follower = robot.localization.getFollower();
        }
        
        // Start TeleOp driving
        if (follower != null) {
            follower.startTeleopDrive();
        }

        // Start shooter motor at current velocity mode
        if (robot.shooting != null) {
            robot.shooting.startShooter(currentShooterVelocity);
        }

        // Intake motor only starts if in intake mode (default is launch mode, so don't start)
        // Intake will start when user switches to intake mode with A/Cross button

        // Set turret goal based on current alliance
        if (robot.autoTurret != null) {
            if (isRed) {
                robot.autoTurret.setRedAlliance();
            } else {
                robot.autoTurret.setBlueAlliance();
            }
        }

        // CRITICAL: Update localization BEFORE enabling goal tracking.
        // initTeleOp() creates a fresh follower that starts at default pose (0, 0, 0°).
        // Without this update, recalibratePosition() uses the WRONG heading (0° instead
        // of the actual IMU heading like 180°). Then when the first loop() updates the
        // heading from IMU, the offset is mismatched and the turret swings to center.
        // This update reads the IMU so heading is correct before calibration.
        if (robot.localization != null) {
            robot.localization.update();
        }

        // Manual-only turret mode: keep goal tracking disabled.
        if (robot.autoTurret != null) {
            robot.autoTurret.disableCombinedGoalTracking();
        }

        String allianceStr = isRed ? "RED" : "BLUE";
        telemetry.addLine("🚀 TeleOp Started!");
        telemetry.addLine("✅ Alliance: " + allianceStr);
        telemetry.addLine("✅ Shooter Running at " + currentShooterVelocity + " (" + velocityModeName + ")");
        telemetry.addLine("✅ Hood at " + HOOD_POSITION_NAMES[currentHoodIndex]);
        telemetry.addLine("✅ Turret: MANUAL CONTROL (GP2 stick/D-pad)");
        if (RobotMotors.hasSavedTurretOffset()) {
            telemetry.addLine("✅ Turret Offset: " + String.format("%.1f°", RobotMotors.getSavedInitialOffsetDegrees()) + " (from Auto)");
        }
        telemetry.addLine("ℹ️ Intake Motor: OFF (switch to INTAKE mode to enable)");
        telemetry.addLine("ℹ️ GP1 Y = HARD TURRET RESET (center turret first!)");
        telemetry.update();
    }

    @Override
    public void loop() {
        // ========== DETECT GP2 MANUAL TURRET INPUT FIRST ==========
        double turretDpadInput = 0;
        if (gamepad2.dpad_left) {
            turretDpadInput = TURRET_MANUAL_POWER;
        } else if (gamepad2.dpad_right) {
            turretDpadInput = -TURRET_MANUAL_POWER;
        }
        double turretStickInput = -gamepad2.right_stick_x * 0.5;
        boolean hasManualInput = Math.abs(turretDpadInput) > 0.01 || Math.abs(turretStickInput) > 0.1;
        
        // ========== UPDATE ALL SYSTEMS ==========
        // Manual-only turret mode: always skip auto-turret update.
        robot.updateAllExceptTurret();

        // ========== POSITION LOCK (GP2 Left Trigger HOLD) ==========
        // Hold Left Trigger to lock position - resists being pushed by other robots
        // Uses cached follower reference (same approach as working Decode_Teleop)
        if (follower != null) {
            boolean wasLocked = positionLockEnabled;
            positionLockEnabled = gamepad2.left_trigger > 0.5;
            
            if (positionLockEnabled && !wasLocked) {
                // ENABLE: capture position and break following
                lockedPose = follower.getPose();
                follower.breakFollowing();
                gamepad2.rumble(100);
            } else if (!positionLockEnabled && wasLocked) {
                // DISABLE: resume normal teleop drive
                follower.startTeleopDrive();
                lockedPose = null;
            }
            
            if (positionLockEnabled && lockedPose != null) {
                follower.holdPoint(
                    new com.pedropathing.geometry.BezierPoint(lockedPose),
                    Math.toRadians(5)
                );
            }
        }

        // ========== DRIVE CONTROL ==========
        // Disable normal drive when position lock is active
        if (!positionLockEnabled && follower != null) {
            follower.setTeleOpDrive(
                -gamepad1.left_stick_y,
                -gamepad1.left_stick_x,
                -gamepad1.right_stick_x,
                true // Field-centric control
            );
        }

        // ========== MODE SWITCHING ==========
        // A/Cross: Toggle between Intake and Launch modes
        // Support both controller types: PS4 (cross) and Xbox (a)
        // Manual mode: always allow switching modes.
        boolean aPressed = gamepad1.a || gamepad1.cross;
        if (aPressed && !prevA) {
            robot.autoIntake.toggleMode();

            // Sync LED color and intake motor with mode
            if (robot.autoIntake.isIntakeMode()) {
                robot.led.setIntakeModeColor();
                robot.autoIntake.startIntakeMotor(); // Start intake motor in intake mode
            } else {
                robot.led.setLaunchModeColor();
                robot.autoIntake.stopIntakeMotor(); // Stop intake motor in launch mode
            }
        }
        prevA = aPressed;

        // ========== GAMEPAD 2: MANUAL INTAKE CONTROL (Left Stick Y) ==========
        // Player 2 can manually control intake motor with left stick
        // Up = Intake (positive power), Down = Outtake (negative power)
        double intakePowerFromGP2 = -gamepad2.left_stick_y; // Invert: Up = intake, Down = outtake
        
        if (Math.abs(intakePowerFromGP2) > 0.1) { // Deadzone to prevent drift
            // Manual override - set intake power directly based on stick position
            robot.motors.setIntakePower(intakePowerFromGP2);
        } else {
            // No manual input - revert to automatic behavior based on mode
            if (robot.autoIntake.isIntakeMode()) {
                // In intake mode, run intake at full speed
                robot.motors.setIntakePower(1.0);
            } else {
                // In launch mode, stop intake
                robot.motors.setIntakePower(0);
            }
        }

        // ========== GP1 B/Circle: MANUAL BALL ADVANCE (Sensor Broken Fallback) ==========
        // When color sensor is broken, driver uses this to manually tell the system
        // "a ball was loaded". Increments ballsLoaded + advances servo.
        // Hold GP1 Left Bumper + B to UNDO (retreat + decrement).
        // Only works in INTAKE mode.
        boolean gp1BPressed = gamepad1.b || gamepad1.circle;
        if (gp1BPressed && !prevGP1B && robot.autoIntake.isIntakeMode()) {
            if (gamepad1.left_bumper) {
                // UNDO: retreat intake (decrement ball count)
                if (robot.autoIntake.manualRetreatIntake()) {
                    gamepad1.rumble(100);
                    telemetry.log().add("⬅️ Manual Undo: Balls=" + robot.autoIntake.getBallsLoaded());
                }
            } else {
                // ADVANCE: manual ball loaded (increment ball count)
                if (robot.autoIntake.manualAdvanceIntake()) {
                    gamepad1.rumble(200);
                    telemetry.log().add("➡️ Manual Ball: Balls=" + robot.autoIntake.getBallsLoaded());
                }
            }
        }
        prevGP1B = gp1BPressed;

        // ========== MANUAL TURRET OVERRIDE (D-Pad Left/Right) ==========
        // D-Pad Left: Move to previous slot (servo reposition only, no ball count change)
        if (gamepad1.dpad_left && !prevDpadLeft) {
            robot.autoIntake.moveToPreviousSlot();
            telemetry.addLine("⬅️ Manual: Previous Slot");
        }
        prevDpadLeft = gamepad1.dpad_left;

        // D-Pad Right: Move to next slot (manual advance)
        if (gamepad1.dpad_right && !prevDpadRight) {
            robot.autoIntake.advanceToNextSlot();
            telemetry.addLine("➡️ Manual: Next Slot");
        }
        prevDpadRight = gamepad1.dpad_right;

        // ========== GP1: HOOD PRESET CONTROL (D-Pad Up/Down) ==========
        // D-Pad Up: Move to longer range (lower index = flatter hood)
        if (gamepad1.dpad_up && !prevDpadUp) {
            if (currentHoodIndex > 0) {
                currentHoodIndex--;
            }
            currentHoodPosition = HOOD_POSITIONS[currentHoodIndex];
            isHoodCustomPosition = false; // Using preset
            if (robot.servos != null && robot.servos.hood_5 != null) {
                robot.servos.hood_5.setPosition(currentHoodPosition);
            }
            telemetry.addLine("🎯 Hood: " + HOOD_POSITION_NAMES[currentHoodIndex] + " (" + currentHoodPosition + ")");
        }
        prevDpadUp = gamepad1.dpad_up;

        // D-Pad Down: Move to shorter range (higher index = more angled hood)
        if (gamepad1.dpad_down && !prevDpadDown) {
            if (currentHoodIndex < HOOD_POSITIONS.length - 1) {
                currentHoodIndex++;
            }
            currentHoodPosition = HOOD_POSITIONS[currentHoodIndex];
            isHoodCustomPosition = false; // Using preset
            if (robot.servos != null && robot.servos.hood_5 != null) {
                robot.servos.hood_5.setPosition(currentHoodPosition);
            }
            telemetry.addLine("🎯 Hood: " + HOOD_POSITION_NAMES[currentHoodIndex] + " (" + currentHoodPosition + ")");
        }
        prevDpadDown = gamepad1.dpad_down;

        // ========== GP2: HOOD FINE CONTROL (D-Pad Up/Down) ==========
        // D-Pad Up: Increment hood position (increase angle)
        if (gamepad2.dpad_up && !prevGP2DpadUp) {
            currentHoodPosition += HOOD_INCREMENT;
            if (currentHoodPosition > RobotConstants.Hood.MAX_POSITION) {
                currentHoodPosition = RobotConstants.Hood.MAX_POSITION;
            }
            isHoodCustomPosition = true; // Mark as custom position
            if (robot.servos != null && robot.servos.hood_5 != null) {
                robot.servos.hood_5.setPosition(currentHoodPosition);
            }
            telemetry.addLine("🔧 Hood Fine: " + String.format("%.2f", currentHoodPosition));
        }
        prevGP2DpadUp = gamepad2.dpad_up;

        // D-Pad Down: Decrement hood position (decrease angle)
        if (gamepad2.dpad_down && !prevGP2DpadDown) {
            currentHoodPosition -= HOOD_INCREMENT;
            if (currentHoodPosition < RobotConstants.Hood.MIN_POSITION) {
                currentHoodPosition = RobotConstants.Hood.MIN_POSITION;
            }
            isHoodCustomPosition = true; // Mark as custom position
            if (robot.servos != null && robot.servos.hood_5 != null) {
                robot.servos.hood_5.setPosition(currentHoodPosition);
            }
            telemetry.addLine("🔧 Hood Fine: " + String.format("%.2f", currentHoodPosition));
        }
        prevGP2DpadDown = gamepad2.dpad_down;

        // ========== GP2: TURRET MANUAL CONTROL (D-Pad Left/Right + Right Stick X) ==========
        double manualPower = Math.abs(turretDpadInput) > 0.01 ? turretDpadInput : turretStickInput;
        manualPower = enforceTurretLimitsForManual(manualPower);
        if (robot.motors != null && robot.motors.turret != null) {
            robot.motors.turret.setPower(manualPower);
        }

        // Manual mode: auto turret calibration/reset controls are disabled.

        // ========== GP1 Y/TRIANGLE: HARD TURRET RESET ==========
        // User physically moves turret to center, then presses Y.
        // Resets encoder to 0, clears ALL offsets. Encoder 0 = physical center.
        boolean gp1YPressed = gamepad1.y || gamepad1.triangle;
        if (gp1YPressed && !prevGP1Y && robot.autoTurret != null) {
            robot.autoTurret.hardReset();
            // Clear saved offset so it doesn't get restored
            RobotMotors.clearSavedTurretOffset();
            gamepad1.rumble(500);
            gamepad2.rumble(500);
            telemetry.log().add("🔄 TURRET HARD RESET — Encoder=0, Offset=0");
        }
        prevGP1Y = gp1YPressed;

        // ========== EMERGENCY STOP (Back/Share + Start/Options) ==========
        // Press Back+Start (Xbox) or Share+Options (PS4) together to immediately stop all motors
        boolean gp1Emergency = (gamepad1.back || gamepad1.share) && (gamepad1.start || gamepad1.options);
        boolean gp2Emergency = (gamepad2.back || gamepad2.share) && (gamepad2.start || gamepad2.options);
        if (gp1Emergency || gp2Emergency) {
            robot.stopAll();
            gamepad1.rumble(500);
            gamepad2.rumble(500);
            telemetry.addLine("⚠️ EMERGENCY STOP ACTIVATED!");
        }

        // ========== SHOOTER VELOCITY (Left Trigger = 3 levels) ==========
        // No trigger: LOW (1100) | Partial press: MID (1160) | Near full press (>70%): HIGH (1450)
        // Momentary - releases back to LOW when trigger released
        double leftTriggerValue = gamepad1.left_trigger;
        double targetVelocity;
        String targetModeName;
        if (leftTriggerValue > 0.7) {
            targetVelocity = SHOOTER_HIGH_VELOCITY;  // 1450
            targetModeName = "HIGH";
        } else if (leftTriggerValue > 0.1) {
            targetVelocity = SHOOTER_MID_VELOCITY;   // 1160
            targetModeName = "MID";
        } else {
            targetVelocity = SHOOTER_LOW_VELOCITY;   // 1100
            targetModeName = "LOW";
        }
        
        // Only update if velocity changed (avoid spamming motor commands)
        if (targetVelocity != currentShooterVelocity) {
            currentShooterVelocity = targetVelocity;
            velocityModeName = targetModeName;
            robot.shooting.startShooter(currentShooterVelocity);
        }

        // ========== MANUAL BALL LAUNCH (Right Trigger) ==========
        // Right Trigger: Manual ball push servo control (only in launch mode)
        // Tracks push-release cycle to decrement ballsLoaded (squeeze past 0.7, release below 0.3 = 1 ball shot)
        if (robot.servos != null && robot.servos.ballpush_4 != null) {
            if (!robot.autoIntake.isIntakeMode()) {
                // Map trigger (0.0 to 1.0) to servo offset (0.0 to 0.27)
                double triggerValue = gamepad1.right_trigger;
                double servoOffset = triggerValue * 0.27;

                // Set ball push servo position
                // Initial position + offset moves servo forward to push ball
                robot.servos.ballpush_4.setPosition(
                    robot.servos.ballpush_initial_position + servoOffset
                );

                // Track push-release cycle to decrement ball count
                if (triggerValue > 0.7) {
                    // Trigger is fully squeezed - mark push as active
                    manualPushActive = true;
                } else if (manualPushActive && triggerValue < 0.3) {
                    // Trigger was squeezed and now released - one ball was shot
                    manualPushActive = false;
                    int currentBalls = robot.autoIntake.getBallsLoaded();
                    if (currentBalls > 0) {
                        robot.autoIntake.setBallsLoaded(currentBalls - 1);
                        gamepad1.rumble(100);
                        telemetry.log().add("🔫 Manual Shot! Balls=" + robot.autoIntake.getBallsLoaded());
                    }
                }
            } else {
                // In intake mode, keep ball push servo at initial position
                robot.servos.ballpush_4.setPosition(robot.servos.ballpush_initial_position);
                manualPushActive = false; // Reset tracking when not in launch mode
            }
        }

        // ========== READY TO SHOOT RUMBLE ==========
        // Rumble when system becomes ready to shoot (turret on target + shooter at speed)
        boolean isReadyToShoot = robot.shooting != null
            && robot.shooting.isShooterAtTargetVelocity()
            && (robot.autoIntake == null || !robot.autoIntake.isIntakeMode());
        if (isReadyToShoot && !wasReadyToShoot) {
            // Just became ready - double pulse rumble to signal "ready!"
            gamepad1.rumbleBlips(2);
            gamepad2.rumbleBlips(1);
        }
        wasReadyToShoot = isReadyToShoot;
        
        // Auto-turret limit warnings are disabled in manual-only turret mode.

        // ========== TELEMETRY ==========
        updateTelemetry();
    }

    /**
     * Perform Limelight-based calibration
     * Calibrates:
     * - Robot POSITION only (X, Y) - heading is NOT changed (odometry heading is reliable)
     * - Turret pointing (using tx horizontal angle)
     * @param isManual true if triggered by button press, false if auto-triggered
     */
    private void performLimelightCalibration(boolean isManual) {
        // Check if Limelight is available and has valid target
        if (robot.limelight == null || !robot.limelight.isConnected()) {
            if (isManual) {
                lastCalibrationStatus = "FAILED: No Limelight";
            }
            return;
        }
        
        // Only calibrate if our ALLIANCE TAG is visible (not just any tag)
        if (!robot.limelight.hasAllianceTarget()) {
            if (isManual) {
                int targetTag = robot.limelight.getTargetTagId();
                lastCalibrationStatus = "FAILED: Tag " + targetTag + " not visible";
            }
            return;
        }
        
        double currentTime = System.currentTimeMillis();
        
        // Get Limelight data
        double tx = robot.limelight.getTargetX(); // Horizontal angle to target
        Pose limelightPose = robot.limelight.getRobotPoseFromAprilTag();
        
        // === STEP 1: POSITION CORRECTION (must happen FIRST) ===
        // Correct robot X, Y position from Limelight. Heading is NOT changed.
        // Odometry heading (from IMU) is reliable; only X/Y can drift over time.
        if (limelightPose != null) {
            robot.localization.calibratePositionOnly(limelightPose);
        }
        
        // === STEP 2: TURRET CALIBRATION (uses corrected position) ===
        // Convert tx into a persistent manualOffset so it survives auto-tracking updates.
        // Must happen AFTER position correction so the offset is calculated correctly.
        if (Math.abs(tx) > 0.5) { // Only calibrate if there's meaningful offset
            robot.autoTurret.calibrateFromLimelight(tx);
        }
        
        // Update status and provide rumble feedback
        if (isManual) {
            lastCalibrationStatus = String.format("Manual OK (tx=%.1f°)", tx);
            telemetry.addLine("✅ LIMELIGHT CALIBRATION COMPLETE!");
            telemetry.addLine(String.format("   Turret offset: %.1f°", tx));
            if (limelightPose != null) {
                telemetry.addLine(String.format("   Position corrected: (%.1f, %.1f)", limelightPose.getX(), limelightPose.getY()));
            }
            // Rumble feedback for successful manual calibration
            gamepad2.rumble(300);
            // Reset auto-calibration cooldown so it doesn't immediately overwrite manual calibration
            lastAutoCalibrationTime = currentTime;
        } else {
            lastAutoCalibrationTime = currentTime;
            lastCalibrationStatus = String.format("Auto OK (tx=%.1f°)", tx);
            // Light rumble for auto-calibration (so driver knows it happened)
            gamepad2.rumble(50);
        }
    }

    /**
     * Automatic calibration - triggers when conditions are optimal
     */
    private void performAutoCalibration() {
        // Check cooldown
        double currentTime = System.currentTimeMillis();
        if (currentTime - lastAutoCalibrationTime < AUTO_CALIBRATE_COOLDOWN_MS) {
            return; // Still in cooldown
        }
        
        // Check if Limelight has our alliance's tag visible (not just any tag)
        if (robot.limelight == null || !robot.limelight.hasAllianceTarget()) {
            return;
        }
        
        // Check if robot is relatively stationary (not moving fast)
        // Use gamepad inputs as proxy for movement
        double driveInput = Math.abs(gamepad1.left_stick_x) + Math.abs(gamepad1.left_stick_y) + Math.abs(gamepad1.right_stick_x);
        if (driveInput > AUTO_CALIBRATE_VELOCITY_THRESHOLD) {
            return; // Robot is moving too fast
        }
        
        // Check if turret has settled (not still moving from previous calibration)
        // Calibrating while turret is in motion gives bad tx readings
        if (robot.autoTurret != null && !robot.autoTurret.isOnTarget()) {
            return; // Turret still moving - wait until it settles
        }
        
        // Check tx error - only auto-calibrate if we're close but not perfect
        double tx = robot.limelight.getTargetX();
        if (Math.abs(tx) > AUTO_CALIBRATE_TX_THRESHOLD) {
            return; // Error too large for auto-calibrate (might be wrong target)
        }
        
        // All conditions met - perform calibration
        performLimelightCalibration(false); // false = auto trigger
    }

    /**
     * Enforce turret rotation limits for manual control
     * Prevents turret from rotating beyond safe PHYSICAL limits (±135° from center)
     * Accounts for initial offset (encoder 0 may not be physical 0)
     * Includes soft limit zone that slows down near limits
     * @param requestedPower Requested motor power (-1.0 to 1.0)
     * @return Power with limits enforced (may be reduced or zero)
     */
    private double enforceTurretLimitsForManual(double requestedPower) {
        if (robot.motors == null || robot.motors.turret == null) {
            return 0.0; // No turret motor available
        }
        
        // Use PHYSICAL angle for limit checks (accounts for initial offset)
        double physicalDegrees = (robot.autoTurret != null) 
            ? robot.autoTurret.getPhysicalAngleDegrees()
            : robot.motors.turret.getCurrentPosition() / RobotConstants.Turret.TICKS_PER_DEGREE;
        double maxRotation = RobotConstants.Turret.MAX_ROTATION_DEGREES;
        double softLimitDegrees = RobotConstants.Turret.SOFT_LIMIT_DEGREES;
        
        // Hard limit check - stop if at max physical limit and trying to go further positive
        if (physicalDegrees >= maxRotation && requestedPower > 0) {
            return 0.0;
        }
        
        // Hard limit check - stop if at min physical limit and trying to go further negative
        if (physicalDegrees <= -maxRotation && requestedPower < 0) {
            return 0.0;
        }
        
        // Soft limit - slow down near max physical limit
        if (physicalDegrees > (maxRotation - softLimitDegrees) && requestedPower > 0) {
            double reduction = (maxRotation - physicalDegrees) / softLimitDegrees;
            requestedPower *= Math.max(0.1, reduction);
        }
        
        // Soft limit - slow down near min physical limit
        if (physicalDegrees < (-maxRotation + softLimitDegrees) && requestedPower < 0) {
            double reduction = (physicalDegrees - (-maxRotation)) / softLimitDegrees;
            requestedPower *= Math.max(0.1, reduction);
        }
        
        return requestedPower;
    }

    private void updateTelemetry() {
        // ========== PAGE SWITCHING (GP2 Back/Share Button) ==========
        boolean gp2BackPressed = gamepad2.back || gamepad2.share; // Support both Xbox and PS4
        boolean gp2StartPressed = gamepad2.start || gamepad2.options;
        if (gp2BackPressed && !prevGP2Back && !gp2StartPressed) { // Avoid conflict with emergency stop
            telemetryPage = (telemetryPage + 1) % TELEMETRY_PAGE_COUNT;
            gamepad2.rumble(50);
        }
        prevGP2Back = gp2BackPressed;

        // ========== HEADER (Always shown) ==========
        String[] pageNames = {"ESSENTIAL", "DETAILS", "CONTROLS"};
        telemetry.addLine("═══ PAGE " + (telemetryPage + 1) + "/" + TELEMETRY_PAGE_COUNT + ": " + pageNames[telemetryPage] + " ═══");
        telemetry.addLine("(GP2 Back = Next Page)");
        telemetry.addLine();

        // ========== ALWAYS SHOW: Mode + Ready Status ==========
        String modeText = robot.autoIntake.isIntakeMode() ? "🔵 INTAKE" : "🔴 LAUNCH";
        String readyText = robot.isReadyToShoot() ? "✓ READY" : "⏳ NOT READY";
        telemetry.addData("Mode", modeText + " | " + readyText);

        // ========== PAGE CONTENT ==========
        switch (telemetryPage) {
            case 0: // ESSENTIAL - Most important info during match
                displayEssentialPage();
                break;
            case 1: // DETAILS - Detailed system info
                displayDetailsPage();
                break;
            case 2: // CONTROLS - Quick reference
                displayControlsPage();
                break;
        }

        telemetry.update();
    }

    /**
     * Page 0: Essential info - what drivers need during match
     */
    private void displayEssentialPage() {
        // ========== DEBUG: GP2 INPUT STATUS ==========
        telemetry.addLine("─── DEBUG GP2 ───");
        telemetry.addData("GP2 DpadL/R", gamepad2.dpad_left + " / " + gamepad2.dpad_right);
        telemetry.addData("GP2 RStickX", String.format("%.2f", gamepad2.right_stick_x));
        telemetry.addData("GP2 Square/X", (gamepad2.square || gamepad2.x) + "");
        telemetry.addData("GP2 Triangle/Y", (gamepad2.triangle || gamepad2.y) + "");
        telemetry.addData("GP2 LBumper", gamepad2.left_bumper + "");
        
        // Turret motor status
        if (robot.motors != null && robot.motors.turret != null) {
            telemetry.addData("Turret Encoder", robot.motors.turret.getCurrentPosition());
            telemetry.addData("Turret Power", String.format("%.2f", robot.motors.turret.getPower()));
            telemetry.addData("Turret Mode", robot.motors.turret.getMode().toString());
        } else {
            telemetry.addLine("⚠️ TURRET MOTOR IS NULL!");
        }
        
        telemetry.addData("AutoTurret Enabled", autoTurretEnabled);
        telemetry.addData("Manual Override", turretManualOverride);
        if (robot.autoTurret != null) {
            telemetry.addData("Physical Angle", String.format("%.1f°", robot.autoTurret.getPhysicalAngleDegrees()));
            telemetry.addData("Initial Offset", String.format("%.1f°", robot.autoTurret.getInitialOffsetDegrees()));
            telemetry.addData("Manual Offset", String.format("%.1f°", robot.autoTurret.getManualOffsetDegrees()));
        } else {
            telemetry.addLine("⚠️ AUTO-TURRET SYSTEM IS NULL!");
        }
        telemetry.addLine("─────────────────");

        // Position Lock Status (important for shooting!)
        if (positionLockEnabled) {
            telemetry.addLine("🔒 POSITION LOCKED (release GP2 L-Trigger to move)");
        }
        
        // Turret Slot
        telemetry.addData("Balls Loaded", robot.autoIntake.getBallsLoaded());

        // Shooter
        telemetry.addLine();
        telemetry.addData("Shooter", "%.0f/%.0f %s", 
            robot.motors.getShooterVelocity(), 
            currentShooterVelocity,
            robot.shooting.isShooterAtTargetVelocity() ? "✓" : "⏳");

        // Turret (manual-only)
        double turretAngle = (robot.motors != null && robot.motors.turret != null)
            ? robot.motors.turret.getCurrentPosition() / RobotConstants.Turret.TICKS_PER_DEGREE
            : 0;
        telemetry.addData("Turret", "%.0f° MANUAL", turretAngle);

        // Hood
        String hoodMode = isHoodCustomPosition ? "CUSTOM" : HOOD_POSITION_NAMES[currentHoodIndex];
        telemetry.addData("Hood", "%.2f (%s)", currentHoodPosition, hoodMode);

        // Distance (to current alliance goal)
        double distGoalX = robot.autoTurret != null ? robot.autoTurret.getGoalX() : RobotConstants.Field.GOAL_X;
        double distGoalY = robot.autoTurret != null ? robot.autoTurret.getGoalY() : RobotConstants.Field.GOAL_Y;
        telemetry.addData("Distance", "%.0f in", robot.localization.getDistanceToPoint(distGoalX, distGoalY));

        // Alliance and AprilTag (simple status)
        if (robot.limelight != null && robot.limelight.isConnected()) {
            String allianceStr = robot.limelight.getAlliance() == LimelightManager.Alliance.BLUE ? "BLUE" : "RED";
            int targetTag = robot.limelight.getTargetTagId();
            boolean tagVisible = robot.limelight.hasAllianceTarget();
            telemetry.addData("Alliance", "%s (Tag %d)", allianceStr, targetTag);
            telemetry.addData("AprilTag", tagVisible ? 
                "✓ tx=" + String.format("%.1f°", robot.limelight.getTargetX()) : "✗ Tag " + targetTag + " not visible");
        }

        // Feature Toggles (compact)
        telemetry.addLine();
        telemetry.addData("Features", "Turret:MANUAL | AutoShoot:OFF");
    }

    /**
     * Page 1: Detailed system information
     */
    private void displayDetailsPage() {
        // Position
        telemetry.addLine("─── POSITION ───");
        telemetry.addData("X, Y", "%.1f, %.1f", robot.localization.getX(), robot.localization.getY());
        telemetry.addData("Heading", "%.1f°", robot.localization.getHeadingDegrees());

        // Intake details
        telemetry.addLine();
        telemetry.addLine("─── INTAKE ───");
        double gp2IntakeInput = -gamepad2.left_stick_y;
        if (Math.abs(gp2IntakeInput) > 0.1) {
            telemetry.addData("Motor", gp2IntakeInput > 0 ? "GP2 INTAKE" : "GP2 OUTTAKE");
        } else {
            telemetry.addData("Motor", robot.autoIntake.isIntakeMode() ? "RUNNING" : "OFF");
        }
        if (robot.autoIntake.isIntakeMode()) {
            telemetry.addData("Ball", robot.autoIntake.isBallDetected() ? 
                "✓ " + robot.autoIntake.getDetectedColor() : "✗ None");
        }

        // Limelight details
        telemetry.addLine();
        telemetry.addLine("─── LIMELIGHT ───");
        if (robot.limelight != null && robot.limelight.isConnected()) {
            telemetry.addData("Status", "✓ Connected");
            // Show alliance and target tag
            String allianceStr = robot.limelight.getAlliance() == LimelightManager.Alliance.BLUE ? "BLUE" : "RED";
            telemetry.addData("Alliance", "%s (Target: Tag %d)", allianceStr, robot.limelight.getTargetTagId());
            // Show all visible tags (with * marking our target)
            telemetry.addData("Tags Seen", robot.limelight.getVisibleTagIdsString());
            // Show tx/ty if our alliance tag is visible
            if (robot.limelight.hasAllianceTarget()) {
                telemetry.addData("tx/ty", "%.1f° / %.1f°", 
                    robot.limelight.getTargetX(), robot.limelight.getTargetY());
            } else if (robot.limelight.hasValidTarget()) {
                telemetry.addData("tx/ty", "%.1f° / %.1f° (other tag)", 
                    robot.limelight.getTargetX(), robot.limelight.getTargetY());
            } else {
                telemetry.addData("Target", "✗ No AprilTag visible");
            }
            telemetry.addData("Last Cal", lastCalibrationStatus);
        } else {
            telemetry.addData("Status", "✗ Not connected");
        }

        // Hood details
        telemetry.addLine();
        telemetry.addLine("─── HOOD ───");
        telemetry.addData("Position", "%.2f", currentHoodPosition);
        telemetry.addData("Range", "%.2f - %.2f", RobotConstants.Hood.MIN_POSITION, RobotConstants.Hood.MAX_POSITION);
    }

    /**
     * Page 2: Controls quick reference
     * Shows both Xbox and PS4 button names
     */
    private void displayControlsPage() {
        telemetry.addLine("─── GP1 (DRIVER) ───");
        telemetry.addData("A/Cross", "Toggle Mode");
        telemetry.addData("B/○", "Manual Ball +1 (sensor broken)");
        telemetry.addData("LB+B", "Manual Ball Undo -1");
        telemetry.addData("R Bumper", "Not used (Auto-Shoot OFF)");
        telemetry.addData("D-Pad U/D", "Hood Preset");
        telemetry.addData("D-Pad L/R", "Manual Turret");
        telemetry.addData("L Trigger", "Hold = HIGH Velocity");
        telemetry.addData("R Trigger", "Manual Push");
        telemetry.addData("Y/△", "HARD TURRET RESET (center first!)");

        telemetry.addLine();
        telemetry.addLine("─── GP2 (OPERATOR) ───");
        String allianceStr = robot.limelight != null ? 
            (robot.limelight.getAlliance() == LimelightManager.Alliance.BLUE ? "BLUE" : "RED") : "N/A";
        telemetry.addData("A/×", allianceStr + " | Alliance Toggle");
        telemetry.addData("X/□", "Not used (Auto-Turret OFF)");
        telemetry.addData("Y/△", "Not used (Auto-Cal OFF)");
        telemetry.addData("L Trigger", "HOLD = Position Lock 🔒");
        telemetry.addData("L Stick Y", "Intake Control");
        telemetry.addData("R Stick X", "Turret Manual");
        telemetry.addData("D-Pad U/D", "Hood Fine");
        telemetry.addData("D-Pad L/R", "Turret Fine");
        telemetry.addData("L Bumper", "Turret Manual (fine)");
        telemetry.addData("R Bumper", "Turret Manual (fine)");
        telemetry.addData("Back/Share", "Next Page");

        telemetry.addLine();
        telemetry.addLine("─── EMERGENCY ───");
        telemetry.addData("Back+Start", "STOP ALL (both controllers)");
    }

    @Override
    public void stop() {
        robot.stopAll();
        // Clear saved turret position/offset so next standalone TeleOp starts fresh.
        // Without this, the next TeleOp run thinks Auto ran and skips encoder reset.
        RobotMotors.clearSavedTurretPosition();
        RobotMotors.clearSavedTurretOffset();
        telemetry.addLine("⏹️ TeleOp Stopped");
        telemetry.update();
    }
}
