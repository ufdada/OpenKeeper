/*
 * Copyright (C) 2014-2017 OpenKeeper
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
package toniarts.openkeeper.game.task.objective;

import toniarts.openkeeper.game.task.worker.DigTileTask;
import toniarts.openkeeper.tools.convert.map.Terrain;
import toniarts.openkeeper.world.TileData;
import toniarts.openkeeper.world.WorldState;
import toniarts.openkeeper.world.creature.CreatureControl;

/**
 * Dig tile task for objectives
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class ObjectiveDigTileTask extends DigTileTask {

    public ObjectiveDigTileTask(WorldState worldState, int x, int y, short playerId) {
        super(worldState, x, y, playerId);
    }

    @Override
    public boolean isValid(CreatureControl creature) {
        TileData tile = worldState.getMapData().getTile(getTaskLocation());
        return tile.getTerrain().getFlags().contains(Terrain.TerrainFlag.SOLID) && (tile.getTerrain().getFlags().contains(Terrain.TerrainFlag.DWARF_CAN_DIG_THROUGH) || tile.getTerrain().getFlags().contains(Terrain.TerrainFlag.ATTACKABLE));
    }

    @Override
    public void executeTask(CreatureControl creature) {
        if (creature.isWorker()) { // Only workers
            super.executeTask(creature);
        }
    }

}
