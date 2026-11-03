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
package toniarts.openkeeper.view.map;

import com.jme3.scene.BatchNode;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Spatial;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.control.LodControl;
import com.jme3.scene.mesh.IndexBuffer;
import com.jme3.util.BufferUtils;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.regex.Pattern;

/**
 * The models have the vertices of all of their LOD levels in the vertex
 * buffers, but the most detailed level (the only one that {@link BatchNode}
 * keeps) uses just a part of them. The batching still copies all of the
 * vertices, so this removes the unused vertices from the meshes before the
 * batching. The models themselves are left as they are, the compacted meshes are
 * copies, shared by all the geometries of the same model.
 *
 * @author ufdada
 */
public final class BatchVertexCompactor {

    /**
     * The compacted meshes, a missing value means that the mesh can't be
     * compacted (or doesn't need to be)
     */
    private static final Map<Mesh, Mesh> COMPACTED = Collections.synchronizedMap(new WeakHashMap<>());
    private static final Mesh NOT_COMPACTABLE = new Mesh();
    private static final Pattern BATCH_NAME = Pattern.compile(".*-batch[0-9]+");

    private BatchVertexCompactor() {
    }

    /**
     * Compact the meshes of all the not yet batched geometries under the
     * given node. Call right before {@link BatchNode#batch()}.
     *
     * @param batchNode the node about to be batched
     */
    public static void compact(BatchNode batchNode) {
        batchNode.depthFirstTraversal((Spatial spatial) -> {
            if (spatial instanceof Geometry geometry && !geometry.isGrouped() && !isBatch(geometry)) {
                Mesh compacted = getCompacted(geometry.getMesh());
                if (compacted != null) {
                    // The LOD control has nothing to work with in the compacted mesh
                    geometry.removeControl(LodControl.class);
                    geometry.setMesh(compacted);
                }
            }
        });
    }

    /**
     * The geometries that BatchNode has created from the others, named like
     * "name-batch0"
     */
    private static boolean isBatch(Geometry geometry) {
        return geometry.getName() != null && BATCH_NAME.matcher(geometry.getName()).matches();
    }

    private static Mesh getCompacted(Mesh mesh) {
        Mesh result = COMPACTED.get(mesh);
        if (result == null) {
            result = compactMesh(mesh);
            COMPACTED.put(mesh, result != null ? result : NOT_COMPACTABLE);
        }
        return result == NOT_COMPACTABLE ? null : result;
    }

    private static Mesh compactMesh(Mesh mesh) {
        VertexBuffer indices = mesh.getBuffer(VertexBuffer.Type.Index);
        if (indices == null || mesh.getMode() != Mesh.Mode.Triangles || mesh.getMorphTargets().length > 0) {
            return null;
        }

        // Find the used vertices
        int vertexCount = mesh.getVertexCount();
        IndexBuffer in = IndexBuffer.wrapIndexBuffer(indices.getData());
        int indexCount = indices.getNumElements() * 3;
        int[] newIndex = new int[vertexCount];
        Arrays.fill(newIndex, -1);
        for (int i = 0; i < indexCount; i++) {
            int v = in.get(i);
            if (v >= vertexCount) {
                return null;
            }
            newIndex[v] = 0;
        }
        int used = 0;
        for (int i = 0; i < vertexCount; i++) {
            if (newIndex[i] == 0) {
                newIndex[i] = used++;
            }
        }
        if (used == vertexCount || used == 0) {
            return null; // Nothing to gain
        }
        int[] oldIndex = new int[used];
        for (int i = 0; i < vertexCount; i++) {
            if (newIndex[i] >= 0) {
                oldIndex[newIndex[i]] = i;
            }
        }

        // Copy the vertex data, give up on anything unusual
        Mesh result = new Mesh();
        for (VertexBuffer buffer : mesh.getBufferList().getArray()) {
            VertexBuffer.Type type = buffer.getBufferType();
            if (type == VertexBuffer.Type.Index) {
                continue;
            }
            if (buffer.getNumElements() != vertexCount || buffer.getStride() != 0 || buffer.getOffset() != 0
                    || buffer.getInstanceSpan() != 0 || buffer.getData() == null) {
                return null;
            }
            Buffer data = copy(buffer.getData(), buffer.getNumComponents(), oldIndex);
            if (data == null) {
                return null;
            }
            VertexBuffer copy = new VertexBuffer(type);
            copy.setupData(VertexBuffer.Usage.Static, buffer.getNumComponents(), buffer.getFormat(), data);
            copy.setNormalized(buffer.isNormalized());
            result.setBuffer(copy);
        }

        // Remap the indices of the most detailed level
        VertexBuffer.Format format = indices.getFormat();
        int triangles = indices.getNumElements();
        VertexBuffer newIndices = new VertexBuffer(VertexBuffer.Type.Index);
        newIndices.setupData(VertexBuffer.Usage.Static, 3, format, VertexBuffer.createBuffer(format, 3, triangles));
        IndexBuffer out = IndexBuffer.wrapIndexBuffer(newIndices.getData());
        for (int i = 0; i < indexCount; i++) {
            out.put(i, newIndex[in.get(i)]);
        }
        result.setBuffer(newIndices);

        result.setMode(Mesh.Mode.Triangles);
        result.setStatic();
        result.updateBound();
        result.updateCounts();
        return result;
    }

    private static Buffer copy(Buffer source, int components, int[] oldIndex) {
        int count = oldIndex.length;
        if (source instanceof FloatBuffer from) {
            FloatBuffer to = BufferUtils.createFloatBuffer(count * components);
            for (int i = 0; i < count; i++) {
                for (int c = 0; c < components; c++) {
                    to.put(i * components + c, from.get(oldIndex[i] * components + c));
                }
            }
            return to;
        } else if (source instanceof ByteBuffer from) {
            ByteBuffer to = BufferUtils.createByteBuffer(count * components);
            for (int i = 0; i < count; i++) {
                for (int c = 0; c < components; c++) {
                    to.put(i * components + c, from.get(oldIndex[i] * components + c));
                }
            }
            return to;
        } else if (source instanceof ShortBuffer from) {
            ShortBuffer to = BufferUtils.createShortBuffer(count * components);
            for (int i = 0; i < count; i++) {
                for (int c = 0; c < components; c++) {
                    to.put(i * components + c, from.get(oldIndex[i] * components + c));
                }
            }
            return to;
        } else if (source instanceof IntBuffer from) {
            IntBuffer to = BufferUtils.createIntBuffer(count * components);
            for (int i = 0; i < count; i++) {
                for (int c = 0; c < components; c++) {
                    to.put(i * components + c, from.get(oldIndex[i] * components + c));
                }
            }
            return to;
        }
        return null;
    }
}
