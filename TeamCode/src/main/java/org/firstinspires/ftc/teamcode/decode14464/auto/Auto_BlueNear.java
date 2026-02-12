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
 * Auto Blue Near - Phase-by-Phase Selection
 * 
 * SEQUENCE (13 steps):
 *   Step 0:  Begin on initial pose (12, 132) heading 180°
 *   Step 1:  Drive to launch position (60, 84)
 *   Step 2:  Auto-shoot 3 preloaded balls
 *   Step 3:  Drive to first intake position (36, 84)
 *   Step 4:  Start auto intake
 *   Step 5:  Forward sweep to X=10 to pick up 3 balls
 *   Step 6:  Return to launch position (60, 84)
 *   Step 7:  Auto-shoot 3 balls
 *   Step 8:  Drive to second intake position (36, 60)
 *   Step 9:  Start auto intake
 *   Step 10: Forward sweep to X=10 to pick up 3 balls
 *   Step 11: Return to launch position (60, 84)
 *   Step 12: Auto-shoot 3 balls
 *   Step 13: Drive to park position (72, 96)
 * 
 * DURING INIT:
 * - D-Pad Up/Down: Select which phase to STOP after
 * - A/Cross: Toggle "Manual Advance" mode (step through states one by one)
 * 
 * PHASES:
 *  1. DRIVE_TO_LAUNCH  - Drive from start to launch position
 *  2. FIRST_SHOOT      - Flywheel spinup + auto-shoot 3 preloaded balls
 *  3. DRIVE_INTAKE_1   - Drive to first intake zone (36, 84)
 *  4. INTAKE_1         - Forward sweep to X=10 while intaking
 *  5. DRIVE_LAUNCH_1   - Return to launch position
 *  6. SECOND_SHOOT     - Auto-shoot 3 intaked balls
 *  7. DRIVE_INTAKE_2   - Drive to second intake zone (36, 60)
 *  8. INTAKE_2         - Forward sweep to X=10 while intaking
 *  9. DRIVE_LAUNCH_2   - Return to launch position
 * 10. THIRD_SHOOT      - Auto-shoot 3 intaked balls
 * 11. PARK             - Drive to park position
 * 12. COMPLETE         - Full auto (no stop)
 * 
 * DURING RUN (if Manual Advance ON):
 * - Press A/Cross to advance to next state
 * 
 * Uses Pedro Pathing coordinate system:
 * - X increases to the right
 * - Y increases upward
 * - 0° = facing right, 90° = facing up (counterclockwise positive)
 */
@Autonomous(name = "Auto-Blue Near (Phase Select)", group = "DECODE Auto")
public class Auto_BlueNear extends OpMode {

    // ========== ROBOT SYSTEM ==========
    private RobotSystem robot;
    private Follower follower;
    private Timer actionTimer;
    
    // ========== TEST CONFIGURATION ==========
    private enum TestPhase {
        DRIVE_TO_LAUNCH("1. Drive to Launch"),
        FIRST_SHOOT("2. First Shoot (preloaded)"),
        DRIVE_INTAKE_1("3. Drive to Intake Zone 1"),
        INTAKE_1("4. Intake Balls (sweep 1)"),
        DRIVE_LAUNCH_1("5. Return to Launch"),
        SECOND_SHOOT("6. Second Shoot"),
        DRIVE_INTAKE_2("7. Drive to Intake Zone 2"),
        INTAKE_2("8. Intake Balls (sweep 2)"),
        DRIVE_LAUNCH_2("9. Return to Launch"),
        THIRD_SHOOT("10. Third Shoot"),
        PARK("11. Park"),
        COMPLETE("12. Full Auto (no stop)");
        
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
    // All measurements in INCHES using Pedro Pathing coordinate system
    // Robot is 18x15 inches. Pose = robot center.
    
    // Step 0: Starting position - Blue Near Zone (top-left area)
    private static final Pose START_POSE = new Pose(15, 120, Math.toRadians(180));
    
    // Steps 1/6/11: Launch position - where we shoot from
    private static final Pose LAUNCH_POSE = new Pose(50, 84, Math.toRadians(180));
    
