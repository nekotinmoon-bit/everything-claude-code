package com.cuteferrets.registry;

import com.cuteferrets.CuteFerrets;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public final class ModTags {
    /** Blocks a ferret in "ore hunt" mode sniffs out and mines. */
    public static final TagKey<Block> FERRET_ORES = TagKey.create(Registries.BLOCK, CuteFerrets.id("ferret_ores"));
    /** Ores worth an extra-excited alert when a ferret smells them. */
    public static final TagKey<Block> PRECIOUS_ORES = TagKey.create(Registries.BLOCK, CuteFerrets.id("precious_ores"));
    /** Ground a ferret in "treasure" mode can dig in. */
    public static final TagKey<Block> FERRET_DIGGABLE = TagKey.create(Registries.BLOCK, CuteFerrets.id("ferret_diggable"));
    /** Food that tames, heals and breeds ferrets. */
    public static final TagKey<Item> FERRET_FOOD = TagKey.create(Registries.ITEM, CuteFerrets.id("ferret_food"));

    private ModTags() {
    }
}
