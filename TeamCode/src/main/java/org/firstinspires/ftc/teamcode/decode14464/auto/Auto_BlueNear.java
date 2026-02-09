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
 * AUTO: Blue Alliance - Near Zone
 * 
 * Starting Position: (12, 132) @ 45° facing goal
 * Alliance: BLUE (AprilTag 20)
 * 
 * FEATURES:
 * - Auto-Turret: Combined Goal Tracking keeps turret motor pointed at goal
 * - Auto-Shoot: Automated 3-ball shooting sequence
 * - Auto-Intake: Ball detection with automatic carousel advance
 * - Continuous Flywheel: Motor runs at constant velocity throughout auto
 * 
 * SEQUENCE:
 * 1. Start with 3 balls loaded, flywheel starts immediately
 * 2. Enable auto-turret (Combined Goal Tracking)
 * 3. Wait for flywheel velocity + turret aim
 * 4. Shoot all 3 balls
 * 5. Drive to intake zone - flywheel stays running
 * 6. Intake 3 new balls (auto-advance on detection)
 * 7. Return to launch zone - flywheel already at speed
 * 8. Wait for turret to re-aim at goal
 * 9. Shoot all 3 balls immediately
 * 
 * Uses Pedro Pathing coordinate system:
 * - X increases to the right
 * - Y increases upward  
 * - 0° = facing right, 90° = facing up (counterclockwise positive)
 */
@Autonomous(name = "Auto-Blue Near", group = "DECODE Auto")
public class Auto_BlueNear extends OpMode {

    // ========== ROBOT SYSTEM ==========
    private RobotSystem robot;
    private Follower follower;
    private Timer actionTimer;
    
    // ========== FIELD POSITIONS ==========
    // All measurements in INCHES using Pedro Pathing coordinate system
    
    // Starting/Launch position - Blue Near Zone
    private static final Pose START_POSE = new Pose(12, 132, Math.toRadians(45)); // Facing goal
    
    // Intake zone - where we pick up balls
    private static final Pose INTAKE_POSE = new Pose(36, 84, Math.toRadians(45));
    
    // ========== SHOOTING PARAMETERS (from RobotConstants) ==========
    // Near zone = short distance shots = LOW velocity + SHORT RANGE hood
    private static final double SHOOTER_VELOCITY = RobotConstants.Shooter.LOW_VELOCITY;
    private static final double HOOD_POSITION = RobotConstants.Hood.SHORT_RANGE_POSITION;
    
    // ========== ALLIANCE ==========
    private static final LimelightManager.Alliance ALLIANCE = LimelightManager.Alliance.BLUE;
    
    // ========== INITIAL TURRET ANGLE (TUNABLE) ==========
    private static final double INITIAL_TURRET_ANGLE = RobotConstants.Auto.BLUE_NEAR_INITIAL_TURRET_ANGLE;
    
    // ========== STATE MACHINE ==========
    private enum AutoState {
        // Initial setup
        INIT,
        MOVE_TURRET_INITIAL,
        WAIT_TURRET_INITIAL,
        
        // Initial shooting sequence
        WAIT_SHOOTER_READY_1,
        WAIT_TURRET_AIM_1,
        SHOOT_SEQUENCE_1,
        WAIT_SHOOT_COMPLETE_1,
        
        // Drive to intake
        DRIVE_TO_INTAKE,
        WAIT_ARRIVE_INTAKE,
        
        // Intake sequence
        INTAKING,
        
        // Return to launch
        DRIVE_TO_LAUNCH,
        WAIT_ARRIVE_LAUNCH,
        
        // Second shooting sequence
        WAIT_TURRET_AIM_2,
        SHOOT_SEQUENCE_2,
        WAIT_SHOOT_COMPLETE_2,
        
        // Done
        COMPLETE
    }
    
    private AutoState currentState = AutoState.INIT;
    
    // ========== PATHS ==========
    private Path pathToIntake;
    private Path pathToLaunch;
    
