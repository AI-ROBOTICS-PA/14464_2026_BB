package org.firstinspires.ftc.teamcode.decode14464;

import com.bylazar.configurables.annotations.Configurable;
import com.bylazar.telemetry.PanelsTelemetry;
import com.bylazar.telemetry.TelemetryManager;
import com.pedropathing.follower.Follower;
import com.pedropathing.geometry.BezierLine;
import com.pedropathing.geometry.Pose;
import com.pedropathing.paths.HeadingInterpolator;
import com.pedropathing.paths.Path;
import com.pedropathing.paths.PathChain;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DcMotor;
import org.firstinspires.ftc.teamcode.pedroPathing.Constants;
import java.util.function.Supplier;

@Configurable
@TeleOp
public class Decode_Teleop extends OpMode {
    private Follower follower;
    public static Pose startingPose; //See ExampleAuto to understand how to use this
    private boolean automatedDrive;
    private Supplier<PathChain> pathChain;
    private TelemetryManager telemetryM;
    private boolean slowMode = false;
    private double slowModeMultiplier = 0.5;

    private RobotMotors robotMotors = new RobotMotors();
    private RobotServos robotServos = new RobotServos();
    private LimelightManager limelightManager = new LimelightManager();
    private RobotSensors robotSensors = new RobotSensors();
    private boolean prevDpadUpPressed  = false;
    private boolean prevDpadDownPressed  = false;
    private boolean prevDpadLeftPressed  = false;
    private boolean prevDpadRightPressed  = false;
    private boolean prevDpadLeftPressed2  = false;
    private boolean prevDpadRightPressed2  = false;
    private boolean prevAPressed2  = false;
    private final double BI_INCREMENT = 0.1; // Amount to increment/decrement
    private double BI_servoPosition = 0.67; // Initial servo position
    private final double BI_MIN_POSITION = 0.37;
    private final double BI_MAX_POSITION = 0.67;
    private final double SH_INCREMENT = 0.02; // Amount to increment/decrement
    private double SH_servoPosition = 0.45; // Initial servo position
    private final double SH_MIN_POSITION = 0.45;
    private final double SH_MAX_POSITION = 0.67;
    private final double tmp_INCREMENT = 0.05; // Amount to increment/decrement
    private double tmp_servoPosition = 0.25; // Initial servo position
    private final double tmp_MIN_POSITION = .23;
    private final double tmp_MAX_POSITION = .57;
    private boolean leftBumped;
    private boolean isintakeActive = false;
    private boolean rightBumped;
    private boolean rightBumped2;
    private boolean isshooterActive = false;
    private double targetShooterVelocity = 1460; // Target velocity for shooter
    private boolean isscanActive = false;
    private boolean iscatActive = false;
    private boolean leftBumped2;
    private boolean isholdActive = false;

    @Override
    public void init() {
        robotMotors.init(hardwareMap);
        robotServos.init(hardwareMap);
        // limelightManager.init(hardwareMap);
        // robotSensors.init(hardwareMap);
        follower = Constants.createFollower(hardwareMap);
        follower.setStartingPose(startingPose == null ? new Pose() : startingPose);
        follower.update();
        telemetryM = PanelsTelemetry.INSTANCE.getTelemetry();
        robotServos.longShootHeight();
        robotServos.holdCatapult();
        robotServos.scanballload();
    }

    @Override
    public void start() {
        //The parameter controls whether the Follower should use break mode on the motors (using it is recommended).
        //In order to use float mode, add .useBrakeModeInTeleOp(true); to your Drivetrain Constants in Constant.java (for Mecanum)
        //If you don't pass anything in, it uses the default (false)
        follower.startTeleopDrive();
        robotServos.lockCatapult();
    }

