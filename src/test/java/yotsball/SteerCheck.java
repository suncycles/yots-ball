package yotsball;

import java.util.Random;

/**
 * Self-check for Steer, run by build.ps1: Minecraft's ground movement simulated tick by tick, crouched at 400% speed
 * (input normalised, times 0.3 crouching, times 0.98, times 0.4, turned by your yaw; you move; velocity times 0.546),
 * from a random start (where you face, which way you're already going) to a random spot up to 10 blocks off. Steer has
 * to put you within GOOD of it and stopped, in time. Exits non-zero on failure.
 */
public final class SteerCheck {
    static final double GOOD = 0.3, FRICTION = 0.6 * 0.91, ACCEL = Steer.SPEED * Steer.SNEAK;  // green under 0.3

    public static void main(String[] args) {
        Random rnd = new Random(1);
        int fails = 0, worst = 0;
        double worstOff = 0;
        for (int trial = 0; trial < 2000; trial++) {
            float yaw = (float) (rnd.nextDouble() * 360 - 180);
            double px = 0, pz = 0, a = rnd.nextDouble() * 2 * Math.PI, d = rnd.nextDouble() * 10;
            double tx = Math.cos(a) * d, tz = Math.sin(a) * d;
            double top = 2.2 * ACCEL, va = rnd.nextDouble() * 2 * Math.PI, vs = rnd.nextDouble() * top;
            double vx = Math.cos(va) * vs, vz = Math.sin(va) * vs;
            int settled = -1;
            for (int t = 0; t < 120; t++) {
                Steer.Keys k = Steer.keys(tx - px, tz - pz, vx, vz, yaw, ACCEL, FRICTION);
                double fwd = (k.forward() ? 1 : 0) - (k.back() ? 1 : 0), strafe = (k.left() ? 1 : 0) - (k.right() ? 1 : 0);
                double len = Math.hypot(fwd, strafe);
                if (len > 0) {
                    fwd /= len;
                    strafe /= len;
                }
                double s = Math.sin(Math.toRadians(yaw)), c = Math.cos(Math.toRadians(yaw));
                vx += (strafe * c - fwd * s) * 0.98 * ACCEL;
                vz += (fwd * c + strafe * s) * 0.98 * ACCEL;
                px += vx;
                pz += vz;
                vx *= FRICTION;
                vz *= FRICTION;
                boolean there = Math.hypot(tx - px, tz - pz) < GOOD && Math.hypot(vx, vz) < 0.02;
                if (there && settled < 0) settled = t;
                if (!there) settled = -1;
            }
            double off = Math.hypot(tx - px, tz - pz);
            worstOff = Math.max(worstOff, off);
            // crouched at 400% you go about 5 blocks a second: 10 blocks in about 40 ticks, plus the stop
            if (settled < 0 || settled > 60) {
                if (fails++ < 5) System.out.printf("FAIL  spot %.1f off at yaw %.0f: %.2f off after 120 ticks, settled at %d%n", d, yaw, off, settled);
            } else {
                worst = Math.max(worst, settled);
            }
        }
        System.out.println(fails == 0
                ? String.format("ok    steer: 2000 runs crouched at 400%%, all within %.1f and stopped by tick %d; worst %.2f off", GOOD, worst, worstOff)
                : fails + " failed");
        System.exit(fails == 0 ? 0 : 1);
    }
}
