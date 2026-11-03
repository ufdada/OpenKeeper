/*
 * Copyright (C) 2014-2026 OpenKeeper
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

import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Geometry;
import com.jme3.scene.control.LodControl;

/**
 * Selects the LOD level purely by the distance of the camera, one level
 * coarser for every {@value #TILES_PER_LEVEL} tiles. jME's own
 * {@link LodControl} decides by the on-screen size of the bounds, which keeps
 * the most detailed level for very long.
 *
 * @author ufdada
 */
public final class DistanceLodControl extends LodControl {

    /**
     * Name of the main view port, shadow and other passes must not make the
     * decision
     */
    private static final String MAIN_VIEW_PORT = "Default";
    /**
     * The distance in tiles after which the next LOD level is used
     */
    public static final float TILES_PER_LEVEL = 12f;

    @Override
    protected void controlRender(RenderManager rm, ViewPort vp) {
        if (!(spatial instanceof Geometry geometry) || !MAIN_VIEW_PORT.equals(vp.getName())) {
            return;
        }

        int levels = geometry.getMesh().getNumLodLevels();
        if (levels <= 1) {
            return;
        }

        float distance = geometry.getWorldBound().getCenter().distance(vp.getCamera().getLocation());
        int level = Math.min(levels - 1, (int) (distance / TILES_PER_LEVEL));
        if (geometry.getLodLevel() != level) {
            geometry.setLodLevel(level);
        }
    }
}
