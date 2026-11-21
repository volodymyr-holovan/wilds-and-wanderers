package ua.wilds.core.colony;

import com.minecolonies.api.colony.IColonyManager;
import com.minecolonies.api.colony.IColonyView;
import com.minecolonies.api.colony.buildings.views.IBuildingView;
import com.minecolonies.api.colony.buildings.workerbuildings.ITownHallView;
import com.minecolonies.core.client.gui.map.WindowColonyMap;
import net.minecraft.client.Minecraft;
import ua.wilds.core.WildsCore;

/**
 * Client half of the colony test: the client self-test (CmdTest, "!ua.wilds.core.colony.ColonyClientTest#townHallMap")
 * opens the town hall's map window of the nearest colony, the way its "Map" button does.
 */
public final class ColonyClientTest {
    private ColonyClientTest() {}

    public static void townHallMap() {
        Minecraft mc = Minecraft.getInstance();
        IColonyView colony = IColonyManager.getInstance().getClosestColonyView(mc.level, mc.player.blockPosition());
        if (colony == null) {
            WildsCore.LOG.info("[colonytest] client: no colony view near the player");
            return;
        }
        for (IBuildingView b : colony.getClientBuildingManager().getBuildings().values()) {
            if (b instanceof ITownHallView hall) {
                WildsCore.LOG.info("[colonytest] client: town hall {} has {} map(s)", b.getPosition().toShortString(), hall.getMapDataList().size());
                new WindowColonyMap(true, hall).open();
                return;
            }
        }
        WildsCore.LOG.info("[colonytest] client: colony {} has no town hall view", colony.getID());
    }

    /** the skill tree, for screenshots: Pufferfish reads its key from input events, which the scripted key press skips */
    public static void skills() {
        net.puffish.skillsmod.client.SkillsClientMod.getInstance().openScreen(java.util.Optional.empty());
    }
}
