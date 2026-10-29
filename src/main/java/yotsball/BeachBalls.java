package yotsball;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

/**
 * SkyHanni's Beach Ball Line (BeachBallCatchHelper), read by reflection since it's all private: the beach balls it
 * tracks (entity id to Predictor), and where each comes back down (the end of its predicted path: where it gets back
 * to the height you last hit it at).
 */
final class BeachBalls {
    static final double RANGE = 30;  // blocks: the nearest ball this close is yours (a long hit can carry it ahead of you)
    private Map<?, ?> predictors;
    private Method path, getX, getZ;
    private boolean missing;

    /** The ball (its entity) and where it comes down. */
    record Landing(Entity ball, double x, double z) {}

    /**
     * The nearest beach ball within RANGE and where it comes down: the end of SkyHanni's predicted path, or straight
     * under the ball while there's no prediction yet. Null if there's no ball, or SkyHanni's isn't readable.
     */
    Landing landing(Minecraft mc, LocalPlayer p) {
        if (missing) return null;
        try {
            if (predictors == null) {
                Field f = Class.forName("at.hannibal2.skyhanni.features.event.yearoftheseal.BeachBallCatchHelper").getDeclaredField("predictors");
                f.setAccessible(true);
                predictors = (Map<?, ?>) f.get(null);
            }
            Entity ball = null;
            Object predictor = null;
            for (Map.Entry<?, ?> e : List.copyOf(predictors.entrySet())) {
                Entity en = mc.level.getEntity((Integer) e.getKey());
                if (en != null && Math.hypot(en.getX() - p.getX(), en.getZ() - p.getZ()) < RANGE
                        && (ball == null || en.distanceToSqr(p) < ball.distanceToSqr(p))) {
                    ball = en;
                    predictor = e.getValue();
                }
            }
            if (ball == null) return null;
            if (path == null) {
                path = predictor.getClass().getMethod("getPredictedPath");
                path.setAccessible(true);  // a public method of SkyHanni's private class
            }
            List<?> points = (List<?>) path.invoke(predictor);
            if (points.size() < 2) return new Landing(ball, ball.getX(), ball.getZ());
            Object end = points.getLast();
            if (getX == null) {
                getX = end.getClass().getMethod("getX");
                getZ = end.getClass().getMethod("getZ");
            }
            return new Landing(ball, (Double) getX.invoke(end), (Double) getZ.invoke(end));
        } catch (ReflectiveOperationException | RuntimeException e) {
            missing = true;
            YotsBall.LOG.error("Can't read SkyHanni's beach ball predictions (is SkyHanni 9.3+ installed?)", e);
            YotsBall.say(mc, "§c>> YOTS Ball: can't read SkyHanni's beach ball predictions (SkyHanni missing or changed)");
            return null;
        }
    }
}
