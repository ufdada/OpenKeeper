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
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import toniarts.openkeeper.game.map.IMapDataInformation;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.WorldUtils;

/**
 * Central service for terrain vertex lighting. Maintains a registry of all
 * light sources (torches, lava, effects) and computes per-vertex lighting
 * contributions. Uses a spatial grid for efficient neighbor light queries.
 * <p>
 * Only computes "base" lighting for terrain baking. Dynamic flicker effects are
 * handled separately by JME PointLights on movable objects.
 *
 * @author OpenKeeper
 */
public class TerrainLightingService {

    private static final Logger logger = System.getLogger(TerrainLightingService.class.getName());

    private static final int PAGE_SIZE = 8;

    /**
     * Base ambient contribution so unlit areas are not completely black.
     */
    private static final float AMBIENT_R = 0.05f;
    private static final float AMBIENT_G = 0.05f;
    private static final float AMBIENT_B = 0.05f;

    /**
     * Overlap in tiles beyond page boundaries when computing lighting for a
     * page. Ensures cross-page seams are smooth.
     */
    private static final int CROSS_PAGE_OVERLAP = 2;

    private final TerrainLightingGrid grid;
    private final Map<Point, TerrainLightSource> lightRegistry = new HashMap<>();
    private final IMapDataInformation<?> mapData;
    private final Set<Point> dirtyPages = new HashSet<>();
    private Consumer<Set<Point>> dirtyPageCallback;

    public TerrainLightingService(IMapDataInformation<?> mapData) {
        this.mapData = mapData;
        this.grid = new TerrainLightingGrid(mapData.getWidth(), mapData.getHeight());
    }

    /**
     * Set a callback that is invoked when pages need to be rebuilt. The
     * callback receives the set of dirty page coordinates.
     *
     * @param callback the callback to invoke
     */
    public void setDirtyPageCallback(Consumer<Set<Point>> callback) {
        this.dirtyPageCallback = callback;
    }

    /**
     * Register a light source at a given tile position.
     *
     * @param tile the tile coordinates
     * @param source the light source data
     */
    public void addLight(Point tile, TerrainLightSource source) {
        lightRegistry.put(tile, source);
        grid.addLight(source);
        markAffectedPagesDirty(tile, source);
    }

    /**
     * Remove a light source at a given tile position.
     *
     * @param tile the tile coordinates
     */
    public void removeLight(Point tile) {
        TerrainLightSource source = lightRegistry.remove(tile);
        if (source != null) {
            grid.removeLight(source);
            markAffectedPagesDirty(tile, source);
        }
    }

    /**
     * Update a light source at a given tile position.
     *
     * @param tile the tile coordinates
     * @param source the updated light source data
     */
    public void updateLight(Point tile, TerrainLightSource source) {
        TerrainLightSource old = lightRegistry.put(tile, source);
        if (old != null) {
            grid.removeLight(old);
        }
        grid.addLight(source);
        markAffectedPagesDirty(tile, source);
    }

    /**
     * Get the number of registered light sources.
     *
     * @return light count
     */
    public int getLightCount() {
        return lightRegistry.size();
    }

    /**
     * Compute vertex colors for a batch of vertex world positions. For each
     * vertex, sums the contribution of all nearby light sources with linear
     * distance attenuation.
     *
     * @param worldPositions array of vertex world positions
     * @return array of RGBA colors, one per vertex
     */
    public ColorRGBA[] computeVertexColors(Vector3f[] worldPositions) {
        ColorRGBA[] colors = new ColorRGBA[worldPositions.length];

        for (int i = 0; i < worldPositions.length; i++) {
            colors[i] = computeSingleVertexColor(worldPositions[i]);
        }

        return colors;
    }

