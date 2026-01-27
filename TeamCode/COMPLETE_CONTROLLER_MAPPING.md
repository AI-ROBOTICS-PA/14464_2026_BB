# Complete Controller Mapping Guide 🎮

## FTC Robot Controller Mapping - Full Reference

This document shows **ALL** button and stick mappings for both PlayStation and Xbox controllers used with your FTC robot.

---

## 🎮 Gamepad 1 (Driver) - Complete Mapping

### Face Buttons

| Function | PlayStation | Xbox | Description |
|----------|-------------|------|-------------|
| **Mode Switch** | Cross (✕) | A | Toggle between INTAKE and LAUNCH modes |
| **Shooter Velocity** | Triangle (△) | Y | Toggle between HIGH (long distance) and LOW (short distance) velocity |
| **[Not Used]** | Circle (○) | B | Currently not assigned |
| **[Not Used]** | Square (□) | X | Currently not assigned |

### Bumpers & Triggers

| Function | Button | Description |
|----------|--------|-------------|
| **Intake Control** | Left Bumper (L1/LB) | Toggle intake motor ON/OFF |
| **Shooter Control** | Right Bumper (R1/RB) | Toggle shooter motor ON/OFF |
| **Ball Push Servo** | Right Trigger (R2/RT) | Control ball push servo position (0.0 to 0.27 range) |
| **[Not Used]** | Left Trigger (L2/LT) | Currently not assigned |

### Analog Sticks

| Stick/Axis | Function | Description |
|------------|----------|-------------|
| **Left Stick Y** | Drive Forward/Backward | Robot movement forward (up) and backward (down) |
| **Left Stick X** | Strafe Left/Right | Robot movement left and right (mecanum drive) |
| **Right Stick X** | Rotate | Robot rotation clockwise (right) and counter-clockwise (left) |
| **Right Stick Y** | [Not Used] | Currently not assigned |

### D-Pad

| Button | Function | Description |
|--------|----------|-------------|
| **D-Pad Up** | Hood Increment | Increase hood servo position (+0.01) |
| **D-Pad Down** | Hood Decrement | Decrease hood servo position (-0.01) |
| **D-Pad Left** | Turret Position Left | Move to previous turret position (left in current mode) |
| **D-Pad Right** | Turret Position Right | Move to next turret position (right in current mode) |

### Special Buttons

| Button | PlayStation | Xbox | Description |
|--------|-------------|------|-------------|
| **Start** | Options | Start/Menu | [Not Used] |
| **Select** | Share | Back/View | [Not Used] |
| **L3** | Left Stick Click | Left Stick Click | [Not Used] |
| **R3** | Right Stick Click | Right Stick Click | [Not Used] |

---

## 🎮 Gamepad 2 (Operator) - Complete Mapping

### Auto-Turret Mode Control

| Function | PlayStation | Xbox | Description |
|----------|-------------|------|-------------|
| **Enable Auto-Track** | Cross (✕) | A | Enable automatic turret tracking |
| **Disable Auto-Track** | Circle (○) | B | Switch to manual turret control |
| **Switch Target Tag** | Triangle (△) | Y | Toggle between AprilTag 20 and 24 |
| **[Not Used]** | Square (□) | X | Currently not assigned |

### Manual Turret Control (When Auto-Track Disabled)

| Control | Function | Description |
|---------|----------|-------------|
| **Right Stick Y** | Continuous Turret Control | Primary manual control (60% max speed, inverted) |
| **Left Stick X** | Alternative Turret Control | Secondary control option (40% max speed) |
| **D-Pad Up** | Fine Adjust Positive | Precise positive rotation (20% power) |
| **D-Pad Down** | Fine Adjust Negative | Precise negative rotation (20% power) |

### Control Priority (Manual Mode)

When in manual turret mode, controls have the following priority:
1. **D-Pad** (highest priority) - Fine adjustment at 20% power
2. **Right Stick Y** - Continuous control at 60% max speed
3. **Left Stick X** (lowest priority) - Alternative control at 40% max speed

