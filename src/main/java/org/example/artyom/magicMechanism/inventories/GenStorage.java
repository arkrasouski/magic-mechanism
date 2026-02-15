package org.example.artyom.magicMechanism.inventories;

import org.bukkit.block.TileState;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.example.artyom.magicMechanism.utils.ItemStackArrayCodec;

import java.io.IOException;

import static org.example.artyom.magicMechanism.data.Keys.KEY_ITEMS;

public final class GenStorage {




    public static void loadItems(TileState tile, Inventory inv) {
        String encoded = tile.getPersistentDataContainer().get(KEY_ITEMS, PersistentDataType.STRING);
        if (encoded == null || encoded.isBlank()) return;

        try {
            var items = ItemStackArrayCodec.fromBase64(encoded);
            // подгоняем под размер GUI
            ItemStack[] trimmed = new ItemStack[inv.getSize()];
            System.arraycopy(items, 0, trimmed, 0, Math.min(items.length, trimmed.length));
            inv.setContents(trimmed);
        } catch (IOException ex) {
            // если данные битые — можно очистить, чтобы не падало постоянно
            tile.getPersistentDataContainer().remove(KEY_ITEMS);
            tile.update(); // применить remove [web:69]
        }

    }

    public static void saveItems(TileState tile, Inventory inv) {
        String encoded = ItemStackArrayCodec.toBase64(inv.getContents());
        tile.getPersistentDataContainer()
                .set(KEY_ITEMS, PersistentDataType.STRING, encoded); // [web:34]

        // важно: применить изменения TileState к реальному блоку
        tile.update(); // без этого PDC останется только в snapshot [web:33]
    }
}
