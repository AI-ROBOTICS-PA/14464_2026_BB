# Deep Code Review — Decode14464

**Scope:** `/auto/`, `/systems/`, and `/teleop/` folders  
**Files Reviewed:** RobotConstants, RobotMotors, RobotServos, RobotSensors, AutoTurretSystem, ShootingSystem, AutoHoodSystem, AutoShootSystem, AutoIntakeSystem, LocalizationSystem, LimelightManager, LEDSystem, RobotSystem, DecodeTeleOp, DecodeTeleOp_Red, TurretCalibration, TurretTest, LocalizationTest, SystemTeleOpExample, Auto_BlueNear, Auto_RedNear, Auto_BlueFar_TEST, Auto_RedFar_TEST

---

## Table of Contents

1. [Architecture Overview](#1-architecture-overview)
2. [Requirements & Rules Extracted from Code](#2-requirements--rules-extracted-from-code)
3. [Bug Report](#3-bug-report)
4. [Performance & Maintainability Issues](#4-performance--maintainability-issues)
5. [Positive Findings](#5-positive-findings)
6. [Recommendations](#6-recommendations)

---

## 1. Architecture Overview

### System Hierarchy

```
RobotSystem
├── RobotMotors         — hardware: drive, flywheel ("shooter"), turret, intake
├── RobotServos         — hardware: ballpush_1..4, hood_5, intake_gate
├── RobotSensors        — hardware: front/back colors, touch sensor
├── AutoTurretSystem    — turret motor PID + 3 tracking modes
├── ShootingSystem      — flywheel PIDF + ball-push servo state machine
├── AutoHoodSystem      — hood servo via odometry distance to goal
├── AutoIntakeSystem    — intake gate servo + ball count tracking
├── AutoShootSystem     — top-level shoot orchestrator (delegates to ShootingSystem)
├── LocalizationSystem  — Pedro Pathing wrapper
├── LimelightManager    — AprilTag pose estimation
└── LEDSystem           — alliance/mode color feedback
```

### Alliance Variant Pattern

`DecodeTeleOp_Red extends DecodeTeleOp`, overriding only `getDefaultAlliance()` → returns `Alliance.RED`. This is clean and avoids all code duplication. Every future TeleOp change applies automatically to both alliances.

### Auto→TeleOp Handoff

`RobotMotors` uses JVM-level `static` fields (`savedTurretPosition`, `savedTurretOffsetTicks`, `savedInitialOffsetDegrees`, `hasSavedOffset`) to persist across OpMode instances within the same app session. `DecodeTeleOp.start()` reads these to restore turret state after autonomous.

### Coordinate System

Pedro Pathing — origin at field bottom-left, x-axis right, y-axis up, 0° = up (north), CCW positive.
- Blue Near: `(50, 7.5, 180°)` — Blue Far: `(94, 7.5, 0°)`
- Red Near: `(94, 7.5, 0°)` — Red Far: `(50, 7.5, 180°)` — mirrored with `x = 144 − blueX`

---

## 2. Requirements & Rules Extracted from Code

These are the operational rules implied by the code. Use this section to verify intent vs. implementation.

### 2.1 Turret

| Rule | Value | Source |
|---|---|---|
| Max rotation | ±135° from physical center | `RobotConstants.Turret.MAX_ROTATION_DEGREES` |
| Ticks per degree | 3.01 | `RobotConstants.Turret.TICKS_PER_DEGREE` |
| Deadband | ±4.0° | `RobotConstants.Turret.DEADBAND_DEGREES` |
| Soft limit zone | 15° before hard limit | `RobotConstants.Turret.SOFT_LIMIT_DEGREES` |
| PID gains | KP=0.007, KI=0, KD=0 | `RobotConstants.Turret.KP/KI/KD` |
| Hard reset trigger | GP1 Y — physically center turret first | `DecodeTeleOp.java` |
| Manual override input | GP2 D-Pad L/R or right stick X | `DecodeTeleOp.java` |
| Recommended tracking mode | Combined Goal Tracking (position + rotation) | `AutoTurretSystem` |

### 2.2 Shooter (Flywheel)

| Rule | Value | Source |
|---|---|---|
| HIGH velocity | 1450 ticks/sec | `RobotConstants.Shooter.HIGH_VELOCITY` |
| MID velocity | 1300 ticks/sec | `RobotConstants.Shooter.MID_VELOCITY` |
| LOW velocity | 1100 ticks/sec | `RobotConstants.Shooter.LOW_VELOCITY` |
| TeleOp HIGH | 1450 | `DecodeTeleOp.SHOOTER_HIGH_VELOCITY` ✓ matches |
| TeleOp MID | **1160** | `DecodeTeleOp.SHOOTER_MID_VELOCITY` **≠ 1300** — see BUG-005 |
| TeleOp LOW | 1100 | `DecodeTeleOp.SHOOTER_LOW_VELOCITY` ✓ matches |
| Distance thresholds | CLOSE ≤36 in, MID 36–72 in, FAR >72 in | `RobotConstants.Shooter.*_DISTANCE` |
| PIDF | KP=300, KI=0, KD=0, KF=13.65 | `RobotConstants.Shooter.KP/KI/KD/KF` |
| Velocity tolerance | ±100 ticks/sec | `RobotConstants.Shooter.VELOCITY_TOLERANCE` |

### 2.3 Hood

| Rule | Value | Source |
|---|---|---|
| Valid servo range | [0.28, 0.82] | `RobotConstants.Hood.MIN_POSITION / MAX_POSITION` |
| LONG range preset | 0.27 | `RobotConstants.Hood.LONG_RANGE_POSITION` **(< MIN — see BUG-004)** |
| MID range preset | 0.55 | `RobotConstants.Hood.MID_RANGE_POSITION` |
| SHORT range preset | 0.80 | `RobotConstants.Hood.SHORT_RANGE_POSITION` |
| TeleOp LONG preset | **0.30** | `DecodeTeleOp.HOOD_POSITIONS[0]` (differs from constant 0.27) |
| TeleOp MID preset | 0.55 | `DecodeTeleOp.HOOD_POSITIONS[1]` ✓ |
| TeleOp SHORT preset | 0.80 | `DecodeTeleOp.HOOD_POSITIONS[2]` ✓ |
| Fine increment | 0.01 | `DecodeTeleOp.HOOD_INCREMENT` |

### 2.4 Ball Push Servo

| Rule | Value | Source |
|---|---|---|
| Initial position (RobotServos) | 0.49 | `RobotServos.ballpush_initial_position` |
| Initial position (ShootingSystem) | 0.48 | `RobotConstants.BallPush.INITIAL_POSITION` |
| Push position | 0.77 | `RobotConstants.BallPush.PUSH_POSITION` |
| Retract position | 0.48 | `RobotConstants.BallPush.RETRACT_POSITION` |
| Push duration | 200ms | `RobotConstants.BallPush.BALL_PUSH_DURATION_MS` |
| Sequence (3 balls) | PUSH_1, RETRACT_1, WAIT_RETRACT_1, ADVANCE_TURRET_1, WAIT_TURRET_1, PUSH_2, … | `ShootingSystem` |

### 2.5 Autonomous Timing

| Rule | Value | Source |
|---|---|---|
| Balls per cycle | 3 | `RobotConstants.Auto.BALLS_PER_CYCLE` |
| Shooter spin-up | 3.0 s | `RobotConstants.Auto.SHOOTER_SPINUP_SECONDS` |
| Shoot sequence timeout | 5.0 s | `RobotConstants.Auto.SHOOT_SEQUENCE_TIMEOUT` |
| Path timeout | 5.0 s | `RobotConstants.Auto.PATH_TIMEOUT` |

### 2.6 Goal Coordinates

| Constant | Value | Note |
|---|---|---|
| `Field.BLUE_GOAL_X` | 12.0 in | Blue goal X |
| `Field.BLUE_GOAL_Y` | 72.0 in | Blue goal Y |
| `Field.RED_GOAL_X` | 132.0 in | Red goal X |
| `Field.RED_GOAL_Y` | 72.0 in | Red goal Y |
| `Field.GOAL_X` | `BLUE_GOAL_X` (12.0) | **Always Blue** — see BUG-003 |
| `Field.GOAL_Y` | `BLUE_GOAL_Y` (72.0) | **Always Blue** — see BUG-003 |

---

## 3. Bug Report

Ordered by severity. **High** = match impact or physical damage risk. **Medium** = incorrect behavior possible. **Low** = code quality concern.

---

### BUG-001 — High | Wrong hardware config name for `ballpush_4`

**File:** [systems/RobotServos.java](src/main/java/org/firstinspires/ftc/teamcode/decode14464/systems/RobotServos.java)

```java
ballpush_4 = hardwareMap.get(Servo.class, "ballpush_2");
//            Field named _4 ^^^       Hardware named _2 ^^^
```

**Problem:** The Java field is `ballpush_4` but the hardware config device name is `"ballpush_2"`. If the hardware config is updated to name this device `"ballpush_4"` (natural expectation), the mapping silently breaks. Every caller of `robot.servos.ballpush_4` will get the wrong servo.

**Fix:** Rename the Java field to `ballpush_2`, or rename the hardware config device to `"ballpush_4"`. Apply consistently everywhere the field is referenced.

---

### BUG-002 — High | TurretCalibration computes limits for 150° instead of actual 135°

**File:** [systems/TurretCalibration.java](src/main/java/org/firstinspires/ftc/teamcode/decode14464/systems/TurretCalibration.java)

```java
telemetry.addData("Max Ticks (150°)", (int)(ticksPerDegree * 150));
// RobotConstants.Turret.MAX_ROTATION_DEGREES = 135
```

**Problem:** The calibration output tells the user to aim for ticks representing 150° of rotation. The software enforces 135°. If a user sets mechanical stops at 150° from this guidance, the turret will have 15° of unprotected travel between the software limit and the physical stop. If someone tunes `MAX_ROTATION_DEGREES` upward to match 150°, the physical stop may be located at an unsafe position. *(OpMode currently disabled — but fix before re-enabling.)*

**Fix:**
```java
telemetry.addData(
    "Max Ticks (" + (int)RobotConstants.Turret.MAX_ROTATION_DEGREES + "°)",
    (int)(ticksPerDegree * RobotConstants.Turret.MAX_ROTATION_DEGREES));
```

---

### BUG-003 — High | Goal coordinates always use Blue alliance, breaking Red-side shots

**Files:** [systems/RobotConstants.java](src/main/java/org/firstinspires/ftc/teamcode/decode14464/systems/RobotConstants.java), [systems/ShootingSystem.java](src/main/java/org/firstinspires/ftc/teamcode/decode14464/systems/ShootingSystem.java), `AutoHoodSystem.java`, [systems/TurretTest.java](src/main/java/org/firstinspires/ftc/teamcode/decode14464/systems/TurretTest.java)

```java
// RobotConstants.java
public static final double GOAL_X = BLUE_GOAL_X; // 12.0 — always Blue

// ShootingSystem.getDistanceToGoal()
return localization.getDistanceToPoint(
    RobotConstants.Field.GOAL_X,   // Always 12.0
    RobotConstants.Field.GOAL_Y);  // Always 72.0
```

**Problem:** `ShootingSystem` and `AutoHoodSystem` use `Field.GOAL_X/Y` for velocity selection and hood angle respectively. For Red alliance, the robot starts far from Blue goal and close to Red goal. At Red Far start `(94, 7.5)`:
- Actual distance to Red goal `(132, 72)` ≈ 82 in → should select MID velocity
- Computed distance to Blue goal `(12, 72)` ≈ 107 in → selects FAR velocity (1450)

Result: wrong flywheel speed and hood angle for every Red-alliance shot.

**Note:** `AutoTurretSystem` correctly handles this via `setBlueAlliance()`/`setRedAlliance()` that update `goalX`/`goalY` instance variables. `ShootingSystem` and `AutoHoodSystem` need the same pattern.

**Fix:**
1. Add `private Alliance alliance = Alliance.BLUE;` and a setter to `ShootingSystem` and `AutoHoodSystem`.
2. In `getDistanceToGoal()`, use goal coordinates based on `alliance`:
   ```java
   double goalX = (alliance == Alliance.RED) ? RobotConstants.Field.RED_GOAL_X : RobotConstants.Field.BLUE_GOAL_X;
   double goalY = RobotConstants.Field.BLUE_GOAL_Y; // same for both alliances
   return localization.getDistanceToPoint(goalX, goalY);
   ```
3. Call `robot.shooting.setAlliance(ALLIANCE)` and `robot.autoHood.setAlliance(ALLIANCE)` in all Auto opModes immediately after `RobotSystem.init()`, alongside the existing `robot.limelight.setAlliance(ALLIANCE)` call.
4. Call them in `DecodeTeleOp.start()` alongside the existing `autoTurret.setBlueAlliance()/setRedAlliance()` calls.

---

### BUG-004 — High | Hood LONG_RANGE_POSITION is outside the declared valid servo range

**File:** [systems/RobotConstants.java](src/main/java/org/firstinspires/ftc/teamcode/decode14464/systems/RobotConstants.java)

```java
public static final double MIN_POSITION        = 0.28;  // declared minimum
public static final double MAX_POSITION        = 0.82;
public static final double LONG_RANGE_POSITION = 0.27;  // BELOW min!
public static final double MID_RANGE_POSITION  = 0.55;
public static final double SHORT_RANGE_POSITION = 0.80;
```

**Problem:** `LONG_RANGE_POSITION = 0.27 < MIN_POSITION = 0.28`. Two consequences:
1. Any code that clamps the servo to `[MIN, MAX]` will silently deliver 0.28 instead of 0.27 — the hood angle will be wrong for long-range shots without any error.
2. Auto opModes set the servo directly to `LONG_RANGE_POSITION = 0.27`, bypassing the clamping, potentially driving the servo beyond its mechanical travel.

**Fix:** Measure the actual safe servo minimum on the physical robot, then either:
- Set `MIN_POSITION = 0.27` (if 0.27 is the true minimum), or
- Set `LONG_RANGE_POSITION = 0.28` (if 0.28 is correct and 0.27 was a typo).

---

### BUG-005 — Medium | TeleOp mid velocity (1160) does not match RobotConstants mid velocity (1300)

**Files:** [systems/DecodeTeleOp.java](src/main/java/org/firstinspires/ftc/teamcode/decode14464/systems/DecodeTeleOp.java), [systems/RobotConstants.java](src/main/java/org/firstinspires/ftc/teamcode/decode14464/systems/RobotConstants.java)

```java
// DecodeTeleOp.java
private static final double SHOOTER_MID_VELOCITY = 1160.0;

// RobotConstants.java
public static final double MID_VELOCITY = 1300.0;
```

**Problem:** Autonomous uses `RobotConstants.Shooter.MID_VELOCITY = 1300` for mid-range shots. TeleOp mid trigger fires at 1160. The same "mid" label represents two different speeds. A driver tuning shots based on TeleOp behavior will see different results in autonomous.

**Fix:**
```java
// DecodeTeleOp.java
private static final double SHOOTER_MID_VELOCITY = RobotConstants.Shooter.MID_VELOCITY; // 1300
```
If 1160 was intentionally different, rename it to `SHOOTER_MID_TELEOP_VELOCITY` and add a comment explaining why it differs from the constant.

---

### BUG-006 — Medium | RobotMotors.init() hardcodes shooter PIDF instead of reading RobotConstants

**File:** [systems/RobotMotors.java](src/main/java/org/firstinspires/ftc/teamcode/decode14464/systems/RobotMotors.java)

```java
flywheelMotor.setPIDFCoefficients(DcMotor.RunMode.RUN_USING_ENCODER,
    new PIDFCoefficients(300, 0, 0, 13.65)); // hardcoded
```

**Problem:** These values happen to match `RobotConstants.Shooter.KP/KI/KD/KF`, but they are not linked. Tuning `RobotConstants` will not update `RobotMotors`. `ShootingSystem.init()` overwrites them afterward — but if a future OpMode uses `RobotSystem.init()` without instantiating `ShootingSystem`, the motor runs with stale PIDF values silently.

**Fix:**
```java
flywheelMotor.setPIDFCoefficients(DcMotor.RunMode.RUN_USING_ENCODER,
    new PIDFCoefficients(
        RobotConstants.Shooter.KP,
        RobotConstants.Shooter.KI,
        RobotConstants.Shooter.KD,
        RobotConstants.Shooter.KF));
```

---

### BUG-007 — Medium | Double initialization of ballpush servo causes a position jump at startup

**Files:** [systems/RobotServos.java](src/main/java/org/firstinspires/ftc/teamcode/decode14464/systems/RobotServos.java), [systems/ShootingSystem.java](src/main/java/org/firstinspires/ftc/teamcode/decode14464/systems/ShootingSystem.java)

```java
// RobotServos.init():
ballpush_initial_position = 0.49;
ballpush_4.setPosition(0.49);    // first write

// ShootingSystem.init() (called shortly after):
servos.ballpush_initial_position = RobotConstants.BallPush.INITIAL_POSITION; // = 0.48
servos.ballpush_4.setPosition(servos.ballpush_initial_position);             // second write
```

**Problem:** The servo moves 0.49 → 0.48 (2% jump) every init. The two classes disagree on initial position. Any code that runs between the two inits reads the wrong `ballpush_initial_position`.

**Fix:** Use `RobotConstants.BallPush.INITIAL_POSITION` as the single source in `RobotServos.java`:
```java
ballpush_initial_position = RobotConstants.BallPush.INITIAL_POSITION;
ballpush_4.setPosition(ballpush_initial_position);
```
Then remove the redundant assignment from `ShootingSystem.init()`.

---

### BUG-008 — Medium | Ball push retract waits on timer only; available touch sensor is not used

**File:** [systems/ShootingSystem.java](src/main/java/org/firstinspires/ftc/teamcode/decode14464/systems/ShootingSystem.java)

```java
case WAIT_RETRACT_1:
    if (elapsed - stateStartTime > BALL_PUSH_DURATION_MS) { // pure 200ms timer
        transitionTo(State.ADVANCE_TURRET_1);
    }
    break;
```

**Problem:** `RobotSensors.ballPushSensor` (touch sensor) exists. Under low battery or mechanical resistance, the servo may not retract in 200ms. The system then starts the next push state from an incompletely retracted position, jamming or skipping a ball.

**Fix:** Use touch sensor for confirmation, with timer as safety fallback:
```java
case WAIT_RETRACT_1:
    boolean retracted = !sensors.ballPushSensor.isPressed(); // pressed = still extended
    boolean timerExpired = (elapsed - stateStartTime > BALL_PUSH_DURATION_MS * 2);
    if (retracted || timerExpired) {
        transitionTo(State.ADVANCE_TURRET_1);
    }
    break;
```

---

### BUG-009 — Medium | hardReset() resets encoder without explicitly stopping the turret motor first

**File:** [systems/AutoTurretSystem.java](src/main/java/org/firstinspires/ftc/teamcode/decode14464/systems/AutoTurretSystem.java)

```java
public void hardReset() {
    motors.turret.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
    // ...
    motors.turret.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
}
```

**Problem:** If the motor was in `RUN_TO_POSITION` mode with an active target, the mode transition to `STOP_AND_RESET_ENCODER` may briefly jerk the motor before it stops. Explicitly zeroing power before the reset is clearer and safer.

**Fix:**
```java
public void hardReset() {
    motors.turret.setPower(0);                                    // Explicit stop
    motors.turret.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
    // ... rest of reset ...
}
```

---

### BUG-010 — Low | `ballpush_final_position` is declared but never used anywhere

**File:** [systems/RobotServos.java](src/main/java/org/firstinspires/ftc/teamcode/decode14464/systems/RobotServos.java)

```java
public double ballpush_final_position = 0.47; // never referenced elsewhere
```

**Problem:** Dead field. It appears to be an intended "end of stroke" position (0.47, just below initial 0.48) that was never wired into any system.

**Fix:** Remove the field, or intentionally use it in `ShootingSystem` as the retract confirmation position.

---

### BUG-011 — Low | LocalizationTest uses a hardcoded turret KP (0.015) instead of RobotConstants (0.010)

**File:** [systems/LocalizationTest.java](src/main/java/org/firstinspires/ftc/teamcode/decode14464/systems/LocalizationTest.java)

```java
double kp = 0.015; // local, 50% higher than RobotConstants.Turret.KP = 0.010
```

**Problem:** Test results from this OpMode are not transferable to production because the turret responds 50% more aggressively. Updating `RobotConstants.Turret.KP` has no effect here.

**Fix:**
```java
double kp = RobotConstants.Turret.KP;
```

---

## 4. Performance & Maintainability Issues

### PERF-001 — Duplicate PIDF source of truth

**Files:** `RobotMotors.java`, `ShootingSystem.java`, `RobotConstants.java`

Shooter PIDF values are defined in three places: hardcoded in `RobotMotors.init()`, in `RobotConstants.Shooter.*`, and applied again in `ShootingSystem.init()`. Any tuning run that edits `RobotConstants` must also check `RobotMotors` for the hardcoded copy. See BUG-006 for the fix.

---

### PERF-002 — ShootingSystem state machine is hardcoded for exactly 3 balls

**File:** [systems/ShootingSystem.java](src/main/java/org/firstinspires/ftc/teamcode/decode14464/systems/ShootingSystem.java)

States `PUSH_1, RETRACT_1, WAIT_RETRACT_1, ADVANCE_TURRET_1, WAIT_TURRET_1, PUSH_2, …, PUSH_3, RETRACT_3, COMPLETE` are a flat, non-looping sequence. If the ball count ever changes from 3, every new ball requires 5 new state entries. A loop-based design would handle N balls with no code changes:

```java
// Concept — replaces 15+ flat states with a single loop
int currentBallIndex = 0;
enum Phase { PUSH, RETRACT, WAIT_RETRACT, ADVANCE_TURRET, WAIT_TURRET }
Phase currentPhase = Phase.PUSH;

// In update(): drive currentPhase → next phase; increment currentBallIndex when a ball cycle completes
```

---

### PERF-003 — Auto-calibration uses gamepad input as a proxy for robot velocity

**File:** [systems/DecodeTeleOp.java](src/main/java/org/firstinspires/ftc/teamcode/decode14464/systems/DecodeTeleOp.java), `performAutoCalibration()`

```java
double driveInput = Math.abs(gamepad1.left_stick_x) + Math.abs(gamepad1.left_stick_y) + ...;
if (driveInput > AUTO_CALIBRATE_VELOCITY_THRESHOLD) { return; }
```

A robot decelerating after release (coasting) has zero stick input but non-zero velocity, so this check passes prematurely. The Limelight pose is then captured while the robot is still moving, introducing position error into the correction.

**Better:** Use odometry velocity:
```java
if (follower.getVelocity() > AUTO_CALIBRATE_VELOCITY_THRESHOLD) { return; }
```

---

### PERF-004 — Debug telemetry is on the ESSENTIAL page during competition

**File:** [systems/DecodeTeleOp.java](src/main/java/org/firstinspires/ftc/teamcode/decode14464/systems/DecodeTeleOp.java), `displayEssentialPage()`

```java
telemetry.addLine("─── DEBUG GP2 ───");
telemetry.addData("GP2 DpadL/R", ...);
telemetry.addData("GP2 RStickX", ...);
// ... 5 more debug lines
```

This appears on Page 0 (ESSENTIAL), which is what drivers see during a match. The Driver Station screen is small; these debug lines push critical data (balls loaded, shooter RPM, turret status) further down or off screen.

**Fix:** Move debug lines to Page 1 (DETAILS), or guard behind a `DEBUG_MODE` boolean constant.

---

### PERF-005 — TurretCalibration is silently disabled with no re-enable guidance

**File:** [systems/TurretCalibration.java](src/main/java/org/firstinspires/ftc/teamcode/decode14464/systems/TurretCalibration.java)

```java
// @TeleOp(name = "Turret Calibration", group = "Test") // DISABLED
```

Nothing in the codebase documents why this was disabled or when to re-enable it. If the turret needs recalibration, a developer must know to look for this file.

**Fix:** Add a comment: `// Re-enable before measuring new turret hardware or after mechanical rebuilds`.
Also fix [BUG-002](#bug-002--high--turretcalibration-computes-limits-for-150-instead-of-actual-135) before re-enabling.

---

### PERF-006 — SystemTeleOpExample uses prototype velocities that would cause failed shots

**File:** [systems/SystemTeleOpExample.java](src/main/java/org/firstinspires/ftc/teamcode/decode14464/systems/SystemTeleOpExample.java)

```java
static final double HIGH_VELOCITY = 460;   // dev/test value
static final double LOW_VELOCITY  = 100;   // dev/test value
// Competition values: HIGH = 1450, LOW = 1100
```

The OpMode is disabled, so there is no match risk. However, if someone re-enables it (e.g., to prototype a new feature), they will see servo motion but no functional shots, with no in-file warning about why.

**Fix:** Replace with `RobotConstants.Shooter.HIGH_VELOCITY / LOW_VELOCITY`, or add a prominent comment: `// TEST VALUES — not competition velocities`.

---

## 5. Positive Findings

**Alliance variant via single-method override** — `DecodeTeleOp_Red extends DecodeTeleOp`, overriding only `getDefaultAlliance()`. Zero code duplication.

**AutoTurretSystem correctly handles alliance** — Has `setBlueAlliance()`/`setRedAlliance()` methods that update internal `goalX`/`goalY`. Called correctly from both `DecodeTeleOp.start()` and `DecodeTeleOp.init_loop()`. This is the right pattern; `ShootingSystem` and `AutoHoodSystem` need to adopt it.

**Auto→TeleOp turret offset persistence** — JVM `static` fields in `RobotMotors` elegantly bridge the OpMode boundary without requiring inter-process communication.

**Manual override + auto-resume** — When GP2 provides manual turret input, auto-tracking is suspended. On release, `recalibratePosition()` is called before resuming. This prevents the turret from snapping back to a stale computed position.

**Mode-switching blocked during auto-shoot** — Intake/launch mode toggle is guarded against running mid-sequence with a rumble warning. Prevents servo collisions.

**Limelight calibration is position-first, then turret** — `performLimelightCalibration()` corrects X/Y position before computing turret offset from `tx`. Order matters: the offset calculation uses the corrected position.

**Emergency stop on both controllers** — Both Xbox and PS4 button combos covered; either controller can trigger `robot.stopAll()`.

**Auto state timeout fallbacks** — Every `WAIT_*` state has `elapsed > TIMEOUT` protection. Autonomous will not hang on a subsystem failure.

**HSV-based color detection** — `RobotSensors` uses HSV with minimum saturation threshold (0.25) for ball color detection, making it robust to ambient light changes.

**Back color sensors intentionally disabled** — Commented out with an explanation that it reduces I2C bus polling time. This is a deliberate, documented tradeoff.

---

## 6. Recommendations

### Fix before competition (Critical)

1. **BUG-003** — Add alliance setters to `ShootingSystem` and `AutoHoodSystem`; call in all auto opModes and TeleOp `start()`. Red-alliance shots currently use wrong goal position for velocity and hood calculations.
2. **BUG-004** — Verify servo travel on physical robot; align `LONG_RANGE_POSITION` (0.27) with `MIN_POSITION` (0.28). One is wrong.
3. **BUG-005** — Unify TeleOp MID velocity with `RobotConstants.Shooter.MID_VELOCITY`. Currently 1160 vs 1300 — auto and TeleOp will perform differently at the same range.

### Fix soon (High priority)

4. **PERF-004** — Move/remove debug telemetry from the ESSENTIAL Driver Station page.
5. **BUG-006** — Replace hardcoded PIDF in `RobotMotors.init()` with `RobotConstants.Shooter.*` references to prevent silent divergence during tuning.
6. **BUG-007** — Unify `ballpush_initial_position` to `RobotConstants.BallPush.INITIAL_POSITION` in `RobotServos`. Remove the extra servo write from `ShootingSystem.init()`.
7. **BUG-001** — Align `ballpush_4` field name with hardware config name `"ballpush_2"`, or vice versa.

### Fix when time allows (Medium priority)

8. **BUG-008** — Use touch sensor for ball push retract confirmation; keep timer as a safety fallback.
9. **BUG-009** — Add `setPower(0)` before encoder reset in `hardReset()`.
10. **BUG-011** — Replace `kp = 0.015` in `LocalizationTest` with `RobotConstants.Turret.KP`.
11. **BUG-002** — Fix `TurretCalibration` to use `MAX_ROTATION_DEGREES` in its output; add re-enable guidance comment.
12. **PERF-002** — Refactor `ShootingSystem` to a loop-based N-ball state machine.
13. **PERF-003** — Replace gamepad-proxy velocity check in auto-calibration with actual odometry velocity.

### Cleanup (Low priority)

14. **BUG-010** — Remove `ballpush_final_position` dead field, or intentionally wire it in.
15. **PERF-005** — Document why TurretCalibration is disabled and add re-enable guidance.
16. **PERF-006** — Update `SystemTeleOpExample` to use real velocity constants or add a warning comment.

---

*End of Report — 11 bugs (4 High, 5 Medium, 2 Low) + 6 performance/maintainability issues identified*
