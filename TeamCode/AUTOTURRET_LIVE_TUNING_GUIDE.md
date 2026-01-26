# 🎮 AutoTurret - Live Tuning Controls Guide

## 🎉 What's New - Live PID Tuning!

You can now adjust **KP, KD, and Max Power** on-the-fly **without redeploying** code! Just like FlywheelTuner, you can tune the turret in real-time during testing.

---

## 🎮 Complete Controls

### Gamepad 1 (Driver) - Turret Operation
| Button | Function |
|--------|----------|
| **Cross (X)** | Toggle auto-tracking ON/OFF |
| **Triangle** | Switch between tag 20 ↔ 24 |
| **Circle** | Reset turret to center (150°) |
| **Square** | Change step size (0.1 → 0.01 → 0.001 → 0.0001) |
| **Right Stick X** | Manual turret control |

### Gamepad 2 (Tuner) - Live PID Tuning ⭐ NEW!
| Button | Function | Adjusts |
|--------|----------|---------|
| **D-Pad UP** | Increase KP | Proportional gain (speed) |
| **D-Pad DOWN** | Decrease KP | Proportional gain (speed) |
| **D-Pad RIGHT** | Increase KD | Derivative gain (smoothness) |
| **D-Pad LEFT** | Decrease KD | Derivative gain (smoothness) |
| **Right Bumper** | Increase Max Power | Motor power limit |
| **Left Bumper** | Decrease Max Power | Motor power limit |

---

## 🎯 How to Use Live Tuning

### Step 1: Start Testing
1. Run **Auto Turret** program
2. Press **Cross (X)** to enable auto-tracking
3. Watch turret track the tag

### Step 2: Observe Behavior
Watch for these issues:
- **Too slow?** → Increase KP or Max Power
- **Oscillating?** → Increase KD or decrease KP
- **Jerky?** → Increase KD
- **Overshoots?** → Decrease KP or increase KD

### Step 3: Adjust Values (Gamepad 2)

**If turret is TOO SLOW:**
1. Press **D-Pad UP** to increase KP
2. OR press **Right Bumper** to increase Max Power
3. Watch telemetry - KP value increases
4. Test tracking - should be faster now

**If turret OSCILLATES (bounces back and forth):**
1. Press **D-Pad RIGHT** to increase KD (adds damping)
2. OR press **D-Pad DOWN** to decrease KP (less aggressive)
3. Watch telemetry - values update
4. Test tracking - should be smoother

**If turret OVERSHOOTS:**
1. Press **D-Pad RIGHT** to increase KD
2. Watch it slow down more gently

### Step 4: Fine-Tune with Step Size
1. Press **Square** to change step size
2. Cycles: 0.1 → 0.01 → 0.001 → 0.0001
3. Use larger steps (0.1) for big changes
4. Use smaller steps (0.001) for fine-tuning

### Step 5: Record Your Values!
Once you find good values, **write them down**:
```
KP: _______ (shown in telemetry)
KD: _______ (shown in telemetry)
Max Power: _______ (shown in telemetry)
```

Then update the code permanently:
```java
public static double AUTO_TRACK_KP = _____; // Your value
public static double AUTO_TRACK_KD = _____; // Your value
public static double MAX_TURRET_POWER = _____; // Your value
```

---

## 📊 Telemetry Display

You'll see a new tuning section at the bottom:

```
=================================
  AUTO TURRET STATUS
=================================
Mode: AUTO-TRACKING
---------------------------------
Target Tag ID: 20
Tag Visible: YES ✓
Limelight: Connected
---------------------------------
Turret Position: 145.2°
Motor Power: 0.287
Direction: ←←← (Negative)
---------------------------------
Horizontal Offset (tx): -3.45°
Status: TRACKING...
---------------------------------
Rotation Range: 0° - 300°
---------------------------------
  TUNING VALUES (Gamepad 2)      ← NEW SECTION!
---------------------------------
KP: 0.0350                        ← Current proportional gain
KD: 0.0120                        ← Current derivative gain
Max Power: 0.60                   ← Current power limit
Step Size: 0.0100                 ← Current adjustment increment

Adjust: D-Pad Up/Down (KP)
        D-Pad L/R (KD)
        Bumpers (Max Power)
        Square (Step Size)
=================================
```

