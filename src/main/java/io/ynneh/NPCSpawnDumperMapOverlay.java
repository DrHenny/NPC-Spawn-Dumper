package io.ynneh;

import com.google.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.Point;
import net.runelite.api.RenderOverview;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;

import java.awt.*;
import java.awt.geom.Rectangle2D;

/**
 * @author Ynneh | 17/03/2023 - 12:25
 * <https://github.com/drhenny>
 */
public class NPCSpawnDumperMapOverlay extends Overlay {

    private final NPCSpawnDumper plugin;
    private final NPCSpawnDumperConfig config;

    private static final int LABEL_PADDING = 4;
    private static final Color WHITE_TRANSLUCENT = new Color(255, 255, 255, 127);

    @Inject
    private final Client client;

    @Inject
    public NPCSpawnDumperMapOverlay(Client client, NPCSpawnDumper plugin, NPCSpawnDumperConfig config) {
        this.client = client;
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_WIDGETS);
        this.plugin = plugin;
        this.config = config;
    }

    @Override
    public Dimension render(Graphics2D graphics) {

        drawMapLines(graphics, 64, Color.GREEN);

            plugin.getSpawns().keySet().forEach(v -> {

                Color color = Color.CYAN;

                int index = v.intValue();

                NPC npc = plugin.getForIndex(index);

                if (npc != null) {
                    Point minimapLocation = npc.getMinimapLocation();
                    if (minimapLocation != null) {
                        OverlayUtil.renderMinimapLocation(graphics, minimapLocation, color.darker());
                    }
                }
            });

        return null;
    }

    private void drawMapLines(Graphics2D graphics, int gridSize, Color gridColour) {
        final int gridTruncate = ~(gridSize - 1);

        RenderOverview ro = client.getRenderOverview();
        Widget map = client.getWidget(WidgetInfo.WORLD_MAP_VIEW);
        Float pixelsPerTile = ro.getWorldMapZoom();

        if (map == null) {
            return;
        }

        if (gridSize * pixelsPerTile < 3) {
            return;
        }

        Rectangle worldMapRect = map.getBounds();
        graphics.setClip(worldMapRect);

        int widthInTiles = (int) Math.ceil(worldMapRect.getWidth() / pixelsPerTile);
        int heightInTiles = (int) Math.ceil(worldMapRect.getHeight() / pixelsPerTile);

        Point worldMapPosition = ro.getWorldMapPosition();

        // Offset in tiles from anchor sides
        int yTileMin = worldMapPosition.getY() - heightInTiles / 2;
        int xRegionMin = (worldMapPosition.getX() - widthInTiles / 2) & gridTruncate;
        int xRegionMax = ((worldMapPosition.getX() + widthInTiles / 2) & gridTruncate) + gridSize;
        int yRegionMin = (yTileMin & gridTruncate);
        int yRegionMax = ((worldMapPosition.getY() + heightInTiles / 2) & gridTruncate) + gridSize;
        int regionPixelSize = (int) Math.ceil(gridSize * pixelsPerTile);

        for (int x = xRegionMin; x < xRegionMax; x += gridSize) {
            for (int y = yRegionMin; y < yRegionMax; y += gridSize) {
                int yTileOffset = -(yTileMin - y);
                int xTileOffset = x + widthInTiles / 2 - worldMapPosition.getX();

                int xPos = ((int) (xTileOffset * pixelsPerTile)) + (int) worldMapRect.getX();
                int yPos = (worldMapRect.height - (int) (yTileOffset * pixelsPerTile)) + (int) worldMapRect.getY();
                // Offset y-position by a single region to correct for drawRect starting from the top
                yPos -= regionPixelSize;

                graphics.setColor(gridColour);

                graphics.drawRect(xPos, yPos, regionPixelSize, regionPixelSize);

                graphics.setColor(WHITE_TRANSLUCENT);

                if (gridSize == 64) {
                    int regionId = ((x >> 6) << 8) | (y >> 6);
                    String regionText = String.valueOf(regionId);
                    FontMetrics fm = graphics.getFontMetrics();
                    Rectangle2D textBounds = fm.getStringBounds(regionText, graphics);
                    int labelWidth = (int) textBounds.getWidth() + 2 * LABEL_PADDING;
                    int labelHeight = (int) textBounds.getHeight() + 2 * LABEL_PADDING;
                    graphics.fillRect(xPos, yPos, labelWidth, labelHeight);
                    graphics.setColor(Color.BLACK);
                    graphics.drawString(
                            regionText,
                            xPos + LABEL_PADDING,
                            yPos + (int) textBounds.getHeight() + LABEL_PADDING);
                }
            }
        }
    }
}
