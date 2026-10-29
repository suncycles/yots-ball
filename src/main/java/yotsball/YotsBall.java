package yotsball;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * YOTS Ball, a SkyHanni addon for the Year of the Seal beach ball: walks you to where SkyHanni's Beach Ball Line says
 * your ball comes down (see BeachBalls), with the movement keys only. Your camera faces straight down and never turns;
 * W/A/S/D go where it already faces. While there's a ball you're crouched the whole time, and you only move while it's
 * on its way down: going up, you stay put. Steering takes your speed as 400% (see Steer). The toggle key (B by
 * default; Controls > Miscellaneous) turns it on and off.
 */
public final class YotsBall implements ClientModInitializer {
    static final Logger LOG = LoggerFactory.getLogger("yots-ball");
    static final double FALLING = 0.01;  // blocks a tick the ball must drop by to count as on its way down
    private final KeyMapping toggleKey = new KeyMapping("key.yots-ball.toggle", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_B, KeyMapping.Category.MISC);
    private final BeachBalls balls = new BeachBalls();
    private boolean on, steering, falling;
    private int ballId = -1;
    private double ballY;  // the ball's height last tick it moved

    @Override
    public void onInitializeClient() {
        KeyMappingHelper.registerKeyMapping(toggleKey);
        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
    }

    private void tick(Minecraft mc) {
        while (toggleKey.consumeClick()) {
            on = !on;
            say(mc, on ? "§a>> YOTS Ball on: walking to your beach ball" : "§e>> YOTS Ball off");
        }
        LocalPlayer p = mc.player;
        BeachBalls.Landing spot = on && p != null && mc.level != null && mc.gui.screen() == null ? balls.landing(mc, p) : null;
        if (spot == null) {
            if (steering) press(mc, Steer.Keys.NONE, false);  // let go once: your keys are yours again
            steering = false;
            ballId = -1;
            return;
        }
        steering = true;
        p.setXRot(90);  // straight down; the yaw, which way the keys go, never changes
        // On its way down: its height dropped since it last moved (a tick it doesn't move keeps what it was doing)
        double y = spot.ball().getY();
        if (spot.ball().getId() != ballId) {
            ballId = spot.ball().getId();
            falling = false;
            ballY = y;
        } else if (Math.abs(y - ballY) > FALLING) {
            falling = y < ballY;
            ballY = y;
        }
        if (!falling) {
            press(mc, Steer.Keys.NONE, true);  // going up: stay put, crouched
            return;
        }
        // Minecraft's movement, crouched at 400% speed: on the ground a held key adds 0.4 x 0.3 x 0.216 / block
        // friction^3 to your velocity each tick and block friction x 0.91 of it is left after; in the air 0.02 x 0.3
        // and 0.91 left
        boolean ground = p.onGround();
        float block = mc.level.getBlockState(p.getBlockPosBelowThatAffectsMyMovement()).getBlock().getFriction();
        double accel = (ground ? Steer.SPEED * 0.21600002 / (block * block * block) : 0.02) * Steer.SNEAK;
        double friction = ground ? block * 0.91 : 0.91;
        Vec3 v = p.getDeltaMovement();
        press(mc, Steer.keys(spot.x() - p.getX(), spot.z() - p.getZ(), v.x, v.z, p.getYRot(), accel, friction), true);
    }

    private static void press(Minecraft mc, Steer.Keys k, boolean crouch) {
        mc.options.keyUp.setDown(k.forward());
        mc.options.keyDown.setDown(k.back());
        mc.options.keyLeft.setDown(k.left());
        mc.options.keyRight.setDown(k.right());
        mc.options.keyShift.setDown(crouch);
    }

    static void say(Minecraft mc, String message) {
        if (mc.player != null) mc.player.sendSystemMessage(Component.literal(message));
    }
}
