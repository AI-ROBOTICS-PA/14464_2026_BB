package org.firstinspires.ftc.teamcode.decode14464.auto;

import com.pedropathing.follower.Follower;
import com.pedropathing.geometry.BezierLine;
import com.pedropathing.geometry.Pose;
import com.pedropathing.paths.Path;
import com.pedropathing.paths.PathChain;
import com.pedropathing.util.Timer;
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;

import org.firstinspires.ftc.teamcode.decode14464.systems.LimelightManager;
import org.firstinspires.ftc.teamcode.decode14464.systems.RobotConstants;
import org.firstinspires.ftc.teamcode.decode14464.systems.RobotMotors;
import org.firstinspires.ftc.teamcode.decode14464.systems.RobotSystem;

/**
 * Auto Blue Far - Phase-by-Phase Selection
 * 
 * SEQUENCE (8 steps):
 *   Step 0: Begin on initial pose (50, 7.5) heading 180°
 *   Step 1: Move turret to goal angle
 *   Step 2: Auto-shoot 3 preloaded balls
 *   Step 3: Drive to intake position (36, 36)
 *   Step 4: Start auto intake motor
 *   Step 5: Drive forward to X=10 while intaking to sweep up 3 balls
 *   Step 6: Return to start/launch position (50, 7.5)
 *   Step 7: Auto-shoot 3 newly intaked balls
 *   Step 8: Drive to park position (24, 24)
 * 
 * DURING INIT:
 * - D-Pad Up/Down: Select which phase to STOP after
 * - A/Cross: Toggle "Manual Advance" mode (step through states one by one)
 * 
 * PHASES:
 * 1. INIT_TURRET - Move turret to initial goal angle
 * 2. FIRST_SHOOT - Flywheel spinup + auto-shoot 3 preloaded balls
 * 3. DRIVE_INTAKE - Drive to intake zone (36, 36)
 * 4. INTAKE - Forward sweep to X=10 while intaking (stops at 3 balls)
 * 5. DRIVE_LAUNCH - Return to launch position
 * 6. SECOND_SHOOT - Auto-shoot 3 intaked balls
 * 7. PARK - Drive to park position
 * 8. COMPLETE - Full auto (no stop)
 * 
 * DURING RUN (if Manual Advance ON):
 * - Press A/Cross to advance to next state
 */
@Autonomous(name = "Auto-Blue Far (Phase Select)", group = "DECODE Auto")
public class Auto_BlueFar_TEST extends OpMode {

    // ========== ROBOT SYSTEM ==========
    private RobotSystem robot;
    private Follower follower;
    private Timer actionTimer;
    
    // ========== TEST CONFIGURATION ==========
    private enum TestPhase {
        INIT_TURRET("1. Init Turret Only"),
        FIRST_SHOOT("2. First Shooting"),
        DRIVE_INTAKE("3. Drive to Intake"),
        INTAKE("4. Intake Balls (forward sweep)"),
        DRIVE_LAUNCH("5. Drive to Launch"),
        SECOND_SHOOT("6. Second Shooting"),
        PARK("7. Park"),
        COMPLETE("8. Full Auto (no stop)");
        
        final String displayName;
        TestPhase(String name) { this.displayName = name; }
    }
    
    private TestPhase stopAfterPhase = TestPhase.COMPLETE;
    private boolean manualAdvanceMode = false;
    private boolean waitingForAdvance = false;
    
    // Gamepad tracking for init_loop
    private boolean prevDpadUp = false;
    private boolean prevDpadDown = false;
    private boolean prevA = false;
    
    // ========== FIELD POSITIONS ==========
    // Robot is 18x15 inches. Pose = center. Y offset = 15/2 = 7.5 (side against bottom wall)
    private static final Pose START_POSE = new Pose(50, 7.5, Math.toRadians(180));
    private static final Pose INTAKE_POSE = new Pose(36, 36, Math.toRadians(180));
    // Step 5: Robot drives forward (heading 180° = -X direction) while intaking to sweep up balls
    private static final Pose INTAKE_FORWARD_POSE = new Pose(10, 36, Math.toRadians(180));
    // Step 8: Park position after second shoot (TUNE THIS for your field)
    private static final Pose PARK_POSE = new Pose(24, 24, Math.toRadians(180));
    
