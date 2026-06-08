package foundry.imgui.neoforge.impl;

import foundry.imgui.api.ImGuiMC;
import foundry.imgui.impl.ImGuiMCImpl;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(value = ImGuiMC.MOD_ID, dist = Dist.CLIENT)
public class ImGuiMCNeoforge {

    public ImGuiMCNeoforge(final IEventBus bus) {
        ImGuiMCImpl.init();


        //? if >= 1.21.5 {
        /*final net.minecraft.resources.ResourceLocation id = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(ImGuiMC.MOD_ID, "font_manager");
        bus.<net.neoforged.neoforge.client.event.AddClientReloadListenersEvent>addListener(event -> {
            ImGuiMCImpl.initHandler();
            if (ImGuiMCImpl.handler == null) {
                return;
            }

            event.addListener(id, ImGuiMCImpl.handler.getFontManager());
        });
        *///?} else {
        bus.<net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent>addListener(event -> {
            ImGuiMCImpl.initHandler();
            if (ImGuiMCImpl.handler == null) {
                return;
            }

            event.registerReloadListener(ImGuiMCImpl.handler.getFontManager());
        });
        //?}
    }
}
