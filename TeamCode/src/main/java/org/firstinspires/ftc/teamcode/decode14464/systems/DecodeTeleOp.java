package org.firstinspires.ftc.teamcode.decode14464.systems;

import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import org.firstinspires.ftc.teamcode.decode14464.HoodConstants;

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
 * - Left Stick Y: Manual intake control (Up = Intake, Down = Outtake)
 * - Right Stick X: Turret manual override (variable speed)
 * - D-Pad Left/Right: Turret manual override (fine control)
 * - D-Pad Up/Down: Hood fine control (+/- 0.02)
 * - Left Bumper: RECALIBRATE turret (locks current position as correct reference)
 *
 * Auto-Turret Behavior:
 * =====================
 * - Auto-tracking is ALWAYS running (tracks goal automatically)
 * - GP2 controls provide MANUAL OVERRIDE when you need to adjust
 * - When you've manually aimed correctly, press LEFT BUMPER to RECALIBRATE
 * - Recalibrate tells the system "this is the correct position" and fixes drift
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
    private boolean prevLeftTrigger = false;

    // ========== SHOOTER VELOCITY SETTINGS ==========
    private static final double SHOOTER_HIGH_VELOCITY = 1450; // Long distance
    private static final double SHOOTER_LOW_VELOCITY = 1100;  // Short distance
    private double currentShooterVelocity = SHOOTER_HIGH_VELOCITY;
    private boolean isHighVelocityMode = true;

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
    private boolean prevGP2LeftBumper = false; // For recalibrate button detection

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

        // Enable auto-turret tracking (combined goal tracking)
        robot.autoTurret.enableCombinedGoalTracking();

        telemetry.addLine("🚀 TeleOp Started!");
        telemetry.addLine("✅ Shooter Running at " + currentShooterVelocity + " (" + (isHighVelocityMode ? "HIGH" : "LOW") + ")");
        telemetry.addLine("✅ Hood at " + HOOD_POSITION_NAMES[currentHoodIndex]);
        telemetry.addLine("✅ Auto-Turret: TRACKING (GP2 for manual override)");
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
            if (currentHoodPosition > HoodConstants.HOOD_MAX_POSITION) {
                currentHoodPosition = HoodConstants.HOOD_MAX_POSITION;
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
            if (currentHoodPosition < HoodConstants.HOOD_MIN_POSITION) {
                currentHoodPosition = HoodConstants.HOOD_MIN_POSITION;
            }
            isHoodCustomPosition = true; // Mark as custom position
            if (robot.servos != null && robot.servos.hood_5 != null) {
                robot.servos.hood_5.setPosition(currentHoodPosition);
            }
            telemetry.addLine("🔧 Hood Fine: " + String.format("%.2f", currentHoodPosition));
        }
        prevGP2DpadDown = gamepad2.dpad_down;

        // ========== GP2: TURRET MANUAL OVERRIDE (D-Pad Left/Right + Right Stick X) ==========
        // Auto-tracking is always running, but GP2 can manually override when needed
        double turretDpadInput = 0;
        if (gamepad2.dpad_left) {
            turretDpadInput = -TURRET_MANUAL_POWER;
        } else if (gamepad2.dpad_right) {
            turretDpadInput = TURRET_MANUAL_POWER;
        }
        
        double turretStickInput = -gamepad2.right_stick_x * 0.5; // Right stick X for variable control
        
        // Determine if manual override is active
        boolean hasManualInput = Math.abs(turretDpadInput) > 0.01 || Math.abs(turretStickInput) > 0.1;
        
        if (hasManualInput) {
            // Manual override active - apply power directly to turret motor
            turretManualOverride = true;
            double manualPower = Math.abs(turretDpadInput) > 0.01 ? turretDpadInput : turretStickInput;
            robot.motors.turret.setPower(manualPower);
        } else {
            // No manual input - auto-tracking runs normally via robot.updateAll()
            turretManualOverride = false;
            // Auto-tracking handled by robot.updateAll() -> autoTurret.update()
        }

        // ========== GP2: RECALIBRATE TURRET POSITION (Left Bumper) ==========
        // When you've manually aimed to the correct position, press Left Bumper to
        // tell the system "this is correct" - fixes any tracking drift/error
        boolean gp2LeftBumperPressed = gamepad2.left_bumper;
        if (gp2LeftBumperPressed && !prevGP2LeftBumper) {
            robot.autoTurret.recalibratePosition();
            telemetry.addLine("✅ Turret RECALIBRATED - Position locked as reference!");
        }
        prevGP2LeftBumper = gp2LeftBumperPressed;

        // ========== SHOOTER VELOCITY TOGGLE (Left Trigger) ==========
        // Press left trigger (>0.5) to toggle between HIGH and LOW velocity
        boolean leftTriggerPressed = gamepad1.left_trigger > 0.5;
        if (leftTriggerPressed && !prevLeftTrigger) {
            isHighVelocityMode = !isHighVelocityMode;
            currentShooterVelocity = isHighVelocityMode ? SHOOTER_HIGH_VELOCITY : SHOOTER_LOW_VELOCITY;
            
            // Update shooter velocity immediately
            robot.shooting.startShooter(currentShooterVelocity);
            
            telemetry.addLine(isHighVelocityMode ? "🚀 Velocity: HIGH (long range)" : "🎯 Velocity: LOW (short range)");
        }
        prevLeftTrigger = leftTriggerPressed;

        // ========== MANUAL BALL LAUNCH (Right Trigger) ==========
        // Right Trigger: Manual ball push servo control (only in launch mode AND when auto-shoot is NOT active)
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

        // ========== TELEMETRY ==========
        updateTelemetry();
    }

    private void updateTelemetry() {
        // ===== MODE STATUS =====
        telemetry.addLine("========== MODE ==========");
        String modeText = robot.autoIntake.isIntakeMode() ? "🔵 INTAKE" : "🔴 LAUNCH";
        telemetry.addData("Current Mode", modeText);
        telemetry.addData("Turret Slot", robot.autoIntake.getCurrentIndex());
        // Show intake motor status with GP2 override info
        double gp2IntakeInput = -gamepad2.left_stick_y;
        String intakeStatus;
        if (Math.abs(gp2IntakeInput) > 0.1) {
            intakeStatus = gp2IntakeInput > 0 ? "🎮 GP2 INTAKE" : "🎮 GP2 OUTTAKE";
        } else {
            intakeStatus = robot.autoIntake.isIntakeMode() ? "✓ RUNNING" : "✗ OFF";
        }
        telemetry.addData("Intake Motor", intakeStatus);

        // ===== AUTO-INTAKE STATUS =====
        if (robot.autoIntake.isIntakeMode()) {
            telemetry.addLine();
            telemetry.addLine("===== AUTO-INTAKE =====");
            telemetry.addData("Ball Detected", robot.autoIntake.isBallDetected() ? "✓ YES" : "✗ NO");
            telemetry.addData("Color", robot.autoIntake.getDetectedColor());
        }

        // ===== AUTO-SHOOT STATUS =====
        if (robot.autoShoot.isInProgress()) {
            telemetry.addLine();
            telemetry.addLine("===== AUTO-SHOOT =====");
            telemetry.addData("Status", "🎯 SHOOTING!");
            telemetry.addData("Balls Shot", robot.autoShoot.getBallsShot() + "/" + robot.autoShoot.getBallsToShoot());
            telemetry.addData("State", robot.autoShoot.getCurrentState());
        }

        // ===== SHOOTER STATUS =====
        telemetry.addLine();
        telemetry.addLine("====== SHOOTER ======");
        telemetry.addData("Mode", isHighVelocityMode ? "🚀 HIGH" : "🎯 LOW");
        telemetry.addData("Velocity", "%.0f / %.0f", robot.motors.getShooterVelocity(), currentShooterVelocity);
        telemetry.addData("Ready", robot.shooting.isShooterAtTargetVelocity() ? "✓ YES" : "⏳ Spinning Up");

        // ===== HOOD STATUS =====
        telemetry.addLine();
        telemetry.addLine("======= HOOD =======");
        if (isHoodCustomPosition) {
            telemetry.addData("Mode", "🔧 CUSTOM (GP2)");
        } else {
            telemetry.addData("Mode", "📌 " + HOOD_POSITION_NAMES[currentHoodIndex]);
        }
        telemetry.addData("Position", "%.2f", currentHoodPosition);
        telemetry.addData("Range", String.format("%.2f - %.2f", HoodConstants.HOOD_MIN_POSITION, HoodConstants.HOOD_MAX_POSITION));

        // ===== TURRET MOTOR STATUS =====
        telemetry.addLine();
        telemetry.addLine("===== AUTO-TURRET =====");
        telemetry.addData("Mode", turretManualOverride ? "🎮 MANUAL OVERRIDE" : "🎯 AUTO-TRACKING");
        telemetry.addData("Status", robot.autoTurret.isOnTarget() ? "✓ ON TARGET" : "⏳ Adjusting");
        telemetry.addData("Turret Angle", "%.1f°", robot.autoTurret.getTurretAngle());

        // ===== POSITION INFO =====
        telemetry.addLine();
        telemetry.addLine("====== POSITION ======");
        telemetry.addData("X, Y", "%.1f, %.1f", robot.localization.getX(), robot.localization.getY());
        telemetry.addData("Heading", "%.1f°", robot.localization.getHeadingDegrees());

        // ===== CONTROLS REMINDER =====
        telemetry.addLine();
        telemetry.addLine("=== GP1 CONTROLS ===");
        telemetry.addData("A/Cross", "Toggle Mode");
        telemetry.addData("Right Bumper", "Auto-Shoot");
        telemetry.addData("D-Pad L/R", "Manual Turret");
        telemetry.addData("D-Pad U/D", "Hood Preset");
        telemetry.addData("Left Trigger", "Toggle Velocity");
        telemetry.addData("Right Trigger", robot.autoIntake.isIntakeMode() ? "Disabled" :
            String.format("Manual Push (%.0f%%)", gamepad1.right_trigger * 100));
        
        telemetry.addLine();
        telemetry.addLine("=== GP2 CONTROLS ===");
        telemetry.addData("Left Stick Y", String.format("Intake (%.0f%%)", Math.abs(gp2IntakeInput) * 100));
        telemetry.addData("Right Stick X", "Turret Override");
        telemetry.addData("D-Pad L/R", "Turret Fine Override");
        telemetry.addData("D-Pad U/D", "Hood Fine (+/- " + HOOD_INCREMENT + ")");
        telemetry.addData("Left Bumper", "RECALIBRATE Turret");

        telemetry.update();
    }

    @Override
    public void stop() {
        robot.stopAll();
        telemetry.addLine("⏹️ TeleOp Stopped");
        telemetry.update();
    }
}
