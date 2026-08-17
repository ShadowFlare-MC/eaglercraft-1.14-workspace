package net.minecraft.client.gui.screen;

import net.deltaclient.network.NetworkHandler;
import net.lax1dude.eaglercraft.sp.SingleplayerServerController;
import net.lax1dude.eaglercraft.sp.lan.LANServerController;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.GameType;

public class LANHostConnectingScreen extends Screen {

    private final Screen lastScreen;
    private final String gameMode;
    private final boolean allowCheats;
    private final String worldName;
    private final boolean hiddenToggle;
    private final boolean isOnline;

    private int renderCount = 0;
    private String loadingMessage = "Connecting...";

    public LANHostConnectingScreen(Screen lastScreen, String gameMode, boolean allowCheats, String worldName, boolean hiddenToggle) {
        this(lastScreen, gameMode, allowCheats, worldName, hiddenToggle, false);
    }

    public LANHostConnectingScreen(Screen lastScreen, String gameMode, boolean allowCheats, String worldName, boolean hiddenToggle, boolean isOnline) {
        super(new TranslationTextComponent("lanServer.title"));
        this.lastScreen = lastScreen;
        this.gameMode = gameMode;
        this.allowCheats = allowCheats;
        this.worldName = worldName;
        this.hiddenToggle = hiddenToggle;
        this.isOnline = isOnline;
    }

    public void render(int mouseX, int mouseY, float partialTicks) {
        this.renderBackground();
        this.drawCenteredString(this.font, loadingMessage, this.width / 2, this.height / 3 + 10, 0xFFFFFF);

        super.render(mouseX, mouseY, partialTicks);

        if (++renderCount == 2) {
            String code = LANServerController.shareToLAN((msg) -> {
                this.loadingMessage = msg;
            }, worldName, hiddenToggle);

            if (code != null) {
                SingleplayerServerController.configureLAN(GameType.getByName(this.gameMode), this.allowCheats);

                // If Online mode, notify the socials server so friends can see "Request to Join"
                if (isOnline) {
                    NetworkHandler.openWorld(code, worldName);
                    this.mc.ingameGUI.getChatGUI().printChatMessage(new StringTextComponent(
                            TextFormatting.GREEN + "World opened online! Friends can now request to join."));
                }

                this.mc.ingameGUI.getChatGUI().printChatMessage(new StringTextComponent(
                        I18n.format("lanServer.opened")
                                .replace("$relay$", LANServerController.getCurrentURI())
                                .replace("$code$", code)));
                this.mc.displayGuiScreen(null);
            } else {
                this.mc.displayGuiScreen(this.lastScreen);
                this.mc.ingameGUI.getChatGUI().printChatMessage(
                        new StringTextComponent(TextFormatting.RED + "Failed to open LAN server. No relays available."));
            }
        }
    }

    public boolean shouldCloseOnEsc() {
        return false;
    }
}
