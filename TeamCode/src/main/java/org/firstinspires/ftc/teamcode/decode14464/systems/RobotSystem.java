package org.firstinspires.ftc.teamcode.decode14464.systems;

import com.pedropathing.geometry.Pose;
import com.qualcomm.robotcore.hardware.HardwareMap;

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
    public AutoShootSystem autoShoot; // Reusable auto-shoot system
    public LEDSystem led;
    public RobotMotors motors; // Direct motor control
    public RobotSensors sensors; // Centralized sensor management
    public RobotServos servos; // Centralized servo management
    public LimelightManager limelight; // Limelight camera for AprilTag detection

    /**
     * Initialize all systems FOR AUTO PROGRAMS
     * Sets a specific starting position
     * @param hardwareMap The hardware map from OpMode
     * @param startPose Starting position for localization
     */
    public void init(HardwareMap hardwareMap, Pose startPose) {
        // Initialize systems in dependency order

        // 0. Robot Motors, Sensors, and Servos (centralized hardware)
        motors = new RobotMotors();
        motors.init(hardwareMap);

        sensors = new RobotSensors();
        sensors.init(hardwareMap);

        servos = new RobotServos();
        servos.init(hardwareMap);

        // 1. Localization (no dependencies)
        localization = new LocalizationSystem();
        localization.init(hardwareMap, startPose);

        // 2. Auto-Intake (depends on sensors, motors, servos)
        autoIntake = new AutoIntakeSystem();
        autoIntake.init(hardwareMap, sensors, motors, servos);

        // 3. LED (depends on servos)
        led = new LEDSystem();
        led.init(hardwareMap, servos);

        // 4. Auto-Turret (depends on localization, motors, autoIntake for launch-mode-only)
        autoTurret = new AutoTurretSystem();
        autoTurret.init(hardwareMap, localization, motors, autoIntake);

        // 5. Auto-Hood (depends on localization, servos)
        autoHood = new AutoHoodSystem();
        autoHood.init(hardwareMap, localization, servos);

        // 6. Shooting (depends on localization, autoIntake, motors, servos)
        shooting = new ShootingSystem();
        shooting.init(hardwareMap, autoIntake, localization, motors, servos);

        // 7. Auto-Shoot (depends on motors, servos, sensors, autoIntake)
        autoShoot = new AutoShootSystem();
        autoShoot.init(hardwareMap, motors, servos, sensors, autoIntake);

        // 8. Limelight Manager (for AprilTag detection and calibration)
        limelight = new LimelightManager();
        limelight.init(hardwareMap);

        // Sync LED with intake mode
        led.setLaunchModeColor(); // Start in launch mode
    }

    /**
     * Initialize for TeleOp - PRESERVES position from Auto
     * Use this in TeleOp programs to maintain position continuity from Auto period
     * @param hardwareMap The hardware map from OpMode
     */
    public void initTeleOp(HardwareMap hardwareMap) {
        // Initialize hardware
        motors = new RobotMotors();
        motors.init(hardwareMap);

        sensors = new RobotSensors();
        sensors.init(hardwareMap);

        servos = new RobotServos();
        servos.init(hardwareMap);

        // Localization - will preserve existing position from Auto
        localization = new LocalizationSystem();
        localization.initTeleOp(hardwareMap); // Use TeleOp init to preserve position

        // 2. Auto-Intake (depends on sensors, motors, servos)
        autoIntake = new AutoIntakeSystem();
        autoIntake.init(hardwareMap, sensors, motors, servos);

        // 3. LED (depends on servos)
        led = new LEDSystem();
        led.init(hardwareMap, servos);

        // 4. Auto-Turret (depends on localization, motors, autoIntake for launch-mode-only)
        autoTurret = new AutoTurretSystem();
        autoTurret.init(hardwareMap, localization, motors, autoIntake);

        // 5. Auto-Hood (depends on localization, servos)
        autoHood = new AutoHoodSystem();
        autoHood.init(hardwareMap, localization, servos);

        // 6. Shooting (depends on localization, autoIntake, motors, servos)
        shooting = new ShootingSystem();
        shooting.init(hardwareMap, autoIntake, localization, motors, servos);

        // 7. Auto-Shoot (depends on motors, servos, sensors, autoIntake)
        autoShoot = new AutoShootSystem();
        autoShoot.init(hardwareMap, motors, servos, sensors, autoIntake);

        // 8. Limelight Manager (for AprilTag detection and calibration)
        limelight = new LimelightManager();
        limelight.init(hardwareMap);

        // Sync LED with intake mode
        led.setLaunchModeColor(); // Start in launch mode
    }

    /**
     * Update all systems - call this every loop
     */
    public void updateAll() {
        // Update in correct order
        localization.update();           // Update position first
        if (limelight != null) limelight.update(); // Update Limelight data
        autoTurret.update();             // Then auto-aim turret
        autoHood.update();               // And adjust hood
        autoIntake.update();             // Handle auto-advancement
        shooting.update();               // Process shooting sequence
        autoShoot.update();              // Update auto-shoot state machine
        
        // Sync LED with intake mode
        if (led != null && autoIntake != null) {
            led.syncWithIntakeMode(autoIntake.isIntakeMode());
        }
    }

    /**
     * Stop all systems (for OpMode stop())
     */
    public void stopAll() {
        if (autoTurret != null) autoTurret.stop();
        if (shooting != null) shooting.stopShooter();
        if (autoShoot != null) autoShoot.stop();
        if (autoIntake != null) autoIntake.stopAll();
        if (led != null) led.turnOff();
        if (limelight != null) limelight.stop();
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
     * Note: If autoTurret is disabled, we assume turret is manually positioned and ready
     */
    public boolean isReadyToShoot() {
        boolean turretReady;
        if (autoTurret != null && autoTurret.isAnyModeEnabled()) {
            // Auto-turret is active (any mode) - check if on target
            turretReady = autoTurret.isOnTarget();
        } else {
            // Auto-turret disabled or not initialized - assume manually ready
            turretReady = true;
        }
        boolean shooterReady = shooting != null && shooting.isShooterAtTargetVelocity();
        return turretReady && shooterReady;
    }

    /**
     * Quick access: Shoot from anywhere (enable auto, start shooter)
     */
    public void shootFromAnywhere() {
        enableFullAuto();
        if (shooting != null) {
            shooting.startShooterAuto(); // Start with distance-based velocity
        }
    }
}

