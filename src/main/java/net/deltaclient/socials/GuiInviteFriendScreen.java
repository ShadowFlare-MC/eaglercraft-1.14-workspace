package net.deltaclient.socials;

import com.mojang.blaze3d.platform.GlStateManager;
import net.deltaclient.network.NetworkHandler;import net.lax1dude.eaglercraft.internal.PlatformWebRTC;
import net.lax1dude.eaglercraft.sp.SingleplayerServerController;
import net.lax1dude.eaglercraft.sp.lan.LANServerController;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.StringTextComponent;

import java.util.ArrayList;
import java.util.List;

public class GuiInviteFriendScreen extends Screen {

    private final Screen parentScreen;
    private float scrollOffset = 0;

    private boolean startingWorld = false;
    private String invitingFriend = null;
    private String pendingWorldName = null;
    private boolean inviteSent = false;
    private int inviteCloseTimer = 0; // ticks until auto-close after invite sent

    public GuiInviteFriendScreen(Screen parent) {
        super(new StringTextComponent("Invite Friend"));
        this.parentScreen = parent;
    }

    @Override
    protected void init() {
        super.init();
        this.mc.keyboardListener.enableRepeatEvents(true);
    }

    @Override
    public void removed() {
        this.mc.keyboardListener.enableRepeatEvents(false);
    }

    @Override
    public void tick() {
        super.tick();
        NetworkHandler.tick();

        if (inviteSent) {
            inviteCloseTimer++;
            if (inviteCloseTimer > 30) { // ~1.5s at 20 tps
                this.mc.displayGuiScreen(null);
                this.mc.setIngameFocus();
            }
        }
    }

    @Override
    public void render(int mx, int my, float partialTicks) {
        this.renderBackground();

        int menuW = 280;
        int menuH = this.height - 80;
        int menuX = (this.width - menuW) / 2;
        int menuY = 40;

        // Background
        fill(menuX, menuY, menuX + menuW, menuY + menuH, 0xDD111111);
        fill(menuX, menuY, menuX + menuW, menuY + 32, 0xFF222222); // Header
        fill(menuX, menuY + 32, menuX + menuW, menuY + 33, 0xFF000000);

        // Back / Cancel
        boolean hoverBack = mx >= menuX + 5 && mx < menuX + 50 && my >= menuY + 8 && my < menuY + 24;
        drawString(this.font, "< Back", menuX + 10, menuY + 12, hoverBack ? 0xFFFFFFFF : 0xFFAAAAAA);

        // Title
        drawCenteredString(this.font, "§lInvite a Friend", menuX + menuW / 2, menuY + 12, 0xFFFFFF);

        // Instructions
        drawCenteredString(this.font, "Invite online friends to your world!", menuX + menuW / 2, menuY + 45, 0xAAAAAA);

        // Status message
        if (startingWorld) {
            drawCenteredString(this.font, "§eStarting shared world...", menuX + menuW / 2, menuY + 60, 0xFFFFAA);
            super.render(mx, my, partialTicks);
            return;
        }
        if (inviteSent) {
            drawCenteredString(this.font, "§aInvite sent! Returning...", menuX + menuW / 2, menuY + 60, 0x55FF55);
            super.render(mx, my, partialTicks);
            return;
        }

        // Friends list
        int listY = menuY + 70;
        int listH = menuY + menuH - listY - 40;

        // Gather onleine friends
        List<GuiSocialScreen.SocialPlayerEntry> onlineFriends = new ArrayList<>();
        for (GuiSocialScreen.SocialPlayerEntry f : NetworkHandler.friends) {
            if (f.online && !f.name.equals(NetworkHandler.loggedInUsername)) {
                onlineFriends.add(f);
            }
        }

        if (onlineFriends.isEmpty()) {
            drawCenteredString(this.font, "§7No online friends found", menuX + menuW / 2, listY + 20, 0x888888);
            drawCenteredString(this.font, "§7Use the Social screen to add friends!", menuX + menuW / 2, listY + 36, 0x666666);
            super.render(mx, my, partialTicks);
            return;
        }

        double scale = this.mc.mainWindow.getGuiScaleFactor();
        int fbH = this.mc.mainWindow.getFramebufferHeight();

        GlStateManager.enableScissorTest();
        GlStateManager.scissor((int)(menuX * scale), (int)(fbH - (listY + listH) * scale), (int)(menuW * scale), (int)(listH * scale));

        GlStateManager.pushMatrix();
        GlStateManager.translatef(0.0F, -scrollOffset, 0.0F);

        int entryHeight = 36;
        float maxScroll = Math.max(0, onlineFriends.size() * entryHeight - listH);
        if (scrollOffset > maxScroll) scrollOffset = maxScroll;
        if (scrollOffset < 0) scrollOffset = 0;

        int listMyAdj = (int)(my + scrollOffset);

        for (int i = 0; i < onlineFriends.size(); i++) {
            GuiSocialScreen.SocialPlayerEntry entry = onlineFriends.get(i);
            String uname = entry.name;
            int y = listY + i * entryHeight;

            boolean hover = mx >= menuX && mx < menuX + menuW && listMyAdj >= y && listMyAdj < y + entryHeight;
            if (hover) {
                fill(menuX, y, menuX + menuW, y + entryHeight, 0xFF222222);
            }

            int headSize = 24;
            int headX = menuX + 10;
            int headY = y + 6;
            drawPlayerHead(headX, headY, headSize, headSize, uname, entry.skinLocation);

            // Username
            drawString(this.font, uname, menuX + 45, y + 14, 0xFFFFFF);

            // Status dot (green since these are online)
            fill(menuX + 42, y + 4, menuX + 46, y + 8, 0xFF55FF55);

            // "Invite" button
            int btnX = menuX + menuW - 75;
            int btnY = y + 6;
            boolean hoverBtn = mx >= btnX && mx < btnX + 65 && listMyAdj >= btnY && listMyAdj < btnY + 24;
            fill(btnX, btnY, btnX + 65, btnY + 24, hoverBtn ? 0xFF3388DD : 0xFF2266AA);
            drawCenteredString(this.font, "§lInvite", btnX + 32, btnY + 7, 0xFFFFFF);
        }

        GlStateManager.popMatrix();
        GlStateManager.disableScissorTest();

        super.render(mx, my, partialTicks);
    }

