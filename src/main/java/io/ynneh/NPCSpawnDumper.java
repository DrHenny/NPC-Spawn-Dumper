package io.ynneh;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
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
import net.runelite.api.events.CommandExecuted;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
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
import java.util.List;
import java.util.Map;

import static net.runelite.client.RuneLite.RUNELITE_DIR;

@PluginDescriptor(
        name = "NPC Spawn Dumper"
)
@Singleton
@Slf4j
public class NPCSpawnDumper extends Plugin {

    public static final File SAVE_DIRECTORY = new File(RUNELITE_DIR, "dumped-spawns");

    private int currentRegionId;

    @Getter(AccessLevel.PACKAGE)
    private Map<Integer, NPCSpawns> spawns;

    @Inject
    @Getter(AccessLevel.PACKAGE)
    private Client client;

    @Getter(AccessLevel.PACKAGE)
    private Map<Integer, Map<Integer, NPCSpawns>> regions_dumped;

    @Inject
    private OverlayManager overlayManager;

    @Inject
    private NPCSpawnDumperMapOverlay minimapOverlay;

    @Override
    protected void startUp() throws Exception {
        if (!SAVE_DIRECTORY.exists())
            SAVE_DIRECTORY.mkdirs();//creates directory if it doesn't exist.
        spawns = Maps.newConcurrentMap();//new map for spawns.
        regions_dumped = Maps.newConcurrentMap();//new map for regions dumped
        overlayManager.add(minimapOverlay);
        log.debug("Started dumping NPC spawns!");
    }

    /** Grabs cached npc based on index **/
    public NPC getForIndex(int index) {
        return client.getCachedNPCs()[index];
    }

    @Subscribe
    public void onGameTick(GameTick event) {
        save_region();
    }

    /**
     * Cycles every game tick to ensure it updates the region id correctly
     */
    private void save_region() {
        int regionId = client.getLocalPlayer().getWorldLocation().getRegionID();
        if (regionId != currentRegionId) {
            regions_dumped.put(currentRegionId, spawns);//adds old region to prevent duplicates
            currentRegionId = regionId;
            spawns.clear();
            client.addChatMessage(ChatMessageType.BROADCAST, "Ynneh", regionSaveExists(regionId) ? "Region has already been dumped.. skipping id=" + regionId : "Clearing NPC list.. and setting new RegionId=" + regionId, null);
        }
    }

    @Subscribe
    public void onCommandExecuted(CommandExecuted command) {
        switch (command.getCommand()) {
            case "d": {
                int currentRegion = currentRegionId;
                regions_dumped.remove(currentRegion);
                spawns.clear();
                client.getNpcs().stream().forEach(n -> dump(n));
                client.addChatMessage(ChatMessageType.GAMEMESSAGE, "Ynneh", "Region " + currentRegion + " has been reset and has began to dump again.", null);
                return;
            }

            case "check": {
                int size = getLocalNPCSize();//spawns within render? i assume
                int saved = spawns.size();//spawned saved upon npcspawned
                client.addChatMessage(ChatMessageType.GAMEMESSAGE, "Ynneh", "Region " + currentRegionId + " spawn stats = " + saved + "/ (Predicted) " + size, null);
                return;
            }

            case "r": {
                client.addChatMessage(ChatMessageType.GAMEMESSAGE, "Ynneh", "Total Regions Saved: " + regions_dumped.size(), null);
                return;
            }
            default:
                break;
        }
    }

    private int getLocalNPCSize() {
        return client.getNpcs().size();
    }

    /**
     * Event for when npc isViewable usually 15-20 tiles
     *
     * @param event
     */
    @Subscribe
    public void onNpcSpawned(NpcSpawned event) {
        dump(event.getNpc());
    }

    private void dump(NPC npc) {
        /** NPC Definitions **/
        NPCComposition def = client.getNpcDefinition(npc.getId());

        /** Region ID of the NPC **/
        int regionId = npc.getWorldLocation().getRegionID();

        /** To prevent duplicate NPC spawns in different files **/
        if (regionId != currentRegionId)
            return;

        Map<Integer, NPCSpawns> data = regions_dumped.get(regionId);
        if (data != null) {
            /**
             * Region already been saved.
             */
            return;
        }

        int index = npc.getIndex();

        /** Checks if the saved list contains the same NPC by Index **/
        NPCSpawns saved_spawn = spawns.get(index);

        boolean pet = def.isFollower();

        if (saved_spawn != null && saved_spawn.id == npc.getId() || pet || npc.getName().contains("impling")) {
            /**
             * If the NPC Index already exists then return OR is a follower / impling
             */
            System.out.println("spawn blocked.. " + (pet ? "npc is a follower." : "already added?.. " + npc.getId() + " " + index));
            return;
        }

        NPCSpawns spawn = new NPCSpawns();
        spawn.setId(npc.getId());
        spawn.setFacing(Direction.toString(npc.getOrientation()));
        spawn.getPosition().add(npc.getWorldLocation());
        spawn.setDescription(npc.getName());
        spawns.put(index, spawn);
        try {
            File spawnsPath = new File(SAVE_DIRECTORY.getPath() + "/" + regionId + ".json");
            Files.write(spawnsPath.toPath(), new GsonBuilder().setPrettyPrinting().create().toJson(spawns.values()).getBytes());
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        System.err.println("Added new NPC spawn: Index=" + npc.getIndex() + ", Id=" + npc.getId() + ", Name=" + def.getName() + " RegionId=" + regionId + " CB=" + def.getCombatLevel() + " ListSize=" + spawns.size());

    }

    /**
     * IF region save already exists in map.
     *
     * @param regionId
     * @return
     */
    private boolean regionSaveExists(int regionId) {
        Map<Integer, NPCSpawns> data = regions_dumped.get(regionId);
        return data != null;
    }

    @Override
    protected void shutDown() throws Exception {
        overlayManager.remove(minimapOverlay);
        log.debug("Stopped dumping NPC spawns!");
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged state) {
        switch (state.getGameState()) {
            /** Sets current region on login to start dumping. **/
            case LOGGED_IN:
                currentRegionId = client.getLocalPlayer().getWorldLocation().getRegionID();
                save_region();
                break;
        }
    }

    @Provides
    NPCSpawnDumperConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(NPCSpawnDumperConfig.class);
    }
}
