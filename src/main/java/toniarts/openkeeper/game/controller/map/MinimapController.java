package toniarts.openkeeper.game.controller.map;

import com.simsilica.es.Entity;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntitySet;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import static java.awt.image.BufferedImage.TYPE_INT_RGB;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.IndexColorModel;
import java.awt.image.PixelInterleavedSampleModel;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.List;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.map.IMapDataInformation;
import toniarts.openkeeper.game.map.IMapTileInformation;
import toniarts.openkeeper.tools.convert.map.KwdFile;
import toniarts.openkeeper.tools.convert.map.Terrain;
import toniarts.openkeeper.utils.MapThumbnailGenerator;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.WorldUtils;
import static java.awt.image.BufferedImage.TYPE_INT_ARGB;

/**
 * Controller for runtime minimap generation and creature overlays.
 */
public final class MinimapController {

    private MinimapController() {
        // utility class
    }

    private static final int CREATURE_MARKER_RADIUS = 2;

    private static final Logger logger = System.getLogger(MinimapController.class.getName());

    public static final class CreatureMarker {
        private final int x;
        private final int y;
        private final short playerId;
        private final int blinkPhase;

        public CreatureMarker(int x, int y, short playerId, int blinkPhase) {
            this.x = x;
            this.y = y;
            this.playerId = playerId;
            this.blinkPhase = blinkPhase;
        }

        public int getX() {
            return x + 1;
        }

        public int getY() {
            return y + 1;
        }

        public short getPlayerId() {
            return playerId;
        }

        public int getBlinkPhase() {
            return blinkPhase;
        }
    }

    public static BufferedImage createMinimapImage(final KwdFile kwd,
            final IMapDataInformation<IMapTileInformation> mapData,
            final Integer width,
            final Integer height,
            final boolean preserveAspectRatio) {
        return MapThumbnailGenerator.generateMapFromMap(kwd, mapData, width, height, preserveAspectRatio);
    }

    public static List<CreatureMarker> collectCreatureMarkers(final EntityData entityData) {
        List<CreatureMarker> markers = new ArrayList<>();
        if (entityData == null) {
            return markers;
        }

        try {
            EntitySet creatureEntities = entityData.getEntities(CreatureComponent.class, Position.class, Owner.class);
            for (Entity entity : creatureEntities) {
                Position position = entityData.getComponent(entity.getId(), Position.class);
                Owner owner = entityData.getComponent(entity.getId(), Owner.class);
                if (position == null || owner == null || position.position == null) {
                    continue;
                }

                Point tile = WorldUtils.vectorToPoint(position.position);
                if (tile != null) {
                    markers.add(new CreatureMarker(tile.x, tile.y, owner.ownerId, (int) (System.nanoTime() / 50_000_000L)));
                }
            }
            creatureEntities.release();
        } catch (Exception ignored) {
            // ignore if entity stream is not available in this context
        }

        return markers;
    }

    public static BufferedImage applyCreatureMarkers(final KwdFile kwd,
            final IMapDataInformation<IMapTileInformation> mapData,
            final BufferedImage baseImage,
            final List<CreatureMarker> markers) {
        return addCreatureMarkersToMap(kwd, mapData, baseImage, markers);
    }

    public static BufferedImage renderSelectedTiles(final BufferedImage sourceImage,
            final IMapDataInformation<IMapTileInformation> mapData,
            final short playerId) {
        if (sourceImage == null || mapData == null) {
            return sourceImage;
        }

        BufferedImage rendered = new BufferedImage(sourceImage.getWidth(), sourceImage.getHeight(), TYPE_INT_ARGB);
        Graphics2D g = rendered.createGraphics();
        g.drawImage(sourceImage, 0, 0, null);

        final int cellWidth = Math.max(1, sourceImage.getWidth() / Math.max(1, mapData.getWidth()));
        final int cellHeight = Math.max(1, sourceImage.getHeight() / Math.max(1, mapData.getHeight()));
        final Color selectedColor = new Color(0, 255, 255, 255);

        for (int y = 0; y < mapData.getHeight(); y++) {
            for (int x = 0; x < mapData.getWidth(); x++) {
                IMapTileInformation tile = mapData.getTile(x, y);
                if (tile == null || !tile.isSelected(playerId)) {
                    continue;
                }

                int px = x * cellWidth;
                int py = y * cellHeight;
                g.setColor(selectedColor);
                int offsetX = cellWidth;
                int offsetY = (cellHeight + cellHeight / 2);
                g.fillRect(px + offsetX, py + offsetY, cellWidth, cellHeight);
            }
        }

        g.dispose();
        return rendered;
    }

