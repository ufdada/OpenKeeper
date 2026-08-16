package toniarts.openkeeper.utils;

import java.awt.Rectangle;

/**
 * Helpers used for the fullscreen map preview interaction.
 */
public final class MapPreviewUtils {

    private MapPreviewUtils() {
    }

    public static Rectangle calculatePreviewBounds(int mapWidth, int mapHeight, int hoveredX, int hoveredY, int size) {
        int halfSize = Math.max(1, size / 2);
        int x = Math.max(0, Math.min(mapWidth - size, hoveredX - halfSize));
        int y = Math.max(0, Math.min(mapHeight - size, hoveredY - halfSize));
        return new Rectangle(x, y, size, size);
    }
}
