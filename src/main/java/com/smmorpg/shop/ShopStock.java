package com.smmorpg.shop;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** Eight goods and what each of them costs, in emeralds. */
public record ShopStock(List<ItemStack> goods, List<Integer> prices) {

    public static final int SIZE = 8;

    public static final ShopStock EMPTY = new ShopStock(List.of(), List.of());

    public ItemStack good(int index) {
        return index >= 0 && index < goods.size() ? goods.get(index) : ItemStack.EMPTY;
    }

    public int price(int index) {
        return index >= 0 && index < prices.size() ? prices.get(index) : 0;
    }

    /** Removes one sold entry, leaving the gap on the shelf rather than shuffling it up. */
    public ShopStock sold(int index) {
        if (index < 0 || index >= goods.size()) return this;

        List<ItemStack> next = new ArrayList<>(goods);
        List<Integer> nextPrices = new ArrayList<>(prices);
        next.set(index, ItemStack.EMPTY);
        nextPrices.set(index, 0);
        return new ShopStock(List.copyOf(next), List.copyOf(nextPrices));
    }

    public void write(RegistryFriendlyByteBuf buf) {
        buf.writeVarInt(goods.size());
        for (int i = 0; i < goods.size(); i++) {
            ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, goods.get(i));
            buf.writeVarInt(price(i));
        }
    }

    public static ShopStock read(RegistryFriendlyByteBuf buf) {
        int count = buf.readVarInt();
        List<ItemStack> goods = new ArrayList<>(count);
        List<Integer> prices = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            goods.add(ItemStack.OPTIONAL_STREAM_CODEC.decode(buf));
            prices.add(buf.readVarInt());
        }
        return new ShopStock(List.copyOf(goods), List.copyOf(prices));
    }

    /** Kept out of the record's own equality: a codec pair is nicer than two calls. */
    public static final net.minecraft.network.codec.StreamCodec<RegistryFriendlyByteBuf, ShopStock>
            STREAM_CODEC = net.minecraft.network.codec.StreamCodec.of(
                    (buf, stock) -> stock.write(buf), ShopStock::read);
}
