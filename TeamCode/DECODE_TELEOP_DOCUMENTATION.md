# DecodeTeleOp.java - Complete Documentation

## Overview

`DecodeTeleOp.java` is the main competition TeleOp program for DECODE Team 14464's robot. It provides a dual-driver control system with automatic and manual capabilities for intake, shooting, and turret tracking.

**File Location:** `TeamCode/src/main/java/org/firstinspires/ftc/teamcode/decode14464/systems/DecodeTeleOp.java`

---

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                        DecodeTeleOp                             │
│                     (Main Control Loop)                         │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                        RobotSystem                              │
│                   (Subsystem Integration)                       │
├─────────────────────────────────────────────────────────────────┤
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐          │
│  │ Localization │  │  AutoTurret  │  │  AutoIntake  │          │
│  │   System     │  │    System    │  │    System    │          │
│  └──────────────┘  └──────────────┘  └──────────────┘          │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐          │
│  │   Shooting   │  │  AutoShoot   │  │  Limelight   │          │
│  │   System     │  │    System    │  │   Manager    │          │
│  └──────────────┘  └──────────────┘  └──────────────┘          │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐          │
│  │ RobotMotors  │  │ RobotServos  │  │ RobotSensors │          │
│  └──────────────┘  └──────────────┘  └──────────────┘          │
└─────────────────────────────────────────────────────────────────┘
```

---

## State Variables

### Shooter Configuration
| Variable | Value | Description |
|----------|-------|-------------|
| `SHOOTER_HIGH_VELOCITY` | 1450 | Long distance shots |
| `SHOOTER_LOW_VELOCITY` | 1100 | Short distance (default) |
| `currentShooterVelocity` | Variable | Currently active velocity |
| `isHighVelocityMode` | Boolean | Toggle state |

### Hood Configuration
| Variable | Value | Description |
|----------|-------|-------------|
| `HOOD_POSITIONS` | {0.30, 0.55, 0.80} | LONG, MEDIUM, SHORT presets |
| `currentHoodIndex` | 2 (default) | Start at SHORT |
| `currentHoodPosition` | Variable | Actual servo value |
| `isHoodCustomPosition` | Boolean | True if fine-tuned |
| `HOOD_INCREMENT` | 0.02 | Fine adjustment step |

### Turret Control
| Variable | Value | Description |
|----------|-------|-------------|
| `autoTurretEnabled` | false (default) | Starts DISABLED for safety |
| `turretManualOverride` | Boolean | True when GP2 is controlling |
| `TURRET_MANUAL_POWER` | 0.3 | Manual control speed |

### Limelight Calibration
| Variable | Value | Description |
|----------|-------|-------------|
| `autoCalibrationEnabled` | true | Auto-calibrate when conditions good |
| `AUTO_CALIBRATE_COOLDOWN_MS` | 2000 | 2 second cooldown |
| `AUTO_CALIBRATE_TX_THRESHOLD` | 5.0 | Max error for auto-cal |
| `AUTO_CALIBRATE_VELOCITY_THRESHOLD` | 0.3 | Robot must be still |

---

## Main Loop Flow

```
┌──────────────────────────────────────────────────────────────┐
│                         loop()                                │
└──────────────────────────────────────────────────────────────┘
                              │
    ┌─────────────────────────┼─────────────────────────┐
    ▼                         ▼                         ▼
┌─────────┐            ┌─────────────┐           ┌─────────────┐
│ UPDATE  │            │    DRIVE    │           │    MODE     │
│ SYSTEMS │            │   CONTROL   │           │  SWITCHING  │
└─────────┘            └─────────────┘           └─────────────┘
robot.updateAll()      Field-centric             A/Cross toggles
                       mecanum drive             INTAKE ↔ LAUNCH
    │                         │                         │
    ▼                         ▼                         ▼
┌─────────────┐        ┌─────────────┐           ┌─────────────┐
│   INTAKE    │        │    HOOD     │           │   TURRET    │
│   CONTROL   │        │   CONTROL   │           │   CONTROL   │
└─────────────┘        └─────────────┘           └─────────────┘
GP2 Left Stick         GP1: Presets              Auto-tracking
Manual override        GP2: Fine tune            Manual override
    │                         │                         │
    ▼                         ▼                         ▼
