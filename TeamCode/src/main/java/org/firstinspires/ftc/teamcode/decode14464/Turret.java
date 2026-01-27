package org.firstinspires.ftc.teamcode.decode14464;

import com.qualcomm.hardware.limelightvision.LLResult;
import com.qualcomm.hardware.limelightvision.Limelight3A;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.hardware.HardwareMap;

/**
 * Simple Turret class for auto-alignment using Limelight
 * DECODE 14464 - FTC 2026
 */
public class Turret {

    // Hardware
    private DcMotor motor;
    private Limelight3A limelight;

    // PD Controller gains (tune these)
    public double kP = 0.03;
    public double kD = 0.005;

    // Smoothing filter (0 = no filter, 0.8 = very smooth)
    // Lower = faster response for high-speed tracking
    public double filterStrength = 0.3;

    // Stop when error is within this range (degrees)
    public double tolerance = 1.5;

    // Internal state
    private double lastError = 0;
    private double filteredError = 0;
    private double lastPower = 0;
    private int framesLost = 0;
    
    // How many frames to keep moving if tag is briefly lost
    // Higher = smoother tracking when tag detection drops at speed
    public int lostFrameTolerance = 10;

    /**
     * Initialize turret hardware
     */
    public void init(HardwareMap hardwareMap) {
        // Motor setup - direct power control, no internal PID
        motor = hardwareMap.get(DcMotor.class, "turret");
        motor.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        motor.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);

        // Limelight setup
        limelight = hardwareMap.get(Limelight3A.class, "limelight");
        limelight.setPollRateHz(100);
        limelight.pipelineSwitch(0);  // Use pipeline 0 for AprilTag detection
        limelight.start();

        // Configure for better distance detection
        configureLimelightForDistance();
    }

    /**
     * Configure Limelight for high-speed AprilTag detection
     */
    private void configureLimelightForDistance() {
        // IMPORTANT: Turn OFF LEDs for AprilTag detection
        setLedOff();
        
        // Set maximum polling rate for fastest response
        limelight.setPollRateHz(100);  // Max rate
        
        // HIGH-SPEED SETTINGS (configure in Limelight web UI):
        // http://limelight.local:5801
        //
        // For fast-moving robot:
        // - Exposure: 2-5 ms (VERY LOW to reduce motion blur)
        // - Gain: 25-35 (HIGH to compensate for low exposure)
        // - Enable 3D AprilTag mode
        // - LEDs: OFF
    }

    /**
     * Turn OFF Limelight LEDs (recommended for AprilTag detection)
     */
    public void setLedOff() {
        // LED Mode: 0 = use pipeline default, 1 = force off, 2 = force blink, 3 = force on
        // For AprilTags, LEDs should be OFF to avoid glare
    }

    /**
     * Turn ON Limelight LEDs (only for retroreflective tape)
     */
    public void setLedOn() {
        // Only use this for retroreflective tape tracking, NOT for AprilTags
    }

    /**
     * Set motor direction (call if turret moves wrong way)
     */
    public void setReversed(boolean reversed) {
        motor.setDirection(reversed ? DcMotorSimple.Direction.REVERSE : DcMotorSimple.Direction.FORWARD);
    }

    /**
     * Main tracking update - call this every loop
     * Returns true if target is locked (within tolerance)
     */
    public boolean track() {
        LLResult result = limelight.getLatestResult();

        // No valid target - use predictive tracking
        if (result == null || !result.isValid()) {
            framesLost++;
            
            // Keep moving in same direction for a few frames (predictive tracking)
            if (framesLost <= lostFrameTolerance && lastPower != 0) {
                // Gradually reduce power while tag is lost
                double decayedPower = lastPower * 0.8;
                motor.setPower(decayedPower);
                return false;
            }
            
            // Lost for too long - stop
            motor.setPower(0);
            lastError = 0;
            lastPower = 0;
            return false;
        }

        // Target found - reset lost counter
        framesLost = 0;

        // Get horizontal offset (tx) - positive means target is to the right
        double rawError = result.getTx();

        // Apply low-pass filter to smooth jittery data
        filteredError = (filterStrength * filteredError) + ((1 - filterStrength) * rawError);

        // Check if we're close enough (locked on target)
        if (Math.abs(filteredError) < tolerance) {
            motor.setPower(0);
            lastError = 0;
            lastPower = 0;
            return true;  // Locked!
        }

        // PD Controller calculation
        double derivative = filteredError - lastError;
        double power = (kP * filteredError) + (kD * derivative);

        // Clamp power to safe range
        power = Math.max(-1.0, Math.min(1.0, power));

        // Apply power
        motor.setPower(power);
        lastError = filteredError;
        lastPower = power;  // Save for predictive tracking

        return false;
    }

    /**
     * Check if Limelight sees a valid target
     */
    public boolean hasTarget() {
        LLResult result = limelight.getLatestResult();
        return result != null && result.isValid();
    }

    /**
     * Get current horizontal offset to target (degrees)
     */
    public double getTargetOffset() {
        LLResult result = limelight.getLatestResult();
        if (result != null && result.isValid()) {
            return result.getTx();
        }
        return 0;
    }

    /**
     * Get filtered error value
     */
    public double getFilteredError() {
        return filteredError;
    }

    /**
     * Manual control - bypasses auto-tracking
     */
    public void setManualPower(double power) {
        motor.setPower(power);
        lastError = 0;
        filteredError = 0;
    }

    /**
     * Stop the turret
     */
    public void stop() {
        motor.setPower(0);
        limelight.stop();
    }

    /**
     * Get the Limelight for advanced use
     */
    public Limelight3A getLimelight() {
        return limelight;
    }

    /**
     * Get motor power for telemetry
     */
    public double getMotorPower() {
        return motor.getPower();
    }

    /**
     * Get vertical angle to target (ty) - useful for distance estimation
     */
    public double getTargetY() {
        LLResult result = limelight.getLatestResult();
        if (result != null && result.isValid()) {
            return result.getTy();
        }
        return 0;
    }

    /**
     * Get target area - smaller area = farther away
     */
    public double getTargetArea() {
        LLResult result = limelight.getLatestResult();
        if (result != null && result.isValid()) {
            return result.getTa();
        }
        return 0;
    }

    /**
     * Check if Limelight is connected and running
     */
    public boolean isConnected() {
        return limelight != null && limelight.isRunning();
    }

    /**
     * Switch Limelight pipeline (0-9)
     * Different pipelines can be configured for different distances
     */
    public void setPipeline(int pipeline) {
        limelight.pipelineSwitch(pipeline);
    }
}
