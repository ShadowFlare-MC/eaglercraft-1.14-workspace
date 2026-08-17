package net.deltaclient.friends;

import com.mojang.blaze3d.platform.GlStateManager;
import net.deltaclient.socials.GuiSocialScreen;
import net.deltaclient.network.NetworkHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.resources.I18n;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public class PendingTab extends AbstractFriendsTab {

    private static final int ENTRY_HEIGHT = 28;

    private final FriendsOverlayScreen screen;

    private final List<IncomingEntry> incomingEntries = new ArrayList<>();
    private final List<OutgoingEntry> outgoingEntries = new ArrayList<>();
    private final List<JoinRequestEntry> joinRequestEntries = new ArrayList<>();
    private final List<InviteEntry> inviteEntries = new ArrayList<>();
    private float scrollOffset;

    private enum State { LOADING, EMPTY, LIST, ERROR }
    private State state = State.EMPTY;
    private String errorMessage;

    public PendingTab(FriendsOverlayScreen screen, int width, int height) {
        super(width, height);
        this.screen = screen;
    }

    @Override
    void rearrangeElements() {
    }

    @Override
    public void render(int mouseX, int mouseY, float partialTicks, int x, int y, int contentY, int contentHeight) {
        FontRenderer font = Minecraft.getInstance().fontRenderer;

        // Scissor setup
        GlStateManager.enableScissorTest();
        Minecraft mc = Minecraft.getInstance();
        double scale = mc.mainWindow.getGuiScaleFactor();
        int fbH = mc.mainWindow.getFramebufferHeight();
        int scissorX = (int) (x * scale);
        int scissorY = (int) (fbH - (y + contentHeight) * scale);
        int scissorW = (int) ((width + 10) * scale);
        int scissorH = (int) (contentHeight * scale);
        GlStateManager.scissor(scissorX, scissorY, scissorW, scissorH);

        GlStateManager.pushMatrix();
        GlStateManager.translatef(0, -scrollOffset, 0);

        switch (state) {
            case LOADING:
                renderLoading(font, x, y, width + 10, contentHeight);
                break;
            case EMPTY:
                renderEmpty(font, x, y, width + 10, contentHeight);
                break;
            case ERROR:
                renderError(font, x, y, width + 10, contentHeight);
                break;
            case LIST:
                renderList(mouseX, mouseY, x, y, width + 10);
                break;
        }

        GlStateManager.popMatrix();
        GlStateManager.disableScissorTest();
    }

    private void renderLoading(FontRenderer font, int x, int y, int w, int h) {
        String text = I18n.format("gui.friends.loading_requests");
        font.drawStringWithShadow(text, x + (w - font.getStringWidth(text)) / 2,
                y + h / 2 - font.FONT_HEIGHT / 2, 0xAAAAAA);
    }

    private void renderEmpty(FontRenderer font, int x, int y, int w, int h) {
        String text = I18n.format("gui.friends.pending.empty");
        font.drawStringWithShadow("§7" + text, x + (w - font.getStringWidth(text)) / 2,
                y + h / 2 - font.FONT_HEIGHT / 2, 0xFF888888);
    }

    private void renderError(FontRenderer font, int x, int y, int w, int h) {
        String text = errorMessage != null ? errorMessage : I18n.format("gui.friends.error.generic");
        font.drawStringWithShadow("§c" + text, x + (w - font.getStringWidth("§c" + text)) / 2,
                y + h / 2 - font.FONT_HEIGHT / 2, 0xFF5555);
    }

    private void renderList(int mouseX, int mouseY, int x, int y, int w) {
        int currentY = y;

        if (!incomingEntries.isEmpty()) {
            FontRenderer font = Minecraft.getInstance().fontRenderer;
            String header = I18n.format("gui.friends.pending.received");
            String headerText = "§l§n" + header;
            font.drawStringWithShadow(headerText, x + (w - font.getStringWidth(headerText)) / 2, currentY, 0xFFFFFF);
            currentY += 12;

            for (IncomingEntry entry : incomingEntries) {
                entry.setPosition(x + 2, currentY);
                entry.render(mouseX, (int) (mouseY + scrollOffset), 0);
                currentY += ENTRY_HEIGHT;
            }
            currentY += 4;
        }

        if (!outgoingEntries.isEmpty()) {
            FontRenderer font = Minecraft.getInstance().fontRenderer;
            String header = I18n.format("gui.friends.pending.sent");
            String headerText = "§l§n" + header;
            font.drawStringWithShadow(headerText, x + (w - font.getStringWidth(headerText)) / 2, currentY, 0xFFFFFF);
            currentY += 12;

            for (OutgoingEntry entry : outgoingEntries) {
                entry.setPosition(x + 2, currentY);
                entry.render(mouseX, (int) (mouseY + scrollOffset), 0);
                currentY += ENTRY_HEIGHT;
            }
            currentY += 4;
        }

        // Join requests section
        if (!joinRequestEntries.isEmpty()) {
            FontRenderer font = Minecraft.getInstance().fontRenderer;
            String header = "§6§lJoin Requests";
            font.drawStringWithShadow(header, x + (w - font.getStringWidth(header)) / 2, currentY, 0xFFFFFF);
            currentY += 12;

            for (JoinRequestEntry entry : joinRequestEntries) {
                entry.setPosition(x + 2, currentY);
                entry.render(mouseX, (int) (mouseY + scrollOffset), 0);
                currentY += ENTRY_HEIGHT;
            }
            currentY += 4;
        }

        // Invites section
        if (!inviteEntries.isEmpty()) {
            FontRenderer font = Minecraft.getInstance().fontRenderer;
            String header = "§a§lInvites";
            font.drawStringWithShadow(header, x + (w - font.getStringWidth(header)) / 2, currentY, 0xFFFFFF);
            currentY += 12;

            for (InviteEntry entry : inviteEntries) {
                entry.setPosition(x + 2, currentY);
                entry.render(mouseX, (int) (mouseY + scrollOffset), 0);
                currentY += ENTRY_HEIGHT;
            }
            currentY += 4;
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button, int x, int y, int contentY, int contentHeight) {
        if (state != State.LIST) return false;

        int localMy = (int) (mouseY + scrollOffset);
        int currentY = y;

        if (!incomingEntries.isEmpty()) {
            currentY += 12; // skip header
            for (IncomingEntry entry : incomingEntries) {
                entry.setPosition(x + 2, currentY);
                if (localMy >= currentY && localMy < currentY + ENTRY_HEIGHT) {
                    if (entry.mouseClicked((int) mouseX, localMy, button)) {
                        return true;
                    }
                    return true;
                }
                currentY += ENTRY_HEIGHT;
            }
            currentY += 4;
        }

        if (!outgoingEntries.isEmpty()) {
            currentY += 12; // skip header
            for (OutgoingEntry entry : outgoingEntries) {
                entry.setPosition(x + 2, currentY);
                if (localMy >= currentY && localMy < currentY + ENTRY_HEIGHT) {
                    if (entry.mouseClicked((int) mouseX, localMy, button)) {
                        return true;
                    }
                    return true;
                }
                currentY += ENTRY_HEIGHT;
            }
            currentY += 4;
        }

        // Join requests
        if (!joinRequestEntries.isEmpty()) {
            currentY += 12; // skip header
            for (JoinRequestEntry entry : joinRequestEntries) {
                entry.setPosition(x + 2, currentY);
                if (localMy >= currentY && localMy < currentY + ENTRY_HEIGHT) {
                    if (entry.mouseClicked((int) mouseX, localMy, button)) {
                        return true;
                    }
                    return true;
                }
                currentY += ENTRY_HEIGHT;
            }
            currentY += 4;
        }

        // Invites
        if (!inviteEntries.isEmpty()) {
            currentY += 12; // skip header
            for (InviteEntry entry : inviteEntries) {
                entry.setPosition(x + 2, currentY);
                if (localMy >= currentY && localMy < currentY + ENTRY_HEIGHT) {
                    if (entry.mouseClicked((int) mouseX, localMy, button)) {
                        return true;
                    }
                    return true;
                }
                currentY += ENTRY_HEIGHT;
            }
        }

        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return false;
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (state == State.LIST) {
            int totalContent = (incomingEntries.isEmpty() ? 0 : 12 + incomingEntries.size() * ENTRY_HEIGHT + 4)
                    + (outgoingEntries.isEmpty() ? 0 : 12 + outgoingEntries.size() * ENTRY_HEIGHT + 4)
                    + (joinRequestEntries.isEmpty() ? 0 : 12 + joinRequestEntries.size() * ENTRY_HEIGHT + 4)
                    + (inviteEntries.isEmpty() ? 0 : 12 + inviteEntries.size() * ENTRY_HEIGHT + 4);
            float maxScroll = Math.max(0, totalContent - height);
            scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset - (float) amount * 15));
            return true;
        }
        return false;
    }

    @Override
    public void tick() {
        updateFromSocialManager();
    }

    private void updateFromSocialManager() {
        if (!NetworkHandler.isAuthenticated) {
            state = State.LOADING;
            return;
        }

        incomingEntries.clear();
        outgoingEntries.clear();
        joinRequestEntries.clear();
        inviteEntries.clear();

        for (GuiSocialScreen.SocialPlayerEntry req : NetworkHandler.pendingRequests) {
            incomingEntries.add(new IncomingEntry(screen, req.name,
                    req.skinLocation, true, req.status,
                    0, 0, width, ENTRY_HEIGHT));
        }

        for (GuiSocialScreen.SocialPlayerEntry req : NetworkHandler.pendingRequestsOutgoing) {
            outgoingEntries.add(new OutgoingEntry(screen, req.name,
                    req.skinLocation, true, req.status,
                    0, 0, width, ENTRY_HEIGHT));
        }

        for (NetworkHandler.JoinRequestInfo req : NetworkHandler.pendingJoinRequests) {
            joinRequestEntries.add(new JoinRequestEntry(screen, req.from,
                    "", 0, 0, width, ENTRY_HEIGHT));
        }

        for (NetworkHandler.JoinRequestInfo inv : NetworkHandler.pendingInvites) {
            inviteEntries.add(new InviteEntry(screen, inv.from,
                    "", inv.worldName, inv.code,
                    0, 0, width, ENTRY_HEIGHT));
        }

        if (incomingEntries.isEmpty() && outgoingEntries.isEmpty() && joinRequestEntries.isEmpty() && inviteEntries.isEmpty()) {
            state = State.EMPTY;
        } else {
            state = State.LIST;
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
