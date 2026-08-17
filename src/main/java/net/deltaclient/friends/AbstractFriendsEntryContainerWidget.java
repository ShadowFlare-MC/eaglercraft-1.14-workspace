package net.deltaclient.friends;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.resources.I18n;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public abstract class AbstractFriendsEntryContainerWidget {
    protected static final int FACE_SIZE = 24;
    protected static final int BUTTON_SIZE = 20;

    public final FriendsOverlayScreen screen;
    public final String playerName;
    public final String skinLocation;
    public final boolean online;
    public final String status;

    protected int x;
    protected int y;
    protected final int width;
    protected final int height;
    protected boolean visible = true;

    public AbstractFriendsEntryContainerWidget(FriendsOverlayScreen screen, String playerName,
                                                String skinLocation, boolean online, String status,
                                                int x, int y, int width, int height) {
        this.screen = screen;
        this.playerName = playerName;
        this.skinLocation = skinLocation != null ? skinLocation : "";
        this.online = online;
        this.status = status != null ? status : "";
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int getX() { return x; }
    public int getY() { return y; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public boolean isVisible() {
        return visible;
    }

    public boolean isMouseOver(int mouseX, int mouseY) {
        return visible && mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    public abstract void render(int mouseX, int mouseY, float partialTicks);

    public abstract boolean mouseClicked(int mouseX, int mouseY, int button);

    public abstract void disable();

    protected void renderBase(int mouseX, int mouseY, float partialTicks) {
        if (!visible) return;

        FontRenderer font = Minecraft.getInstance().fontRenderer;

        // Draw player face
        screen.drawPlayerHead(x + 4, y + (height - FACE_SIZE) / 2, FACE_SIZE, FACE_SIZE, playerName, skinLocation);

        // Draw name
        String nameColor = online ? "§a" : "§7";
        font.drawStringWithShadow(nameColor + playerName, x + 4 + FACE_SIZE + 4, y + 4, 0xFFFFFF);

        // Draw status
        String statusColor = online ? "§a" : "§7";
        String statusText = online ? status : I18n.format("gui.friends.presence.status.offline");
        font.drawStringWithShadow(statusColor + statusText, x + 4 + FACE_SIZE + 4, y + 14, 0xAAAAAA);
    }
}
