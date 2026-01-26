# 🎯 AutoTurret in Decode_Teleop - Quick Guide

## ✅ What Was Implemented

AutoTurret functionality has been fully integrated into your **Decode_Teleop** program using the utility functions from DecodeUtils!

---

## 🎮 Controls

### Gamepad 1 (Driver) - Existing Controls
- **Left Stick** - Drive robot
- **Right Stick X** - Manual robot rotation
- **Left Bumper** - Toggle intake ON/OFF
- **Right Bumper** - Toggle shooter ON/OFF
- **Right Trigger** - Ball pusher
- **A Button** - Toggle intake/launch mode
- **D-Pad Left/Right** - Adjust turret servo position

### Gamepad 2 (Gunner) - Turret Controls ⭐ NEW!
| Button | Function |
|--------|----------|
| **Cross (X)** | Enable auto-tracking (turret aims at AprilTag) |
| **Circle (O)** | Disable auto-tracking (manual control) |
| **Triangle** | Switch between tag 20 ↔ tag 24 |
| **Right Stick Y** | Manual turret control (when auto-tracking OFF) |

---

## 🎯 How to Use Auto-Tracking

### Step 1: Position Robot
Drive your robot (Gamepad 1) so the Limelight camera can see the AprilTag (20 or 24) on the basket.

### Step 2: Enable Auto-Tracking
**Gamepad 2:** Press **Cross (X)**
- Turret will automatically aim at the detected AprilTag
- Watch telemetry for "🎯 Auto-Track: ENABLED"

### Step 3: Wait for Lock-On
Watch telemetry:
- **"Tag Visible: YES ✓"** - Limelight sees the tag
- **"Status: TRACKING..."** - Turret is moving toward target
- **"Status: LOCKED ON ✓"** - Turret is centered on target!

### Step 4: Shoot!
**Gamepad 1:** 
1. Press **Right Bumper** to start shooter (if not already running)
2. Pull **Right Trigger** to push ball into shooter

### Step 5: Manual Mode (if needed)
**Gamepad 2:** Press **Circle (O)** to disable auto-tracking
- Use **Right Stick Y** for manual turret control
- Useful if tag is blocked or you want manual aim

---

## 📊 Telemetry Display

While running, you'll see:

```
🎯 Auto-Track: ENABLED         ← or "Manual"
Target Tag: 20                 ← Currently targeting tag 20 or 24
Tag Visible: YES ✓             ← Is Limelight seeing the tag?
Status: LOCKED ON ✓            ← or "TRACKING..."
Error: 1.2°                    ← How far off target
Turret Position: 145.2° (4.8° clockwise from center)
⚠️ WARNING: Turret near rotation limit!  ← If approaching limits

(Plus all your existing telemetry...)
Color BR: Red
Shooter Current Velocity: 1498
...
```

---

## 🎯 Usage Scenarios

### Scenario 1: Quick Auto-Aim and Shoot
```
1. Driver (GP1) positions robot facing basket
2. Gunner (GP2) presses Cross (X) → Auto-tracking ON
3. Wait for "LOCKED ON ✓" in telemetry
4. Driver (GP1) pulls Right Trigger → Ball launches
5. Repeat!
```

### Scenario 2: Switch Between Multiple Goals
```
1. Auto-tracking enabled, aiming at tag 20
2. Want to aim at different basket?
3. Gunner (GP2) presses Triangle → Switches to tag 24
4. Turret automatically re-aims at new target
5. Shoot when locked on!
```

### Scenario 3: Fallback to Manual
```
1. Auto-tracking enabled but tag blocked by opponent
2. Telemetry shows "Tag Visible: NO ✗"
3. Gunner (GP2) presses Circle (O) → Manual mode
4. Use Right Stick Y to aim manually
5. Shoot when aimed
6. Later: Press Cross (X) to re-enable auto-tracking
```

---

## 🔧 Features

### ✅ Automatic Tag Detection
- Uses Limelight to detect AprilTags 20 or 24
- Real-time position tracking
- Automatically compensates for tag movement

### ✅ PID Control
- Smooth, controlled aiming
- No oscillation or jerky movements
- Configurable speed and smoothness (via TurretConstants.java)

### ✅ Safety Limits
- Turret limited to 0° - 300° rotation (±150° from center)
- Automatic slowdown near limits
- Warning displayed when approaching limits

### ✅ Manual Override
- Can switch to manual control anytime
- Useful for precise adjustments or blocked tags
- PID state resets when switching modes

### ✅ Multi-Target Support
- Switch between tag 20 and tag 24 instantly
- No need to re-enable tracking
- Smooth transition between targets

---

## 💡 Pro Tips