    // Step 3: First intake zone (same Y as launch, shifted left toward balls)
    private static final Pose INTAKE_POSE_1 = new Pose(36, 84, Math.toRadians(180));
    
    // Step 5: End of first forward sweep (drive to X=10 to pick up balls)
    private static final Pose INTAKE_FORWARD_POSE_1 = new Pose(10, 84, Math.toRadians(180));
    
    // Step 8: Second intake zone (one tile row down from first)
    private static final Pose INTAKE_POSE_2 = new Pose(36, 60, Math.toRadians(180));
    
    // Step 10: End of second forward sweep
    private static final Pose INTAKE_FORWARD_POSE_2 = new Pose(10, 60, Math.toRadians(180));
    
    // Step 13: Park position
    private static final Pose PARK_POSE = new Pose(36, 84, Math.toRadians(180));
    
    // ========== SHOOTING PARAMETERS ==========
    // Near zone = medium distance = 1160 velocity + SHORT RANGE hood
    private static final double SHOOTER_VELOCITY = 1160.0;
    private static final double HOOD_POSITION = RobotConstants.Hood.SHORT_RANGE_POSITION;
    private static final LimelightManager.Alliance ALLIANCE = LimelightManager.Alliance.BLUE;
    private static final double INITIAL_TURRET_ANGLE = RobotConstants.Auto.BLUE_NEAR_INITIAL_TURRET_ANGLE;
    
    // ========== TIMING OVERRIDES (tighter for 3-cycle Blue Near to fit 30s) ==========
    // Blue Near has 3 shoot + 2 intake cycles, so every second matters
    private static final double SHOOTER_SPINUP_TIMEOUT = 0.5;    // Flywheel spins up during drive to launch
    private static final double TURRET_AIM_TIMEOUT = 1.0;        // Reduced from 1.5s
    private static final double SHOOT_TIMEOUT = 4.0;             // Reduced from 5.0s
    private static final double INTAKE_SWEEP_TIMEOUT = 3.0;      // Reduced from 4.0s
    private static final double DRIVE_TIMEOUT = 3.0;             // Reduced from 3.5s
    private static final double PARK_TIMEOUT = 2.5;              // Reduced from 3.0s
    
    // ========== STATE MACHINE ==========
    private enum AutoState {
        // Phase 1: Init + Drive to Launch
        INIT,
        DRIVE_TO_LAUNCH_0,
        WAIT_ARRIVE_LAUNCH_0,
        
        // Phase 2: First Shoot (preloaded 3 balls)
        WAIT_SHOOTER_READY,
        WAIT_TURRET_AIM_1,
        SHOOT_SEQUENCE_1,
        WAIT_SHOOT_COMPLETE_1,
        
        // Phase 3: Drive to First Intake Zone
        DRIVE_TO_INTAKE_1,
        WAIT_ARRIVE_INTAKE_1,
        
        // Phase 4: First Intake - forward sweep
        INTAKE_FORWARD_1,
        WAIT_INTAKE_COMPLETE_1,
        
        // Phase 5: Drive back to Launch
        DRIVE_TO_LAUNCH_1,
        WAIT_ARRIVE_LAUNCH_1,
        
        // Phase 6: Second Shoot
        WAIT_TURRET_AIM_2,
        SHOOT_SEQUENCE_2,
        WAIT_SHOOT_COMPLETE_2,
        
        // Phase 7: Drive to Second Intake Zone
        DRIVE_TO_INTAKE_2,
        WAIT_ARRIVE_INTAKE_2,
        
        // Phase 8: Second Intake - forward sweep
        INTAKE_FORWARD_2,
        WAIT_INTAKE_COMPLETE_2,
        
        // Phase 9: Drive back to Launch
        DRIVE_TO_LAUNCH_2,
        WAIT_ARRIVE_LAUNCH_2,
        
        // Phase 10: Third Shoot
        WAIT_TURRET_AIM_3,
        SHOOT_SEQUENCE_3,
        WAIT_SHOOT_COMPLETE_3,
        
