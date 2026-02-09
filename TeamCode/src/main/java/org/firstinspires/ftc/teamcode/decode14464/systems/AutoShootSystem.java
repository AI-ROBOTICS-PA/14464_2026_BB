package org.firstinspires.ftc.teamcode.decode14464.systems;

import com.qualcomm.robotcore.hardware.HardwareMap;

/**
 * Auto-Shoot System
 * Reusable auto-shooting functionality with touch sensor feedback
 * Can be used in any TeleOp or Auto program
 */
public class AutoShootSystem {
    private RobotMotors motors;
    private RobotServos servos;
    private RobotSensors sensors;
    private AutoIntakeSystem autoIntake;

    // Auto-shoot state machine
    private enum AutoShootState {
        IDLE,
        POSITION_TURRET,  // Move turret to correct starting slot before first push
        PUSH,
        WAIT_PUSH,
        RETRACT,
        WAIT_RETRACT,
        ADVANCE
    }

    private AutoShootState state = AutoShootState.IDLE;
    private int ballsShot = 0;
    private int ballsToShoot = 0;
    private long stateStartTime = 0;
    private boolean turretMoved = false;

    // Diagnostic counters
    private int sensorDetectedCount = 0;
    private int timeoutUsedCount = 0;

    // Timing constants
    private static final long PUSH_DURATION_MS = 300;
    private static final long RETRACT_TIMEOUT_MS = 500;
    private static final long ADVANCE_DURATION_MS = 300;

    /**
     * Initialize auto-shoot system
     */
    public void init(HardwareMap hardwareMap, RobotMotors motors, RobotServos servos,
                     RobotSensors sensors, AutoIntakeSystem autoIntake) {
        this.motors = motors;
        this.servos = servos;
        this.sensors = sensors;
        this.autoIntake = autoIntake;
    }

    /**
     * Start auto-shoot sequence
     * @param numberOfBalls Number of balls to shoot (1-3)
     * @return true if started successfully, false if already shooting or invalid state
     */
    public boolean start(int numberOfBalls) {
        // Can't start if already shooting
        if (state != AutoShootState.IDLE) {
            return false;
        }

        // Must be in launch mode
        if (autoIntake != null && autoIntake.isIntakeMode()) {
            return false;
        }

        // Valid number of balls
        if (numberOfBalls < 1 || numberOfBalls > 3) {
            return false;
        }

        // Start shooting!
        ballsToShoot = numberOfBalls;
        ballsShot = 0;
        sensorDetectedCount = 0;
        timeoutUsedCount = 0;
        turretMoved = false;

        // Position turret at the correct starting slot before first push
        state = AutoShootState.POSITION_TURRET;
        stateStartTime = System.currentTimeMillis();

        return true;
    }

    /**
     * Start auto-shoot based on actual balls loaded (detected by color sensor).
     * Uses ballsLoaded count instead of servo position index to avoid desync bugs.
     */
    public boolean startFromCurrentPosition() {
        if (autoIntake == null) return false;

        int numberOfBalls = autoIntake.getBallsLoaded();
        if (numberOfBalls <= 0) return false; // No balls to shoot

        return start(numberOfBalls);
    }

    /**
     * Stop auto-shoot sequence
     */
    public void stop() {
        state = AutoShootState.IDLE;
        ballsShot = 0;
        ballsToShoot = 0;

        // Retract ball push servo
        if (servos != null && servos.ballpush_4 != null) {
            servos.ballpush_4.setPosition(RobotConstants.BallPush.RETRACT_POSITION);
        }
    }