### 1. Pre-Position for Best Results
- Position robot so Limelight has clear view of tag
- Closer is better (within 3-6 feet ideal)
- Avoid extreme angles

### 2. Watch Telemetry
- "Tag Visible: YES ✓" means system is working
- "LOCKED ON ✓" means ready to shoot
- "Error: X.X°" shows how precise the aim is

### 3. Use Manual Mode When Needed
- If tag is blocked by opponent → switch to manual
- For quick shots without perfect aim → manual is faster
- Practice both modes!

### 4. Combine with Shooter
- Start shooter early (it takes time to spin up)
- Wait for "LOCKED ON" before shooting
- Better accuracy = more points!

### 5. Know Your Limits
- Turret can't rotate past 300° (warning shown)
- If you see warning, reposition robot
- Don't force turret against limits

---

## 🎮 Control Mapping Summary

```
GAMEPAD 1 (DRIVER):
├─ Left Stick → Drive
├─ Right Stick X → Robot rotation
├─ Left Bumper → Intake toggle
├─ Right Bumper → Shooter toggle
├─ Right Trigger → Ball pusher
└─ A Button → Mode switch

GAMEPAD 2 (GUNNER):
├─ Cross (X) → Enable auto-tracking    ⭐
├─ Circle (O) → Disable auto-tracking  ⭐
├─ Triangle → Switch tag (20 ↔ 24)    ⭐
└─ Right Stick Y → Manual turret       ⭐
```

---

## 🔧 Configuration (If Needed)

All turret settings are in **TurretConstants.java**:

```java
// PID tuning (adjust if turret is too slow/fast/oscillates)
AUTO_TRACK_KP = 0.035  // Speed
AUTO_TRACK_KD = 0.022  // Smoothness
MAX_TURRET_POWER = 0.6 // Max power

// Rotation limits
CENTER_POSITION_DEGREES = 150.0
MAX_ROTATION_DEGREES = 300.0
MIN_ROTATION_DEGREES = 0.0

// AprilTag IDs
PRIMARY_TAG_ID = 20
SECONDARY_TAG_ID = 24
```

**You can adjust these without changing Decode_Teleop!**

---

## ✅ What's Reused

The implementation uses:
- ✅ Your existing **RobotMotors.turret** motor
- ✅ Your existing **LimelightManager** for tag detection
- ✅ **DecodeUtils** utility functions (no code duplication!)
- ✅ **TurretConstants** for all configuration

**Result:** Clean, maintainable code with minimal additions to Decode_Teleop!

---

## 🎯 Testing Checklist

Before competition, verify:

**Hardware:**
- [ ] Limelight connected and working
- [ ] AprilTags 20 and 24 mounted near baskets
- [ ] Turret motor responds to manual control
- [ ] Rotation limits don't hit mechanical stops

**Software:**
- [ ] Gamepad 2 Cross enables auto-tracking
- [ ] Telemetry shows "Tag Visible: YES ✓"
- [ ] Turret aims at tag automatically
- [ ] "LOCKED ON ✓" appears when centered
- [ ] Manual mode works (Circle + Right Stick)
- [ ] Tag switching works (Triangle)

**Integration:**
- [ ] Auto-tracking + shooter + ball pusher = successful shot
- [ ] Can switch between tags 20 and 24
- [ ] Manual override works when tag blocked
- [ ] Telemetry shows accurate status

---

## 🆘 Troubleshooting

### "Tag Visible: NO ✗" even with tag in front
- Check Limelight is connected ("Limelight: Connected" in init)
- Verify tag is ID 20 or 24 (run TagReader to confirm)
- Ensure good lighting and tag is flat
- Try getting closer to tag

### Turret doesn't move when auto-tracking enabled
- Check "Tag Visible" status
- Verify turret isn't at rotation limit
- Check telemetry for errors
- Try manual mode to verify motor works

### Turret oscillates (bounces back and forth)
- Adjust TurretConstants.AUTO_TRACK_KD (increase for more damping)
- Or reduce TurretConstants.AUTO_TRACK_KP (less aggressive)
- See AUTOTURRET_LIVE_TUNING_GUIDE.md for tuning

### "WARNING: Turret near rotation limit!"
- Reposition robot
- Turret is approaching 0° or 300° limit
- Don't force it - move robot instead

---

## 🎉 Summary

Your **Decode_Teleop** now has:
- ✅ Full auto-tracking capability
- ✅ Manual override
- ✅ Multi-target support (tags 20 and 24)
- ✅ Safety limits
- ✅ Comprehensive telemetry
- ✅ Clean integration with existing controls

**Ready for competition! Good luck! 🏆**


