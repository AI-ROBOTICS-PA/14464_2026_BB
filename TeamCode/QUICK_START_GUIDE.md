# 🚀 Quick Start Guide - Modular Robot Systems

## 📦 Installation Complete!

✅ 10 new files created in `/systems` folder  
✅ All 8 features implemented  
✅ Zero compilation errors  
✅ Ready to use immediately

---

## ⚡ 5-Minute Quick Start

### Step 1: Copy the Example
```java
// Create a new TeleOp file, copy from SystemTeleOpExample.java
```

### Step 2: Minimal TeleOp (3 lines!)
```java
@TeleOp(name = "Quick Start")
public class QuickStart extends OpMode {
    private RobotSystem robot;
    
    public void init() {
        robot = new RobotSystem();
        robot.init(hardwareMap, new Pose(0, 0, 0));  // Line 1: Init everything
    }
    
    public void loop() {
        robot.updateAll();  // Line 2: Update everything
        
        // Line 3: Drive (FIELD-CENTRIC for proper localization!)
        robot.localization.getFollower().setTeleOpDrive(
            -gamepad1.left_stick_y, -gamepad1.left_stick_x, -gamepad1.right_stick_x, false
        );
        // Note: Last parameter FALSE = field-centric (required for localization)
    }
    
    public void stop() {
        robot.stopAll();
    }
}
```

### Step 3: Add Features (One line each!)
```java
// In loop(), add any features you want:

if (gamepad1.a) robot.autoIntake.toggleMode();           // Toggle intake/launch
if (gamepad1.b) robot.autoTurret.toggle();               // Toggle auto-aim
if (gamepad1.x) robot.autoHood.toggleAutoAdjust();       // Toggle auto-hood
if (gamepad1.y) robot.shooting.triggerAutoShoot();       // Shoot 3 balls
if (gamepad1.left_bumper) robot.shootFromAnywhere();     // Full auto shoot!
```

---

## 🎯 Common Use Cases

### Use Case 1: Full Auto Shooting
```java
// Enable everything, robot aims and shoots automatically
robot.enableFullAuto();
robot.shooting.startShooterAuto();

// When ready, trigger shooting
if (robot.isReadyToShoot()) {
    robot.shooting.triggerAutoShoot();
}
```

### Use Case 2: Manual Control Everything
```java
// Turn off all auto features
robot.autoTurret.disable();
robot.autoHood.disableAutoAdjust();

// Control manually
robot.autoTurret.setManualPower(gamepad2.right_stick_x);
robot.autoHood.setPosition(0.65);
robot.shooting.startShooter(1300);
```

### Use Case 3: Hybrid (Auto-Aim, Manual Shooter)
```java
// Auto-aim turret and hood
robot.autoTurret.enable();
robot.autoHood.enableAutoAdjust();

// Manual shooter control
if (gamepad1.right_bumper) {
    robot.shooting.startShooter(1450);
} else {
    robot.shooting.stopShooter();
}
```

### Use Case 4: Distance-Based Everything
```java
// Everything auto-adjusts based on distance to goal
robot.autoTurret.enable();           // Points at goal
robot.autoHood.enableAutoAdjust();   // Adjusts angle
robot.shooting.startShooterAuto();   // Sets velocity

// Just drive and shoot!
if (gamepad1.y) robot.shooting.triggerAutoShoot();
```

---

## 📊 Feature Access Cheat Sheet

| What You Want | Code |
|---------------|------|
| **Get robot position** | `robot.localization.getX()`, `.getY()` |
| **Get robot heading** | `robot.localization.getHeadingDegrees()` |
| **Distance to goal** | `robot.getDistanceToGoal()` |
| **Angle to goal** | `robot.getAngleToGoal()` |
| **Enable auto-turret** | `robot.autoTurret.enable()` |
| **Check if on target** | `robot.autoTurret.isOnTarget()` |
| **Enable auto-hood** | `robot.autoHood.enableAutoAdjust()` |
| **Set hood preset** | `robot.autoHood.setLongRange()` |
| **Toggle intake mode** | `robot.autoIntake.toggleMode()` |
| **Check ball detected** | `robot.autoIntake.isBallDetected()` |
| **Start shooter** | `robot.shooting.startShooterAuto()` |
| **Shoot 3 balls** | `robot.shooting.triggerAutoShoot()` |
| **Check shooter ready** | `robot.shooting.isShooterAtTargetVelocity()` |
| **Set LED color** | `robot.led.setIntakeModeColor()` |
| **Ready to shoot?** | `robot.isReadyToShoot()` |
| **Shoot from anywhere** | `robot.shootFromAnywhere()` |

