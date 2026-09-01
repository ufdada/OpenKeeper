/*
 * Copyright (C) 2014-2015 OpenKeeper
 *
 * OpenKeeper is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * OpenKeeper is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with OpenKeeper.  If not, see <http://www.gnu.org/licenses/>.
 */
package toniarts.openkeeper.view.control;

import com.jme3.light.PointLight;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.control.AbstractControl;

/**
 * Animates a torch {@link PointLight} so that its intensity flickers. The
 * flicker is mostly discrete: the brightness holds at a level, then snaps
 * abruptly to a new one, giving a sharp, flame-like jitter rather than a
 * smooth sine glide. A per-light variant (0..N) selects a distinct step
 * sequence so neighbouring torches do not flicker in sync. Updates the
 * real-time light directly each frame, which is far cheaper than rebuilding
 * baked vertex lighting.
 *
 * @author OpenKeeper
 */
public final class TorchLightControl extends AbstractControl {

    /**
     * Number of distinct flicker patterns.
     */
    public static final int FLICKER_VARIANTS = 8;

    /**
     * Highest the light can reach as a fraction of its base intensity.
     */
    private static final float MAX_SCALE = 1.0f;

    /**
     * Lowest the light can dip to as a fraction of its base intensity. Keep
     * close to 1.0 for a subtle flicker.
     */
    private static final float MIN_SCALE = 0.85f;

    /**
     * How often the brightness steps to a new level, in Hz.
     */
    private static final float RATE = 15f;

    /**
     * Fraction of each step interval the level is held before snapping.
     */
    private static final float HOLD_FRACTION = 0.8f;

    /**
     * How much a slow sine is mixed in to keep the pattern from feeling too
     * mechanical. 0 is fully stepped, 1 is fully smooth.
     */
    private static final float SINE_WEIGHT = 0.2f;

    /**
     * Slow sine frequency in rad/s.
     */
    private static final float SINE_FREQUENCY = 2.5f;

    private final PointLight light;
    private final ColorRGBA baseColor;
    private final float baseRadius;

    // Per-variant deterministic seed for a distinct, stable step sequence
    private final int variantSeed;
    // Slow drift term, kept subtle
    private final float phase;
    private final float speed;
    private float time;

    /**
     * Construct a light flicker control.
     *
     * @param light the point light to animate
     * @param variant the flicker variant (0..N-1) selecting the pattern
     * @param minScale unused; kept for call-site compatibility (the model has
     * its own minimum)
     */
    public TorchLightControl(PointLight light, int variant, float minScale) {
        this.light = light;
        this.baseColor = light.getColor().clone();
        this.baseRadius = light.getRadius();

        int v = Math.floorMod(variant, FLICKER_VARIANTS);
        this.variantSeed = v + 1;
        float fraction = FLICKER_VARIANTS == 0 ? 0f : v / (float) FLICKER_VARIANTS;
        this.phase = fraction * FastMath.TWO_PI;
        this.speed = 0.9f + 0.2f * fraction;
    }

    /**
     * Cheap deterministic hash of a positive integer into [0, 1).
     */
    private static float hash01(int x) {
        int h = x * 374761393 + 668265263;
        h = (h ^ (h >>> 13)) * 1274126177;
        h = h ^ (h >>> 16);
        return (h & 0x7fffffff) / 2147483648f;
    }

    /**
     * Map a hash value to the brightness range.
     */
    private float level(float h) {
        return MIN_SCALE + (MAX_SCALE - MIN_SCALE) * h;
    }

    /**
     * Compute the current intensity scale for the given time.
     *
     * @param time the animation time in seconds
     * @return intensity scale in the range [MIN_SCALE, MAX_SCALE]
     */
    public float flickerAt(float time) {
        // Stepped component: hold, then snap abruptly to a new level
        float t = time * RATE;
        int step = (int) FastMath.floor(t);
        float frac = t - step;
        float current = hash01(step * 7919 + variantSeed);
        float next = hash01((step + 1) * 7919 + variantSeed);

        float stepped;
        if (frac < HOLD_FRACTION) {
            stepped = level(current);
        } else {
            stepped = level(next);
        }

        // Subtle slow sine drift to avoid a purely mechanical repeat
        float sine = 0.5f + 0.5f * FastMath.sin(time * SINE_FREQUENCY * speed + phase);
        float drift = MIN_SCALE + (MAX_SCALE - MIN_SCALE) * sine;

        float scale = stepped * (1f - SINE_WEIGHT) + drift * SINE_WEIGHT;
        return FastMath.clamp(scale, MIN_SCALE, MAX_SCALE);
    }

    @Override
    protected void controlUpdate(float tpf) {
        time += tpf;
        float scale = flickerAt(time);

        light.setColor(new ColorRGBA(
                baseColor.r * scale,
                baseColor.g * scale,
                baseColor.b * scale,
                baseColor.a));
        light.setRadius(baseRadius * scale);
    }

    @Override
    protected void controlRender(RenderManager rm, ViewPort vp) {
        // Nothing to render
    }
}
