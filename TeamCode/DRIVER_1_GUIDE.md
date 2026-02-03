# 🎮 Driver 1 Quick Reference Guide
## System TeleOp Example - Controls & Workflow

### Date: February 2, 2026
### Program: "System TeleOp Example"

---

## 🚀 **QUICK START (3 Steps)**

### 1. **Select Program**
- On Driver Station: **TeleOp** → **Systems** → **"System TeleOp Example"**

### 2. **Press INIT**
- Robot initializes all systems
- You'll see controls on Driver Station screen

### 3. **Press PLAY**
- Drive and use features!

---

## 🎮 **YOUR CONTROLS (Driver 1)**

### **DRIVING (Always Active)**
```
Left Stick Y:  Forward/Backward
Left Stick X:  Strafe Left/Right
Right Stick X: Rotate/Turn
```
**Note**: Field-centric drive - forward is always "up the field" regardless of robot rotation

---

## 🔵 **MODE CONTROL**

### **A Button / Cross ⭕** - Toggle Intake/Launch Mode
**What it does**: Switches between collecting balls and shooting balls

**INTAKE MODE** (collecting balls):
- Turret servo at intake positions (0.07, 0.44, 0.81)
- LED shows intake color
- Auto-advances when ball detected by color sensor
- **Use this when**: Picking up balls from field

**LAUNCH MODE** (shooting balls):
- Turret servo at launch positions (0.17, 0.54, 0.91)
- LED shows launch color
- Balls aligned for shooting
- **Use this when**: Ready to shoot at goal

**Your action**: Press **A** to switch between modes as needed

---

## 🎯 **AUTO-AIMING FEATURES**

### **B Button / Circle 🔴** - Toggle Auto-Turret
**What it does**: Robot automatically aims at goal

**When ENABLED** (🎯 ON):
- Turret motor automatically points toward goal (1, 144)
- Uses robot position to calculate angle
- Tracks target as you drive
- **You just drive, robot aims!**

**When DISABLED** (⭕ OFF):
- Manual turret control only

**Your action**: 
- Press **B** to enable auto-aim
- Drive around - turret follows goal automatically

---

### **X Button / Square ◻️** - Toggle Auto-Hood
**What it does**: Adjusts hood angle based on distance to goal

**When ENABLED** (🎯 ON):
- Hood servo adjusts automatically
- Closer = higher angle
- Farther = lower angle
- **You just drive, hood adjusts!**

**When DISABLED** (⭕ OFF):
- Manual hood control only

**Your action**:
- Press **X** to enable auto-hood
- Robot calculates optimal angle automatically

---

## 🔥 **SHOOTING**

### **Y Button / Triangle 🔺** - Toggle Shooter Motor
**What it does**: Turns flywheel motor on/off

**When ON**:
- Flywheel spins at calculated velocity (based on distance)
- Ready to shoot balls
- Makes spinning sound

**When OFF**:
- Flywheel stopped
- Can't shoot

**Your action**:
- Press **Y** to start shooter motor
- Wait ~1 second for it to spin up
- Press **Y** again to stop

---

### **Right Bumper (R1)** - Trigger Auto-Shoot Sequence
**What it does**: Automatically shoots all 3 balls

**Requirements**:
- ✅ Must be in **LAUNCH MODE** (press A if not)
- ✅ Shooter motor should be **ON** (press Y)
- ✅ Balls loaded in turret servo slots

**What happens**:
1. Shooter spins up (if not already)
2. Ball 1: Push → Retract → Turret moves → Wait
3. Ball 2: Push → Retract → Turret moves → Wait
4. Ball 3: Push → Retract → Done!

**Total time**: ~2 seconds for all 3 balls

**Your action**:
1. Make sure in launch mode (A button)
2. Turn on shooter (Y button)
3. Aim at goal (drive or use auto-turret)
4. Press **Right Bumper** once
5. Wait for all 3 balls to shoot
6. Drive away!

---

### **Left Bumper (L1)** - Full Auto Mode (Shoot From Anywhere!)
**What it does**: ONE BUTTON does EVERYTHING!

**This button**:
- ✅ Enables auto-turret (aims at goal)
- ✅ Enables auto-hood (adjusts angle)
- ✅ Starts shooter (calculates velocity)

**Your action** - THE EASY WAY:
1. Press **Left Bumper** (enables everything)
2. Drive to any position on field
3. Press **Right Bumper** to shoot all 3 balls
4. Robot does the rest!

**Perfect for**: Quick scoring during match

---

## 🕹️ **MANUAL OVERRIDES**

### **DPAD Left ⬅️** - Move Turret Servo to Previous Slot
**What it does**: Manually cycles turret servo backward

**Use when**: Need to reposition balls manually

---

### **DPAD Right ➡️** - Move Turret Servo to Next Slot
**What it does**: Manually cycles turret servo forward

**Use when**: Need to reposition balls manually

---

## 📋 **TYPICAL MATCH WORKFLOW**

### **COLLECTING BALLS (Start of Match)**

1. **Press A** → Switch to **INTAKE MODE**
   - LED changes to intake color
   
2. **Drive to balls** on field
   - Use left stick to drive
   - Field-centric - push forward = robot goes "up field"

3. **Balls auto-detect**
   - Color sensor sees ball
   - Turret servo auto-advances to next slot
   - Repeat until all 3 slots full

4. **Ready to shoot!**
   - You now have 3 balls loaded

---

### **SHOOTING (Quick Method)**

1. **Press A** → Switch to **LAUNCH MODE**
   - Balls now aligned for shooting

2. **Press Left Bumper** (L1)
   - Auto-turret: ON
   - Auto-hood: ON
   - Shooter: ON