        // Phase 11: Park
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
    private Path pathStartToLaunch;          // START → LAUNCH
    private Path pathLaunchToIntake1;        // LAUNCH → INTAKE_1
    private PathChain pathIntakeForward1;    // INTAKE_1 → INTAKE_FORWARD_1 (slow sweep)
    // pathIntake1ToLaunch: built dynamically at runtime (intake sweep may end early)
    private Path pathLaunchToIntake2;        // LAUNCH → INTAKE_2
    private PathChain pathIntakeForward2;    // INTAKE_2 → INTAKE_FORWARD_2 (slow sweep)
    // pathIntake2ToLaunch: built dynamically at runtime (intake sweep may end early)
    private Path pathToPark;                 // LAUNCH → PARK
    
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
        
        // Set hood for short range
        if (robot.servos != null && robot.servos.hood_5 != null) {
            robot.servos.hood_5.setPosition(HOOD_POSITION);
        }
        
        // Start with 3 balls preloaded in launch mode
        if (robot.autoIntake != null) {
            robot.autoIntake.setBallsLoaded(RobotConstants.Auto.BALLS_PER_CYCLE);
            robot.autoIntake.setLaunchMode();
        }
        
        // Use fast shoot timing (3 cycles must fit in 30s)
        if (robot.autoShoot != null) {
            robot.autoShoot.setFastTiming();
        }
        
        buildPaths();
        
        telemetry.addLine("=== TEST MODE: Blue Near ===");
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
        telemetry.addLine("   TEST MODE: Blue Near - Phase Select");
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
        telemetry.addData("Shooter Velocity", "LOW @ %.0f", SHOOTER_VELOCITY);
        telemetry.addData("Hood", "SHORT RANGE @ %.2f", HOOD_POSITION);
        
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
        
        // Start flywheel immediately - runs continuously throughout auto
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
        // Step 1: Start → Launch
        pathStartToLaunch = new Path(new BezierLine(START_POSE, LAUNCH_POSE));
        pathStartToLaunch.setLinearHeadingInterpolation(START_POSE.getHeading(), LAUNCH_POSE.getHeading());
        
        // Step 3: Launch → First Intake
        pathLaunchToIntake1 = new Path(new BezierLine(LAUNCH_POSE, INTAKE_POSE_1));
        pathLaunchToIntake1.setLinearHeadingInterpolation(LAUNCH_POSE.getHeading(), INTAKE_POSE_1.getHeading());
        
        // Step 5: First Intake → Forward Sweep (slow, PathChain for setMaxPower)
        pathIntakeForward1 = follower.pathBuilder()
                .addPath(new BezierLine(INTAKE_POSE_1, INTAKE_FORWARD_POSE_1))
                .setConstantHeadingInterpolation(INTAKE_POSE_1.getHeading())
                .build();
        
        // Step 6: End of First Sweep → Launch (built dynamically at runtime)
        
        // Step 8: Launch → Second Intake
        pathLaunchToIntake2 = new Path(new BezierLine(LAUNCH_POSE, INTAKE_POSE_2));
        pathLaunchToIntake2.setLinearHeadingInterpolation(LAUNCH_POSE.getHeading(), INTAKE_POSE_2.getHeading());
        
        // Step 10: Second Intake → Forward Sweep (slow, PathChain for setMaxPower)
        pathIntakeForward2 = follower.pathBuilder()
                .addPath(new BezierLine(INTAKE_POSE_2, INTAKE_FORWARD_POSE_2))
                .setConstantHeadingInterpolation(INTAKE_POSE_2.getHeading())
                .build();
        
        // Step 11: End of Second Sweep → Launch (built dynamically at runtime)
        
        // Step 13: Launch → Park
        pathToPark = new Path(new BezierLine(LAUNCH_POSE, PARK_POSE));
        pathToPark.setLinearHeadingInterpolation(LAUNCH_POSE.getHeading(), PARK_POSE.getHeading());
    }
    
    // ========== STATE MACHINE ==========
    
