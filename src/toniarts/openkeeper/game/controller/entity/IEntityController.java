/*
 * Copyright (C) 2014-2019 OpenKeeper
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
package toniarts.openkeeper.game.controller.entity;

import com.jme3.math.Vector3f;
import com.simsilica.es.EntityId;

/**
 * Common interface for all kinds of entities
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public interface IEntityController extends Comparable<IEntityController> {

    public EntityId getEntityId();

    public short getOwnerId();

    public Vector3f getPosition();

    public int getHealth();

    public int getMaxHealth();

    public boolean isFullHealth();

    /**
     * Get percentage of health
     *
     * @return human formatted percentage
     */
    default int getHealthPercentage() {
        return (int) ((getHealth() * 100.0f) / getMaxHealth());
    }

    public boolean isPickedUp();

    /**
     * Removes the entity from the world immediately. Entity loses all its
     * posession
     *
     * @see #removePosession()
     */
    public void remove();

    /**
     * Removes all posession from the entity, the posession is dropped back to
     * world
     *
     * @see #remove()
     */
    public void removePosession();

}
