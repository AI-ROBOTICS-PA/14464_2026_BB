# 🔧 AutoTurret Direction Fix Guide

## 🎯 Problem: Turret Only Goes One Direction

**Symptom:** Turret rotates anticlockwise when tag moves right, but doesn't come back when tag moves left (or vice versa).

**Root Cause:** The motor power calculation or motor direction is inverted.

---

## ✅ How the Fix Works

### What Was Added:

1. **INVERT_TURRET_DIRECTION Flag** (Line 49)
   ```java
   public static boolean INVERT_TURRET_DIRECTION = false;
   ```
   - Set to `true` if turret goes the wrong direction
   - Set to `false` for normal operation

2. **Enhanced Telemetry**
   - Shows motor power with direction arrows
   - Shows what tx means (tag left/right)
   - Visual indicators: `→→→`, `←←←`, `■`

---

## 🧪 Testing Procedure

### Step 1: Check Current Behavior

Run AutoTurret and watch telemetry:

```
Motor Power: 0.150        ← Positive power
Direction: →→→ (Positive)  ← Turning one way

Horizontal Offset (tx): 5.23°
tx Meaning: Tag is RIGHT of center
```

**Test:**
1. Move tag to the **RIGHT** of camera center
2. Watch `tx` value (should be **positive**, like +5.0)
3. Watch `Motor Power` and `Direction`
4. **Question:** Does turret rotate to **follow** the tag?

### Step 2: Test Opposite Direction

1. Move tag to the **LEFT** of camera center
2. Watch `tx` value (should be **negative**, like -5.0)
3. Watch `Motor Power` and `Direction`
4. **Question:** Does turret rotate to **follow** the tag?

---

## 🔍 Diagnosis Chart

### Scenario A: Turret Follows Correctly ✅

**Tag RIGHT (+tx) → Turret turns RIGHT**
**Tag LEFT (-tx) → Turret turns LEFT**

**This is CORRECT!** No changes needed.

```
tx: +5.0° (tag right)
Motor Power: +0.15
Direction: →→→ (Positive)
✓ Turret correctly turns right
```

```
tx: -5.0° (tag left)
Motor Power: -0.15
Direction: ←←← (Negative)
✓ Turret correctly turns left
```

---

### Scenario B: Turret Goes Opposite Direction ❌

**Tag RIGHT (+tx) → Turret turns LEFT (wrong!)**
**Tag LEFT (-tx) → Turret turns RIGHT (wrong!)**

**This is INVERTED!**

```
tx: +5.0° (tag right)
Motor Power: +0.15
Direction: →→→ (Positive)
✗ But turret turns LEFT (opposite of power sign)
```

**FIX:** Set `INVERT_TURRET_DIRECTION = true`

This inverts the power calculation to match your motor wiring.

---

### Scenario C: Turret Only Goes One Way ❌

**Tag RIGHT (+tx) → Turret turns RIGHT**
**Tag LEFT (-tx) → Turret does NOTHING or turns same way**

```
tx: +5.0° (tag right)
Motor Power: +0.15
Direction: →→→ (Positive)
✓ Turret turns right

tx: -5.0° (tag left)
Motor Power: -0.15  ← Shows negative
Direction: ←←← (Negative)
✗ But turret doesn't move or keeps going right!
```

**Possible Causes:**
1. Motor is hitting rotation limit
2. Motor controller only accepts positive power
3. Motor direction is set wrong in hardware config

**FIXES:**

**Fix 1: Check Rotation Limits**
- Look at telemetry: `Turret Position: X°`
- Is it at 0° or 270° (the limits)?
- If yes → motor is physically blocked
- Manually move turret to center (135°) and try again

**Fix 2: Check Motor Direction in init()**
- Line 78: `turretMotor.setDirection(DcMotor.Direction.FORWARD);`
- Try changing to: `turretMotor.setDirection(DcMotor.Direction.REVERSE);`
- This changes motor wiring interpretation

**Fix 3: Use Absolute Power**
- If motor only accepts positive power
- You may need custom motor control logic
- Contact me for advanced fix

---

## ⚙️ Configuration Steps

### If Turret Goes Opposite Direction:

**Option 1: Use INVERT_TURRET_DIRECTION**

In `AutoTurret.java` line 49, change:
```java
public static boolean INVERT_TURRET_DIRECTION = true;  // Changed to true
```

**Option 2: Change Motor Direction**

In `AutoTurret.java` line 78, change:
```java
turretMotor.setDirection(DcMotor.Direction.REVERSE);  // Changed to REVERSE
```

**Try Option 1 first!** It's easier and doesn't require restarting the robot.

---

## 📊 Understanding tx Values

### What tx Means:

- **tx = 0°**: Tag is perfectly centered in camera
- **tx = +5°**: Tag is 5 degrees to the RIGHT of center
- **tx = -5°**: Tag is 5 degrees to the LEFT of center

### Expected Behavior:

**Goal:** Make tx = 0 (center the tag)

**When tx is positive (+):**
- Tag is RIGHT of center
- Turret should turn RIGHT
- Motor power should be POSITIVE

**When tx is negative (-):**
- Tag is LEFT of center
- Turret should turn LEFT
- Motor power should be NEGATIVE

### PID Logic:
```
error = tx
power = KP × error

If tx = +5°:
  power = 0.015 × 5 = +0.075
  → Positive power → Turn right

If tx = -5°:
  power = 0.015 × (-5) = -0.075
  → Negative power → Turn left
```

If this is backwards, set `INVERT_TURRET_DIRECTION = true`

---

## 🎯 Step-by-Step Fix

### Your Issue: "Motor goes only anticlockwise, doesn't come back"