    @Override
    public void loop() {
        //Call this once per loop
        follower.update();
        telemetryM.update();

        // Intake control For player 1 with left bumper and player 2 right stick
        if (gamepad1.left_bumper && !leftBumped) {
            if (isintakeActive){
               robotMotors.stopIntake();
                isintakeActive = false;
            }
            else{
                robotMotors.startIntake();
                isintakeActive = true;
           }
        }
        leftBumped = gamepad1.left_bumper;
        if (!isintakeActive)
        {
            robotMotors.intake.setPower(-gamepad2.right_stick_y/2.5);
        }

        // Shooter control with right bumper
        if (gamepad1.right_bumper && !rightBumped) {
            if (isshooterActive){
                robotMotors.stopShooter();
                isshooterActive = false;
            }
            else{
                robotMotors.startShooter(targetShooterVelocity);
                isshooterActive = true;
            }
        }
        rightBumped = gamepad1.right_bumper;

        // Drive control
        if (!automatedDrive) {
            //This is the normal version to use in the TeleOp
            if (!slowMode) follower.setTeleOpDrive(
                    -gamepad1.left_stick_y,
                    -gamepad1.left_stick_x,
                    -gamepad1.right_stick_x,
                    true // Robot Centric
            );
            //This is how it looks with slowMode on
            else follower.setTeleOpDrive(
                    -gamepad1.left_stick_y * slowModeMultiplier,
                    -gamepad1.left_stick_x * slowModeMultiplier,
                    -gamepad1.right_stick_x * slowModeMultiplier,
                    true // Robot Centric
            );
        }

        // Servos control
        // Scan ball control with A button on gamepad2
        if(gamepad2.a && !prevAPressed2){
            if(isscanActive){
                robotServos.scanballresetscan();
                isscanActive = false;
            }
            else{
                robotServos.scanballload();
                isscanActive = true;
            }
        }
        prevAPressed2 = gamepad2.a;

        // Cataput control with right bumper
        if (gamepad2.right_bumper && !rightBumped2) {
            if (iscatActive){
                robotServos.holdCatapult();
                iscatActive = false;
            }
            else{
                robotServos.releaseCatapult();
                iscatActive = true;
            }
        }
        rightBumped2 = gamepad2.right_bumper;

        if (gamepad2.left_bumper && !leftBumped2) {
            if (isholdActive){
                robotServos.launchCatapult();
                isholdActive = false;
            }
            else{
                robotServos.lockCatapult();
                isholdActive = true;
            }
        }
        leftBumped2 = gamepad2.left_bumper;

        // SG: Ball stop servo control with left trigger
        robotServos.ballstop_0c.setPosition(gamepad1.left_trigger/4);
        //SG: Ball push servo control with right trigger
        robotServos.ballpush_0.setPosition((gamepad1.right_trigger/6)+.2);
        // SG: Intake servo control
        // D-pad up increases position by 0.1, D-pad down decreases position by 0.1
        // Check if the dpad_up button is pressed
        if (gamepad2.dpad_up && !prevDpadUpPressed) {
            BI_servoPosition -= BI_INCREMENT;
            // Ensure the servo position doesn't go below the min
            if (BI_servoPosition < BI_MIN_POSITION) {
                BI_servoPosition = BI_MIN_POSITION;
            }
            robotServos.intake_1.setPosition(BI_servoPosition);
        }
        prevDpadUpPressed = gamepad2.dpad_up;
        // Check if the dpad_down button is pressed
        if (gamepad2.dpad_down && !prevDpadDownPressed) {
            BI_servoPosition += BI_INCREMENT;
            // Ensure the servo position doesn't exceed the max
            if (BI_servoPosition > BI_MAX_POSITION) {
                BI_servoPosition = BI_MAX_POSITION;
            }
            robotServos.intake_1.setPosition(BI_servoPosition);
        }
        prevDpadDownPressed = gamepad2.dpad_down;
        // Check if the dpad_up button is pressed
        if (gamepad1.dpad_left && !prevDpadLeftPressed) {
            SH_servoPosition -= SH_INCREMENT;
            // Ensure the servo position doesn't go below the min
            if (SH_servoPosition < SH_MIN_POSITION) {
                SH_servoPosition = SH_MIN_POSITION;
            }
            robotServos.shooterHeight_2.setPosition(SH_servoPosition);
        }
        prevDpadLeftPressed = gamepad1.dpad_left;
        // Check if the dpad_down button is pressed
        if (gamepad1.dpad_right && !prevDpadRightPressed) {
            SH_servoPosition += SH_INCREMENT;
            // Ensure the servo position doesn't exceed the max
            if (SH_servoPosition > SH_MAX_POSITION) {
                SH_servoPosition = SH_MAX_POSITION;
            }
            robotServos.shooterHeight_2.setPosition(SH_servoPosition);
        }
        prevDpadRightPressed = gamepad1.dpad_right;
        // Check if the dpad_up button is pressed
        if (gamepad2.dpad_left && !prevDpadLeftPressed2) {
            tmp_servoPosition -= tmp_INCREMENT;
            // Ensure the servo position doesn't go below the min
            if (tmp_servoPosition < tmp_MIN_POSITION) {
                tmp_servoPosition = tmp_MIN_POSITION;
            }
            robotServos.scanner_5.setPosition(tmp_servoPosition);
        }
        prevDpadLeftPressed2 = gamepad2.dpad_left;
        // Check if the dpad_down button is pressed
        if (gamepad2.dpad_right && !prevDpadRightPressed2) {
            tmp_servoPosition += tmp_INCREMENT;
            // Ensure the servo position doesn't exceed the max
            if (tmp_servoPosition > tmp_MAX_POSITION) {
                tmp_servoPosition = tmp_MAX_POSITION;
            }
            robotServos.scanner_5.setPosition(tmp_servoPosition);
        }
        prevDpadRightPressed2 = gamepad2.dpad_right;
        telemetry.addData("Push Ball Position", BI_servoPosition);
        telemetry.addData("Shooter height Position", SH_servoPosition);
        telemetry.addData("Shooter height Position", tmp_servoPosition);
        // Enhanced shooter telemetry
        telemetry.addData("isholdActive Active", isholdActive);
        telemetry.addData("iscatActive Active", iscatActive);
        telemetry.addData("Shooter Target Velocity", targetShooterVelocity);
        telemetry.addData("Shooter Current Velocity", robotMotors.getShooterVelocity());
        // Show if shooter is ready (at target velocity)
        if (isshooterActive) {
            boolean isReady = robotMotors.isShooterAtVelocity(targetShooterVelocity, 50);
            telemetry.addData("Shooter Ready", isReady ? "YES ✓" : "Spinning up...");
            // Show velocity error
            double velocityError = targetShooterVelocity - robotMotors.getShooterVelocity();
            telemetry.addData("Velocity Error", String.format("%.1f ticks/sec", velocityError));
        }
        telemetry.update();
    }
}