    /**
     * Compute the vertex color for a single world position by summing all
     * nearby light contributions.
     *
     * @param worldPos the vertex world position
     * @return the computed RGBA color
     */
    public ColorRGBA computeSingleVertexColor(Vector3f worldPos) {
        float r = AMBIENT_R;
        float g = AMBIENT_G;
        float b = AMBIENT_B;

        // Find max radius among all lights for the grid query
        float maxRadius = getMaxLightRadius();

        List<TerrainLightSource> candidates = grid.getLightsNear(worldPos, maxRadius);
        for (TerrainLightSource light : candidates) {
            float dist = worldPos.distance(light.worldPosition());
            if (dist >= light.radius()) {
                continue;
            }

            float attenuation = 1.0f - (dist / light.radius());
            attenuation = FastMath.clamp(attenuation, 0f, 1f);

            // Apply attenuation curve (smoothstep for nicer falloff)
            attenuation = attenuation * attenuation * (3f - 2f * attenuation);

            r += light.color().r * attenuation;
            g += light.color().g * attenuation;
            b += light.color().b * attenuation;
        }

        // Clamp to [0, 1]
        r = FastMath.clamp(r, 0f, 1f);
        g = FastMath.clamp(g, 0f, 1f);
        b = FastMath.clamp(b, 0f, 1f);

        return new ColorRGBA(r, g, b, 1f);
    }

    /**
     * Get all dirty pages that need lighting rebuild.
     *
     * @return set of page coordinates that are dirty
     */
    public Set<Point> getDirtyPages() {
        return new HashSet<>(dirtyPages);
    }

    /**
     * Clear all dirty page flags.
     */
    public void clearDirtyPages() {
        dirtyPages.clear();
    }

    /**
     * Mark a single page as dirty.
     *
     * @param pageX page x coordinate
     * @param pageY page y coordinate
     */
    public void markPageDirty(int pageX, int pageY) {
        dirtyPages.add(new Point(pageX, pageY));
    }

    /**
     * Get the page coordinate for a given tile coordinate.
     *
     * @param tileX tile x
     * @param tileY tile y
     * @return page coordinate as Point(pageX, pageY)
     */
    public static Point tileToPage(int tileX, int tileY) {
        int pageX = tileX / PAGE_SIZE;
        int pageY = tileY / PAGE_SIZE;
        return new Point(pageX, pageY);
    }

    /**
     * Get the tile range for a page (inclusive).
     *
     * @param pageX page x
     * @param pageY page y
     * @return int[]{minTileX, minTileY, maxTileX, maxTileY}
     */
    public static int[] getPageTileRange(int pageX, int pageY) {
        int minTileX = pageX * PAGE_SIZE;
        int minTileY = pageY * PAGE_SIZE;
        int maxTileX = Math.min(minTileX + PAGE_SIZE - 1, /* map width */ Integer.MAX_VALUE);
        int maxTileY = Math.min(minTileY + PAGE_SIZE - 1, /* map height */ Integer.MAX_VALUE);
        return new int[]{minTileX, minTileY, maxTileX, maxTileY};
    }

    /**
     * Mark pages affected by a light source as dirty. Includes the light's
     * tile page and all pages within the light's radius.
     */
    private void markAffectedPagesDirty(Point tile, TerrainLightSource source) {
        float radiusTiles = source.radius() / WorldUtils.TILE_WIDTH;

        // Determine page range affected by this light
        int minPageX = Math.max(0, (int) Math.floor((tile.x - radiusTiles) / (double) PAGE_SIZE));
        int minPageY = Math.max(0, (int) Math.floor((tile.y - radiusTiles) / (double) PAGE_SIZE));
        int maxPageX = (int) Math.floor((tile.x + radiusTiles) / (double) PAGE_SIZE);
        int maxPageY = (int) Math.floor((tile.y + radiusTiles) / (double) PAGE_SIZE);

        int mapPagesX = (int) Math.ceil((double) mapData.getWidth() / PAGE_SIZE);
        int mapPagesY = (int) Math.ceil((double) mapData.getHeight() / PAGE_SIZE);
        maxPageX = Math.min(maxPageX, mapPagesX - 1);
        maxPageY = Math.min(maxPageY, mapPagesY - 1);

        for (int px = minPageX; px <= maxPageX; px++) {
            for (int py = minPageY; py <= maxPageY; py++) {
                dirtyPages.add(new Point(px, py));
            }
        }
    }

    private float getMaxLightRadius() {
        float maxRadius = 0;
        for (TerrainLightSource source : lightRegistry.values()) {
            if (source.radius() > maxRadius) {
                maxRadius = source.radius();
            }
        }
        return maxRadius;
    }
}
