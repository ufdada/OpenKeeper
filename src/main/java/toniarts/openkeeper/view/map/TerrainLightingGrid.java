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

import com.jme3.math.Vector3f;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import toniarts.openkeeper.utils.WorldUtils;

/**
 * Spatial grid for efficient light source lookups. Divides the map into cells
 * and stores light sources in cells they overlap. Queries only check nearby
 * cells instead of all lights in the level.
 *
 * @author OpenKeeper
 */
public class TerrainLightingGrid {

    private static final int CELL_SIZE_TILES = 4;

    private final Set<TerrainLightSource>[][] cells;
    private final int gridWidth;
    private final int gridHeight;
    private final int mapWidth;
    private final int mapHeight;

    @SuppressWarnings("unchecked")
    public TerrainLightingGrid(int mapWidth, int mapHeight) {
        this.mapWidth = mapWidth;
        this.mapHeight = mapHeight;
        this.gridWidth = (int) Math.ceil((double) mapWidth / CELL_SIZE_TILES);
        this.gridHeight = (int) Math.ceil((double) mapHeight / CELL_SIZE_TILES);
        cells = new HashSet[gridWidth][gridHeight];
        for (int x = 0; x < gridWidth; x++) {
            for (int y = 0; y < gridHeight; y++) {
                cells[x][y] = new HashSet<>();
            }
        }
    }

    /**
     * Add a light source to the grid. The light is inserted into all cells it
     * overlaps based on its radius.
     *
     * @param source the light source to add
     */
    public void addLight(TerrainLightSource source) {
        int[] cellRange = getCellRange(source);
        for (int cx = cellRange[0]; cx <= cellRange[2]; cx++) {
            for (int cy = cellRange[1]; cy <= cellRange[3]; cy++) {
                cells[cx][cy].add(source);
            }
        }
    }

    /**
     * Remove a light source from the grid.
     *
     * @param source the light source to remove
     */
    public void removeLight(TerrainLightSource source) {
        int[] cellRange = getCellRange(source);
        for (int cx = cellRange[0]; cx <= cellRange[2]; cx++) {
            for (int cy = cellRange[1]; cy <= cellRange[3]; cy++) {
                cells[cx][cy].remove(source);
            }
        }
    }

    /**
     * Update a light source position/radius. Removes from old cells, adds to
     * new cells.
     *
     * @param source the light source (position/radius must be current)
     */
    public void updateLight(TerrainLightSource source) {
        removeLight(source);
        addLight(source);
    }

    /**
     * Query all light sources that could potentially affect a given world
     * position within a certain radius. Returns candidates from nearby cells.
     *
     * @param worldPos the world position to query around
     * @param queryRadius the maximum radius to search
     * @return list of candidate light sources (may include some outside radius,
     * caller should distance-check)
     */
    public List<TerrainLightSource> getLightsNear(Vector3f worldPos, float queryRadius) {
        List<TerrainLightSource> result = new ArrayList<>();

        int cellX = worldPositionToCellX(worldPos.x);
        int cellY = worldPositionToCellZ(worldPos.z);

        // Determine how many cells to search based on query radius
        int cellRadius = (int) Math.ceil(queryRadius / (CELL_SIZE_TILES * WorldUtils.TILE_WIDTH));

        int startX = Math.max(0, cellX - cellRadius);
        int endX = Math.min(gridWidth - 1, cellX + cellRadius);
        int startY = Math.max(0, cellY - cellRadius);
        int endY = Math.min(gridHeight - 1, cellY + cellRadius);

        for (int cx = startX; cx <= endX; cx++) {
            for (int cy = startY; cy <= endY; cy++) {
                result.addAll(cells[cx][cy]);
            }
        }

        return result;
    }

    /**
     * Get all lights in a set of tile coordinates (for page rebuild).
     *
     * @param tileX tile x coordinate
     * @param tileY tile y coordinate
     * @param radius tile radius to search
     * @return list of candidate light sources
     */
    public List<TerrainLightSource> getLightsNearTile(int tileX, int tileY, float radius) {
        Vector3f worldPos = WorldUtils.pointToVector3f(tileX, tileY);
        return getLightsNear(worldPos, radius * WorldUtils.TILE_WIDTH);
    }

    private int[] getCellRange(TerrainLightSource source) {
        float radiusTiles = source.radius() / WorldUtils.TILE_WIDTH;
        Vector3f pos = source.worldPosition();

        int tileMinX = Math.max(0, (int) Math.floor((pos.x / WorldUtils.TILE_WIDTH) - radiusTiles));
        int tileMinY = Math.max(0, (int) Math.floor((pos.z / WorldUtils.TILE_WIDTH) - radiusTiles));
        int tileMaxX = Math.min(mapWidth - 1, (int) Math.ceil((pos.x / WorldUtils.TILE_WIDTH) + radiusTiles));
        int tileMaxY = Math.min(mapHeight - 1, (int) Math.ceil((pos.z / WorldUtils.TILE_WIDTH) + radiusTiles));

        int cellMinX = tileToCellX(tileMinX);
        int cellMinY = tileToCellY(tileMinY);
        int cellMaxX = tileToCellX(tileMaxX);
        int cellMaxY = tileToCellY(tileMaxY);

        return new int[]{cellMinX, cellMinY, cellMaxX, cellMaxY};
    }

    private int tileToCellX(int tileX) {
        return Math.max(0, Math.min(gridWidth - 1, tileX / CELL_SIZE_TILES));
    }

    private int tileToCellY(int tileY) {
        return Math.max(0, Math.min(gridHeight - 1, tileY / CELL_SIZE_TILES));
    }

    private int worldPositionToCellX(float worldX) {
        int tileX = Math.round(worldX / WorldUtils.TILE_WIDTH);
        return tileToCellX(tileX);
    }

    private int worldPositionToCellZ(float worldZ) {
        int tileY = Math.round(worldZ / WorldUtils.TILE_WIDTH);
        return tileToCellY(tileY);
    }
}