    /**
     * FIXME: This is a copy of the MapThumbnailGenerator.generateMap method, but it uses the runtime map data instead of the KWD map data. This is a temporary solution until we can figure out how to properly handle this.
     * 
     * Generate a map image from runtime map data (IMapDataInformation). Uses
     * the provided KwdFile for terrain lookup and palette.
     */
    public static BufferedImage generateMapFromMap(final KwdFile kwd, final IMapDataInformation<IMapTileInformation> mapData, final Integer width, final Integer height, final boolean preserveAspectRatio) {

        // Determine wanted width/height based on runtime map size
        int imageWidth = mapData.getWidth();
        int imageHeight = mapData.getHeight();
        int drawWidth = mapData.getWidth();
        int drawHeight = mapData.getHeight();
        if (width != null || height != null) {
            imageWidth = (width != null ? width : imageWidth);
            imageHeight = (height != null ? height : imageHeight);

            if (preserveAspectRatio) {
                if (width != null && height == null) {
                    imageHeight = imageWidth * mapData.getHeight() / mapData.getWidth();
                } else if (height != null && width == null) {
                    imageWidth = imageHeight * mapData.getWidth() / mapData.getHeight();
                } else {
                    int byWidthArea = (imageWidth * mapData.getHeight() / mapData.getWidth()) * imageWidth;
                    int byHeightArea = (imageHeight * mapData.getWidth() / mapData.getHeight()) * imageHeight;
                    if (byWidthArea > byHeightArea) {
                        imageHeight = imageWidth * mapData.getHeight() / mapData.getWidth();
                    } else {
                        imageWidth = imageHeight * mapData.getWidth() / mapData.getHeight();
                    }
                }
            }

            drawWidth = mapData.getWidth() * (int) Math.ceil((float) imageWidth / mapData.getWidth());
            drawHeight = mapData.getHeight() * (int) Math.ceil((float) imageHeight / mapData.getHeight());
        }

        int[] bandOffsets = new int[1];
        PixelInterleavedSampleModel sampleModel = new PixelInterleavedSampleModel(DataBuffer.TYPE_BYTE,
                drawWidth, drawHeight,
                1,
                drawWidth,
                bandOffsets);
        WritableRaster raster = Raster.createWritableRaster(sampleModel, null);
        BufferedImage bi = new BufferedImage(MapThumbnailGenerator.getColorModel(), raster, false, null);
        byte[] data = (byte[]) ((DataBufferByte) raster.getDataBuffer()).getData();

        drawMapFromMap(kwd, mapData, data, drawWidth / mapData.getWidth(), drawHeight / mapData.getHeight());

        if (drawWidth != imageWidth || drawHeight != imageHeight) {
            BufferedImage newImage = new BufferedImage(imageWidth, imageHeight, TYPE_INT_RGB);
            Graphics2D g = newImage.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.drawImage(bi, 0, 0, newImage.getWidth(), newImage.getHeight(), 0, 0, bi.getWidth(), bi.getHeight(), null);
            g.dispose();
            return newImage;
        }

        return bi;
    }

     /**
     * FIXME: This is a copy of the MapThumbnailGenerator.drawMap method, but it uses the runtime map data instead of the KWD map data. This is a temporary solution until we can figure out how to properly handle this.
     * 
     * Draws the map from the given data
     *
     * @param kwd      the KWD file
     * @param mapData  the map data
     * @param data     the data to draw
     * @param xScale   the x scale
     * @param yScale   the y scale
     */
    private static void drawMapFromMap(final KwdFile kwd, final IMapDataInformation<IMapTileInformation> mapData, byte[] data, int xScale, int yScale) {

        for (int y = 0; y < mapData.getHeight(); y++) {
            for (int x = 0; x < mapData.getWidth(); x++) {
                IMapTileInformation tile = mapData.getTile(x, y);
                byte value = 0;

                // Water and lava
                Terrain terrainTile = kwd.getTerrain(tile.getTerrainId());
                if (x == 0 || y == 0 || y == mapData.getHeight() - 1 || x == mapData.getWidth() - 1) {
                    value = 46; // Edge of maps
                } else if (kwd.getMap().getLava().getTerrainId() == tile.getTerrainId()) {
                    value = 10; // Lava
                } else if (kwd.getMap().getWater().getTerrainId() == tile.getTerrainId()) {
                    value = 8; // Water
                } // Other non-ownable tiles
                else if (terrainTile.getFlags().contains(Terrain.TerrainFlag.IMPENETRABLE)) {
                    if (terrainTile.getGoldValue() > 0) {
                        value = 4; // Gems
                    } else {
                        value = 2; // Impenetrable
                    }
                } else if (terrainTile.getGoldValue() > 0) {
                    value = 6; // Gold
                } else if (!terrainTile.getFlags().contains(Terrain.TerrainFlag.OWNABLE)) {
                    if (terrainTile.getFlags().contains(Terrain.TerrainFlag.SOLID)) {
                        value = 3; // Rock
                    } else {
                        value = 1; // Dirt path
                    }
                } // Owned tiles & buildings
                else if (terrainTile.getFlags().contains(Terrain.TerrainFlag.ROOM)) {
                    value = (byte) (35 + tile.getOwnerId()); // Building + owned color
                } else if (terrainTile.getFlags().contains(Terrain.TerrainFlag.SOLID)) {
                    value = (byte) (15 + tile.getOwnerId()); // Wall + owned color
                } else if (terrainTile.getFlags().contains(Terrain.TerrainFlag.OWNABLE)) {
                    value = (byte) (25 + tile.getOwnerId()); // Path + owned color
                } else {
                    logger.log(Level.WARNING, "Unknown runtime tile on {0} at tile {1}, {2}!", new Object[]{kwd, x, y});
                }

                for (int yScaling = 0; yScaling < yScale; yScaling++) {
                    for (int xScaling = 0; xScaling < xScale; xScaling++) {
                        data[(y * yScale + yScaling) * mapData.getWidth() * xScale + (x * xScale + xScaling)] = value;
                    }
                }
            }
        }
    }

