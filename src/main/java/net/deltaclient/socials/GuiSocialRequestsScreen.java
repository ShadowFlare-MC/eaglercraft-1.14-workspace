package net.deltaclient.socials;

import com.mojang.blaze3d.platform.GlStateManager;
import net.deltaclient.network.NetworkHandler;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TranslationTextComponent;

import java.util.ArrayList;
import java.util.List;

public class GuiSocialRequestsScreen extends Screen {
    private final GuiSocialScreen parentScreen;
    private float scrollOffset = 0;
    
    private List<String> removedTemporarily = new ArrayList<>();

    public GuiSocialRequestsScreen(GuiSocialScreen parent) {
        super(new TranslationTextComponent("socials.requests"));
        this.parentScreen = parent;
    }

    @Override
    protected void init() {
        this.mc.keyboardListener.enableRepeatEvents(true);
        
        int menuW = 240;
        int menuX = (this.width - menuW) / 2;
        int menuY = 40;
        
        this.mc.keyboardListener.enableRepeatEvents(true);
    }

    @Override
    public void removed() {
        this.mc.keyboardListener.enableRepeatEvents(false);
    }

    @Override
    public void render(int mx, int my, float partialTicks) {
        this.renderBackground();
        
        int menuW = 240;
        int menuH = this.height - 80;
        int menuX = (this.width - menuW) / 2;
        int menuY = 40;
        
        // Background
        fill(menuX, menuY, menuX + menuW, menuY + menuH, 0xDD111111);
        fill(menuX, menuY, menuX + menuW, menuY + 32, 0xFF222222); // Header
        fill(menuX, menuY + 32, menuX + menuW, menuY + 33, 0xFF000000);
        
        // Back Button
        boolean hoverBack = mx >= menuX + 5 && mx < menuX + 30 && my >= menuY + 8 && my < menuY + 24;
        drawString(this.font, "< Back", menuX + 10, menuY + 12, hoverBack ? 0xFFFFFFFF : 0xFFAAAAAA);
        
        // Title
        font.drawStringWithShadow(net.minecraft.client.resources.I18n.format("socials.requests"), menuX + menuW / 2 - font.getStringWidth(net.minecraft.client.resources.I18n.format("socials.requests")) / 2, menuY + 12, 0xFFFFFF);
        
        // List Area
        int listY = menuY + 35;
        int listH = menuY + menuH - listY;
        
        List<GuiSocialScreen.SocialPlayerEntry> displayList = new ArrayList<>();
        for (GuiSocialScreen.SocialPlayerEntry u : NetworkHandler.pendingRequests) {
            if (!removedTemporarily.contains(u.name)) {
                displayList.add(u);
            }
        }
        
        if (!NetworkHandler.pendingRequestsOutgoing.isEmpty()) {
            displayList.add(new GuiSocialScreen.SocialPlayerEntry("##SEPARATOR##", false, "", ""));
            for (GuiSocialScreen.SocialPlayerEntry u : NetworkHandler.pendingRequestsOutgoing) {
                if (!removedTemporarily.contains(u.name)) {
                    displayList.add(new GuiSocialScreen.SocialPlayerEntry(u.name, false, "outgoing", u.skinLocation));
                }
            }
        }
        
        double scale = this.mc.mainWindow.getGuiScaleFactor();
        int fbH = this.mc.mainWindow.getFramebufferHeight();
        
        GlStateManager.enableScissorTest();
        GlStateManager.scissor((int)(menuX * scale), (int)(fbH - (listY + listH) * scale), (int)(menuW * scale), (int)(listH * scale));
        
        GlStateManager.pushMatrix();
        GlStateManager.translatef(0.0F, -scrollOffset, 0.0F);
        
        int entryHeight = 36;
        float maxScroll = Math.max(0, displayList.size() * entryHeight - listH);
        if (scrollOffset > maxScroll) scrollOffset = maxScroll;
        if (scrollOffset < 0) scrollOffset = 0;
        
        int listMyAdj = (int)(my + scrollOffset);
        
        for (int i = 0; i < displayList.size(); i++) {
            GuiSocialScreen.SocialPlayerEntry entry = displayList.get(i);
            String uname = entry.name;
            int y = listY + i * entryHeight;
            
            if (uname.equals("##SEPARATOR##")) {
                drawString(this.font, net.minecraft.client.resources.I18n.format("socials.pending"), menuX + 10, y + 14, 0xAAAAAA);
                continue;
            }
            
            boolean hover = mx >= menuX && mx < menuX + menuW && listMyAdj >= y && listMyAdj < y + entryHeight;
            if (hover) {
                fill(menuX, y, menuX + menuW, y + entryHeight, 0xFF222222);
            }
            
            // Draw head
            parentScreen.drawPlayerHead(menuX + 10, y + 6, 24, 24, uname, entry.skinLocation);
            drawString(this.font, "§l" + uname, menuX + 45, y + 14, 0xFFFFFF);
            
            if ("outgoing".equals(entry.status)) {
                // Deny/Cancel Button
                int denyX = menuX + menuW - 35;
                boolean hoverDeny = mx >= denyX && mx < denyX + 25 && listMyAdj >= y + 5 && listMyAdj < y + 30;
                fill(denyX, y + 5, denyX + 25, y + 30, hoverDeny ? 0xFFAA2222 : 0xFF881111);
                drawString(this.font, "X", denyX + 12 - font.getStringWidth("X") / 2, y + 14, 0xFFFFFF);
            } else {
                // Accept Button
                int btnX = menuX + menuW - 65;
                boolean hoverAdd = mx >= btnX && mx < btnX + 25 && listMyAdj >= y + 5 && listMyAdj < y + 30;
                fill(btnX, y + 5, btnX + 25, y + 30, hoverAdd ? 0xFF22AA22 : 0xFF118811);
                drawString(this.font, "A", btnX + 12 - font.getStringWidth("A") / 2, y + 14, 0xFFFFFF);
                
                // Deny Button
                int denyX = menuX + menuW - 35;
                boolean hoverDeny = mx >= denyX && mx < denyX + 25 && listMyAdj >= y + 5 && listMyAdj < y + 30;
                fill(denyX, y + 5, denyX + 25, y + 30, hoverDeny ? 0xFFAA2222 : 0xFF881111);
                drawString(this.font, "X", denyX + 12 - font.getStringWidth("X") / 2, y + 14, 0xFFFFFF);
            }
        }
        
        GlStateManager.popMatrix();
        GlStateManager.disableScissorTest();
        
        super.render(mx, my, partialTicks);
    }
    
    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        int menuW = 240;
        int menuH = this.height - 80;
        int menuX = (this.width - menuW) / 2;
        int menuY = 40;
        
