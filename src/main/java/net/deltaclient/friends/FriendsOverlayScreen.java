package net.deltaclient.friends;

import com.mojang.blaze3d.platform.GlStateManager;
import net.deltaclient.network.NetworkHandler;
import net.lax1dude.eaglercraft.internal.EnumCursorType;
import net.lax1dude.eaglercraft.internal.PlatformInput;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;

@OnlyIn(Dist.CLIENT)
public class FriendsOverlayScreen extends Screen {

    private static final ResourceLocation BACKGROUND_TEXTURE = new ResourceLocation("textures/gui/friends/background.png");

    // 9-slice background: texture is 236x34 with 8px borders
    private static final int BG_TEX_W = 236;
    private static final int BG_TEX_H = 34;
    private static final int BG_BORDER = 8;

    public static final int OVERLAY_WIDTH = 180;
    private static final int TAB_BAR_HEIGHT = 20;

    private final Screen backgroundScreen;

    private FriendsTab friendsTab;
    private PendingTab pendingTab;
    private AbstractFriendsTab currentTab;

    private int overlayX;
    private int overlayY;
    private int overlayHeight;

    private boolean tabHoveredFriends;
    private boolean tabHoveredPending;

    // Tooltip support — set by child entries, rendered after scissor
    private String tooltipText;
    private int tooltipX;
    private int tooltipY;

    // Cursor support — TEXT > HAND > DEFAULT priority
    private EnumCursorType desiredCursor = EnumCursorType.DEFAULT;

    public void setTooltip(String text, int x, int y) {
        if (this.tooltipText == null) {
            this.tooltipText = text;
            this.tooltipX = x;
            this.tooltipY = y;
        }
    }

    public void setCursor(EnumCursorType cursor) {
        // TEXT > HAND > DEFAULT — update desiredCursor, actual call happens at end of render()
        if (cursor == EnumCursorType.TEXT) {
            desiredCursor = cursor;
        } else if (cursor == EnumCursorType.HAND && desiredCursor == EnumCursorType.DEFAULT) {
            desiredCursor = cursor;
        }
    }

    public FriendsOverlayScreen(@Nullable Screen backgroundScreen) {
        super(new TranslationTextComponent("gui.friends.open"));
        this.backgroundScreen = backgroundScreen;
    }