---

## 🔧 Tuning Workflow

### Starting Values (Current Defaults):
```
KP = 0.035
KD = 0.012
Max Power = 0.6 (60%)
Step Size = 0.01
```

### Problem: Turret is slow and takes long to track

**Solution:**
1. **Increase KP** using D-Pad UP
   - Try: 0.035 → 0.045 → 0.055
   - Watch tracking speed increase
   - Stop when fast enough

2. **Increase Max Power** using Right Bumper
   - Try: 0.60 → 0.70 → 0.80
   - Gives more torque
   - Stop when satisfied

**Test:** Move tag - turret should follow quickly

---

### Problem: Turret oscillates (bounces back and forth)

**Solution:**
1. **Increase KD** using D-Pad RIGHT
   - Try: 0.012 → 0.020 → 0.030
   - Adds damping/braking
   - Reduces overshoot

2. **OR Decrease KP** using D-Pad DOWN
   - Try: 0.035 → 0.030 → 0.025
   - Less aggressive response
   - Smoother approach

**Test:** Move tag - should track smoothly without bouncing

---

### Problem: Turret overshoots target then comes back

**Solution:**
1. **Increase KD** using D-Pad RIGHT
   - More damping = slower approach
   - Try: 0.012 → 0.025
   - Should reduce overshoot

**Test:** Should slow down as it approaches, gentle stop

---

### Problem: Adjustments too large or too small

**Solution:**
1. Press **Square** to change step size
2. Step sizes available:
   - **0.1** - Big changes (rough tuning)
   - **0.01** - Medium changes (normal tuning)
   - **0.001** - Small changes (fine-tuning)
   - **0.0001** - Tiny changes (precision tuning)

**Example:**
- Start with 0.01 for normal adjustments
- Switch to 0.001 for final precision
- Switch to 0.1 if you need big changes quickly

---

## 💡 Understanding the Parameters

### KP (Proportional Gain) - SPEED
- **What it does:** Controls how strongly turret responds to error
- **Higher KP** → Faster response, more aggressive
- **Lower KP** → Slower response, more gentle

**Formula:** `Power = KP × Error`

**Example:**
- Error = 10°, KP = 0.035 → Power = 0.35 (35%)
- Error = 10°, KP = 0.070 → Power = 0.70 (70%) - twice as fast!

**Typical Range:** 0.01 to 0.10
- Below 0.02 = Very slow
- 0.02-0.04 = Moderate speed
- 0.04-0.06 = Fast
- Above 0.06 = Very aggressive (may oscillate)

---

### KD (Derivative Gain) - SMOOTHNESS
- **What it does:** Applies braking based on rate of change
- **Higher KD** → More damping, smoother motion, less overshoot
- **Lower KD** → Less damping, may bounce/overshoot

**Formula:** `Damping = KD × (Error - Previous Error)`

**When to increase KD:**
- Turret oscillates back and forth
- Turret overshoots target
- Movement is jerky or bouncy
- After increasing KP (to compensate)

**Typical Range:** 0.005 to 0.030
- Below 0.005 = Very little damping
- 0.005-0.015 = Moderate damping
- 0.015-0.030 = Strong damping
- Above 0.030 = May be too sluggish

---

### Max Power - TORQUE LIMIT
- **What it does:** Limits maximum motor power
- **Higher Max Power** → More torque, faster movement
- **Lower Max Power** → Less torque, more controllable

**Range:** 0.1 to 1.0 (10% to 100%)

**Recommendations:**
- **0.3-0.5** = Conservative, very controllable
- **0.5-0.7** = Balanced speed and control
- **0.7-0.9** = Fast, aggressive
- **0.9-1.0** = Maximum speed (may be hard to control)

**Current:** 0.6 (60%) - good balance

---

## 🎯 Recommended Tuning Process

### Phase 1: Set Max Power (30 seconds)
1. Start with Max Power = 0.6
2. Test tracking speed
3. If too slow → increase to 0.7 or 0.8
4. If too fast → decrease to 0.5
5. Find comfortable speed