### Bumpers & Triggers

| Button | Description |
|--------|-------------|
| **Left Bumper** | Currently not assigned |
| **Right Bumper** | Currently not assigned |
| **Left Trigger** | Currently not assigned |
| **Right Trigger** | Currently not assigned |

### Special Buttons

| Button | PlayStation | Xbox | Description |
|--------|-------------|------|-------------|
| **Start** | Options | Start/Menu | [Not Used] |
| **Select** | Share | Back/View | [Not Used] |
| **L3** | Left Stick Click | Left Stick Click | [Not Used] |
| **R3** | Right Stick Click | Right Stick Click | [Not Used] |

---

## 📊 Detailed Control Descriptions

### 1. Mode Switch (Cross/A Button)

**PlayStation:** Press **Cross (✕)**  
**Xbox:** Press **A**

**What it does:**
- Toggles between **INTAKE mode** and **LAUNCH mode**
- In INTAKE mode:
  - Turret servo goes to index 0 (position 0.07)
  - Color servo set to position 1.0
  - Auto-advance enabled (uses color sensor)
  - Positions: 0.07, 0.44, 0.81
- In LAUNCH mode:
  - Turret servo finds closest launch position
  - Color servo set to position 0.38
  - Positions: 0.17, 0.54, 0.91

**Telemetry feedback:**
- "✅ Switched to INTAKE mode - Reset to Index 0"
- "✅ Switched to LAUNCH mode"

---

### 2. Shooter Velocity Toggle (Triangle/Y Button)

**PlayStation:** Press **Triangle (△)**  
**Xbox:** Press **Y**

**What it does:**
- Toggles between **HIGH velocity** (1450) and **LOW velocity** (1100)
- HIGH velocity = Long distance shooting
- LOW velocity = Short distance shooting
- If shooter is already running, updates velocity immediately

**Telemetry feedback:**
- "🚀 Switched to HIGH velocity (long distance)"
- "🎯 Switched to LOW velocity (short distance)"
- "✅ Shooter velocity updated" (if shooter already active)

---

### 3. Intake Control (Left Bumper)

**Both Controllers:** **Left Bumper (L1/LB)**

