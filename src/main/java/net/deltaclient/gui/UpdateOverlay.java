package net.deltaclient.gui;

import net.deltaclient.network.NetworkHandler;
import net.lax1dude.eaglercraft.EagRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class UpdateOverlay {

    private static final ResourceLocation EAGLER_ICONS = new ResourceLocation("eagler", "gui/eagler_gui.png");

    private static final int PANEL_X = 2;
    private static final int PANEL_Y = 2;
    private static final int PANEL_W = 172;
    private static final int PANEL_H = 26;
    private static final int ICON_X = 6;
    private static final int ICON_Y = 6;
    private static final int ICON_SCREEN_W = 20;
    private static final int ICON_SCREEN_H = 20;
    private static final int ICON_TEX_U = 156;
    private static final int ICON_TEX_V = 0;
    private static final int ICON_TEX_W = 100;
    private static final int ICON_TEX_H = 100;
    private static final int ICON_TEX_FULL = 256;
    private static final int TEXT_X = 30;
    private static final int TEXT_Y = 6;
    private static final int BUTTON_W = 50;
    private static final int BUTTON_H = 16;
    private static final int BUTTON_X = PANEL_X + PANEL_W - BUTTON_W - 3;
    private static final int BUTTON_Y = 7;

    private final Button downloadButton;
    private boolean updateAvailable = false;

    public UpdateOverlay() {
        this.downloadButton = new Button(
                BUTTON_X, BUTTON_Y, BUTTON_W, BUTTON_H,
                I18n.format("menu.update.download"),
                (btn) -> {
                    if (NetworkHandler.latestDownloadUrl != null
                            && !NetworkHandler.latestDownloadUrl.isEmpty()) {
                        EagRuntime.openLink(NetworkHandler.latestDownloadUrl);
                    }
                }
        );
        this.downloadButton.visible = false;
        this.downloadButton.active = false;
    }

    public Button getDownloadButton() {
        return downloadButton;
    }

    public void setUpdateAvailable(boolean available) {
        this.updateAvailable = available;
        this.downloadButton.visible = available;
        this.downloadButton.active = available;
    }

    public void render(int mouseX, int mouseY, float partialTicks) {
        if (!updateAvailable) return;

        Minecraft mc = Minecraft.getInstance();

        AbstractGui.fill(PANEL_X, PANEL_Y, PANEL_X + PANEL_W, PANEL_Y + PANEL_H, 0xCC111111);
        AbstractGui.fill(PANEL_X, PANEL_Y, PANEL_X + 2, PANEL_Y + PANEL_H, 0xFF44AAFF);


        mc.getTextureManager().bindTexture(EAGLER_ICONS);
        AbstractGui.blit(ICON_X, ICON_Y, ICON_SCREEN_W, ICON_SCREEN_H,
                (float) ICON_TEX_U, (float) ICON_TEX_V,
                ICON_TEX_W, ICON_TEX_H, ICON_TEX_FULL, ICON_TEX_FULL);

        mc.fontRenderer.drawStringWithShadow(
                I18n.format("menu.update.available"),
                TEXT_X, TEXT_Y, 0xFFAA00);

        mc.fontRenderer.drawStringWithShadow(
                I18n.format("menu.update.version", NetworkHandler.latestVersion),
                TEXT_X, TEXT_Y + 11, 0xCCCCCC);
    }
}
