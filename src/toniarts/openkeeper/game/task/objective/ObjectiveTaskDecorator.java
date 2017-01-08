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

import com.jme3.math.Vector2f;
import java.awt.Point;
import java.util.ArrayDeque;
import java.util.Date;
import java.util.Deque;
import toniarts.openkeeper.game.task.Task;
import toniarts.openkeeper.tools.convert.map.ArtResource;
import toniarts.openkeeper.world.creature.CreatureControl;

/**
 * A decorator for some simple task to create complex task chains
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class ObjectiveTaskDecorator implements Task, ObjectiveTask {

    private final Task task;
    private final Deque<ObjectiveTask> taskQueue = new ArrayDeque<>();

    public ObjectiveTaskDecorator(Task task) {
        this.task = task;
    }

    @Override
    public Deque<ObjectiveTask> getTaskQueue() {
        return taskQueue;
    }

    @Override
    public void executeTask(CreatureControl creature) {
        task.executeTask(creature);
        ObjectiveTask.super.executeTask(creature);
    }

    @Override
    public void assign(CreatureControl creature, boolean setToCreature) {
        task.assign(creature, false);

        // Override the assign
        creature.setAssignedTask(this);

        if (isWorkerPartyTask() && creature.getParty() != null) {

            // Assign to all workers
            for (CreatureControl c : creature.getParty().getActualMembers()) {
                if (!c.equals(creature) && c.isWorker() && task.canAssign(c)) {
                    task.assign(c, true);
                }
            }
        }
    }

    @Override
    public boolean canAssign(CreatureControl creature) {
        return task.canAssign(creature);
    }

    @Override
    public int getAssigneeCount() {
        return task.getAssigneeCount();
    }

    @Override
    public int getMaxAllowedNumberOfAsignees() {
        return task.getMaxAllowedNumberOfAsignees();
    }

    @Override
    public int getPriority() {
        return task.getPriority();
    }

    @Override
    public Vector2f getTarget(CreatureControl creature) {
        return task.getTarget(creature);
    }

    @Override
    public ArtResource getTaskAnimation(CreatureControl creature) {
        return task.getTaskAnimation(creature);
    }

    @Override
    public Date getTaskCreated() {
        return task.getTaskCreated();
    }

    @Override
    public String getTaskIcon() {
        return task.getTaskIcon();
    }

    @Override
    public Point getTaskLocation() {
        return task.getTaskLocation();
    }

    @Override
    public String getTooltip() {
        return task.getTooltip();
    }

    @Override
    public boolean isFaceTarget() {
        return task.isFaceTarget();
    }

    @Override
    public boolean isReachable(CreatureControl creature) {
        return task.isReachable(creature);
    }

    @Override
    public boolean isValid(CreatureControl creature) {
        return task.isValid(creature);
    }

    @Override
    public void unassign(CreatureControl creature) {
        task.unassign(creature);
    }

}
