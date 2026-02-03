# 🤖 Modular Robot Systems Architecture

## Overview
This folder contains a clean, modular implementation of all robot features that can be reused across TeleOp and Autonomous programs.

## 📁 System Modules

### 1. **RobotConstants.java**
Unified constants file containing ALL tunable values:
- Field dimensions and goal position
- Turret PID constants and limits
- Hood servo positions and distance mapping
- Turret servo positions (intake/launch modes)
- Ball push servo timing
- LED/PWM light colors
- Shooter velocities and PIDF coefficients
- Intake power levels
- Color sensor thresholds
- Auto-shooting trajectory parameters
- Sequence timing values

### 2. **LocalizationSystem.java** (Feature 1)
**Purpose**: Robot always knows its position and heading

**Methods**:
- `init()` - Initialize Pedro Pathing
- `update()` - Update localization (call every loop)
- `getPose()` - Get current robot pose
- `getX()`, `getY()` - Get position in inches
- `getHeadingDegrees()` - Get heading (0-360°)
- `getDistanceToPoint(x, y)` - Calculate distance to target
- `getAngleToPoint(x, y)` - Calculate angle to target
- `setPose()` - Reset/calibrate position

**Usage**:
```java
localization.update();
double x = localization.getX();
double heading = localization.getHeadingDegrees();
double distToGoal = localization.getDistanceToPoint(GOAL_X, GOAL_Y);
```

### 3. **AutoTurretSystem.java** (Feature 2)
**Purpose**: Automatically point turret motor toward goal

**Methods**:
- `init()` - Initialize turret motor
- `enable()` / `disable()` / `toggle()` - Control auto-tracking
- `update()` - Run PID control (call every loop)
- `setManualPower()` - Manual control (disables auto)
- `getTurretAngle()` - Get angle relative to robot
- `getDistanceToGoal()` - Get distance to goal
- `isOnTarget()` - Check if turret is aimed correctly
- `stop()` - Stop turret motor

**Features**:
- PD control for smooth tracking
- Deadband to prevent jitter
- Power clamping for safety
- Uses localization for field-aware aiming

**Usage**:
```java
autoTurret.enable();
autoTurret.update(); // In loop
if (autoTurret.isOnTarget()) {
    // Ready to shoot!
}
```

### 4. **AutoHoodSystem.java** (Feature 3)
**Purpose**: Automatically adjust hood angle based on distance

**Methods**:
- `init()` - Initialize hood servo
- `enableAutoAdjust()` / `disableAutoAdjust()` / `toggleAutoAdjust()`
- `update()` - Adjust hood based on distance (call every loop)
- `setLongRange()` / `setShortRange()` / `setHome()` - Presets
- `setPosition()` - Manual position
- `increment()` / `decrement()` - Fine adjustments
- `getDistanceToGoal()` - Get distance to goal

**Features**:
- Linear interpolation for smooth distance mapping
- Preset positions for quick switching
- Manual override capability
- Auto-clamping to safe servo range

**Usage**:
```java
autoHood.enableAutoAdjust();
autoHood.update(); // Automatically adjusts
// OR
autoHood.setLongRange(); // Manual preset
```

### 5. **AutoIntakeSystem.java** (Feature 4)
**Purpose**: Automatically advance turret servo when ball detected

**Methods**:
- `init()` - Initialize turret servo and color sensor
- `update()` - Handle color detection and auto-advance (call every loop)
- `setIntakeMode()` / `setLaunchMode()` / `toggleMode()`
- `advanceToNextSlot()` - Manual advance
- `moveToPreviousSlot()` - Manual reverse
- `isBallDetected()` - Check if ball present
- `getDetectedColor()` - Get color name
- `getCurrentIndex()` - Get current slot (0-2)

**Features**:
- Transition detection (no ball → ball)
- Prevents multiple triggers on same ball
- Automatic slot progression
- Manual override available
- Always starts at slot 0 in intake mode

