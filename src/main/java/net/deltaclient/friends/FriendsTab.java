package net.deltaclient.friends;

import com.mojang.blaze3d.platform.GlStateManager;
import net.deltaclient.socials.GuiSocialScreen;
import net.deltaclient.network.NetworkHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public class FriendsTab extends AbstractFriendsTab {

    private static final ResourceLocation ILLUSTRATION_TEX = new ResourceLocation("textures/gui/friends/illustrations_00.png");

    private final FriendsOverlayScreen screen;
    private final AddFriendWidget addFriendWidget;

    private final List<FriendEntry> entries = new ArrayList<>();
    private float scrollOffset;

    private enum State { LOADING, EMPTY, LIST, ERROR }
    private State state = State.LOADING;
    private String errorMessage;
    private int animTick;

    public FriendsTab(FriendsOverlayScreen screen, int width, int height) {
        super(width, height);
        this.screen = screen;
        this.addFriendWidget = new AddFriendWidget(screen, 0, 0, width + 10);
        this.rearrangeElements();
    }

    @Override
    void rearrangeElements() {
        // Width and height are already set
    }

    @Override
    public void render(int mouseX, int mouseY, float partialTicks, int x, int y, int contentY, int contentHeight) {
        FontRenderer font = Minecraft.getInstance().fontRenderer;

        // Render AddFriendWidget at the top
        addFriendWidget.setPosition(x - 5, y);
        addFriendWidget.render(mouseX, mouseY, partialTicks);

        int listY = y + addFriendWidget.getHeight();
        int listHeight = contentHeight - addFriendWidget.getHeight();
        int listWidth = width + 10;

        switch (state) {
            case LOADING:
                renderLoading(font, x, listY, listWidth, listHeight);
                break;
            case EMPTY:
                renderEmpty(font, x, listY, listWidth, listHeight);
                break;
            case ERROR:
                renderError(font, x, listY, listWidth, listHeight);
                break;
            case LIST:
                renderFriendList(mouseX, mouseY, x, listY, listWidth, listHeight);
                break;
        }
    }

    private void renderLoading(FontRenderer font, int x, int y, int w, int h) {
        String text = I18n.format("gui.friends.loading_friends");
        font.drawStringWithShadow(text, x + (w - font.getStringWidth(text)) / 2,
                y + h / 2 - font.FONT_HEIGHT / 2, 0xAAAAAA);
    }

    private void renderEmpty(FontRenderer font, int x, int y, int w, int h) {
        Minecraft mc = Minecraft.getInstance();

        // Draw illusturation
        mc.getTextureManager().bindTexture(ILLUSTRATION_TEX);
        int illW = 128;
        int illH = 48;
        int illX = x + (w - illW) / 2;
        int illY = y + h / 2 - illH - 10;
        screen.blit(illX, illY, 0, 0, illW, illH, illW, illH);

        // Draw empy state text
        String emptyText = I18n.format("gui.friends.empty_state.link");
        font.drawStringWithShadow(emptyText, x + (w - font.getStringWidth(emptyText)) / 2,
                y + h / 2 + 10, 0xFF555555);

        String mainText = I18n.format("gui.friends.empty_state", "");
        font.drawStringWithShadow("§7" + mainText, x + (w - font.getStringWidth(mainText)) / 2,
                y + h / 2, 0xFF888888);
    }

    private void renderError(FontRenderer font, int x, int y, int w, int h) {
        String text = errorMessage != null ? errorMessage : I18n.format("gui.friends.error.generic");
        font.drawStringWithShadow("§c" + text, x + (w - font.getStringWidth("§c" + text)) / 2,
                y + h / 2 - font.FONT_HEIGHT / 2, 0xFF5555);
    }

    private void renderFriendList(int mouseX, int mouseY, int x, int y, int w, int h) {
        FontRenderer font = Minecraft.getInstance().fontRenderer;
        GlStateManager.enableScissorTest();
        Minecraft mc = Minecraft.getInstance();
        double scale = mc.mainWindow.getGuiScaleFactor();
        int fbH = mc.mainWindow.getFramebufferHeight();
        int scissorX = (int) (x * scale);
        int scissorY = (int) (fbH - (y + h) * scale);
        int scissorW = (int) (w * scale);
        int scissorH = (int) (h * scale);
        GlStateManager.scissor(scissorX, scissorY, scissorW, scissorH);

        int entryHeight = 28;
        int totalContentHeight = entries.size() * entryHeight;
        float maxScroll = Math.max(0, totalContentHeight - h);
        if (scrollOffset > maxScroll) scrollOffset = maxScroll;
        if (scrollOffset < 0) scrollOffset = 0;

        GlStateManager.pushMatrix();
        GlStateManager.translatef(0, -scrollOffset, 0);

        int currentY = y;
        for (FriendEntry entry : entries) {
            entry.setPosition(x + 2, currentY);
            entry.render(mouseX, (int) (mouseY + scrollOffset), 0);
            currentY += entryHeight;
        }

        GlStateManager.popMatrix();
        GlStateManager.disableScissorTest();

        String footer = I18n.format("gui.friends.manage_account_footer", "");
        font.drawStringWithShadow("§7" + footer, x + (w - font.getStringWidth(footer)) / 2,
                y + h - font.FONT_HEIGHT - 4, 0xFF888888);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button, int x, int y, int contentY, int contentHeight) {
        // Try AddFriendWidget first
        if (addFriendWidget.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }

        if (state != State.LIST) return false;

        int listY = y + addFriendWidget.getHeight();
        int listHeight = contentHeight - addFriendWidget.getHeight();

        int entryHeight = 28;
        int localMy = (int) (mouseY + scrollOffset);

        for (FriendEntry entry : entries) {
            int entryY = listY;
            if (localMy >= entryY && localMy < entryY + entryHeight) {
                entry.setPosition(x + 2, entryY);
                if (entry.mouseClicked((int) mouseX, localMy, button)) {
                    return true;
                }
                return true; // Consume click even if not on button
            }
            listY += entryHeight;
        }

        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return addFriendWidget.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        return addFriendWidget.charTyped(codePoint, modifiers);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (state == State.LIST) {
            scrollOffset = Math.max(0, scrollOffset - (float) amount * 15);
            int totalContentHeight = entries.size() * 28;
            float maxScroll = Math.max(0, totalContentHeight - (height - addFriendWidget.getHeight()));
            if (scrollOffset > maxScroll) scrollOffset = maxScroll;
            return true;
        }
        return false;
    }

    @Override
    public void tick() {
        addFriendWidget.tick();
        animTick++;

        updateFromSocialManager();
    }

    private void updateFromSocialManager() {
        if (NetworkHandler.isAuthenticated) {
            if (NetworkHandler.friends.isEmpty()) {
                state = State.EMPTY;
            } else {
                state = State.LIST;
                entries.clear();
                for (GuiSocialScreen.SocialPlayerEntry friend : NetworkHandler.friends) {
                    entries.add(new FriendEntry(screen, friend.name,
                            friend.skinLocation, friend.online, friend.status,
                            0, 0, width, 28));
                }
            }
            errorMessage = null;
        } else {
            state = State.LOADING;
        }

        if (NetworkHandler.lastAuthError != null && !NetworkHandler.lastAuthError.isEmpty()
                && !NetworkHandler.lastAuthError.equals("socials.success.registered")) {
            state = State.ERROR;
            errorMessage = NetworkHandler.lastAuthError;
        }
    }

    public void showLoading() {
        state = State.LOADING;
    }

    public void showEmpty() {
        state = State.EMPTY;
    }

    public void showError(String message) {
        state = State.ERROR;
        errorMessage = message;
    }
}
