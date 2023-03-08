package io.ynneh;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("example")
public interface NPCSpawnDumperConfig extends Config {

	@ConfigItem(keyName = "json", name = "Open .json File", description = "When enabling this setting, the plugin will open up the location of where the .json file saved to.")
	default boolean myCheckbox()
	{
		return false;
	}
}