    // ========== SHOOTING PARAMETERS ==========
    private static final double SHOOTER_VELOCITY = RobotConstants.Shooter.HIGH_VELOCITY;
    private static final double HOOD_POSITION = RobotConstants.Hood.LONG_RANGE_POSITION;
    private static final LimelightManager.Alliance ALLIANCE = LimelightManager.Alliance.BLUE;
    private static final double INITIAL_TURRET_ANGLE = RobotConstants.Auto.BLUE_FAR_INITIAL_TURRET_ANGLE;
    
    // ========== STATE MACHINE ==========
    private enum AutoState {
        // Phase 1: Init Turret (offset-based, no motor movement)
        INIT,
        
        // Phase 2: First Shoot (preloaded 3 balls)
        LIMELIGHT_CALIBRATE,    // One-time: fine-tune turret with AprilTag after initial offset
        WAIT_SHOOTER_READY_1,
        WAIT_TURRET_AIM_1,
        SHOOT_SEQUENCE_1,
        WAIT_SHOOT_COMPLETE_1,
        
        // Phase 3: Drive to Intake Zone
        DRIVE_TO_INTAKE,
        WAIT_ARRIVE_INTAKE,
        
        // Phase 4: Intake - drive forward while intaking to sweep up balls
        INTAKE_FORWARD,         // Start forward sweep with intake running
        WAIT_INTAKE_COMPLETE,   // Wait for 3 balls OR reached X=10 OR timeout
        
        // Phase 5: Drive back to Launch Position
        DRIVE_TO_LAUNCH,
        WAIT_ARRIVE_LAUNCH,
        
        // Phase 6: Second Shoot (3 newly intaked balls)
        WAIT_TURRET_AIM_2,
        SHOOT_SEQUENCE_2,
        WAIT_SHOOT_COMPLETE_2,
        
        // Phase 7: Park
        DRIVE_TO_PARK,
        WAIT_ARRIVE_PARK,
        
        // Done
        COMPLETE,
        
        // Test states
        PHASE_COMPLETE,  // Stopped at selected phase
        PAUSED           // Waiting for manual advance
    }
    
    private AutoState currentState = AutoState.INIT;
    private AutoState pausedFromState = null;  // Track where we paused from
    
    // ========== PATHS ==========
    private Path pathToIntake;              // START_POSE → INTAKE_POSE
    private PathChain pathIntakeForward;    // INTAKE_POSE → INTAKE_FORWARD_POSE (slow sweep - PathChain for setMaxPower)
    private Path pathToLaunch;              // INTAKE_FORWARD_POSE → START_POSE
    private Path pathToPark;                // START_POSE → PARK_POSE
    
    // ========== LIFECYCLE METHODS ==========
    
    @Override
    public void init() {
        robot = new RobotSystem();
        robot.init(hardwareMap, START_POSE);
        follower = robot.localization.getFollower();
        
        if (robot.limelight != null) {
            robot.limelight.setAlliance(ALLIANCE);
        }
        
        actionTimer = new Timer();
        
        if (robot.servos != null && robot.servos.hood_5 != null) {
            robot.servos.hood_5.setPosition(HOOD_POSITION);
        }
        
        if (robot.autoIntake != null) {
            robot.autoIntake.setBallsLoaded(RobotConstants.Auto.BALLS_PER_CYCLE); // Tell system balls are pre-loaded
            robot.autoIntake.setLaunchMode(); // Now positions correctly based on ballsLoaded
        }
        
        buildPaths();
        
        telemetry.addLine("=== TEST MODE: Blue Far ===");
        telemetry.addLine("Use D-Pad Up/Down to select stop phase");
        telemetry.addLine("Press A to toggle Manual Advance");
        telemetry.update();
    }
    
