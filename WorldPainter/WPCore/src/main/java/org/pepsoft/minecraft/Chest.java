/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.minecraft;

import org.jnbt.CompoundTag;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.pepsoft.minecraft.Constants.LEGACY_ID_CHEST;
import static org.pepsoft.minecraft.Constants.TAG_ITEMS;

/**
 *
 * @author pepijn
 */
public class Chest extends TileEntity implements ItemContainer {
    public Chest() {
        super(LEGACY_ID_CHEST); // TODO add MC 1.14 support
    }

    public Chest(CompoundTag tag) {
        super(tag);
    }

    @Override
    public List<InventoryItem> getItems() {
        List<CompoundTag> list = getList(TAG_ITEMS);
        if (list != null) {
            List<InventoryItem> items = new ArrayList<>(list.size());
            items.addAll(list.stream().map(InventoryItem::new).collect(Collectors.toList()));
            return items;
        } else {
            return new ArrayList<>();
        }
    }

    public void setItems(List<InventoryItem> items) {
        List<CompoundTag> list = new ArrayList<>(items.size());
        list.addAll(items.stream().map(InventoryItem::toNBT).collect(Collectors.toList()));
        setList(TAG_ITEMS, CompoundTag.class, list);
    }

    private static final long serialVersionUID = 1L;
}