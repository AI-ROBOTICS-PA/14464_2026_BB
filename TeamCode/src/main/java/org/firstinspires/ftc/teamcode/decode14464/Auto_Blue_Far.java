package org.firstinspires.ftc.teamcode.decode14464;

import com.pedropathing.follower.Follower;
import com.pedropathing.geometry.BezierLine;
import com.pedropathing.geometry.Pose;
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.pedropathing.paths.Path;
import com.pedropathing.paths.PathChain;
import com.pedropathing.util.Timer;

import org.firstinspires.ftc.teamcode.decode14464.systems.LimelightManager;
import org.firstinspires.ftc.teamcode.pedroPathing.Constants;
import org.firstinspires.ftc.teamcode.decode14464.systems.RobotMotors;
import org.firstinspires.ftc.teamcode.decode14464.systems.RobotServos;
import org.firstinspires.ftc.teamcode.decode14464.systems.RobotSensors;


//// DISABLED: Old version — use auto/Auto_BlueFar_TEST.java instead
// @Autonomous(name = "Auto Blue Far", group = "DECODE Auto")
public class Auto_Blue_Far extends OpMode {


    private final double[] INTAKE_POSITIONS = {0.07, 0.44, 0.81};
    private int currentTurretPositionIndex = 0;
    private boolean colorTransitionDetected = false;
    private String previousColor = "Black";
    // Hardware and systems
    private Follower follower;
    private Timer actionTimer;
    private final RobotMotors robotMotors = new RobotMotors();
    private final RobotServos robotServos = new RobotServos();
    private final RobotSensors robotSensors = new RobotSensors();
    private final LimelightManager limelightManager = new LimelightManager();

    // Auto-turret
    private final DecodeUtils.TurretPIDState turretPidState = new DecodeUtils.TurretPIDState();

    private int pathState = 0;


    // Field positions (BLUE ALLIANCE - CLOSE STARTING POSITION)
    // All measurements in INCHES
    private static final Pose startPose = new Pose(58, 10); // Close blue starting position
    private static final Pose spikePose1 = new Pose(94, 24, Math.toRadians(70));
    private static final Pose ball2 = new Pose(100, 27, Math.toRadians(70));
    private static final Pose ball3 = new Pose(100, 30, Math.toRadians(70));
    private static final Pose ball4 = new Pose(100, 34, Math.toRadians(70));


    // Shooting parameters (adjusted for close position)
    private static final double SHOOT_VELOCITY = 1100; // Lower velocity for close shots
    private static final double HOOD_ANGLE_BASKET_CLOSE = 0.50; // Lower hood angle for close basket

    // Timing constants
    private static final int SHOOT_DELAY_MS = 500; // Delay between shots
    private static final int INTAKE_DELAY_MS = 1000; // Time to intake sample
    private static final double AUTO_TRACK_TIMEOUT = 3.0; // Max time to lock onto target

    private Path pickup1;
    private PathChain pickup2, pickup3, pickup4, score2;

    public void buildPaths() {
            pickup1 = new Path(new BezierLine(startPose, spikePose1));
            pickup1.setLinearHeadingInterpolation(startPose.getHeading(), spikePose1.getHeading());

            pickup2 = follower.pathBuilder()
                    .addPath(new BezierLine(spikePose1,ball2))
                    .setLinearHeadingInterpolation(spikePose1.getHeading(), ball2.getHeading())
                    .build();

            pickup3 = follower.pathBuilder()
                    .addPath(new BezierLine(ball2, ball3))
                    .setLinearHeadingInterpolation(ball2.getHeading(), ball3.getHeading())
                    .build();
            pickup4 = follower.pathBuilder()
                    .addPath(new BezierLine(ball3, ball4))
                    .setLinearHeadingInterpolation(ball3.getHeading(), ball4.getHeading())
                    .build();

            score2 = follower.pathBuilder()
                    .addPath(new BezierLine(ball4, startPose))
                    .setLinearHeadingInterpolation(ball4.getHeading(), startPose.getHeading())
                    .build();
    }

    @Override
    public void init() {
        follower = Constants.createFollower(hardwareMap);
        follower.setStartingPose(startPose);

        robotMotors.init(hardwareMap);
        robotServos.init(hardwareMap);
        robotSensors.init(hardwareMap);
        limelightManager.init(hardwareMap);
        DecodeUtils.initializeTurretMotor(robotMotors.turret);

        actionTimer = new Timer();

        robotServos.hood_5.setPosition(0.5);
        robotServos.turret_1.setPosition(0.17);

        buildPaths();

        telemetry.addData("Status", "Initialized- Blue Close");
        telemetry.update();
    }
    @Override
    public void start() {
        pathState = 1;
        actionTimer.resetTimer();
    }