┌─────────────┐        ┌─────────────┐           ┌─────────────┐
│  SHOOTER    │        │  LIMELIGHT  │           │ AUTO-SHOOT  │
│  VELOCITY   │        │ CALIBRATION │           │   SYSTEM    │
└─────────────┘        └─────────────┘           └─────────────┘
Left Trigger           Manual: GP2 RB            Right Bumper
momentary HIGH         Auto: when still          shoots all balls
    │                         │                         │
    └─────────────────────────┼─────────────────────────┘
                              ▼
                      ┌─────────────┐
                      │  TELEMETRY  │
                      │   UPDATE    │
                      └─────────────┘
                      3 pages: Essential,
                      Details, Controls
```

---

## Control Mapping

### Gamepad 1 (Driver) - Primary Controls

| Control | Action | Details |
|---------|--------|---------|
| **Left Stick** | Drive | Field-centric X/Y movement |
| **Right Stick X** | Rotate | Robot rotation |
| **A/Cross** | Toggle Mode | INTAKE ↔ LAUNCH |
| **Right Bumper** | Auto-Shoot | Shoots all loaded balls |
| **Left Trigger** | HIGH Velocity | Hold for 1450, release for 1100 |
| **Right Trigger** | Manual Push | Direct ball push control |
| **D-Pad Up** | Hood LONGER | Flatter angle (0.30) |
| **D-Pad Down** | Hood SHORTER | Steeper angle (0.80) |
| **D-Pad Left** | Previous Slot | Manual turret retreat |
| **D-Pad Right** | Next Slot | Manual turret advance |
| **Back+Start** | EMERGENCY STOP | Stops all motors |

### Gamepad 2 (Operator) - Fine Control & Calibration

| Control | Action | Details |
|---------|--------|---------|
| **A/Cross** | Toggle Alliance | BLUE (Tag 20) ↔ RED (Tag 24) |
| **X/Square** | Toggle Auto-Turret | Enable/disable tracking |
| **Y/Triangle** | Toggle Auto-Cal | Enable/disable auto-calibration |
| **Left Stick Y** | Intake Motor | Up=Intake, Down=Outtake |
| **Right Stick X** | Turret Manual | Variable speed control |
| **D-Pad Up** | Hood Fine + | +0.02 increment |
| **D-Pad Down** | Hood Fine - | -0.02 decrement |
| **D-Pad Left** | Turret Fine L | Slow left rotation |
| **D-Pad Right** | Turret Fine R | Slow right rotation |
| **Left Bumper** | Recalibrate | Lock current position |
| **Right Bumper** | Limelight Cal | AprilTag calibration |
| **Back/Share** | Next Telem Page | Cycle through 3 pages |

---

## Key Features Explained

### 1. Mode System (INTAKE vs LAUNCH)

```
┌─────────────────┐                    ┌─────────────────┐
│   INTAKE MODE   │   A/Cross Button   │   LAUNCH MODE   │
│   🔵 Blue LED   │ ◄──────────────────►│   🔴 Red LED    │
├─────────────────┤                    ├─────────────────┤
│ • Intake motor  │                    │ • Intake motor  │
│   runs at 100%  │                    │   stopped       │
│ • Ball detection│                    │ • Ready to shoot│
│   active        │                    │ • Manual push   │
│ • Auto-advance  │                    │   enabled       │
│   on ball detect│                    │                 │
└─────────────────┘                    └─────────────────┘
```

**Code Logic:**
```java
// A/Cross: Toggle between Intake and Launch modes
boolean aPressed = gamepad1.a || gamepad1.cross;
if (aPressed && !prevA) {
    robot.autoIntake.toggleMode();
    
    if (robot.autoIntake.isIntakeMode()) {
        robot.led.setIntakeModeColor();
        robot.autoIntake.startIntakeMotor();
    } else {
        robot.led.setLaunchModeColor();
        robot.autoIntake.stopIntakeMotor();
    }
}
prevA = aPressed;
```

---

### 2. Auto-Turret System

```
              ┌──────────────────────────────────┐
              │       autoTurretEnabled?         │
              └──────────────────────────────────┘
                     │                    │
                    YES                   NO
                     │                    │
                     ▼                    ▼
         ┌──────────────────┐    ┌──────────────────┐
         │  Auto-Tracking   │    │  Manual Control  │
         │  (Combined Goal  │    │  Only            │
         │   Tracking)      │    └──────────────────┘
         └──────────────────┘
                     │
                     ▼
         ┌──────────────────┐
         │ Manual Override? │
         │ (GP2 input)      │
         └──────────────────┘
                │         │
               YES        NO
                │         │
                ▼         ▼
         ┌──────────┐  ┌──────────┐
         │ Manual   │  │  Auto    │
         │ Power    │  │ Tracking │
         │ Applied  │  │ Active   │
         └──────────┘  └──────────┘
                │
                ▼ (when released)
         ┌──────────────────┐
         │ recalibratePos() │
         │ (sync position)  │
         └──────────────────┘