    // ========== LIFECYCLE METHODS ==========
    
    @Override
    public void init() {
        // Initialize robot system with starting pose
        robot = new RobotSystem();
        robot.init(hardwareMap, START_POSE);
        
        // Get follower reference from robot's localization system
        follower = robot.localization.getFollower();
        
        // Set alliance
        if (robot.limelight != null) {
            robot.limelight.setAlliance(ALLIANCE);
        }
        
        // Initialize timer
        actionTimer = new Timer();
        
        // Set initial hood position for near zone
        if (robot.servos != null && robot.servos.hood_5 != null) {
            robot.servos.hood_5.setPosition(HOOD_POSITION);
        }
        
        // Ensure we're in launch mode with 3 balls loaded (index 2)
        if (robot.autoIntake != null) {
            robot.autoIntake.setBallsLoaded(RobotConstants.Auto.BALLS_PER_CYCLE); // Tell system balls are pre-loaded
            robot.autoIntake.setLaunchMode(); // Now positions correctly based on ballsLoaded
        }
        
        // Build paths
        buildPaths();
        
        // Telemetry
        displayInitTelemetry();
    }
    
    @Override
    public void init_loop() {
        // Update limelight during init for AprilTag detection
        if (robot.limelight != null) {
            robot.limelight.update();
            telemetry.addData("Limelight", robot.limelight.isConnected() ? "✓ Connected" : "✗ Not Connected");
            if (robot.limelight.hasAllianceTarget()) {
                telemetry.addData("AprilTag", "✓ VISIBLE - tx: %.1f°", robot.limelight.getTargetX());
            } else {
                telemetry.addData("AprilTag", "✗ Tag %d not visible", robot.limelight.getTargetTagId());
            }
        }
        telemetry.update();
    }
    
    @Override
    public void start() {
        // Reset timer and start state machine
        actionTimer.resetTimer();
        currentState = AutoState.INIT;
        
        // Set LED to launch mode
        if (robot.led != null) {
            robot.led.setLaunchModeColor();
        }
        
        // NOTE: Auto-turret enabled AFTER initial turret positioning in state machine
        
        // Start flywheel immediately - runs continuously throughout auto
        robot.shooting.startShooter(SHOOTER_VELOCITY);
    }
    
    @Override
    public void loop() {
        // Update all robot systems
        robot.updateAll();
        follower.update();
        
        // Run state machine
        runStateMachine();
        
        // Telemetry
        updateTelemetry();
    }
    
    @Override
    public void stop() {
        if (robot != null) {
            robot.stopAll();
        }
        telemetry.addData("Status", "✓ Auto Complete");
        telemetry.update();
    }
    
    // ========== PATH BUILDING ==========
    
    private void buildPaths() {
        // Path from start/launch zone to intake zone
        pathToIntake = new Path(new BezierLine(START_POSE, INTAKE_POSE));
        pathToIntake.setLinearHeadingInterpolation(START_POSE.getHeading(), INTAKE_POSE.getHeading());
        
        // Path from intake zone back to start/launch zone
        pathToLaunch = new Path(new BezierLine(INTAKE_POSE, START_POSE));
        pathToLaunch.setLinearHeadingInterpolation(INTAKE_POSE.getHeading(), START_POSE.getHeading());
    }
    
    // ========== STATE MACHINE ==========
    
