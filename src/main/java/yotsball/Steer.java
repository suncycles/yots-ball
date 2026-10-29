package yotsball;

/**
 * Which movement keys take you to a spot and stop you on it, crouched the whole way, your look never moving: W goes
 * where you face, A to your left. Minecraft, each tick: your velocity gains your input (a key's share is 0.71 with two
 * held) times accel; you move by it; it's multiplied by friction. So with no keys you coast
 * velocity / (1 - friction) further, and holding a key one tick moves where you'll stop by about
 * accel / (1 - friction): a "step", about 0.26 blocks crouched at 400% speed. Each axis (ahead, to your left) presses
 * toward where you'll stop short of the spot, only while that's more than half a step off.
 */
final class Steer {
    static final double SPEED = 0.4;  // your movement speed, taken as 400% (Minecraft's 0.1 is 100%)
    static final double SNEAK = 0.3;  // what crouching leaves of your input

    record Keys(boolean forward, boolean back, boolean left, boolean right) {
        static final Keys NONE = new Keys(false, false, false, false);
    }

    /**
     * Keys for this tick. dx, dz: the spot, from you. vx, vz: your velocity (blocks a tick). yaw: where you face
     * (degrees). accel: what a held key adds to your velocity each tick (crouching counted). friction: what's left of
     * your velocity after a tick.
     */
    static Keys keys(double dx, double dz, double vx, double vz, float yaw, double accel, double friction) {
        double s = Math.sin(Math.toRadians(yaw)), c = Math.cos(Math.toRadians(yaw)), coast = 1 / (1 - friction);
        // where you'll stop with no keys, short of the spot: ahead is (-sin, cos), your left (cos, sin)
        double ahead = (-s * dx + c * dz) - (-s * vx + c * vz) * coast;
        double left = (c * dx + s * dz) - (c * vx + s * vz) * coast;
        double half = 0.98 * accel * coast / 2;
        return new Keys(ahead > half, ahead < -half, left > half, left < -half);
    }
}