**Usage**:
```java
autoIntake.setIntakeMode();
autoIntake.update(); // Auto-advances on ball detection
// Current slot: autoIntake.getCurrentIndex()
```

### 6. **ShootingSystem.java** (Features 5 & 7)
**Purpose**: Manage shooter motor and automatic shooting sequence

**Methods**:
- `init()` - Initialize flywheel, intake, ball push servo
- `update()` - Run shooting state machine (call every loop)
- `startShooter(velocity)` - Start at specific velocity
- `startShooterAuto()` - Start with distance-based velocity
- `stopShooter()` - Stop flywheel
- `triggerAutoShoot()` - Start 3-ball shooting sequence
- `stopAutoShoot()` - Cancel sequence
- `isShooterAtTargetVelocity()` - Check if ready
- `getCurrentVelocity()` - Get flywheel speed
- `isAutoShootingInProgress()` - Check sequence status
- `pushBall()` / `retractBall()` - Manual ball push control

**Features**:
- State machine for 3-ball sequence (spin up → push → retract → repeat)
- Distance-based velocity calculation (Feature 7)
- Velocity tolerance checking
- Automatic timing between balls
- Integration with intake system for slot advancement

**Shooting Sequence**:
1. SPIN_UP - Wait for shooter to reach velocity
2. PUSH_BALL_1 - Push first ball (200ms)
3. RETRACT_1 - Retract and advance to next slot (300ms)
4. PUSH_BALL_2 - Push second ball
5. RETRACT_2 - Retract and advance
6. PUSH_BALL_3 - Push third ball
7. RETRACT_3 - Retract and finish
8. COMPLETE - Sequence done

**Usage**:
```java
shooting.startShooterAuto(); // Distance-based velocity
if (shooting.isShooterAtTargetVelocity()) {
    shooting.triggerAutoShoot(); // Shoot all 3 balls
}
```

### 7. **LEDSystem.java** (Feature 6)
**Purpose**: Control PWM light color based on robot mode

**Methods**:
- `init()` - Initialize color servo
- `setIntakeModeColor()` - Blue/intake color
- `setLaunchModeColor()` - Red/launch color
- `setNeutralColor()` - Neutral
- `turnOff()` - Lights off
- `setCustomColor(position)` - Custom color
- `syncWithIntakeMode(boolean)` - Auto-sync with intake

**Usage**:
```java
led.syncWithIntakeMode(autoIntake.isIntakeMode());
// Automatically sets correct color
```

### 8. **RobotSystem.java** (Master Integration)
**Purpose**: Integrate all subsystems into one easy-to-use class

**Public Members**:
- `localization` - LocalizationSystem
- `autoTurret` - AutoTurretSystem
- `autoHood` - AutoHoodSystem
- `autoIntake` - AutoIntakeSystem
- `shooting` - ShootingSystem
- `led` - LEDSystem

**Methods**:
- `init(hardwareMap, startPose)` - Initialize ALL systems
- `updateAll()` - Update ALL systems (call every loop)
- `stopAll()` - Stop ALL systems (for OpMode stop())
- `enableFullAuto()` - Enable turret + hood auto
- `disableFullAuto()` - Disable turret + hood auto
- `getDistanceToGoal()` - Quick distance access
- `getAngleToGoal()` - Quick angle access
- `isReadyToShoot()` - Check if turret + shooter ready
- `shootFromAnywhere()` - Feature 8 - Enable full auto and shoot

**Usage**:
```java
robot = new RobotSystem();
robot.init(hardwareMap, new Pose(0, 0, 0));

// In loop:
robot.updateAll();

// Access any subsystem:
robot.autoTurret.enable();
robot.shooting.triggerAutoShoot();
```

## 🎯 Feature Implementation Summary