    @Override
    public void loop() {
        follower.update();
        limelightManager.update();
        autonomousPathUpdate();

        telemetry.addData("Path State", pathState);
        telemetry.addData("Turret Pos", robotMotors.turret.getCurrentPosition());
        telemetry.addData("Detected Color", robotSensors.getColorName(robotSensors.color_f));
        telemetry.update();
    }

    public void autonomousPathUpdate() {
        switch (pathState) {
            case 1:
                pathState = 2;
                break;
            case 2:
                if (!follower.isBusy()) {
                    robotMotors.startShooter(1450);
                    robotServos.turret_1.setPosition(0.17);
                    actionTimer.resetTimer();

                    pathState = 3;
                }
                break;
            case 3:
                if (actionTimer.getElapsedTimeSeconds() > 2) {
                    actionTimer.resetTimer();
                    robotServos.ballpush_4.setPosition(robotServos.ballpush_initial_position + 0.27);
                    pathState = 4;
                }
                break;
            case 4:
                if (actionTimer.getElapsedTimeSeconds()>0.5){
                    robotServos.ballpush_4.setPosition(robotServos.ballpush_initial_position);
                    actionTimer.resetTimer();
                    pathState = 5;
                }
                break;
            case 5:
                if (actionTimer.getElapsedTimeSeconds() > 0.5) {
                    robotServos.turret_1.setPosition(0.53);
                    actionTimer.resetTimer();
                    pathState = 6;
                }
                break;
            case 6:
                if (actionTimer.getElapsedTimeSeconds() > 0.5) {
                    robotServos.ballpush_4.setPosition(robotServos.ballpush_initial_position + 0.27);
                    actionTimer.resetTimer();
                    pathState = 7;
                }
                break;
            case 7:
                if (actionTimer.getElapsedTimeSeconds() >0.5){
                    robotServos.ballpush_4.setPosition(robotServos.ballpush_initial_position);
                    pathState = 8;
                }
                break;
            case 8:
                if (actionTimer.getElapsedTimeSeconds() > 1) {
                    robotServos.turret_1.setPosition(0.90);
                    actionTimer.resetTimer();
                    pathState = 9;
                }
                break;
            case 9:
                if (actionTimer.getElapsedTimeSeconds() > 0.5) {
                    robotServos.ballpush_4.setPosition(robotServos.ballpush_initial_position + 0.27);
                    actionTimer.resetTimer();
                    pathState = 10;
                }
                break;
            case 10:
                if (actionTimer.getElapsedTimeSeconds() >0.5){
                    robotServos.ballpush_4.setPosition(robotServos.ballpush_initial_position);
                    actionTimer.resetTimer();
                    pathState = 11;
                }
                break;
            case 11:
                if(actionTimer.getElapsedTimeSeconds()>0.5) {
                    follower.followPath(pickup1);
                    robotMotors.intake.setPower(1);
                    robotServos.turret_1.setPosition(0.07);
                    actionTimer.resetTimer();
                    pathState = 12;
                }
                break;
            case 12:
                if (actionTimer.getElapsedTimeSeconds()>1) {
                    actionTimer.resetTimer();
                    String currentColor = robotSensors.getColorName(robotSensors.color_f);
                    boolean seeingBall = !currentColor.equalsIgnoreCase("Black") && !currentColor.equalsIgnoreCase("None");

                    if (seeingBall && !colorTransitionDetected) {
                        colorTransitionDetected = true;

                        if (currentTurretPositionIndex == 0){
                            follower.setMaxPower(0.4);
                            follower.followPath(pickup2);
                        } else if (currentTurretPositionIndex == 1){
                            follower.setMaxPower(0.4);
                            follower.followPath(pickup3);
                        } else if (currentTurretPositionIndex == 2){
                            follower.setMaxPower(0.4);
                            follower.followPath(pickup4);
                        } else if (actionTimer.getElapsedTimeSeconds()>7) {
                            pathState = 13;
                            follower.followPath(score2);
                        }

                        if (currentTurretPositionIndex <  INTAKE_POSITIONS.length - 1){
                            currentTurretPositionIndex++;
                            robotServos.turret_1.setPosition(INTAKE_POSITIONS[currentTurretPositionIndex]);

                        }
                    }



                    if (!seeingBall) {
                        colorTransitionDetected = false;
                    }



                }
                break;

            case 13:

        }
    }
}


