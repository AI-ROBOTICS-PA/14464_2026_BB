package org.firstinspires.ftc.teamcode.decode14464.auto;

import com.pedropathing.follower.Follower;
import com.pedropathing.geometry.BezierLine;
import com.pedropathing.geometry.Pose;
import com.pedropathing.paths.Path;
import com.pedropathing.util.Timer;
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;

import org.firstinspires.ftc.teamcode.decode14464.systems.LimelightManager;
import org.firstinspires.ftc.teamcode.decode14464.systems.RobotConstants;
import org.firstinspires.ftc.teamcode.decode14464.systems.RobotSystem;

/**
 * TEST VERSION: Auto Blue Far - Phase-by-Phase Testing
 * 
 * DURING INIT:
 * - D-Pad Up/Down: Select which phase to STOP after
 * - A/Cross: Toggle "Manual Advance" mode (step through states one by one)
 * 
 * PHASES:
 * 1. INIT_TURRET - Just move turret to initial position
 * 2. FIRST_SHOOT - Turret + Flywheel + First 3-ball shoot
 * 3. DRIVE_INTAKE - Drive to intake zone
 * 4. INTAKE - Intake balls
 * 5. DRIVE_LAUNCH - Return to launch zone
 * 6. SECOND_SHOOT - Final 3-ball shoot
 * 7. COMPLETE - Full auto (no stop)
 * 
 * DURING RUN (if Manual Advance ON):
 * - Press A/Cross to advance to next state
 * 
 * This allows you to test each phase independently!
 */