    /**
     * Update auto-shoot state machine - call this every loop
     */
    public void update() {
        if (state == AutoShootState.IDLE) {
            return;
        }

        long elapsed = System.currentTimeMillis() - stateStartTime;

        switch (state) {
            case POSITION_TURRET:
                // Move turret to the correct starting slot (highest loaded slot)
                // This ensures we start from the right place even if manual D-Pad moved the servo
                if (!turretMoved) {
                    int targetSlot = ballsToShoot - 1;
                    if (targetSlot >= 0 && targetSlot < RobotConstants.TurretServo.LAUNCH_POSITIONS.length) {
                        if (autoIntake != null) {
                            autoIntake.prepareForShooting();
                        } else if (servos != null && servos.turret_1 != null) {
                            // Fallback: directly set servo if autoIntake not available
                            servos.turret_1.setPosition(RobotConstants.TurretServo.LAUNCH_POSITIONS[targetSlot]);
                        }
                    }
                    turretMoved = true;
                }
                // Wait for turret servo to reach position before pushing
                if (elapsed > ADVANCE_DURATION_MS) {
                    turretMoved = false; // Reset for ADVANCE state use
                    state = AutoShootState.PUSH;
                    stateStartTime = System.currentTimeMillis();
                }
                break;

            case PUSH:
                // Command servo to push position
                if (servos != null && servos.ballpush_4 != null) {
                    servos.ballpush_4.setPosition(RobotConstants.BallPush.PUSH_POSITION);
                }
                state = AutoShootState.WAIT_PUSH;
                stateStartTime = System.currentTimeMillis();
                break;

            case WAIT_PUSH:
                // Wait for servo to push ball
                if (elapsed > PUSH_DURATION_MS) {
                    ballsShot++;
                    state = AutoShootState.RETRACT;
                    stateStartTime = System.currentTimeMillis();
                }
                break;

            case RETRACT:
                // Command servo to retract position
                if (servos != null && servos.ballpush_4 != null) {
                    servos.ballpush_4.setPosition(RobotConstants.BallPush.RETRACT_POSITION);
                }
                state = AutoShootState.WAIT_RETRACT;
                stateStartTime = System.currentTimeMillis();
                break;

            case WAIT_RETRACT:
                // Wait for ball push servo to retract - USE TOUCH SENSOR with timeout safety
                boolean sensorRetracted = (sensors != null && sensors.isBallPushRetracted());
                boolean timeoutReached = elapsed > RETRACT_TIMEOUT_MS;

                if (sensorRetracted) {
                    sensorDetectedCount++;
                    handleRetractComplete();
                } else if (timeoutReached) {
                    timeoutUsedCount++;
                    handleRetractComplete();
                }
                break;

            case ADVANCE:
                // Move turret backwards to previous slot - ONLY ONCE using flag
                if (!turretMoved && autoIntake != null) {
                    autoIntake.moveToPreviousSlot();
                    turretMoved = true;
                }

                // Wait for turret servo to move
                if (elapsed > ADVANCE_DURATION_MS) {
                    state = AutoShootState.PUSH;
                    stateStartTime = System.currentTimeMillis();
                }
                break;
        }
    }

    /**
     * Handle retract complete (sensor or timeout)
     */
    private void handleRetractComplete() {
        // Check if we have more balls to shoot
        if (ballsShot < ballsToShoot) {
            // More balls to shoot - advance turret
            state = AutoShootState.ADVANCE;
            stateStartTime = System.currentTimeMillis();
            turretMoved = false; // Reset flag for new advance
        } else {
            // All balls shot! Reset ball count since turret is now empty
            if (autoIntake != null) {
                autoIntake.resetBallCount();
            }
            state = AutoShootState.IDLE;
        }
    }

    /**
     * Check if auto-shoot is in progress
     */
    public boolean isInProgress() {
        return state != AutoShootState.IDLE;
    }

    /**
     * Check if auto-shoot is complete
     */
    public boolean isComplete() {
        return state == AutoShootState.IDLE && ballsShot > 0;
    }

    /**
     * Get current state as string (for telemetry)
     */
    public String getCurrentState() {
        return state.toString();
    }

    /**
     * Get balls shot count
     */
    public int getBallsShot() {
        return ballsShot;
    }

    /**
     * Get total balls to shoot
     */
    public int getBallsToShoot() {
        return ballsToShoot;
    }

    /**
     * Get sensor detection count (for diagnostics)
     */
    public int getSensorDetectedCount() {
        return sensorDetectedCount;
    }

    /**
     * Get timeout count (for diagnostics)
     */
    public int getTimeoutUsedCount() {
        return timeoutUsedCount;
    }

    /**
     * Get elapsed time in current state (ms)
     */
    public long getStateElapsedTime() {
        if (state == AutoShootState.IDLE) return 0;
        return System.currentTimeMillis() - stateStartTime;
    }

    /**
     * Check if sensor is working properly
     * @return true if sensor is being used (not timeouts)
     */
    public boolean isSensorWorking() {
        if (ballsShot == 0) return false; // No data yet
        return sensorDetectedCount > 0 && timeoutUsedCount == 0;
    }

    /**
     * Get diagnostic status message
     */
    public String getDiagnosticStatus() {
        if (ballsShot == 0) return "Not started";

        if (sensorDetectedCount > 0 && timeoutUsedCount == 0) {
            return "✅ SENSOR WORKING!";
        } else if (timeoutUsedCount > 0 && sensorDetectedCount == 0) {
            return "⚠️ USING TIMEOUT (sensor not working)";
        } else if (sensorDetectedCount > 0 && timeoutUsedCount > 0) {
            return "⚠️ MIXED (sensor unreliable)";
        }

        return "Unknown";
    }
}