    @Override
    public void init() {
        if (this.backgroundScreen != null) {
            this.backgroundScreen.init(this.mc, this.width, this.height);
        }

        int scrollableMaxHeight = this.height - 80;
        this.overlayX = (this.width - OVERLAY_WIDTH) / 2;
        this.overlayY = (this.height - scrollableMaxHeight) / 2;
        this.overlayHeight = scrollableMaxHeight;

        this.friendsTab = new FriendsTab(this, OVERLAY_WIDTH - 10, scrollableMaxHeight);
        this.pendingTab = new PendingTab(this, OVERLAY_WIDTH - 10, scrollableMaxHeight);
        this.currentTab = this.friendsTab;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.currentTab != null) {
            this.currentTab.tick();
        }
        NetworkHandler.tick();
    }

    @Override
    public void render(int mouseX, int mouseY, float partialTicks) {
        tooltipText = null;
        desiredCursor = EnumCursorType.DEFAULT;

        if (this.backgroundScreen != null) {
            this.backgroundScreen.render(-1, -1, partialTicks);
            fill(0, 0, this.width, this.height, 0x66000000);
        } else {
            fill(0, 0, this.width, this.height, 0xFF111111);
        }

        int panelW = OVERLAY_WIDTH + 16;
        int panelH = this.overlayHeight + 8 + 16;
        int panelX = this.overlayX - 8;
        int panelY = this.overlayY - 8;
        renderNineSliceBackground(BACKGROUND_TEXTURE, panelX, panelY, panelW, panelH, BG_BORDER, BG_TEX_W, BG_TEX_H);

        tabHoveredFriends = false;
        tabHoveredPending = false;

        int tabY = this.overlayY - 27;
        int tabButtonWidth = OVERLAY_WIDTH / 2;

        boolean friendsSelected = currentTab == friendsTab;
        boolean friendsHovered = mouseX >= panelX + 8 && mouseX < panelX + 8 + tabButtonWidth
                && mouseY >= tabY && mouseY < tabY + TAB_BAR_HEIGHT;
        if (friendsHovered) tabHoveredFriends = true;
        renderTabButton(panelX + 8, tabY, tabButtonWidth, TAB_BAR_HEIGHT, friendsSelected, friendsHovered,
                I18n.format("gui.friends.tab_friends"));

        boolean pendingSelected = currentTab == pendingTab;
        boolean pendingHovered = mouseX >= panelX + 8 + tabButtonWidth && mouseX < panelX + 8 + OVERLAY_WIDTH
                && mouseY >= tabY && mouseY < tabY + TAB_BAR_HEIGHT;
        if (pendingHovered) tabHoveredPending = true;
        int incomingCount = NetworkHandler.pendingRequests.size();
        int joinReqCount = NetworkHandler.pendingJoinRequests.size();
        String pendingLabel = I18n.format("gui.friends.requests_count", incomingCount);
        if (joinReqCount > 0) {
            pendingLabel += " §6(+" + joinReqCount + ")";
        }
        renderTabButton(panelX + 8 + tabButtonWidth, tabY, tabButtonWidth, TAB_BAR_HEIGHT, pendingSelected, pendingHovered,
                pendingLabel);

        if (currentTab != null) {
            int contentX = panelX + 8;
            int contentY = this.overlayY;
            int contentHeight = panelY + panelH - 8 - contentY;
            currentTab.render(mouseX, mouseY, partialTicks, contentX, contentY, contentY, contentHeight);
        }

        super.render(mouseX, mouseY, partialTicks);

        if (tooltipText != null) {
            this.renderTooltip(tooltipText, tooltipX, tooltipY);
        } else if (tabHoveredFriends) {
            this.renderTooltip(I18n.format("gui.friends.tab_friends"), mouseX, mouseY);
        } else if (tabHoveredPending) {
            this.renderTooltip(I18n.format("gui.friends.requests_count", NetworkHandler.pendingRequests.size()), mouseX, mouseY);
        }

        if (tabHoveredFriends || tabHoveredPending) {
            desiredCursor = EnumCursorType.HAND;
        }

        if (tooltipText != null && desiredCursor == EnumCursorType.DEFAULT) {
            desiredCursor = EnumCursorType.HAND;
        }
        PlatformInput.showCursor(desiredCursor);
    }

    private void renderTabButton(int x, int y, int width, int height, boolean selected, boolean hovered, String text) {
        ResourceLocation tex;
        if (selected || hovered) {
            tex = new ResourceLocation("textures/gui/friends/button_highlighted.png");
        } else {
            tex = new ResourceLocation("textures/gui/friends/button_disabled.png");
        }
        this.mc.getTextureManager().bindTexture(tex);
        GlStateManager.color4f(0.7F, 0.7F, 0.7F, 1.0F);
        blit(x, y, width, height, 0.0F, 0.0F, 200, 20, 200, 20);
        GlStateManager.color4f(1.0F, 1.0F, 1.0F, 1.0F);

        if (selected) {
            // Draw underline
            int textWidth = this.font.getStringWidth(text);
            int underLineWidth = Math.min(textWidth, width - 4);
            int underLineX = x + (width - underLineWidth) / 2;
            int underLineY = y + height - 2;
            fill(underLineX, underLineY, underLineX + underLineWidth, underLineY + 1, 0xFFFFFFFF);
        }

        // Draw text
        int textColor = selected ? 0xFFFFFFFF : (hovered ? 0xFFFFFFAA : 0xFFAAAAAA);
        this.font.drawStringWithShadow(text, x + (width - this.font.getStringWidth(text)) / 2,
                y + (height - 8) / 2, textColor);
    }

    public static void renderNineSliceBackground(ResourceLocation texture, int x, int y, int w, int h,
                                                   int border, int texW, int texH) {
        Minecraft mc = Minecraft.getInstance();
        mc.getTextureManager().bindTexture(texture);

        int edgeW = texW - border * 2;
        int edgeH = texH - border * 2;

        GlStateManager.color4f(1.0F, 1.0F, 1.0F, 1.0F);

        blit(x, y, border, border, 0.0F, 0.0F, border, border, texW, texH);
        blit(x + w - border, y, border, border, (float)(texW - border), 0.0F, border, border, texW, texH);
        blit(x, y + h - border, border, border, 0.0F, (float)(texH - border), border, border, texW, texH);
        blit(x + w - border, y + h - border, border, border, (float)(texW - border), (float)(texH - border), border, border, texW, texH);
        blit(x + border, y, w - border * 2, border, (float)border, 0.0F, edgeW, border, texW, texH);
        blit(x + border, y + h - border, w - border * 2, border, (float)border, (float)(texH - border), edgeW, border, texW, texH);
        blit(x, y + border, border, h - border * 2, 0.0F, (float)border, border, edgeH, texW, texH);
        blit(x + w - border, y + border, border, h - border * 2, (float)(texW - border), (float)border, border, edgeH, texW, texH);
        GlStateManager.color4f(0.4F, 0.4F, 0.4F, 1.0F);
        blit(x + border, y + border, w - border * 2, h - border * 2, (float)border, (float)border, edgeW, edgeH, texW, texH);
        GlStateManager.color4f(1.0F, 1.0F, 1.0F, 1.0F);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int panelX = this.overlayX - 8;
        int panelW = OVERLAY_WIDTH + 16;
        int panelH = this.overlayHeight + 8 + 16;

        int tabY = this.overlayY - 27;
        int panelTop = tabY; // clickable area starts from tab bar top
        int panelBottom = this.overlayY - 8 + panelH;

        if (mouseX < panelX || mouseX > panelX + panelW || mouseY < panelTop || mouseY > panelBottom) {
            this.mc.displayGuiScreen(this.backgroundScreen);
            return true;
        }

        int tabButtonWidth = OVERLAY_WIDTH / 2;

        if (mouseY >= tabY && mouseY < tabY + TAB_BAR_HEIGHT) {
            if (mouseX >= panelX + 8 && mouseX < panelX + 8 + tabButtonWidth) {
                if (currentTab != friendsTab) {
                    currentTab = friendsTab;
                }
                return true;
            }
            if (mouseX >= panelX + 8 + tabButtonWidth && mouseX < panelX + 8 + OVERLAY_WIDTH) {
                if (currentTab != pendingTab) {
                    currentTab = pendingTab;
                }
                return true;
            }
        }

        if (currentTab != null) {
            int contentX = panelX + 8;
            int contentY = this.overlayY;
            int contentH = panelBottom - contentY;
            if (currentTab.mouseClicked(mouseX, mouseY, button, contentX, contentY, contentY, contentH)) {
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (currentTab != null) {
            if (currentTab.mouseScrolled(mouseX, mouseY, amount)) {
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, amount);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.currentTab != null && this.currentTab.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        if (keyCode == 256) {
            this.mc.displayGuiScreen(this.backgroundScreen);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (this.currentTab != null && this.currentTab.charTyped(codePoint, modifiers)) {
            return true;
        }
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public void onClose() {
        this.mc.displayGuiScreen(this.backgroundScreen);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    public FontRenderer getFont() { return this.font; }
    public Minecraft getMinecraft() { return this.mc; }

    public void drawPlayerHead(int x, int y, int w, int h, String username, String skinLocation) {
        drawPlayerHeadStatic(this, x, y, w, h, username, skinLocation);
    }

    void refreshLists() {
    }

    public static void drawPlayerHeadStatic(Screen screen, int x, int y, int w, int h, String username, String skinLocation) {
        ResourceLocation skinLoc;
        if (skinLocation != null && !skinLocation.isEmpty()) {
            skinLoc = new ResourceLocation(skinLocation);
        } else {
            skinLoc = DefaultPlayerSkin.getDefaultSkin(
                    net.lax1dude.eaglercraft.EaglercraftUUID.nameUUIDFromBytes(username.getBytes()));
        }
        Minecraft mc = Minecraft.getInstance();
        mc.getTextureManager().bindTexture(skinLoc);
        GlStateManager.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.enableBlend();
        screen.blit(x, y, w, h, 8.0F, 8.0F, 8, 8, 64, 64);
        screen.blit(x, y, w, h, 40.0F, 8.0F, 8, 8, 64, 64);
    }
}
