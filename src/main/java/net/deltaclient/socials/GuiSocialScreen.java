package net.deltaclient.socials;

import com.mojang.blaze3d.platform.GlStateManager;
import net.deltaclient.network.NetworkHandler;import net.lax1dude.eaglercraft.profile.EaglerProfile;
import net.minecraft.client.gui.screen.LANConnectingScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.renderer.RenderSkybox;
import net.minecraft.client.renderer.RenderSkyboxCube;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.List;

@OnlyIn(Dist.CLIENT)
public class GuiSocialScreen extends Screen {

    public static final RenderSkyboxCube PANORAMA_RESOURCES = new RenderSkyboxCube(new ResourceLocation("textures/gui/title/background/panorama"));
    private static final ResourceLocation PANORAMA_OVERLAY_TEXTURES = new ResourceLocation("textures/gui/title/background/panorama_overlay.png");
    private final RenderSkybox panorama = new RenderSkybox(PANORAMA_RESOURCES);

    private final Screen parentScreen;

    public int currentTab = 0;
    public static final int TAB_MESSAGES = 0;
    public static final int TAB_FRIENDS = 1;

    private final ISocialsTab[] tabs;
    public float listScrollOffset = 0;

    // Active Chat State
    public String selectedFriendChat = null;
    public TextFieldWidget messageField;
    public float chatScrollOffset = 0;

    // Join button tracking for world invite messages (populated on each render pass)
    private static class JoinBtnInfo {
        int x1, y1, x2, y2;
        String code;
        JoinBtnInfo(int x1, int y1, int x2, int y2, String code) {
            this.x1 = x1; this.y1 = y1; this.x2 = x2; this.y2 = y2; this.code = code;
        }
    }
    private final java.util.List<JoinBtnInfo> visibleJoinBtns = new java.util.ArrayList<>();

    public GuiSocialScreen(Screen parent) {
        super(new TranslationTextComponent("socials.title"));
        this.parentScreen = parent;
        NetworkHandler.connect();
        tabs = new ISocialsTab[]{
                new SocialsMessagesTab(this),
                new SocialsFriendsTab(this)
        };
        currentTab = TAB_MESSAGES; // Default to Chat
    }

    public net.minecraft.client.gui.FontRenderer getFont() { return this.font; }
    public net.minecraft.client.Minecraft getMinecraft() { return this.mc; }

    public void drawRect(int minX, int minY, int maxX, int maxY, int color) { fill(minX, minY, maxX, maxY, color); }
    public void drawGradientRect(int x1, int y1, int x2, int y2, int color1, int color2) { fillGradient(x1, y1, x2, y2, color1, color2); }
    public void drawStringText(net.minecraft.client.gui.FontRenderer font, String text, int x, int y, int color) { drawString(font, text, x, y, color); }
    public void drawCenteredStringText(net.minecraft.client.gui.FontRenderer font, String text, int x, int y, int color) { drawCenteredString(font, text, x, y, color); }
    public void drawTexture(int x, int y, int width, int height, float uOffset, float vOffset, int uWidth, int vHeight, int textureWidth, int textureHeight) { blit(x, y, width, height, uOffset, vOffset, uWidth, vHeight, textureWidth, textureHeight); }
    public void addTabButton(net.minecraft.client.gui.widget.button.Button btn) { this.addButton(btn); }

    @Override
    protected void init() {
        this.mc.keyboardListener.enableRepeatEvents(true);

        int windowW = (int)(this.width * 0.75);
        int windowH = (int)(this.height * 0.75);
        int windowX = (this.width - windowW) / 2;
        int windowY = (this.height - windowH) / 2;
        int leftPaneW = windowW / 3;

        int listY = windowY + 60;
        int listH = windowH - 60;

        for (ISocialsTab tab : tabs) {
            tab.init(windowX, listY, leftPaneW, listH);
        }

        int rightPaneX = windowX + leftPaneW;
        int rightPaneW = windowW - leftPaneW;
        
        messageField = new TextFieldWidget(font, rightPaneX + 14, windowY + windowH - 28, rightPaneW - 30, 20, "");
        messageField.setMaxStringLength(256);
        messageField.setEnableBackgroundDrawing(false); // custom background
        messageField.setVisible(selectedFriendChat != null);
        
        updateFields();
    }