    private void runStateMachine() {
        double elapsed = actionTimer.getElapsedTimeSeconds();
        
        switch (currentState) {
            
            // ==================== PHASE 1: INIT + DRIVE TO LAUNCH ====================
            
            case INIT:
                // Store turret offset and hold position (same as Blue Far)
                robot.autoTurret.setInitialOffset(INITIAL_TURRET_ANGLE);
                robot.autoTurret.holdCurrentPosition();
                transitionTo(AutoState.DRIVE_TO_LAUNCH_0);
                break;
                
            case DRIVE_TO_LAUNCH_0:
                follower.followPath(pathStartToLaunch);
                transitionTo(AutoState.WAIT_ARRIVE_LAUNCH_0);
                break;
                
            case WAIT_ARRIVE_LAUNCH_0:
                if (!follower.isBusy() || elapsed > DRIVE_TIMEOUT) {
                    if (stopAfterPhase == TestPhase.DRIVE_TO_LAUNCH) {
                        currentState = AutoState.PHASE_COMPLETE;
                    } else {
                        transitionTo(AutoState.WAIT_SHOOTER_READY);
                    }
                }
                break;
            
            // ==================== PHASE 2: FIRST SHOOT (preloaded 3 balls) ====================
                
            case WAIT_SHOOTER_READY:
                // Flywheel started in start(), should be up to speed by now
                if (robot.shooting.isShooterAtTargetVelocity() || 
                    elapsed > SHOOTER_SPINUP_TIMEOUT) {
                    // Skip turret aim — heading is constant 180° and turret holds position
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
                        transitionTo(AutoState.DRIVE_TO_INTAKE_1);
                    }
                } else if (elapsed > SHOOT_TIMEOUT) {
                    robot.autoShoot.stop();
                    if (stopAfterPhase == TestPhase.FIRST_SHOOT) {
                        currentState = AutoState.PHASE_COMPLETE;
                    } else {
                        transitionTo(AutoState.DRIVE_TO_INTAKE_1);
                    }
                }
                break;
                
            // ==================== PHASE 3: DRIVE TO FIRST INTAKE ZONE ====================
                
            case DRIVE_TO_INTAKE_1:
                // Switch to intake mode and start motor while driving
                if (robot.autoIntake != null) {
                    robot.autoIntake.setIntakeMode();
                    robot.autoIntake.resetBallCount();
                    robot.autoIntake.startIntakeMotor();
                }
                if (robot.led != null) {
                    robot.led.setIntakeModeColor();
                }
                follower.followPath(pathLaunchToIntake1);
                transitionTo(AutoState.WAIT_ARRIVE_INTAKE_1);
                break;
                
            case WAIT_ARRIVE_INTAKE_1:
                if (!follower.isBusy() || elapsed > DRIVE_TIMEOUT) {
                    if (stopAfterPhase == TestPhase.DRIVE_INTAKE_1) {
                        if (robot.autoIntake != null) robot.autoIntake.stopIntakeMotor();
                        currentState = AutoState.PHASE_COMPLETE;
                    } else {
                        transitionTo(AutoState.INTAKE_FORWARD_1);
                    }
                }
                break;
                
            // ==================== PHASE 4: FIRST INTAKE FORWARD SWEEP ====================
                
            case INTAKE_FORWARD_1:
                // Intake motor already running from DRIVE_TO_INTAKE_1
                follower.setMaxPower(RobotConstants.Auto.INTAKE_FORWARD_SPEED);
                follower.followPath(pathIntakeForward1);
                transitionTo(AutoState.WAIT_INTAKE_COMPLETE_1);
                break;
                
            case WAIT_INTAKE_COMPLETE_1:
                // Enforce slow speed every loop
                follower.setMaxPower(RobotConstants.Auto.INTAKE_FORWARD_SPEED);
                
                boolean balls1Full = (robot.autoIntake != null && 
                    robot.autoIntake.getBallsLoaded() >= RobotConstants.Auto.BALLS_PER_CYCLE);
                boolean path1Done = !follower.isBusy();
                boolean timeout1 = elapsed > INTAKE_SWEEP_TIMEOUT;
                
                if (balls1Full || path1Done || timeout1) {
                    follower.setMaxPower(1.0);
                    if (robot.autoIntake != null) {
                        robot.autoIntake.stopIntakeMotor();
                        robot.autoIntake.setLaunchMode();
                    }
                    if (robot.led != null) {
                        robot.led.setLaunchModeColor();
                    }
                    if (stopAfterPhase == TestPhase.INTAKE_1) {
                        currentState = AutoState.PHASE_COMPLETE;
                    } else {
                        transitionTo(AutoState.DRIVE_TO_LAUNCH_1);
                    }
                }
                break;
                
            // ==================== PHASE 5: DRIVE BACK TO LAUNCH ====================
                
            case DRIVE_TO_LAUNCH_1:
                // Build path dynamically from actual position (intake sweep may end early)
                Pose current1 = follower.getPose();
                Path dynamicToLaunch1 = new Path(new BezierLine(current1, LAUNCH_POSE));
                dynamicToLaunch1.setLinearHeadingInterpolation(current1.getHeading(), LAUNCH_POSE.getHeading());
                follower.followPath(dynamicToLaunch1);
                transitionTo(AutoState.WAIT_ARRIVE_LAUNCH_1);
                break;
                
            case WAIT_ARRIVE_LAUNCH_1:
                if (!follower.isBusy() || elapsed > DRIVE_TIMEOUT) {
                    if (stopAfterPhase == TestPhase.DRIVE_LAUNCH_1) {
                        currentState = AutoState.PHASE_COMPLETE;
                    } else {
                        // Skip turret aim — go straight to shoot
                        transitionTo(AutoState.SHOOT_SEQUENCE_2);
                    }
                }
                break;
                
            // ==================== PHASE 6: SECOND SHOOT ====================
                
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
                    if (stopAfterPhase == TestPhase.SECOND_SHOOT) {
                        currentState = AutoState.PHASE_COMPLETE;
                    } else {
                        transitionTo(AutoState.DRIVE_TO_INTAKE_2);
                    }
                } else if (elapsed > SHOOT_TIMEOUT) {
                    robot.autoShoot.stop();
                    if (stopAfterPhase == TestPhase.SECOND_SHOOT) {
                        currentState = AutoState.PHASE_COMPLETE;
                    } else {
                        transitionTo(AutoState.DRIVE_TO_INTAKE_2);
                    }
                }
                break;
                