```

**Key Behavior:**
- **Auto-tracking ON + No manual input**: System automatically tracks the goal
- **Auto-tracking ON + Manual input**: Temporary override, then recalibrates when released
- **Auto-tracking OFF**: Full manual control only

---

### 3. Limelight Calibration System

```
┌───────────────────────────────────────────────────────────────┐
│                    CALIBRATION TYPES                          │
├───────────────────────────────────────────────────────────────┤
│                                                               │
│  MANUAL (GP2 Right Bumper)          AUTO (Background)         │
│  ─────────────────────────          ─────────────────         │
│  • Triggered on button press        • Runs continuously       │
│  • Strong rumble feedback           • Light rumble feedback   │
│  • Works anytime tag visible        • Requires:               │
│                                       - Robot stationary      │
│                                       - tx < 5° error         │
│                                       - 2 sec cooldown        │
│                                       - Alliance tag visible  │
│                                                               │
│  RECALIBRATE (GP2 Left Bumper)                                │
│  ─────────────────────────────                                │
│  • No Limelight needed                                        │
│  • Tells system "current position is correct"                 │
│  • Fixes drift without AprilTag                               │
│                                                               │
└───────────────────────────────────────────────────────────────┘
```

**Calibration Flow:**
```
         CALIBRATION FLOW
         ─────────────────
                │
                ▼
    ┌───────────────────────┐
    │ Check Limelight       │
    │ connected?            │───NO───► Return
    └───────────────────────┘
                │YES
                ▼
    ┌───────────────────────┐
    │ Alliance tag visible? │───NO───► Return (show error)
    │ (Tag 20 or 24)        │
    └───────────────────────┘
                │YES
                ▼
    ┌───────────────────────┐
    │ Get tx (horizontal    │
    │ angle to target)      │
    └───────────────────────┘
                │
        ┌───────┴───────┐
        ▼               ▼
┌───────────────┐ ┌───────────────┐
│ TURRET CAL    │ │ LOCALIZATION  │
│ Adjust target │ │ Update robot  │
│ position by   │ │ position from │
│ tx offset     │ │ botpose       │
└───────────────┘ └───────────────┘
```

---

### 4. Alliance-Based AprilTag Filtering

```
┌─────────────────────────────────────────────────────────────┐
│                   ALLIANCE SYSTEM                            │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   BLUE Alliance                    RED Alliance             │
│   ┌─────────────┐                  ┌─────────────┐          │
│   │  Tag ID: 20 │                  │  Tag ID: 24 │          │
│   │  Short rumble                  │  Long rumble           │
│   └─────────────┘                  └─────────────┘          │
│                                                             │
│   Toggle: GP2 A/Cross button                                │
│                                                             │
│   Effect:                                                   │
│   • getTargetX/Y() only returns data if YOUR tag visible    │
│   • Calibration only triggers for YOUR alliance tag         │
│   • Ignores opponent's AprilTag                             │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

---

### 5. Telemetry Pagination

The telemetry system has 3 pages to fit information on the small Driver Station screen:

#### Page 0: ESSENTIAL
What drivers need during the match:
- Mode (INTAKE/LAUNCH) + Ready status
- Balls loaded count
- Shooter velocity (current/target)
- Turret status (tracking/manual/on target)
- Hood position
- Distance to goal
- Alliance & AprilTag status
- Feature toggles (compact)

#### Page 1: DETAILS
Debugging and tuning information:
- **POSITION**: X, Y coordinates, Heading
- **INTAKE**: Motor status, Ball detection
- **LIMELIGHT**: Connection, Alliance, Tags seen, tx/ty, Last calibration
- **HOOD**: Current position, Min/max range

#### Page 2: CONTROLS
Quick reference guide showing all button mappings for both GP1 and GP2, with Xbox and PS4 button names.

---

## Lifecycle Methods

### `init()`
1. Creates `RobotSystem` instance
2. Calls `robot.initTeleOp(hardwareMap)` - preserves Auto position
3. Sets hood servo to starting position (SHORT preset)
4. Displays initialization status and control summary

