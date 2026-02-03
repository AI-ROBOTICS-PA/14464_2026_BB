package org.firstinspires.ftc.teamcode.decode14464.systems;

import com.pedropathing.geometry.Pose;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

/**
 * Example TeleOp using the new modular RobotSystem
 * Demonstrates how to use all features in a clean, simple way
 */
@TeleOp(name = "System TeleOp Example", group = "Systems")
public class SystemTeleOpExample extends OpMode {

    private RobotSystem robot;

    // Button state tracking
    private boolean prevA = false;
    private boolean prevB = false;
    private boolean prevX = false;
    private boolean prevY = false;
    private boolean prevDpadLeft = false;
    private boolean prevDpadRight = false;
    private boolean prevRightBumper = false;
    private boolean prevLeftBumper = false;

    // Intake motor state - AUTO START
    private boolean intakeMotorOn = true; // Start intake ON by default

    // Shooter motor state - AUTO START with high velocity
    private boolean shooterMotorOn = true; // Start shooter ON by default

    // Shooter velocity modes (LOW values for testing)
    private static final double HIGH_VELOCITY = 460;  // Testing velocity
    private static final double LOW_VELOCITY = 100;   // Testing velocity
    private boolean isHighVelocityMode = true; // Start with high velocity

    // Auto-shoot state machine - hybrid approach with minimal timing
    private enum AutoShootState {
        IDLE,
        PUSH,           // Push ball forward
        WAIT_PUSH,      // Wait for push servo to reach position
        RETRACT,        // Retract ball push
        WAIT_RETRACT,   // Wait for retract to complete
        ADVANCE         // Move turret backwards
    }
    private AutoShootState autoShootState = AutoShootState.IDLE;
    private int ballsShot = 0;
    private int ballsToShoot = 0;
    private long stateStartTime = 0;
    private boolean turretMoved = false; // Track if turret already moved this cycle

    // Diagnostic counters
    private int sensorDetectedCount = 0;  // How many times sensor triggered
    private int timeoutUsedCount = 0;     // How many times timeout was used

    @Override
    public void init() {
        // Initialize robot system with starting pose
        robot = new RobotSystem();
        robot.init(hardwareMap, new Pose(0, 0, 0));


        telemetry.addLine("✅ Robot System Initialized");
        telemetry.addLine("⏸️ Motors will start when you press START");
        telemetry.addLine();
        telemetry.addLine("Controls:");
        telemetry.addLine("  A/Cross: Toggle Intake/Launch Mode");
        telemetry.addLine("  Y/Triangle: Toggle Shooter Velocity (460/100)");
        telemetry.addLine("  Right Bumper: AUTO-SHOOT 3 Balls");
        telemetry.addLine("  Left Stick Y (GP2): Variable Intake (for outtake)");
        telemetry.addLine("  DPAD Left/Right: Manual turret servo");
        telemetry.addLine();
        telemetry.addLine("Auto-Turret & Auto-Hood: DISABLED for basic testing");
        telemetry.update();
    }

    @Override
    public void start() {
        // Start teleop drive
        robot.localization.getFollower().startTeleopDrive();

        // START MOTORS when match begins (not during init)

        // Start intake motor
        robot.autoIntake.startIntakeMotor();
        intakeMotorOn = true;

        // Start shooter at high velocity (1460)
        robot.shooting.startShooter(HIGH_VELOCITY);
        shooterMotorOn = true;
        isHighVelocityMode = true;
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
            false // Field-centric (required for accurate localization)
        );

        // ========== BUTTON CONTROLS ==========

        // A/Cross: Toggle Intake/Launch Mode (support both Xbox and PlayStation)
        boolean aPressed = gamepad1.a || gamepad1.cross;
        if (aPressed && !prevA) {
            robot.autoIntake.toggleMode();
        }
        prevA = aPressed;

        // B/Circle: Toggle Auto-Turret [COMMENTED OUT FOR BASIC TESTING]
        /*
        boolean bPressed = gamepad1.b || gamepad1.circle;
        if (bPressed && !prevB) {
            robot.autoTurret.toggle();
        }
        prevB = bPressed;
        */