            // ==================== PHASE 7: DRIVE TO SECOND INTAKE ZONE ====================
                
            case DRIVE_TO_INTAKE_2:
                // Switch to intake mode and start motor while driving
                if (robot.autoIntake != null) {
                    robot.autoIntake.setIntakeMode();
                    robot.autoIntake.resetBallCount();
                    robot.autoIntake.startIntakeMotor();
                }
                if (robot.led != null) {
                    robot.led.setIntakeModeColor();
                }
                follower.followPath(pathLaunchToIntake2);
                transitionTo(AutoState.WAIT_ARRIVE_INTAKE_2);
                break;
                
            case WAIT_ARRIVE_INTAKE_2:
                if (!follower.isBusy() || elapsed > DRIVE_TIMEOUT) {
                    if (stopAfterPhase == TestPhase.DRIVE_INTAKE_2) {
                        if (robot.autoIntake != null) robot.autoIntake.stopIntakeMotor();
                        currentState = AutoState.PHASE_COMPLETE;
                    } else {
                        transitionTo(AutoState.INTAKE_FORWARD_2);
                    }
                }
                break;
                
            // ==================== PHASE 8: SECOND INTAKE FORWARD SWEEP ====================
                
            case INTAKE_FORWARD_2:
                // Intake motor already running from DRIVE_TO_INTAKE_2
                follower.setMaxPower(RobotConstants.Auto.INTAKE_FORWARD_SPEED);
                follower.followPath(pathIntakeForward2);
                transitionTo(AutoState.WAIT_INTAKE_COMPLETE_2);
                break;
                
            case WAIT_INTAKE_COMPLETE_2:
                // Enforce slow speed every loop
                follower.setMaxPower(RobotConstants.Auto.INTAKE_FORWARD_SPEED);
                