    @Override
    public void init_loop() {
        // Phase selection with D-Pad
        if (gamepad1.dpad_up && !prevDpadUp) {
            int idx = stopAfterPhase.ordinal();
            if (idx < TestPhase.values().length - 1) {
                stopAfterPhase = TestPhase.values()[idx + 1];
            }
        }
        prevDpadUp = gamepad1.dpad_up;
        
        if (gamepad1.dpad_down && !prevDpadDown) {
            int idx = stopAfterPhase.ordinal();
            if (idx > 0) {
                stopAfterPhase = TestPhase.values()[idx - 1];
            }
        }
        prevDpadDown = gamepad1.dpad_down;
        
        // Toggle manual advance mode
        boolean aPressed = gamepad1.a || gamepad1.cross;
        if (aPressed && !prevA) {
            manualAdvanceMode = !manualAdvanceMode;
        }
        prevA = aPressed;
        
        // Update limelight
        if (robot.limelight != null) {
            robot.limelight.update();
        }
        
        // Display configuration
        telemetry.addLine("══════════════════════════════════════");
        telemetry.addLine("   TEST MODE: Blue Far - Phase Select");
        telemetry.addLine("══════════════════════════════════════");
        telemetry.addLine();
        
        telemetry.addLine("─── STOP AFTER PHASE (D-Pad Up/Down) ───");
        for (TestPhase phase : TestPhase.values()) {
            String marker = (phase == stopAfterPhase) ? ">> " : "   ";
            telemetry.addLine(marker + phase.displayName);
        }
        
        telemetry.addLine();
        telemetry.addData("Manual Advance (A)", manualAdvanceMode ? "ON - Step through states" : "OFF - Auto run");
        
        telemetry.addLine();
        telemetry.addLine("─── ROBOT STATUS ───");
        if (robot.limelight != null && robot.limelight.isConnected()) {
            telemetry.addData("Limelight", robot.limelight.hasAllianceTarget() ? 
                "✓ Tag visible (tx: " + String.format("%.1f", robot.limelight.getTargetX()) + "°)" : 
                "Connected (no tag)");
        } else {
            telemetry.addData("Limelight", "Not connected");
        }
        telemetry.addData("Initial Turret Offset", "%.1f° (AIM HERE before START)", INITIAL_TURRET_ANGLE);
        if (robot.autoTurret != null) {
            telemetry.addData("Turret Encoder", "%d ticks", 
                robot.motors != null && robot.motors.turret != null ? 
                robot.motors.turret.getCurrentPosition() : 0);
        }
        telemetry.addData("Shooter Velocity", "%.0f", SHOOTER_VELOCITY);
        
        telemetry.addLine();
        telemetry.addLine("*** MANUAL AIM: Point turret at goal ***");
        telemetry.addLine(">>> Then press START to begin <<<");
        telemetry.update();
    }
    
    @Override
    public void start() {
        actionTimer.resetTimer();
        currentState = AutoState.INIT;
        waitingForAdvance = false;
        
        if (robot.led != null) {
            robot.led.setLaunchModeColor();
        }
        
        // Start flywheel
        if (robot.shooting != null) {
            robot.shooting.startShooter(SHOOTER_VELOCITY);
        }
        
        // Reset gamepad tracking for run loop
        prevA = gamepad1.a || gamepad1.cross;
    }
    
    @Override
    public void loop() {
        // Update all robot systems
        robot.updateAll();
        if (follower != null) follower.update();
        
        // Handle manual advance
        if (currentState == AutoState.PAUSED) {
            boolean aPressed = gamepad1.a || gamepad1.cross;
            if (aPressed && !prevA) {
                // Advance to next state
                currentState = getNextState(pausedFromState);
                waitingForAdvance = false;
                actionTimer.resetTimer();
            }
            prevA = aPressed;
        } else {
            prevA = gamepad1.a || gamepad1.cross;
            
            // Run state machine
            runStateMachine();
        }
        
        // Telemetry
        updateTelemetry();
    }
    