        // X/Square: Toggle Auto-Hood [COMMENTED OUT FOR BASIC TESTING]
        /*
        boolean xPressed = gamepad1.x || gamepad1.square;
        if (xPressed && !prevX) {
            robot.autoHood.toggleAutoAdjust();
        }
        prevX = xPressed;
        */

        // Y/Triangle: Toggle Shooter Velocity between 1460 and 1100 (support both Xbox and PlayStation)
        boolean yPressed = gamepad1.y || gamepad1.triangle;
        if (yPressed && !prevY) {
            if (isHighVelocityMode) {
                // Switch to low velocity
                robot.shooting.startShooter(LOW_VELOCITY);
                isHighVelocityMode = false;
            } else {
                // Switch to high velocity
                robot.shooting.startShooter(HIGH_VELOCITY);
                isHighVelocityMode = true;
            }
        }
        prevY = yPressed;

        // Right Bumper: Trigger Auto-Shoot (hybrid timing - fast but reliable)
        if (gamepad1.right_bumper && !prevRightBumper) {
            // Only start if in LAUNCH mode and not already shooting
            if (!robot.autoIntake.isIntakeMode() && autoShootState == AutoShootState.IDLE) {
                // Calculate balls to shoot: position 0=1 ball, 1=2 balls, 2=3 balls
                ballsToShoot = robot.autoIntake.getCurrentIndex() + 1;
                ballsShot = 0;
                sensorDetectedCount = 0;  // Reset diagnostic counters
                timeoutUsedCount = 0;
                autoShootState = AutoShootState.PUSH;
                stateStartTime = System.currentTimeMillis();
                telemetry.addLine("🎯 AUTO-SHOOT STARTED! Shooting " + ballsToShoot + " balls from position " + robot.autoIntake.getCurrentIndex());
            }
        }
        prevRightBumper = gamepad1.right_bumper;

        // ========== SIMPLE AUTO-SHOOT STATE MACHINE ==========
        updateAutoShoot();

        // Left Bumper: Enable Full Auto Mode [COMMENTED OUT - Auto features disabled for testing]
        /*
        if (gamepad1.left_bumper && !prevLeftBumper) {
            if (robot.autoTurret.isEnabled() && robot.autoHood.isAutoAdjustEnabled()) {
                robot.disableFullAuto();
            } else {
                robot.enableFullAuto();
            }
        }
        prevLeftBumper = gamepad1.left_bumper;
        */

        // DPAD Left/Right: Manual turret servo control
        if (gamepad1.dpad_left && !prevDpadLeft) {
            robot.autoIntake.moveToPreviousSlot();
        }
        prevDpadLeft = gamepad1.dpad_left;

        if (gamepad1.dpad_right && !prevDpadRight) {
            robot.autoIntake.advanceToNextSlot();
        }
        prevDpadRight = gamepad1.dpad_right;

        // ========== INTAKE MOTOR CONTROLS ==========

        // Intake motor runs continuously as part of the intake system
        // Only Gamepad 2 can override for variable speed (e.g., outtake)

        // Left Stick Y (Gamepad 2): Variable intake control (for outtake if needed)
        if (Math.abs(gamepad2.left_stick_y) > 0.1) {
            robot.autoIntake.setIntakeMotorPower(-gamepad2.left_stick_y);
            intakeMotorOn = (Math.abs(gamepad2.left_stick_y) > 0.1);
        } else {
            // Return to normal intake operation
            if (!intakeMotorOn) {
                robot.autoIntake.startIntakeMotor();
                intakeMotorOn = true;
            }
        }

        // ========== SHOOTER MOTOR CONTROLS ==========

        // Shooter motor runs continuously - Y button only changes velocity
        // No manual on/off control - shooter is always running when program is active

