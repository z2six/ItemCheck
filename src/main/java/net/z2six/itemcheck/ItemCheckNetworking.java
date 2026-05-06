package net.z2six.itemcheck;

import java.util.Comparator;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.z2six.itemcheck.network.SyncChecklistStatePayload;
import net.z2six.itemcheck.network.UpdateItemCheckedPayload;

public final class ItemCheckNetworking {
    private ItemCheckNetworking() {
    }

    public static void sendFullSync(ServerPlayer player) {
        ItemCheckSavedData savedData = ItemCheckSavedData.get(player.serverLevel());
        List<ResourceLocation> checkedItems = savedData.getCheckedItems().stream()
                .sorted(Comparator.comparing(ResourceLocation::toString))
                .toList();
        PacketDistributor.sendToPlayer(player, new SyncChecklistStatePayload(checkedItems, savedData.getAllTabViewState(), savedData.getFilterTabs()));
    }

    public static void sendFullSyncToAll(MinecraftServer server) {
        server.getPlayerList().getPlayers().forEach(ItemCheckNetworking::sendFullSync);
    }

    public static void broadcastUpdate(ResourceLocation itemId, boolean checked) {
        PacketDistributor.sendToAllPlayers(new UpdateItemCheckedPayload(itemId, checked));
    }
}