    private void updateFields() {
        for (int i = 0; i < tabs.length; i++) {
            tabs[i].setVisible(currentTab == i);
        }
        if (messageField != null) {
            messageField.setVisible(selectedFriendChat != null);
        }
    }

    public void openConversation(String friendName) {
        selectedFriendChat = friendName;
        chatScrollOffset = 0;
        updateFields();
    }

    @Override
    public void tick() {
        super.tick();
        tabs[currentTab].tick();
        if (messageField != null) {
            messageField.tick();
            if (selectedFriendChat != null) {
                String txt = messageField.getText();
                messageField.setSuggestion(txt.isEmpty() ? "Message " + selectedFriendChat : null);
            }
        }
        NetworkHandler.tick();
    }

    private void sendMessage() {
        if (selectedFriendChat == null) return;
        String text = messageField.getText().trim();
        if (!text.isEmpty()) {
            NetworkHandler.sendMessage(selectedFriendChat, text);
            messageField.setText("");
            chatScrollOffset = Float.MAX_VALUE; // scroll to bottom, clamped in render
        }
    }

    @Override
    public void render(int mx, int my, float partialTicks) {
        visibleJoinBtns.clear();

        // Panorama background
        GlStateManager.disableAlphaTest();
        GlStateManager.enableBlend();
        this.panorama.render(partialTicks, 1.0F);
        this.mc.getTextureManager().bindTexture(PANORAMA_OVERLAY_TEXTURES);
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        GlStateManager.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        blit(0, 0, this.width, this.height, 0.0F, 0.0F, 16, 128, 16, 128);
        GlStateManager.enableAlphaTest();

        fillGradient(0, 0, this.width, this.height, 0xAA111111, 0xDD111111);

        int windowW = (int)(this.width * 0.75);
        int windowH = (int)(this.height * 0.75);
        int windowX = (this.width - windowW) / 2;
        int windowY = (this.height - windowH) / 2;
        int leftPaneW = windowW / 3;
        int rightPaneX = windowX + leftPaneW;
        int rightPaneW = windowW - leftPaneW;

        // Base window backgrounds
        fill(windowX, windowY, windowX + leftPaneW, windowY + windowH, 0xFF191919);
        fill(rightPaneX, windowY, rightPaneX + rightPaneW, windowY + windowH, 0xFF222222);

        // Unified Grey Window Header
        fill(windowX, windowY, windowX + windowW, windowY + 24, 0xFF333333);
        // Header separator line
        fill(windowX, windowY + 24, windowX + windowW, windowY + 25, 0xFF111111);

        // Header X button
        int closeBtnX = windowX + windowW + 10;
        int closeBtnY = windowY + 4;
        boolean hoverClose = mx >= closeBtnX && mx < closeBtnX + 16 && my >= closeBtnY && my < closeBtnY + 16;
        if (hoverClose) fill(closeBtnX - 2, closeBtnY - 2, closeBtnX + 14, closeBtnY + 14, 0x44FF0000);
        font.drawStringWithShadow("X", closeBtnX + 6 - font.getStringWidth("X") / 2, closeBtnY + 4, hoverClose ? 0xFFFF5555 : 0xFFAAAAAA);

        // Sidebar Header
        font.drawStringWithShadow("§lSocial", windowX + 10, windowY + 8, 0xFFFFFF);
        
        // Sidebar icons (+, R) - R just left of separator with gap
        int iconY = windowY + 4;
        int rBtnRight = rightPaneX - 6; // gap from grey separator
        int rBtnLeft = rBtnRight - 16;
        int plusBtnRight = rBtnLeft - 2;
        int plusBtnLeft = plusBtnRight - 16;

        boolean hoverAdd = mx >= plusBtnLeft && mx < plusBtnRight && my >= iconY && my < iconY + 16;
        fill(plusBtnLeft, iconY, plusBtnRight, iconY + 16, hoverAdd ? 0xFF555555 : 0xFF444444);
        font.drawStringWithShadow("+", plusBtnLeft + 8 - font.getStringWidth("+") / 2, iconY + 4, 0x55AAFF);

        boolean hoverReq = mx >= rBtnLeft && mx < rBtnRight && my >= iconY && my < iconY + 16;
        fill(rBtnLeft, iconY, rBtnRight, iconY + 16, hoverReq ? 0xFF555555 : 0xFF444444);
        boolean hasRequests = NetworkHandler.pendingRequests.size() > 0;
        font.drawStringWithShadow("R", rBtnLeft + 8 - font.getStringWidth("R") / 2, iconY + 4, hasRequests ? 0xFFFFFF : 0xAAAAAA);
        if (hasRequests) fill(rBtnRight - 2, iconY + 2, rBtnRight, iconY + 4, 0xFFFF5555);

        // Tabs (Chat, Friends)
        int tabY = windowY + 30;
        boolean isChat = currentTab == TAB_MESSAGES;
        drawString(font, "Chat", windowX + 8, tabY, isChat ? 0xFF55AAFF : 0xFFAAAAAA);
        drawString(font, "Friends", windowX + 43, tabY, !isChat ? 0xFFFFFFFF : 0xFFAAAAAA);
        
        fill(windowX, windowY + 44, windowX + leftPaneW, windowY + 46, 0xFF3A3A3A); // Horizontal line under tabs
        fill(rightPaneX - 2, windowY, rightPaneX, windowY + windowH, 0xFF3A3A3A); // Vertical line separating panes

        // Sidebar List Scissors
        int listY = windowY + 50;
        int listH = windowH - 50;
        double scale = this.mc.mainWindow.getGuiScaleFactor();
        int fbH = this.mc.mainWindow.getFramebufferHeight();
        
        GlStateManager.enableScissorTest();
        GlStateManager.scissor((int)(windowX * scale), (int)(fbH - (listY + listH) * scale), (int)(leftPaneW * scale), (int)(listH * scale));
        
        GlStateManager.pushMatrix();
        GlStateManager.translatef(0.0F, -listScrollOffset, 0.0F);
        
        int entryHeight = 36;
        float maxListScroll = Math.max(0, tabs[currentTab].getListSize() * entryHeight - listH);
        if (listScrollOffset > maxListScroll) listScrollOffset = maxListScroll;
        if (listScrollOffset < 0) listScrollOffset = 0;

        int listMyAdj = (int)(my + listScrollOffset);
        tabs[currentTab].render(mx, listMyAdj, partialTicks, windowX, listY, leftPaneW, listY, entryHeight);

        GlStateManager.popMatrix();
        GlStateManager.disableScissorTest();

        // Right Pane
        if (selectedFriendChat != null) {
            String chatSkin = "";
            for (SocialPlayerEntry f : NetworkHandler.friends) {
                if (f.name.equals(selectedFriendChat)) { chatSkin = f.skinLocation; break; }
            }
            drawPlayerHead(rightPaneX + 5, windowY + 2, 20, 20, selectedFriendChat, chatSkin);
            font.drawStringWithShadow(selectedFriendChat, rightPaneX + 30, windowY + 8, 0xFFFFFF);
            
            // Join Game button
            boolean hoverJoin = mx >= rightPaneX + rightPaneW - 80 && mx < rightPaneX + rightPaneW - 10 && my >= windowY + 4 && my < windowY + 20;
            fillGradient(rightPaneX + rightPaneW - 80, windowY + 4, rightPaneX + rightPaneW - 10, windowY + 20, hoverJoin ? 0xFF3388DD : 0xFF2266AA, hoverJoin ? 0xFF2266AA : 0xFF114488);
            font.drawStringWithShadow("Join Game", rightPaneX + rightPaneW - 45 - font.getStringWidth("Join Game") / 2, windowY + 8, 0xFFFFFF);

            // Chat Area
            int chatY = windowY + 24;
            int chatH = windowH - 59; 

            
            GlStateManager.enableScissorTest();
            GlStateManager.scissor((int)(rightPaneX * scale), (int)(fbH - (chatY + chatH) * scale), (int)(rightPaneW * scale), (int)(chatH * scale));
            
            GlStateManager.pushMatrix();
            GlStateManager.translatef(0.0F, -chatScrollOffset, 0.0F);

            List<SocialMessage> chatHistory = NetworkHandler.getMessages(selectedFriendChat);
            
            // --- Precompute Heights ---
            int totalChatHeight = 0;
            int maxWidth = rightPaneW - 80;
            List<List<String>> messageLines = new java.util.ArrayList<>();
            List<Integer> messageWidths = new java.util.ArrayList<>();
            List<Integer> messageHeights = new java.util.ArrayList<>();
            List<Boolean> inviteFlags = new java.util.ArrayList<>();
            List<String> inviteCodes = new java.util.ArrayList<>();

            for (SocialMessage msg : chatHistory) {
                List<String> lines = font.listFormattedStringToWidth(msg.text, maxWidth);
                messageLines.add(lines);

                int bW = 0;
                for (String line : lines) {
                    bW = Math.max(bW, font.getStringWidth(line));
                }
                messageWidths.add(bW);

                boolean isInvite = msg.text.startsWith("§b⚑ §lWorld Invite§r");
                inviteFlags.add(isInvite);
                if (isInvite) {
                    inviteCodes.add(extractInviteCode(msg.text));
                } else {
                    inviteCodes.add(null);
                }

                int extraHeight = isInvite ? 30 : 0;
                int bH = lines.size() * font.FONT_HEIGHT + 16 + extraHeight;
                int mH = bH + 16;
                messageHeights.add(mH);
                totalChatHeight += mH;
            }

            float maxChatScroll = Math.max(0, totalChatHeight + 40 - chatH);
            if (chatScrollOffset > maxChatScroll) chatScrollOffset = maxChatScroll;
            if (chatScrollOffset < 0) chatScrollOffset = 0;

            int chatMyAdj = (int)(my + chatScrollOffset);

            int currentY = chatY + 14;

            for (int i = 0; i < chatHistory.size(); i++) {
                SocialMessage msg = chatHistory.get(i);
                List<String> lines = messageLines.get(i);
                int bW = messageWidths.get(i);
                boolean isInvite = inviteFlags.get(i);
                int extraHeight = isInvite ? 30 : 0;
                int bH = lines.size() * font.FONT_HEIGHT + 16 + extraHeight;
                int mH = messageHeights.get(i);

                boolean isYou = msg.sender.equals(NetworkHandler.loggedInUsername != null ? NetworkHandler.loggedInUsername : EaglerProfile.username);
                String timeStr = getTimeAgo(msg.timestamp);

                int bubbleX;
                if (isInvite) {
                    // Center invite bubbles regardless of sender
                    bubbleX = rightPaneX + (rightPaneW - bW) / 2 - 10;
                } else if (isYou) {
                    bubbleX = rightPaneX + rightPaneW - bW - 25;
                } else {
                    bubbleX = rightPaneX + 25;
                }

                if (isInvite && !isYou) {
                    GlStateManager.pushMatrix();
                    GlStateManager.scalef(0.8f, 0.8f, 0.8f);
                    drawString(font, "§8" + timeStr, (int)((bubbleX + bW) / 0.8f) - font.getStringWidth(timeStr) + 10, (int)((currentY - 10) / 0.8f), 0x888888);
                    GlStateManager.popMatrix();

                    fill(bubbleX - 8, currentY - 2, bubbleX + bW + 8, currentY + bH - 6, 0xFF3A2A0A);
                    fill(bubbleX - 8, currentY - 2, bubbleX + bW + 8, currentY, 0xFFDDB040); // Top border

                    for (int l = 0; l < lines.size(); l++) {
                        int lineW = font.getStringWidth(lines.get(l));
                        int lineX = bubbleX + (bW - lineW) / 2;
                        drawString(font, lines.get(l), lineX, currentY + 4 + l * font.FONT_HEIGHT, 0xFFFFFF);
                    }

                    int joinBtnW = 70;
                    int joinBtnH = 16;
                    int joinBtnX = bubbleX + (bW - joinBtnW) / 2;
                    int joinBtnY = currentY + lines.size() * font.FONT_HEIGHT + 10;
                    boolean hoverJoinBtn = chatMyAdj >= joinBtnY && chatMyAdj < joinBtnY + joinBtnH && mx >= joinBtnX && mx < joinBtnX + joinBtnW;
                    fill(joinBtnX, joinBtnY, joinBtnX + joinBtnW, joinBtnY + joinBtnH, hoverJoinBtn ? 0xFF33AA33 : 0xFF228822);
                    drawCenteredString(font, "§lJoin", joinBtnX + joinBtnW / 2, joinBtnY + 4, 0xFFFFFF);

                    String code = inviteCodes.get(i);
                    if (code != null) {
                        visibleJoinBtns.add(new JoinBtnInfo(joinBtnX, joinBtnY, joinBtnX + joinBtnW, joinBtnY + joinBtnH, code));
                    }
                } else if (isYou) {
                    GlStateManager.pushMatrix();
                    GlStateManager.scalef(0.8f, 0.8f, 0.8f);
                    drawString(font, "§8" + timeStr, (int)((bubbleX + bW) / 0.8f) - font.getStringWidth(timeStr) + 10, (int)((currentY - 10) / 0.8f), 0x888888);
                    GlStateManager.popMatrix();

                    fill(bubbleX - 6, currentY - 2, bubbleX + bW + 6, currentY + bH - 6, 0xFF2266AA);
                    for (int l = 0; l < lines.size(); l++) {
                        int lineW = font.getStringWidth(lines.get(l));
                        int lineX = bubbleX + (bW - lineW) / 2;
                        drawString(font, "§f" + lines.get(l), lineX, currentY + 4 + l * font.FONT_HEIGHT, 0xFFFFFF);
                    }
                } else {
                    GlStateManager.pushMatrix();
                    GlStateManager.scalef(0.8f, 0.8f, 0.8f);
                    drawString(font, "§8" + timeStr, (int)(bubbleX / 0.8f) - 6, (int)((currentY - 10) / 0.8f), 0x888888);
                    GlStateManager.popMatrix();

                    fill(bubbleX - 6, currentY - 2, bubbleX + bW + 6, currentY + bH - 6, 0xFF333333);
                    for (int l = 0; l < lines.size(); l++) {
                        int lineW = font.getStringWidth(lines.get(l));
                        int lineX = bubbleX + (bW - lineW) / 2;
                        drawString(font, "§f" + lines.get(l), lineX, currentY + 4 + l * font.FONT_HEIGHT, 0xFFFFFF);
                    }
                }

                currentY += mH;
            }

            GlStateManager.popMatrix();
            GlStateManager.disableScissorTest();

            // Footer Input
            fill(rightPaneX, windowY + windowH - 35, rightPaneX + rightPaneW, windowY + windowH, 0xFF1E1E1E);
            fill(rightPaneX, windowY + windowH - 36, rightPaneX + rightPaneW, windowY + windowH - 35, 0xFF111111);
            
            // Custom Text Field Background
            fill(rightPaneX + 10, windowY + windowH - 28, rightPaneX + rightPaneW - 10, windowY + windowH - 8, 0xFF1C1C1C);
            
            if (messageField != null) {
                GlStateManager.pushMatrix();
                GlStateManager.translatef(0, 6, 0);
                messageField.render(mx, my, partialTicks);
                GlStateManager.popMatrix();
            }
            
            // temp s button maybe ill upload a texture?
            boolean hasText = messageField != null && !messageField.getText().trim().isEmpty();
            if (hasText) {
                int sBtnX = rightPaneX + rightPaneW - 26;
                int sBtnY = windowY + windowH - 26;
                boolean hoverSend = mx >= sBtnX && mx < sBtnX + 16 && my >= sBtnY && my < sBtnY + 16;
                fill(sBtnX, sBtnY, sBtnX + 16, sBtnY + 16, hoverSend ? 0xFF555555 : 0xFF444444);
                font.drawStringWithShadow("S", sBtnX + 8 - font.getStringWidth("S") / 2, sBtnY + 4, 0x55AAFF);
            }
        } else {
            font.drawStringWithShadow("Select a chat to start messaging", rightPaneX + rightPaneW / 2 - font.getStringWidth("Select a chat to start messaging") / 2, windowY + windowH / 2, 0x888888);
        }

        super.render(mx, my, partialTicks);
    }

