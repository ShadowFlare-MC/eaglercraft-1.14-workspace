package net.deltaclient.socials;

import com.mojang.blaze3d.platform.GlStateManager;
import net.deltaclient.network.NetworkHandler;

import java.util.List;

public class SocialsFriendsTab implements ISocialsTab {
    private final GuiSocialScreen screen;
    private String selectedFriend = null;
    
    private String contextMenuFriend = null;
    private int contextMenuX = 0;
    private int contextMenuY = 0;

    public SocialsFriendsTab(GuiSocialScreen screen) {
        this.screen = screen;
    }

    @Override
    public void init(int panelX, int panelY, int panelW, int panelH) {
    }

    @Override
    public void render(int mx, int my, float partialTicks, int panelX, int panelY, int panelW, int entryStartY, int entryHeight) {
        List<GuiSocialScreen.SocialPlayerEntry> entries = NetworkHandler.friends;
        int displayIdx = 0;

        for (int i = 0; i < entries.size(); i++) {
            GuiSocialScreen.SocialPlayerEntry entry = entries.get(i);

            int y = entryStartY + displayIdx * entryHeight;
            boolean hovered = mx >= panelX + 2 && mx < panelX + panelW - 8 && my >= y && my < y + entryHeight;

            if (entry.name.equals(selectedFriend)) {
                screen.drawGradientRect(panelX + 2, y, panelX + panelW - 8, y + entryHeight, 0x66FFFFFF, 0x33FFFFFF);
            } else if (hovered) {
                screen.drawGradientRect(panelX + 2, y, panelX + panelW - 8, y + entryHeight, 0x44FFFFFF, 0x11FFFFFF);
            }
            
            if (i < entries.size() - 1) {
                screen.drawRect(panelX + 15, y + entryHeight - 1, panelX + panelW - 25, y + entryHeight, 0x22FFFFFF);
            }

            screen.drawPlayerHead(panelX + 8, y + 6, 24, 24, entry.name, entry.skinLocation);

            String nameColor = entry.online ? "§a" : "§7";
            screen.drawStringText(screen.getFont(), nameColor + entry.name, panelX + 38, y + 8, 0xFFFFFF);

            String statusColor = entry.online ? "§a" : "§7";
            screen.drawStringText(screen.getFont(), statusColor + entry.status, panelX + 38, y + 18, 0xAAAAAA);

            int dotColor = entry.online ? 0xFF00FF00 : 0xFF666666;
            screen.drawRect(panelX + 4, y + 14, panelX + 7, y + 17, dotColor);

            displayIdx++;
        }

        if (contextMenuFriend != null) {
            GlStateManager.pushMatrix();
            GlStateManager.translatef(0, screen.listScrollOffset, 0); // cancel scissor offset
            int boxW = 80;
            int boxH = 44;
            screen.drawRect(contextMenuX, contextMenuY, contextMenuX + boxW, contextMenuY + boxH, 0xFF222222);
            screen.drawRect(contextMenuX, contextMenuY, contextMenuX + boxW, contextMenuY + 1, 0xFF444444);
            screen.drawRect(contextMenuX, contextMenuY, contextMenuX + 1, contextMenuY + boxH, 0xFF444444);
            screen.drawRect(contextMenuX + boxW - 1, contextMenuY, contextMenuX + boxW, contextMenuY + boxH, 0xFF111111);
            screen.drawRect(contextMenuX, contextMenuY + boxH - 1, contextMenuX + boxW, contextMenuY + boxH, 0xFF111111);

            boolean hMsg = mx >= contextMenuX && mx < contextMenuX + boxW && my >= contextMenuY + 2 && my < contextMenuY + 22;
            if (hMsg) screen.drawRect(contextMenuX + 1, contextMenuY + 2, contextMenuX + boxW - 1, contextMenuY + 22, 0x44FFFFFF);
            screen.drawStringText(screen.getFont(), "Message", contextMenuX + 5, contextMenuY + 8, 0xFFFFFF);

            boolean hRem = mx >= contextMenuX && mx < contextMenuX + boxW && my >= contextMenuY + 22 && my < contextMenuY + 42;
            if (hRem) screen.drawRect(contextMenuX + 1, contextMenuY + 22, contextMenuX + boxW - 1, contextMenuY + 42, 0x44FF3333);
            screen.drawStringText(screen.getFont(), "Remove", contextMenuX + 5, contextMenuY + 28, 0xFF5555);
            GlStateManager.popMatrix();
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button, int panelX, int panelY, int panelW, int entryStartY, int entryHeight) {
        if (contextMenuFriend != null) {
            int boxW = 80;
            int boxH = 44;
            double myUnscrolled = my - screen.listScrollOffset;
            boolean hMsg = mx >= contextMenuX && mx < contextMenuX + boxW && myUnscrolled >= contextMenuY + 2 && myUnscrolled < contextMenuY + 22;
            boolean hRem = mx >= contextMenuX && mx < contextMenuX + boxW && myUnscrolled >= contextMenuY + 22 && myUnscrolled < contextMenuY + 42;
            if (hMsg && button == 0) {
                screen.openConversation(contextMenuFriend);
                contextMenuFriend = null;
                return true;
            }
            if (hRem && button == 0) {
                NetworkHandler.removeFriend(contextMenuFriend);
                contextMenuFriend = null;
                return true;
            }
            contextMenuFriend = null;
            return true;
        }

        List<GuiSocialScreen.SocialPlayerEntry> entries = NetworkHandler.friends;
        int displayIdx = 0;
        for (int i = 0; i < entries.size(); i++) {
            GuiSocialScreen.SocialPlayerEntry entry = entries.get(i);
            int y = entryStartY + displayIdx * entryHeight;
            if (mx >= panelX + 2 && mx < panelX + panelW - 8 && my >= y && my < y + entryHeight) {
                selectedFriend = entry.name;
                if (button == 1) {
                    contextMenuFriend = entry.name;
                    contextMenuX = (int)mx;
                    contextMenuY = (int)(my - screen.listScrollOffset); 
                } else if (button == 0) {
                }
                return true;
            }
            displayIdx++;
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
    public int getListSize() {
        return NetworkHandler.friends.size();
    }

    @Override
    public void tick() {
    }

    @Override
    public void setVisible(boolean visible) {
        if (!visible) {
            contextMenuFriend = null;
        }
    }
}
