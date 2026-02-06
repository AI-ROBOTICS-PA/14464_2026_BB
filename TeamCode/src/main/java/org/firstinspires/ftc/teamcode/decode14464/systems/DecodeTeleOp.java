package org.firstinspires.ftc.teamcode.decode14464.systems;

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
 * - A/Cross: Toggle Alliance (BLUE=Tag 20, RED=Tag 24) for Limelight targeting
 * - X/Square: Toggle Auto-Turret tracking ON/OFF
 * - Y/Triangle: Toggle Auto-Calibration ON/OFF
 * - Left Stick Y: Manual intake control (Up = Intake, Down = Outtake)
 * - Right Stick X: Turret manual override (variable speed)
 * - D-Pad Left/Right: Turret manual override (fine control)
 * - D-Pad Up/Down: Hood fine control (+/- 0.02)
 * - Left Bumper: Manual RECALIBRATE (locks current position as correct reference)
 * - Right Bumper: LIMELIGHT CALIBRATE (uses AprilTag for precision calibration)
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
 * - MANUAL: Press GP2 RIGHT BUMPER when AprilTag visible for instant calibration
 * - Calibrates BOTH turret pointing AND robot localization position
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
@TeleOp(name = "DECODE TeleOp", group = "Main")
public class DecodeTeleOp extends OpMode {

    private RobotSystem robot;

    // Button state tracking for edge detection
    private boolean prevA = false;
    private boolean prevRightBumper = false;
    private boolean prevDpadLeft = false;
    private boolean prevDpadRight = false;
    private boolean prevDpadUp = false;
    private boolean prevDpadDown = false;

    // ========== SHOOTER VELOCITY SETTINGS ==========
    private static final double SHOOTER_HIGH_VELOCITY = 1450; // Long distance (hold trigger)
    private static final double SHOOTER_LOW_VELOCITY = 1100;  // Short distance (default)
    private double currentShooterVelocity = SHOOTER_LOW_VELOCITY; // Start at LOW
    private boolean isHighVelocityMode = false; // Start at LOW

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
    private boolean autoTurretEnabled = true; // Toggle with GP2 X - ENABLED by default
    private boolean prevGP2X = false; // For auto-turret toggle button
    private boolean prevGP2LeftBumper = false; // For recalibrate button detection
    private boolean prevGP2RightBumper = false; // For Limelight calibration button

    // ========== LIMELIGHT AUTO-CALIBRATION ==========
    private static final double AUTO_CALIBRATE_COOLDOWN_MS = 2000; // Minimum time between auto-calibrations
    private static final double AUTO_CALIBRATE_TX_THRESHOLD = 5.0; // Max tx error (degrees) for auto-calibrate
    private static final double AUTO_CALIBRATE_VELOCITY_THRESHOLD = 0.3; // Max robot velocity for auto-calibrate
    private double lastAutoCalibrationTime = 0;
    private boolean autoCalibrationEnabled = true; // Can be toggled with GP2 Y button
    private String lastCalibrationStatus = "None"; // For telemetry
    private boolean prevGP2Y = false; // For auto-calibration toggle button
    private boolean prevGP2A = false; // For alliance toggle button

    // ========== RUMBLE FEEDBACK ==========
    private boolean wasReadyToShoot = false; // Track state change for rumble
    private boolean wasTurretAtLimit = false; // Track turret limit for warning rumble

    // ========== TELEMETRY PAGES ==========
    private int telemetryPage = 0; // 0=Essential, 1=Details, 2=Controls
    private static final int TELEMETRY_PAGE_COUNT = 3;
    private boolean prevGP2Back = false;

    @Override
    public void init() {
        // Initialize robot - preserves position from Auto
        robot = new RobotSystem();
        robot.initTeleOp(hardwareMap);

        // Initialize hood servo to starting position
        currentHoodPosition = HOOD_POSITIONS[currentHoodIndex];
        if (robot.servos != null && robot.servos.hood_5 != null) {
            robot.servos.hood_5.setPosition(currentHoodPosition);
        }

        telemetry.addLine("✅ DECODE TeleOp Initialized");
        telemetry.addLine();
        telemetry.addLine("Controls:");
        telemetry.addLine("  A/Cross: Toggle Intake ↔ Launch Mode");
        telemetry.addLine("  Right Bumper: Auto-Shoot All Balls");
        telemetry.addLine("  D-Pad Up/Down: Hood Preset (LONG/MEDIUM/SHORT)");
        telemetry.addLine("  Left Trigger: Toggle HIGH/LOW Velocity");
        telemetry.addLine();
        telemetry.addData("Current Mode", "LAUNCH (ready to shoot)");
        telemetry.addData("Hood", HOOD_POSITION_NAMES[currentHoodIndex]);
        telemetry.addData("Velocity", isHighVelocityMode ? "HIGH" : "LOW");
        telemetry.update();
    }

