package io.ynneh;

import com.google.common.collect.Lists;
import lombok.Data;
import net.runelite.api.coords.WorldPoint;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Ynneh | 08/03/2023 - 15:08
 * <https://github.com/drhenny>
 */
@Data
public class NPCSpawns {

    public String facing;
    public int radius;
    public int id;
    public List<WorldPoint> position = Lists.newArrayList();
    public String description;

}
