package net.deltaclient.socials;

public interface ISocialsTab {
    void init(int panelX, int panelY, int panelW, int panelH);
    void render(int mx, int my, float partialTicks, int panelX, int panelY, int panelW, int entryStartY, int entryHeight);
    boolean mouseClicked(double mx, double my, int button, int panelX, int panelY, int panelW, int entryStartY, int entryHeight);
    boolean keyPressed(int keyCode, int scanCode, int modifiers);
    boolean charTyped(char codePoint, int modifiers);
    int getListSize();
    void tick();
    void setVisible(boolean visible);
}