    private void runStateMachine() {
        double elapsed = actionTimer.getElapsedTimeSeconds();
        
        switch (currentState) {
            
            // ==================== INITIAL SHOOTING ====================
            
            case INIT:
                robot.autoTurret.moveToAngle(INITIAL_TURRET_ANGLE);
                currentState = AutoState.MOVE_TURRET_INITIAL;
                actionTimer.resetTimer();
                break;
                
            case MOVE_TURRET_INITIAL:
                if (robot.autoTurret.isAtTargetPosition() || 
                    elapsed > RobotConstants.Auto.INITIAL_TURRET_MOVE_TIMEOUT) {
                    robot.autoTurret.resumeFromReset();
                    robot.autoTurret.enableCombinedGoalTracking();
                    currentState = AutoState.WAIT_TURRET_INITIAL;
                    actionTimer.resetTimer();
                }
                break;
                
            case WAIT_TURRET_INITIAL:
                if (elapsed > RobotConstants.Auto.TURRET_SETTLING_TIME) {
                    currentState = AutoState.WAIT_SHOOTER_READY_1;
                    actionTimer.resetTimer();
                }
                break;
                
            case WAIT_SHOOTER_READY_1:
                // Wait for shooter to reach target velocity
                if (robot.shooting.isShooterAtTargetVelocity() || 
                    elapsed > RobotConstants.Auto.SHOOTER_SPINUP_SECONDS) {
                    currentState = AutoState.WAIT_TURRET_AIM_1;
                    actionTimer.resetTimer();
                }
                break;
                
            case WAIT_TURRET_AIM_1:
                // Wait for turret to be on target
                if (robot.autoTurret.isOnTarget() || 
                    elapsed > RobotConstants.Auto.TURRET_AIM_TIMEOUT) {
                    currentState = AutoState.SHOOT_SEQUENCE_1;
                    actionTimer.resetTimer();
                }
                break;
                
            case SHOOT_SEQUENCE_1:
                // Start auto-shoot sequence
                robot.autoShoot.start(RobotConstants.Auto.BALLS_PER_CYCLE);
                currentState = AutoState.WAIT_SHOOT_COMPLETE_1;
                actionTimer.resetTimer();
                break;
                
            case WAIT_SHOOT_COMPLETE_1:
                // Wait for shooting to complete (timeout is safety fallback)
                if (!robot.autoShoot.isInProgress()) {
                    currentState = AutoState.DRIVE_TO_INTAKE;
                    actionTimer.resetTimer();
                } else if (elapsed > RobotConstants.Auto.SHOOT_SEQUENCE_TIMEOUT) {
                    robot.autoShoot.stop();
                    currentState = AutoState.DRIVE_TO_INTAKE;
                    actionTimer.resetTimer();
                }
                break;
                
            // ==================== DRIVE TO INTAKE ====================
                
            case DRIVE_TO_INTAKE:
                // Switch to intake mode and start driving
                if (robot.autoIntake != null) {
                    robot.autoIntake.setIntakeMode();
                    robot.autoIntake.startIntakeMotor();
                }
                if (robot.led != null) {
                    robot.led.setIntakeModeColor();
                }
                follower.followPath(pathToIntake);
                currentState = AutoState.WAIT_ARRIVE_INTAKE;
                actionTimer.resetTimer();
                break;
                
            case WAIT_ARRIVE_INTAKE:
                // Wait to arrive at intake zone
                if (!follower.isBusy() || elapsed > RobotConstants.Auto.PATH_TIMEOUT) {
                    currentState = AutoState.INTAKING;
                    actionTimer.resetTimer();
                }
                break;
                
            // ==================== INTAKE SEQUENCE ====================
                
            case INTAKING:
                // Wait for intake time (auto-advance handles ball detection)
                if (elapsed > RobotConstants.Auto.INTAKE_DURATION) {
                    // Stop intake and switch to launch mode
                    if (robot.autoIntake != null) {
                        robot.autoIntake.stopIntakeMotor();
                        robot.autoIntake.setLaunchMode();
                    }
                    if (robot.led != null) {
                        robot.led.setLaunchModeColor();
                    }
                    currentState = AutoState.DRIVE_TO_LAUNCH;
                    actionTimer.resetTimer();
                }
                break;
                
            // ==================== DRIVE TO LAUNCH ====================
                
            case DRIVE_TO_LAUNCH:
                // Drive back to launch zone (flywheel already running)
                follower.followPath(pathToLaunch);
                currentState = AutoState.WAIT_ARRIVE_LAUNCH;
                actionTimer.resetTimer();
                break;
                
            case WAIT_ARRIVE_LAUNCH:
                // Wait to arrive at launch zone
                if (!follower.isBusy() || elapsed > RobotConstants.Auto.PATH_TIMEOUT) {
                    currentState = AutoState.WAIT_TURRET_AIM_2;
                    actionTimer.resetTimer();
                }
                break;
                
            // ==================== SECOND SHOOTING ====================
                
            case WAIT_TURRET_AIM_2:
                // Wait for turret to be on target (flywheel already at speed)
                if (robot.autoTurret.isOnTarget() || 
                    elapsed > RobotConstants.Auto.TURRET_AIM_TIMEOUT) {
                    currentState = AutoState.SHOOT_SEQUENCE_2;
                    actionTimer.resetTimer();
                }
                break;
                
            case SHOOT_SEQUENCE_2:
                // Start auto-shoot sequence
                robot.autoShoot.start(RobotConstants.Auto.BALLS_PER_CYCLE);
                currentState = AutoState.WAIT_SHOOT_COMPLETE_2;
                actionTimer.resetTimer();
                break;
                
            case WAIT_SHOOT_COMPLETE_2:
                // Wait for shooting to complete (timeout is safety fallback)
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
                // Auto is done
                robot.stopAll();
                break;
        }
    }
    