        // Back Button
        if (mx >= menuX + 5 && mx < menuX + 50 && my >= menuY + 8 && my < menuY + 24) {
            this.mc.displayGuiScreen(parentScreen);
            return true;
        }
        
        // List Area
        int listY = menuY + 35;
        
        if (mx >= menuX && mx < menuX + menuW && my >= listY && my < menuY + menuH) {
            int listMyAdj = (int)(my + scrollOffset);
            int entryHeight = 36;
            
            List<GuiSocialScreen.SocialPlayerEntry> displayList = new ArrayList<>();
            for (GuiSocialScreen.SocialPlayerEntry u : NetworkHandler.pendingRequests) {
                if (!removedTemporarily.contains(u.name)) {
                    displayList.add(u);
                }
            }
            if (!NetworkHandler.pendingRequestsOutgoing.isEmpty()) {
                displayList.add(new GuiSocialScreen.SocialPlayerEntry("##SEPARATOR##", false, "", ""));
                for (GuiSocialScreen.SocialPlayerEntry u : NetworkHandler.pendingRequestsOutgoing) {
                    if (!removedTemporarily.contains(u.name)) {
                        displayList.add(new GuiSocialScreen.SocialPlayerEntry(u.name, false, "outgoing", u.skinLocation));
                    }
                }
            }
            
            for (int i = 0; i < displayList.size(); i++) {
                GuiSocialScreen.SocialPlayerEntry entry = displayList.get(i);
                String uname = entry.name;
                int y = listY + i * entryHeight;
                if (uname.equals("##SEPARATOR##")) continue;
                
                if ("outgoing".equals(entry.status)) {
                    int denyX = menuX + menuW - 35;
                    if (mx >= denyX && mx < denyX + 25 && listMyAdj >= y + 5 && listMyAdj < y + 30) {
                        if (!removedTemporarily.contains(uname)) {
                            NetworkHandler.cancelFriendRequest(uname);
                            removedTemporarily.add(uname);
                        }
                        return true;
                    }
                } else {
                    int btnX = menuX + menuW - 65;
                    if (mx >= btnX && mx < btnX + 25 && listMyAdj >= y + 5 && listMyAdj < y + 30) {
                        if (!removedTemporarily.contains(uname)) {
                            NetworkHandler.acceptFriend(uname);
                            removedTemporarily.add(uname);
                        }
                        return true;
                    }
                    
                    int denyX = menuX + menuW - 35;
                    if (mx >= denyX && mx < denyX + 25 && listMyAdj >= y + 5 && listMyAdj < y + 30) {
                        if (!removedTemporarily.contains(uname)) {
                            NetworkHandler.refuseFriend(uname);
                            removedTemporarily.add(uname);
                        }
                        return true;
                    }
                }
            }
        }
        
        return super.mouseClicked(mx, my, button);
    }
    
    @Override
    public boolean mouseScrolled(double p_mouseScrolled_1_, double p_mouseScrolled_3_, double amount) {
        int menuW = 240;
        int menuX = (this.width - menuW) / 2;
        if (p_mouseScrolled_1_ >= menuX && p_mouseScrolled_1_ < menuX + menuW) {
            scrollOffset = (float) MathHelper.clamp(scrollOffset - amount * 15, 0, 9999);
            return true;
        }
        return false;
    }
    
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) {
            this.mc.displayGuiScreen(parentScreen);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
    
    @Override
    public void tick() {
        NetworkHandler.tick();
    }
}
