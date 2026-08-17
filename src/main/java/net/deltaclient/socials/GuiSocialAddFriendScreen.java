package net.deltaclient.socials;

import com.mojang.blaze3d.platform.GlStateManager;
import net.deltaclient.network.NetworkHandler;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TranslationTextComponent;

import java.util.ArrayList;
import java.util.List;

public class GuiSocialAddFriendScreen extends Screen {
    private final GuiSocialScreen parentScreen;
    private TextFieldWidget searchField;
    private float scrollOffset = 0;
    
    private List<String> addedTemporarily = new ArrayList<>();
    private List<String> removedTemporarily = new ArrayList<>();
    private long lastAddAnimTime = 0;

    public GuiSocialAddFriendScreen(GuiSocialScreen parent) {
        super(new TranslationTextComponent("socials.addFriend"));
        this.parentScreen = parent;
    }

    @Override
    protected void init() {
        super.init();
        this.mc.keyboardListener.enableRepeatEvents(true);
        
        int menuW = 240;
        int menuX = (this.width - menuW) / 2;
        int menuY = 40;
        
        searchField = new TextFieldWidget(this.font, menuX + 12, menuY + 44, menuW - 24, 20, I18n.format("socials.search"));
        searchField.setMaxStringLength(32);
        searchField.setEnableBackgroundDrawing(false);
        searchField.setVisible(true);
        
        NetworkHandler.searchUsers("");
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
        drawCenteredString(this.font, "Add Friend", menuX + menuW / 2, menuY + 12, 0xFFFFFF);
        
        // Search Field
        int searchY = menuY + 40;
        fill(menuX + 10, searchY, menuX + menuW - 10, searchY + 28, 0xFF1C1C1C); // 28 height box
        if (searchField != null) {
            GlStateManager.pushMatrix();
            GlStateManager.translatef(0, 4, 0);
            searchField.render(mx, my, partialTicks);
            GlStateManager.popMatrix();
        }
        
        // List Area
        int listY = searchY + 35;
        int listH = menuY + menuH - listY;
        
        List<GuiSocialScreen.SocialPlayerEntry> displayList = new ArrayList<>();
        for (GuiSocialScreen.SocialPlayerEntry u : NetworkHandler.searchResults) {
            if (!removedTemporarily.contains(u.name)) {
                displayList.add(u);
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
            
            boolean hover = mx >= menuX && mx < menuX + menuW && listMyAdj >= y && listMyAdj < y + entryHeight;
            if (hover) {
                fill(menuX, y, menuX + menuW, y + entryHeight, 0xFF222222);
            }
            
            // Draw head
            parentScreen.drawPlayerHead(menuX + 10, y + 6, 24, 24, uname, entry.skinLocation);
            drawString(this.font, "§l" + uname, menuX + 45, y + 14, 0xFFFFFF);
            
            // + Button
            boolean isAdded = addedTemporarily.contains(uname);
            int btnX = menuX + menuW - 35;
            int btnY = y + 8;
            
            if (isAdded) {
                drawString(this.font, "§a✔", btnX + 5, btnY + 6, 0xFF55FF55);
                
                // Remove from list after a short delay
                if (System.currentTimeMillis() - lastAddAnimTime > 1000) {
                    addedTemporarily.remove(uname);
                    removedTemporarily.add(uname);
                }
            } else {
                boolean hoverBtn = hover && mx >= btnX && mx < btnX + 20 && listMyAdj >= btnY && listMyAdj < btnY + 20;
                fill(btnX, btnY, btnX + 20, btnY + 20, hoverBtn ? 0xFF22AA22 : 0xFF117711);
                drawString(this.font, "+", btnX + 7, btnY + 6, 0xFFFFFF);
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
        
        int searchY = menuY + 40;
        if (searchField != null && my >= searchY && my < searchY + 28) {
            if (searchField.mouseClicked(mx, my, button)) {
                return true;
            }
        }
        
        int listY = searchY + 35;
        int listH = menuY + menuH - listY;
        
        if (mx >= menuX && mx < menuX + menuW && my >= listY && my < listY + listH) {
            int listMyAdj = (int)(my + scrollOffset);
            int entryHeight = 36;
            
            List<GuiSocialScreen.SocialPlayerEntry> displayList = new ArrayList<>();
            for (GuiSocialScreen.SocialPlayerEntry u : NetworkHandler.searchResults) {
                if (!removedTemporarily.contains(u.name)) {
                    displayList.add(u);
                }
            }
            
            for (int i = 0; i < displayList.size(); i++) {
                String uname = displayList.get(i).name;
                int y = listY + i * entryHeight;
                
                int btnX = menuX + menuW - 35;
                int btnY = y + 8;
                
                if (!addedTemporarily.contains(uname)) {
                    if (mx >= btnX && mx < btnX + 20 && listMyAdj >= btnY && listMyAdj < btnY + 20) {
                        NetworkHandler.addFriend(uname);
                        addedTemporarily.add(uname);
                        lastAddAnimTime = System.currentTimeMillis();
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
        if (searchField != null && searchField.isFocused()) {
            String before = searchField.getText();
            if (searchField.keyPressed(keyCode, scanCode, modifiers)) {
                String after = searchField.getText();
                if (!before.equals(after)) {
                    NetworkHandler.searchUsers(after.trim());
                    scrollOffset = 0;
                }
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
    
    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (searchField != null && searchField.isFocused()) {
            String before = searchField.getText();
            if (searchField.charTyped(codePoint, modifiers)) {
                String after = searchField.getText();
                if (!before.equals(after)) {
                    NetworkHandler.searchUsers(after.trim());
                    scrollOffset = 0;
                }
                return true;
            }
        }
        return super.charTyped(codePoint, modifiers);
    }
    
    @Override
    public void tick() {
        if (searchField != null) searchField.tick();
        NetworkHandler.tick();
    }
}
