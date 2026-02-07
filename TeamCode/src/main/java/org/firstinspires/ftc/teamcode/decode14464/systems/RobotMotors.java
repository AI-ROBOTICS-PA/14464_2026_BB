package org.firstinspires.ftc.teamcode.decode14464.systems;

import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.hardware.PIDFCoefficients;

public class RobotMotors {
    public DcMotor intake = null;
    public DcMotorEx flywheelMotor = null;  // Changed to DcMotorEx for velocity control
    public DcMotorEx turret = null;
    
    // ========== TURRET POSITION PERSISTENCE ==========
    // Static variable to persist turret position between OpModes (Auto → TeleOp)
    // This allows TeleOp to know where the turret ended up after Auto
    private static int savedTurretPosition = 0;
    private static boolean hasSavedPosition = false;
    
    /**
     * Save current turret position for use in next OpMode
     * Call this at the END of Autonomous before stopping
     */
    public void saveTurretPosition() {
        if (turret != null) {
            savedTurretPosition = turret.getCurrentPosition();
            hasSavedPosition = true;
        }
    }
    
    /**
     * Check if there's a saved turret position from previous OpMode
     */
    public static boolean hasSavedTurretPosition() {
        return hasSavedPosition;
    }
    
    /**
     * Get the saved turret position (in encoder ticks)
     */
    public static int getSavedTurretPosition() {
        return savedTurretPosition;
    }
    
    /**
     * Clear the saved position (use when you want to reset)
     */
    public static void clearSavedTurretPosition() {
        savedTurretPosition = 0;
        hasSavedPosition = false;
    }

    /**
     * Standard init - resets turret encoder to 0
     * Use for: First run of the day, or when turret is at known 0 position
     */
    public void init(HardwareMap hardwareMap) {
        init(hardwareMap, true); // Default: reset encoder
    }
    
    /**
     * Init with option to preserve turret encoder position
     * @param hardwareMap Hardware map
     * @param resetTurretEncoder If false, preserves turret position from Auto
     */
    public void init(HardwareMap hardwareMap, boolean resetTurretEncoder) {
        // Initialize intake motor
        intake = hardwareMap.get(DcMotor.class, "intake");
        intake.setDirection(DcMotor.Direction.REVERSE);

        // Initialize turret motor with proper configuration
        turret = hardwareMap.get(DcMotorEx.class, "turret");
        
        if (resetTurretEncoder) {
            // Reset encoder to 0 (normal init)
            turret.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        }
        // If not resetting, encoder keeps its current value
        
        turret.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        turret.setDirection(DcMotor.Direction.FORWARD);
        turret.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);

        // Initialize the flywheel motor (using your "shooter" motor)
        flywheelMotor = hardwareMap.get(DcMotorEx.class, "shooter");
        flywheelMotor.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        flywheelMotor.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        flywheelMotor.setDirection(DcMotor.Direction.FORWARD); // Change to REVERSE if needed
        flywheelMotor.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);

        // Set initial PIDF coefficients (P, I, D, F)
        PIDFCoefficients pidfCoefficients = new PIDFCoefficients(300, 0, 0, 13.65);
        flywheelMotor.setPIDFCoefficients(DcMotor.RunMode.RUN_USING_ENCODER, pidfCoefficients);
    }

    /**
     * Start shooter at constant velocity (maintains speed during launches)
     * @param curTargetVelocity Target velocity (e.g., 1400-1600 for typical shooter)
     */
    public void startShooter(double curTargetVelocity) {
        flywheelMotor.setVelocity(curTargetVelocity);
    }

    /**
     * Get current shooter velocity in ticks per second
     * @return Current velocity
     */
    public double getShooterVelocity() {
        return flywheelMotor.getVelocity();
    }

    /**
     * Stop shooter motor
     */
    public void stopShooter() {
        flywheelMotor.setVelocity(0);
    }

    /**
     * Check if shooter has reached target velocity (within tolerance)
     * @param targetVelocity Target velocity in ticks per second
     * @param tolerance Acceptable error (e.g., 50 ticks/sec)
     * @return true if within tolerance
     */
    public boolean isShooterAtVelocity(double targetVelocity, double tolerance) {
        double currentVelocity = flywheelMotor.getVelocity();
        return Math.abs(currentVelocity - targetVelocity) <= tolerance;
    }

    public void startIntake(){
        intake.setPower(1);
    }

    public void stopIntake(){
        intake.setPower(0);
    }

    /**
     * Set intake motor power (allows variable speed and reverse for outtake)
     * @param power Motor power (-1.0 to 1.0, positive = intake, negative = outtake)
     */
    public void setIntakePower(double power) {
        intake.setPower(power);
    }


}
