package toniarts.openkeeper.utils;

import com.simsilica.es.EntityId;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import static org.junit.Assert.*;
import toniarts.openkeeper.game.controller.map.MinimapController;
import toniarts.openkeeper.game.map.IMapDataInformation;
import toniarts.openkeeper.game.map.IMapTileInformation;
import toniarts.openkeeper.tools.convert.map.Tile;
import toniarts.openkeeper.utils.Point;

public class MapThumbnailGeneratorTest {

    @Test
    public void shouldOverlayCreatureMarkersWithPlayerColors() {
        BufferedImage image = new BufferedImage(8, 8, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(new Color(0, 0, 0, 0));
        graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
        graphics.dispose();

        List<MinimapController.CreatureMarker> markers = Arrays.asList(
                new MinimapController.CreatureMarker(1, 2, (short) 3, 1),
                new MinimapController.CreatureMarker(2, 4, (short) 5, 2)
        );

        BufferedImage result = MinimapController.renderCreatureMarkers(image, markers, 8, 8, 2, 2);

        assertNotNull(result);
        assertEquals(8, result.getWidth());
        assertEquals(8, result.getHeight());
        assertNotEquals(new Color(0, 0, 0, 0).getRGB(), result.getRGB(2, 2));
        assertNotEquals(new Color(0, 0, 0, 0).getRGB(), result.getRGB(4, 5));
    }

    @Test
    public void shouldHighlightSelectedTilesInTurquoise() {
        BufferedImage image = new BufferedImage(4, 4, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(new Color(0, 0, 0, 0));
        graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
        graphics.dispose();

        IMapDataInformation<IMapTileInformation> mapData = createMapData(2, 2, new Point(0, 0), new Point(1, 1));

        BufferedImage result = MinimapController.renderSelectedTiles(image, mapData, (short) 3);

        assertEquals(new Color(0, 255, 255, 255).getRGB(), result.getRGB(0, 0));
        assertEquals(new Color(0, 0, 0, 0).getRGB(), result.getRGB(2, 2));
    }

    private static IMapDataInformation<IMapTileInformation> createMapData(int width, int height, Point... selectedTiles) {
        Map<Point, IMapTileInformation> tiles = new HashMap<>();
        for (Point point : selectedTiles) {
            tiles.put(point, new TestMapTile(point, true));
        }

        return new IMapDataInformation<IMapTileInformation>() {
            @Override
            public int getHeight() {
                return height;
            }

            @Override
            public IMapTileInformation getTile(int x, int y) {
                Point point = new Point(x, y);
                if (tiles.containsKey(point)) {
                    return tiles.get(point);
                }
                return new TestMapTile(point, false);
            }

            @Override
            public int getWidth() {
                return width;
            }

            @Override
            public void setTiles(List<IMapTileInformation> mapTiles) {
                // Not used in this test
            }
        };
    }

    private static final class TestMapTile implements IMapTileInformation {
        private final Point location;
        private final boolean selected;

        private TestMapTile(Point location, boolean selected) {
            this.location = location;
            this.selected = selected;
        }

        @Override
        public EntityId getEntityId() {
            return null;
        }

        @Override
        public Tile.BridgeTerrainType getBridgeTerrainType() {
            return null;
        }

        @Override
        public int getGold() {
            return 0;
        }

        @Override
        public int getHealth() {
            return 0;
        }

        @Override
        public Integer getHealthPercent() {
            return 0;
        }

        @Override
        public int getIndex() {
            return 0;
        }

        @Override
        public Point getLocation() {
            return location;
        }

        @Override
        public int getManaGain() {
            return 0;
        }

        @Override
        public int getMaxHealth() {
            return 0;
        }

        @Override
        public short getOwnerId() {
            return 0;
        }

        @Override
        public int getRandomTextureIndex() {
            return 0;
        }

        @Override
        public short getTerrainId() {
            return 0;
        }

        @Override
        public int getX() {
            return location.x;
        }

        @Override
        public int getY() {
            return location.y;
        }

        @Override
        public boolean isAtFullHealth() {
            return true;
        }

        @Override
        public boolean isFlashed(short playerId) {
            return false;
        }

        @Override
        public boolean isSelected(short playerId) {
            return selected;
        }

        @Override
        public EntityId getRoomId() {
            return null;
        }
    }
}
