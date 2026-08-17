package net.lax1dude.eaglercraft.sp.gui;

import net.lax1dude.eaglercraft.EagRuntime;
import net.lax1dude.eaglercraft.sp.SingleplayerServerController;
import net.lax1dude.eaglercraft.sp.ipc.IPCPacket15Crashed;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.MainMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.text.StringTextComponent;

import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;

public class GuiScreenIntegratedServerBusy extends Screen {

    private static final Runnable defaultTerminateAction = () -> {
        if (SingleplayerServerController.canKillWorker()) {
            SingleplayerServerController.killWorker();
            Minecraft.getInstance().displayGuiScreen(new GuiScreenIntegratedServerFailed("singleplayer.failed.killed", new MainMenuScreen()));
        } else {
            EagRuntime.showPopup("Cannot kill worker tasks on desktop runtime!");
        }
    };
    private static final BiConsumer<Screen, IPCPacket15Crashed[]> defaultExceptionAction = (t, u) -> {
        GuiScreenIntegratedServerBusy tt = (GuiScreenIntegratedServerBusy) t;
        Minecraft.getInstance().displayGuiScreen(createException(tt.menu, tt.failMessage, u));
    };
    public final Screen menu;
    public final String failMessage;
    private final it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap<net.minecraft.world.chunk.ChunkStatus> dummyStatuses = new it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap<>();
    private BooleanSupplier checkTaskComplete;
    private Runnable taskKill;
    private String lastStatus;
    private String currentStatus;
    private BiConsumer<Screen, IPCPacket15Crashed[]> onException;
    private int areYouSure;
    private long startStartTime;
    private net.minecraft.client.gui.screen.WorldLoadProgressScreen progressScreen = null;
    private net.minecraft.util.math.ChunkPos dummyCenter = new net.minecraft.util.math.ChunkPos(0, 0);
    private net.minecraft.world.chunk.listener.TrackingChunkStatusListener dummyListener = new net.minecraft.world.chunk.listener.TrackingChunkStatusListener(11) {
        @Override
        public void start(net.minecraft.util.math.ChunkPos center) {
            dummyCenter = center;
        }

        @Override
        public void statusChanged(net.minecraft.util.math.ChunkPos pos, net.minecraft.world.chunk.ChunkStatus status) {
            if (status == null) {
                dummyStatuses.remove(pos.asLong());
            } else {
                dummyStatuses.put(pos.asLong(), status);
            }
        }

        @Override
        public void func_219521_a() {
            dummyStatuses.clear();
        }

        @Override
        public void stop() {
        }

        @Override
        public net.minecraft.world.chunk.ChunkStatus func_219525_a(int x, int y) {
            int radius = 11;
            int e = radius + net.minecraft.world.chunk.ChunkStatus.func_222600_b();
            return dummyStatuses.get(net.minecraft.util.math.ChunkPos.asLong(x + dummyCenter.x - e, y + dummyCenter.z - e));
        }

        @Override
        public int getPercentDone() {
            float prog = net.lax1dude.eaglercraft.sp.SingleplayerServerController.worldStatusProgress();
            if (prog < 0.0f) prog = 0.0f;
            if (prog > 1.0f) prog = 1.0f;
            return (int) (prog * 100.0f);
        }
    };
    private String lastChunkData = "";

    public GuiScreenIntegratedServerBusy(Screen menu, String progressMessage, String failMessage, BooleanSupplier checkTaskComplete) {
        this(menu, progressMessage, failMessage, checkTaskComplete, defaultExceptionAction, defaultTerminateAction);
    }

    public GuiScreenIntegratedServerBusy(Screen menu, String progressMessage, String failMessage, BooleanSupplier checkTaskComplete, BiConsumer<Screen, IPCPacket15Crashed[]> exceptionAction) {
        this(menu, progressMessage, failMessage, checkTaskComplete, exceptionAction, defaultTerminateAction);
    }

    public GuiScreenIntegratedServerBusy(Screen menu, String progressMessage, String failMessage, BooleanSupplier checkTaskComplete, Runnable onTerminate) {
        this(menu, progressMessage, failMessage, checkTaskComplete, defaultExceptionAction, onTerminate);
    }

    public GuiScreenIntegratedServerBusy(Screen menu, String progressMessage, String failMessage, BooleanSupplier checkTaskComplete, BiConsumer<Screen, IPCPacket15Crashed[]> onException, Runnable onTerminate) {
        super(new StringTextComponent(""));
        this.menu = menu;
        this.failMessage = failMessage;
        this.checkTaskComplete = checkTaskComplete;
        this.onException = onException;
        this.taskKill = onTerminate;
        this.lastStatus = SingleplayerServerController.worldStatusString();
        this.currentStatus = progressMessage;
    }

    public static Screen createException(Screen ok, String msg, IPCPacket15Crashed[] exceptions) {
        ok = new GuiScreenIntegratedServerFailed(msg, ok);
        if (exceptions != null) {
            for (int i = exceptions.length - 1; i >= 0; --i) {
                ok = new GuiScreenIntegratedServerCrashed(ok, exceptions[i].crashReport);
            }
        }
        return ok;
    }

    protected void init() {
        if (startStartTime == 0) this.startStartTime = EagRuntime.steadyTimeMillis();
        areYouSure = 0;
    }

    public boolean isPauseScreen() {
        return false;
    }