    // ========== TELEMETRY ==========
    
    private void displayInitTelemetry() {
        telemetry.addData("Status", "✓ Initialized");
        telemetry.addData("Alliance", ALLIANCE == LimelightManager.Alliance.BLUE ? "BLUE" : "RED");
        telemetry.addData("Start Pose", "(%.1f, %.1f) @ %.1f°", 
            START_POSE.getX(), START_POSE.getY(), Math.toDegrees(START_POSE.getHeading()));
        telemetry.addLine();
        telemetry.addLine("─── CONFIGURATION ───");
        telemetry.addData("Flywheel", "LOW @ %.0f ticks/sec", SHOOTER_VELOCITY);
        telemetry.addData("Hood", "SHORT RANGE @ %.2f", HOOD_POSITION);
        telemetry.addData("Balls/Cycle", RobotConstants.Auto.BALLS_PER_CYCLE);
        telemetry.addLine();
        telemetry.addData("Sequence", "Shoot → Intake @ (%.0f,%.0f) → Shoot", 
            INTAKE_POSE.getX(), INTAKE_POSE.getY());
        telemetry.addLine();
        telemetry.addLine(">>> READY TO START <<<");
        telemetry.update();
    }
    
    private void updateTelemetry() {
        telemetry.addData("State", currentState.toString());
        telemetry.addData("Timer", "%.2f s", actionTimer.getElapsedTimeSeconds());
        telemetry.addLine();
        
        // Position
        telemetry.addData("Position", "(%.1f, %.1f) @ %.1f°",
            robot.localization.getX(), 
            robot.localization.getY(),
            robot.localization.getHeadingDegrees());
        
        // Auto-turret status
        if (robot.autoTurret != null) {
            telemetry.addData("Turret", "%s %s",
                robot.autoTurret.isAnyModeEnabled() ? "TRACKING" : "OFF",
                robot.autoTurret.isOnTarget() ? "✓" : "⏳");
        }
        
        // Shooter status
        telemetry.addData("Shooter", "%.0f/%.0f %s",
            robot.shooting.getCurrentVelocity(),
            SHOOTER_VELOCITY,
            robot.shooting.isShooterAtTargetVelocity() ? "✓" : "⏳");
        
        // Auto-shoot status
        if (robot.autoShoot.isInProgress()) {
            telemetry.addData("Shooting", "%d/%d balls",
                robot.autoShoot.getBallsShot(),
                robot.autoShoot.getBallsToShoot());
        }
        
        // Mode and path status
        telemetry.addData("Mode", robot.autoIntake.isIntakeMode() ? "INTAKE" : "LAUNCH");
        telemetry.addData("Path", follower.isBusy() ? "Moving" : "Done");
        
        telemetry.update();
    }
}
