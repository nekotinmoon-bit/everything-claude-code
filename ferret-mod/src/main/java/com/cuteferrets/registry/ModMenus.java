package com.cuteferrets.registry;

import com.cuteferrets.CuteFerrets;
import com.cuteferrets.menu.FerretMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, CuteFerrets.MOD_ID);

    public static final RegistryObject<MenuType<FerretMenu>> FERRET = MENUS.register("ferret",
            () -> IForgeMenuType.create(FerretMenu::new));

    private ModMenus() {
    }
}
