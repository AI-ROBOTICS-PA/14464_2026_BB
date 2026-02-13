# DECODE 14464 — Autonomous System Overview

## Team & Challenge

DECODE Team 14464 competes in the FIRST Tech Challenge (FTC), where robots must perform tasks both autonomously (30 seconds, no driver input) and under driver control (TeleOp). Our 2026 season robot features a ball-shooting mechanism with a rotating turret, an automated intake system, and a vision-guided aiming system — all coordinated by a modular software architecture written in Java.

## The Problem

During autonomous, the robot must:
1. Start from a known position on the field
2. Shoot three preloaded balls into a goal
3. Drive to a ball collection zone, pick up three more balls
4. Return to the shooting position and shoot again
5. Repeat the intake-and-shoot cycle (for "Near" autos)
6. Park in a designated zone — all within 30 seconds

The challenge is coordinating precise movement, ball detection, turret aiming, and a multi-ball shooting mechanism under strict time constraints, with no human input.

## Our Solution: A Modular, State-Machine Architecture

### System Design

We decomposed the robot into **nine independent subsystems**, each with a single responsibility:

| System | Role |
|--------|------|
| **LocalizationSystem** | Tracks the robot's position and heading on the field using GoBILDA Pinpoint odometry |
| **AutoTurretSystem** | Aims the turret toward the goal using PID control, with multiple tracking modes |
| **AutoHoodSystem** | Adjusts the hood angle based on distance to the goal |
| **AutoIntakeSystem** | Detects balls via a color sensor, advances the turret servo to load up to 3 balls |
| **AutoShootSystem** | Fires balls one-by-one through a push-retract-advance state machine |
| **ShootingSystem** | Controls flywheel velocity for consistent launch speed |
| **LimelightManager** | Processes AprilTag detections from a Limelight 3A camera for position correction |
| **LEDSystem** | Visual feedback — green for intake mode, red for launch mode |
| **RobotSystem** | Central orchestrator that initializes and updates all subsystems in the correct order |

Every loop cycle (~50 Hz), `RobotSystem.updateAll()` calls each subsystem's `update()` method in a carefully ordered sequence: localization first (so all other systems have a fresh position), then vision, turret, hood, intake, and shooting.

### System Architecture Diagram

```mermaid
graph TB
    subgraph Orchestrator
        RS["RobotSystem<br/>updateAll() every 20ms"]
    end

    subgraph Sensors & Input
        OD["Pinpoint Odometry<br/>(X, Y, Heading)"]
        LL["Limelight 3A<br/>(AprilTags)"]
        CS["Color Sensor<br/>(Ball Detect)"]
        TS["Touch Sensor<br/>(Retract Confirm)"]
    end

    subgraph Processing
        LOC["LocalizationSystem<br/>(Position Tracking)"]
        LM["LimelightManager<br/>(Vision Processing)"]
        AT["AutoTurretSystem<br/>(Goal Aiming / PID)"]
        AH["AutoHoodSystem<br/>(Distance-based Angle)"]
        AI["AutoIntakeSystem<br/>(Ball Count / Slots)"]
        AS["AutoShootSystem<br/>(Fire Sequence)"]
        SH["ShootingSystem<br/>(Flywheel Velocity)"]
    end

    subgraph Actuators & Output
        DM["Drive Motors<br/>(Pedro Pathing)"]
        TM["Turret Motor<br/>(PID Control)"]
        TSV["Turret Servo<br/>(Slot Position)"]
        PS["Push Servo<br/>(Ball Launch)"]
        HS["Hood Servo<br/>(Launch Angle)"]
        FM["Flywheel Motor<br/>(Velocity Control)"]
        LED["LEDs<br/>(Status Color)"]
    end

    RS --> LOC
    RS --> LM
    RS --> AT
    RS --> AH
    RS --> AI
    RS --> AS
    RS --> SH

    OD --> LOC
    LL --> LM
    CS --> AI
    TS --> AS

    LOC --> DM
    LOC --> AT
    LOC --> AH
    LM --> AT
    AT --> TM
    AH --> HS
    AI --> TSV
    AI --> AS
    AS --> PS
    SH --> FM
    AI --> LED
```

### Update Order (Every Loop Cycle)

```mermaid
graph TD
    START(["Loop Start (~50 Hz)"]) --> S1
    S1["1. localization.update()<br/>Read odometry, update X/Y/Heading"] --> S2
    S2["2. limelight.update()<br/>Read camera, cache AprilTag data"] --> S3
    S3["3. autoTurret.update()<br/>Calculate goal angle, PID motor"] --> S4
    S4["4. autoHood.update()<br/>Set hood based on goal distance"] --> S5
    S5["5. autoIntake.update()<br/>Check color sensor, advance slot"] --> S6
    S6["6. shooting.update()<br/>Maintain flywheel velocity"] --> S7
    S7["7. autoShoot.update()<br/>Push/retract/advance state machine"] --> S8
    S8["8. follower.update()<br/>Path following (Pedro Pathing)"] --> S9
    S9["9. State Machine switch<br/>Check conditions, transition"] --> END
    END(["Loop End (~20ms)"])
```

### Autonomous Programs

We developed **four autonomous programs** — one for each starting position on the field:

- **Blue Near** / **Red Near**: 3 shoot cycles + 2 intake cycles (9 balls total)
- **Blue Far** / **Red Far**: 2 shoot cycles + 1 intake cycle (6 balls total)

Red programs are coordinate-mirrored from Blue (X_red = 144 − X_blue, heading flipped 180°).

Each autonomous uses an **enum-based state machine** with 15–20 states. A `transitionTo()` method handles state changes with timer resets, and every state has a timeout to prevent the robot from getting stuck.

