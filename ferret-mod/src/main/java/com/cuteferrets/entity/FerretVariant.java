package com.cuteferrets.entity;

import com.cuteferrets.CuteFerrets;
import net.minecraft.resources.ResourceLocation;

/**
 * Fur colours. Picked at random on spawn; babies inherit one of their parents' colours.
 */
public enum FerretVariant {
    SABLE("sable"),
    ALBINO("albino"),
    CHAMPAGNE("champagne"),
    BLACK("black"),
    PANDA("panda");

    private final String name;
    private final ResourceLocation texture;

    FerretVariant(String name) {
        this.name = name;
        this.texture = CuteFerrets.id("textures/entity/ferret/" + name + ".png");
    }

    public String getSerializedName() {
        return name;
    }

    public ResourceLocation texture() {
        return texture;
    }

    public static FerretVariant byId(int id) {
        FerretVariant[] values = values();
        return id >= 0 && id < values.length ? values[id] : SABLE;
    }
}