    @Override
    public void start() {
        // Start TeleOp driving
        robot.localization.getFollower().startTeleopDrive();

        // Start shooter motor at current velocity mode
        robot.shooting.startShooter(currentShooterVelocity);

        // Intake motor only starts if in intake mode (default is launch mode, so don't start)
        // Intake will start when user switches to intake mode with A/Cross button

        // Auto-turret starts DISABLED - press GP2 X to enable after testing in TurretTest
        // This allows you to verify turret tracking works before enabling in competition code
        // Enable auto-turret tracking by default
        robot.autoTurret.enableCombinedGoalTracking();

        telemetry.addLine("🚀 TeleOp Started!");
        telemetry.addLine("✅ Shooter Running at " + currentShooterVelocity + " (" + (isHighVelocityMode ? "HIGH" : "LOW") + ")");
        telemetry.addLine("✅ Hood at " + HOOD_POSITION_NAMES[currentHoodIndex]);
        telemetry.addLine("✅ Auto-Turret: ENABLED (GP2 X to toggle)");
        telemetry.addLine("ℹ️ Intake Motor: OFF (switch to INTAKE mode to enable)");
        telemetry.update();
    }

    @Override
    public void loop() {
        // ========== UPDATE ALL SYSTEMS ==========
        robot.updateAll();

        // ========== DRIVE CONTROL ==========
        robot.localization.getFollower().setTeleOpDrive(
            -gamepad1.left_stick_y,
            -gamepad1.left_stick_x,
            -gamepad1.right_stick_x,
            true // Field-centric control
        );

        // ========== MODE SWITCHING ==========
        // A/Cross: Toggle between Intake and Launch modes
        // Support both controller types: PS4 (cross) and Xbox (a)
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

        // ========== MANUAL TURRET OVERRIDE (D-Pad Left/Right) ==========
        // D-Pad Left: Move to previous slot (manual retreat)
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

        // ========== GP2: TOGGLE AUTO-TURRET (X/Square Button) ==========
        // Press X (Xbox) or Square (PS4) to toggle auto-turret tracking on/off
        // Starts DISABLED - test in TurretTest first, then enable here
        boolean gp2XPressed = gamepad2.x || gamepad2.square; // Support both Xbox and PS4
        if (gp2XPressed && !prevGP2X) {
            autoTurretEnabled = !autoTurretEnabled;
            if (autoTurretEnabled) {
                robot.autoTurret.enableCombinedGoalTracking();
                gamepad2.rumble(300);
                telemetry.log().add("🎯 Auto-Turret ENABLED");
            } else {
                robot.autoTurret.disableCombinedGoalTracking();
                gamepad2.rumble(100);
                telemetry.log().add("⏹️ Auto-Turret DISABLED");
            }
        }
        prevGP2X = gp2XPressed;

        // ========== GP2: TURRET MANUAL CONTROL (D-Pad Left/Right + Right Stick X) ==========
        // When auto-turret is enabled: GP2 provides manual override
        // When auto-turret is disabled: GP2 provides direct manual control
        double turretDpadInput = 0;
        if (gamepad2.dpad_left) {
            turretDpadInput = -TURRET_MANUAL_POWER;
        } else if (gamepad2.dpad_right) {
            turretDpadInput = TURRET_MANUAL_POWER;
        }
        
        double turretStickInput = -gamepad2.right_stick_x * 0.5; // Right stick X for variable control
        
        // Determine if manual input is active
        boolean hasManualInput = Math.abs(turretDpadInput) > 0.01 || Math.abs(turretStickInput) > 0.1;
        
        if (autoTurretEnabled) {
            // Auto-turret is ON - manual input provides temporary override
            if (hasManualInput) {
                turretManualOverride = true;
                double manualPower = Math.abs(turretDpadInput) > 0.01 ? turretDpadInput : turretStickInput;
                if (robot.motors != null && robot.motors.turret != null) {
                    robot.motors.turret.setPower(manualPower);
                }
            } else {
                // No manual input - if we were overriding, recalibrate to sync position
                if (turretManualOverride && robot.autoTurret != null) {
                    robot.autoTurret.recalibratePosition();
                }
                turretManualOverride = false;
                // Auto-tracking handled by robot.updateAll() -> autoTurret.update()
            }
        } else {
            // Auto-turret is OFF - always use manual control
            turretManualOverride = true; // Always in manual mode
            if (hasManualInput) {
                double manualPower = Math.abs(turretDpadInput) > 0.01 ? turretDpadInput : turretStickInput;
                if (robot.motors != null && robot.motors.turret != null) {
                    robot.motors.turret.setPower(manualPower);
                }
            } else {
                // No input - stop turret (since auto-tracking is off)
                if (robot.motors != null && robot.motors.turret != null) {
                    robot.motors.turret.setPower(0);
                }
            }
        }

        // ========== GP2: RECALIBRATE TURRET POSITION (Left Bumper) ==========
        // When you've manually aimed to the correct position, press Left Bumper to
        // tell the system "this is correct" - fixes any tracking drift/error
        boolean gp2LeftBumperPressed = gamepad2.left_bumper;
        if (gp2LeftBumperPressed && !prevGP2LeftBumper) {
            robot.autoTurret.recalibratePosition();
            lastCalibrationStatus = "Manual (LB)";
            telemetry.addLine("✅ Turret RECALIBRATED - Position locked as reference!");
        }
        prevGP2LeftBumper = gp2LeftBumperPressed;

        // ========== GP2: LIMELIGHT CALIBRATION (Right Bumper) ==========
        // Press Right Bumper to trigger Limelight-based calibration (when AprilTag visible)
        boolean gp2RightBumperPressed = gamepad2.right_bumper;
        if (gp2RightBumperPressed && !prevGP2RightBumper) {
            performLimelightCalibration(true); // true = manual trigger
        }
        prevGP2RightBumper = gp2RightBumperPressed;

        // ========== GP2: TOGGLE AUTO-CALIBRATION (Y/Triangle Button) ==========
        // Press Y (Xbox) or Triangle (PS4) to toggle automatic Limelight calibration on/off
        boolean gp2YPressed = gamepad2.y || gamepad2.triangle; // Support both Xbox and PS4
        if (gp2YPressed && !prevGP2Y) {
            autoCalibrationEnabled = !autoCalibrationEnabled;
            // Rumble feedback for toggle
            gamepad2.rumble(autoCalibrationEnabled ? 200 : 100);
        }
        prevGP2Y = gp2YPressed;

        // ========== GP2: TOGGLE ALLIANCE (A/Cross Button) ==========
        // Press A (Xbox) or Cross (PS4) to toggle between BLUE and RED alliance
        // This changes which AprilTag ID is used for calibration (BLUE=20, RED=24)
        boolean gp2APressed = gamepad2.a || gamepad2.cross; // Support both Xbox and PS4
        if (gp2APressed && !prevGP2A) {
            robot.limelight.toggleAlliance();
            // Long rumble for RED, short rumble for BLUE
            if (robot.limelight.getAlliance() == LimelightManager.Alliance.RED) {
                gamepad2.rumble(400); // Long = RED
            } else {
                gamepad2.rumble(150); // Short = BLUE
            }
        }
        prevGP2A = gp2APressed;

        // ========== AUTOMATIC LIMELIGHT CALIBRATION ==========
        // Auto-calibrate when conditions are good
        if (autoCalibrationEnabled && !turretManualOverride) {
            performAutoCalibration();
        }

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

        // ========== SHOOTER VELOCITY (Left Trigger = HIGH) ==========
        // Default: LOW velocity | Hold Left Trigger: HIGH velocity
        // Momentary - releases back to LOW when trigger released
        boolean leftTriggerHeld = gamepad1.left_trigger > 0.5;
        double targetVelocity = leftTriggerHeld ? SHOOTER_HIGH_VELOCITY : SHOOTER_LOW_VELOCITY;
        
        // Only update if velocity changed (avoid spamming motor commands)
        if (targetVelocity != currentShooterVelocity) {
            currentShooterVelocity = targetVelocity;
            isHighVelocityMode = leftTriggerHeld;
            robot.shooting.startShooter(currentShooterVelocity);
        }

        // ========== MANUAL BALL LAUNCH (Right Trigger) ==========
        // Right Trigger: Manual ball push servo control (only in launch mode AND when auto-shoot is NOT active)
        if (robot.servos != null && robot.servos.ballpush_4 != null) {
            if (!robot.autoIntake.isIntakeMode() && !robot.autoShoot.isInProgress()) {
                // Map trigger (0.0 to 1.0) to servo offset (0.0 to 0.27)
                double triggerValue = gamepad1.right_trigger;
                double servoOffset = triggerValue * 0.27;

                // Set ball push servo position
                // Initial position + offset moves servo forward to push ball
                robot.servos.ballpush_4.setPosition(
                    robot.servos.ballpush_initial_position + servoOffset
                );
            } else if (robot.autoIntake.isIntakeMode() && !robot.autoShoot.isInProgress()) {
                // In intake mode AND auto-shoot not active, keep ball push servo at initial position
                robot.servos.ballpush_4.setPosition(robot.servos.ballpush_initial_position);
            }
            // If auto-shoot IS active, don't touch the servo - let AutoShootSystem control it!
        }

        // ========== AUTO-SHOOT ==========
        // Right Bumper: Start auto-shooting sequence
        // Support both controller types
        boolean rightBumperPressed = gamepad1.right_bumper;
        if (rightBumperPressed && !prevRightBumper) {
            // Only shoot if in launch mode
            if (!robot.autoIntake.isIntakeMode() && !robot.autoShoot.isInProgress()) {
                // Determine number of balls based on current turret position
                int ballsToShoot = robot.autoIntake.getCurrentIndex() + 1;
                robot.autoShoot.start(ballsToShoot);
            }
        }
        prevRightBumper = rightBumperPressed;

        // ========== READY TO SHOOT RUMBLE ==========
        // Rumble when system becomes ready to shoot (turret on target + shooter at speed)
        boolean isReadyToShoot = robot.isReadyToShoot() && !robot.autoIntake.isIntakeMode();
        if (isReadyToShoot && !wasReadyToShoot) {
            // Just became ready - double pulse rumble to signal "ready!"
            gamepad1.rumbleBlips(2);
            gamepad2.rumbleBlips(1);
        }
        wasReadyToShoot = isReadyToShoot;
        
        // ========== TURRET AT LIMIT WARNING ==========
        // Warn driver when turret hits rotation limit (goal behind robot)
        boolean turretAtLimit = autoTurretEnabled && robot.autoTurret.isAtLimit();
        if (turretAtLimit && !wasTurretAtLimit) {
            // Just hit limit - long rumble to warn driver to rotate robot
            gamepad1.rumble(500); // 500ms warning
        }
        wasTurretAtLimit = turretAtLimit;

        // ========== TELEMETRY ==========
        updateTelemetry();
    }

