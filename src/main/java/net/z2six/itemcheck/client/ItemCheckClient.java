package net.z2six.itemcheck.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.z2six.itemcheck.Itemcheck;
import org.lwjgl.glfw.GLFW;

public final class ItemCheckClient {
    private static final KeyMapping OPEN_CHECKLIST = new KeyMapping(
            "key.itemcheck.open",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_O,
            "key.categories.itemcheck"
    );

    private ItemCheckClient() {
    }

    private static void openChecklistScreen() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }

        ChecklistClientState.requestSync();
        minecraft.setScreen(new ItemChecklistScreen());
    }

    @EventBusSubscriber(modid = Itemcheck.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class ModBusEvents {
        private ModBusEvents() {
        }

        @SubscribeEvent
        public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
            ChecklistClientState.installBridge();
            event.register(OPEN_CHECKLIST);
        }
    }

    @EventBusSubscriber(modid = Itemcheck.MODID, value = Dist.CLIENT)
    public static final class GameBusEvents {
        private GameBusEvents() {
        }

        @SubscribeEvent
        public static void onClientTick(ClientTickEvent.Post event) {
            while (OPEN_CHECKLIST.consumeClick()) {
                openChecklistScreen();
            }
        }

        @SubscribeEvent
        public static void onPlayerLogin(ClientPlayerNetworkEvent.LoggingIn event) {
            ChecklistClientState.reset();
            ChecklistClientState.requestSync();
        }

        @SubscribeEvent
        public static void onPlayerLogout(ClientPlayerNetworkEvent.LoggingOut event) {
            ChecklistClientState.reset();
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.screen instanceof ItemChecklistScreen) {
                minecraft.setScreen(null);
            }
        }
    }
}
