package de.hype.bingonet.fabric;

import net.minecraft.nbt.NbtCompound;

import java.util.Set;

public class NBTCompound implements de.hype.bingonet.client.common.mclibraries.interfaces.NBTCompound {
    private final NbtCompound mcCompound;
    public NBTCompound(NbtCompound mcCompound){
        this.mcCompound = mcCompound;
    }

    public NbtCompound getMcCompound() {
        return mcCompound;
    }

    @Override
    public Set<String> getKeys() {
        return mcCompound.getKeys();
    }

    @Override
    public Long getLong(String key) {
        return mcCompound.getLong(key).orElseGet(() -> null);
    }

    @Override
    public int getInt(String key) {
        return mcCompound.getInt(key).orElseGet(() -> null);
    }

    @Override
    public String get(String key) {
        return mcCompound.get(key).asString().orElseGet(() -> null);
    }
}
