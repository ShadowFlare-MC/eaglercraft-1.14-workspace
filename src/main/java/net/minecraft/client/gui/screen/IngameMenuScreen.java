package net.minecraft.client.gui.screen;

import com.mojang.blaze3d.platform.GlStateManager;
import net.deltaclient.friends.FriendsOverlayScreen;
import net.deltaclient.network.NetworkHandler;
import net.lax1dude.eaglercraft.EagRuntime;
import net.lax1dude.eaglercraft.PauseMenuCustomizeState;
import org.lwjgl.input.Mouse;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.gui.advancements.AdvancementsScreen;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.client.gui.widget.button.ImageButton;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class IngameMenuScreen extends Screen {
    private static final ResourceLocation FRIENDS_TEX = new ResourceLocation("textures/gui/friends.png");

    private final boolean isFullMenu;

    public IngameMenuScreen(boolean p_i51519_1_) {
        super(p_i51519_1_ ? new TranslationTextComponent("menu.game") : new TranslationTextComponent("menu.paused"));
        this.isFullMenu = p_i51519_1_;
    }

    protected void init() {
        if (this.isFullMenu) {
            this.addButtons();
        }

    }

    private static class ButtonWithStupidIcons extends Button {
        private ResourceLocation iconL;
        private float aspectL;
        private ResourceLocation iconR;
        private float aspectR;

        public ButtonWithStupidIcons(int x, int y, int width, int height, String text, ResourceLocation iconL, float aspectL, ResourceLocation iconR, float aspectR, Button.IPressable onPress) {
            super(x, y, width, height, text, onPress);
            this.iconL = iconL;
            this.aspectL = aspectL;
            this.iconR = iconR;
            this.aspectR = aspectR;
        }

        @Override
        public void renderButton(int mouseX, int mouseY, float partialTicks) {
            super.renderButton(mouseX, mouseY, partialTicks);
            if (!this.visible) return;
            int textWidth = Minecraft.getInstance().fontRenderer.getStringWidth(this.getMessage());
            if (this.iconL != null) {
                Minecraft.getInstance().getTextureManager().bindTexture(this.iconL);
                GlStateManager.pushMatrix();
                GlStateManager.translatef(this.x + this.width / 2.0F - textWidth / 2.0F - 4.0F - 10.0F * this.aspectL, this.y + (this.height - 10) / 2.0F, 0.0F);
                float f = 10.0F / 256.0F;
                GlStateManager.scalef(f * this.aspectL, f, f);
                GlStateManager.color4f(1.0F, 1.0F, 1.0F, 1.0F);
                AbstractGui.blit(0, 0, 0, 0, 256, 256, 256, 256);
                GlStateManager.popMatrix();
            }
            if (this.iconR != null) {
                Minecraft.getInstance().getTextureManager().bindTexture(this.iconR);
                GlStateManager.pushMatrix();
                GlStateManager.translatef(this.x + this.width / 2.0F + textWidth / 2.0F + 4.0F, this.y + (this.height - 10) / 2.0F, 0.0F);
                float f = 10.0F / 256.0F;
                GlStateManager.scalef(f * this.aspectR, f, f);
                GlStateManager.color4f(1.0F, 1.0F, 1.0F, 1.0F);
                AbstractGui.blit(0, 0, 0, 0, 256, 256, 256, 256);
                GlStateManager.popMatrix();
            }
        }
    }

    private void addButtons() {
        int i = -16;
        int j = 98;

        this.addButton(new ButtonWithStupidIcons(this.width / 2 - 102, this.height / 4 + 24 + -16, 204, 20, I18n.format("menu.returnToGame"),
                PauseMenuCustomizeState.icon_backToGame_L, PauseMenuCustomizeState.icon_backToGame_L_aspect,
                PauseMenuCustomizeState.icon_backToGame_R, PauseMenuCustomizeState.icon_backToGame_R_aspect,
                (p_213070_1_) -> {
                    this.mc.displayGuiScreen((Screen) null);
                    this.mc.setIngameFocus();
                }));

        this.addButton(new ButtonWithStupidIcons(this.width / 2 - 102, this.height / 4 + 48 + -16, 98, 20, I18n.format("gui.advancements"),
                PauseMenuCustomizeState.icon_achievements_L, PauseMenuCustomizeState.icon_achievements_L_aspect,
                PauseMenuCustomizeState.icon_achievements_R, PauseMenuCustomizeState.icon_achievements_R_aspect,
                (p_213065_1_) -> {
                    this.mc.displayGuiScreen(new AdvancementsScreen(this.mc.player.connection.getAdvancementManager()));
                }));

        this.addButton(new ButtonWithStupidIcons(this.width / 2 + 4, this.height / 4 + 48 + -16, 98, 20, I18n.format("gui.stats"),
                PauseMenuCustomizeState.icon_statistics_L, PauseMenuCustomizeState.icon_statistics_L_aspect,
                PauseMenuCustomizeState.icon_statistics_R, PauseMenuCustomizeState.icon_statistics_R_aspect,
                (p_213066_1_) -> {
                    this.mc.displayGuiScreen(new StatsScreen(this, this.mc.player.getStats()));
                }));

        if (PauseMenuCustomizeState.serverInfoMode != PauseMenuCustomizeState.SERVER_INFO_MODE_NONE) {
            this.addButton(new ButtonWithStupidIcons(this.width / 2 - 102, this.height / 4 + 72 + -16, 204, 20, PauseMenuCustomizeState.serverInfoButtonText,
                    PauseMenuCustomizeState.icon_serverInfo_L, PauseMenuCustomizeState.icon_serverInfo_L_aspect,
                    PauseMenuCustomizeState.icon_serverInfo_R, PauseMenuCustomizeState.icon_serverInfo_R_aspect,
                    (p) -> {
                        if (PauseMenuCustomizeState.serverInfoMode == PauseMenuCustomizeState.SERVER_INFO_MODE_EXTERNAL_URL && PauseMenuCustomizeState.serverInfoURL != null) {
                            EagRuntime.openLink(PauseMenuCustomizeState.serverInfoURL);
                        }
                    }));
        }

        this.addButton(new ButtonWithStupidIcons(this.width / 2 - 102, this.height / 4 + 96 + -16, 98, 20, I18n.format("menu.options"),
                PauseMenuCustomizeState.icon_options_L, PauseMenuCustomizeState.icon_options_L_aspect,
                PauseMenuCustomizeState.icon_options_R, PauseMenuCustomizeState.icon_options_R_aspect,
                (p_213071_1_) -> {
                    this.mc.displayGuiScreen(new OptionsScreen(this, this.mc.gameSettings));
                }));

        Button button = this.addButton(new ButtonWithStupidIcons(this.width / 2 + 4, this.height / 4 + 96 + -16, 98, 20, I18n.format("Invite"),
                PauseMenuCustomizeState.icon_discord_L, PauseMenuCustomizeState.icon_discord_L_aspect,
                PauseMenuCustomizeState.icon_discord_R, PauseMenuCustomizeState.icon_discord_R_aspect,
                (p_213068_1_) -> {
                    if (PauseMenuCustomizeState.discordButtonMode == PauseMenuCustomizeState.DISCORD_MODE_INVITE_URL && PauseMenuCustomizeState.discordInviteURL != null) {
                        EagRuntime.openLink(PauseMenuCustomizeState.discordInviteURL);
                    } else{
                        mc.displayGuiScreen(new ShareToLanScreen(this));
                    }
                }));

        if (PauseMenuCustomizeState.discordButtonMode != PauseMenuCustomizeState.DISCORD_MODE_NONE) {
            button.setMessage(PauseMenuCustomizeState.discordButtonText);
        } else {
            button.active = this.mc.isSingleplayer() && !(this.mc.getIntegratedServer() != null && this.mc.getIntegratedServer().getPublic());
        }

        Button button1 = this.addButton(new ButtonWithStupidIcons(this.width / 2 - 102, this.height / 4 + 120 + -16, 204, 20, I18n.format("menu.returnToMenu"),
                PauseMenuCustomizeState.icon_disconnect_L, PauseMenuCustomizeState.icon_disconnect_L_aspect,
                PauseMenuCustomizeState.icon_disconnect_R, PauseMenuCustomizeState.icon_disconnect_R_aspect,
                (p_213067_1_) -> {
                    boolean flag = this.mc.isIntegratedServerRunning();
                    boolean flag1 = this.mc.isConnectedToRealms();
                    p_213067_1_.active = false;
                    if (flag) {
                        this.mc.scheduleWorldUnload(new DirtMessageScreen(new TranslationTextComponent("menu.savingLevel")), new MainMenuScreen());
                        this.mc.world.sendQuittingDisconnectingPacket();

                        if (flag) {
                            this.mc.shutdownIntegratedServer(new MainMenuScreen());
                        } else {
                            this.mc.shutdownIntegratedServer(new MultiplayerScreen(new MainMenuScreen()));
                        }
                    } else {
                        this.mc.world.sendQuittingDisconnectingPacket();
                        this.mc.func_213254_o();
                        this.mc.displayGuiScreen(new MultiplayerScreen(new MainMenuScreen()));
                    }

                }));
        if (!this.mc.isIntegratedServerRunning()) {
            button1.setMessage(I18n.format("menu.disconnect"));
        }

        if(mc.gameSettings.socialFeatures && mc.isSingleplayer()) {
            this.addButton(new ImageButton(this.width / 2 - 10, this.height / 4 + 144 + -16, 20, 20,
                    0, 0, 0, FRIENDS_TEX, 16, 16,
                    (p) -> {
                        this.mc.displayGuiScreen(new FriendsOverlayScreen(this));
                    }, I18n.format("socials.friends")) {
                @Override
                public void renderButton(int mouseX, int mouseY, float partialTicks) {
                    Minecraft mc = Minecraft.getInstance();
                    mc.getTextureManager().bindTexture(WIDGETS_LOCATION);
                    int i = this.getYImage(this.isHovered());
                    GlStateManager.color4f(1.0F, 1.0F, 1.0F, this.alpha);
                    GlStateManager.enableBlend();
                    GlStateManager.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
                    GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
                    this.blit(this.x, this.y, 0, 46 + i * 20, this.width / 2, this.height);
                    this.blit(this.x + this.width / 2, this.y, 200 - this.width / 2, 46 + i * 20, this.width / 2, this.height);

                    mc.getTextureManager().bindTexture(FRIENDS_TEX);
                    GlStateManager.color4f(1.0F, 1.0F, 1.0F, 1.0F);
                    this.blit(this.x + 2, this.y + 2, 0, 0, 16, 16, 16, 16);

                    // Red dot indicator for pending requests
                    if (NetworkHandler.pendingRequests.size() > 0 || NetworkHandler.pendingJoinRequests.size() > 0) {
                        int dotX = this.x + this.width - 5;
                        int dotY = this.y + 1;
                        fill(dotX, dotY, dotX + 5, dotY + 5, 0xFFFF3333);
                        fill(dotX + 1, dotY + 1, dotX + 4, dotY + 4, 0xFFFF5555);
                    }

                    if (this.isHovered()) {
                        IngameMenuScreen.this.renderTooltip(I18n.format("socials.friends"), mouseX, mouseY);
                    }
                }
            });
        }

    }

    public void tick() {
        NetworkHandler.tick();
        super.tick();
        if (Mouse.isActuallyGrabbed()) {
            Mouse.setGrabbed(false);
            this.mc.mouseHelper.ungrabMouse();
        }
    }

    public void render(int p_render_1_, int p_render_2_, float p_render_3_) {
        if (this.isFullMenu) {
            this.renderBackground();
            this.drawCenteredString(this.font, this.title.getFormattedText(), this.width / 2, 40, 16777215);
        } else {
            this.drawCenteredString(this.font, this.title.getFormattedText(), this.width / 2, 10, 16777215);
        }

        int titleStrWidth = this.font.getStringWidth(this.title.getFormattedText());

        if (PauseMenuCustomizeState.icon_title_L != null) {
            this.mc.getTextureManager().bindTexture(PauseMenuCustomizeState.icon_title_L);
            GlStateManager.pushMatrix();
            GlStateManager.translatef((this.width - titleStrWidth) / 2.0F - 6.0F - 16.0F * PauseMenuCustomizeState.icon_title_L_aspect, this.isFullMenu ? 40.0F : 10.0F, 0.0F);
            float f2 = 16.0F / 256.0F;
            GlStateManager.scalef(f2 * PauseMenuCustomizeState.icon_title_L_aspect, f2, f2);
            GlStateManager.color4f(1.0F, 1.0F, 1.0F, 1.0F);
            AbstractGui.blit(0, 0, 0, 0, 256, 256, 256, 256);
            GlStateManager.popMatrix();
        }
        if (PauseMenuCustomizeState.icon_title_R != null) {
            this.mc.getTextureManager().bindTexture(PauseMenuCustomizeState.icon_title_R);
            GlStateManager.pushMatrix();
            GlStateManager.translatef((this.width + titleStrWidth) / 2.0F + 6.0F, this.isFullMenu ? 40.0F : 10.0F, 0.0F);
            float f2 = 16.0F / 256.0F;
            GlStateManager.scalef(f2 * PauseMenuCustomizeState.icon_title_R_aspect, f2, f2);
            GlStateManager.color4f(1.0F, 1.0F, 1.0F, 1.0F);
            AbstractGui.blit(0, 0, 0, 0, 256, 256, 256, 256);
            GlStateManager.popMatrix();
        }

        super.render(p_render_1_, p_render_2_, p_render_3_);
    }
}