**Step 1: Enable Auto-Tracking**
1. Hold tag in front of camera
2. Press Cross (X) to enable tracking
3. Watch telemetry

**Step 2: Move Tag RIGHT**
1. Move tag to right side of camera
2. Check telemetry:
   - `tx`: Should be POSITIVE (e.g., +5.0)
   - `Motor Power`: Note the value and sign
   - `Direction`: Note the arrow direction
   - **Actual turret movement**: Which way does it ACTUALLY turn?

**Step 3: Move Tag LEFT**
1. Move tag to left side of camera
2. Check telemetry:
   - `tx`: Should be NEGATIVE (e.g., -5.0)
   - `Motor Power`: Note the value and sign
   - `Direction`: Note the arrow direction
   - **Actual turret movement**: Which way does it ACTUALLY turn?

**Step 4: Analyze Results**

| tx Value | Motor Power | Expected Rotation | Actual Rotation | Problem? |
|----------|-------------|-------------------|-----------------|----------|
| +5.0     | +0.15       | RIGHT (→)         | ?               | ?        |
| -5.0     | -0.15       | LEFT (←)          | ?               | ?        |

**If Actual doesn't match Expected:**
→ Set `INVERT_TURRET_DIRECTION = true`

**If Motor Power doesn't change sign:**
→ Check PID calculation or motor controller

**If turret hits limit and stops:**
→ Reset to center with Circle button

---

## 🔧 Quick Fixes

### Fix 1: Invert Direction (Most Common)

```java
// Line 49 in AutoTurret.java
public static boolean INVERT_TURRET_DIRECTION = true;  // SET TO TRUE
```

Deploy and test again.

---

### Fix 2: Change Motor Hardware Direction

```java
// Line 78 in AutoTurret.java
turretMotor.setDirection(DcMotor.Direction.REVERSE);  // Change to REVERSE
```

This is equivalent to Fix 1 but changes hardware interpretation.

---

### Fix 3: Adjust PID Gain (If Oscillating)

If turret goes back and forth rapidly:

```java
// Line 43 in AutoTurret.java
public static double AUTO_TRACK_KP = 0.010;  // REDUCE from 0.015
```

Lower KP = slower, smoother tracking

---

### Fix 4: Check Rotation Limits

If turret stops at one end:

1. Press **Circle** button to reset to center
2. Try tracking again
3. If still stuck, check:
   ```java
   // Line 34-35
   public static double MAX_ROTATION_DEGREES = 270.0;
   public static double MIN_ROTATION_DEGREES = 0.0;
   ```
   Make sure these match your physical turret limits!

---

## 📋 Troubleshooting Checklist

After deploying the fix:

**Basic Checks:**
- [ ] Limelight Connected
- [ ] Tag Visible: YES ✓
- [ ] Auto-Tracking Enabled (Cross pressed)

**Direction Test:**
- [ ] Move tag RIGHT → tx becomes positive (+)
- [ ] Move tag RIGHT → Motor Power becomes positive (+)
- [ ] Move tag RIGHT → Turret rotates to follow
- [ ] Move tag LEFT → tx becomes negative (-)
- [ ] Move tag LEFT → Motor Power becomes negative (-)
- [ ] Move tag LEFT → Turret rotates to follow

**If all checked → Turret tracking works!** ✅

**If any fail:**
1. Note which ones fail
2. Check telemetry values
3. Apply appropriate fix above

---

## 🎯 Expected Results

### After Fix:

**Test 1: Tag Moves Right**
```
tx: +8.5° (tag right)
Motor Power: +0.128
Direction: →→→ (Positive)
✓ Turret rotates RIGHT to follow
```

**Test 2: Tag Moves Left**
```
tx: -8.5° (tag left)
Motor Power: -0.128
Direction: ←←← (Negative)
✓ Turret rotates LEFT to follow
```

**Test 3: Tag Centered**
```
tx: 0.3° (almost centered)
Motor Power: 0.005
Direction: ■ (Stopped)
Status: LOCKED ON ✓
✓ Turret stops when centered
```

---

## 💡 Pro Tips

1. **Start with tag centered**
   - Easier to see which direction it tracks first

2. **Move tag slowly**
   - Gives turret time to respond
   - Easier to observe behavior

3. **Watch telemetry values**
   - tx sign should match tag position
   - Motor Power sign should match tx sign
   - If signs don't match → invert needed

4. **Test both directions**
   - Some issues only show in one direction
   - Make sure it tracks BOTH ways

5. **Reset to center if stuck**
   - Press Circle button
   - Turret returns to 135° (middle)
   - Try tracking again

---

## 🆘 Still Not Working?

### If turret STILL only goes one way after fixes:

**Check these:**

1. **Encoder position**
   - Is it at 0 or 1350 (limits)?
   - Turret might be physically blocked
   - Reset to center and try again

2. **Motor controller**
   - Some motor controllers need special config
   - Check Robot Configuration settings
   - Verify motor type is correct

3. **Wiring**
   - Loose wire = intermittent power
   - Check all connections
   - Try wiggling wires while testing

4. **Power supply**
   - Low battery = weak motor
   - Check battery voltage
   - Fully charge and test again

5. **Mechanical binding**
   - Turret might be catching on something
   - Check for obstructions
   - Manually rotate turret freely?

---

## ✅ Success Criteria

You know it's working when:

1. ✓ Move tag right → turret follows right
2. ✓ Move tag left → turret follows left
3. ✓ Tag centered → turret stops
4. ✓ Motor Power changes sign with tag direction
5. ✓ Telemetry shows "LOCKED ON ✓" when centered

**Once all 5 work → AutoTurret is fully functional!** 🎉

---

**Deploy the fix and test with tag in both directions. Tell me what you see in telemetry!**


