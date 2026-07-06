# 🎮 Gamepad Control Mapping - DecodeTeleOp_Manual

## Overview
This document describes the gamepad controls for the **DecodeTeleOp_Manual** program.

**Current Configuration:** Single Gamepad (Simplified)
**Supported Controllers:** Xbox & PlayStation 4

---

## Gamepad Layout Reference

```
                    Y / △
                    |
        X / □ --- [Center] --- B / ○
                    |
                    A / ×

        LB          LT              RT          RB
        |           |               |           |
    [L Stick]   [R Stick]
        |           |
```

---

## Control Mapping

### 🚗 **MOVEMENT & DRIVING**
| Button | Action | Details |
|--------|--------|---------|
| **Left Stick Y** | Move Forward/Backward | Push up = forward, down = backward |
| **Left Stick X** | Strafe Left/Right | Push left = strafe left, right = strafe right |
| **Right Stick X** | Robot Rotation | Push left = CCW, right = CW |

### 🔵 **MODE SELECTION**
| Button | Action | Details |
|--------|--------|---------|
| **A / Cross** | Toggle Mode | INTAKE ↔ LAUNCH |
| | **INTAKE Mode** | Intake motor runs, color sensor auto-advances indexer |
| | **LAUNCH Mode** | Ready to shoot, manual ball push |

### 🎬 **INTAKE SYSTEM**
| Button | Action | Details |
|--------|--------|---------|
| **A / Cross** | Enable INTAKE Mode | Switches from LAUNCH to INTAKE |
| | Auto-Collect | Color sensor automatically advances 1→2→3 balls |
| | No manual control needed | Just hold intake and let it work |

### 💥 **SHOOTER SYSTEM**
| Button | Action | Details |
|--------|--------|---------|
| **Left Trigger** | Toggle Velocity | Switches between HIGH (1450) ↔ LOW (1100) |
| | Press once | Switch to opposite velocity |
| | Shooter always running | Motor spins continuously at selected speed |
| **Right Trigger** | Manual Ball Push | Squeeze to push ball into shooter |
| | Squeeze to ~70% | Servo moves forward, pushes ball |
| | Release below 30% | Servo returns to rest, counts as 1 shot |

### 🎯 **TURRET SYSTEM**
| Button | Action | Notes |
|--------|--------|-------|
| **NOT AVAILABLE** | No turret control | Turret motor removed for single-gamepad mode |
| **Y / Triangle** | Hard Turret Reset | Physical center turret, then press to reset encoder |

### 🎪 **HOOD SYSTEM**
| Button | Action | Notes |
|--------|--------|-------|
| **NOT AVAILABLE** | No hood control | Hood locked at default position |
| | Hood Position | Fixed at MEDIUM (0.55) by default |

### 🚨 **EMERGENCY CONTROLS**
| Button | Action | Details |
|--------|--------|---------|
| **Back + Start** | Emergency Stop | Stops ALL motors immediately (hold both) |
| | Both controllers work | Either gamepad can trigger |

---

## Complete Shooting Workflow

### **COLLECT PHASE** (INTAKE Mode)
```
1. Press A / Cross
   ↓
2. Intake motor starts automatically
   ↓
3. Drive to ball area
   ↓
4. Position intake over balls
   ↓
5. Color sensor detects:
   - Ball 1 → Indexer at Position 1
   - Ball 2 → Indexer moves to Position 2
   - Ball 3 → Indexer moves to Position 3
   ↓
6. Press A / Cross to switch to LAUNCH mode
```

### **SHOOT PHASE** (LAUNCH Mode)
```
1. Press A / Cross
   ↓
2. Aim robot at goal
   ↓
3. (Optional) Press Left Trigger to change velocity
   - Default: LOW (1100) for short-range
   - HIGH (1450) for long-range
   ↓
4. SQUEEZE Right Trigger ~70%
   - Ball Push Servo moves forward
   - Ball enters shooter
   - Gets launched at goal 🎯
   ↓
5. RELEASE Right Trigger
   - Servo returns to rest position
   - Feel rumble confirmation (100ms vibration)
   ↓
6. Repeat for remaining balls
   (Ball 2 & 3 automatically align in current implementation)
```

