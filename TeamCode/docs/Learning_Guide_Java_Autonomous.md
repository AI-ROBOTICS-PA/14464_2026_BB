# Learning Java Through FTC Robotics: How Our Autonomous System Works

## Who Is This For?

This guide is for students learning Java who want to understand how our FTC robot's autonomous code works. We'll walk through real code from our robot and explain the Java concepts behind it. You don't need to know everything about Java to follow along — we'll explain each concept as we go.

---

## 1. The Big Picture: What Does Autonomous Do?

In FTC, the robot runs by itself for 30 seconds. Our robot needs to:
1. Drive to a position
2. Shoot 3 balls
3. Drive to pick up more balls
4. Come back and shoot again
5. Park

All of this happens with **zero human input**. The code controls everything.

---

## 2. Java Concept: Classes and Objects

In Java, a **class** is like a blueprint, and an **object** is a thing built from that blueprint.

Our robot has many systems, each represented by a class:

```java
public class RobotSystem {
    public LocalizationSystem localization;   // Knows where the robot is
    public AutoTurretSystem autoTurret;       // Aims the turret
    public AutoIntakeSystem autoIntake;       // Picks up balls
    public ShootingSystem shooting;           // Spins the flywheel
    public AutoShootSystem autoShoot;         // Fires balls one by one
    public LimelightManager limelight;        // Camera for vision
}
```

When we write `robot.autoTurret.holdCurrentPosition()`, we are:
- Accessing the `robot` object
- Going to its `autoTurret` field (another object)
- Calling the `holdCurrentPosition()` method on it

**Key takeaway:** Breaking a big problem (robot control) into small classes (turret, intake, shooting) makes the code easier to write, test, and fix.

---

## 3. Java Concept: Enums (Enumerated Types)

An **enum** is a special type that can only be one of a fixed set of values. We use enums to define the "states" our robot can be in:

```java
private enum AutoState {
    INIT,
    DRIVE_TO_LAUNCH_0,
    WAIT_ARRIVE_LAUNCH_0,
    WAIT_SHOOTER_READY,
    SHOOT_SEQUENCE_1,
    WAIT_SHOOT_COMPLETE_1,
    DRIVE_TO_INTAKE_1,
    WAIT_ARRIVE_INTAKE_1,
    INTAKE_FORWARD_1,
    DRIVE_TO_PARK,
    DONE
}
```

Think of it like a checklist — the robot can only be doing ONE thing at a time, and these are all the possible things it can do.

We track the current state with a variable:

```java
private AutoState currentState = AutoState.INIT;
```

---

## 4. Java Concept: Switch Statements (The State Machine)

A **switch statement** checks a value and runs different code depending on what it is. This is the heart of our autonomous:

```java
switch (currentState) {
    case INIT:
        // Set up the turret
        robot.autoTurret.setInitialOffset(INITIAL_TURRET_ANGLE);
        robot.autoTurret.holdCurrentPosition();
        transitionTo(AutoState.DRIVE_TO_LAUNCH_0);
        break;

    case DRIVE_TO_LAUNCH_0:
        // Start driving to the launch position
        follower.followPath(pathStartToLaunch);
        transitionTo(AutoState.WAIT_ARRIVE_LAUNCH_0);
        break;

    case WAIT_ARRIVE_LAUNCH_0:
        // Wait until we arrive (or timeout)
        if (!follower.isBusy() || elapsed > DRIVE_TIMEOUT) {
            transitionTo(AutoState.WAIT_SHOOTER_READY);
        }
        break;
}
```

**How it works:**
1. The `loop()` method runs ~50 times per second
2. Each time, the switch checks `currentState`
3. It runs the code for that state
4. When a condition is met (arrived, timeout, etc.), it transitions to the next state
5. The `break` keyword stops it from falling through to the next case

**This pattern is called a State Machine** — one of the most useful patterns in robotics programming.

---

## 5. Java Concept: Static Final Variables (Constants)

We use `static final` to define values that never change:

```java
private static final Pose START_POSE = new Pose(15, 120, Math.toRadians(180));
private static final Pose LAUNCH_POSE = new Pose(50, 84, Math.toRadians(180));
private static final double SHOOTER_VELOCITY = 1160.0;
private static final double HOOD_POSITION = 0.80;
```

- `static` means it belongs to the class, not to any particular object
- `final` means it cannot be changed after being set
- Together, they create a **constant** — a named value you can use everywhere

**Why use constants?** If you need to change the launch position, you change it in ONE place instead of hunting through hundreds of lines of code.

---

## 6. Java Concept: Methods (Functions)

A **method** is a reusable block of code with a name. Here's our state transition method:

```java
private void transitionTo(AutoState newState) {
    currentState = newState;
    actionTimer.resetTimer();
}
```

