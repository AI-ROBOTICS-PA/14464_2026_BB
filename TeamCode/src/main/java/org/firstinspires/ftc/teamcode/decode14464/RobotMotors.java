package org.firstinspires.ftc.teamcode.decode14464;

import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.HardwareMap;

public class RobotMotors {
    public DcMotor intake = null;
    public DcMotorEx shooter = null;  // Changed to DcMotorEx for velocity control
    public DcMotorEx turret = null;


    public void init(HardwareMap hardwareMap) {
        intake = hardwareMap.get(DcMotor.class, "intake");
        shooter = hardwareMap.get(DcMotorEx.class, "shooter");  // Use DcMotorEx for velocity control
        turret = hardwareMap.get(DcMotorEx.class, "turret");


        // Configure shooter for optimal velocity control
        shooter.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        shooter.setMode(DcMotor.RunMode.RUN_USING_ENCODER);

        // Use BRAKE mode to help motor recover velocity faster after shooting
        shooter.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);

        // Add drivetrain motor initialization here if needed
    }

    /**
     * Start shooter at constant velocity (maintains speed during launches)
     * @param velocityTicksPerSecond Target velocity (e.g., 1400-1600 for typical shooter)
     */
    public void startShooter(double velocityTicksPerSecond) {
        shooter.setVelocity(velocityTicksPerSecond);
    }

    /**
     * Get current shooter velocity in ticks per second
     * @return Current velocity
     */
    public double getShooterVelocity() {
        return shooter.getVelocity();
    }

    /**
     * Stop shooter motor
     */
    public void stopShooter() {
        shooter.setVelocity(0);
    }

    /**
     * Check if shooter has reached target velocity (within tolerance)
     * @param targetVelocity Target velocity in ticks per second
     * @param tolerance Acceptable error (e.g., 50 ticks/sec)
     * @return true if within tolerance
     */
    public boolean isShooterAtVelocity(double targetVelocity, double tolerance) {
        double currentVelocity = shooter.getVelocity();
        return Math.abs(currentVelocity - targetVelocity) <= tolerance;
    }

    public void startIntake(){
        intake.setPower(.6);
    }

    public void stopIntake(){
        intake.setPower(0);
    }


}

