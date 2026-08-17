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
package toniarts.openkeeper.view.map;

import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import java.util.EnumSet;
import toniarts.openkeeper.tools.convert.map.Light.LightFlag;

/**
 * Represents a static or semi-static light source that contributes to terrain
 * vertex lighting. Stores position, color, radius, and flags (flicker, pulse,
 * player-coloured, etc.).
 *
 * @author OpenKeeper
 */
public record TerrainLightSource(
        Vector3f worldPosition,
        ColorRGBA color,
        float radius,
        EnumSet<LightFlag> flags
) {

    public static TerrainLightSource of(Vector3f position, ColorRGBA color, float radius) {
        return new TerrainLightSource(position.clone(), color.clone(), radius,
                EnumSet.noneOf(LightFlag.class));
    }

    public static TerrainLightSource of(Vector3f position, ColorRGBA color, float radius,
            EnumSet<LightFlag> flags) {
        return new TerrainLightSource(position.clone(), color.clone(), radius, flags.clone());
    }

    /**
     * Check if this light has the flicker flag.
     */
    public boolean isFlickering() {
        return flags.contains(LightFlag.FLICKER);
    }

    /**
     * Check if this light has the pulse flag.
     */
    public boolean isPulsing() {
        return flags.contains(LightFlag.PULSE);
    }

    /**
     * Check if this light is player-coloured.
     */
    public boolean isPlayerColoured() {
        return flags.contains(LightFlag.PLAYER_COLOURED);
    }
}