| # | Feature | Module(s) | Status |
|---|---------|-----------|--------|
| 1 | Position & Heading Tracking | LocalizationSystem | ✅ Complete |
| 2 | Auto-Turret (point to goal) | AutoTurretSystem | ✅ Complete |
| 3 | Auto-Hood (distance-based) | AutoHoodSystem | ✅ Complete |
| 4 | Auto-Intake (color sensor) | AutoIntakeSystem | ✅ Complete |
| 5 | Auto-Shooting Sequence | ShootingSystem | ✅ Complete |
| 6 | LED Color Control | LEDSystem | ✅ Complete |
| 7 | Distance-Based Velocity | ShootingSystem | ✅ Complete |
| 8 | Shoot From Anywhere | RobotSystem | ✅ Complete |

## 📝 Example Usage in TeleOp

```java
@TeleOp(name = "My TeleOp")
public class MyTeleOp extends OpMode {
    private RobotSystem robot;
    
    @Override
    public void init() {
        robot = new RobotSystem();
        robot.init(hardwareMap, new Pose(0, 0, 0));
    }
    
    @Override
    public void loop() {
        // Update all systems
        robot.updateAll();
        
        // Drive robot
        robot.localization.getFollower().setTeleOpDrive(...);
        
        // Use features
        if (gamepad1.a) robot.autoIntake.toggleMode();
        if (gamepad1.b) robot.autoTurret.toggle();
        if (gamepad1.y) robot.shooting.triggerAutoShoot();
        if (gamepad1.right_bumper) robot.enableFullAuto();
        
        // Shoot from anywhere (Feature 8)
        if (gamepad1.left_bumper) {
            robot.shootFromAnywhere();
        }
    }
    
    @Override
    public void stop() {
        robot.stopAll();
    }
}
```

## 📝 Example Usage in Autonomous

```java
@Autonomous(name = "My Auto")
public class MyAuto extends OpMode {
    private RobotSystem robot;
    
    @Override
    public void init() {
        robot = new RobotSystem();
        robot.init(hardwareMap, new Pose(0, 0, 0));
        
        // Pre-configure for autonomous
        robot.enableFullAuto(); // Enable turret + hood
        robot.shooting.startShooterAuto(); // Start shooter
    }
    
    @Override
    public void loop() {
        robot.updateAll();
        
        // When at shooting position:
        if (robot.isReadyToShoot()) {
            robot.shooting.triggerAutoShoot();
        }
    }
}
```

## 🔧 Tuning

All tunable values are in **RobotConstants.java**. No need to modify module code!

**Common Tuning Tasks**:
1. **Turret PID**: Adjust `RobotConstants.Turret.KP` and `KD`
2. **Hood Distance Mapping**: Adjust distance/position pairs in `RobotConstants.Hood`
3. **Shooter Velocity**: Adjust velocity values in `RobotConstants.Shooter`
4. **Shooting Timing**: Adjust timing in `RobotConstants.Timing`
5. **Goal Position**: Change `RobotConstants.Field.GOAL_X` and `GOAL_Y`

## 🎨 Architecture Benefits

✅ **Modular** - Each feature is self-contained
✅ **Reusable** - Use in any TeleOp or Auto program
✅ **Maintainable** - Easy to update individual features
✅ **Testable** - Test each system independently
✅ **Clean** - No code duplication
✅ **Documented** - Clear purpose and usage for each module
✅ **Unified Constants** - One file for all tuning values

## 📂 File Structure

```
systems/
├── RobotConstants.java         (All constants)
├── LocalizationSystem.java     (Feature 1)
├── AutoTurretSystem.java       (Feature 2)
├── AutoHoodSystem.java         (Feature 3)
├── AutoIntakeSystem.java       (Feature 4)
├── ShootingSystem.java         (Features 5 & 7)
├── LEDSystem.java              (Feature 6)
├── RobotSystem.java            (Master integration)
└── SystemTeleOpExample.java    (Example program)
```

---

**Created**: February 2026  
**Team**: 14464 - DECODE Season  
**Purpose**: Modular, reusable robot systems for competition

