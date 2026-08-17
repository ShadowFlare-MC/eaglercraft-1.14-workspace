package net.deltaclient.gui;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;

import java.util.List;

public class NoticeScreen extends Screen {

    private final Screen nextScreen;
    private final String infoMessage;
    private List<String> splitMessage;

    public NoticeScreen(String infoMessage, Screen nextScreen) {
        super(new TranslationTextComponent("Notice"));
        this.infoMessage = infoMessage;
        this.nextScreen = nextScreen;
    }

    @Override
    protected void init() {
        this.addButton(new Button(this.width / 2 - 100, this.height / 2 + 50, 200, 20, I18n.format("Understand"), button -> {
            this.mc.displayGuiScreen(this.nextScreen);
            this.mc.gameSettings.hasReadIt = true;
        }));

        if (this.infoMessage != null) {
            this.splitMessage = this.font.listFormattedStringToWidth(this.infoMessage, 300);
        }
    }

    @Override
    public void render(int mouseX, int mouseY, float partialTicks) {
        this.renderBackground();

        String titleText = TextFormatting.RED.toString() + TextFormatting.BOLD.toString() + "NOTICE";
        this.drawCenteredString(this.font, titleText, this.width / 2, 45, 0xFFFFFFFF);

        if (this.splitMessage != null) {
            int textY = this.height / 2 - 35;
            for (String line : this.splitMessage) {
                this.drawCenteredString(this.font, line, this.width / 2, textY, 0xFFDDDDDD);
                textY += this.font.FONT_HEIGHT + 2; 
            }
        }

        super.render(mouseX, mouseY, partialTicks);
    }
}