    @Override
    public void stop() {
        if (robot != null) {
            // Save turret offset for TeleOp (Auto → TeleOp persistence)
            if (robot.autoTurret != null) {
                RobotMotors.saveTurretOffset(
                    robot.autoTurret.getManualOffsetTicks(),
                    robot.autoTurret.getInitialOffsetDegrees()
                );
            }
            if (robot.motors != null) {
                robot.motors.saveTurretPosition();
            }
            robot.stopAll();
        }
    }
    
    // ========== PATH BUILDING ==========
    
    private void buildPaths() {
        // Step 3: Drive from start to intake zone
        pathToIntake = new Path(new BezierLine(START_POSE, INTAKE_POSE));
        pathToIntake.setLinearHeadingInterpolation(START_POSE.getHeading(), INTAKE_POSE.getHeading());
        
        // Step 5: Forward sweep to pick up balls (INTAKE_POSE → X=10)
        // Built as PathChain (same pattern as old Auto_Blue_Far where setMaxPower worked)
        pathIntakeForward = follower.pathBuilder()
                .addPath(new BezierLine(INTAKE_POSE, INTAKE_FORWARD_POSE))
                .setConstantHeadingInterpolation(INTAKE_POSE.getHeading())
                .build();
        
        // Step 6: Return from intake area back to launch position
        pathToLaunch = new Path(new BezierLine(INTAKE_FORWARD_POSE, START_POSE));
        pathToLaunch.setLinearHeadingInterpolation(INTAKE_FORWARD_POSE.getHeading(), START_POSE.getHeading());
        
        // Step 8: Drive to park position
        pathToPark = new Path(new BezierLine(START_POSE, PARK_POSE));
        pathToPark.setLinearHeadingInterpolation(START_POSE.getHeading(), PARK_POSE.getHeading());
    }
    
    // ========== STATE MACHINE ==========
    