        // ========== TELEMETRY ==========
        telemetry.addLine("━━━ POSITION ━━━");
        telemetry.addData("X, Y", "%.1f, %.1f", robot.localization.getX(), robot.localization.getY());
        telemetry.addData("Heading", "%.1f°", robot.localization.getHeadingDegrees());
        telemetry.addLine();

        // AUTO SYSTEMS - COMMENTED OUT FOR BASIC TESTING
        /*
        telemetry.addLine("━━━ AUTO SYSTEMS ━━━");
        telemetry.addData("Auto-Turret", robot.autoTurret.isEnabled() ? "✓ ON" : "✗ OFF");
        telemetry.addData("Auto-Hood", robot.autoHood.isAutoAdjustEnabled() ? "✓ ON" : "✗ OFF");
        telemetry.addData("Turret Angle", "%.1f°", robot.autoTurret.getTurretAngle());
        telemetry.addData("On Target", robot.autoTurret.isOnTarget() ? "✓ YES" : "✗ NO");
        telemetry.addLine();
        */

        telemetry.addLine("━━━ SHOOTING ━━━");
        telemetry.addData("Distance to Goal", "%.1f in", robot.getDistanceToGoal());
        telemetry.addData("Shooter Motor", shooterMotorOn ? "✓ ON" : "✗ OFF");
        telemetry.addData("Velocity Mode", isHighVelocityMode ? "HIGH (460)" : "LOW (100)");
        telemetry.addData("Current Velocity", "%.0f", robot.motors.getShooterVelocity());

        // Show sensor state with more detail for debugging
        boolean sensorPressed = robot.shooting.isBallPushRetracted();
        telemetry.addData("Ball Push Sensor", sensorPressed ? "✓ PRESSED (retracted)" : "✗ NOT PRESSED (extended)");
        telemetry.addData("Sensor Raw", sensorPressed ? "TRUE" : "FALSE");

        // Diagnostic: Show which path auto-shoot is using
        if (autoShootState != AutoShootState.IDLE || sensorDetectedCount > 0 || timeoutUsedCount > 0) {
            telemetry.addData("Sensor Detections", sensorDetectedCount);
            telemetry.addData("Timeout Fallbacks", timeoutUsedCount);

            if (sensorDetectedCount > 0 && timeoutUsedCount == 0) {
                telemetry.addData("Status", "✅ SENSOR WORKING!");
            } else if (timeoutUsedCount > 0 && sensorDetectedCount == 0) {
                telemetry.addData("Status", "⚠️ USING TIMEOUT (sensor not working)");
            } else if (sensorDetectedCount > 0 && timeoutUsedCount > 0) {
                telemetry.addData("Status", "⚠️ MIXED (sensor unreliable)");
            }
        }

        telemetry.addData("Auto-Shoot", autoShootState == AutoShootState.IDLE ? "Idle" : "🎯 SHOOTING");
        telemetry.addData("Balls Shot", ballsShot + "/" + ballsToShoot);
        telemetry.addData("State", autoShootState.toString());
        if (autoShootState != AutoShootState.IDLE) {
            long elapsed = System.currentTimeMillis() - stateStartTime;
            telemetry.addData("State Elapsed", elapsed + "ms");
        }
        telemetry.addLine();

        telemetry.addLine("━━━ INTAKE ━━━");
        telemetry.addData("Mode", robot.autoIntake.isIntakeMode() ? "🔵 INTAKE" : "🔴 LAUNCH");
        telemetry.addData("Slot", robot.autoIntake.getCurrentIndex());
        telemetry.addData("Ball Detected", robot.autoIntake.isBallDetected() ? "✓ YES" : "✗ NO");
        telemetry.addData("Color", robot.autoIntake.getDetectedColor());
        telemetry.addData("Intake Motor", intakeMotorOn ? "✓ ON" : "✗ OFF");
        telemetry.addData("GP2 Intake Power", "%.2f", -gamepad2.left_stick_y);