### `start()`
1. Starts TeleOp driving via `follower.startTeleopDrive()`
2. Starts shooter at default LOW velocity (1100)
3. **Disables** auto-turret (safety - test in TurretTest first)
4. Intake motor stays OFF (starts in LAUNCH mode)

### `loop()`
1. **Update** - `robot.updateAll()` updates all subsystems
2. **Drive** - Field-centric mecanum drive from GP1 sticks
3. **Mode** - A/Cross toggles INTAKE ↔ LAUNCH
4. **Intake** - GP2 left stick overrides, auto based on mode
5. **Turret** - Manual slots (GP1 D-Pad) + auto/manual tracking (GP2)
6. **Hood** - GP1 presets + GP2 fine control
7. **Shooter** - GP1 left trigger momentary HIGH velocity
8. **Ball Push** - GP1 right trigger manual control
9. **Auto-Shoot** - GP1 right bumper starts sequence
10. **Calibration** - Manual (GP2 RB) + Auto (background)
11. **Telemetry** - 3-page display system

### `stop()`
1. Calls `robot.stopAll()` to stop all motors/servos
2. Displays stop confirmation

---

## Safety Features

| Feature | Description |
|---------|-------------|
| **Auto-Turret Disabled by Default** | Must explicitly enable with GP2 X |
| **Emergency Stop** | Back+Start on either controller stops everything |
| **Manual Override** | All automatic systems can be overridden |
| **Alliance Filtering** | Only responds to YOUR team's AprilTag |
| **Rumble Feedback** | Haptic confirmation for all toggle actions |
| **Mode-Aware Controls** | Right trigger only works in LAUNCH mode |

---

## Rumble Feedback Reference

| Action | Rumble Pattern |
|--------|----------------|
| Ready to shoot | Double blip (GP1), Single blip (GP2) |
| Auto-turret enabled | 300ms (GP2) |
| Auto-turret disabled | 100ms (GP2) |
| Alliance → BLUE | 150ms short (GP2) |
| Alliance → RED | 400ms long (GP2) |
| Auto-calibration toggle | 200ms ON, 100ms OFF (GP2) |
| Manual Limelight cal | 300ms (GP2) |
| Auto Limelight cal | 50ms (GP2) |
| Emergency stop | 500ms (both controllers) |
| Page change | 50ms (GP2) |

---

## Dependencies

### Direct Dependencies (Same Package - `systems/`)
- `RobotSystem.java` - Main robot controller
- `RobotConstants.java` - All constants (Hood, Field, Turret)
- `LimelightManager.java` - AprilTag vision processing

### Indirect Dependencies (via RobotSystem)
- `LocalizationSystem.java` - Position tracking (Pedro Pathing)
- `AutoTurretSystem.java` - Automatic turret tracking
- `AutoIntakeSystem.java` - Ball detection and carousel control
- `ShootingSystem.java` - Flywheel velocity control
- `AutoShootSystem.java` - Automated shooting sequence
- `RobotMotors.java` - Motor hardware abstraction
- `RobotServos.java` - Servo hardware abstraction
- `RobotSensors.java` - Sensor hardware abstraction
- `LEDController.java` - LED status indicators

---

## Usage Tips

### Pre-Match Checklist
1. ✅ Set alliance (GP2 A) to match your starting side
2. ✅ Verify Limelight connected (Details page)
3. ✅ Check AprilTag visibility
4. ✅ Confirm shooter velocity mode (LOW default)

### During Intake
- Let auto-advance work automatically
- Use GP1 D-Pad Left/Right if carousel gets stuck
- GP2 Left Stick provides manual intake control

### During Shooting
1. Verify "READY" status before shooting
2. Use Left Trigger (hold) for long-distance shots
3. GP2 can fine-tune turret if auto-tracking needs adjustment
4. Press GP2 Right Bumper when AprilTag visible for calibration

### Telemetry Navigation
- GP2 Back/Share cycles through 3 pages
- Page 0 (Essential) - Use during match
- Page 1 (Details) - Use for debugging
- Page 2 (Controls) - Quick reference

---

## Troubleshooting

| Issue | Solution |
|-------|----------|
| Turret not tracking | Press GP2 X to enable auto-turret |
| Wrong AprilTag | Press GP2 A to switch alliance |
| Calibration fails | Ensure YOUR alliance tag is visible |
| Shooter not ready | Wait for velocity to reach target |
| Intake not working | Check mode (must be INTAKE mode) |
| Emergency needed | Press Back+Start on either controller |

---

*Last Updated: February 2026*
*Team 14464 - DECODE*