### Autonomous Flow — Blue Near (3-Cycle, 9 Balls)

```mermaid
flowchart TD
    START([START]) --> INIT["INIT<br/>Set turret offset (30°)<br/>Hold position via PID"]
    
    INIT --> DRIVE0["DRIVE TO LAUNCH<br/>(50, 84)"]
    DRIVE0 --> SHOOT1["SHOOT 3 BALLS<br/>Cycle 1: Preloaded"]

    SHOOT1 --> INTAKE1["DRIVE TO INTAKE 1<br/>(36, 84)"]
    INTAKE1 --> SWEEP1["FORWARD SWEEP<br/>Slow drive to X=10<br/>Intake motor ON<br/>Color sensor detects balls<br/>Stop at 3 balls"]
    SWEEP1 --> RETURN1["DYNAMIC RETURN<br/>TO LAUNCH<br/>(path from actual pose)"]
    RETURN1 --> SHOOT2["SHOOT 3 BALLS<br/>Cycle 2"]

    SHOOT2 --> INTAKE2["DRIVE TO INTAKE 2<br/>(36, 60)"]
    INTAKE2 --> SWEEP2["FORWARD SWEEP 2<br/>Intake motor ON<br/>Stop at 3 balls"]
    SWEEP2 --> RETURN2["DYNAMIC RETURN<br/>TO LAUNCH<br/>(path from actual pose)"]
    RETURN2 --> SHOOT3["SHOOT 3 BALLS<br/>Cycle 3"]

    SHOOT3 --> PARK["PARK<br/>(36, 84)"]
    PARK --> DONE([DONE — 9 balls in 30s])

    style SHOOT1 fill:#f96,stroke:#333
    style SHOOT2 fill:#f96,stroke:#333
    style SHOOT3 fill:#f96,stroke:#333
    style SWEEP1 fill:#6c6,stroke:#333
    style SWEEP2 fill:#6c6,stroke:#333
    style RETURN1 fill:#69f,stroke:#333
    style RETURN2 fill:#69f,stroke:#333
```

### AutoShoot State Machine (Per Ball)

```mermaid
flowchart LR
    POS["POSITION<br/>Turret Servo<br/>to Slot N"] --> PUSH["PUSH<br/>Servo Forward"]
    PUSH --> WAIT_PUSH["WAIT PUSH<br/>(300ms)"]
    WAIT_PUSH --> RETRACT["RETRACT<br/>Servo Back"]
    RETRACT --> WAIT_RET["WAIT RETRACT<br/>(touch sensor<br/>or 300ms)"]
    WAIT_RET --> ADV["ADVANCE<br/>to Next Slot"]
    ADV --> CHECK{Balls<br/>left?}
    CHECK -- YES --> POS
    CHECK -- NO --> DONE([COMPLETE])

    style PUSH fill:#f96,stroke:#333
    style RETRACT fill:#69f,stroke:#333
    style DONE fill:#6c6,stroke:#333
```

### Auto-to-TeleOp Handoff

```mermaid
flowchart LR
    subgraph AUTONOMOUS
        A1["Turret at 30°"]
        A2["Robot at (X, Y, H)"]
        A3["setInitialOffset()"]
        A4["saveTurretOffset()"]
    end

    subgraph STATIC_VARS["Static Variables<br/>(persist in memory)"]
        SV1["turretOffsetTicks"]
        SV2["initialOffsetDegrees"]
        SV3["savedPosition"]
    end

    subgraph TELEOP
        T1["restoreOffset()"]
        T2["Resume position"]
        T3["enableGoalTracking()"]
    end

    A1 --> SV1
    A2 --> SV3
    A3 --> SV2
    A4 --> SV1
    SV1 --> T1
    SV2 --> T1
    SV3 --> T2
    T1 --> T3
```

### Key Technical Features

**Pedro Pathing Integration** — We use the Pedro Pathing library for smooth path following with Bezier curves. Paths are defined as `BezierLine` segments with heading interpolation, and the `Follower` class handles motion profiling and PID-based trajectory tracking.

**Dynamic Path Generation** — After an intake sweep (which may end early if 3 balls are detected), the robot builds its return path dynamically from its actual current position rather than a hardcoded endpoint. This eliminates drift accumulation.

**Configurable Shooting Timings** — The `AutoShootSystem` supports `setFastTiming()` for Near autos (where every millisecond matters in a 3-cycle, 30-second window) and `setDefaultTiming()` for Far autos with more margin.

**Turret Pre-Aiming** — Before autonomous starts, the driver manually aims the turret to a known angle. The system records this as an offset (`setInitialOffset()`), then holds the position via PID throughout autonomous. This offset persists into TeleOp via static variables in `RobotMotors`, so the turret seamlessly transitions to goal-tracking mode.

**Phase Selection & Manual Advance** — During the init phase, drivers can select which phase to stop after (for testing individual segments) and enable manual-advance mode to step through states one at a time. This dramatically accelerated our field testing.

### Testing & Development Tools

- `TurretTest` (1,047 lines) — comprehensive turret calibration and testing OpMode
- `LocalizationTest` — validates odometry accuracy with known positions
- `TurretCalibration` — encoder-to-degree mapping verification
- `CompleteSystemTest` — end-to-end system integration test

## Results

Our autonomous system reliably scores **6–9 balls** per match in the 30-second autonomous period. The modular architecture allowed us to develop and test each subsystem independently, then integrate them with confidence. The state machine pattern made debugging straightforward — we could pinpoint exactly which state caused an issue and test it in isolation using the phase selection feature.

The same codebase seamlessly transitions from autonomous to driver-controlled operation, with turret offsets, position data, and system states preserved across the boundary.

---

*DECODE Team 14464 — FIRST Tech Challenge 2026*
