package org.firstinspires.ftc.teamcode.decode14464;

import com.pedropathing.follower.Follower;
import com.pedropathing.geometry.BezierLine;
import com.pedropathing.geometry.Pose;
import com.pedropathing.paths.Path;
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.util.ElapsedTime;
import com.pedropathing.util.Timer;
import org.firstinspires.ftc.teamcode.pedroPathing.Constants;


@Autonomous(name = "Auto Red Far", group = "DECODE Auto")
public class Auto_Red_Far extends OpMode {

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
    private static final Pose startPose = new Pose(-12, -63, Math.toRadians(90)); // Close blue starting position
    private static final Pose parkPose = new Pose(10, 60);


    // Shooting parameters (adjusted for close position)
    private static final double SHOOT_VELOCITY = 1100; // Lower velocity for close shots
    private static final double HOOD_ANGLE_BASKET_CLOSE = 0.50; // Lower hood angle for close basket

    // Timing constants
    private static final int SHOOT_DELAY_MS = 500; // Delay between shots
    private static final int INTAKE_DELAY_MS = 1000; // Time to intake sample
    private static final double AUTO_TRACK_TIMEOUT = 3.0; // Max time to lock onto target

    private Path park;

    public void buildPaths() {
                park = new Path(new BezierLine(startPose, parkPose));
                park.setLinearHeadingInterpolation(startPose.getHeading(), parkPose.getHeading());
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
                //follower.followPath(startPose);
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
                if(actionTimer.getElapsedTimeSeconds() > 1){
                    robotMotors.stopShooter();
                    follower.followPath(park, true);
                }

        }
    }
}


