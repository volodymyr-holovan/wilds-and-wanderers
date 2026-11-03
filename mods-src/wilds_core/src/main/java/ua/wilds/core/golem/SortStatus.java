package ua.wilds.core.golem;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.ArrayList;
import java.util.List;

/**
 * What the copper golem does with a storage, set by the player (sneak + right click with an empty hand, round and
 * round) and kept in the storage's block entity, never in its name: the name the player gave stays theirs.
 * A storage placed is always {@link #NORMAL} (so breaking and placing again resets it); a chest placed next to one to
 * make a double chest takes the status of the one that stood there; both halves of a double chest always have the same.
 */
public enum SortStatus {
    /** the golem puts things here (where the same or alike things already are) */
    NORMAL("normal", "Звичайна"),
    /** the golem takes from here, and puts each stack where the very same item is */
    SOURCE("source", "Джерело: точно"),
    /** the golem takes from here, and puts each stack where the same or the most alike items are */
    SOURCE_LIKE("source_like", "Джерело: за схожістю"),
    /** what fits nowhere else */
    MISC("misc", "Різне"),
    /** the golem never touches it */
    IGNORE("ignore", "Не чіпати");

    static final String KEY = "wilds_sort";
    /** before statuses (03.11): copper chests were the sources, this flag their likeness mode */
    private static final String OLD_MODE = "wilds_sort_mode";

    public final String id, label;

    SortStatus(String id, String label) {
        this.id = id;
        this.label = label;
    }

    public boolean source() {
        return this == SOURCE || this == SOURCE_LIKE;
    }

    SortStatus next() {
        return values()[(ordinal() + 1) % values().length];
    }

    static SortStatus byId(String id) {
        for (SortStatus s : values()) if (s.id.equals(id)) return s;
        return NORMAL;
    }

    public static SortStatus of(ServerLevel level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be == null) return NORMAL;
        var tag = be.getPersistentData();
        if (tag.contains(KEY)) return byId(tag.getString(KEY));
        // a copper chest of the first version, placed before statuses: it stays the source it was
        if (BuiltInRegistries.BLOCK.getKey(be.getBlockState().getBlock()).getPath().endsWith("copper_chest"))
            return tag.getBoolean(OLD_MODE) ? SOURCE_LIKE : SOURCE;
        return NORMAL;
    }

    /** sets the status of the storage, both halves of a double chest */
    public static void set(ServerLevel level, BlockPos pos, SortStatus s) {
        for (BlockPos p : halves(level, pos)) {
            BlockEntity be = level.getBlockEntity(p);
            if (be == null) continue;
            be.getPersistentData().putString(KEY, s.id);
            be.getPersistentData().remove(OLD_MODE);
            be.setChanged();
        }
    }

    static List<BlockPos> halves(ServerLevel level, BlockPos pos) {
        List<BlockPos> out = new ArrayList<>(List.of(pos));
        BlockPos other = partner(level, pos);
        if (other != null) out.add(other);
        return out;
    }

    /**
     * the other half of a double chest, vanilla's or any mod's that uses the same "type" and "facing" properties
     * (Sophisticated Storage), or null
     */
    static BlockPos partner(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        ChestType type = null;
        Direction facing = null;
        for (Property<?> p : state.getProperties()) {
            Object v = state.getValue(p);
            if (p.getName().equals("type") && v instanceof ChestType t) type = t;
            else if (p.getName().equals("facing") && v instanceof Direction d && d.getAxis().isHorizontal()) facing = d;
        }
        if (type == null || facing == null || type == ChestType.SINGLE) return null;
        BlockPos other = pos.relative(type == ChestType.LEFT ? facing.getClockWise() : facing.getCounterClockWise());
        return level.getBlockState(other).getBlock() == state.getBlock() ? other : null;
    }
}
