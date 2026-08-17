package net.lax1dude.eaglercraft.sp.gui;

import net.lax1dude.eaglercraft.EagRuntime;
import net.lax1dude.eaglercraft.sp.SingleplayerServerController;
import net.lax1dude.eaglercraft.sp.WorkerStartupFailedException;
import net.lax1dude.eaglercraft.sp.ipc.IPCPacket15Crashed;
import net.lax1dude.eaglercraft.sp.ipc.IPCPacket1CIssueDetected;
import net.minecraft.client.gui.screen.MainMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.WorldSelectionScreen;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.text.StringTextComponent;

public class GuiScreenIntegratedServerStartup extends Screen {

    private static final String[] dotDotDot = new String[]{"", ".", "..", "..."};
    private final Screen backScreen;
    private final boolean singleThread;
    private int counter = 0;

    public GuiScreenIntegratedServerStartup(Screen backScreen) {
        super(new StringTextComponent(""));
        this.backScreen = backScreen;
        this.singleThread = false;
    }

    public GuiScreenIntegratedServerStartup(Screen backScreen, boolean singleThread) {
        super(new StringTextComponent(""));
        this.backScreen = backScreen;
        this.singleThread = singleThread;
    }

    protected void init() {
    }

    public void tick() {
        ++counter;
        if (counter == 2) {
            try {
                SingleplayerServerController.startIntegratedServerWorker(singleThread);
            } catch (WorkerStartupFailedException ex) {
                mc.displayGuiScreen(new GuiScreenIntegratedServerFailed(ex.getMessage(), new MainMenuScreen()));
                return;
            }
        } else if (counter > 2) {
            IPCPacket15Crashed[] crashReport = SingleplayerServerController.worldStatusErrors();
            if (crashReport != null) {
                mc.displayGuiScreen(GuiScreenIntegratedServerBusy.createException(new MainMenuScreen(), "singleplayer.failed.notStarted", crashReport));
            } else if (SingleplayerServerController.isIntegratedServerWorkerStarted()) {
                Screen cont = new WorldSelectionScreen(backScreen);
                if (SingleplayerServerController.isRunningSingleThreadMode()) {
                    cont = new GuiScreenIntegratedServerFailed("singleplayer.failed.singleThreadWarning.1", "singleplayer.failed.singleThreadWarning.2", cont);
                } else if (!EagRuntime.getConfiguration().isRamdiskMode()
                        && SingleplayerServerController.isIssueDetected(IPCPacket1CIssueDetected.ISSUE_RAMDISK_MODE)
                        && SingleplayerServerController.canKillWorker()) {
                    cont = new GuiScreenRAMDiskModeDetected(cont);
                }
                mc.displayGuiScreen(cont);
            }
        }
    }

    public boolean isPauseScreen() {
        return false;
    }

    public void render(int i, int j, float f) {
        this.renderDirtBackground(0);
        String txt = I18n.format("singleplayer.integratedStartup");
        int w = this.font.getStringWidth(txt);
        this.drawString(this.font, txt + dotDotDot[(int) ((EagRuntime.steadyTimeMillis() / 300L) % 4L)], (this.width - w) / 2, this.height / 2 - 50, 16777215);
        super.render(i, j, f);
    }

    public boolean canCloseGui() {
        return false;
    }

}