---

## Button Press Timing

### **For Velocity Toggle (Left Trigger)**
```
Press once → Switches velocity
Wait for response → Motor spins at new speed
Press again → Switches back
```

### **For Ball Push (Right Trigger)**
```
Squeeze to ~70% → Servo pushes ball (FAST action)
        ↓
Ball launches
        ↓
Release below 30% → Servo resets
        ↓
Wait ~200ms → Ready for next ball
Squeeze again → Push next ball
```

### **For Mode Toggle (A/Cross)**
```
Press once → Mode switches (INTAKE → LAUNCH or vice versa)
Wait for LED color change → Confirms mode switched
```

---

## Visual Gamepad Map (Simplified Single-Controller)

```
              ▲ Y/△
              │ (Not Used)
    X/□ ◄─────┼─────► B/○
              │       (Emergency Stop: Back+Start)
              ▼ A/×
           TOGGLE MODE
           INTAKE ↔ LAUNCH


    [L Trigger]                [R Trigger]
    Toggle Velocity            Manual Ball Push
    HIGH ↔ LOW                 Squeeze/Release = Shoot


    [L Stick]                  [R Stick]
    Drive Control              Rotation
    - Forward/Back             - Left = CCW
    - Left/Right               - Right = CW


    [L Bumper]                 [R Bumper]
    (Not Used)                 (Not Used)
```

---

## Telemetry Display

The program shows real-time status on telemetry:

### **Page 1: ESSENTIAL (default)**
- Current Mode (INTAKE/LAUNCH)
- Turret Angle
- Shooter Velocity & Status
- Hood Position
- Balls Loaded
- Ball Color Detection
- Auto-Advance Status

### **Page 2: CONTROLS**
- Quick button reference
- All available controls
- Emergency stop info

**Toggle pages:** Use GP2 Back button (if two controllers used)

---

## Control Summary Table

| Function | Single Gamepad | Purpose |
|----------|---|---------|
| Move Forward | Left Stick ▲ | Navigate field |
| Move Backward | Left Stick ▼ | Navigate field |
| Strafe Left | Left Stick ◄ | Navigate field |
| Strafe Right | Left Stick ► | Navigate field |
| Rotate CCW | Right Stick ◄ | Aim robot |
| Rotate CW | Right Stick ► | Aim robot |
| INTAKE Mode | A / Cross | Start collecting balls |
| LAUNCH Mode | A / Cross | Start shooting |
| HIGH Velocity | Left Trigger | Long-range power shot |
| LOW Velocity | Left Trigger | Short-range controlled shot |
| Push Ball | Right Trigger | Shoot current ball |
| Emergency Stop | Back + Start | Stop everything |

---

## Tips & Tricks

✅ **Collection is automatic** - Just switch to INTAKE and drive
✅ **Shooter always runs** - Toggle velocity before/during shooting
✅ **Color sensor is smart** - No need to count balls manually
✅ **One button mode toggle** - A/Cross switches everything
✅ **Rumble feedback** - Feel the confirmation when ball is pushed
✅ **Position lock available** - (Requires second controller)

---

## Troubleshooting

| Issue | Solution |
|-------|----------|
| Intake not starting | Press A/Cross to enter INTAKE mode first |
| Shooter not spinning | Already running - check voltage/battery |
| Ball not shooting | Squeeze Right Trigger past 70%, don't tap it |
| Ball detection not working | Check color sensor connection & cleanliness |
| Indexer not advancing | Check servo connection & power supply |

---

## Notes

- **Hood Position:** Fixed at default (0.55) - no in-match adjustment
- **Turret Control:** Removed for single-gamepad simplicity
- **Ball Indexing:** Color sensor auto-advances during collection
- **Shooter:** Continuously running, no OFF button
- **Emergency:** Always available with Back+Start combo

---

**Created:** July 5, 2026
**Program:** DecodeTeleOp_Manual.java
**Version:** Single Gamepad (Simplified)