### Phase 2: Tune KP for Speed (2-3 minutes)
1. Set step size to 0.01
2. Start with KP = 0.035
3. Test tracking
4. **Too slow?** → D-Pad UP (increase)
5. **Too fast/aggressive?** → D-Pad DOWN (decrease)
6. Find balance between speed and control

### Phase 3: Tune KD for Smoothness (2-3 minutes)
1. Keep step size at 0.01
2. Start with KD = 0.012
3. Test tracking
4. **Oscillates?** → D-Pad RIGHT (increase KD)
5. **Overshoots?** → D-Pad RIGHT (increase KD)
6. **Too sluggish?** → D-Pad LEFT (decrease KD)
7. Find smooth, controlled motion

### Phase 4: Fine-Tune (2-3 minutes)
1. Change step size to 0.001 (press Square)
2. Make small KP/KD adjustments
3. Test from different positions
4. Get perfect balance

**Total Time: ~10 minutes to find optimal values!**

---

## ✅ Success Criteria

You know tuning is good when:
1. ✓ Turret follows tag **quickly** but not too fast
2. ✓ **No oscillation** or bouncing
3. ✓ **Smooth approach** to target
4. ✓ **Gentle stop** when locked on
5. ✓ Status shows "LOCKED ON ✓" consistently
6. ✓ Works well from **various starting positions**

---

## 📝 Example Tuning Session

**Starting values:**
```
KP: 0.035
KD: 0.012
Max Power: 0.60
```

**Observation:** Turret is slow and oscillates a bit

**Tuning steps:**

1. **Increase speed** - Press D-Pad UP twice
   ```
   KP: 0.035 → 0.045 → 0.055
   ```
   Result: Faster, but now oscillates more

2. **Add damping** - Press D-Pad RIGHT three times
   ```
   KD: 0.012 → 0.022 → 0.032 → 0.042
   ```
   Result: No oscillation, but seems too slow to stop

3. **Fine-tune KD** - Press Square to change step to 0.001, then D-Pad LEFT a few times
   ```
   KD: 0.042 → 0.038 → 0.035
   ```
   Result: Perfect! Smooth and controlled

**Final values:**
```
KP: 0.055  (increased for speed)
KD: 0.035  (increased for smoothness)
Max Power: 0.60 (kept same)
```

**Write these down and update code permanently!**

---

## 🆘 Troubleshooting

### Values won't change when I press buttons
- Check you're using **Gamepad 2** (not Gamepad 1)
- Make sure program is running (not in init)
- Check step size isn't too small to notice

### Changed values but turret behavior same
- Auto-tracking must be **enabled** (press Cross)
- Values only apply during auto-tracking
- Tag must be visible

### Accidentally made it worse
- Press **Circle** to reset turret
- Redeploy code to restore defaults
- Or manually adjust back using opposite buttons

### Can't tell what's changing
- Watch telemetry "TUNING VALUES" section
- Values update in real-time
- Compare before/after numbers

---

## 💾 Saving Your Tuned Values

Once you find perfect values:

1. **Write them down** from telemetry:
   ```
   KP: 0.055
   KD: 0.035
   Max Power: 0.75
   ```

2. **Update code** (lines 44-47):
   ```java
   public static double AUTO_TRACK_KP = 0.055;
   public static double AUTO_TRACK_KD = 0.035;
   public static double MAX_TURRET_POWER = 0.75;
   ```

3. **Redeploy** to make permanent

**Or** keep using live tuning every session!

---

## 🎯 Quick Reference

**Turret too slow:**
→ D-Pad UP (increase KP) or Right Bumper (more power)

**Turret oscillates:**
→ D-Pad RIGHT (increase KD) or D-Pad DOWN (decrease KP)

**Turret overshoots:**
→ D-Pad RIGHT (increase KD)

**Need finer control:**
→ Square (change step size)

**Reset everything:**
→ Circle (reset turret) then redeploy code

---

**Happy tuning! You can now perfect your turret tracking without constant redeployment!** 🎮✨