                boolean balls2Full = (robot.autoIntake != null && 
                    robot.autoIntake.getBallsLoaded() >= RobotConstants.Auto.BALLS_PER_CYCLE);
                boolean path2Done = !follower.isBusy();
                boolean timeout2 = elapsed > INTAKE_SWEEP_TIMEOUT;
                
                if (balls2Full || path2Done || timeout2) {
                    follower.setMaxPower(1.0);
                    if (robot.autoIntake != null) {
                        robot.autoIntake.stopIntakeMotor();
                        robot.autoIntake.setLaunchMode();
                    }
                    if (robot.led != null) {
                        robot.led.setLaunchModeColor();
                    }
                    if (stopAfterPhase == TestPhase.INTAKE_2) {
                        currentState = AutoState.PHASE_COMPLETE;
                    } else {
                        transitionTo(AutoState.DRIVE_TO_LAUNCH_2);
                    }
                }
                break;
                
            // ==================== PHASE 9: DRIVE BACK TO LAUNCH ====================
                
            case DRIVE_TO_LAUNCH_2:
                // Build path dynamically from actual position (intake sweep may end early)
                Pose current2 = follower.getPose();
                Path dynamicToLaunch2 = new Path(new BezierLine(current2, LAUNCH_POSE));
                dynamicToLaunch2.setLinearHeadingInterpolation(current2.getHeading(), LAUNCH_POSE.getHeading());
                follower.followPath(dynamicToLaunch2);
                transitionTo(AutoState.WAIT_ARRIVE_LAUNCH_2);
                break;
                
            case WAIT_ARRIVE_LAUNCH_2:
                if (!follower.isBusy() || elapsed > DRIVE_TIMEOUT) {
                    if (stopAfterPhase == TestPhase.DRIVE_LAUNCH_2) {
                        currentState = AutoState.PHASE_COMPLETE;
                    } else {
                        // Skip turret aim — go straight to shoot
                        transitionTo(AutoState.SHOOT_SEQUENCE_3);
                    }
                }
                break;
                
            // ==================== PHASE 10: THIRD SHOOT ====================
                
            case WAIT_TURRET_AIM_3:
                // SKIPPED: turret aim not needed (heading & turret don't change)
                transitionTo(AutoState.SHOOT_SEQUENCE_3);
                break;
                
            case SHOOT_SEQUENCE_3:
                robot.autoShoot.start(RobotConstants.Auto.BALLS_PER_CYCLE);
                transitionTo(AutoState.WAIT_SHOOT_COMPLETE_3);
                break;
                
            case WAIT_SHOOT_COMPLETE_3:
                if (!robot.autoShoot.isInProgress()) {
                    // Last shoot done — stop flywheel
                    robot.shooting.stopShooter();
                    if (stopAfterPhase == TestPhase.THIRD_SHOOT) {
                        currentState = AutoState.PHASE_COMPLETE;
                    } else {
                        transitionTo(AutoState.DRIVE_TO_PARK);
                    }
                } else if (elapsed > SHOOT_TIMEOUT) {
                    robot.autoShoot.stop();
                    robot.shooting.stopShooter();
                    if (stopAfterPhase == TestPhase.THIRD_SHOOT) {
                        currentState = AutoState.PHASE_COMPLETE;
                    } else {
                        transitionTo(AutoState.DRIVE_TO_PARK);
                    }
                }
                break;
                
            // ==================== PHASE 11: PARK ====================
                
            case DRIVE_TO_PARK:
                follower.followPath(pathToPark);
                transitionTo(AutoState.WAIT_ARRIVE_PARK);
                break;
                
            case WAIT_ARRIVE_PARK:
                if (!follower.isBusy() || elapsed > PARK_TIMEOUT) {
                    currentState = AutoState.COMPLETE;
                }
                break;
                
            // ==================== COMPLETE ====================
                
            case COMPLETE:
                robot.stopAll();
                break;
                
            case PHASE_COMPLETE:
                // Stopped at selected phase - keep systems running for observation
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
        telemetry.addLine("   BLUE NEAR - RUNNING");
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
        
        // Limelight
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
            telemetry.addData("Mode", robot.autoIntake.isIntakeMode() ? "INTAKE" : "LAUNCH");
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