@Autonomous(name = "TEST Blue Far (Phase Select)", group = "DECODE Test")
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
        INTAKE("4. Intake Balls"),
        DRIVE_LAUNCH("5. Drive to Launch"),
        SECOND_SHOOT("6. Second Shooting"),
        COMPLETE("7. Full Auto (no stop)");
        
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
    private static final Pose START_POSE = new Pose(40, 0, Math.toRadians(90));
    private static final Pose INTAKE_POSE = new Pose(36, 36, Math.toRadians(90));
    
    // ========== SHOOTING PARAMETERS ==========
    private static final double SHOOTER_VELOCITY = RobotConstants.Shooter.HIGH_VELOCITY;
    private static final double HOOD_POSITION = RobotConstants.Hood.LONG_RANGE_POSITION;
    private static final LimelightManager.Alliance ALLIANCE = LimelightManager.Alliance.BLUE;
    private static final double INITIAL_TURRET_ANGLE = RobotConstants.Auto.BLUE_FAR_INITIAL_TURRET_ANGLE;
    
    // ========== STATE MACHINE ==========
    private enum AutoState {
        // Phase 1: Init Turret
        INIT,
        MOVE_TURRET_INITIAL,
        WAIT_TURRET_INITIAL,
        
        // Phase 2: First Shoot
        WAIT_SHOOTER_READY_1,
        WAIT_TURRET_AIM_1,
        SHOOT_SEQUENCE_1,
        WAIT_SHOOT_COMPLETE_1,
        
        // Phase 3: Drive to Intake
        DRIVE_TO_INTAKE,
        WAIT_ARRIVE_INTAKE,
        
        // Phase 4: Intake
        INTAKING,
        
        // Phase 5: Drive to Launch
        DRIVE_TO_LAUNCH,
        WAIT_ARRIVE_LAUNCH,
        
        // Phase 6: Second Shoot
        WAIT_TURRET_AIM_2,
        SHOOT_SEQUENCE_2,
        WAIT_SHOOT_COMPLETE_2,
        
        // Phase 7: Complete
        COMPLETE,
        
        // Test states
        PHASE_COMPLETE,  // Stopped at selected phase
        PAUSED           // Waiting for manual advance
    }
    
    private AutoState currentState = AutoState.INIT;
    private AutoState pausedFromState = null;  // Track where we paused from
    
    // ========== PATHS ==========
    private Path pathToIntake;
    private Path pathToLaunch;
    
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
            robot.autoIntake.setLaunchMode();
            if (robot.servos != null && robot.servos.turret_1 != null) {
                robot.servos.turret_1.setPosition(
                    RobotConstants.TurretServo.LAUNCH_POSITIONS[RobotConstants.Auto.BALLS_PER_CYCLE - 1]
                );
            }
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
        telemetry.addData("Initial Turret", "%.1f°", INITIAL_TURRET_ANGLE);
        telemetry.addData("Shooter Velocity", "%.0f", SHOOTER_VELOCITY);
        
        telemetry.addLine();
        telemetry.addLine(">>> Press START to begin <<<");
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
        robot.shooting.startShooter(SHOOTER_VELOCITY);
        
        // Reset gamepad tracking for run loop
        prevA = gamepad1.a || gamepad1.cross;
    }
    
    @Override
    public void loop() {
        // Update all robot systems
        robot.updateAll();
        follower.update();
        
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
            robot.stopAll();
        }
    }
    
    // ========== PATH BUILDING ==========
    
    private void buildPaths() {
        pathToIntake = new Path(new BezierLine(START_POSE, INTAKE_POSE));
        pathToIntake.setLinearHeadingInterpolation(START_POSE.getHeading(), INTAKE_POSE.getHeading());
        
        pathToLaunch = new Path(new BezierLine(INTAKE_POSE, START_POSE));
        pathToLaunch.setLinearHeadingInterpolation(INTAKE_POSE.getHeading(), START_POSE.getHeading());
    }
    
    // ========== STATE MACHINE ==========
    
    private void runStateMachine() {
        double elapsed = actionTimer.getElapsedTimeSeconds();
        
        switch (currentState) {
            
            // ==================== PHASE 1: INIT TURRET ====================
            
            case INIT:
                robot.autoTurret.moveToAngle(INITIAL_TURRET_ANGLE);
                transitionTo(AutoState.MOVE_TURRET_INITIAL);
                break;
                
            case MOVE_TURRET_INITIAL:
                if (robot.autoTurret.isAtTargetPosition() || 
                    elapsed > RobotConstants.Auto.INITIAL_TURRET_MOVE_TIMEOUT) {
                    robot.autoTurret.resumeFromReset();
                    robot.autoTurret.enableCombinedGoalTracking();
                    transitionTo(AutoState.WAIT_TURRET_INITIAL);
                }
                break;
                
            case WAIT_TURRET_INITIAL:
                if (elapsed > RobotConstants.Auto.TURRET_SETTLING_TIME) {
                    // Check if we should stop after this phase
                    if (stopAfterPhase == TestPhase.INIT_TURRET) {
                        currentState = AutoState.PHASE_COMPLETE;
                    } else {
                        transitionTo(AutoState.WAIT_SHOOTER_READY_1);
                    }
                }
                break;
            
            // ==================== PHASE 2: FIRST SHOOTING ====================
                
            case WAIT_SHOOTER_READY_1:
                if (robot.shooting.isShooterAtTargetVelocity() || 
                    elapsed > RobotConstants.Auto.SHOOTER_SPINUP_SECONDS) {
                    transitionTo(AutoState.WAIT_TURRET_AIM_1);
                }
                break;
                
            case WAIT_TURRET_AIM_1:
                if (robot.autoTurret.isOnTarget() || 
                    elapsed > RobotConstants.Auto.TURRET_AIM_TIMEOUT) {
                    transitionTo(AutoState.SHOOT_SEQUENCE_1);
                }
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
                
            // ==================== PHASE 3: DRIVE TO INTAKE ====================
                
            case DRIVE_TO_INTAKE:
                if (robot.autoIntake != null) {
                    robot.autoIntake.setIntakeMode();
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
                        transitionTo(AutoState.INTAKING);
                    }
                }
                break;
                
            // ==================== PHASE 4: INTAKE ====================
                
            case INTAKING:
                if (elapsed > RobotConstants.Auto.INTAKE_DURATION) {
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
                
            // ==================== PHASE 5: DRIVE TO LAUNCH ====================
                
            case DRIVE_TO_LAUNCH:
                follower.followPath(pathToLaunch);
                transitionTo(AutoState.WAIT_ARRIVE_LAUNCH);
                break;
                
            case WAIT_ARRIVE_LAUNCH:
                if (!follower.isBusy() || elapsed > RobotConstants.Auto.PATH_TIMEOUT) {
                    if (stopAfterPhase == TestPhase.DRIVE_LAUNCH) {
                        currentState = AutoState.PHASE_COMPLETE;
                    } else {
                        transitionTo(AutoState.WAIT_TURRET_AIM_2);
                    }
                }
                break;
                
            // ==================== PHASE 6: SECOND SHOOTING ====================
                
            case WAIT_TURRET_AIM_2:
                if (robot.autoTurret.isOnTarget() || 
                    elapsed > RobotConstants.Auto.TURRET_AIM_TIMEOUT) {
                    transitionTo(AutoState.SHOOT_SEQUENCE_2);
                }
                break;
                
            case SHOOT_SEQUENCE_2:
                robot.autoShoot.start(RobotConstants.Auto.BALLS_PER_CYCLE);
                transitionTo(AutoState.WAIT_SHOOT_COMPLETE_2);
                break;
                
            case WAIT_SHOOT_COMPLETE_2:
                if (!robot.autoShoot.isInProgress()) {
                    robot.shooting.stopShooter();
                    currentState = AutoState.COMPLETE;
                } else if (elapsed > RobotConstants.Auto.SHOOT_SEQUENCE_TIMEOUT) {
                    robot.autoShoot.stop();
                    robot.shooting.stopShooter();
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
        
        // Mode
        if (robot.autoIntake != null) {
            telemetry.addData("Mode", robot.autoIntake.isIntakeMode() ? "🔵 INTAKE" : "🔴 LAUNCH");
        }
        
        // Path status
        telemetry.addData("Path", follower.isBusy() ? "MOVING" : "IDLE");
        
        // Limelight
        if (robot.limelight != null && robot.limelight.isConnected()) {
            telemetry.addData("Limelight", robot.limelight.hasAllianceTarget() ? 
                "Tag visible (tx: " + String.format("%.1f", robot.limelight.getTargetX()) + "°)" : 
                "No tag");
        }
        
        telemetry.update();
    }
}