    private void runStateMachine() {
        double elapsed = actionTimer.getElapsedTimeSeconds();
        
        switch (currentState) {
            
            // ==================== PHASE 1: INIT TURRET (Offset-based) ====================
            // User manually aimed turret before init. Encoder was reset to 0.
            // Turret stays exactly where user aimed — NO motor movement.
            // Auto-tracking is NOT enabled yet (robot is stationary, manual aim is perfect).
            // Auto-tracking activates later when robot starts driving to intake.
            
            case INIT:
                // Store the initial offset for later use
                robot.autoTurret.setInitialOffset(INITIAL_TURRET_ANGLE);
                
                // Hold turret at current position using PID (prevents drift during driving)
                // This keeps the turret locked at encoder 0 (= user's manual aim)
                robot.autoTurret.holdCurrentPosition();
                
                // Check if we should stop after this phase
                if (stopAfterPhase == TestPhase.INIT_TURRET) {
                    currentState = AutoState.PHASE_COMPLETE;
                } else {
                    // Skip Limelight calibration — user's manual aim is trusted.
                    // Go straight to first shooting phase.
                    transitionTo(AutoState.WAIT_SHOOTER_READY_1);
                }
                break;
            
            // ==================== LIMELIGHT CALIBRATION (ONE-TIME) ====================
            // Reserved for future use. Currently skipped — manual aim is used for first shot.
            // Auto-tracking + Limelight correction activates when robot starts moving.
                
            case LIMELIGHT_CALIBRATE:
                // Currently unused — transition straight through
                transitionTo(AutoState.WAIT_SHOOTER_READY_1);
                break;
                
            // ==================== PHASE 2: FIRST SHOOTING ====================
                
            case WAIT_SHOOTER_READY_1:
                if (robot.shooting.isShooterAtTargetVelocity() || 
                    elapsed > RobotConstants.Auto.SHOOTER_SPINUP_SECONDS) {
                    // Skip turret aim — turret holds position, no aiming needed
                    transitionTo(AutoState.SHOOT_SEQUENCE_1);
                }
                break;
                
            case WAIT_TURRET_AIM_1:
                // SKIPPED: turret aim not needed (heading & turret don't change)
                transitionTo(AutoState.SHOOT_SEQUENCE_1);
                break;
                
            case SHOOT_SEQUENCE_1:
                robot.autoShoot.start(RobotConstants.Auto.BALLS_PER_CYCLE);
                transitionTo(AutoState.WAIT_SHOOT_COMPLETE_1);
                break;
                
            case WAIT_SHOOT_COMPLETE_1:
                if (!robot.autoShoot.isInProgress()) {
                    if (stopAfterPhase == TestPhase.FIRST_SHOOT) {
                        currentState = AutoState.PHASE_COMPLETE;
                    } else {
                        transitionTo(AutoState.DRIVE_TO_INTAKE);
                    }
                } else if (elapsed > RobotConstants.Auto.SHOOT_SEQUENCE_TIMEOUT) {
                    robot.autoShoot.stop();
                    if (stopAfterPhase == TestPhase.FIRST_SHOOT) {
                        currentState = AutoState.PHASE_COMPLETE;
                    } else {
                        transitionTo(AutoState.DRIVE_TO_INTAKE);
                    }
                }
                break;
                
            // ==================== PHASE 3: DRIVE TO INTAKE ZONE ====================
                
            case DRIVE_TO_INTAKE:
                // Turret stays at manual aim — no auto-tracking during auto.
                // Robot returns to same launch position, so angle stays the same.
                
                // Switch to intake mode and start intake motor while driving
                if (robot.autoIntake != null) {
                    robot.autoIntake.setIntakeMode();
                    robot.autoIntake.resetBallCount();  // Reset count for new intake cycle
                    robot.autoIntake.startIntakeMotor();
                }
                if (robot.led != null) {
                    robot.led.setIntakeModeColor();
                }
                follower.followPath(pathToIntake);
                transitionTo(AutoState.WAIT_ARRIVE_INTAKE);
                break;
                
            case WAIT_ARRIVE_INTAKE:
                if (!follower.isBusy() || elapsed > RobotConstants.Auto.PATH_TIMEOUT) {
                    if (stopAfterPhase == TestPhase.DRIVE_INTAKE) {
                        if (robot.autoIntake != null) robot.autoIntake.stopIntakeMotor();
                        currentState = AutoState.PHASE_COMPLETE;
                    } else {
                        transitionTo(AutoState.INTAKE_FORWARD);
                    }
                }
                break;
                
            // ==================== PHASE 4: INTAKE FORWARD SWEEP ====================
            // Robot drives forward from INTAKE_POSE toward X=10 while intake runs
            // Stops when: 3 balls loaded OR reached forward position OR timeout
                
            case INTAKE_FORWARD:
                // Intake motor should already be running from DRIVE_TO_INTAKE
                // SLOW DOWN: setMaxPower BEFORE followPath (same pattern as old Auto_Blue_Far)
                follower.setMaxPower(RobotConstants.Auto.INTAKE_FORWARD_SPEED);
                follower.followPath(pathIntakeForward);
                transitionTo(AutoState.WAIT_INTAKE_COMPLETE);
                break;
                
            case WAIT_INTAKE_COMPLETE:
                // ENFORCE slow speed every loop - follower.update() may reset it
                follower.setMaxPower(RobotConstants.Auto.INTAKE_FORWARD_SPEED);
                
                // Check if we've collected all 3 balls
                boolean ballsFull = (robot.autoIntake != null && 
                    robot.autoIntake.getBallsLoaded() >= RobotConstants.Auto.BALLS_PER_CYCLE);
                boolean pathDone = !follower.isBusy();
                boolean timedOut = elapsed > RobotConstants.Auto.INTAKE_FORWARD_TIMEOUT;
                
                if (ballsFull || pathDone || timedOut) {
                    // Restore full speed for next path
                    follower.setMaxPower(1.0);
                    // Stop intake and switch to launch mode
                    if (robot.autoIntake != null) {
                        robot.autoIntake.stopIntakeMotor();
                        robot.autoIntake.setLaunchMode();
                    }
                    if (robot.led != null) {
                        robot.led.setLaunchModeColor();
                    }
                    if (stopAfterPhase == TestPhase.INTAKE) {
                        currentState = AutoState.PHASE_COMPLETE;
                    } else {
                        transitionTo(AutoState.DRIVE_TO_LAUNCH);
                    }
                }
                break;
                
            // ==================== PHASE 5: DRIVE BACK TO LAUNCH ====================
                
            case DRIVE_TO_LAUNCH:
                follower.followPath(pathToLaunch);
                transitionTo(AutoState.WAIT_ARRIVE_LAUNCH);
                break;
                
            case WAIT_ARRIVE_LAUNCH:
                if (!follower.isBusy() || elapsed > RobotConstants.Auto.PATH_TIMEOUT) {
                    if (stopAfterPhase == TestPhase.DRIVE_LAUNCH) {
                        currentState = AutoState.PHASE_COMPLETE;
                    } else {
                        // Skip turret aim — turret holds position, no aiming needed
                        transitionTo(AutoState.SHOOT_SEQUENCE_2);
                    }
                }
                break;
                
            // ==================== PHASE 6: SECOND SHOOTING ====================
                
            case WAIT_TURRET_AIM_2:
                // SKIPPED: turret aim not needed (heading & turret don't change)
                transitionTo(AutoState.SHOOT_SEQUENCE_2);
                break;
                
            case SHOOT_SEQUENCE_2:
                robot.autoShoot.start(RobotConstants.Auto.BALLS_PER_CYCLE);
                transitionTo(AutoState.WAIT_SHOOT_COMPLETE_2);
                break;
                
            case WAIT_SHOOT_COMPLETE_2:
                if (!robot.autoShoot.isInProgress()) {
                    robot.shooting.stopShooter();
                    if (stopAfterPhase == TestPhase.SECOND_SHOOT) {
                        currentState = AutoState.PHASE_COMPLETE;
                    } else {
                        transitionTo(AutoState.DRIVE_TO_PARK);
                    }
                } else if (elapsed > RobotConstants.Auto.SHOOT_SEQUENCE_TIMEOUT) {
                    robot.autoShoot.stop();
                    robot.shooting.stopShooter();
                    if (stopAfterPhase == TestPhase.SECOND_SHOOT) {
                        currentState = AutoState.PHASE_COMPLETE;
                    } else {
                        transitionTo(AutoState.DRIVE_TO_PARK);
                    }
                }
                break;
                
            // ==================== PHASE 7: PARK ====================
                
            case DRIVE_TO_PARK:
                follower.followPath(pathToPark);
                transitionTo(AutoState.WAIT_ARRIVE_PARK);
                break;
                
            case WAIT_ARRIVE_PARK:
                if (!follower.isBusy() || elapsed > RobotConstants.Auto.PARK_TIMEOUT) {
                    currentState = AutoState.COMPLETE;
                }
                break;
                
            // ==================== COMPLETE ====================
                
            case COMPLETE:
                robot.stopAll();
                break;
                
            case PHASE_COMPLETE:
                // Stopped at selected phase - keep systems running for observation
                // Don't call stopAll() so you can see turret tracking, etc.
                break;
                
            case PAUSED:
                // Waiting for manual advance - handled in loop()
                break;
        }
    }
    
