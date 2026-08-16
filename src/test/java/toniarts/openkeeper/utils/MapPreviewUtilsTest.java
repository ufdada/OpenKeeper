package toniarts.openkeeper.utils;

import java.awt.Rectangle;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class MapPreviewUtilsTest {

    @Test
    public void calculatesPreviewBoundsAroundHoveredTile() {
        Rectangle bounds = MapPreviewUtils.calculatePreviewBounds(100, 80, 42, 35, 20);

        assertEquals(new Rectangle(32, 25, 20, 20), bounds);
    }
}