    public void drawPlayerHead(int x, int y, int w, int h, String username, String skinLocation) {
        ResourceLocation skinLoc;
        if (skinLocation != null && !skinLocation.isEmpty()) {
            skinLoc = new ResourceLocation(skinLocation);
        } else {
            skinLoc = net.minecraft.client.resources.DefaultPlayerSkin.getDefaultSkin(net.lax1dude.eaglercraft.EaglercraftUUID.nameUUIDFromBytes(username.getBytes()));
        }
        this.mc.getTextureManager().bindTexture(skinLoc);
        GlStateManager.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.enableBlend();
        blit(x, y, w, h, 8.0F, 8.0F, 8, 8, 64, 64);
        blit(x, y, w, h, 40.0F, 8.0F, 8, 8, 64, 64);
    }

    /**
     * Extracts the join code from an invite message text.
     * Format: "... §8Code: §f{code}§r ... §a[Join]"
     */
    private static String extractInviteCode(String text) {
        try {
            String marker = "§8Code: §f";
            int idx = text.indexOf(marker);
            if (idx >= 0) {
                String after = text.substring(idx + marker.length());
                int end = after.indexOf("§r");
                if (end >= 0) {
                    return after.substring(0, end).trim();
                }
                return after.trim();
            }
        } catch (Exception e) {
            // fall through
        }
        return "";
    }