    /**
     * Transition to next state, with manual advance pause if enabled
     */
    private void transitionTo(AutoState nextState) {
        if (manualAdvanceMode) {
            pausedFromState = currentState;
            currentState = AutoState.PAUSED;
            waitingForAdvance = true;
        } else {
            currentState = nextState;
            actionTimer.resetTimer();
        }
    }
    
    /**
     * Get the next state after a paused state
     */
    private AutoState getNextState(AutoState fromState) {
        AutoState[] states = AutoState.values();
        int idx = fromState.ordinal();
        if (idx < states.length - 3) { // -3 to skip COMPLETE, PHASE_COMPLETE, PAUSED
            return states[idx + 1];
        }
        return AutoState.COMPLETE;
    }
    
    // ========== TELEMETRY ==========
    
    private void updateTelemetry() {
        telemetry.addLine("══════════════════════════════════════");
        telemetry.addLine("   TEST: Blue Far - RUNNING");
        telemetry.addLine("══════════════════════════════════════");
        
        // Current state with highlight
        telemetry.addLine();
        if (currentState == AutoState.PAUSED) {
            telemetry.addLine(">>> PAUSED - Press A to advance <<<");
            telemetry.addData("Paused at", pausedFromState.toString());
            telemetry.addData("Next state", getNextState(pausedFromState).toString());
        } else if (currentState == AutoState.PHASE_COMPLETE) {
            telemetry.addLine(">>> PHASE COMPLETE <<<");
            telemetry.addData("Stopped after", stopAfterPhase.displayName);
            telemetry.addLine("Systems still running for observation");
        } else {
            telemetry.addData("STATE", ">> " + currentState.toString() + " <<");
        }
        
        telemetry.addData("Timer", "%.2f s", actionTimer.getElapsedTimeSeconds());
        
        // Phase progress
        telemetry.addLine();
        telemetry.addLine("─── PHASE PROGRESS ───");
        telemetry.addData("Stop After", stopAfterPhase.displayName);
        telemetry.addData("Manual Mode", manualAdvanceMode ? "ON (step-by-step)" : "OFF");
        
        // Position
        telemetry.addLine();
        telemetry.addLine("─── ROBOT STATUS ───");
        telemetry.addData("Position", "(%.1f, %.1f) @ %.1f°",
            robot.localization.getX(), 
            robot.localization.getY(),
            robot.localization.getHeadingDegrees());
        
        // Turret
        if (robot.autoTurret != null) {
            telemetry.addData("Turret", "%.1f° %s %s",
                robot.autoTurret.getCurrentAngleDegrees(),
                robot.autoTurret.isOnTarget() ? "✓ ON TARGET" : "tracking",
                robot.autoTurret.isAnyModeEnabled() ? "(AUTO)" : "(OFF)");
        }
        
        // Limelight aim assist
        if (robot.limelight != null && robot.limelight.isConnected()) {
            if (robot.limelight.hasAllianceTarget()) {
                double tx = robot.limelight.getTargetX();
                telemetry.addData("Limelight Aim", "tx=%.1f° %s", tx,
                    Math.abs(tx) < RobotConstants.Auto.LIMELIGHT_ON_TARGET_DEGREES ? 
                    "✓ LOCKED" : "correcting");
            } else {
                telemetry.addData("Limelight Aim", "No tag visible");
            }
        }
        
        // Shooter
        telemetry.addData("Shooter", "%.0f / %.0f %s",
            robot.shooting.getCurrentVelocity(),
            SHOOTER_VELOCITY,
            robot.shooting.isShooterAtTargetVelocity() ? "✓ READY" : "spinning up");
        
        // Auto-shoot
        if (robot.autoShoot.isInProgress()) {
            telemetry.addData("Auto-Shoot", "ACTIVE: %d/%d balls",
                robot.autoShoot.getBallsShot(),
                robot.autoShoot.getBallsToShoot());
        }
        
        // Mode & Ball Count
        if (robot.autoIntake != null) {
            telemetry.addData("Mode", robot.autoIntake.isIntakeMode() ? "🔵 INTAKE" : "🔴 LAUNCH");
            telemetry.addData("Balls Loaded", "%d / %d", 
                robot.autoIntake.getBallsLoaded(), RobotConstants.Auto.BALLS_PER_CYCLE);
            if (robot.autoIntake.getBallsLoaded() > 0) {
                telemetry.addData("Ball Colors", robot.autoIntake.getBallColorSummary());
            }
        }
        
        // Path status
        telemetry.addData("Path", follower.isBusy() ? "MOVING" : "IDLE");
        
        telemetry.update();
    }
}
