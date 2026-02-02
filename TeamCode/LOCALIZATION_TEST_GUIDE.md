# 🎯 Localization Test Program Guide

## Overview
The **LocalizationTest** program is a comprehensive TeleOp tool for testing Pedro Pathing localization using the official Pedro Pathing coordinate system. It provides real-time position tracking, heading information, visual field representation, and **auto-turret functionality** that always points toward a target position.

## Pedro Pathing Coordinate System

This program follows the **official Pedro Pathing coordinate system** as documented at https://pedropathing.com/docs/pathing/reference/coordinates

### 📐 Coordinate Reference
```
Y-Axis (Vertical)
  ↑
144" ┌──────────────┐ (144,144)
  |  │  TOP-LEFT   │  TOP-RIGHT    │
  |  │      Q2     │     Q1        │
  |  │             │               │
72" │  ────────⊕───────────       │ ← Center (72, 72)
  |  │      Q3     │     Q4        │
  |  │             │               │
  |  │ BOTTOM-LEFT │ BOTTOM-RIGHT  │
0"  └──────────────┘ (144,0)
    0"      72"     144"
    └────────────────────→ X-Axis (Horizontal)
  (0,0) = ORIGIN
  Bottom-Left Corner
```

### 🧭 Heading Reference (Counter-Clockwise)
```
        0° (UP ↑)
           |
           |
90° (LEFT ←)──⊕──→ 270° (RIGHT)
           |
           |
      180° (DOWN ↓)
```

- **0°** = UP ↑ (Positive Y direction)
- **90°** = LEFT ← (Negative X direction)
- **180°** = DOWN ↓ (Negative Y direction)
- **270°** = RIGHT → (Positive X direction)
- **Rotation**: Counter-clockwise positive

### 🎯 Key Points
- **Origin (0, 0)**: Bottom-left corner of the field
- **X-axis**: Horizontal, 0 to 144 inches (left to right)
- **Y-axis**: Vertical, 0 to 144 inches (bottom to top)
- **Field Center**: (72, 72)
- **Heading 0°**: Points UP (toward positive Y)
- **Angles**: Increase counter-clockwise

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

### ✅ **5. Auto-Turret Functionality**
- **Automatic Targeting**: Turret automatically points toward position (1, 144) - top-left corner
- **Field-Aware**: Calculates angle based on robot position AND heading
- **Real-Time Adjustment**: Continuously updates as robot moves
- **Toggle Control**: Enable/disable with Circle/B button
- **Live Telemetry**: Shows target distance, angles, and turret status

## Controls

### 🎮 Gamepad 1 Controls

| Input | Function |
|-------|----------|
| **Left Stick Y** | Forward/Backward |
| **Left Stick X** | Strafe Left/Right |
| **Right Stick X** | Rotate (turn) |
| **DPAD UP** | Reset position to origin (0, 0, 0°) - Bottom-left corner |
| **DPAD DOWN** | Set position to field center (72, 72, 0°) |
| **DPAD LEFT** | Set to Blue Alliance position (0, 72, 0°) - Left side middle |
| **DPAD RIGHT** | Set to Red Alliance position (144, 72, 0°) - Right side middle |
| **X / Square** | Toggle Field-Centric ⟷ Robot-Centric drive |
| **Circle / B** | Toggle Auto-Turret ON/OFF |

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
Direction: DOWN-LEFT ↙ (135°)
Arrow: ↙
```

### 🎯 Auto-Turret Section
```
Status: 🎯 ACTIVE
Target Position: (1, 144)
Distance to Target: 98.3 inches
Field Angle to Target: 42.5°
Turret Angle (Relative): 15.3°
Turret Direction: ← Turn LEFT 15.3°
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

## Field Coordinate System - Official Pedro Pathing

For complete coordinate system documentation, see:
**https://pedropathing.com/docs/pathing/reference/coordinates**

### Summary:
- **Origin**: Bottom-left corner (0, 0)
- **X-axis**: Horizontal, left to right (0 to 144 inches)
- **Y-axis**: Vertical, bottom to top (0 to 144 inches)  
- **Heading 0°**: Points UP (positive Y direction)
- **Rotation**: Counter-clockwise positive

### Important Notes:
1. All Pedro Pathing paths and autonomous routines use this coordinate system
2. When setting waypoints, remember (0,0) is bottom-left, not top-left
3. Heading 0° points UP, not right - this is different from traditional robotics coordinates
4. Always verify your robot's starting position matches your code's starting pose