    private void drawPlayerHead(int x, int y, int w, int h, String username, String skinLocation) {
        net.minecraft.util.ResourceLocation skinLoc;
        if (skinLocation != null && !skinLocation.isEmpty()) {
            skinLoc = new net.minecraft.util.ResourceLocation(skinLocation);
        } else {
            skinLoc = net.minecraft.client.resources.DefaultPlayerSkin.getDefaultSkin(
                    net.lax1dude.eaglercraft.EaglercraftUUID.nameUUIDFromBytes(username.getBytes()));
        }
        this.mc.getTextureManager().bindTexture(skinLoc);
        GlStateManager.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.enableBlend();
        blit(x, y, w, h, 8.0F, 8.0F, 8, 8, 64, 64);
        blit(x, y, w, h, 40.0F, 8.0F, 8, 8, 64, 64);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        int menuW = 280;
        int menuH = this.height - 80;
        int menuX = (this.width - menuW) / 2;
        int menuY = 40;

        // Back button
        if (mx >= menuX + 5 && mx < menuX + 50 && my >= menuY + 8 && my < menuY + 24) {
            this.mc.displayGuiScreen(parentScreen);
            return true;
        }

        // Prevent clicks during world start
        if (startingWorld || inviteSent) return true;

        int listY = menuY + 70;
        int listH = menuY + menuH - listY - 40;

        // Gather online friends
        List<GuiSocialScreen.SocialPlayerEntry> onlineFriends = new ArrayList<>();
        for (GuiSocialScreen.SocialPlayerEntry f : NetworkHandler.friends) {
            if (f.online && !f.name.equals(NetworkHandler.loggedInUsername)) {
                onlineFriends.add(f);
            }
        }

        if (mx >= menuX && mx < menuX + menuW && my >= listY && my < listY + listH) {
            int listMyAdj = (int)(my + scrollOffset);
            int entryHeight = 36;

            for (int i = 0; i < onlineFriends.size(); i++) {
                String uname = onlineFriends.get(i).name;
                int y = listY + i * entryHeight;

                int btnX = menuX + menuW - 75;
                int btnY = y + 6;

                if (mx >= btnX && mx < btnX + 65 && listMyAdj >= btnY && listMyAdj < btnY + 24) {
                    inviteFriend(uname);
                    return true;
                }
            }
        }

        return super.mouseClicked(mx, my, button);
    }

    private void inviteFriend(String friendName) {
        startingWorld = true;
        invitingFriend = friendName;

        if (!PlatformWebRTC.supported()) {
            System.out.println("[Invite] WebRTC is not supported on this platform!");
            startingWorld = false;
            invitingFriend = null;
            return;
        }

        String worldName;
        try {
            if (Minecraft.getInstance().world != null && Minecraft.getInstance().world.getWorldInfo() != null) {
                String saveName = Minecraft.getInstance().world.getWorldInfo().getWorldName();
                worldName = (saveName != null && !saveName.isEmpty()) ? saveName : Minecraft.getInstance().getSession().getUsername() + "'s World";
            } else {
                worldName = Minecraft.getInstance().getSession().getUsername() + "'s World";
            }
        } catch (Exception e) {
            worldName = Minecraft.getInstance().getSession().getUsername() + "'s World";
        }
        pendingWorldName = worldName;

        if (!LANServerController.isLANOpen()) {
            PlatformWebRTC.startRTCLANServer();
            String code = LANServerController.shareToLAN((msg) -> {
                System.out.println("[Invite] " + msg);
            }, worldName, false);

            if (code == null) {
                System.out.println("[Invite] Failed to start shared world!");
                startingWorld = false;
                invitingFriend = null;
                return;
            }
            SingleplayerServerController.configureLAN(
                    net.minecraft.world.GameType.SURVIVAL,
                    false
            );
        }

        String code = LANServerController.getCurrentCode();
        sendInviteMessage(friendName, worldName, code);

        startingWorld = false;
        inviteSent = true;
    }

    private void sendInviteMessage(String friendName, String worldName, String code) {
        String relayURI = LANServerController.getCurrentURI();
        String inviteText = "§b⚑ §lWorld Invite§r"
                + "\n§7" + NetworkHandler.loggedInUsername + " invited you to join §f" + worldName + "§r"
                + "\n§8Code: §f" + code + "§r"
                + "\n§a[Join]";
        NetworkHandler.sendMessage(friendName, inviteText);
    }

    @Override
    public boolean mouseScrolled(double p_mouseScrolled_1_, double p_mouseScrolled_3_, double amount) {
        int menuW = 280;
        int menuX = (this.width - menuW) / 2;
        if (p_mouseScrolled_1_ >= menuX && p_mouseScrolled_1_ < menuX + menuW) {
            scrollOffset = (float) MathHelper.clamp(scrollOffset - amount * 15, 0, 9999);
            return true;
        }
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) {  // Escape
            this.mc.displayGuiScreen(parentScreen);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return true; // Keep the game paused while this screen is shown
    }
}
