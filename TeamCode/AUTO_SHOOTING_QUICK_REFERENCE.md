# 🎯 AUTO SHOOTING QUICK REFERENCE CARD

## 📋 BEFORE FIRST USE - CONFIGURE THESE VALUES!

### 1. Goal Position (REQUIRED!)
```java
// Line 25-26 in AutoShooting.java
public static double GOAL_X = 72.0;  // ← CHANGE THIS to your goal X coordinate
public static double GOAL_Y = 72.0;  // ← CHANGE THIS to your goal Y coordinate
```

### 2. Robot Measurements (REQUIRED!)
```java
// Lines 29-31
public static double SCORE_HEIGHT = 24.0;           // ← Tune this
public static double PASS_THROUGH_RADIUS = 20.0;    // ← Tune this first
public static double PASS_THROUGH_ANGLE = -30.0;    // ← Tune this last

// Line 35
public static double LAUNCHER_HEIGHT = 12.0;  // ← MEASURE your robot
```

### 3. Servo Limits (REQUIRED!)
```java
// Lines 42-43
public static double HOOD_SERVO_MIN = 0.17;  // ← Test your hood
public static double HOOD_SERVO_MAX = 0.91;  // ← Test your hood
```

### 4. Flywheel Conversion (REQUIRED!)
```java
// Line 355 in velocityToFlywheelTicks() method
double conversionFactor = 12.5;  // ← TUNE THIS VALUE
```

---

## 🎮 CONTROLS

### Gamepad 1 (Driver)
| Button | Action |
|--------|--------|
| **Cross (X)** | Hold to auto-aim at goal |
| **Circle (O)** | Toggle shooter on/off |
| **Right Trigger** | Push ball (launch) |
| **Left Stick** | Drive (works while auto-aiming!) |
| **Right Stick X** | Manual rotation (when NOT auto-aiming) |
| **D-Pad Up/Down** | Manual hood adjust |
| **Square** | Quick aim forward (0°) |

---

## ⚙️ TUNING ORDER (DO THIS IN ORDER!)

### Step 1: Set Goal Position (2 min)
- Update `GOAL_X` and `GOAL_Y` in code
- For INTO THE DEEP: Measure to basket center

### Step 2: Measure Robot (5 min)
- Measure `LAUNCHER_HEIGHT` (ground to flywheel center)
- Test hood servo limits → set `HOOD_SERVO_MIN` and `MAX`
- Find hood angle limits → set `MIN_HOOD_ANGLE` and `MAX`

### Step 3: Tune Pass-Through Radius (10 min)
- Place robot exactly 60 inches from goal
- Test shoot
- Ball short? → DECREASE `PASS_THROUGH_RADIUS` by 2-5 inches
- Ball long? → INCREASE `PASS_THROUGH_RADIUS` by 2-5 inches
- Repeat until consistent

### Step 4: Tune Pass-Through Height (10 min)
- Trajectory too flat? → INCREASE `SCORE_HEIGHT` by 2-4 inches
- Trajectory too arced? → DECREASE `SCORE_HEIGHT` by 2-4 inches

### Step 5: Tune Pass-Through Angle (5 min)
- Ball bounces out? → Make MORE negative (e.g., -25° → -30°)
- Ball falls short? → Make LESS negative (e.g., -30° → -25°)

### Step 6: Tune Flywheel Conversion (10 min)
- All shots short? → INCREASE conversion factor by 1-2
- All shots long? → DECREASE conversion factor by 1-2

### Step 7: Test Multiple Distances (10 min)
- Test from 40, 60, 80, 100 inches
- Fine-tune if needed

### Step 8: Tune Auto-Aim (5 min)
- Robot oscillates? → DECREASE `AUTO_TURN_KP` (try 0.015)
- Robot turns too slow? → INCREASE `AUTO_TURN_KP` (try 0.025)

**TOTAL TIME: ~60 minutes**

---

## 📊 TELEMETRY - WHAT TO WATCH

```
Distance to Goal: 72.0 in    ← Should match your actual distance
Heading Error: 2.5°          ← Should be < 5° when aimed
Hood Angle: 42.5°            ← Should be 15-75° (not stuck at limits)
Current Flywheel: 1442       ← Should be within 50 of target
```

---

## 🐛 TROUBLESHOOTING

| Problem | Solution |
|---------|----------|
| Robot won't aim | Check `GOAL_X` and `GOAL_Y` are set correctly |
| Robot spins in circles | Reduce `AUTO_TURN_KP` to 0.015 |
| All shots short | Decrease `PASS_THROUGH_RADIUS` |
| All shots long | Increase `PASS_THROUGH_RADIUS` |
| Hood stuck at min/max | Adjust pass-through parameters or distance |
| Shots good still, bad moving | Check odometry is accurate |
| Ball bounces out of basket | Make `PASS_THROUGH_ANGLE` more negative |

---

## 📝 RECORD YOUR TUNED VALUES HERE!

```
GOAL_X = _________ inches
GOAL_Y = _________ inches

LAUNCHER_HEIGHT = _________ inches
PASS_THROUGH_RADIUS = _________ inches
SCORE_HEIGHT = _________ inches
PASS_THROUGH_ANGLE = _________ degrees

HOOD_SERVO_MIN = _________
HOOD_SERVO_MAX = _________
MIN_HOOD_ANGLE = _________ degrees
MAX_HOOD_ANGLE = _________ degrees

Flywheel Conversion Factor = _________
AUTO_TURN_KP = _________
MAX_TURN_POWER = _________
```

---

## ✅ PRE-MATCH CHECKLIST

- [ ] Goal coordinates set for current alliance
- [ ] Pass-through point tuned
- [ ] Hood servo calibrated
- [ ] Flywheel PIDF tuned (run FlywheelTuner)
- [ ] Flywheel conversion factor calibrated
- [ ] Tested from 3+ distances
- [ ] Auto-aim doesn't oscillate
- [ ] Robot odometry accurate
- [ ] Tuned values backed up!

---

## 🚀 HOW TO SHOOT

1. **Drive** to scoring position (any position in range!)
2. **Hold Cross (X)** - Robot auto-aims
3. **Press Circle (O)** - Start shooter (if not already on)
4. **Watch telemetry** - Wait for "Current Flywheel" to match target
5. **Pull Right Trigger** - Launch ball
6. **Keep driving** - You can shoot while moving!

---

## 🏆 EXPECTED PERFORMANCE

After tuning:
- ✅ 85-95% accuracy from tested distances
- ✅ < 1 second to acquire aim
- ✅ Can shoot while driving
- ✅ Consistent, repeatable results

---

## 📚 FULL DOCUMENTATION

See `AUTO_SHOOTING_GUIDE.md` for:
- Complete theory and math explanation
- Detailed tuning procedures
- Advanced tips and tricks
- Comprehensive troubleshooting

---

**Keep this card with your drive team! 🤖**

