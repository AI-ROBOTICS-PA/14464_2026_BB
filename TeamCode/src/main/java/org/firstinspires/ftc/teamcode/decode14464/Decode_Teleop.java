package org.firstinspires.ftc.teamcode.decode14464;

import com.bylazar.configurables.annotations.Configurable;
import com.bylazar.telemetry.PanelsTelemetry;
import com.bylazar.telemetry.TelemetryManager;
import com.pedropathing.follower.Follower;
import com.pedropathing.geometry.Pose;
import com.pedropathing.paths.PathChain;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
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
    private boolean prevAPressed  = false;
    private final double BI_INCREMENT = 0.1; // Amount to increment/decrement
    private double BI_servoPosition = 0.67; // Initial servo position
    private final double BI_MIN_POSITION = 0.37;
    private final double BI_MAX_POSITION = 0.67;

    private  boolean intakeMode  = false;
    private final double launch_INCREMENT = 0.37; // Amount to increment/decrement
    private double launch_servoPosition = 0.17; // Initial servo position
    private final double launch_MIN_POSITION = .17;
    private final double launch_MAX_POSITION = .91;

    private final double intake_INCREMENT = 0.37; // Amount to increment/decrement
    private double intake_servoPosition = 0.07; // Initial servo position
    private final double intake_MIN_POSITION = .07;
    private final double intake_MAX_POSITION = .81;

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
        robotSensors.init(hardwareMap); // <-- Ensure color sensors are initialized
        follower = Constants.createFollower(hardwareMap);
        follower.setStartingPose(startingPose == null ? new Pose() : startingPose);
        follower.update();
        telemetryM = PanelsTelemetry.INSTANCE.getTelemetry();
    }

    @Override
    public void start() {
        follower.startTeleopDrive();

    }

    public void stop() {
        robotServos.colorservo.setPosition(0);
    }

    @Override
    public void loop() {
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
            if (!slowMode) follower.setTeleOpDrive(
                    -gamepad1.left_stick_y,
                    -gamepad1.left_stick_x,
                    -gamepad1.right_stick_x,
                    true // Robot Centric
            );
            else follower.setTeleOpDrive(
                    -gamepad1.left_stick_y * slowModeMultiplier,
                    -gamepad1.left_stick_x * slowModeMultiplier,
                    -gamepad1.right_stick_x * slowModeMultiplier,
                    true // Robot Centric
            );
        }

            robotMotors.turret.setPower(gamepad2.right_stick_y);
        if(!intakeMode) {
            robotServos.ballpush_4.setPosition(robotServos.ballpush_initial_position + (gamepad1.right_trigger * 0.27));
        }
        if(gamepad1.a && !prevAPressed){
            if(!intakeMode)
            {robotServos.turret_1.setPosition(intake_MIN_POSITION);
                robotServos.colorservo.setPosition(1);
            intakeMode = true;}
            else{
                robotServos.turret_1.setPosition(launch_MIN_POSITION);
                robotServos.colorservo.setPosition(.38);
            intakeMode = false;}
        }
        prevAPressed = gamepad1.a;


        if(!intakeMode) {
    if (gamepad1.dpad_left && !prevDpadLeftPressed) {
        launch_servoPosition -= launch_INCREMENT;
        if (launch_servoPosition < launch_MIN_POSITION) {
            launch_servoPosition = launch_MIN_POSITION;
        }
        if (robotServos.turret_1 != null) {
            robotServos.turret_1.setPosition(launch_servoPosition);
        } else {
            telemetry.addData("ERROR", "Servo 'ballpush_2' not found. Check hardwareMap name and configuration.");
        }
    }
    prevDpadLeftPressed = gamepad1.dpad_left;

    // Check if the dpad_down button is pressed
    if (gamepad1.dpad_right && !prevDpadRightPressed) {
        launch_servoPosition += launch_INCREMENT;
        if (launch_servoPosition > launch_MAX_POSITION) {
            launch_servoPosition = launch_MAX_POSITION;
        }
        if (robotServos.turret_1 != null) {
            robotServos.turret_1.setPosition(launch_servoPosition);
        } else {
            telemetry.addData("ERROR", "Servo 'ballpush_2' not found. Check hardwareMap name and configuration.");
        }
    }
    prevDpadRightPressed = gamepad1.dpad_right;

}   else
{
    if (gamepad1.dpad_left && !prevDpadLeftPressed) {
        intake_servoPosition -= intake_INCREMENT;
        if (intake_servoPosition < intake_MIN_POSITION) {
            intake_servoPosition = intake_MIN_POSITION;
        }
        if (robotServos.turret_1 != null) {
            robotServos.turret_1.setPosition(intake_servoPosition);
        } else {
            telemetry.addData("ERROR", "Servo 'ballpush_2' not found. Check hardwareMap name and configuration.");
        }
    }
    prevDpadLeftPressed = gamepad1.dpad_left;

    // Check if the dpad_down button is pressed
    if (gamepad1.dpad_right && !prevDpadRightPressed) {
        intake_servoPosition += intake_INCREMENT;
        if (intake_servoPosition > intake_MAX_POSITION) {
            intake_servoPosition = intake_MAX_POSITION;
        }
        if (robotServos.turret_1 != null) {
            robotServos.turret_1.setPosition(intake_servoPosition);
        } else {
            telemetry.addData("ERROR", "Servo 'ballpush_2' not found. Check hardwareMap name and configuration.");
        }
    }
    prevDpadRightPressed = gamepad1.dpad_right;

}


        // Color sensor detection with comprehensive info (FTC best practices)
        telemetry.addData("Color BR", robotSensors.getColorName(robotSensors.color_br));
        telemetry.addData("BR Info", robotSensors.getSensorInfo(robotSensors.color_br));

        telemetry.addData("Color BL", robotSensors.getColorName(robotSensors.color_bl));
        telemetry.addData("BL Info", robotSensors.getSensorInfo(robotSensors.color_bl));

        telemetry.addData("Color F", robotSensors.getColorName(robotSensors.color_f));
        telemetry.addData("F Info", robotSensors.getSensorInfo(robotSensors.color_f));

        telemetry.addData("Turret Servo Position", launch_servoPosition);
        telemetry.addData("Mode", intakeMode);
        telemetry.addData("Shooter Target Velocity", targetShooterVelocity);
        telemetry.addData("Shooter Current Velocity", robotMotors.getShooterVelocity());

        telemetry.update();
    }
}