    private net.minecraft.world.chunk.ChunkStatus getStatusFromChar(char c) {
        switch (c) {
            case '0':
                return net.minecraft.world.chunk.ChunkStatus.EMPTY;
            case '1':
                return net.minecraft.world.chunk.ChunkStatus.STRUCTURE_STARTS;
            case '2':
                return net.minecraft.world.chunk.ChunkStatus.STRUCTURE_REFERENCES;
            case '3':
                return net.minecraft.world.chunk.ChunkStatus.BIOMES;
            case '4':
                return net.minecraft.world.chunk.ChunkStatus.NOISE;
            case '5':
                return net.minecraft.world.chunk.ChunkStatus.SURFACE;
            case '6':
                return net.minecraft.world.chunk.ChunkStatus.CARVERS;
            case '7':
                return net.minecraft.world.chunk.ChunkStatus.LIQUID_CARVERS;
            case '8':
                return net.minecraft.world.chunk.ChunkStatus.FEATURES;
            case '9':
                return net.minecraft.world.chunk.ChunkStatus.LIGHT;
            case 'A':
                return net.minecraft.world.chunk.ChunkStatus.SPAWN;
            case 'B':
                return net.minecraft.world.chunk.ChunkStatus.HEIGHTMAPS;
            case 'C':
                return net.minecraft.world.chunk.ChunkStatus.FULL;
            default:
                return null;
        }
    }

    public void render(int par1, int par2, float par3) {
        float prog = SingleplayerServerController.worldStatusProgress();
        String str = SingleplayerServerController.worldStatusString();

        if (str != null && str.startsWith("chk:")) {
            if (!str.equals(lastChunkData)) {
                lastChunkData = str;
                String[] parts = str.split(":");
                if (parts.length >= 4) {
                    int cx = Integer.parseInt(parts[1]);
                    int cz = Integer.parseInt(parts[2]);
                    String mapData = parts[3];

                    dummyListener.start(new net.minecraft.util.math.ChunkPos(cx, cz));

                    int radius = 11;
                    int e = radius + net.minecraft.world.chunk.ChunkStatus.func_222600_b();
                    int diameter = e * 2 + 1;

                    for (int x = 0; x < diameter; ++x) {
                        for (int z = 0; z < diameter; ++z) {
                            int idx = x * diameter + z;
                            if (idx < mapData.length()) {
                                char c = mapData.charAt(idx);
                                net.minecraft.world.chunk.ChunkStatus status = getStatusFromChar(c);
                                net.minecraft.util.math.ChunkPos p = new net.minecraft.util.math.ChunkPos(x - e + cx, z - e + cz);
                                dummyListener.statusChanged(p, status);
                            }
                        }
                    }
                }
            }
            str = "singleplayer.busy.startingIntegratedServer"; // Hide the raw string visually
            currentStatus = str;
        }

        if (prog <= 1.0f) {
            if (progressScreen == null) {
                progressScreen = new net.minecraft.client.gui.screen.WorldLoadProgressScreen(dummyListener);
                progressScreen.init(this.mc, this.width, this.height);
            }
            progressScreen.render(par1, par2, par3);
            if (areYouSure > 0) {
                this.drawCenteredString(font, I18n.format("singleplayer.busy.cancelWarning"), this.width / 2, 70, 0xFF8888);
            }
            super.render(par1, par2, par3);
            return;
        }

        this.renderBackground();
        int top = this.height / 3;
        long millis = EagRuntime.steadyTimeMillis();
        str = I18n.format(currentStatus);
        long dots = (millis / 500l) % 4l;
        this.drawString(font, str + (dots > 0 ? "." : "") + (dots > 1 ? "." : "") + (dots > 2 ? "." : ""), (this.width - this.font.getStringWidth(str)) / 2, top + 10, 0xFFFFFF);

        if (areYouSure > 0) {
            this.drawCenteredString(font, I18n.format("singleplayer.busy.cancelWarning"), this.width / 2, top + 25, 0xFF8888);
        } else {
            if (this.currentStatus.equals(this.lastStatus) && prog > 0.01f) {
                this.drawCenteredString(font, (prog > 1.0f ? ("(" + (prog > 1000000.0f ? "" + (int) (prog / 1000000.0f) + "MB" :
                        (prog > 1000.0f ? "" + (int) (prog / 1000.0f) + "kB" : "" + (int) prog + "B")) + ")") : "" + (int) (prog * 100.0f) + "%"), this.width / 2, top + 25, 0xFFFFFF);
            } else {
                long elapsed = (millis - startStartTime) / 1000l;
                if (elapsed > 3) {
                    this.drawCenteredString(font, "(" + elapsed + "s)", this.width / 2, top + 25, 0xFFFFFF);
                }
            }
        }

        super.render(par1, par2, par3);
    }

    public void tick() {
        if (SingleplayerServerController.didLastCallFail() || !SingleplayerServerController.isIntegratedServerWorkerAlive()) {
            onException.accept(this, SingleplayerServerController.worldStatusErrors());
            return;
        }
        if (checkTaskComplete.getAsBoolean()) {
            this.mc.displayGuiScreen(menu);
        }
        String str = SingleplayerServerController.worldStatusString();
        if (!lastStatus.equals(str)) {
            lastStatus = str;
            currentStatus = str;
        }
        if (areYouSure > 0) {
            --areYouSure;
        }
    }

    public boolean shouldHangupIntegratedServer() {
        return false;
    }

    public boolean canCloseGui() {
        return false;
    }

}