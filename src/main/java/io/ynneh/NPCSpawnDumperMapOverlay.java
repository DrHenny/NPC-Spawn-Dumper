package io.ynneh;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import javax.inject.Inject;
import lombok.AccessLevel;
import lombok.Setter;
import net.runelite.api.GraphicID;
import net.runelite.api.NPC;
import net.runelite.api.NpcID;
import net.runelite.api.Point;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.FishingSpot;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;
/**
 * @author Ynneh | 17/03/2023 - 12:25
 * <https://github.com/drhenny>
 */
public class NPCSpawnDumperMapOverlay extends Overlay {

    private final NPCSpawnDumper plugin;
    private final NPCSpawnDumperConfig config;

    @Inject
    public NPCSpawnDumperMapOverlay(NPCSpawnDumper plugin, NPCSpawnDumperConfig config) {
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_WIDGETS);
        this.plugin = plugin;
        this.config = config;
    }

    @Override
    public Dimension render(Graphics2D graphics) {


        for (NPC npc : plugin.getNpcList()) {

            if (npc == null) {
                continue;
            }

            Color color = Color.CYAN;

            Point minimapLocation = npc.getMinimapLocation();

            if (minimapLocation != null) {
                OverlayUtil.renderMinimapLocation(graphics, minimapLocation, color.darker());
            }
        }
        return null;
    }
}