## Quadrants Explained

Based on Pedro Pathing coordinate system with origin at bottom-left:

| Quadrant | Position | X Range | Y Range | Description |
|----------|----------|---------|---------|-------------|
| **Q1** | Top-Right | 72-144 | 72-144 | Upper right portion |
| **Q2** | Top-Left | 0-72 | 72-144 | Upper left portion |
| **Q3** | Bottom-Left | 0-72 | 0-72 | Lower left (contains origin) |
| **Q4** | Bottom-Right | 72-144 | 0-72 | Lower right portion |

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

## Auto-Turret Feature

### 🎯 How It Works

The auto-turret uses **field-aware localization** to automatically point the turret toward a fixed target position on the field **(1, 144)** - which is the top-left corner, 1 inch from the left edge.

### 📐 Calculation Method

```
Example: Robot at (72, 36) facing UP (0°)
Target at (1, 144)

Y
↑
144 ●────────────────── Target (1, 144)
    │╲
    │ ╲ Field angle to target
    │  ╲ atan2(dx, dy) ≈ 88°
    │   ╲
 72 │    ╲
    │     ╲
 36 │      🤖 Robot (72, 36)
    │        Heading: 0° (UP ↑)
    │        Turret angle = 88° - 0° = 88°
  0 └────────────────────→ X
    0      72        144

Result: Turret rotates 88° LEFT to point at target
```

1. **Calculate Field Angle**: Uses `atan2(dx, dy)` to find the absolute angle from robot to target
   - `dx = TARGET_X - robot_x = 1 - 72 = -71`
   - `dy = TARGET_Y - robot_y = 144 - 36 = 108`
   - `field_angle = atan2(-71, 108) ≈ 88°`
   - Note: Uses `atan2(dx, dy)` (not `dy, dx`) because Pedro Pathing's 0° = UP

2. **Account for Robot Heading**: Subtracts robot's heading from field angle
   - `turret_angle = field_angle_to_target - robot_heading`
   - `turret_angle = 88° - 0° = 88°`
   - This gives the angle the turret needs to rotate relative to robot

3. **Normalize to Shortest Path**: Converts to -180° to 180° range
   - Ensures turret takes shortest rotation path
   - Example: 270° becomes -90° (turn right instead of left)

4. **Proportional Control**: Applies simple P-controller
   - `turret_power = turret_angle × kp`
   - Default kp = 0.015 (tunable)
   - Clamped to ±0.5 max power

### 🎮 Controls
- **Circle / B Button**: Toggle auto-turret ON/OFF
- **Default**: Auto-turret is ENABLED on startup

### 📊 Telemetry Information

When auto-turret is active, you'll see:
- **Status**: 🎯 ACTIVE or ⭕ DISABLED
- **Target Position**: Shows (1, 144)
- **Distance to Target**: How far the target is from robot (inches)
- **Field Angle to Target**: Absolute angle on field (0-360°)
- **Turret Angle (Relative)**: How much turret needs to rotate (-180° to 180°)
- **Turret Direction**: Visual indicator showing rotation direction
  - `✓ ON TARGET` - Within 2° of target
  - `← Turn LEFT X°` - Needs to rotate counter-clockwise
  - `→ Turn RIGHT X°` - Needs to rotate clockwise

### 🔧 How to Test

1. **Enable auto-turret** (default ON, or press Circle/B)
2. **Drive around the field** - watch turret automatically track target
3. **Check telemetry** - verify angles make sense:
   - When at (72, 0) facing UP: Field angle ≈ 88°, turret should point slightly left
   - When at (72, 72) facing UP: Field angle ≈ 90°, turret should point left
   - When at (144, 144) facing UP: Field angle ≈ 180°, turret should point down
4. **Verify physical motion** - turret should actually rotate to point at (1, 144)

### 🎯 Why (1, 144)?

This is the **top-left corner** of the field - a common scoring position in FTC games. You can modify `TARGET_X` and `TARGET_Y` in the code to point at any field position (e.g., goal, scoring zone, pickup location).

### ⚙️ Tuning Parameters

If turret is too slow/fast or oscillates, adjust these in code:
- **kp** (0.015): Increase for faster response, decrease if oscillating
- **maxPower** (0.5): Maximum turret motor power
- **deadband** (2.0°): Angle tolerance before stopping (prevents jitter)

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

