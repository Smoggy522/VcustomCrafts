package io.github.smoggy522.vcustomcrafts.item;

import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

public final class ItemCodec {
    private ItemCodec() {
    }

    public static String encode(ItemStack item) throws IOException {
        try (ByteArrayOutputStream bytes = new ByteArrayOutputStream();
             BukkitObjectOutputStream output = new BukkitObjectOutputStream(bytes)) {
            output.writeObject(item.clone());
            output.flush();
            return Base64.getEncoder().encodeToString(bytes.toByteArray());
        }
    }

    public static ItemStack decode(String encoded) throws IOException {
        try (ByteArrayInputStream bytes = new ByteArrayInputStream(Base64.getDecoder().decode(encoded));
             BukkitObjectInputStream input = new BukkitObjectInputStream(bytes)) {
            Object value = input.readObject();
            if (!(value instanceof ItemStack item)) {
                throw new IOException("Stored value is not an ItemStack");
            }
            return item;
        } catch (ClassNotFoundException | IllegalArgumentException exception) {
            throw new IOException("Invalid ItemStack snapshot", exception);
        }
    }
}

