# 🎮 SystemTeleOpExample - Current Configuration

## AUTO-START ON INIT ✅

```
Intake Motor  → ON (Full Power)
Shooter Motor → ON (1450 RPM - HIGH Velocity)
```

**You don't need to press any buttons - motors start automatically!**

---

## BUTTON MAPPING

### Gamepad 1 (Driver)

```
        Y/△             B/○ (disabled)
    X/□     A/✕
    
Y/△  → Toggle Velocity (1450 ↔ 1100)
A/✕  → Toggle Intake/Launch Mode
B/○  → [DISABLED] Auto-Turret
X/□  → [DISABLED] Auto-Hood

      ↑
  ←  DPAD  →     → Manual Turret Servo
      ↓

Left Trigger  → Shooter ON/OFF
Right Trigger → Intake ON/OFF

Left Bumper  → [DISABLED] Full Auto
Right Bumper → Auto-Shoot 3 Balls

Left Stick   → Drive (X/Y)
Right Stick  → Rotation (X axis only)
```

### Gamepad 2 (Operator)

```
Left Stick Y → Variable Intake Speed
```

---

## VELOCITY MODES

**DEFAULT: HIGH (1450 RPM)**
- For long distance shots
- Active on startup

**Press Y/△ to switch to: LOW (1100 RPM)**
- For short distance shots
- Press Y/△ again to go back to HIGH

---

## INTAKE MODES

**Launch Mode** (Red 🔴)
- Balls ready to shoot
- Default mode

**Intake Mode** (Blue 🔵)
- Collecting balls
- Press A/✕ to switch

---

## TELEMETRY LAYOUT

```
━━━ POSITION ━━━
X, Y: [position]
Heading: [angle]°

━━━ SHOOTING ━━━
Distance to Goal: [distance] in
Shooter Motor: ✓ ON / ✗ OFF
Velocity Mode: HIGH (1450) / LOW (1100)
Current Velocity: [actual RPM]
Auto-Shoot: Idle / 🎯 SHOOTING
State: [current state]

━━━ INTAKE ━━━
Mode: 🔵 INTAKE / 🔴 LAUNCH
Slot: [0-2]
Ball Detected: ✓ YES / ✗ NO
Color: [detected color]
Intake Motor: ✓ ON / ✗ OFF
GP2 Intake Power: [value]
```

---

## WHAT'S DISABLED

❌ Auto-Turret (B/Circle button)
❌ Auto-Hood (X/Square button)
❌ Full Auto Mode (Left Bumper)

**Reason:** Testing basics first

**To re-enable:** Uncomment code sections marked with `[COMMENTED OUT FOR BASIC TESTING]`

---

## QUICK OPERATION GUIDE

### Starting Up
1. Select "System TeleOp Example"
2. Press INIT
3. Wait for "✅ Robot System Initialized"
4. Press START
5. **You're ready! Motors are already running.**

### Shooting Long Distance
1. Robot is already at HIGH (1450) - no action needed
2. Aim with DPAD
3. Press Right Bumper to shoot

### Shooting Short Distance
1. Press Y/△ to switch to LOW (1100)
2. Aim with DPAD
3. Press Right Bumper to shoot
4. Press Y/△ again when done to go back to HIGH

### Emergency Stop
- Left Trigger → Stops Shooter
- Right Trigger → Stops Intake
- Release trigger to restart

---

## CURRENT FILE VERSION

**File:** `TeamCode/src/main/java/org/firstinspires/ftc/teamcode/decode14464/systems/SystemTeleOpExample.java`

**Key Settings:**
- `HIGH_VELOCITY = 1450`
- `LOW_VELOCITY = 1100`
- `intakeMotorOn = true` (auto-start)
- `shooterMotorOn = true` (auto-start)
- `isHighVelocityMode = true` (default)

**Dependencies:**
- `RobotSystem.java`
- `RobotMotors.java`
- `ShootingSystem.java`
- `AutoIntakeSystem.java`
- `LocalizationSystem.java`

---

## TESTING STATUS

✅ Ready for basic testing
✅ Motors auto-start
✅ Simple velocity control
✅ Auto features disabled
✅ No compilation errors

**Next:** Test on actual robot hardware