3. **Drive near goal area**
   - Robot aims automatically
   - Hood adjusts automatically

4. **Press Right Bumper** (R1)
   - All 3 balls shoot automatically
   - Takes ~2 seconds

5. **Done!** Go collect more balls

---

### **SHOOTING (Manual Method)**

1. **Press A** → Switch to **LAUNCH MODE**

2. **Press Y** → Start shooter motor
   - Wait ~1 second for spin-up

3. **Press B** → Enable auto-turret
   - Robot aims at goal

4. **Press X** → Enable auto-hood
   - Hood adjusts for distance

5. **Drive into position**
   - Turret tracks goal as you move

6. **Press Right Bumper** (R1)
   - All 3 balls shoot

---

## 🎯 **DRIVER TIPS**

### **Best Practices**:

1. **Enable auto-turret early**
   - Press **B** as soon as you're ready to shoot
   - Let robot do the aiming while you drive

2. **Use Left Bumper for speed**
   - One button enables everything
   - Fastest way to shoot

3. **Stay in launch mode when shooting**
   - Don't switch to intake mode while shooting
   - Safety system prevents shooting in intake mode

4. **Watch telemetry**
   - Shows if turret is "ON TARGET"
   - Shows shooter velocity
   - Shows current mode

5. **Field-centric driving**
   - Forward stick = always toward opponent goal
   - Makes driving more intuitive
   - Required for accurate position tracking

---

## ⚠️ **IMPORTANT SAFETY**

### **DO NOT**:
- ❌ Try to shoot while in INTAKE MODE
  - Safety system prevents this
  - Switch to launch mode first (press A)

- ❌ Manually move turret servo during auto-shoot
  - State machine controls timing
  - Let it finish sequence

- ❌ Rapid-press shoot button
  - Wait for current sequence to finish
  - Takes ~2 seconds per 3-ball sequence

---

## 🎮 **CONTROL SUMMARY TABLE**

| Button | Function | When to Use |
|--------|----------|-------------|
| **A / Cross** | Toggle Intake/Launch | Switching between collecting & shooting |
| **B / Circle** | Auto-Turret ON/OFF | When you want robot to aim |
| **X / Square** | Auto-Hood ON/OFF | When you want auto angle adjustment |
| **Y / Triangle** | Shooter Motor ON/OFF | Before shooting |
| **R1 / Right Bumper** | Shoot All 3 Balls | When ready to score |
| **L1 / Left Bumper** | Full Auto Mode | Quick enable everything |
| **DPAD Left** | Manual Turret Back | Manual adjustments |
| **DPAD Right** | Manual Turret Forward | Manual adjustments |
| **Left Stick** | Drive | Always |
| **Right Stick X** | Rotate | Always |

---

## 🏆 **MATCH STRATEGY**

### **Fast Cycle (15-20 seconds)**:

1. **Collect** (Intake mode, drive to balls) - 5 sec
2. **Switch** (Press A to launch mode) - 1 sec
3. **Enable** (Press L1 for full auto) - 1 sec
4. **Position** (Drive near goal) - 3 sec
5. **Shoot** (Press R1) - 2 sec
6. **Repeat!**

### **Goal**: 3-4 cycles per match = 9-12 balls scored!

---

## 📊 **WHAT YOU'LL SEE (Telemetry)**

```
━━━ POSITION ━━━
X, Y: 72.0, 50.0
Heading: 45.0°

━━━ AUTO SYSTEMS ━━━
Auto-Turret: ✓ ON
Auto-Hood: ✓ ON
Turret Angle: 15.3°
On Target: ✓ YES

━━━ SHOOTING ━━━
Distance to Goal: 98.5 in
Shooter: ✓ ACTIVE
Velocity: 1350
Auto-Shoot: 🎯 SHOOTING
State: PUSH_BALL_2

━━━ INTAKE ━━━
Mode: 🔴 LAUNCH
Slot: 1
Ball Detected: ✗ NO
Color: Black
```

---

## 🎓 **TRAINING SEQUENCE**

### **Practice in this order**:

1. **Week 1**: Just drive (left stick, right stick)
2. **Week 2**: Practice mode switching (A button)
3. **Week 3**: Enable auto-turret (B button) + drive
4. **Week 4**: Full shooting sequence
5. **Week 5**: Speed runs - how fast can you cycle?

---

## ✅ **PRE-MATCH CHECKLIST**

Before each match:
- [ ] Verify robot initializes (press INIT)
- [ ] Test drive controls (left stick, right stick)
- [ ] Test mode switch (A button)
- [ ] Test auto-turret (B button)
- [ ] Test shooter (Y button)
- [ ] Practice 1 full cycle (collect → shoot)
- [ ] Confirm with Driver 2 on roles

---

## 🤝 **COORDINATION WITH DRIVER 2**

**You (Driver 1)** handle:
- ✅ Driving (position on field)
- ✅ Mode switching (intake/launch)
- ✅ Enabling auto features (turret, hood)
- ✅ Triggering shooting

**Driver 2** can help with:
- Fine tune shooter velocity (if needed)
- Manual hood adjustments
- Intake motor control
- Override auto systems if needed

**Communication**: Call out "Collecting", "Shooting", "3 balls loaded", etc.

---

## 💡 **REMEMBER**

The robot does most of the work for you:
- ✅ **You drive** - robot aims
- ✅ **You position** - robot calculates angle
- ✅ **You trigger** - robot shoots all balls
- ✅ **You focus on strategy** - robot handles mechanics

**Your job**: Get to the right position at the right time!

---

**Good luck in competition!** 🏆🤖

You've got this! The robot is designed to make your job easier - just drive, press buttons, score points! 🎯

