# ✅ SystemTeleOpExample - Basic Testing Configuration

## 🎯 What Changed

The `SystemTeleOpExample` program has been simplified and configured for **basic testing** with the following improvements:

### 1. ✅ **Intake Motor Auto-Start**
- **Intake motor now starts automatically** when the program initializes
- No need to manually turn it on - it's part of the intake system
- Can still toggle ON/OFF with Right Trigger if needed

### 2. ✅ **Shooter Motor Auto-Start**
- **Shooter motor starts automatically at 1450 RPM** (high velocity)
- Ready to shoot immediately when TeleOp begins
- Matches the working configuration from `Decode_TeleOp`

### 3. ✅ **Two Fixed Shooter Velocities**
- **HIGH (1450 RPM)**: Long distance shooting
- **LOW (1100 RPM)**: Short distance shooting
- Toggle between them with **Y/Triangle** button
- No more complicated auto-calculation - simple and reliable

### 4. ✅ **Auto-Turret and Auto-Hood Disabled**
- Both features **commented out** for basic testing
- You can test core functionality first
- Easy to re-enable later when ready

---

## 🎮 Controls Reference

### Gamepad 1 (Driver)

| Button | Function | Status |
|--------|----------|--------|
| **A / Cross** | Toggle Intake/Launch Mode | ✅ Active |
| **B / Circle** | ~~Auto-Turret~~ | ❌ Disabled |
| **X / Square** | ~~Auto-Hood~~ | ❌ Disabled |
| **Y / Triangle** | Toggle Velocity (1450↔1100) | ✅ Active |
| **Right Bumper** | Auto-Shoot 3 Balls | ✅ Active |
| **Left Bumper** | ~~Full Auto Mode~~ | ❌ Disabled |
| **Right Trigger** | Intake Motor ON/OFF | ✅ Active |
| **Left Trigger** | Shooter Motor ON/OFF | ✅ Active |
| **DPAD Left/Right** | Manual Turret Servo | ✅ Active |
| **Left Stick** | Drive (X/Y) | ✅ Active |
| **Right Stick X** | Drive (Rotation) | ✅ Active |

### Gamepad 2 (Operator)

| Button | Function | Status |
|--------|----------|--------|
| **Left Stick Y** | Variable Intake Speed | ✅ Active |

---

## 🚀 Auto-Start Behavior

When you **INIT** the program:

```
✅ Intake Motor: AUTO-STARTED (full power)
✅ Shooter Motor: AUTO-STARTED at 1450 RPM (HIGH velocity)
```

When you press **START**:
- Robot is immediately ready to shoot
- No need to press any buttons to activate motors
- Just drive and shoot!

---

## 📊 Telemetry Display

```
━━━ POSITION ━━━
X, Y: 0.0, 0.0
Heading: 0.0°

━━━ SHOOTING ━━━
Distance to Goal: 72.3 in
Shooter Motor: ✓ ON
Velocity Mode: HIGH (1450)
Current Velocity: 1448
Auto-Shoot: Idle
State: IDLE

━━━ INTAKE ━━━
Mode: 🔴 LAUNCH
Slot: 0
Ball Detected: ✗ NO
Color: Black
Intake Motor: ✓ ON
GP2 Intake Power: 0.00
```

---

## 🎯 How to Use

### Basic Shooting Workflow

1. **INIT Program** → Intake and Shooter auto-start
2. **Load balls** → Use intake mode to collect
3. **Switch to Launch Mode** → Press A/Cross
4. **Aim manually** → Use DPAD Left/Right for turret servo
5. **Shoot!** → Press Right Bumper for auto-shoot sequence

### Velocity Selection

**For Long Distance (e.g., far side of field):**
- Default mode: **HIGH (1450 RPM)** ✅ Already set
- Shooter already running at this velocity

**For Short Distance (e.g., close to basket):**
- Press **Y/Triangle** to switch to **LOW (1100 RPM)**
- Telemetry shows: `Velocity Mode: LOW (1100)`

**Switch back to high:**
- Press **Y/Triangle** again

---

## 🔧 Manual Controls (If Needed)

### Turn Shooter OFF (Emergency)
- Press and hold **Left Trigger** → Releases trigger → Motor stops

### Turn Shooter ON Again
- Press and hold **Left Trigger** → Motor starts at current velocity mode

### Turn Intake OFF
- Press and hold **Right Trigger** → Releases trigger → Motor stops

### Turn Intake ON Again
- Press and hold **Right Trigger** → Motor starts

---

## ⚠️ What's Disabled (For Testing)

These features are **commented out** and won't respond:

| Feature | Button | Why Disabled |
|---------|--------|--------------|
| Auto-Turret | B/Circle | Testing basic first |
| Auto-Hood | X/Square | Testing basic first |
| Full Auto Mode | Left Bumper | Depends on auto features |

---

## 🛠️ Re-Enabling Auto Features Later

When ready to test auto-turret and auto-hood:

1. Open `SystemTeleOpExample.java`
2. Find the commented sections marked with `/* ... */`
3. Remove the `/*` and `*/` to uncomment
4. Redeploy the program

**Search for these comments:**
```java
// B/Circle: Toggle Auto-Turret [COMMENTED OUT FOR BASIC TESTING]
// X/Square: Toggle Auto-Hood [COMMENTED OUT FOR BASIC TESTING]
// Left Bumper: Enable Full Auto Mode [COMMENTED OUT...]
```

---

## ✅ Testing Checklist

Use this checklist to verify everything works:

### At INIT:
- [ ] Telemetry shows "✅ Intake Motor: AUTO-STARTED"
- [ ] Telemetry shows "✅ Shooter Motor: AUTO-STARTED at 1450 RPM"
- [ ] You hear the intake motor running
- [ ] You hear the shooter motor spinning

### During Operation:
- [ ] Intake motor is running (collecting balls)
- [ ] Shooter motor is spinning at ~1450 RPM
- [ ] Press Y/Triangle → Velocity changes to 1100
- [ ] Press Y/Triangle again → Velocity changes back to 1450
- [ ] Press Right Trigger → Intake stops
- [ ] Release Right Trigger → Intake starts again
- [ ] Press Left Trigger → Shooter stops
- [ ] Release Left Trigger → Shooter starts again
- [ ] Press A/Cross → Mode switches (Intake ↔ Launch)
- [ ] Press Right Bumper (in Launch mode) → Auto-shoot fires 3 balls

---

## 🎓 Pro Tips

1. **Let motors warm up**: Give the shooter 2-3 seconds after INIT before shooting
2. **Check velocity**: Watch telemetry to confirm shooter reaches target velocity
3. **Practice velocity switching**: Get comfortable with Y/Triangle toggle
4. **Test both velocities**: Try shooting at both 1450 and 1100 to see the difference
5. **Keep it simple**: Stick with this basic setup until everything works perfectly

---

## 📝 Summary

| Feature | Old Behavior | New Behavior |
|---------|--------------|--------------|
| Intake Motor | Manual start | **Auto-start ON** |
| Shooter Motor | Manual start | **Auto-start at 1450** |
| Shooter Velocity | Auto-calculated | **Fixed: 1450 or 1100** |
| Velocity Control | Distance-based | **Y button toggle** |
| Auto-Turret | Available | **Disabled for testing** |
| Auto-Hood | Available | **Disabled for testing** |

---

## 🚦 Current Status

✅ **READY FOR BASIC TESTING**

- Motors auto-start
- Simple velocity toggle
- No complex auto features
- Easy to understand and operate
- Matches proven `Decode_TeleOp` configuration

Test this configuration first, then gradually re-enable auto features once the basics are working perfectly!

