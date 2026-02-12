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
 * AUTO: Red Alliance - Far Zone (Phase Select)
 * 
 * SEQUENCE (8 steps):
 *   Step 0: Begin on initial pose (95, 9) heading 90°
 *   Step 1: Load turret offset (user pre-aimed)
 *   Step 2: Auto-shoot 3 preloaded balls
 *   Step 3: Drive to intake position (131, 36)
 *   Step 4: Start auto intake motor
 *   Step 5: Drive forward while intaking to sweep up 3 balls
 *   Step 6: Return to start/launch position (95, 9)
 *   Step 7: Auto-shoot 3 newly intaked balls
 *   Step 8: Drive to park position
 * 
 * DURING INIT:
 * - D-Pad Up/Down: Select which phase to STOP after
 * - A/Cross: Toggle "Manual Advance" mode (step through states one by one)
 * 
 * PHASES:
 * 1. INIT_TURRET - Load turret offset (no motor movement)
 * 2. FIRST_SHOOT - Flywheel spinup + auto-shoot 3 preloaded balls
 * 3. DRIVE_INTAKE - Drive to intake zone
 * 4. INTAKE - Forward sweep while intaking (stops at 3 balls)
 * 5. DRIVE_LAUNCH - Return to launch position
 * 6. SECOND_SHOOT - Auto-shoot 3 intaked balls
 * 7. PARK - Drive to park position
 * 8. COMPLETE - Full auto (no stop)
 */
@Autonomous(name = "Auto-Red Far (Phase Select)", group = "DECODE Auto")
public class Auto_RedFar extends OpMode {

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
    // Robot is 18x15 inches. Pose = center. Y offset = 18/2 = 9 (back against bottom wall)
    private static final Pose START_POSE = new Pose(95, 9, Math.toRadians(90));
    private static final Pose INTAKE_POSE = new Pose(131, 36, Math.toRadians(90));
    // Forward sweep: heading 90° = +Y direction. Drive forward ~26" while intaking.
    private static final Pose INTAKE_FORWARD_POSE = new Pose(131, 62, Math.toRadians(90)); // TUNE THIS
    // Park position after second shoot (TUNE THIS for your field)
    private static final Pose PARK_POSE = new Pose(120, 24, Math.toRadians(90)); // TUNE THIS
    
    // ========== SHOOTING PARAMETERS ==========
    private static final double SHOOTER_VELOCITY = RobotConstants.Shooter.HIGH_VELOCITY;
    private static final double HOOD_POSITION = RobotConstants.Hood.LONG_RANGE_POSITION;
    private static final LimelightManager.Alliance ALLIANCE = LimelightManager.Alliance.RED;
    private static final double INITIAL_TURRET_ANGLE = RobotConstants.Auto.RED_FAR_INITIAL_TURRET_ANGLE;
    
    // ========== STATE MACHINE ==========
    private enum AutoState {
        // Phase 1: Init Turret (offset-based, no motor movement)
        INIT,
        
        // Phase 2: First Shoot (preloaded 3 balls)
        LIMELIGHT_CALIBRATE,
        WAIT_SHOOTER_READY_1,
        WAIT_TURRET_AIM_1,
        SHOOT_SEQUENCE_1,
        WAIT_SHOOT_COMPLETE_1,
        
        // Phase 3: Drive to Intake Zone
        DRIVE_TO_INTAKE,
        WAIT_ARRIVE_INTAKE,
        
        // Phase 4: Intake - drive forward while intaking to sweep up balls
        INTAKE_FORWARD,
        WAIT_INTAKE_COMPLETE,
        
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
        PHASE_COMPLETE,
        PAUSED
    }
    
    private AutoState currentState = AutoState.INIT;
    private AutoState pausedFromState = null;
    