---

## 🎮 Recommended Button Mapping

```
Gamepad 1 (Driver):
├── Left Stick: Drive (strafe + forward/back)
├── Right Stick X: Rotate
├── A/Cross: Toggle Intake/Launch Mode
├── B/Circle: Toggle Auto-Turret
├── X/Square: Toggle Auto-Hood
├── Y/Triangle: Start/Stop Shooter
├── Left Bumper: Shoot From Anywhere (Full Auto)
├── Right Bumper: Trigger Shooting Sequence
└── DPAD: Manual overrides (if needed)

Gamepad 2 (Operator):
├── Left Stick Y: Manual intake control
├── Right Stick X: Manual turret control
└── Buttons: Additional features
```

---

## 🔧 Tuning Guide

### Quick Tuning Steps:

1. **Open** `RobotConstants.java`
2. **Find** the section you want to tune:
   - `Turret` - PID constants for auto-aim
   - `Hood` - Distance/position mapping
   - `Shooter` - Velocities and PID
   - `Field` - Goal position
3. **Change** values
4. **Redeploy** - All programs use updated constants!

### Most Important Values:

```java
// Goal position (WHERE to aim):
RobotConstants.Field.GOAL_X = 1.0;
RobotConstants.Field.GOAL_Y = 144.0;

// Turret tracking speed:
RobotConstants.Turret.KP = 0.015;  // Higher = faster, may oscillate

// Shooter velocities:
RobotConstants.Shooter.HIGH_VELOCITY = 1450.0;  // Long distance
RobotConstants.Shooter.LOW_VELOCITY = 1100.0;   // Short distance
```

---

## 🐛 Quick Troubleshooting

| Problem | Solution |
|---------|----------|
| Robot doesn't track goal | 1. Check `GOAL_X` and `GOAL_Y` are correct<br>2. Call `robot.autoTurret.enable()` |
| Hood doesn't adjust | Call `robot.autoHood.enableAutoAdjust()` |
| Balls don't auto-advance | Ensure in intake mode: `robot.autoIntake.setIntakeMode()` |
| Shooter won't start | Use `robot.shooting.startShooterAuto()` |
| Nothing works | Did you call `robot.updateAll()` in loop? |

---

## 📖 Full Documentation

- **Detailed API**: `/systems/README.md`
- **Complete Summary**: `SYSTEMS_IMPLEMENTATION_SUMMARY.md`
- **Example Code**: `/systems/SystemTeleOpExample.java`

---

## ✨ Pro Tips

1. **Always call `robot.updateAll()`** in your loop - this is critical!
2. **Use FIELD-CENTRIC drive** (last parameter = `false`) for accurate localization
3. **Use `robot.stopAll()`** in stop() to safely stop everything
4. **Test features one at a time** to understand how they work
5. **Tune in `RobotConstants.java`** - don't modify module code
6. **Check telemetry** to debug - example program shows how

### ⚠️ Important: Field-Centric vs Robot-Centric

For **localization features to work correctly**, you MUST use field-centric drive:

```java
// ✅ CORRECT - Field-centric (last param = false)
robot.localization.getFollower().setTeleOpDrive(
    -gamepad1.left_stick_y, -gamepad1.left_stick_x, -gamepad1.right_stick_x, false
);

// ❌ WRONG - Robot-centric breaks localization accuracy
robot.localization.getFollower().setTeleOpDrive(
    -gamepad1.left_stick_y, -gamepad1.left_stick_x, -gamepad1.right_stick_x, true
);
```

**Why?** Field-centric drive maintains consistent orientation relative to the field, allowing Pedro Pathing to accurately track position. Robot-centric drive can cause drift and inaccurate position tracking.

---

## 🎓 Learning Path

1. ✅ Deploy `SystemTeleOpExample` and test all features
2. ✅ Create your own TeleOp using `RobotSystem`
3. ✅ Tune constants for your robot
4. ✅ Add custom features by extending modules
5. ✅ Create autonomous programs using same system

---

## 🎯 Mission: Shoot From Anywhere!

The ultimate test - one button does it all:

```java
if (gamepad1.left_bumper) {
    robot.shootFromAnywhere();
    // Robot:
    // 1. Enables auto-turret (aims at goal)
    // 2. Enables auto-hood (adjusts for distance)
    // 3. Starts shooter (calculates velocity)
    // 4. Waits for ready
    // 5. Shoots all 3 balls
    // All automatic! 🎯
}
```

---

**You're ready to compete!** 🏆🤖

Deploy and dominate! 🚀

