package org.firstinspires.ftc.teamcode.decode14464.systems;

import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

/**
 * DECODE TeleOp - RED Alliance Version
 * 
 * Identical to DECODE TeleOp (BLUE) but defaults to RED alliance.
 * This means:
 * - Default starting position: (95, 0) heading 0°
 * - Default goal target: (132, 132) - Red alliance basket
 * - Default AprilTag: Tag 24 (Red)
 * 
 * User can still toggle to BLUE with GP2 A/Cross before pressing START.
 * 
 * Pick the right program for your alliance:
 * - DECODE TeleOp (BLUE) → defaults BLUE, toggle to RED if needed
 * - DECODE TeleOp (RED)  → defaults RED, toggle to BLUE if needed
 */
@TeleOp(name = "DECODE TeleOp (RED)", group = "Main")
public class DecodeTeleOp_Red extends DecodeTeleOp {

    @Override
    protected LimelightManager.Alliance getDefaultAlliance() {
        return LimelightManager.Alliance.RED;
    }
}
