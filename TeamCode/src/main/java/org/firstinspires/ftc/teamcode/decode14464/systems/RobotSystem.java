package org.firstinspires.ftc.teamcode.decode14464.systems;

import com.pedropathing.geometry.Pose;
import com.qualcomm.robotcore.hardware.HardwareMap;
import org.firstinspires.ftc.teamcode.decode14464.RobotMotors;

/**
 * Master Robot System
 * Integrates all subsystems for easy use in TeleOp and Auto programs
 */
public class RobotSystem {
    // All subsystems
    public LocalizationSystem localization;
    public AutoTurretSystem autoTurret;
    public AutoHoodSystem autoHood;
    public AutoIntakeSystem autoIntake;
    public ShootingSystem shooting;
    public LEDSystem led;
    public RobotMotors motors; // Added for direct motor control

    /**
     * Initialize all systems
     * @param hardwareMap The hardware map from OpMode
     * @param startPose Starting position for localization
     */
    public void init(HardwareMap hardwareMap, Pose startPose) {
        // Initialize systems in dependency order

        // 0. Robot Motors (for direct control)
        motors = new RobotMotors();
        motors.init(hardwareMap);

        // 1. Localization (no dependencies)
        localization = new LocalizationSystem();
        localization.init(hardwareMap, startPose);

        // 2. Auto-Intake (no dependencies)
        autoIntake = new AutoIntakeSystem();
        autoIntake.init(hardwareMap);

        // 3. LED (no dependencies)
        led = new LEDSystem();
        led.init(hardwareMap);

        // 4. Auto-Turret (depends on localization)
        autoTurret = new AutoTurretSystem();
        autoTurret.init(hardwareMap, localization);

        // 5. Auto-Hood (depends on localization)
        autoHood = new AutoHoodSystem();
        autoHood.init(hardwareMap, localization);

        // 6. Shooting (depends on localization and autoIntake)
        shooting = new ShootingSystem();
        shooting.init(hardwareMap, autoIntake, localization);

        // Sync LED with intake mode
        led.setLaunchModeColor(); // Start in launch mode
    }

    /**
     * Update all systems - call this every loop
     */
    public void updateAll() {
        // Update in correct order
        localization.update();           // Update position first
        autoTurret.update();             // Then auto-aim turret
        autoHood.update();               // And adjust hood
        autoIntake.update();             // Handle auto-advancement
        shooting.update();               // Process shooting sequence

        // Sync LED with intake mode
        led.syncWithIntakeMode(autoIntake.isIntakeMode());
    }

    /**
     * Stop all systems (for OpMode stop())
     */
    public void stopAll() {
        if (autoTurret != null) autoTurret.stop();
        if (shooting != null) shooting.stopShooter();
        if (shooting != null) shooting.stopAutoShoot();
        if (autoIntake != null) autoIntake.stopAll(); // Stop intake motor through AutoIntakeSystem
        if (motors != null) motors.stopShooter();
        if (led != null) led.turnOff();
    }

    /**
     * Enable full auto mode (turret + hood auto-adjust)
     */
    public void enableFullAuto() {
        if (autoTurret != null) autoTurret.enable();
        if (autoHood != null) autoHood.enableAutoAdjust();
    }

    /**
     * Disable full auto mode
     */
    public void disableFullAuto() {
        if (autoTurret != null) autoTurret.disable();
        if (autoHood != null) autoHood.disableAutoAdjust();
    }

    /**
     * Quick access: Get distance to goal
     */
    public double getDistanceToGoal() {
        return localization != null ?
            localization.getDistanceToPoint(RobotConstants.Field.GOAL_X, RobotConstants.Field.GOAL_Y) : 0;
    }

    /**
     * Quick access: Get angle to goal
     */
    public double getAngleToGoal() {
        return localization != null ?
            localization.getAngleToPoint(RobotConstants.Field.GOAL_X, RobotConstants.Field.GOAL_Y) : 0;
    }

    /**
     * Quick access: Check if ready to shoot (turret on target, shooter at speed)
     */
    public boolean isReadyToShoot() {
        boolean turretReady = autoTurret != null && autoTurret.isOnTarget();
        boolean shooterReady = shooting != null && shooting.isShooterAtTargetVelocity();
        return turretReady && shooterReady;
    }

    /**
     * Quick access: Shoot from anywhere (enable auto, start shooter, wait, shoot)
     */
    public void shootFromAnywhere() {
        enableFullAuto();

        if (shooting != null) {
            shooting.startShooterAuto(); // Start with distance-based velocity
            // Trigger shooting when ready (check in your loop)
        }
    }
}

