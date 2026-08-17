package net.deltaclient.friends;

import com.mojang.blaze3d.platform.GlStateManager;
import net.deltaclient.network.NetworkHandler;
import net.lax1dude.eaglercraft.internal.EnumCursorType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.LANConnectingScreen;
import net.minecraft.client.gui.widget.button.ImageButton;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class InviteEntry extends AbstractFriendsEntryContainerWidget {

    private static final ResourceLocation ACCEPT_TEX = new ResourceLocation("textures/gui/friends/accept.png");
    private static final ResourceLocation REJECT_TEX = new ResourceLocation("textures/gui/friends/reject.png");

    private final ImageButton acceptButton;
    private final ImageButton rejectButton;
    private boolean actionInProgress;

    private final String worldName;
    private final String worldCode;

    public InviteEntry(FriendsOverlayScreen screen, String fromPlayer,
                       String skinLocation, String worldName, String worldCode,
                       int x, int y, int width, int height) {
        super(screen, fromPlayer, skinLocation, true, "Invited you to " + worldName, x, y, width, height);
        this.worldName = worldName;
        this.worldCode = worldCode;

        this.acceptButton = new ImageButton(0, 0, BUTTON_SIZE, BUTTON_SIZE,
                0, 0, 0, ACCEPT_TEX, 18, 18,
                btn -> {
                    if (!actionInProgress) {
                        actionInProgress = true;
                        disable();
                        NetworkHandler.removeIfSafe(NetworkHandler.pendingInvites, i -> i.from.equals(playerName));
                        Minecraft mc = Minecraft.getInstance();
                        if (mc != null) {
                            mc.displayGuiScreen(new LANConnectingScreen(screen, worldCode));
                        }
                    }
                }, "") {
            @Override
            public void renderButton(int mx, int my, float pt) {
                Minecraft mc = Minecraft.getInstance();
                mc.getTextureManager().bindTexture(WIDGETS_LOCATION);
                int i = this.getYImage(this.isHovered());
                GlStateManager.color4f(1.0F, 1.0F, 1.0F, this.alpha);
                GlStateManager.enableBlend();
                GlStateManager.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
                GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
                this.blit(this.x, this.y, 0, 46 + i * 20, this.width / 2, this.height);
                this.blit(this.x + this.width / 2, this.y, 200 - this.width / 2, 46 + i * 20, this.width / 2, this.height);

                mc.getTextureManager().bindTexture(ACCEPT_TEX);
                GlStateManager.color4f(1.0F, 1.0F, 1.0F, 1.0F);
                int iconX = this.x + (this.width - 18) / 2;
                int iconY = this.y + (this.height - 18) / 2;
                blit(iconX, iconY, 0.0F, 0.0F, 18, 18, 18, 18);
            }
        };

        this.rejectButton = new ImageButton(0, 0, BUTTON_SIZE, BUTTON_SIZE,
                0, 0, 0, REJECT_TEX, 18, 18,
                btn -> {
                    if (!actionInProgress) {
                        actionInProgress = true;
                        disable();
                        NetworkHandler.removeIfSafe(NetworkHandler.pendingInvites, i -> i.from.equals(playerName));
                    }
                }, "") {
            @Override
            public void renderButton(int mx, int my, float pt) {
                Minecraft mc = Minecraft.getInstance();
                mc.getTextureManager().bindTexture(WIDGETS_LOCATION);
                int i = this.getYImage(this.isHovered());
                GlStateManager.color4f(1.0F, 1.0F, 1.0F, this.alpha);
                GlStateManager.enableBlend();
                GlStateManager.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
                GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
                this.blit(this.x, this.y, 0, 46 + i * 20, this.width / 2, this.height);
                this.blit(this.x + this.width / 2, this.y, 200 - this.width / 2, 46 + i * 20, this.width / 2, this.height);

                mc.getTextureManager().bindTexture(REJECT_TEX);
                GlStateManager.color4f(1.0F, 1.0F, 1.0F, 1.0F);
                int iconX = this.x + (this.width - 18) / 2;
                int iconY = this.y + (this.height - 18) / 2;
                blit(iconX, iconY, 0.0F, 0.0F, 18, 18, 18, 18);
            }
        };
    }

    @Override
    public void render(int mouseX, int mouseY, float partialTicks) {
        if (!visible) return;

        int btnY = y + (height - BUTTON_SIZE) / 2;
        int rejectX = x + width - BUTTON_SIZE + 2;
        int acceptX = rejectX - BUTTON_SIZE - 4;

        acceptButton.x = acceptX;
        acceptButton.y = btnY;
        acceptButton.visible = this.visible;
        rejectButton.x = rejectX;
        rejectButton.y = btnY;
        rejectButton.visible = this.visible;

        renderBase(mouseX, mouseY, partialTicks);
        acceptButton.render(mouseX, mouseY, partialTicks);
        rejectButton.render(mouseX, mouseY, partialTicks);

        if (acceptButton.isHovered()) {
            screen.setTooltip(I18n.format("gui.friends.accept_invite"), mouseX, mouseY);
            screen.setCursor(EnumCursorType.HAND);
        } else if (rejectButton.isHovered()) {
            screen.setTooltip(I18n.format("gui.friends.reject_invite"), mouseX, mouseY);
            screen.setCursor(EnumCursorType.HAND);
        }
    }

    @Override
    public boolean mouseClicked(int mouseX, int mouseY, int button) {
        if (!visible || button != 0 || actionInProgress) return false;
        if (acceptButton.mouseClicked(mouseX, mouseY, button)) return true;
        return rejectButton.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void disable() {
        actionInProgress = true;
        acceptButton.active = false;
        rejectButton.active = false;
    }
}
