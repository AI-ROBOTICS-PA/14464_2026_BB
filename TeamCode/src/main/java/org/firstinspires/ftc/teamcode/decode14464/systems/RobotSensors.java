package org.firstinspires.ftc.teamcode.decode14464.systems;

import android.graphics.Color;
import com.qualcomm.robotcore.hardware.DistanceSensor;
import com.qualcomm.robotcore.hardware.NormalizedColorSensor;
import com.qualcomm.robotcore.hardware.NormalizedRGBA;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.hardware.TouchSensor;
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;
import org.firstinspires.ftc.teamcode.decode14464.systems.RobotConstants;

public class RobotSensors {
    public NormalizedColorSensor color_br;
    public NormalizedColorSensor color_bl;
    public NormalizedColorSensor color_f;

    // Touch sensor for ball push detection (Digital Port 1, 3, 5, or 7 only!)
    public TouchSensor ballPushSensor;

    // FTC Best Practice: Configurable parameters
    private static final float GAIN = 2.0f;  // Software gain (adjust for lighting)
    private static final float MIN_SATURATION = 0.25f;  // Minimum saturation to trust color (lowered for real-world readings)
    private static final double MAX_DISTANCE_CM = 5.0;  // Maximum distance for accurate reading (increased from 2cm for reliability)

    public void init(HardwareMap hardwareMap) {
        // Use NormalizedColorSensor for better reliability
        color_br = hardwareMap.get(NormalizedColorSensor.class, "color_br");
        color_bl = hardwareMap.get(NormalizedColorSensor.class, "color_bl");
        color_f = hardwareMap.get(NormalizedColorSensor.class, "color_f");

        // Apply gain to all sensors (FTC Best Practice)
        if (color_br != null) color_br.setGain(GAIN);
        if (color_bl != null) color_bl.setGain(GAIN);
        if (color_f != null) color_f.setGain(GAIN);

        // Initialize touch sensor for ball push detection
        // NOTE: REV Touch Sensors must be on Digital Port 1, 3, 5, or 7 ONLY!
        ballPushSensor = hardwareMap.get(TouchSensor.class, RobotConstants.BallPush.TOUCH_SENSOR_NAME);
    }

    /**
     * Detect color using FTC best practices:
     * 1. Check proximity (sensor must be within 2cm)
     * 2. Validate saturation (must be > 0.45 to trust color)
     * 3. Use Hue from HSV conversion for color identification
     *
     * Hue ranges (0-360 degrees):
     * Red: 0-30 or 330-360
     * Yellow: 30-90
     * Green: 90-160
     * Cyan: 160-210
     * Blue: 210-270
     * Magenta: 270-330
     */
    public String getColorName(NormalizedColorSensor sensor) {
        if (sensor == null) return "Unknown";

        // Step 1: Check proximity (REV Color Sensor V3 is most accurate within 2cm)
        // Note: We check but don't strictly enforce to avoid always returning "Too Far"
        boolean isTooFar = false;
        if (sensor instanceof DistanceSensor) {
            DistanceSensor distanceSensor = (DistanceSensor) sensor;
            double distance = distanceSensor.getDistance(DistanceUnit.CM);
            if (distance > MAX_DISTANCE_CM) {
                isTooFar = true;
                // Don't return here - proceed with detection but user can check telemetry
            }
        }

        // Step 2: Get NormalizedRGBA values (scaled 0-1)
        NormalizedRGBA colors = sensor.getNormalizedColors();

        // Check if sensor is reading anything meaningful
        if (colors.alpha < 0.02f) return "None";

        // Step 3: Convert to HSV for reliable color detection
        float[] hsv = new float[3];
        Color.colorToHSV(colors.toColor(), hsv);

        float hue = hsv[0];        // 0-360 degrees
        float saturation = hsv[1]; // 0-1
        float value = hsv[2];      // 0-1

        // Step 4: Validate saturation FIRST (FTC Best Practice)
        // High saturation means it's a vibrant color even if value is low
        // Low saturation means gray/white/black
        if (saturation < MIN_SATURATION) {
            // Only check value if saturation is low
            if (value < 0.15f) return "Black";
            if (value > 0.70f) return "White";
            return "Gray";
        }

        // Step 5: If saturation is good, use Hue to identify color
        // (even if value is very low like 0.01-0.02)
        // RED: 0-30 or 330-360 degrees
        if (hue >= 330 || hue < 30) {
            return "Red";
        }

        // YELLOW: 30-90 degrees
        if (hue >= 30 && hue < 90) {
            return "Yellow";
        }

        // GREEN: 90-160 degrees (expanded to capture FTC green artifacts)
        if (hue >= 90 && hue < 160) {
            return "Green";
        }

        // CYAN: 160-210 degrees
        if (hue >= 160 && hue < 210) {
            return "Cyan";
        }

        // BLUE: 210-270 degrees
        if (hue >= 210 && hue < 270) {
            return "Blue";
        }

        // MAGENTA: 270-330 degrees
        if (hue >= 270 && hue < 330) {
            return "Magenta";
        }

        return "Unknown";
    }

