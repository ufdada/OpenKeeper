package toniarts.openkeeper.utils;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;
import toniarts.openkeeper.game.controller.map.MinimapController;

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
}