    private int getUnreadCount() {
        int count = 0;
        String myName = NetworkHandler.loggedInUsername != null ? NetworkHandler.loggedInUsername : EaglerProfile.username;
        for (List<SocialMessage> msgs : NetworkHandler.conversations.values()) {
            for (SocialMessage m : msgs) {
                if (!m.read && !m.sender.equals(myName)) count++;
            }
        }
        return count;
    }

    private String getTimeAgo(long timestamp) {
        return new java.text.SimpleDateFormat("hh:mm a").format(new java.util.Date(timestamp));
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double amount) {
        int windowW = (int)(this.width * 0.75);
        int windowH = (int)(this.height * 0.75);
        int windowX = (this.width - windowW) / 2;
        int windowY = (this.height - windowH) / 2;
        int leftPaneW = windowW / 3;
        int rightPaneW = windowW - leftPaneW;

        if (mx < windowX + leftPaneW) {
            int listH = windowH - 45;
            listScrollOffset = (float) MathHelper.clamp(listScrollOffset - amount * 15, 0, Math.max(0, tabs[currentTab].getListSize() * 36 - listH));
        } else if (selectedFriendChat != null) {
            int chatH = windowH - 59;
            int maxWidth = rightPaneW - 80;
            List<SocialMessage> chatHistory = NetworkHandler.getMessages(selectedFriendChat);
            int totalChatHeight = 0;
            for (SocialMessage msg : chatHistory) {
                List<String> lines = font.listFormattedStringToWidth(msg.text, maxWidth);
                boolean isInvite = msg.text.startsWith("§b⚑ §lWorld Invite§r");
                int extraHeight = isInvite ? 30 : 0;
                int bH = lines.size() * font.FONT_HEIGHT + 16 + extraHeight;
                totalChatHeight += bH + 16;
            }
            chatScrollOffset = (float) MathHelper.clamp(chatScrollOffset - amount * 15, 0, Math.max(0, totalChatHeight + 40 - chatH));
        }
        return true;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        int windowW = (int)(this.width * 0.75);
        int windowH = (int)(this.height * 0.75);
        int windowX = (this.width - windowW) / 2;
        int windowY = (this.height - windowH) / 2;
        int leftPaneW = windowW / 3;
        int rightPaneX = windowX + leftPaneW;
        int rightPaneW = windowW - leftPaneW;

        int iconY = windowY + 4;
        int closeBtnX = windowX + windowW;
        if (mx >= closeBtnX && mx < closeBtnX + 16 && my >= iconY && my < iconY + 16) {
            this.mc.displayGuiScreen(parentScreen);
            return true;
        }

        int rBtnRight = rightPaneX - 6;
        int rBtnLeft = rBtnRight - 16;
        int plusBtnRight = rBtnLeft - 2;
        int plusBtnLeft = plusBtnRight - 16;
        if (my >= iconY && my < iconY + 16) {
            if (mx >= plusBtnLeft && mx < plusBtnRight) {
                this.mc.displayGuiScreen(new GuiSocialAddFriendScreen(this));
                return true;
            }
            if (mx >= rBtnLeft && mx < rBtnRight) {
                this.mc.displayGuiScreen(new GuiSocialRequestsScreen(this));
                return true;
            }
        }

        // Tabs
        int tabY = windowY + 32;
        if (my >= tabY - 10 && my < tabY + 10) {
            if (mx >= windowX + 10 && mx < windowX + 45) {
                currentTab = TAB_MESSAGES;
                listScrollOffset = 0;
                updateFields();
                return true;
            }
            if (mx >= windowX + 45 && mx < windowX + 100) {
                currentTab = TAB_FRIENDS;
                listScrollOffset = 0;
                updateFields();
                return true;
            }
        }

        // S Send Button (inside text field)
        if (selectedFriendChat != null && messageField != null && !messageField.getText().trim().isEmpty()) {
            int sBtnX = rightPaneX + rightPaneW - 26;
            int sBtnY = windowY + windowH - 26;
            if (mx >= sBtnX && mx < sBtnX + 16 && my >= sBtnY && my < sBtnY + 16) {
                sendMessage();
                return true;
            }
        }

        if (selectedFriendChat != null && button == 0) {
            int chatMyAdjJoin = (int)(my + chatScrollOffset);
            for (JoinBtnInfo jbi : visibleJoinBtns) {
                if (mx >= jbi.x1 && mx < jbi.x2 && chatMyAdjJoin >= jbi.y1 && chatMyAdjJoin < jbi.y2) {
                    this.mc.displayGuiScreen(new LANConnectingScreen(GuiSocialScreen.this, jbi.code));
                    return true;
                }
            }
        }

        if (selectedFriendChat != null && mx >= rightPaneX) {
            int chatY = windowY + 24;
            int chatMyAdj = (int)(my + chatScrollOffset);
            List<SocialMessage> chatHistory = NetworkHandler.getMessages(selectedFriendChat);
            
            int currentY = chatY + 14;
            int maxWidth = rightPaneW - 80;
            
            for (int i = 0; i < chatHistory.size(); i++) {
                SocialMessage msg = chatHistory.get(i);
                
                List<String> lines = font.listFormattedStringToWidth(msg.text, maxWidth);
                int bW = 0;
                for (String line : lines) bW = Math.max(bW, font.getStringWidth(line));
                int bH = lines.size() * font.FONT_HEIGHT + 16;
                int mH = bH + 16;
                
                boolean isYou = msg.sender.equals(NetworkHandler.loggedInUsername != null ? NetworkHandler.loggedInUsername : EaglerProfile.username);
                if (isYou) {
                    int bubbleX = rightPaneX + rightPaneW - bW - 25;
                    if (button == 1 && mx >= bubbleX - 6 && mx < bubbleX + bW + 6 && chatMyAdj >= currentY - 2 && chatMyAdj < currentY + bH - 6) {
                        NetworkHandler.deleteMessage(selectedFriendChat, msg);
                        chatHistory.remove(i);
                        NetworkHandler.saveMessages();
                        return true;
                    }
                }
                currentY += mH;
            }
        }

        if (messageField != null && messageField.getVisible()) {
            if (messageField.mouseClicked(mx, my, button)) return true;
        }

        int listY = windowY + 55;
        int listMyAdj = (int)(my + listScrollOffset);
        if (tabs[currentTab].mouseClicked(mx, listMyAdj, button, windowX, listY, leftPaneW, listY, 36)) {
            return true;
        }

        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) {
            this.mc.displayGuiScreen(parentScreen);
            return true;
        }
        if (messageField != null && messageField.isFocused() && messageField.getVisible()) {
            if (keyCode == 257 || keyCode == 335) {
                sendMessage();
                return true;
            }
            if (messageField.keyPressed(keyCode, scanCode, modifiers)) return true;
        }
        if (tabs[currentTab].keyPressed(keyCode, scanCode, modifiers)) return true;
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (messageField != null && messageField.isFocused() && messageField.getVisible()) {
            if (messageField.charTyped(codePoint, modifiers)) return true;
        }
        if (tabs[currentTab].charTyped(codePoint, modifiers)) return true;
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public void removed() { this.mc.keyboardListener.enableRepeatEvents(false); }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public boolean shouldCloseOnEsc() { return false; }

    public static class SocialPlayerEntry {
        public final String name;
        public boolean online;
        public String status;
        public String skinLocation;
        public NetworkHandler.OpenWorldInfo openWorld;
        public SocialPlayerEntry(String name, boolean online, String status, String skinLocation) {
            this.name = name; this.online = online; this.status = status; this.skinLocation = skinLocation;
            this.openWorld = null;
        }
        public SocialPlayerEntry(String name, boolean online, String status, String skinLocation, NetworkHandler.OpenWorldInfo openWorld) {
            this.name = name; this.online = online; this.status = status; this.skinLocation = skinLocation;
            this.openWorld = openWorld;
        }
    }

    public static class SocialMessage {
        public final String sender;
        public final String text;
        public final long timestamp;
        public boolean read;
        public SocialMessage(String sender, String text, long timestamp, boolean read) {
            this.sender = sender; this.text = text; this.timestamp = timestamp; this.read = read;
        }
    }
}