    /**
     * Get Hue value for debugging and calibration
     */
    public float getHue(NormalizedColorSensor sensor) {
        if (sensor == null) return -1;

        NormalizedRGBA colors = sensor.getNormalizedColors();
        float[] hsv = new float[3];
        Color.colorToHSV(colors.toColor(), hsv);

        return hsv[0];
    }

    /**
     * Get saturation for debugging
     */
    public float getSaturation(NormalizedColorSensor sensor) {
        if (sensor == null) return -1;

        NormalizedRGBA colors = sensor.getNormalizedColors();
        float[] hsv = new float[3];
        Color.colorToHSV(colors.toColor(), hsv);

        return hsv[1];
    }

    /**
     * Get distance from sensor (if supported)
     */
    public double getDistance(NormalizedColorSensor sensor) {
        if (sensor == null) return -1;

        if (sensor instanceof DistanceSensor) {
            DistanceSensor distanceSensor = (DistanceSensor) sensor;
            return distanceSensor.getDistance(DistanceUnit.CM);
        }

        return -1; // Distance not supported
    }

    /**
     * Get all RGB and alpha values as a formatted string for debugging
     */
    public String getRGBAString(NormalizedColorSensor sensor) {
        if (sensor == null) return "N/A";

        NormalizedRGBA colors = sensor.getNormalizedColors();
        return String.format("R:%.2f G:%.2f B:%.2f A:%.2f",
            colors.red, colors.green, colors.blue, colors.alpha);
    }

    /**
     * Get HSV values as a formatted string for debugging and calibration
     */
    public String getHSVString(NormalizedColorSensor sensor) {
        if (sensor == null) return "N/A";

        NormalizedRGBA colors = sensor.getNormalizedColors();
        float[] hsv = new float[3];
        Color.colorToHSV(colors.toColor(), hsv);

        return String.format("H:%.0f S:%.2f V:%.2f", hsv[0], hsv[1], hsv[2]);
    }

    /**
     * Check if ball push servo is fully retracted using touch sensor
     * @return true if ball push is retracted (sensor pressed), false otherwise
     */
    public boolean isBallPushRetracted() {
        if (ballPushSensor != null) {
            return ballPushSensor.isPressed();
        }
        return false; // If no sensor, assume not retracted
    }

    /**
     * Get comprehensive sensor info for debugging
     */
    public String getSensorInfo(NormalizedColorSensor sensor) {
        if (sensor == null) return "N/A";

        double distance = getDistance(sensor);
        String distStr = (distance >= 0) ? String.format("%.1fcm", distance) : "N/A";

        return String.format("%s | Dist:%s", getHSVString(sensor), distStr);
    }
}
