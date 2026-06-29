package foundry.imguitest.mixin;

import foundry.imguitest.screen.TestScreen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin extends Screen {

    private TitleScreenMixin(final Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("RETURN"))
    public void init(final CallbackInfo ci) {
        this.addRenderableWidget(Button.builder(Component.literal("ImGui Demo"), button -> {
            this.minecraft.setScreenAndShow(new TestScreen());
        }).bounds(this.width - 75 - 3, 3, 70, 20).build());
    }
}
