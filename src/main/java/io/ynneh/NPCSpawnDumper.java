package io.ynneh;

import com.google.gson.GsonBuilder;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.NPCComposition;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.NpcSpawned;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static net.runelite.client.RuneLite.RUNELITE_DIR;

@PluginDescriptor(
        name = "NPC Spawn Dumper"
)
@Singleton
@Slf4j
public class NPCSpawnDumper extends Plugin {

    public static final File SAVE_DIRECTORY = new File(RUNELITE_DIR, "dumped-spawns");

    @Getter(AccessLevel.PACKAGE)
    private Map<Integer, NPCSpawns> spawns = new HashMap<>();

    @Inject
    private Client client;

    @Override
    protected void startUp() throws Exception {
        if (!SAVE_DIRECTORY.exists())
            SAVE_DIRECTORY.mkdirs();
        log.debug("Started dumping NPC spawns!");
    }

    private int checkBoundsForNPC(String name, boolean attackable, int size) {
        if (!attackable) {
            if (Objects.equals("Banker", name))
                return 0;
            return 5;//apparently 5 is default
        }
        return 5 + size;//not accurate, place holder for now.
    }

    @Subscribe
    public void onNpcSpawned(NpcSpawned event) {
        NPC npc = event.getNpc();

        /** NPC Definitions **/
        NPCComposition def = client.getNpcDefinition(npc.getId());

        /** Checks if the saved list contains the same NPC by Index **/
        NPCSpawns saved_spawn = spawns.get(npc.getIndex());

        boolean pet = def.isFollower();

        if (saved_spawn != null && saved_spawn.id == npc.getId() || pet) {
            /**
             * If the NPC Index already exists then return OR is a follower
             */
            log.debug("spawn blocked.. "+(pet ? "npc is a follower." : "already added?.. " + npc.getId() + " " + npc.getIndex()));
            return;
        }

        int regionId = event.getNpc().getWorldLocation().getRegionID();
        NPCSpawns spawn = new NPCSpawns();
        spawn.setId(npc.getId());
        spawn.setFacing(Direction.toString(npc.getOrientation()));
        spawn.setRadius(checkBoundsForNPC(npc.getName(), def.getCombatLevel() == 0, def.getSize()));
        spawn.getPosition().add(npc.getWorldLocation());
        spawn.setDescription(npc.getName()+" - #NPC-SPAWN-DUMPER Plugin");
        try {
            File spawnsPath = new File(SAVE_DIRECTORY.getPath() + "/"+regionId+".json");
            Files.write(spawnsPath.toPath(), new GsonBuilder().setPrettyPrinting().create().toJson(spawns.values()).getBytes());
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        spawns.put(npc.getIndex(), spawn);
        System.err.println("Added new NPC to spawns: index=" + npc.getIndex() + ", id=" + npc.getId() + ", name=" + def.getName() + " regionId=" + regionId+" cb="+def.getCombatLevel());

    }

    @Override
    protected void shutDown() throws Exception {
        log.debug("Stopped dumping NPC spawns!");
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged state) {
        switch (state.getGameState()) {
            case LOGGED_IN:
                client.addChatMessage(ChatMessageType.GAMEMESSAGE, "Ynneh", "Logging in..", null);
                break;
        }
    }

    @Provides
    NPCSpawnDumperConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(NPCSpawnDumperConfig.class);
    }
}
