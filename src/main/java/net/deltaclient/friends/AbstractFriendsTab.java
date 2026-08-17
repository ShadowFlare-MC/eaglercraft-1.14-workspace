package net.deltaclient.friends;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public abstract class AbstractFriendsTab {
    protected final int width;
    protected int height;

    public AbstractFriendsTab(int width, int height) {
        this.width = width;
        this.height = height;
    }

    abstract void rearrangeElements();

    void setHeight(int height) {
        this.height = height;
        this.rearrangeElements();
    }

    public abstract void render(int mouseX, int mouseY, float partialTicks, int x, int y, int contentY, int contentHeight);

    public abstract boolean mouseClicked(double mouseX, double mouseY, int button, int x, int y, int contentY, int contentHeight);

    public abstract boolean mouseScrolled(double mouseX, double mouseY, double amount);

    public abstract boolean keyPressed(int keyCode, int scanCode, int modifiers);

    public abstract boolean charTyped(char codePoint, int modifiers);

    public abstract void tick();
}
