package io.ynneh;

import com.google.inject.Inject;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.PanelComponent;

import java.awt.*;

/**
 * @author Ynneh | 08/03/2023 - 15:11
 * <https://github.com/drhenny>
 */
public class NPCSpawnOverlay extends Overlay {

    private final NPCSpawnDumper plugin;
    private final PanelComponent panelComponent = new PanelComponent();

    @Inject
    public NPCSpawnOverlay(NPCSpawnDumper plugin) {
        this.plugin = plugin;
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        panelComponent.getChildren().clear();

        panelComponent.getChildren().add(LineComponent.builder()
                .left("Npc Spawns")
                .build());

        panelComponent.getChildren().add(LineComponent.builder()
                .left("Total Spawns")
                .right(String.valueOf(plugin.getSpawns().size()))
                .build());

        return panelComponent.render(graphics);
    }
}