package net.deltaclient.friends;

import com.mojang.blaze3d.platform.GlStateManager;
import net.deltaclient.network.NetworkHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.widget.button.ImageButton;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class OutgoingEntry extends AbstractFriendsEntryContainerWidget {

    private static final ResourceLocation REVOKE_TEX = new ResourceLocation("textures/gui/friends/cancel.png");

    private final ImageButton revokeButton;

    public OutgoingEntry(FriendsOverlayScreen screen, String playerName,
                         String skinLocation, boolean online, String status,
                         int x, int y, int width, int height) {
        super(screen, playerName, skinLocation, online, status, x, y, width, height);

        this.revokeButton = new ImageButton(0, 0, BUTTON_SIZE, BUTTON_SIZE,
                0, 0, 0, REVOKE_TEX, 16, 16,
                btn -> {
                    if (btn.active) {
                        btn.active = false;
                        NetworkHandler.cancelFriendRequest(playerName);
                    }
                }, "") {
            @Override
            public void renderButton(int mx, int my, float pt) {
                Minecraft mc = Minecraft.getInstance();

                // Render button background from widgets.png (3-state)
                mc.getTextureManager().bindTexture(WIDGETS_LOCATION);
                int i = this.getYImage(this.isHovered());
                GlStateManager.color4f(1.0F, 1.0F, 1.0F, this.alpha);
                GlStateManager.enableBlend();
                GlStateManager.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
                GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
                this.blit(this.x, this.y, 0, 46 + i * 20, this.width / 2, this.height);
                this.blit(this.x + this.width / 2, this.y, 200 - this.width / 2, 46 + i * 20, this.width / 2, this.height);

                // Render revoke icon on top
                mc.getTextureManager().bindTexture(REVOKE_TEX);
                GlStateManager.color4f(1.0F, 1.0F, 1.0F, 1.0F);
                int iconX = this.x + (this.width - 16) / 2;
                int iconY = this.y + (this.height - 16) / 2;
                blit(iconX, iconY, 0.0F, 0.0F, 16, 16, 16, 16);
            }
        };
    }

    @Override
    public void render(int mouseX, int mouseY, float partialTicks) {
        if (!visible) return;

        int revokeX = x + width - BUTTON_SIZE + 2;
        int btnY = y + (height - BUTTON_SIZE) / 2;
        revokeButton.x = revokeX;
        revokeButton.y = btnY;
        revokeButton.visible = this.visible;

        renderBase(mouseX, mouseY, partialTicks);

        revokeButton.render(mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean mouseClicked(int mouseX, int mouseY, int button) {
        if (!visible || button != 0) return false;
        return revokeButton.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void disable() {
        revokeButton.active = false;
    }
}
