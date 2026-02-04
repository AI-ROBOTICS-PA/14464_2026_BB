package org.firstinspires.ftc.teamcode.decode14464.systems;

import com.qualcomm.robotcore.hardware.HardwareMap;
import org.firstinspires.ftc.teamcode.decode14464.RobotServos;

/**
 * Feature 6: LED/PWM Light System
 * Controls color servo for visual feedback based on robot mode
 * Uses centralized RobotServos
 */
public class LEDSystem {
    private RobotServos servos;
    private boolean isIntakeMode = false;

    /**
     * Initialize LED system with centralized servos
     */
    public void init(HardwareMap hardwareMap, RobotServos servos) {
        this.servos = servos;

        // Set initial color (launch mode)
        setLaunchModeColor();
    }

    /**
     * Set color for intake mode
     */
    public void setIntakeModeColor() {
        if (servos != null && servos.colorservo != null) {
            servos.colorservo.setPosition(RobotConstants.ColorServo.INTAKE_MODE_COLOR);
            isIntakeMode = true;
        }
    }

    /**
     * Set color for launch mode
     */
    public void setLaunchModeColor() {
        if (servos != null && servos.colorservo != null) {
            servos.colorservo.setPosition(RobotConstants.ColorServo.LAUNCH_MODE_COLOR);
            isIntakeMode = false;
        }
    }

    /**
     * Toggle between intake and launch mode colors
     */
    public void toggleMode() {
        if (isIntakeMode) {
            setLaunchModeColor();
        } else {
            setIntakeModeColor();
        }
    }

    /**
     * Set custom color position
     */
    public void setColor(double position) {
        if (servos != null && servos.colorservo != null) {
            servos.colorservo.setPosition(position);
        }
    }

    /**
     * Check if in intake mode
     */
    public boolean isIntakeMode() {
        return isIntakeMode;
    }
}
