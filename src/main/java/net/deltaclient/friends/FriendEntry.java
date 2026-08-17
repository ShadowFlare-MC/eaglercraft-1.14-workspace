package net.deltaclient.friends;

import com.mojang.blaze3d.platform.GlStateManager;
import net.deltaclient.socials.GuiSocialScreen;
import net.deltaclient.network.NetworkHandler;
import net.lax1dude.eaglercraft.internal.EnumCursorType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.toasts.FriendToast;
import net.minecraft.client.gui.widget.button.ImageButton;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class FriendEntry extends AbstractFriendsEntryContainerWidget {

    private static final ResourceLocation REMOVE_TEX = new ResourceLocation("textures/gui/friends/remove.png");
    private static final ResourceLocation INVITE_TEX = new ResourceLocation("textures/gui/friends/multiplayer/invite.png");
    private static final ResourceLocation JOIN_REQUEST_TEX = new ResourceLocation("textures/gui/friends/multiplayer/join_request.png");

    private final ImageButton removeButton;
    private final ImageButton inviteButton;
    private final ImageButton requestJoinButton;
    private boolean removeLoading;

    public FriendEntry(FriendsOverlayScreen screen, String playerName,
                       String skinLocation, boolean online, String status,
                       int x, int y, int width, int height) {
        super(screen, playerName, skinLocation, online, status, x, y, width, height);

        this.removeButton = new ImageButton(0, 0, BUTTON_SIZE, BUTTON_SIZE,
                0, 0, 0, REMOVE_TEX, 14, 13,
                btn -> {
                    if (!removeLoading) {
                        removeLoading = true;
                        btn.active = false;
                        NetworkHandler.removeFriend(playerName);
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

                mc.getTextureManager().bindTexture(REMOVE_TEX);
                GlStateManager.color4f(1.0F, 1.0F, 1.0F, 1.0F);
                int iconX = this.x + (this.width - 14) / 2;
                int iconY = this.y + (this.height - 13) / 2;
                blit(iconX, iconY, 0.0F, 0.0F, 14, 13, 14, 13);
            }
        };

        this.inviteButton = new ImageButton(0, 0, BUTTON_SIZE, BUTTON_SIZE,
                0, 0, 0, INVITE_TEX, 7, 11,
                btn -> {
                    invitePlayer();
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

                mc.getTextureManager().bindTexture(INVITE_TEX);
                GlStateManager.color4f(1.0F, 1.0F, 1.0F, 1.0F);
                int iconX = this.x + (this.width - 7) / 2;
                int iconY = this.y + (this.height - 11) / 2;
                blit(iconX, iconY, 0.0F, 0.0F, 7, 11, 7, 11);
            }
        };

        this.requestJoinButton = new ImageButton(0, 0, BUTTON_SIZE, BUTTON_SIZE,
                0, 0, 0, JOIN_REQUEST_TEX, 7, 11,
                btn -> {
                    btn.active = false;
                    requestToJoin();
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

                mc.getTextureManager().bindTexture(JOIN_REQUEST_TEX);
                GlStateManager.color4f(1.0F, 1.0F, 1.0F, 1.0F);
                int iconX = this.x + (this.width - 7) / 2;
                int iconY = this.y + (this.height - 11) / 2;
                blit(iconX, iconY, 0.0F, 0.0F, 7, 11, 7, 11);
            }
        };
    }

    private void requestToJoin() {
        NetworkHandler.requestJoin(playerName);
        Minecraft mc = Minecraft.getInstance();
        FriendToast.showRequestSent(mc, playerName, skinLocation);
    }

    private void invitePlayer() {
        NetworkHandler.invitePlayer(playerName);
        Minecraft mc = Minecraft.getInstance();
        FriendToast.showInviteSent(mc, playerName, skinLocation);
    }

    @Override
    public void render(int mouseX, int mouseY, float partialTicks) {
        if (!visible) return;

        int btnY = y + (height - BUTTON_SIZE) / 2;
        int removeX = x + width - BUTTON_SIZE + 2;

        boolean hasOpenWorld = false;
        for (GuiSocialScreen.SocialPlayerEntry f : NetworkHandler.friends) {
            if (f.name.equals(playerName) && f.openWorld != null) {
                hasOpenWorld = true;
                break;
            }
        }

        int nextBtnX = removeX;

        if (hasOpenWorld) {
            nextBtnX -= (BUTTON_SIZE + 4);
            requestJoinButton.x = nextBtnX;
            requestJoinButton.y = btnY;
            requestJoinButton.visible = true;
        } else {
            requestJoinButton.visible = false;
        }

        boolean weHaveOpenWorld = NetworkHandler.ourOpenWorld != null;
        if (weHaveOpenWorld && online) {
            nextBtnX -= (BUTTON_SIZE + 4);
            inviteButton.x = nextBtnX;
            inviteButton.y = btnY;
            inviteButton.visible = true;
        } else {
            inviteButton.visible = false;
        }

        removeButton.x = removeX;
        removeButton.y = btnY;
        removeButton.visible = true;

        renderBase(mouseX, mouseY, partialTicks);

        if (hasOpenWorld) {
            requestJoinButton.render(mouseX, mouseY, partialTicks);
        }
        weHaveOpenWorld = NetworkHandler.ourOpenWorld != null;
        if (weHaveOpenWorld && online) {
            inviteButton.render(mouseX, mouseY, partialTicks);
        }
        removeButton.render(mouseX, mouseY, partialTicks);

        if (requestJoinButton.visible && requestJoinButton.isHovered()) {
            screen.setTooltip(I18n.format("gui.friends.request_join"), mouseX, mouseY);
            screen.setCursor(EnumCursorType.HAND);
        } else if (inviteButton.visible && inviteButton.isHovered()) {
            screen.setTooltip(I18n.format("gui.friends.invite"), mouseX, mouseY);
            screen.setCursor(EnumCursorType.HAND);
        } else if (removeButton.isHovered()) {
            screen.setTooltip(I18n.format("gui.friends.remove"), mouseX, mouseY);
            screen.setCursor(EnumCursorType.HAND);
        }
    }

    @Override
    public boolean mouseClicked(int mouseX, int mouseY, int button) {
        if (!visible || button != 0) return false;

        boolean hasOpenWorld = false;
        for (GuiSocialScreen.SocialPlayerEntry f : NetworkHandler.friends) {
            if (f.name.equals(playerName) && f.openWorld != null) {
                hasOpenWorld = true;
                break;
            }
        }

        if (hasOpenWorld && requestJoinButton.mouseClicked(mouseX, mouseY, button)) return true;
        boolean weHaveOpenWorld = NetworkHandler.ourOpenWorld != null;
        if (weHaveOpenWorld && online && inviteButton.mouseClicked(mouseX, mouseY, button)) return true;
        return removeButton.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void disable() {
        removeButton.active = false;
        inviteButton.active = false;
        requestJoinButton.active = false;
    }
}