    // ========== PATHS ==========
    private Path pathToIntake;
    private PathChain pathIntakeForward;
    private Path pathToLaunch;
    private Path pathToPark;
    
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
            robot.autoIntake.setBallsLoaded(RobotConstants.Auto.BALLS_PER_CYCLE);
            robot.autoIntake.setLaunchMode();
        }
        
        buildPaths();
        
        telemetry.addLine("=== Red Far - Phase Select ===");
        telemetry.addLine("Use D-Pad Up/Down to select stop phase");
        telemetry.addLine("Press A to toggle Manual Advance");
        telemetry.update();
    }
    
    @Override
    public void init_loop() {
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
        
        boolean aPressed = gamepad1.a || gamepad1.cross;
        if (aPressed && !prevA) {
            manualAdvanceMode = !manualAdvanceMode;
        }
        prevA = aPressed;
        
        if (robot.limelight != null) {
            robot.limelight.update();
        }
        
        telemetry.addLine("══════════════════════════════════════");
        telemetry.addLine("   Red Far - Phase Select");
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
                "Tag visible (tx: " + String.format("%.1f", robot.limelight.getTargetX()) + ")" : 
                "Connected (no tag)");
        } else {
            telemetry.addData("Limelight", "Not connected");
        }
        telemetry.addData("Initial Turret Offset", "%.1f (AIM HERE before START)", INITIAL_TURRET_ANGLE);
        if (robot.motors != null && robot.motors.turret != null) {
            telemetry.addData("Turret Encoder", "%d ticks", robot.motors.turret.getCurrentPosition());
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
        
        if (robot.shooting != null) {
            robot.shooting.startShooter(SHOOTER_VELOCITY);
        }
        
        prevA = gamepad1.a || gamepad1.cross;
    }
    
    @Override
    public void loop() {
        robot.updateAll();
        if (follower != null) follower.update();
        
        if (currentState == AutoState.PAUSED) {
            boolean aPressed = gamepad1.a || gamepad1.cross;
            if (aPressed && !prevA) {
                currentState = getNextState(pausedFromState);
                waitingForAdvance = false;
                actionTimer.resetTimer();
            }
            prevA = aPressed;
        } else {
            prevA = gamepad1.a || gamepad1.cross;
            runStateMachine();
        }
        
        updateTelemetry();
    }
    
    @Override
    public void stop() {
        if (robot != null) {
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
        pathToIntake = new Path(new BezierLine(START_POSE, INTAKE_POSE));
        pathToIntake.setLinearHeadingInterpolation(START_POSE.getHeading(), INTAKE_POSE.getHeading());
        
        pathIntakeForward = follower.pathBuilder()
                .addPath(new BezierLine(INTAKE_POSE, INTAKE_FORWARD_POSE))
                .setConstantHeadingInterpolation(INTAKE_POSE.getHeading())
                .build();
        
        pathToLaunch = new Path(new BezierLine(INTAKE_FORWARD_POSE, START_POSE));
        pathToLaunch.setLinearHeadingInterpolation(INTAKE_FORWARD_POSE.getHeading(), START_POSE.getHeading());
        
        pathToPark = new Path(new BezierLine(START_POSE, PARK_POSE));
        pathToPark.setLinearHeadingInterpolation(START_POSE.getHeading(), PARK_POSE.getHeading());
    }
    
    // ========== STATE MACHINE ==========
    
    private void runStateMachine() {
        double elapsed = actionTimer.getElapsedTimeSeconds();
        
        switch (currentState) {
            
            case INIT:
                // Store offset, hold turret at manual aim using PID (prevents drift)
                robot.autoTurret.setInitialOffset(INITIAL_TURRET_ANGLE);
                robot.autoTurret.holdCurrentPosition();
                if (stopAfterPhase == TestPhase.INIT_TURRET) {
                    currentState = AutoState.PHASE_COMPLETE;
                } else {
                    transitionTo(AutoState.WAIT_SHOOTER_READY_1);
                }
                break;
            
            case LIMELIGHT_CALIBRATE:
                // Unused — fully manual aim for first shot
                transitionTo(AutoState.WAIT_SHOOTER_READY_1);
                break;
                
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
                
            case DRIVE_TO_INTAKE:
                // Turret stays at manual aim — no auto-tracking during auto.
                if (robot.autoIntake != null) {
                    robot.autoIntake.setIntakeMode();
                    robot.autoIntake.resetBallCount();
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
                
            case INTAKE_FORWARD:
                follower.setMaxPower(RobotConstants.Auto.INTAKE_FORWARD_SPEED);
                follower.followPath(pathIntakeForward);
                transitionTo(AutoState.WAIT_INTAKE_COMPLETE);
                break;
                
            case WAIT_INTAKE_COMPLETE:
                follower.setMaxPower(RobotConstants.Auto.INTAKE_FORWARD_SPEED);
                
                boolean ballsFull = (robot.autoIntake != null && 
                    robot.autoIntake.getBallsLoaded() >= RobotConstants.Auto.BALLS_PER_CYCLE);
                boolean pathDone = !follower.isBusy();
                boolean timedOut = elapsed > RobotConstants.Auto.INTAKE_FORWARD_TIMEOUT;
                
                if (ballsFull || pathDone || timedOut) {
                    follower.setMaxPower(1.0);
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
                
            case DRIVE_TO_PARK:
                follower.followPath(pathToPark);
                transitionTo(AutoState.WAIT_ARRIVE_PARK);
                break;
                
            case WAIT_ARRIVE_PARK:
                if (!follower.isBusy() || elapsed > RobotConstants.Auto.PARK_TIMEOUT) {
                    currentState = AutoState.COMPLETE;
                }
                break;
                
            case COMPLETE:
                robot.stopAll();
                break;
                
            case PHASE_COMPLETE:
                break;
                
            case PAUSED:
                break;
        }
    }
    
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
    
    private AutoState getNextState(AutoState fromState) {
        AutoState[] states = AutoState.values();
        int idx = fromState.ordinal();
        if (idx < states.length - 3) {
            return states[idx + 1];
        }
        return AutoState.COMPLETE;
    }
    
    // ========== TELEMETRY ==========
    
    private void updateTelemetry() {
        telemetry.addLine("══════════════════════════════════════");
        telemetry.addLine("   Red Far - RUNNING");
        telemetry.addLine("══════════════════════════════════════");
        
        telemetry.addLine();
        if (currentState == AutoState.PAUSED) {
            telemetry.addLine(">>> PAUSED - Press A to advance <<<");
            telemetry.addData("Paused at", pausedFromState.toString());
            telemetry.addData("Next state", getNextState(pausedFromState).toString());
        } else if (currentState == AutoState.PHASE_COMPLETE) {
            telemetry.addLine(">>> PHASE COMPLETE <<<");
            telemetry.addData("Stopped after", stopAfterPhase.displayName);
        } else {
            telemetry.addData("STATE", ">> " + currentState.toString() + " <<");
        }
        
        telemetry.addData("Timer", "%.2f s", actionTimer.getElapsedTimeSeconds());
        
        telemetry.addLine();
        telemetry.addData("Stop After", stopAfterPhase.displayName);
        telemetry.addData("Manual Mode", manualAdvanceMode ? "ON" : "OFF");
        
        telemetry.addLine();
        telemetry.addData("Position", "(%.1f, %.1f) @ %.1f",
            robot.localization.getX(), robot.localization.getY(), robot.localization.getHeadingDegrees());
        
        if (robot.autoTurret != null) {
            telemetry.addData("Turret", "%.1f (phys %.1f) %s",
                robot.autoTurret.getCurrentAngleDegrees(),
                robot.autoTurret.getPhysicalAngleDegrees(),
                robot.autoTurret.isOnTarget() ? "ON TARGET" : "tracking");
        }
        
        telemetry.addData("Shooter", "%.0f / %.0f %s",
            robot.shooting.getCurrentVelocity(), SHOOTER_VELOCITY,
            robot.shooting.isShooterAtTargetVelocity() ? "READY" : "spinning");
        
        if (robot.autoShoot.isInProgress()) {
            telemetry.addData("Auto-Shoot", "%d/%d balls",
                robot.autoShoot.getBallsShot(), robot.autoShoot.getBallsToShoot());
        }
        
        if (robot.autoIntake != null) {
            telemetry.addData("Mode", robot.autoIntake.isIntakeMode() ? "INTAKE" : "LAUNCH");
            telemetry.addData("Balls", "%d / %d", 
                robot.autoIntake.getBallsLoaded(), RobotConstants.Auto.BALLS_PER_CYCLE);
        }
        
        telemetry.addData("Path", follower.isBusy() ? "MOVING" : "IDLE");
        telemetry.update();
    }
}
