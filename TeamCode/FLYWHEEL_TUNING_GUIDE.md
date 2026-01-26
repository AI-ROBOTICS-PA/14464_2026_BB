# Flywheel Tuner - PIDF Tuning Guide

## Overview
The `FlywheelTuner` TeleOp program allows you to tune your flywheel's PIDF (actually PF) controller for optimal performance. This will help your shooter reach target velocity faster and maintain it more accurately.

## What is PIDF?
- **P (Proportional)**: Controls how aggressively the motor corrects errors. Higher P = faster response, but too high causes oscillation/overshoot.
- **I (Integral)**: Accumulates error over time (set to 0 for flywheels).
- **D (Derivative)**: Dampens oscillations (set to 0 for flywheels).
- **F (Feed Forward)**: Baseline power prediction to maintain velocity. This is your starting point.

## Controls
- **Y Button**: Toggle between high velocity (1500) and low velocity (900)
- **B Button**: Cycle through step sizes (10.0 → 1.0 → 0.1 → 0.01 → 0.001)
- **D-Pad Up/Down**: Increase/Decrease P value
- **D-Pad Right/Left**: Increase/Decrease F value

## Tuning Process

### Step 1: Tune F (Feed Forward) First
1. Start the FlywheelTuner OpMode
2. Set step size to 1.0 (press B until you see Step Size: 1.0)
3. Use D-Pad Right to increase F value until the flywheel spins
4. Keep increasing F until Current Velocity gets close to Target Velocity
5. Watch the Error value - you want it close to 0

**Fine-tuning F:**
- Switch to smaller step sizes (0.1, then 0.01) using B button
- Adjust F until Error is consistently near 0 (±20-40 ticks is acceptable)
- Test at your primary shooting velocity (probably 1500)
- If Error oscillates between positive and negative, you're very close!

**Tip**: F value usually ranges from 12-16 for typical FTC flywheel motors.

### Step 2: Tune P (Proportional)
Once F is tuned and maintaining steady velocity:

1. Press Y to toggle between velocities (900 ↔ 1500)
2. Notice how long it takes to reach the new target - this is what P improves
3. Set step size to 1.0 or 10.0 for initial P tuning
4. Use D-Pad Up to increase P value
5. Toggle velocity with Y and watch how quickly it responds

**What to look for:**
- **Too Low P**: Slow to reach target velocity (takes 2-3+ seconds)
- **Good P**: Reaches target quickly (under 1 second) without overshooting
- **Too High P**: Overshoots target (e.g., target is 1500 but briefly hits 1600+), or oscillates up and down

**Fine-tuning P:**
- Once you see slight overshooting, reduce P in smaller steps (0.1, 0.01)
- Find the highest P value that doesn't overshoot
- Test by rapidly toggling Y button - velocity should change quickly and smoothly

**Tip**: P values typically range from 50-300 depending on your motor and flywheel mass.

### Step 3: Record Your Values
Once tuned, **write down your P and F values!** You'll need them for your main TeleOp.

Example final values might look like:
- F = 14.98
- P = 265.0

## Applying Tuned Values to Your Main TeleOp

After tuning, update your `Decode_Teleop.java` or `RobotMotors.java`:

### Option 1: In RobotMotors.init()
```java
// In RobotMotors.java init() method, after setting up the shooter:
PIDFCoefficients pidfCoefficients = new PIDFCoefficients(
    265.0,  // P value from tuning
    0,      // I (keep at 0)
    0,      // D (keep at 0)
    14.98   // F value from tuning
);
shooter.setPIDFCoefficients(DcMotor.RunMode.RUN_USING_ENCODER, pidfCoefficients);
```

### Option 2: Create a constant
```java
// At top of RobotMotors class
public static final PIDFCoefficients SHOOTER_PIDF = new PIDFCoefficients(265.0, 0, 0, 14.98);

// In init()
shooter.setPIDFCoefficients(DcMotor.RunMode.RUN_USING_ENCODER, SHOOTER_PIDF);
```

## Testing Your Tuned Flywheel
After applying values:
1. Run your normal TeleOp
2. Activate the shooter
3. It should spin up to target velocity within ~0.5-1 second
4. After shooting a ball, velocity should recover quickly
5. Error should stay within ±50 ticks/sec

## Troubleshooting

**Problem**: Flywheel won't spin at all
- **Solution**: Increase F value more. Start with F around 10-15.

**Problem**: Velocity overshoots then settles
- **Solution**: Reduce P value in small steps.

**Problem**: Takes forever to reach target velocity
- **Solution**: Increase P value.

**Problem**: Velocity oscillates up and down rapidly
- **Solution**: Reduce P value significantly.

**Problem**: Different velocities need different tuning
- **Solution**: This is normal. Tune for your most common shooting velocity. You may need different PIDF sets for different ranges if precision is critical.

## Advanced Tips

1. **Tune at operating velocity**: If you mostly shoot at 1500 ticks/sec, tune F at that speed.

2. **Test with actual game actions**: After basic tuning, test by actually shooting balls. The load changes velocity behavior.

3. **Battery voltage matters**: A fully charged battery vs. low battery affects motor power. Tune with a moderately charged battery.

4. **Motor direction**: Make sure your motor direction is set correctly in the tuner (line 45). If the wheel spins backward, change `FORWARD` to `REVERSE`.

5. **Re-tune if you change**:
   - Flywheel weight/size
   - Motor type
   - Gear ratio
   - Battery (significantly)

## Expected Results
- **Before tuning**: 2-3 seconds to reach target velocity, inconsistent shooting
- **After tuning**: 0.5-1 second to reach target, consistent velocity, faster cycle time

Good luck with your tuning! 🤖🎯

