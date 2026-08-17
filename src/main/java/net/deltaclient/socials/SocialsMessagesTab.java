package net.deltaclient.socials;

import net.deltaclient.network.NetworkHandler;import net.lax1dude.eaglercraft.profile.EaglerProfile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SocialsMessagesTab implements ISocialsTab {
    private final GuiSocialScreen screen;

    public SocialsMessagesTab(GuiSocialScreen screen) {
        this.screen = screen;
    }

    private static class ConversationEntry {
        String friendName;
        GuiSocialScreen.SocialMessage latestMessage;
        ConversationEntry(String n, GuiSocialScreen.SocialMessage m) {
            this.friendName = n;
            this.latestMessage = m;
        }
    }

    private List<ConversationEntry> getConversations() {
        List<ConversationEntry> list = new ArrayList<>();
        for (Map.Entry<String, List<GuiSocialScreen.SocialMessage>> entry : NetworkHandler.conversations.entrySet()) {
            if (!entry.getValue().isEmpty()) {
                list.add(new ConversationEntry(entry.getKey(), entry.getValue().get(entry.getValue().size() - 1)));
            }
        }
        return list;
    }

    @Override
    public void init(int panelX, int panelY, int panelW, int panelH) {
    }

    @Override
    public void render(int mx, int my, float partialTicks, int panelX, int panelY, int panelW, int entryStartY, int entryHeight) {
        List<String> activeConversations = new java.util.ArrayList<>(NetworkHandler.conversations.keySet());
        List<ConversationEntry> convos = getConversations();

        for (int i = 0; i < convos.size(); i++) {
            ConversationEntry convo = convos.get(i);
            GuiSocialScreen.SocialMessage msg = convo.latestMessage;
            int y = entryStartY + i * entryHeight;
            boolean hovered = mx >= panelX + 2 && mx < panelX + panelW - 8 && my >= y && my < y + entryHeight;

            if (hovered) {
                screen.drawGradientRect(panelX + 2, y, panelX + panelW - 8, y + entryHeight, 0x44FFFFFF, 0x11FFFFFF);
            }
            
            if (i < convos.size() - 1) {
                screen.drawRect(panelX + 15, y + entryHeight - 1, panelX + panelW - 25, y + entryHeight, 0x22FFFFFF);
            }

            if (!msg.read) {
                screen.drawRect(panelX + 3, y + 15, panelX + 6, y + 18, 0xFFFF5555);
            }

            String otherPerson = convo.friendName;
            String msgSkin = "";
            for (GuiSocialScreen.SocialPlayerEntry f : NetworkHandler.friends) {
                if (f.name.equals(otherPerson)) { msgSkin = f.skinLocation; break; }
            }
            screen.drawPlayerHead(panelX + 8, y + 6, 24, 24, otherPerson, msgSkin);

            screen.drawStringText(screen.getFont(), "§b" + otherPerson, panelX + 38, y + 5, 0xFFFFFF);
            String timeAgo = getTimeAgo(msg.timestamp);
            screen.drawStringText(screen.getFont(), "§8" + timeAgo, panelX + panelW - screen.getFont().getStringWidth(timeAgo) - 15, y + 6, 0x888888);

            String myName = NetworkHandler.loggedInUsername != null ? NetworkHandler.loggedInUsername : EaglerProfile.username;
            String senderPrefix = msg.sender.equals(myName) ? "(You) " : "";
            String preview = msg.text.length() > (30 - senderPrefix.length()) ? msg.text.substring(0, 30 - senderPrefix.length()) + "..." : msg.text;
            String previewColor = msg.read ? "§7" : "§f";
            screen.drawStringText(screen.getFont(), previewColor + senderPrefix + preview, panelX + 38, y + 18, 0xCCCCCC);
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button, int panelX, int panelY, int panelW, int entryStartY, int entryHeight) {
        if (button == 0) {
            List<ConversationEntry> convos = getConversations();
            for (int i = 0; i < convos.size(); i++) {
                ConversationEntry convo = convos.get(i);
                int y = entryStartY + i * entryHeight;
                if (mx >= panelX + 2 && mx < panelX + panelW - 8 && my >= y && my < y + entryHeight) {
                    List<GuiSocialScreen.SocialMessage> msgs = NetworkHandler.conversations.get(convo.friendName);
                    if (msgs != null) {
                        for (GuiSocialScreen.SocialMessage m : msgs) {
                            m.read = true;
                        }
                    }
                    NetworkHandler.saveMessages();
                    screen.openConversation(convo.friendName);
                    return true;
                }
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
    public int getListSize() {
        return getConversations().size();
    }

    @Override
    public void tick() {
    }

    @Override
    public void setVisible(boolean visible) {
    }

    private String getTimeAgo(long timestamp) {
        return new java.text.SimpleDateFormat("hh:mm a").format(new java.util.Date(timestamp));
    }
}
