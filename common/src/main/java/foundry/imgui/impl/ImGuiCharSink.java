package foundry.imgui.impl;

import foundry.imgui.api.ImGuiMC;
import imgui.ImFont;
import imgui.ImGui;
import imgui.flag.ImGuiCol;
import imgui.flag.ImGuiMouseCursor;
import imgui.flag.ImGuiStyleVar;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSink;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import java.net.URI;
import java.util.List;

@ApiStatus.Internal
public class ImGuiCharSink implements FormattedCharSink {

    private ImFont font;
    private int textColor;
    private HoverEvent hoverEvent;
    private ClickEvent clickEvent;

    private final StringBuilder buffer;

    public ImGuiCharSink() {
        this.buffer = new StringBuilder();
    }

    private static void openUri(final URI uri) {
        //? if >=1.21.11 {
        /*net.minecraft.util.Util.getPlatform().openUri(uri);
         *///? } else {
        net.minecraft.Util.getPlatform().openUri(uri);
        //? }
    }

    public void setup() {
        this.font = ImGui.getFont();
        this.textColor = ImGuiMC.getColor(ImGuiCol.Text);
    }

    public void reset() {
        this.font = null;
        this.textColor = 0;
        this.buffer.setLength(0);
        this.hoverEvent = null;
        this.clickEvent = null;
    }

    @Override
    public boolean accept(final int positionInCurrentSequence, final @NotNull Style style, final int codePoint) {
        final ImFont font = ImGuiMC.getStyleFont(style);
        final int styleColor = style.getColor() != null ? style.getColor().getValue() : this.textColor;
        if (font != this.font || styleColor != this.textColor || style.getHoverEvent() != this.hoverEvent || style.getClickEvent() != this.clickEvent) {
            if (!this.buffer.isEmpty()) {
                this.finish();
            }
            this.font = ImGuiMC.getStyleFont(style);
            this.textColor = styleColor;
            this.hoverEvent = style.getHoverEvent();
            this.clickEvent = style.getClickEvent();
        }
        this.buffer.appendCodePoint(codePoint);
        return true;
    }

    public void finish() {
        if (!this.buffer.isEmpty()) {
            ImGui.pushStyleVar(ImGuiStyleVar.ItemSpacing, 0, 0);
            ImGui.pushFont(this.font, 0.0F);
            ImGui.textColored(0xFF000000 | (this.textColor & 0xFF0000) >> 16 | (this.textColor & 0xFF00) | (this.textColor & 0xFF) << 16, this.buffer.toString());
            ImGui.popStyleVar();
            this.buffer.setLength(0);

            if (ImGui.isItemClicked() && this.clickEvent != null) {
                this.handleClick();
            }
            if (ImGui.isItemHovered() && this.hoverEvent != null) {
                if (this.clickEvent != null) {
                    ImGui.setMouseCursor(ImGuiMouseCursor.Hand);
                }
                this.handleHover();
            }

            ImGui.sameLine();
            ImGui.popFont();
        }
    }

