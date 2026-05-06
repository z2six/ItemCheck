package net.z2six.itemcheck;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.z2six.itemcheck.network.RequestChecklistStatePayload;
import net.z2six.itemcheck.network.SaveFilterTabsPayload;
import net.z2six.itemcheck.network.SetItemCheckedPayload;
import net.z2six.itemcheck.network.SyncChecklistStatePayload;
import net.z2six.itemcheck.network.UpdateItemCheckedPayload;

@Mod(Itemcheck.MODID)
public final class Itemcheck {
    public static final String MODID = "itemcheck";
    private static final String NETWORK_VERSION = "1";

    public Itemcheck(IEventBus modEventBus) {
        modEventBus.addListener(Itemcheck::registerPayloadHandlers);
    }

    private static void registerPayloadHandlers(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(NETWORK_VERSION);
        registrar.playToServer(RequestChecklistStatePayload.TYPE, RequestChecklistStatePayload.STREAM_CODEC, RequestChecklistStatePayload::handle);
        registrar.playToServer(SaveFilterTabsPayload.TYPE, SaveFilterTabsPayload.STREAM_CODEC, SaveFilterTabsPayload::handle);
        registrar.playToServer(SetItemCheckedPayload.TYPE, SetItemCheckedPayload.STREAM_CODEC, SetItemCheckedPayload::handle);
        registrar.playToClient(SyncChecklistStatePayload.TYPE, SyncChecklistStatePayload.STREAM_CODEC, SyncChecklistStatePayload::handle);
        registrar.playToClient(UpdateItemCheckedPayload.TYPE, UpdateItemCheckedPayload.STREAM_CODEC, UpdateItemCheckedPayload::handle);
    }

    @EventBusSubscriber(modid = MODID)
    public static final class GameEvents {
        private GameEvents() {
        }

        @SubscribeEvent
        public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
            if (event.getEntity() instanceof ServerPlayer player) {
                ItemCheckNetworking.sendFullSync(player);
            }
        }
    }
}
