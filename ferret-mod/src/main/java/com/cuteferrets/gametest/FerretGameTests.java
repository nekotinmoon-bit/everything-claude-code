package com.cuteferrets.gametest;

import com.cuteferrets.CuteFerrets;
import com.cuteferrets.entity.FerretEntity;
import com.cuteferrets.entity.FerretMode;
import com.cuteferrets.menu.FerretMenu;
import com.cuteferrets.registry.ModEntities;
import com.cuteferrets.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.GameTestHolder;

/**
 * In-game tests, run with {@code ./gradlew runGameTestServer}. They only load when the
 * "cuteferrets" game-test namespace is enabled, so they never run in normal play.
 */
@GameTestHolder(CuteFerrets.MOD_ID)
public final class FerretGameTests {
    private static final String EMPTY = "forge:empty3x3x3";

    private FerretGameTests() {
    }

    private static void floor(GameTestHelper helper, net.minecraft.world.level.block.Block block) {
        for (int x = 0; x < 3; x++) {
            for (int z = 0; z < 3; z++) {
                helper.setBlock(x, 0, z, block);
            }
        }
    }

    /** The mock player spawns at the world origin; bring it into the test area so "follow" doesn't teleport away. */
    @SuppressWarnings("removal")
    private static ServerPlayer owner(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        net.minecraft.world.phys.Vec3 pos = helper.absoluteVec(new net.minecraft.world.phys.Vec3(0.5, 1.0, 0.5));
        player.teleportTo(pos.x, pos.y, pos.z);
        return player;
    }

    private static FerretEntity tamedFerret(GameTestHelper helper, ServerPlayer owner, FerretMode mode) {
        FerretEntity ferret = helper.spawn(ModEntities.FERRET.get(), new BlockPos(1, 1, 1));
        ferret.tame(owner);
        ferret.setMode(mode);
        // keep it in place so the test is deterministic
        ferret.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.0);
        return ferret;
    }

    @GameTest(template = EMPTY)
    public static void treatTamesAndMenuChangesMode(GameTestHelper helper) {
        floor(helper, Blocks.STONE);
        ServerPlayer player = owner(helper);
        FerretEntity ferret = helper.spawn(ModEntities.FERRET.get(), new BlockPos(1, 1, 1));
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.FERRET_TREAT.get()));
        ferret.mobInteract(player, InteractionHand.MAIN_HAND);
        helper.assertTrue(ferret.isTame(), "treat should tame the ferret");
        helper.assertTrue(ferret.isOwnedBy(player), "player should own the ferret");
        helper.assertTrue(ferret.getMode() == FerretMode.FOLLOW, "freshly tamed ferret follows");

        FerretMenu menu = new FerretMenu(1, player.getInventory(), ferret);
        menu.clickMenuButton(player, FerretMode.SIT.ordinal());
        helper.assertTrue(ferret.isOrderedToSit(), "sit button makes the ferret sit");
        menu.clickMenuButton(player, FerretMode.DIG.ordinal());
        helper.assertTrue(ferret.getMode() == FerretMode.DIG && !ferret.isOrderedToSit(), "dig button");

        int before = ferret.getAffection();
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        ferret.mobInteract(player, InteractionHand.MAIN_HAND);
        helper.assertTrue(ferret.getAffection() > before, "petting raises affection");

        ferret.getInventory().addItem(new ItemStack(Items.DIAMOND, 2));
        menu.clickMenuButton(player, FerretMenu.BUTTON_GIVE_LOOT);
        helper.assertTrue(ferret.getInventory().isEmpty(), "pouch emptied");
        helper.assertTrue(player.getInventory().countItem(Items.DIAMOND) == 2, "player got the loot");
        helper.succeed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 600)
    public static void digsUpTreasure(GameTestHelper helper) {
        floor(helper, Blocks.DIRT);
        ServerPlayer owner = owner(helper);
        FerretEntity ferret = tamedFerret(helper, owner, FerretMode.DIG);
        ferret.addAffection(FerretEntity.MAX_AFFECTION);
        helper.succeedWhen(() -> helper.assertTrue(!ferret.getInventory().isEmpty(), "ferret has not found anything yet"));
    }

    @GameTest(template = EMPTY, timeoutTicks = 400)
    public static void minesExposedOre(GameTestHelper helper) {
        floor(helper, Blocks.STONE);
        ServerPlayer owner = owner(helper);
        helper.setBlock(2, 1, 1, Blocks.IRON_ORE);
        FerretEntity ferret = tamedFerret(helper, owner, FerretMode.ORE);
        helper.succeedWhen(() -> {
            helper.assertBlockPresent(Blocks.AIR, 2, 1, 1);
            helper.assertTrue(ferret.getInventory().countItem(Items.RAW_IRON) > 0, "raw iron in pouch");
        });
    }

    @GameTest(template = EMPTY)
    public static void pouchSurvivesSaveAndLoad(GameTestHelper helper) {
        floor(helper, Blocks.STONE);
        ServerPlayer owner = owner(helper);
        FerretEntity ferret = tamedFerret(helper, owner, FerretMode.WANDER);
        ferret.getInventory().addItem(new ItemStack(Items.EMERALD, 3));
        ferret.addAffection(42);
        net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag();
        ferret.saveWithoutId(tag);
        FerretEntity copy = helper.spawn(ModEntities.FERRET.get(), new BlockPos(1, 1, 1));
        copy.load(tag);
        helper.assertTrue(copy.getInventory().countItem(Items.EMERALD) == 3, "pouch restored");
        helper.assertTrue(copy.getMode() == FerretMode.WANDER, "mode restored");
        helper.assertTrue(copy.getAffection() == ferret.getAffection(), "affection restored");
        helper.assertTrue(copy.getVariant() == ferret.getVariant(), "variant restored");
        helper.succeed();
    }
}
