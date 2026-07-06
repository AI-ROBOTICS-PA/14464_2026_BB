# Gamepad Control Mapping Table

## Gamepad 1 (Driver)

| Button | Control Name | Function |
|--------|--------------|----------|
| **Left Stick ↑** | Forward Movement | Drive robot forward |
| **Left Stick ↓** | Backward Movement | Drive robot backward |
| **Left Stick ←** | Strafe Left | Move robot left |
| **Left Stick →** | Strafe Right | Move robot right |
| **Right Stick ←** | Rotate Counter-Clockwise | Rotate robot left |
| **Right Stick →** | Rotate Clockwise | Rotate robot right |
| **A / Cross** | Mode Toggle | Switch INTAKE ↔ LAUNCH |
| **B / Circle** | Manual Ball Advance | Add ball manually (if sensor broken) |
| **X / Square** | Manual Retreat | Retreat intake (undo previous ball) |
| **Y / Triangle** | Hard Turret Reset | Reset turret encoder to center |
| **D-Pad ↑** | Hood Long Range | Set hood to LONG position (0.30) |
| **D-Pad ↓** | Hood Short Range | Set hood to SHORT position (0.80) |
| **D-Pad ←** | Turret Previous Slot | Move turret to previous position |
| **D-Pad →** | Turret Next Slot | Move turret to next position |
| **Left Bumper** | N/A | Not used |
| **Right Bumper** | Auto-Shoot Start | Start automatic shooting sequence |
| **Left Trigger** | Velocity Toggle | Switch HIGH (1450) ↔ LOW (1100) |
| **Right Trigger** | Manual Ball Push | Squeeze to push ball (manual shoot) |

---

## Gamepad 2 (Operator)

| Button | Control Name | Function |
|--------|--------------|----------|
| **Left Stick ↑** | Intake Forward | Run intake motor forward |
| **Left Stick ↓** | Intake Reverse | Run intake motor in reverse (outtake) |
| **Left Stick ←** | N/A | Not used |
| **Left Stick →** | N/A | Not used |
| **Right Stick ←** | Turret Rotate CCW | Rotate turret counter-clockwise (variable) |
| **Right Stick →** | Turret Rotate CW | Rotate turret clockwise (variable) |
| **Right Stick ↑** | N/A | Not used |
| **Right Stick ↓** | N/A | Not used |
| **A / Cross** | Alliance Toggle / Full Reset | Before START: Toggle alliance | During match: Full Limelight reset |
| **B / Circle** | N/A | Not used |
| **X / Square** | Toggle Auto-Turret | Enable/Disable automatic goal tracking |
| **Y / Triangle** | Toggle Auto-Calibration | Enable/Disable automatic Limelight calibration |
| **D-Pad ↑** | Hood Fine Up | Increase hood angle (+0.02) |
| **D-Pad ↓** | Hood Fine Down | Decrease hood angle (-0.02) |
| **D-Pad ←** | Turret Fine CCW | Fine turret rotation counter-clockwise |
| **D-Pad →** | Turret Fine CW | Fine turret rotation clockwise |
| **Left Bumper** | Turret Recalibrate | Lock current position as reference |
| **Right Bumper** | Limelight Calibrate | Calibrate using AprilTag |
| **Left Trigger (Hold)** | Position Lock | Lock robot position (hold to maintain) |
| **Right Trigger** | N/A | Not used |
| **Back / Share** | Next Telemetry Page | Cycle through telemetry displays |
| **Start / Options + Back/Share** | Emergency Stop | Stop all motors (hold both) |

---

## Combined Summary - Most Used Controls

### Gamepad 1 (Driver)
| Priority | Button | Action |
|----------|--------|--------|
| **HIGH** | A/Cross | Toggle Mode |
| **HIGH** | Left Stick | Drive movement |
| **HIGH** | Right Stick X | Robot rotation |
| **HIGH** | Right Trigger | Manual push ball |
| **HIGH** | Right Bumper | Auto-shoot |
| **MEDIUM** | Left Trigger | Toggle velocity |
| **MEDIUM** | D-Pad ↑/↓ | Hood preset |
| **MEDIUM** | D-Pad ←/→ | Turret slot control |
| **LOW** | Y/Triangle | Hard turret reset |

### Gamepad 2 (Operator)
| Priority | Button | Action |
|----------|--------|--------|
| **HIGH** | Left Stick Y | Intake control |
| **HIGH** | Right Stick X | Turret rotation |
| **HIGH** | D-Pad ←/→ | Turret fine control |
| **MEDIUM** | X/Square | Toggle auto-turret |
| **MEDIUM** | Left Bumper | Recalibrate turret |
| **MEDIUM** | D-Pad ↑/↓ | Hood fine control |
| **LOW** | A/Cross | Alliance toggle |
| **LOW** | Left Trigger | Position lock |
| **EMERGENCY** | Back+Start | Emergency stop |

---

## Control Categories

### 🚗 DRIVING CONTROLS (GP1)
- Left Stick: Forward/Backward/Strafe
- Right Stick X: Rotation

### 🔵 MODE CONTROLS (GP1)
- A/Cross: Toggle INTAKE ↔ LAUNCH

### 🎬 INTAKE CONTROLS (GP2)
- Left Stick Y: Intake motor power
- B/Circle (GP1): Manual ball advance
- X/Square (GP1): Manual ball retreat

### 💥 SHOOTER CONTROLS (GP1)
- Left Trigger: Velocity toggle
- Right Trigger: Manual push
- Right Bumper: Auto-shoot

### 🎯 TURRET CONTROLS (GP2)
- Right Stick X: Variable speed rotation
- D-Pad ←/→: Fine control rotation
- Left Bumper: Recalibrate position
- Right Bumper: Limelight calibrate
- X/Square: Toggle auto-tracking

### 🎪 HOOD CONTROLS (GP1 & GP2)
- GP1 D-Pad ↑/↓: Preset positions
- GP2 D-Pad ↑/↓: Fine tuning

### 🚨 SPECIAL CONTROLS
- Y/Triangle (GP1): Hard turret reset
- Back+Start (Both): Emergency stop
- Back/Share (GP2): Telemetry page switch

---

## Notes
- **N/A** = Button not used in this program
- **Hold** = Keep button pressed continuously
- **Combo** = Press multiple buttons together (Back+Start)
- **Toggle** = Press once to switch modes
- **Variable** = Effect increases with stick movement amount