    public static BufferedImage renderCreatureMarkers(final BufferedImage sourceImage, final List<CreatureMarker> markers, final int mapWidth, final int mapHeight, final int mapPixelWidth, final int mapPixelHeight) {
        if (sourceImage == null || markers == null || markers.isEmpty()) {
            return sourceImage;
        }

        BufferedImage rendered = new BufferedImage(sourceImage.getWidth(), sourceImage.getHeight(), TYPE_INT_ARGB);
        Graphics2D g = rendered.createGraphics();
        g.drawImage(sourceImage, 0, 0, null);

        final int markerMinSize = Math.max(1, Math.min(mapPixelWidth, mapPixelHeight) / 5);
        final int cellWidth = Math.max(1, sourceImage.getWidth() / Math.max(1, mapWidth));
        final int cellHeight = Math.max(1, sourceImage.getHeight() / Math.max(1, mapHeight));

        for (CreatureMarker marker : markers) {
            if (marker == null) {
                continue;
            }

            int centerX = Math.max(0, Math.min(sourceImage.getWidth() - 1, marker.getX() * cellWidth + cellWidth / 2));
            int centerY = Math.max(0, Math.min(sourceImage.getHeight() - 1, marker.getY() * cellHeight + cellHeight / 2));
            Color tint = MapThumbnailGenerator.getPlayerColor(marker.getPlayerId());
            if (tint == null) {
                tint = new Color(255, 255, 255, 255);
            }

            int cycle = Math.floorMod(marker.getBlinkPhase(), 8);
            boolean usePlayerColor = (cycle < 4);
            int alpha = usePlayerColor ? 220 : 180;
            int red = usePlayerColor ? tint.getRed() : 0;
            int green = usePlayerColor ? tint.getGreen() : 0;
            int blue = usePlayerColor ? tint.getBlue() : 0;
            Color markerColor = new Color(red, green, blue, alpha);
            g.setColor(markerColor);

            //offset because the position is one pixel further away on the map
            int halfSize = Math.max(1, markerMinSize / 2);
            for (int dx = -halfSize; dx <= halfSize; dx++) {
                for (int dy = -halfSize; dy <= halfSize; dy++) {
                    int px = centerX + dx;
                    int py = centerY + dy;
                    if (px >= 0 && px < sourceImage.getWidth() && py >= 0 && py < sourceImage.getHeight()) {
                        double distance = Math.sqrt(dx * dx + dy * dy);
                        if (distance <= halfSize + 0.2) {
                            rendered.setRGB(px, py, markerColor.getRGB());
                        }
                    }
                }
            }

            g.fillOval(centerX - 1, centerY - 1, 3, 3);
        }

        g.dispose();
        return rendered;
    }

    public static BufferedImage addCreatureMarkersToMap(final KwdFile kwd, final IMapDataInformation<IMapTileInformation> mapData, final BufferedImage mapImage, final java.util.Collection<CreatureMarker> markers) {
        if (mapImage == null || markers == null || markers.isEmpty()) {
            return mapImage;
        }

        List<CreatureMarker> visible = new ArrayList<>();
        for (CreatureMarker marker : markers) {
            if (marker != null) {
                visible.add(marker);
            }
        }

        if (visible.isEmpty()) {
            return mapImage;
        }

        return renderCreatureMarkers(mapImage, visible, mapData.getWidth(), mapData.getHeight(), mapImage.getWidth() / Math.max(1, mapData.getWidth()), mapImage.getHeight() / Math.max(1, mapData.getHeight()));
    }
}