Every time we move to a new state, we call `transitionTo()`. It:
1. Updates the current state
2. Resets the timer (so we can track how long we've been in the new state)

**Why not just write these two lines everywhere?** Because if we later need to add logging or a pause feature, we change it in ONE place.

---

## 7. Java Concept: Constructors and Object Creation

When we create paths for the robot to follow, we use constructors:

```java
Path pathStartToLaunch = new Path(new BezierLine(START_POSE, LAUNCH_POSE));
pathStartToLaunch.setLinearHeadingInterpolation(
    START_POSE.getHeading(), 
    LAUNCH_POSE.getHeading()
);
```

- `new Path(...)` creates a new Path **object**
- `new BezierLine(START_POSE, LAUNCH_POSE)` creates a curved line between two points
- `.setLinearHeadingInterpolation(...)` tells the robot how to rotate while following the path

**Constructor** = the special method that runs when you create an object with `new`.

---

## 8. Java Concept: Boolean Logic and Conditions

We use boolean (`true`/`false`) expressions to make decisions:

```java
// Wait until the path is done OR we've run out of time
if (!follower.isBusy() || elapsed > DRIVE_TIMEOUT) {
    transitionTo(AutoState.WAIT_SHOOTER_READY);
}
```

Breaking this down:
- `follower.isBusy()` returns `true` if the robot is still driving
- `!` means "NOT" — so `!follower.isBusy()` means "the robot has arrived"
- `||` means "OR"
- `elapsed > DRIVE_TIMEOUT` means "we've waited too long"
- The whole condition: "if the robot arrived OR we timed out, move on"

**The timeout is a safety net** — without it, the robot could get stuck waiting forever if something goes wrong.

---

## 9. Java Concept: Inheritance and the OpMode Pattern

FTC uses an `OpMode` class that our programs extend:

```java
public class Auto_BlueNear extends OpMode {
    @Override
    public void init() {
        // Runs once when you press INIT
    }

    @Override
    public void init_loop() {
        // Runs repeatedly during init (for setup UI)
    }

    @Override
    public void start() {
        // Runs once when you press START
    }

    @Override
    public void loop() {
        // Runs repeatedly (~50 times/sec) during the match
    }

    @Override
    public void stop() {
        // Runs once when the match ends
    }
}
```

- `extends OpMode` means our class **inherits** from OpMode (gets all its features)
- `@Override` means we're replacing the parent class's version of that method with our own
- FTC's system calls these methods at the right time — we just fill in the code

---

## 10. Java Concept: Static Variables (Persisting Data Between Programs)

A unique challenge: we need to pass the turret's position from Autonomous to TeleOp. But they are **separate programs** — when Auto ends, its objects are destroyed.

Solution: **static variables** in `RobotMotors`:

```java
public class RobotMotors {
    // Static = shared across ALL instances, survives between OpModes
    private static double savedTurretOffsetTicks = 0;
    private static boolean hasSavedOffset = false;

    public static void saveTurretOffset(double offsetTicks, double initialDegrees) {
        savedTurretOffsetTicks = offsetTicks;
        hasSavedOffset = true;
    }

    public static boolean hasSavedTurretOffset() {
        return hasSavedOffset;
    }
}
```

- `static` variables belong to the **class itself**, not to any object
- They survive even when objects are destroyed
- Auto calls `saveTurretOffset()` when it stops
- TeleOp calls `hasSavedTurretOffset()` and `getSavedTurretOffsetTicks()` when it starts

---

## 11. Putting It All Together: The Loop

Here's what happens 50 times per second during autonomous:

```
loop() is called
  │
  ├── robot.updateAll()          ← Update ALL subsystems
  │     ├── localization.update()  ← "Where am I?"
  │     ├── limelight.update()     ← "What does the camera see?"
  │     ├── autoTurret.update()    ← "Am I pointing at the goal?"
  │     ├── autoHood.update()      ← "Is my hood at the right angle?"
  │     ├── autoIntake.update()    ← "Did I pick up a ball?"
  │     ├── shooting.update()      ← "Is my flywheel at speed?"
  │     └── autoShoot.update()     ← "Am I in the middle of shooting?"
  │
  ├── follower.update()          ← Update path following
  │
  └── switch (currentState)      ← Run state machine logic
        ├── INIT → set turret, transition
        ├── DRIVE_TO_LAUNCH → start path, transition
        ├── WAIT_ARRIVE → check if arrived
        ├── SHOOT_SEQUENCE → start autoShoot
        ├── WAIT_SHOOT → check if done
        └── ... and so on
```

Each iteration takes about **20 milliseconds**. The state machine only does a tiny bit of work each time — checking conditions and maybe starting an action. The actual work (driving, shooting) happens in the subsystems.

---

## 12. Coordinate Mirroring: One Program, Two Alliances

The FTC field is symmetric. Instead of writing separate logic for Blue and Red, we **mirror coordinates**:

```
Blue Near Start:  ( 15, 120)  heading 180°
Red Near Start:   (129, 120)  heading   0°

Formula: X_red = 144 - X_blue
         Heading_red = 0° (flipped from 180°)
```

The field is 144 inches wide. The Red side is a mirror image of Blue. All the logic (state machine, intake, shooting) stays identical — only the numbers change.

---

## Summary of Java Concepts Used

| Concept | Where We Use It |
|---------|----------------|
| **Classes & Objects** | Every subsystem (AutoTurretSystem, ShootingSystem, etc.) |
| **Enums** | AutoState, TestPhase — tracking what the robot is doing |
| **Switch Statements** | The state machine — the core of autonomous |
| **Static Final (Constants)** | Positions, velocities, timeouts |
| **Methods** | transitionTo(), buildPaths(), updateAll() |
| **Constructors** | Creating Path, Pose, BezierLine objects |
| **Boolean Logic** | Decision-making (arrived? timed out? ball detected?) |
| **Inheritance** | Extending OpMode for our autonomous programs |
| **Static Variables** | Passing turret offset from Auto to TeleOp |
| **Interfaces & Libraries** | Pedro Pathing, FTC SDK, Limelight |

---

## Want to Learn More?

1. **Read `Auto_BlueNear.java`** — the most complete autonomous with 3 shooting cycles
2. **Read `RobotSystem.java`** — see how all subsystems are wired together
3. **Read `AutoShootSystem.java`** — a clean example of a smaller state machine
4. **Try changing a constant** (like `PARK_POSE`) and see what happens
5. **Use phase selection** to test one phase at a time on the real robot

The best way to learn is to read the code, change something small, and see what happens!

---

*DECODE Team 14464 — FIRST Tech Challenge 2026*
