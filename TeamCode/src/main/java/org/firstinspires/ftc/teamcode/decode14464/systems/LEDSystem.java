package org.firstinspires.ftc.teamcode.decode14464.systems;

import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.hardware.Servo;

/**
 * Feature 6: LED/PWM Light System
 * Controls color servo for visual feedback based on robot mode
 */
public class LEDSystem {
    private Servo colorServo;
    private boolean isIntakeMode = false;

    /**
     * Initialize LED system
     */
    public void init(HardwareMap hardwareMap) {
        colorServo = hardwareMap.get(Servo.class, "colorservo");

        // Set initial color (launch mode)
        setLaunchModeColor();
    }

    /**
     * Set color for intake mode
     */
    public void setIntakeModeColor() {
        if (colorServo != null) {
            colorServo.setPosition(RobotConstants.ColorServo.INTAKE_MODE_COLOR);
            isIntakeMode = true;
        }
    }

    /**
     * Set color for launch mode
     */
    public void setLaunchModeColor() {
        if (colorServo != null) {
            colorServo.setPosition(RobotConstants.ColorServo.LAUNCH_MODE_COLOR);
            isIntakeMode = false;
        }
    }

    /**
     * Set neutral color
     */
    public void setNeutralColor() {
        if (colorServo != null) {
            colorServo.setPosition(RobotConstants.ColorServo.NEUTRAL_COLOR);
        }
    }

    /**
     * Turn off LED
     */
    public void turnOff() {
        if (colorServo != null) {
            colorServo.setPosition(RobotConstants.ColorServo.OFF);
        }
    }

    /**
     * Set custom color (0.0 to 1.0)
     */
    public void setCustomColor(double position) {
        if (colorServo != null) {
            colorServo.setPosition(position);
        }
    }

    /**
     * Sync with intake system mode
     */
    public void syncWithIntakeMode(boolean intakeMode) {
        if (intakeMode) {
            setIntakeModeColor();
        } else {
            setLaunchModeColor();
        }
    }

    /**
     * Check if in intake mode
     */
    public boolean isIntakeMode() {
        return isIntakeMode;
    }
}