    /**
     * Perform Limelight-based calibration
     * Calibrates both localization (position) and turret (pointing)
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
        com.pedropathing.geometry.Pose limelightPose = robot.limelight.getRobotPoseFromAprilTag();
        
        // === TURRET CALIBRATION ===
        // Use tx (horizontal angle) to calibrate turret pointing
        if (Math.abs(tx) > 0.5) { // Only calibrate if there's meaningful offset
            robot.autoTurret.calibrateFromLimelight(tx);
        }
        
        // === LOCALIZATION CALIBRATION ===
        // Use botpose to calibrate robot position (if available)
        if (limelightPose != null) {
            robot.localization.calibrateFromLimelight(limelightPose);
        }
        
        // Update status and provide rumble feedback
        if (isManual) {
            lastCalibrationStatus = String.format("Manual OK (tx=%.1f°)", tx);
            telemetry.addLine("✅ LIMELIGHT CALIBRATION COMPLETE!");
            telemetry.addLine(String.format("   Turret offset: %.1f°", tx));
            if (limelightPose != null) {
                telemetry.addLine(String.format("   Position: (%.1f, %.1f)", limelightPose.getX(), limelightPose.getY()));
            }
            // Rumble feedback for successful manual calibration
            gamepad2.rumble(300);
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
        
        // Check tx error - only auto-calibrate if we're close but not perfect
        double tx = robot.limelight.getTargetX();
        if (Math.abs(tx) > AUTO_CALIBRATE_TX_THRESHOLD) {
            return; // Error too large for auto-calibrate (might be wrong target)
        }
        
        // All conditions met - perform calibration
        performLimelightCalibration(false); // false = auto trigger
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
        // Turret Slot
        telemetry.addData("Balls Loaded", robot.autoIntake.getCurrentIndex() + 1);

        // Auto-Shoot Status (only if active)
        if (robot.autoShoot.isInProgress()) {
            telemetry.addLine();
            telemetry.addLine("🎯 AUTO-SHOOTING!");
            telemetry.addData("Progress", robot.autoShoot.getBallsShot() + "/" + robot.autoShoot.getBallsToShoot());
        }

        // Shooter
        telemetry.addLine();
        telemetry.addData("Shooter", "%.0f/%.0f %s", 
            robot.motors.getShooterVelocity(), 
            currentShooterVelocity,
            robot.shooting.isShooterAtTargetVelocity() ? "✓" : "⏳");

        // Turret (with launch-mode-only info and limit warning)
        String turretStatus;
        if (!autoTurretEnabled) {
            turretStatus = "MANUAL";
        } else if (turretManualOverride) {
            turretStatus = "OVERRIDE";
        } else if (!robot.autoTurret.isMovementAllowed()) {
            // In intake mode with launch-mode-only enabled - show tracking but not moving
            double pendingMove = robot.autoTurret.getPendingMovementDegrees();
            turretStatus = String.format("TRACKING (→%.0f° on LAUNCH)", pendingMove);
        } else if (robot.autoTurret.isAtLimit()) {
            // Goal is behind robot - turret at max rotation
            turretStatus = "⚠️ AT LIMIT (rotate robot!)";
        } else {
            turretStatus = robot.autoTurret.isOnTarget() ? "ON TARGET" : "Tracking...";
        }
        telemetry.addData("Turret", "%.0f° %s", robot.autoTurret.getTurretAngle(), turretStatus);

        // Hood
        String hoodMode = isHoodCustomPosition ? "CUSTOM" : HOOD_POSITION_NAMES[currentHoodIndex];
        telemetry.addData("Hood", "%.2f (%s)", currentHoodPosition, hoodMode);

        // Distance
        telemetry.addData("Distance", "%.0f in", robot.localization.getDistanceToPoint(
            RobotConstants.Field.GOAL_X, RobotConstants.Field.GOAL_Y));

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
        telemetry.addData("Features", 
            (autoTurretEnabled ? "Turret:ON" : "Turret:OFF") + " | " +
            (autoCalibrationEnabled ? "AutoCal:ON" : "AutoCal:OFF"));
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
        telemetry.addData("R Bumper", "Auto-Shoot");
        telemetry.addData("D-Pad U/D", "Hood Preset");
        telemetry.addData("D-Pad L/R", "Manual Turret");
        telemetry.addData("L Trigger", "Hold = HIGH Velocity");
        telemetry.addData("R Trigger", "Manual Push");

        telemetry.addLine();
        telemetry.addLine("─── GP2 (OPERATOR) ───");
        String allianceStr = robot.limelight != null ? 
            (robot.limelight.getAlliance() == LimelightManager.Alliance.BLUE ? "BLUE" : "RED") : "N/A";
        telemetry.addData("A/×", "Alliance: " + allianceStr);
        telemetry.addData("X/□", autoTurretEnabled ? "Turret: ON" : "Turret: OFF");
        telemetry.addData("Y/△", autoCalibrationEnabled ? "AutoCal: ON" : "AutoCal: OFF");
        telemetry.addData("L Stick Y", "Intake Control");
        telemetry.addData("R Stick X", "Turret Manual");
        telemetry.addData("D-Pad U/D", "Hood Fine");
        telemetry.addData("D-Pad L/R", "Turret Fine");
        telemetry.addData("L Bumper", "Recalibrate");
        telemetry.addData("R Bumper", "Limelight Cal");
        telemetry.addData("Back/Share", "Next Page");

        telemetry.addLine();
        telemetry.addLine("─── EMERGENCY ───");
        telemetry.addData("Back+Start", "STOP ALL (both controllers)");
    }

    @Override
    public void stop() {
        robot.stopAll();
        telemetry.addLine("⏹️ TeleOp Stopped");
        telemetry.update();
    }
}
