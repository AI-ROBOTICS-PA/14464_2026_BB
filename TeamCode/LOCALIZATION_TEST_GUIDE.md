# 🎯 Localization Test Program Guide

## Overview
The **LocalizationTest** program is a comprehensive TeleOp tool for testing Pedro Pathing localization. It provides real-time position tracking, heading information, and visual field representation.

## Features

### ✅ **1. Pedro Pathing Integration**
- Uses Pedro Pathing's `Follower` class for localization
- Automatic position tracking using odometry/IMU
- Continuous pose updates every loop iteration

### ✅ **2. Real-Time Localization**
- **X Position**: Horizontal position on field (inches)
- **Y Position**: Vertical position on field (inches)  
- **Heading/Angle**: Robot orientation (degrees and radians)
- Updates at full loop speed (~50Hz typical)

### ✅ **3. Accurate Position & Direction Display**
Shows:
- Exact coordinates: `(X, Y)` in inches
- Heading in degrees (0-360°)
- Compass direction (North, South, East, West, etc.)
- Directional arrow (↑ ↗ → ↘ ↓ ↙ ← ↖)

### ✅ **4. Field Location Information**
- **Quadrant Detection**: Which quarter of the field you're in (Q1-Q4)
- **Distance from Center**: How far from field center (inches)
- **Position Description**: Corner, edge, center, or interior
- **Visual ASCII Map**: Top-down view showing robot position and heading

## Controls

### 🎮 Gamepad 1 Controls

| Input | Function |
|-------|----------|
| **Left Stick Y** | Forward/Backward |
| **Left Stick X** | Strafe Left/Right |
| **Right Stick X** | Rotate (turn) |
| **DPAD UP** | Reset position to origin (0, 0, 0°) |
| **DPAD DOWN** | Set position to field center (72, 72) |
| **DPAD LEFT** | Set to Blue Alliance start (0, 0, 0°) |
| **DPAD RIGHT** | Set to Red Alliance start (144, 0, 180°) |
| **X / Square** | Toggle Field-Centric ⟷ Robot-Centric drive |

## Display Information

### 📍 Position Section
```
X Position: 45.32 inches
Y Position: 78.91 inches
Coordinates: (45.3, 78.9)
```

### 🧭 Heading/Orientation Section
```
Heading: 135.2°
Radians: 2.359 rad
Direction: SOUTHEAST (135°)
Arrow: ↘
```

### 🗺️ Field Location Section
```
Quadrant: Q1 (Top-Right)
Distance from Center: 34.5 inches
Field Position: FIELD INTERIOR
```

### 📊 Field Map (ASCII)
```
+-------------------------+
|                         |
|                         |
|            ⊕            |
|         ↗               |
|                         |
|                         |
+-------------------------+
↗ = Robot  ⊕ = Center
```

### 🎮 Controls Section
```
Drive Mode: 🌍 FIELD-CENTRIC
Gamepad Input: LY:0.75 LX:-0.50 RX:0.00
```

## How to Use

### 1️⃣ **Initial Setup**
1. Deploy the program to your robot
2. Select "Localization Test" from TeleOp menu
3. Initialize the program
4. Press **PLAY** to start

### 2️⃣ **Testing Localization**
1. Drive the robot around the field
2. Watch the position values update in real-time
3. Verify coordinates match your physical position
4. Check heading matches robot orientation

### 3️⃣ **Position Reset/Calibration**
- Use **DPAD UP** to reset to origin when at starting position
- Use **DPAD LEFT/RIGHT** to set alliance-specific starts
- Use **DPAD DOWN** when at field center for calibration

### 4️⃣ **Verify Accuracy**
1. Place robot at known position (e.g., field corner)
2. Reset position using appropriate DPAD button
3. Drive to another known position
4. Compare displayed position with actual position
5. Check accuracy (should be within ±1-2 inches)

## Field Coordinate System

### 📐 Coordinate Reference
```
Y-Axis
  ↑
  |  (0,144)        (144,144)
  |     ┌──────────────┐
  |     │              │
  |     │              │
  |     │      ⊕       │ ← Center (72, 72)
  |     │   (72,72)    │
  |     │              │
  |     └──────────────┘
  |  (0,0)          (144,0)
  └────────────────────→ X-Axis
```

### 🧭 Heading Reference
- **0°** = North (↑) - Robot facing forward
- **90°** = East (→) - Robot facing right
- **180°** = South (↓) - Robot facing backward
- **270°** = West (←) - Robot facing left

## Quadrants Explained

| Quadrant | Position | X Range | Y Range |
|----------|----------|---------|---------|
| **Q1** | Top-Right | 72-144 | 72-144 |
| **Q2** | Top-Left | 0-72 | 72-144 |
| **Q3** | Bottom-Left | 0-72 | 0-72 |
| **Q4** | Bottom-Right | 72-144 | 0-72 |

## Drive Modes

### 🌍 Field-Centric (Default)
- Forward always moves toward field's forward direction
- Left always moves toward field's left side
- Easier for drivers - consistent with field orientation
- **Best for**: Match play, precise positioning

### 🤖 Robot-Centric
- Forward moves in robot's current facing direction
- Left moves to robot's left (relative to robot)
- Traditional tank/arcade drive feel
- **Best for**: Recovery, tight spaces, experienced drivers

## Troubleshooting

### ❌ Position Not Updating
- Check that Pedro Pathing is properly configured
- Verify odometry wheels/encoders are connected
- Ensure IMU is initialized correctly
- Check `Constants.java` for proper setup

### ❌ Position Drifting
- Recalibrate IMU (power cycle robot)
- Check for slipping odometry wheels
- Verify encoder directions are correct
- Tune Pedro Pathing parameters

### ❌ Heading Incorrect
- Check IMU mounting orientation
- Verify IMU axis configuration in Constants
- Ensure heading wrapping is handled (0-360°)

### ❌ Field Map Not Showing Robot
- Robot may be outside field bounds (0-144 inches)
- Use position reset to bring back to valid range

## Tips for Accurate Localization

1. **Start at Known Position**: Always initialize at a known starting point
2. **Regular Recalibration**: Reset position at known landmarks during testing
3. **Slow Movements**: Test with slow, deliberate movements first
4. **Check Symmetry**: Drive in squares/circles to verify accuracy
5. **Mark Field**: Use tape to mark test positions for verification

## Next Steps

After verifying localization works correctly:
1. ✅ Use in autonomous programs for path following
2. ✅ Integrate with AprilTag detection for absolute positioning
3. ✅ Add to main TeleOp for driver assistance
4. ✅ Use for field-relative auto-alignment features

## Related Files
- `Constants.java` - Pedro Pathing configuration
- `Decode_Teleop.java` - Main TeleOp with localization
- Auto programs using Pedro Pathing

---

**Created**: January 2026  
**For**: FTC Team 14464 - DECODE Season  
**Purpose**: Localization testing and verification