        telemetry.update();
    }

    /**
     * Touch sensor-based auto-shoot - MUCH FASTER!
     * Uses touch sensor to detect when ball push is retracted
     * No waiting for arbitrary timing - moves as soon as safe!
     */
    private void updateAutoShoot() {
        if (autoShootState == AutoShootState.IDLE) {
            return; // Not shooting
        }

        long elapsed = System.currentTimeMillis() - stateStartTime;

        switch (autoShootState) {
            case PUSH:
                // Command servo to push position
                robot.shooting.setBallPushPosition(RobotConstants.BallPush.PUSH_POSITION); // 0.77
                telemetry.addLine("→ Pushing ball " + (ballsShot + 1));
                autoShootState = AutoShootState.WAIT_PUSH;
                stateStartTime = System.currentTimeMillis();
                break;

            case WAIT_PUSH:
                // Wait for servo to push ball - optimized timing
                if (elapsed > 300) { // 300ms for servo to push forward
                    telemetry.addLine("✓ Ball " + (ballsShot + 1) + " pushed!");
                    ballsShot++;
                    autoShootState = AutoShootState.RETRACT;
                    stateStartTime = System.currentTimeMillis();
                }
                break;

            case RETRACT:
                // Command servo to retract position
                robot.shooting.setBallPushPosition(RobotConstants.BallPush.RETRACT_POSITION); // 0.50
                telemetry.addLine("→ Retracting...");
                autoShootState = AutoShootState.WAIT_RETRACT;
                stateStartTime = System.currentTimeMillis();
                break;

            case WAIT_RETRACT:
                // Wait for ball push servo to retract - USE TOUCH SENSOR with timeout safety
                // Sensor is pressed when ball push is fully retracted
                boolean sensorRetracted = robot.shooting.isBallPushRetracted();
                boolean timeoutReached = elapsed > 500; // Safety timeout - don't wait forever

                if (sensorRetracted) {
                    sensorDetectedCount++;  // Track sensor success
                    telemetry.addLine("✓ Retracted (SENSOR DETECTED), safe to move");
                    telemetry.addData("Retract time", elapsed + "ms");

                    // Check if we have more balls to shoot
                    if (ballsShot < ballsToShoot) {
                        // More balls to shoot - advance turret
                        autoShootState = AutoShootState.ADVANCE;
                        stateStartTime = System.currentTimeMillis();
                        turretMoved = false; // Reset flag for new advance
                    } else {
                        // All balls shot!
                        telemetry.addLine("✅ All " + ballsShot + " balls shot! Complete.");
                        autoShootState = AutoShootState.IDLE;
                    }
                } else if (timeoutReached) {
                    timeoutUsedCount++;  // Track timeout fallback
                    // Safety timeout - move anyway after 500ms
                    telemetry.addLine("⚠️ Timeout (sensor didn't trigger), moving anyway");
                    telemetry.addData("Retract time", elapsed + "ms (TIMEOUT)");

                    if (ballsShot < ballsToShoot) {
                        autoShootState = AutoShootState.ADVANCE;
                        stateStartTime = System.currentTimeMillis();
                        turretMoved = false;
                    } else {
                        telemetry.addLine("✅ All " + ballsShot + " balls shot! Complete.");
                        autoShootState = AutoShootState.IDLE;
                    }
                } else {
                    // Still waiting for sensor or timeout
                    telemetry.addData("Waiting for sensor", elapsed + "ms");
                }
                break;

            case ADVANCE:
                // Move turret backwards to previous slot - ONLY ONCE using flag
                if (!turretMoved) {
                    robot.autoIntake.moveToPreviousSlot();
                    turretMoved = true;
                    telemetry.addLine("→ Moving to slot " + robot.autoIntake.getCurrentIndex());
                }

                // Wait for turret servo to move
                if (elapsed > 300) { // 300ms for turret to reach next position
                    telemetry.addLine("✓ Ready at slot " + robot.autoIntake.getCurrentIndex());
                    autoShootState = AutoShootState.PUSH;
                    stateStartTime = System.currentTimeMillis();
                }
                break;
        }
    }

    @Override
    public void stop() {
        robot.stopAll();
    }
}

