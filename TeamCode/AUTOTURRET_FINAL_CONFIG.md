# ✅ AutoTurret - Final Configuration

## 🎯 Rotation Range Set

Your AutoTurret is now configured for **±150° rotation** from center!

---

## 📐 Configuration Details

### Rotation Range:
```
Center Position: 150°
│
├─ Clockwise Limit:     0° (150° clockwise from center)
│
└─ Anticlockwise Limit: 300° (150° anticlockwise from center)

Total Range: 300° (0° to 300°)
From Center: ±150°
```

### Updated Values:
```java
CENTER_POSITION_DEGREES = 150.0   // Starting/center position
MIN_ROTATION_DEGREES = 0.0        // 150° clockwise from center
MAX_ROTATION_DEGREES = 300.0      // 150° anticlockwise from center
```

---

## 🎮 How It Works

### Starting Position:
- **When you initialize**: Encoder resets to 0
- **Press Circle**: Turret moves to **150°** (center position)
- This is your **neutral/forward** facing position

### Rotation Limits:
- **Clockwise rotation**: From 150° down to **0°** (150° range)
- **Anticlockwise rotation**: From 150° up to **300°** (150° range)
- **Total coverage**: 300° sweep

### Visual Representation:
```
        0° (Clockwise Limit)
        ↓
    ←150° rotation→
        
    [CENTER: 150°]
    (Neutral/Forward)
        
    ←150° rotation→
        ↓
    300° (Anticlockwise Limit)
```

---

## 🎯 Testing Your Setup

### Step 1: Initialize and Center
1. Run **Auto Turret**
2. Press **Circle** button
3. Turret moves to **150°** (center)
4. This is your starting/neutral position

### Step 2: Test Clockwise Range
1. Use **Right Stick** or **D-Pad Down** in manual mode
2. Turret should rotate **clockwise** toward 0°
3. Watch telemetry: Position decreases (150° → 100° → 50° → 0°)
4. Should stop at **0°** with warning: "Near MIN limit!"

### Step 3: Test Anticlockwise Range
1. Press **Circle** to return to center (150°)
2. Use **Right Stick** or **D-Pad Up** in manual mode
3. Turret should rotate **anticlockwise** toward 300°
4. Watch telemetry: Position increases (150° → 200° → 250° → 300°)
5. Should stop at **300°** with warning: "Near MAX limit!"

---

## 📊 Telemetry Display

When running, you'll see:
```
=================================
  AUTO TURRET STATUS
=================================
Mode: MANUAL (or AUTO-TRACKING)
---------------------------------
Turret Position: 150.0°          ← Current position
Encoder Ticks: 750               ← (150° × 5 ticks/degree)
Motor Power: 0.000
Direction: ■ (Stopped)
---------------------------------
Rotation Range: 0° - 300°        ← Your ±150° range
Center Position: 150°             ← Neutral position
=================================
```

### Position Indicators:
- **150°** = Center (neutral/forward)
- **< 150°** = Clockwise from center
- **> 150°** = Anticlockwise from center
- **0°** = Maximum clockwise
- **300°** = Maximum anticlockwise

---

## 🔧 Important Notes

### 1. Encoder Reset on Init
- When you run the program, encoder resets to **0 ticks**
- This corresponds to **0°** in software
- **Physical position doesn't change** - only encoder reading
- Use **Circle button** to move to center (150°) after init

### 2. Center Position (150°)
- **Why 150°?** This gives equal ±150° range
- Think of it as your "forward" or "neutral" position
- Always return to center with **Circle** button
- Auto-tracking works best from center position

### 3. Soft Limits
- Turret **slows down** 10° before limits
- At **10°** (near 0° limit) → power reduced
- At **290°** (near 300° limit) → power reduced
- Prevents hard stops and mechanical stress

---

## 🎯 Auto-Tracking Behavior