    //? if >=1.21.5 {
    /*private void handleClick() {
        final Minecraft minecraft = Minecraft.getInstance();
        switch (this.clickEvent) {
            case ClickEvent.OpenUrl(final URI uri1): {
                if (!minecraft.options.chatLinks().get()) {
                    return;
                }

                if (minecraft.options.chatLinksPrompt().get()) {
                    //? if >=26.2-pre-2 {
                    /^final Screen oldScreen = minecraft.gui.screen();
                    minecraft.gui.setScreen(new net.minecraft.client.gui.screens.ConfirmLinkScreen((confirm) -> {
                        if (confirm) {
                            openUri(uri1);
                        }

                        minecraft.gui.setScreen(oldScreen);
                    }, uri1.toString(), false));
                    ^///? } else {
                    final Screen oldScreen = minecraft.screen;
                    minecraft.setScreen(new net.minecraft.client.gui.screens.ConfirmLinkScreen((confirm) -> {
                        if (confirm) {
                            openUri(uri1);
                        }

                        minecraft.setScreen(oldScreen);
                    }, uri1.toString(), false));
                    //? }
                } else {
                    openUri(uri1);
                }
                break;
            }
            case final ClickEvent.OpenFile file: {
                openUri(file.file().toURI());
                break;
            }
            case ClickEvent.RunCommand(final String cmd): {
                final LocalPlayer player = Minecraft.getInstance().player;
                if (player == null) {
                    break;
                }

                //? if >=26.2-pre-2 {
                /^player.connection.sendUnattendedCommand(net.minecraft.commands.Commands.trimOptionalPrefix(cmd), minecraft.gui.screen());
                ^///? } else if >=1.21.6 {
                /^player.connection.sendUnattendedCommand(net.minecraft.commands.Commands.trimOptionalPrefix(cmd), minecraft.screen);
                ^///? } else {
                final String command = cmd.startsWith("/") ? cmd.substring(1) : cmd;
                if (!minecraft.player.connection.sendUnsignedCommand(command)) {
                    ImGuiMCImpl.LOGGER.error("Not allowed to run command with signed argument from click event: '{}'", command);
                }
                //? }
                break;
            }
            case ClickEvent.CopyToClipboard(final String text): {
                minecraft.keyboardHandler.setClipboard(text);
                break;
            }
            case ClickEvent.SuggestCommand(final String command): {
                break;
            }
            default: {
                ImGuiMCImpl.LOGGER.error("Don't know how to handle {}", this.clickEvent);
                break;
            }
        }
    }

    private void handleHover() {
        final Minecraft minecraft = Minecraft.getInstance();
        switch (this.hoverEvent) {
            //? if >=26.1 {
            /^case HoverEvent.ShowItem(final net.minecraft.world.item.ItemStackTemplate item): {
                final List<Component> tooltip = Screen.getTooltipFromItem(minecraft, item.create());
            ^///? } else {
            case HoverEvent.ShowItem(final net.minecraft.world.item.ItemStack stack): {
                final List<Component> tooltip = Screen.getTooltipFromItem(minecraft, stack);
            //? }
                ImGui.beginTooltip();
                for (final Component line : tooltip) {
                    ImGuiMC.component(line, ImGui.getFontSize() * 35.0f);
                }
                ImGui.endTooltip();
                break;
            }
            case HoverEvent.ShowEntity(final HoverEvent.EntityTooltipInfo info): {
                if (minecraft.options.advancedItemTooltips) {
                    ImGui.beginTooltip();
                    final List<Component> tooltip = info.getTooltipLines();
                    for (final Component line : tooltip) {
                        ImGuiMC.component(line, ImGui.getFontSize() * 35.0f);
                    }
                    ImGui.endTooltip();
                }
                break;
            }
            case HoverEvent.ShowText(final Component component): {
                ImGui.beginTooltip();
                ImGuiMC.component(component, ImGui.getFontSize() * 35.0f);
                ImGui.endTooltip();
                break;
            }
            default: {
                break;
            }
        }
    }
    *///? } else {
    private void handleClick() {
        final Minecraft minecraft = Minecraft.getInstance();
        final String value = this.clickEvent.getValue();
        if (this.clickEvent.getAction() == ClickEvent.Action.OPEN_URL) {
            try {
                final URI uri = new URI(value);
                final String scheme = uri.getScheme();
                if (scheme == null) {
                    throw new java.net.URISyntaxException(value, "Missing protocol");
                }

                if (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https")) {
                    throw new java.net.URISyntaxException(value, "Unsupported protocol: " + scheme.toLowerCase(java.util.Locale.ROOT));
                }

                openUri(uri);
            } catch (final java.net.URISyntaxException e) {
                ImGuiMCImpl.LOGGER.error("Can't open url for {}", this.clickEvent, e);
            }
            return;
        }

        if (this.clickEvent.getAction() == ClickEvent.Action.OPEN_FILE) {
            openUri(new java.io.File(value).toURI());
            return;
        }

        // TODO
        if (this.clickEvent.getAction() == ClickEvent.Action.SUGGEST_COMMAND) {
            return;
        }

        if (this.clickEvent.getAction() == ClickEvent.Action.RUN_COMMAND) {
            final String s = net.minecraft.util.StringUtil.filterText(this.clickEvent.getValue());
            if (s.startsWith("/")) {
                final LocalPlayer player = Minecraft.getInstance().player;
                if (player != null && !player.connection.sendUnsignedCommand(s.substring(1))) {
                    ImGuiMCImpl.LOGGER.error("Not allowed to run command with signed argument from click event: '{}'", s);
                }
            } else {
                ImGuiMCImpl.LOGGER.error("Failed to run command without '/' prefix from click event: '{}'", s);
            }
            return;
        }

        if (this.clickEvent.getAction() == ClickEvent.Action.COPY_TO_CLIPBOARD) {
            minecraft.keyboardHandler.setClipboard(value);
            return;
        }

        ImGuiMCImpl.LOGGER.error("Don't know how to handle {}", this.clickEvent);
    }

    private void handleHover() {
        final Minecraft minecraft = Minecraft.getInstance();
        final HoverEvent.ItemStackInfo stack = this.hoverEvent.getValue(HoverEvent.Action.SHOW_ITEM);
        if (stack != null) {
            ImGui.beginTooltip();
            final List<Component> tooltip = Screen.getTooltipFromItem(minecraft, stack.getItemStack());
            for (final Component line : tooltip) {
                ImGuiMC.component(line, ImGui.getFontSize() * 35.0f);
            }
            ImGui.endTooltip();
            return;
        }

        final HoverEvent.EntityTooltipInfo entity = this.hoverEvent.getValue(HoverEvent.Action.SHOW_ENTITY);
        if (entity != null) {
            if (minecraft.options.advancedItemTooltips) {
                ImGui.beginTooltip();
                final List<Component> tooltip = entity.getTooltipLines();
                for (final Component line : tooltip) {
                    ImGuiMC.component(line, ImGui.getFontSize() * 35.0f);
                }
                ImGui.endTooltip();
            }
            return;
        }

        final Component showText = this.hoverEvent.getValue(HoverEvent.Action.SHOW_TEXT);
        if (showText != null) {
            ImGui.beginTooltip();
            ImGuiMC.component(showText, ImGui.getFontSize() * 35.0f);
            ImGui.endTooltip();
        }
    }
    //? }
}
