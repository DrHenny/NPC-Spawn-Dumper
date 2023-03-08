package io.ynneh;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class NPCSpawnDumperTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(NPCSpawnDumper.class);
		RuneLite.main(args);
	}
}