**What it does:**
- Toggles intake motor ON/OFF
- First press: Starts intake motor
- Second press: Stops intake motor
- Edge-detected (won't rapid toggle)

---

### 4. Shooter Control (Right Bumper)

**Both Controllers:** **Right Bumper (R1/RB)**

**What it does:**
- Toggles shooter motor ON/OFF
- First press: Starts shooter at current velocity setting
- Second press: Stops shooter motor
- Uses velocity selected by Triangle/Y button
- Edge-detected (won't rapid toggle)

---

### 5. Ball Push Servo (Right Trigger)

**Both Controllers:** **Right Trigger (R2/RT)**

**What it does:**
- Analog control of ball push servo
- Trigger position maps to servo position
- Formula: `initial_position + (trigger_value × 0.27)`
- Smooth analog control (not on/off)
- Used to push balls into shooter

**Range:**
- Trigger released (0.0): Initial position
- Trigger fully pressed (1.0): Initial + 0.27

---

### 6. Drive Controls (Left Stick)

**Both Controllers:** **Left Analog Stick**

**What it does:**
- **Y-Axis (Up/Down):** Forward and backward movement
  - Up = Forward
  - Down = Backward
- **X-Axis (Left/Right):** Strafe movement (mecanum wheels)
  - Left = Strafe left
  - Right = Strafe right

**Features:**
- Field-oriented drive (maintains heading relative to field)
- Slow mode available (50% speed multiplier)
- Inverted Y-axis (up = negative, down = positive in code)

---

### 7. Rotation Control (Right Stick X)

**Both Controllers:** **Right Analog Stick X-Axis**

**What it does:**
- Rotates robot clockwise and counter-clockwise
- Right = Clockwise rotation
- Left = Counter-clockwise rotation

**Features:**
- Independent of drive movement
- Can rotate while moving in any direction
- Affected by slow mode

---

### 8. Hood Servo Controls (D-Pad Up/Down)

**Both Controllers:** **D-Pad Up/Down**

**What it does:**
- **D-Pad Up:** Increases hood servo position by 0.01
- **D-Pad Down:** Decreases hood servo position by 0.01

**Range:**
- Minimum: 0.28
- Maximum: 0.80
- Clamped to prevent exceeding limits

**Purpose:**
- Adjusts shooting angle
- Fine-tune trajectory
- Edge-detected for precise control

---

### 9. Turret Position Controls (D-Pad Left/Right)

**Both Controllers:** **D-Pad Left/Right**

**What it does:**
- **D-Pad Left:** Move to previous turret position in current mode
- **D-Pad Right:** Move to next turret position in current mode

**In INTAKE Mode:**
- Positions: 0.07 ← 0.44 ← 0.81
- Left decreases index, Right increases index

**In LAUNCH Mode:**
- Positions: 0.17 ← 0.54 ← 0.91
- Left decreases index, Right increases index

**Features:**
- Wraps around (at position 0, left goes to position 2)
- Edge-detected (one press = one position change)
- Independent for each mode

---

## 🎮 Gamepad 2 Controls - Auto-Turret System

### 10. Enable Auto-Track (Cross/A Button)

**PlayStation:** Press **Cross (✕)**  
**Xbox:** Press **A**

**What it does:**
- Enables automatic turret tracking mode
- Turret will automatically aim at AprilTag
- Uses Limelight camera for vision tracking
- Implements PID control for smooth tracking

**When enabled:**
- Turret follows detected AprilTag
- Shows "🎯 Auto-Track: ENABLED" in telemetry
- Displays tracking status and error
- Manual controls are disabled

---

### 11. Disable Auto-Track (Circle/B Button)

**PlayStation:** Press **Circle (○)**  
**Xbox:** Press **B**

**What it does:**
- Disables automatic tracking
- Switches to manual turret control mode
- Operator takes full control of turret position

**When disabled:**
- Shows "🎯 Auto-Track: MANUAL MODE" in telemetry
- Gamepad 2 sticks and D-Pad control turret
- PID controller is reset

---

### 12. Switch Target Tag (Triangle/Y Button)

**PlayStation:** Press **Triangle (△)**  
**Xbox:** Press **Y**

**What it does:**
- Toggles between AprilTag ID 20 and ID 24
- Only affects auto-tracking mode
- Allows tracking different field positions

**Telemetry shows:**
- Current target tag ID (20 or 24)
- Whether tag is visible

---

### 13. Manual Turret Control - Right Stick Y (Primary)

**Both Controllers:** **Right Analog Stick Y-Axis**

**What it does:**
- Primary manual control of turret rotation
- Up = Rotate in positive direction
- Down = Rotate in negative direction
- Maximum speed: 60% power
- Inverted control (up = negative in code)

**Features:**
- Smooth analog control
- Only active when auto-track disabled
- Overridden by D-Pad if D-Pad pressed
- Shows "Right Stick" as control source in telemetry

---

### 14. Manual Turret Control - Left Stick X (Alternative)

**Both Controllers:** **Left Analog Stick X-Axis**

**What it does:**
- Alternative manual control of turret rotation
- Right = Rotate in positive direction
- Left = Rotate in negative direction
- Maximum speed: 40% power (slower than right stick)

**Features:**
- Backup control option
- Only active when auto-track disabled
- Lowest priority (overridden by D-Pad and Right Stick)
- Shows "Left Stick" as control source in telemetry

---

### 15. Fine Turret Adjustment - D-Pad Up/Down

**Both Controllers:** **D-Pad Up/Down**

**What it does:**
- **D-Pad Up:** Precise positive rotation at 20% power
- **D-Pad Down:** Precise negative rotation at 20% power

**Features:**
- Highest priority in manual mode
- Constant power (not analog)
- Best for fine positioning
- Shows "D-Pad" as control source in telemetry

**Use cases:**
- Fine-tuning turret position
- Small adjustments when near target
- Precise alignment for shooting

---

## 🎯 Quick Reference Card

### Most Used Controls

```
┌─────────────────────────────────────────┐
│         DRIVER CONTROLS (GP1)           │
├─────────────────────────────────────────┤
│ Left Stick     → Drive/Strafe           │
│ Right Stick X  → Rotate                 │
│                                          │
│ Left Bumper    → Intake                 │
│ Right Bumper   → Shooter                │
│ Right Trigger  → Ball Push               │
│                                          │
│ Cross/A        → Mode Switch             │
│ Triangle/Y     → Velocity Toggle         │
│                                          │
│ D-Pad ↕        → Hood Adjust             │
│ D-Pad ← →      → Turret Position         │
└─────────────────────────────────────────┘

┌─────────────────────────────────────────┐
│       OPERATOR CONTROLS (GP2)           │
├─────────────────────────────────────────┤
│ Cross/A        → Enable Auto-Track       │
│ Circle/B       → Manual Control          │
│ Triangle/Y     → Switch Tag (20↔24)      │
│                                          │
│ === MANUAL MODE (when auto disabled) ===│
│ Right Stick Y  → Turret Control (60%)    │
│ Left Stick X   → Alt Control (40%)       │
│ D-Pad ↕        → Fine Adjust (20%)       │
│                                          │
│ Control Priority: D-Pad > R-Stick > L-Stick │
└─────────────────────────────────────────┘
```

---

## 📱 Mode-Specific Behaviors

### INTAKE Mode
- **Purpose:** Collecting game elements
- **Turret Positions:** 0.07, 0.44, 0.81
- **Auto-Advance:** Enabled (color sensor detects balls)
- **Color Servo:** 1.0
- **Default Start:** Index 0 (position 0.07)

**Controls in INTAKE:**
- D-Pad Left/Right: Manual position change
- Color sensor: Auto-advance on ball detection
- Mode switch (Cross/A): Switch to LAUNCH

### LAUNCH Mode
- **Purpose:** Shooting game elements
- **Turret Positions:** 0.17, 0.54, 0.91
- **Auto-Advance:** Disabled
- **Color Servo:** 0.38
- **Start Position:** Closest to previous position

**Controls in LAUNCH:**
- D-Pad Left/Right: Manual position change
- Shooter velocity (Triangle/Y): HIGH/LOW toggle
- Mode switch (Cross/A): Switch to INTAKE

---

## 🔄 Auto-Advance Feature (INTAKE Mode Only)

**How it works:**
1. Robot in INTAKE mode at position 0 (0.07)
2. Ball enters → Color sensor detects non-black color
3. **Automatic:** Turret advances to position 1 (0.44)
4. Ball exits → Color sensor returns to black/none
5. System ready for next ball
6. Second ball enters → Auto-advance to position 2 (0.81)

**Colors detected:**
- Red, Blue, Yellow → Triggers advance
- Black or None → Ready state (no advance)

**Manual override:**
- D-Pad Left/Right still works during auto-advance
- Can manually move between positions anytime

---

## 🎨 Visual Controller Layout

### PlayStation Controller

```
         L2                    R2
         L1                    R1
    ┌─────────────────────────┐
    │    □      △             │
    │       ✕                 │
    │    ✕     ○              │
    │                          │
    │  [D-Pad]        [Sticks] │
    │    ↑                     │
    │  ←   →                   │
    │    ↓                     │
    └─────────────────────────┘

□ = Square     (not used)
✕ = Cross      (MODE SWITCH)
○ = Circle     (not used)
△ = Triangle   (VELOCITY TOGGLE)
```

### Xbox Controller

```
         LT                    RT
         LB                    RB
    ┌─────────────────────────┐
    │       Y                  │
    │    X     B               │
    │       A                  │
    │                          │
    │  [D-Pad]        [Sticks] │
    │    ↑                     │
    │  ←   →                   │
    │    ↓                     │
    └─────────────────────────┘

X = (not used)
A = (MODE SWITCH)
B = (not used)
Y = (VELOCITY TOGGLE)
```

---

## 📝 Control Priority & Edge Detection

### Edge Detection (Single Press)
These controls use edge detection - they trigger ONCE per button press:

- ✅ Mode Switch (Cross/A)
- ✅ Velocity Toggle (Triangle/Y)
- ✅ Intake Toggle (Left Bumper)
- ✅ Shooter Toggle (Right Bumper)
- ✅ Hood Up/Down (D-Pad Up/Down)
- ✅ Turret Position (D-Pad Left/Right)

### Continuous/Analog Controls
These controls update continuously while held:

- ✅ Drive Sticks (Left Stick)
- ✅ Rotation (Right Stick X)
- ✅ Ball Push Servo (Right Trigger)

---

## ⚙️ Configuration Constants

### Turret Positions
```java
INTAKE_POSITIONS:  [0.07, 0.44, 0.81]
LAUNCH_POSITIONS:  [0.17, 0.54, 0.91]
```

### Hood Servo
```java
MIN_POSITION: 0.28
MAX_POSITION: 0.80
INCREMENT:    0.01
```

### Shooter Velocities
```java
HIGH_VELOCITY: 1450 (long distance)
LOW_VELOCITY:  1100 (short distance)
```

### Drive Settings
```java
SLOW_MODE_MULTIPLIER: 0.5 (50% speed)
```

---

## 🚨 Troubleshooting

### "Button doesn't work"
**Check:**
1. Is it the correct button for your controller type?
2. PlayStation: Use Cross/Triangle
3. Xbox: Use A/Y
4. Are you using Gamepad 1 (not Gamepad 2)?

### "Mode won't switch"
**Check:**
1. Try both Cross (PlayStation) and A (Xbox)
2. Check telemetry for confirmation message
3. Make sure you're not holding the button too long

### "Velocity toggle not working"
**Check:**
1. Try both Triangle (PlayStation) and Y (Xbox)
2. Check telemetry for "HIGH/LOW velocity" message
3. Shooter must be active to see velocity change

---

## ✅ Summary

**Total Controls on Gamepad 1:**
- 2 Face Buttons (used)
- 2 Face Buttons (unused)
- 2 Bumpers
- 1 Trigger (analog)
- 2 Analog Sticks (3 axes used)
- 4 D-Pad directions
- **Total:** 14 active controls

**Total Controls on Gamepad 2:**
- 3 Face Buttons (used for mode control)
- 1 Face Button (unused)
- 0 Bumpers (unused)
- 0 Triggers (unused)
- 2 Analog Sticks (2 axes used - manual turret)
- 2 D-Pad directions (fine turret adjust)
- **Total:** 7 active controls

**Auto-Turret System:**
- ✅ Toggle between AUTO and MANUAL modes
- ✅ Vision-based tracking (Limelight + AprilTags)
- ✅ Manual control with 3 input methods
- ✅ Priority-based control selection
- ✅ Tag selection (ID 20 or 24)

**Controller Compatibility:**
- ✅ PlayStation DualShock 4
- ✅ PlayStation DualSense
- ✅ Xbox One Controller
- ✅ Xbox Series X/S Controller
- ✅ Logitech F310 (Xbox mode)
- ✅ Any FTC-compatible gamepad

**Documentation Status:**
- ✅ All controls documented
- ✅ Both controller types mapped
- ✅ Descriptions complete
- ✅ Edge detection noted
- ✅ Mode-specific behaviors listed
- ✅ Auto-turret system fully documented

---

**Last Updated:** January 27, 2026  
**Program:** Decode_Teleop.java  
**Gamepad Support:** Dual (PlayStation + Xbox)  
**Auto-Turret:** Enabled with Manual Override