### From Center (150°):
When you enable auto-tracking:
- Tag to the **right** → Turret rotates anticlockwise (toward 300°)
- Tag to the **left** → Turret rotates clockwise (toward 0°)
- Tag **centered** → Turret stays at current position

### Maximum Tracking Range:
- Can track from **0° to 300°** (full 300° sweep)
- **±150° from center** gives excellent coverage
- Most targets should be within this range

---

## ⚙️ Configuration Summary

```java
// Position Configuration
CENTER_POSITION_DEGREES = 150.0   // Neutral position
MIN_ROTATION_DEGREES = 0.0        // Clockwise limit
MAX_ROTATION_DEGREES = 300.0      // Anticlockwise limit

// Encoder Calculation
ENCODER_TICKS_PER_DEGREE = 5.0    // TUNE based on your motor
centerEncoderPosition = 750       // (150° × 5)
minEncoderPosition = 0            // (0° × 5)
maxEncoderPosition = 1500         // (300° × 5)

// PID Tuning (Current Balanced Settings)
AUTO_TRACK_KP = 0.025             // Proportional gain
AUTO_TRACK_KD = 0.010             // Derivative damping
MAX_TURRET_POWER = 0.5            // 50% max power
DEADBAND = 2.0                    // ±2° tolerance
MIN_POWER_THRESHOLD = 0.08        // 8% minimum power
INVERT_TURRET_DIRECTION = true    // Direction correction
```

---

## 🧪 Quick Test Checklist

Before using in competition:

**Basic Movement:**
- [ ] Turret initializes successfully
- [ ] Circle button moves to 150° (center)
- [ ] Manual control works both directions
- [ ] Stops at 0° (clockwise limit)
- [ ] Stops at 300° (anticlockwise limit)

**Auto-Tracking:**
- [ ] Limelight connected
- [ ] Tag 20 or 24 detected
- [ ] Auto-tracking follows tag smoothly
- [ ] No oscillation or bouncing
- [ ] Locks on and shows "LOCKED ON ✓"
- [ ] Tracks in both directions from center

**Safety:**
- [ ] Soft limits slow down before hard stops
- [ ] Telemetry shows warnings near limits
- [ ] Emergency stop works (manual mode)
- [ ] No mechanical binding throughout range

---

## 💡 Usage Tips

### 1. Always Start from Center
- After init, press **Circle** to go to 150°
- This gives you equal range in both directions
- Best starting position for auto-tracking

### 2. Monitor Position
- Watch "Turret Position" in telemetry
- **< 50°** or **> 250°** → getting near limits
- Return to center if you're far from 150°

### 3. Optimize Your Robot Positioning
- Position robot so targets are **near center** (150°)
- Avoids hitting rotation limits during tracking
- Better performance in middle of range

### 4. Use Circle Button Often
- Quick way to return to neutral
- Resets auto-tracking
- Centers the turret for next shot

---

## 🎯 Next Steps

Now that the range is set correctly:

1. **Test the physical rotation**
   - Verify 0° and 300° don't hit mechanical limits
   - Ensure no cable/wire interference
   - Check turret moves smoothly throughout range

2. **Calibrate ENCODER_TICKS_PER_DEGREE**
   - Currently set to 5.0 (placeholder)
   - Manually rotate turret exactly 90°
   - Note encoder change: `ticks / 90 = ticks per degree`

3. **Fine-tune PID later** (as you mentioned)
   - Current settings: KP=0.025, KD=0.010
   - Adjust based on your testing
   - Balance speed vs. smoothness

4. **Integrate with shooting**
   - Add flywheel control
   - Add ball pusher control
   - Complete auto-aiming shooter system

---

## ✅ Configuration Complete!

Your AutoTurret now has:
- ✅ **±150° rotation** from center (0° to 300°)
- ✅ **Center position at 150°**
- ✅ **Smooth PID tracking**
- ✅ **Bidirectional movement**
- ✅ **Soft limits** for safety
- ✅ **Ready for tuning** later

**The turret is ready to test!** 🎯✨


