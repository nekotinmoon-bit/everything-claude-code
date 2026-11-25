package com.snailmod.gametest;

import com.snailmod.SnailMod;
import com.snailmod.entity.SnailEntity;
import com.snailmod.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.GameTestHolder;

/** Run with {@code ./gradlew runGameTestServer}. */
@GameTestHolder(SnailMod.MODID)
public final class SnailGameTests {
    private static final String EMPTY = "forge:empty3x3x3";

    private static SnailEntity spawnSnail(GameTestHelper helper) {
        helper.setBlock(new BlockPos(1, 0, 1), Blocks.STONE);
        return helper.spawn(ModEntities.SNAIL.get(), new BlockPos(1, 1, 1));
    }

    @GameTest(template = EMPTY)
    public static void hidesAndTakesLessDamage(GameTestHelper helper) {
        SnailEntity snail = spawnSnail(helper);
        helper.assertFalse(snail.isHiding(), "Snail should start outside its shell");

        float start = snail.getHealth();
        snail.hurt(helper.getLevel().damageSources().generic(), 3.0F);
        float firstLoss = start - snail.getHealth();
        helper.assertTrue(snail.isHiding(), "Snail should hide after being hurt");

        snail.invulnerableTime = 0;
        float mid = snail.getHealth();
        snail.hurt(helper.getLevel().damageSources().generic(), 3.0F);
        float secondLoss = mid - snail.getHealth();

        helper.assertTrue(secondLoss > 0 && secondLoss < firstLoss * 0.5F,
                "Shell should cut damage: first=" + firstLoss + " second=" + secondLoss);
        helper.succeed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void comesOutAfterAWhile(GameTestHelper helper) {
        SnailEntity snail = spawnSnail(helper);
        snail.hurt(helper.getLevel().damageSources().generic(), 1.0F);
        helper.assertTrue(snail.isHiding(), "Snail should hide after being hurt");
        helper.runAfterDelay(110, () -> {
            helper.assertFalse(snail.isHiding(), "Snail should come out after 5 seconds");
            helper.succeed();
        });
    }

    @GameTest(template = EMPTY)
    public static void likesMushrooms(GameTestHelper helper) {
        SnailEntity snail = spawnSnail(helper);
        helper.assertTrue(snail.isFood(new ItemStack(Items.RED_MUSHROOM)), "Red mushroom should be food");
        helper.assertTrue(snail.isFood(new ItemStack(Items.BROWN_MUSHROOM)), "Brown mushroom should be food");
        helper.assertFalse(snail.isFood(new ItemStack(Items.WHEAT)), "Wheat should not be food");
        helper.succeed();
    }
}
