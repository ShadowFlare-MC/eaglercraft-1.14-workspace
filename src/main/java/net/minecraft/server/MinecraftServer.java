package net.minecraft.server;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.JsonElement;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.GameProfileRepository;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import com.mojang.datafixers.DataFixer;
import it.unimi.dsi.fastutil.longs.LongIterator;
import net.deltaclient.CompletableFuture;
import net.lax1dude.eaglercraft.EagUtils;
import net.lax1dude.eaglercraft.IOUtils;
import net.lax1dude.eaglercraft.Random;
import net.lax1dude.eaglercraft.internal.vfs2.VFile2;
import net.minecraft.advancements.AdvancementManager;
import net.minecraft.advancements.FunctionManager;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.ICommandSource;
import net.minecraft.crash.CrashReport;
import net.minecraft.crash.ReportedException;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.crafting.RecipeManager;
import net.minecraft.network.NetworkSystem;
import net.minecraft.network.ServerStatusResponse;
import net.minecraft.network.play.server.SServerDifficultyPacket;
import net.minecraft.network.play.server.SUpdateTimePacket;
import net.minecraft.profiler.DebugProfiler;
import net.minecraft.profiler.ISnooperInfo;
import net.minecraft.profiler.Snooper;
import net.minecraft.resources.*;
import net.minecraft.scoreboard.ScoreboardSaveData;
import net.minecraft.scoreboard.ServerScoreboard;
import net.minecraft.server.management.OpEntry;
import net.minecraft.server.management.PlayerList;
import net.minecraft.server.management.PlayerProfileCache;
import net.minecraft.server.management.WhiteList;
import net.minecraft.tags.NetworkTagManager;
import net.minecraft.util.*;
import net.minecraft.util.concurrent.RecursiveEventLoop;
import net.minecraft.util.concurrent.TickDelayedTask;
import net.minecraft.util.math.*;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.*;
import net.minecraft.world.chunk.listener.IChunkStatusListener;
import net.minecraft.world.chunk.listener.IChunkStatusListenerFactory;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.server.ServerChunkProvider;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.server.TicketType;
import net.minecraft.world.storage.*;
import net.minecraft.world.storage.loot.LootTableManager;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.commons.lang3.Validate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.function.BooleanSupplier;

public abstract class MinecraftServer extends RecursiveEventLoop<TickDelayedTask> implements ISnooperInfo, ICommandSource, AutoCloseable, Runnable {
    private static final Logger LOGGER = LogManager.getLogger();
    public static final VFile2 USER_CACHE_FILE = new VFile2("usercache.json");
    private static final CompletableFuture<Unit> field_223713_i = CompletableFuture.completedFuture(Unit.INSTANCE);
    public static final WorldSettings DEMO_WORLD_SETTINGS = (new WorldSettings((long) "North Carolina".hashCode(), GameType.SURVIVAL, true, false, WorldType.DEFAULT)).enableBonusChest();
    private final SaveFormat anvilConverterForAnvilFile;
    private final Snooper snooper = new Snooper("server", this, Util.milliTime());
    private final VFile2 anvilFile;
    private final List<Runnable> tickables = Lists.newArrayList();
    private final DebugProfiler profiler = new DebugProfiler(this::getTickCounter);
    private final NetworkSystem networkSystem;
    protected final IChunkStatusListenerFactory chunkStatusListenerFactory;
    private final ServerStatusResponse statusResponse = new ServerStatusResponse();
    private final Random random = new Random();
    private final DataFixer dataFixer;
    private String hostname;
    private int serverPort = -1;
    private final Map<DimensionType, ServerWorld> worlds = Maps.newIdentityHashMap();
    private PlayerList playerList;
    private volatile boolean serverRunning = true;
    private boolean serverStopped;
    private int tickCounter;
    private boolean onlineMode;
    private boolean preventProxyConnections;
    private boolean canSpawnAnimals;
    private boolean canSpawnNPCs;
    private boolean pvpEnabled;
    private boolean allowFlight;

    private String motd;
    private int buildLimit;
    private int maxPlayerIdleMinutes;
    public final long[] tickTimeArray = new long[100];

    private Object serverKeyPair;

    private String serverOwner;
    private final String folderName;

    @OnlyIn(Dist.CLIENT)
    private String worldName;
    private boolean isDemo;
    private boolean enableBonusChest;
    private String resourcePackUrl = "";
    private String resourcePackHash = "";
    private volatile boolean serverIsRunning;
    private long timeOfLastWarning;

    private ITextComponent userMessage;
    private boolean startProfiling;
    private boolean isGamemodeForced;

    private final YggdrasilAuthenticationService authService;
    private final MinecraftSessionService sessionService;
    private final GameProfileRepository profileRepo;
    private final PlayerProfileCache profileCache;
    private long nanoTimeSinceStatusRefresh;
    protected final Thread serverThread = Util.make(new Thread(this, "Server thread"), (p_213187_0_) -> {
        p_213187_0_.setUncaughtExceptionHandler((p_213206_0_, p_213206_1_) -> {
            LOGGER.error(p_213206_1_);
        });
    });
    private long serverTime = Util.milliTime();
    private long runTasksUntil;
    private boolean isRunningScheduledTasks;
    @OnlyIn(Dist.CLIENT)
    private boolean worldIconSet;
    private final IReloadableResourceManager resourceManager = new SimpleReloadableResourceManager(ResourcePackType.SERVER_DATA, this.serverThread);
    private final ResourcePackList<ResourcePackInfo> resourcePacks = new ResourcePackList<>(ResourcePackInfo::new);

    private FolderPackFinder datapackFinder;
    private final Commands commandManager;
    private final RecipeManager recipeManager = new RecipeManager();
    private final NetworkTagManager networkTagManager = new NetworkTagManager();
    private final ServerScoreboard scoreboard = new ServerScoreboard(this);
    private final CustomServerBossInfoManager customBossEvents = new CustomServerBossInfoManager(this);
    private final LootTableManager lootTableManager = new LootTableManager();
    private final AdvancementManager advancementManager = new AdvancementManager();
    private final FunctionManager functionManager = new FunctionManager(this);
    private final FrameTimer frameTimer = new FrameTimer();
    private boolean whitelistEnabled;
    private boolean forceWorldUpgrade;
    private boolean eraseCache;
    private float tickTime;
    private final Executor backgroundExecutor;

    private String serverId;

    public MinecraftServer(VFile2 p_i50590_1_, DataFixer dataFixerIn, Commands p_i50590_4_, YggdrasilAuthenticationService p_i50590_5_, MinecraftSessionService p_i50590_6_, GameProfileRepository p_i50590_7_, PlayerProfileCache p_i50590_8_, IChunkStatusListenerFactory p_i50590_9_, String p_i50590_10_) {
        super("Server");
        this.commandManager = p_i50590_4_;
        this.authService = p_i50590_5_;
        this.sessionService = p_i50590_6_;
        this.profileRepo = p_i50590_7_;
        this.profileCache = p_i50590_8_;
        this.anvilFile = p_i50590_1_;
        this.networkSystem = new NetworkSystem(this);
        this.chunkStatusListenerFactory = p_i50590_9_;
        this.anvilConverterForAnvilFile = new net.lax1dude.eaglercraft.sp.server.EaglerSaveFormat(p_i50590_1_, dataFixerIn);
        this.dataFixer = dataFixerIn;
        this.resourceManager.addReloadListener(this.networkTagManager);
        this.resourceManager.addReloadListener(this.recipeManager);
        this.resourceManager.addReloadListener(this.lootTableManager);
        this.resourceManager.addReloadListener(this.functionManager);
        this.resourceManager.addReloadListener(this.advancementManager);
        this.backgroundExecutor = Util.getServerExecutor();
        this.folderName = p_i50590_10_;
    }

    private void func_213204_a(DimensionSavedDataManager p_213204_1_) {
        ScoreboardSaveData scoreboardsavedata = p_213204_1_.getOrCreate(ScoreboardSaveData::new, "scoreboard");
        scoreboardsavedata.setScoreboard(this.getScoreboard());
        this.getScoreboard().addDirtyRunnable(new WorldSavedDataCallableSave(scoreboardsavedata));
    }

    protected abstract boolean init() throws IOException;

    protected void convertMapIfNeeded(String worldNameIn) {
        if (this.getActiveAnvilConverter().isOldMapFormat(worldNameIn)) {
            LOGGER.info("Converting map!");
            this.setUserMessage(new TranslationTextComponent("menu.convertingLevel"));
            this.getActiveAnvilConverter().convertMapFormat(worldNameIn, new IProgressUpdate() {
                private long startTime = Util.milliTime();

                public void displaySavingString(ITextComponent component) {
                }

                @OnlyIn(Dist.CLIENT)
                public void resetProgressAndMessage(ITextComponent component) {
                }

                public void setLoadingProgress(int progress) {
                    if (Util.milliTime() - this.startTime >= 1000L) {
                        this.startTime = Util.milliTime();
                        MinecraftServer.LOGGER.info("Converting... {}%", (int) progress);
                    }

                }

                @OnlyIn(Dist.CLIENT)
                public void setDoneWorking() {
                }

                public void displayLoadingString(ITextComponent component) {
                }
            });
        }

        if (this.forceWorldUpgrade) {
            LOGGER.info("Forcing world upgrade!");
            WorldInfo worldinfo = this.getActiveAnvilConverter().getWorldInfo(this.getFolderName());
            if (worldinfo != null) {
                WorldOptimizer worldoptimizer = new WorldOptimizer(this.getFolderName(), this.getActiveAnvilConverter(), worldinfo, this.eraseCache);
                ITextComponent itextcomponent = null;

                while (!worldoptimizer.isFinished()) {
                    ITextComponent itextcomponent1 = worldoptimizer.getStatusText();
                    if (itextcomponent != itextcomponent1) {
                        itextcomponent = itextcomponent1;
                        LOGGER.info(worldoptimizer.getStatusText().getString());
                    }

                    int i = worldoptimizer.getTotalChunks();
                    if (i > 0) {
                        int j = worldoptimizer.getConverted() + worldoptimizer.getSkipped();
                        LOGGER.info("{}% completed ({} / {} chunks)...", MathHelper.floor((float) j / (float) i * 100.0F), j, i);
                    }

                    if (this.isServerStopped()) {
                        worldoptimizer.cancel();
                    } else {
                        EagUtils.sleep(1000L);
                    }
                }
            }
        }

    }

    protected synchronized void setUserMessage(ITextComponent userMessageIn) {
        this.userMessage = userMessageIn;
    }

    protected void loadAllWorlds(String saveName, String worldNameIn, long seed, WorldType type, JsonElement generatorOptions) {
        this.convertMapIfNeeded(saveName);
        this.setUserMessage(new TranslationTextComponent("menu.loadingLevel"));
        SaveHandler savehandler = this.getActiveAnvilConverter().getSaveLoader(saveName, this);
        this.setResourcePackFromWorld(this.getFolderName(), savehandler);
        WorldInfo worldinfo = savehandler.loadWorldInfo();
        WorldSettings worldsettings;
        if (worldinfo == null) {
            if (this.isDemo()) {
                worldsettings = DEMO_WORLD_SETTINGS;
            } else {
                worldsettings = new WorldSettings(seed, this.getGameType(), this.canStructuresSpawn(), this.isHardcore(), type);
                worldsettings.setGeneratorOptions(generatorOptions);
                if (this.enableBonusChest) {
                    worldsettings.enableBonusChest();
                }
            }

            worldinfo = new WorldInfo(worldsettings, worldNameIn);
        } else {
            worldinfo.setWorldName(worldNameIn);
            worldsettings = new WorldSettings(worldinfo);
        }

        this.loadDataPacks(savehandler.getWorldDirectory(), worldinfo);
        IChunkStatusListener ichunkstatuslistener = this.chunkStatusListenerFactory.create(11);
        this.loadWorlds(savehandler, worldinfo, worldsettings, ichunkstatuslistener);
        this.setDifficultyForAllWorlds(this.getDifficulty(), true);
        this.loadInitialChunks(ichunkstatuslistener);
    }

    protected void loadWorlds(SaveHandler p_213194_1_, WorldInfo info, WorldSettings p_213194_3_, IChunkStatusListener p_213194_4_) {
        if (this.isDemo()) {
            info.populateFromWorldSettings(DEMO_WORLD_SETTINGS);
        }

        ServerWorld serverworld = new ServerWorld(this, this.backgroundExecutor, p_213194_1_, info, DimensionType.OVERWORLD, this.profiler, p_213194_4_);
        this.worlds.put(DimensionType.OVERWORLD, serverworld);
        this.func_213204_a(serverworld.getSavedData());
        serverworld.getWorldBorder().copyFrom(info);
        ServerWorld serverworld1 = this.getWorld(DimensionType.OVERWORLD);
        if (!info.isInitialized()) {
            try {
                serverworld1.createSpawnPosition(p_213194_3_);
                if (info.getGenerator() == WorldType.DEBUG_ALL_BLOCK_STATES) {
                    this.applyDebugWorldInfo(info);
                }

                info.setInitialized(true);
            } catch (Throwable throwable) {
                CrashReport crashreport = CrashReport.makeCrashReport(throwable, "Exception initializing level");

                try {
                    serverworld1.fillCrashReport(crashreport);
                } catch (Throwable var10) {
                    ;
                }

                throw new ReportedException(crashreport);
            }

            info.setInitialized(true);
        }

        this.getPlayerList().func_212504_a(serverworld1);
        if (info.getCustomBossEvents() != null) {
            this.getCustomBossEvents().read(info.getCustomBossEvents());
        }

        for (DimensionType dimensiontype : DimensionType.getAll()) {
            if (dimensiontype != DimensionType.OVERWORLD) {
                this.worlds.put(dimensiontype, new ServerMultiWorld(serverworld1, this, this.backgroundExecutor, p_213194_1_, dimensiontype, this.profiler, p_213194_4_));
            }
        }

    }

    private void applyDebugWorldInfo(WorldInfo worldInfoIn) {
        worldInfoIn.setMapFeaturesEnabled(false);
        worldInfoIn.setAllowCommands(true);
        worldInfoIn.setRaining(false);
        worldInfoIn.setThundering(false);
        worldInfoIn.setClearWeatherTime(1000000000);
        worldInfoIn.setDayTime(6000L);
        worldInfoIn.setGameType(GameType.SPECTATOR);
        worldInfoIn.setHardcore(false);
        worldInfoIn.setDifficulty(Difficulty.PEACEFUL);
        worldInfoIn.setDifficultyLocked(true);
        worldInfoIn.getGameRulesInstance().get(GameRules.DO_DAYLIGHT_CYCLE).set(false, this);
    }

    protected void loadDataPacks(VFile2 p_195560_1_, WorldInfo p_195560_2_) {
        this.resourcePacks.addPackFinder(new ServerPackFinder());
        this.datapackFinder = new FolderPackFinder(new VFile2(p_195560_1_, "datapacks"));
        this.resourcePacks.addPackFinder(this.datapackFinder);
        this.resourcePacks.reloadPacksFromFinders();
        List<ResourcePackInfo> list = Lists.newArrayList();

        for (String s : p_195560_2_.getEnabledDataPacks()) {
            ResourcePackInfo resourcepackinfo = this.resourcePacks.getPackInfo(s);
            if (resourcepackinfo != null) {
                list.add(resourcepackinfo);
            } else {
                LOGGER.warn("Missing data pack {}", (Object) s);
            }
        }

        this.resourcePacks.setEnabledPacks(list);
        this.loadDataPacks(p_195560_2_);
    }

    protected void loadInitialChunks(IChunkStatusListener p_213186_1_) {
        this.setUserMessage(new TranslationTextComponent("menu.generatingTerrain"));
        ServerWorld serverworld = this.getWorld(DimensionType.OVERWORLD);
        LOGGER.info("Preparing start region for dimension " + DimensionType.getKey(serverworld.dimension.getType()));
        BlockPos blockpos = serverworld.getSpawnPoint();
        p_213186_1_.start(new ChunkPos(blockpos));
        ServerChunkProvider serverchunkprovider = serverworld.getChunkProvider();
        serverchunkprovider.getLightManager().func_215598_a(500);
        this.serverTime = Util.milliTime();
        serverchunkprovider.func_217228_a(TicketType.START, new ChunkPos(blockpos), 11, Unit.INSTANCE);

        while (serverchunkprovider.func_217229_b() != 441) {
            this.serverTime = Util.milliTime() + 10L;
            this.runScheduledTasks();
        }

        this.serverTime = Util.milliTime() + 10L;
        this.runScheduledTasks();

        for (DimensionType dimensiontype : DimensionType.getAll()) {
            ForcedChunksSaveData forcedchunkssavedata = this.getWorld(dimensiontype).getSavedData().get(ForcedChunksSaveData::new, "chunks");
            if (forcedchunkssavedata != null) {
                ServerWorld serverworld1 = this.getWorld(dimensiontype);
                LongIterator longiterator = forcedchunkssavedata.getChunks().iterator();

                while (longiterator.hasNext()) {
                    long i = longiterator.nextLong();
                    ChunkPos chunkpos = new ChunkPos(i);
                    serverworld1.getChunkProvider().forceChunk(chunkpos, true);
                }
            }
        }

        this.serverTime = Util.milliTime() + 10L;
        this.runScheduledTasks();
        p_213186_1_.stop();
        serverchunkprovider.getLightManager().func_215598_a(5);
    }

    protected void setResourcePackFromWorld(String worldNameIn, SaveHandler saveHandlerIn) {
        VFile2 file1 = new VFile2(saveHandlerIn.getWorldDirectory(), "resources.zip");
        if (file1.exists()) {
            try {
                this.setResourcePack("level://" + URLEncoder.encode(worldNameIn, StandardCharsets.UTF_8.toString()) + "/" + "resources.zip", "");
            } catch (UnsupportedEncodingException var5) {
                LOGGER.warn("Something went wrong url encoding {}", (Object) worldNameIn);
            }
        }

    }

    public abstract boolean canStructuresSpawn();

    public abstract GameType getGameType();

    public abstract Difficulty getDifficulty();

    public abstract boolean isHardcore();

    public abstract int getOpPermissionLevel();

    public abstract int func_223707_k();

    public abstract boolean allowLoggingRcon();

    public boolean save(boolean suppressLog, boolean flush, boolean forced) {
        boolean flag = false;

        for (ServerWorld serverworld : this.getWorlds()) {
            if (serverworld == null) continue;
            if (!suppressLog) {
                LOGGER.info("Saving chunks for level '{}'/{}", serverworld.getWorldInfo().getWorldName(), DimensionType.getKey(serverworld.dimension.getType()));
            }

            try {
                serverworld.save((IProgressUpdate) null, flush, serverworld.disableLevelSaving && !forced);
            } catch (SessionLockException sessionlockexception) {
                LOGGER.warn(sessionlockexception.getMessage());
            }

            flag = true;
        }

        ServerWorld serverworld1 = this.getWorld(DimensionType.OVERWORLD);
        if (serverworld1 != null) {
            WorldInfo worldinfo = serverworld1.getWorldInfo();
            serverworld1.getWorldBorder().copyTo(worldinfo);
            worldinfo.setCustomBossEvents(this.getCustomBossEvents().write());
            serverworld1.getSaveHandler().saveWorldInfoWithPlayer(worldinfo, this.getPlayerList().getHostPlayerData());
        }
        return flag;
    }

    public void close() {
        this.stopServer();
    }

    public void stopServer() {
        LOGGER.info("Stopping server");
        if (this.getNetworkSystem() != null) {
            this.getNetworkSystem().terminateEndpoints();
        }

        if (this.playerList != null) {
            LOGGER.info("Saving players");
            this.playerList.saveAllPlayerData();
            this.playerList.removeAllPlayers();
        }

        LOGGER.info("Saving worlds");

        for (ServerWorld serverworld : this.getWorlds()) {
            if (serverworld != null) {
                serverworld.disableLevelSaving = false;
            }
        }

        this.save(false, true, false);

        for (ServerWorld serverworld1 : this.getWorlds()) {
            if (serverworld1 != null) {
                try {
                    serverworld1.close();
                } catch (IOException ioexception) {
                    LOGGER.error("Exception closing the level", (Throwable) ioexception);
                }
            }
        }

        if (this.snooper.isSnooperRunning()) {
            this.snooper.stop();
        }

    }

    public String getServerHostname() {
        return this.hostname;
    }

    public void setHostname(String host) {
        this.hostname = host;
    }

    public boolean isServerRunning() {
        return this.serverRunning;
    }

    public void initiateShutdown(boolean p_71263_1_) {
        this.serverRunning = false;
    }

    public void run() {
        try {
            if (this.init()) {
                this.serverTime = Util.milliTime();
                this.statusResponse.setServerDescription(new StringTextComponent(this.motd));
                this.statusResponse.setVersion(new ServerStatusResponse.Version(SharedConstants.getVersion().getName(), SharedConstants.getVersion().getProtocolVersion()));
                this.applyServerIconToResponse(this.statusResponse);

                while (this.serverRunning) {
                    long i = Util.milliTime() - this.serverTime;
                    if (i > 2000L && this.serverTime - this.timeOfLastWarning >= 15000L) {
                        long j = i / 50L;
                        LOGGER.warn("Can't keep up! Is the server overloaded? Running {}ms or {} ticks behind", i, j);
                        this.serverTime += j * 50L;
                        this.timeOfLastWarning = this.serverTime;
                    }

                    this.serverTime += 50L;
                    if (this.startProfiling) {
                        this.startProfiling = false;
                        this.profiler.func_219899_d().func_219939_d();
                    }

                    this.profiler.startTick();
                    this.profiler.startSection("tick");
                    this.tick(this::isAheadOfTime);
                    this.profiler.endStartSection("nextTickWait");
                    this.isRunningScheduledTasks = true;
                    this.runTasksUntil = Math.max(Util.milliTime() + 50L, this.serverTime);
                    this.runScheduledTasks();
                    this.profiler.endSection();
                    this.profiler.endTick();
                    this.serverIsRunning = true;
                }
            } else {
                this.finalTick((CrashReport) null);
            }
        } catch (Throwable throwable1) {
            LOGGER.error("Encountered an unexpected exception", throwable1);
            CrashReport crashreport;
            if (throwable1 instanceof ReportedException) {
                crashreport = this.addServerInfoToCrashReport(((ReportedException) throwable1).getCrashReport());
            } else {
                crashreport = this.addServerInfoToCrashReport(new CrashReport("Exception in server tick loop", throwable1));
            }

            VFile2 file1 = new VFile2(new VFile2(this.getDataDirectory(), "crash-reports"), "crash-" + (new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss")).format(new Date()) + "-server.txt");
            if (crashreport.saveToFile(file1)) {
                LOGGER.error("This crash report has been saved to: {}", (Object) file1.getPath());
            } else {
                LOGGER.error("We were unable to save this crash report to disk.");
            }

            this.finalTick(crashreport);
        } finally {
            try {
                this.serverStopped = true;
                this.stopServer();
            } catch (Throwable throwable) {
                LOGGER.error("Exception stopping the server", throwable);
            } finally {
                this.systemExitNow();
            }

        }

    }

    private boolean isAheadOfTime() {
        return this.isTaskRunning() || Util.milliTime() < (this.isRunningScheduledTasks ? this.runTasksUntil : this.serverTime);
    }

    protected void runScheduledTasks() {
        this.drainTasks();
        this.driveUntil(() -> {
            return !this.isAheadOfTime();
        });
    }

    protected TickDelayedTask wrapTask(Runnable runnable) {
        return new TickDelayedTask(this.tickCounter, runnable);
    }

    protected boolean canRun(TickDelayedTask runnable) {
        return runnable.getScheduledTime() + 3 < this.tickCounter || this.isAheadOfTime();
    }

    public boolean driveOne() {
        boolean flag = this.func_213205_aW();
        this.isRunningScheduledTasks = flag;
        return flag;
    }

    private boolean func_213205_aW() {
        if (super.driveOne()) {
            return true;
        } else {
            if (this.isAheadOfTime()) {
                for (ServerWorld serverworld : this.getWorlds()) {
                    if (serverworld.getChunkProvider().func_217234_d()) {
                        return true;
                    }
                }
            }

            return false;
        }
    }

    public void applyServerIconToResponse(ServerStatusResponse response) {
        VFile2 file1 = this.getFile("server-icon.png");
        if (!file1.exists()) {
            file1 = this.getActiveAnvilConverter().getFile(this.getFolderName(), "icon.png");
        }

        if (file1.exists()) {
            try {
                java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
                IOUtils.copy(file1.getInputStream(), baos);
                byte[] iconBytes = baos.toByteArray();
                net.lax1dude.eaglercraft.opengl.ImageData img = net.lax1dude.eaglercraft.opengl.ImageData.loadImageFile(iconBytes);
                Validate.validState(img.getWidth() == 64, "Must be 64 pixels wide");
                Validate.validState(img.getHeight() == 64, "Must be 64 pixels high");
                String base64 = net.lax1dude.eaglercraft.Base64.encodeBase64String(iconBytes);
                response.setFavicon("data:image/png;base64," + base64);
            } catch (Exception exception) {
                LOGGER.error("Couldn't load server icon", (Throwable) exception);
            }
        }

    }

    @OnlyIn(Dist.CLIENT)
    public boolean isWorldIconSet() {
        boolean hasRawIcon = net.peyton.eagler.fs.WorldsDB.newVFile(this.getFolderName(), "icon.raw").exists();
        this.worldIconSet = this.worldIconSet || this.getWorldIconFile().exists() || hasRawIcon;
        return this.worldIconSet;
    }

    @OnlyIn(Dist.CLIENT)
    public VFile2 getWorldIconFile() {
        return this.getActiveAnvilConverter().getFile(this.getFolderName(), "icon.png");
    }

    public VFile2 getDataDirectory() {
        return new VFile2(".");
    }

    protected void finalTick(CrashReport report) {
    }

    protected void systemExitNow() {
    }

    protected void tick(BooleanSupplier hasTimeLeft) {
        long i = Util.nanoTime();
        ++this.tickCounter;
        this.updateTimeLightAndEntities(hasTimeLeft);
        if (i - this.nanoTimeSinceStatusRefresh >= 5000000000L) {
            this.nanoTimeSinceStatusRefresh = i;
            this.statusResponse.setPlayers(new ServerStatusResponse.Players(this.getMaxPlayers(), this.getCurrentPlayerCount()));
            GameProfile[] agameprofile = new GameProfile[Math.min(this.getCurrentPlayerCount(), 12)];
            int j = MathHelper.nextInt(this.random, 0, this.getCurrentPlayerCount() - agameprofile.length);

            for (int k = 0; k < agameprofile.length; ++k) {
                agameprofile[k] = this.playerList.getPlayers().get(j + k).getGameProfile();
            }

            Collections.shuffle(Arrays.asList(agameprofile));
            this.statusResponse.getPlayers().setPlayers(agameprofile);
        }

        if (this.tickCounter % 6000 == 0) {
            LOGGER.debug("Autosave started");
            this.profiler.startSection("save");
            this.playerList.saveAllPlayerData();
            this.save(true, false, false);
            this.profiler.endSection();
            LOGGER.debug("Autosave finished");
        }

        this.profiler.startSection("snooper");
        if (!this.snooper.isSnooperRunning() && this.tickCounter > 100) {
            this.snooper.start();
        }

        if (this.tickCounter % 6000 == 0) {
            this.snooper.addMemoryStatsToSnooper();
        }

        this.profiler.endSection();
        this.profiler.startSection("tallying");
        long l = this.tickTimeArray[this.tickCounter % 100] = Util.nanoTime() - i;
        this.tickTime = this.tickTime * 0.8F + (float) l / 1000000.0F * 0.19999999F;
        long i1 = Util.nanoTime();
        this.frameTimer.addFrame(i1 - i);
        this.profiler.endSection();
    }

    protected void updateTimeLightAndEntities(BooleanSupplier hasTimeLeft) {
        this.profiler.startSection("commandFunctions");
        this.getFunctionManager().tick();
        this.profiler.endStartSection("levels");

        for (ServerWorld serverworld : this.getWorlds()) {
            if (serverworld.dimension.getType() == DimensionType.OVERWORLD || this.getAllowNether()) {
                this.profiler.startSection(() -> {
                    return serverworld.getWorldInfo().getWorldName() + " " + Registry.DIMENSION_TYPE.getKey(serverworld.dimension.getType());
                });
                if (this.tickCounter % 20 == 0) {
                    this.profiler.startSection("timeSync");
                    this.playerList.sendPacketToAllPlayersInDimension(new SUpdateTimePacket(serverworld.getGameTime(), serverworld.getDayTime(), serverworld.getGameRules().getBoolean(GameRules.DO_DAYLIGHT_CYCLE)), serverworld.dimension.getType());
                    this.profiler.endSection();
                }

                this.profiler.startSection("tick");

                try {
                    serverworld.tick(hasTimeLeft);
                } catch (Throwable throwable) {
                    CrashReport crashreport = CrashReport.makeCrashReport(throwable, "Exception ticking world");
                    serverworld.fillCrashReport(crashreport);
                    throw new ReportedException(crashreport);
                }

                this.profiler.endSection();
                this.profiler.endSection();
            }
        }

        this.profiler.endStartSection("connection");
        this.getNetworkSystem().tick();
        this.profiler.endStartSection("players");
        this.playerList.tick();
        net.lax1dude.eaglercraft.sp.server.EaglerIntegratedServerWorker.tick();
        this.profiler.endStartSection("server gui refresh");

        for (int i = 0; i < this.tickables.size(); ++i) {
            this.tickables.get(i).run();
        }

        this.profiler.endSection();
    }

    public boolean getAllowNether() {
        return true;
    }

    public void registerTickable(Runnable tickable) {
        this.tickables.add(tickable);
    }


    protected void setServerId(String serverIdIn) {
        this.serverId = serverIdIn;
    }

    protected void setForceWorldUpgrade(boolean forceWorldUpgradeIn) {
        this.forceWorldUpgrade = forceWorldUpgradeIn;
    }

    protected void setEraseCache(boolean eraseCacheIn) {
        this.eraseCache = eraseCacheIn;
    }

    public void startServerThread() {
        this.serverThread.start();
    }

    @OnlyIn(Dist.CLIENT)
    public boolean isThreadAlive() {
        return !this.serverThread.isAlive();
    }

    public VFile2 getFile(String fileName) {
        return new VFile2(this.getDataDirectory(), fileName);
    }

    public void logInfo(String msg) {
        LOGGER.info(msg);
    }

    public void logWarning(String msg) {
        LOGGER.warn(msg);
    }

    public ServerWorld getWorld(DimensionType dimension) {
        return this.worlds.get(dimension);
    }

    public Iterable<ServerWorld> getWorlds() {
        return this.worlds.values();
    }

    public String getMinecraftVersion() {
        return SharedConstants.getVersion().getName();
    }

    public int getCurrentPlayerCount() {
        return this.playerList.getCurrentPlayerCount();
    }

    public int getMaxPlayers() {
        return this.playerList.getMaxPlayers();
    }

    public String[] getOnlinePlayerNames() {
        return this.playerList.getOnlinePlayerNames();
    }

    public boolean isDebuggingEnabled() {
        return false;
    }

    public void logSevere(String msg) {
        LOGGER.error(msg);
    }

    public void logDebug(String msg) {
        if (this.isDebuggingEnabled()) {
            LOGGER.info(msg);
        }

    }

    public String getServerModName() {
        return "eagler";
    }

    public CrashReport addServerInfoToCrashReport(CrashReport report) {
        if (this.playerList != null) {
            report.getCategory().addDetail("Player Count", () -> {
                return this.playerList.getCurrentPlayerCount() + " / " + this.playerList.getMaxPlayers() + "; " + this.playerList.getPlayers();
            });
        }

        report.getCategory().addDetail("Data Packs", () -> {
            StringBuilder stringbuilder = new StringBuilder();

            for (ResourcePackInfo resourcepackinfo : this.resourcePacks.getEnabledPacks()) {
                if (stringbuilder.length() > 0) {
                    stringbuilder.append(", ");
                }

                stringbuilder.append(resourcepackinfo.getName());
                if (!resourcepackinfo.getCompatibility().func_198968_a()) {
                    stringbuilder.append(" (incompatible)");
                }
            }

            return stringbuilder.toString();
        });
        if (this.serverId != null) {
            report.getCategory().addDetail("Server Id", () -> {
                return this.serverId;
            });
        }

        return report;
    }

    public boolean isAnvilFileSet() {
        return this.anvilFile != null;
    }

    public void sendMessage(ITextComponent component) {
        LOGGER.info(component.getString());
    }

    public Object getKeyPair() {
        return this.serverKeyPair;
    }

    public int getServerPort() {
        return this.serverPort;
    }

    public void setServerPort(int port) {
        this.serverPort = port;
    }

    public String getServerOwner() {
        return this.serverOwner;
    }

    public void setServerOwner(String owner) {
        this.serverOwner = owner;
    }

    public boolean isSinglePlayer() {
        return this.serverOwner != null;
    }

    public String getFolderName() {
        return this.folderName;
    }

    @OnlyIn(Dist.CLIENT)
    public void setWorldName(String worldNameIn) {
        this.worldName = worldNameIn;
    }

    @OnlyIn(Dist.CLIENT)
    public String getWorldName() {
        return this.worldName;
    }

    public void setKeyPair(Object keyPair) {
        this.serverKeyPair = keyPair;
    }

    public void setDifficultyForAllWorlds(Difficulty difficulty, boolean p_147139_2_) {
        for (ServerWorld serverworld : this.getWorlds()) {
            WorldInfo worldinfo = serverworld.getWorldInfo();
            if (p_147139_2_ || !worldinfo.isDifficultyLocked()) {
                if (worldinfo.isHardcore()) {
                    worldinfo.setDifficulty(Difficulty.HARD);
                    serverworld.setAllowedSpawnTypes(true, true);
                } else if (this.isSinglePlayer()) {
                    worldinfo.setDifficulty(difficulty);
                    serverworld.setAllowedSpawnTypes(serverworld.getDifficulty() != Difficulty.PEACEFUL, true);
                } else {
                    worldinfo.setDifficulty(difficulty);
                    serverworld.setAllowedSpawnTypes(this.allowSpawnMonsters(), this.canSpawnAnimals);
                }
            }
        }

        this.getPlayerList().getPlayers().forEach(this::sendDifficultyToPlayer);
    }

    public void setDifficultyLocked(boolean locked) {
        for (ServerWorld serverworld : this.getWorlds()) {
            WorldInfo worldinfo = serverworld.getWorldInfo();
            worldinfo.setDifficultyLocked(locked);
        }

        this.getPlayerList().getPlayers().forEach(this::sendDifficultyToPlayer);
    }

    private void sendDifficultyToPlayer(ServerPlayerEntity playerIn) {
        WorldInfo worldinfo = playerIn.getServerWorld().getWorldInfo();
        playerIn.connection.sendPacket(new SServerDifficultyPacket(worldinfo.getDifficulty(), worldinfo.isDifficultyLocked()));
    }

    protected boolean allowSpawnMonsters() {
        return true;
    }

    public boolean isDemo() {
        return this.isDemo;
    }

    public void setDemo(boolean demo) {
        this.isDemo = demo;
    }

    public void canCreateBonusChest(boolean enable) {
        this.enableBonusChest = enable;
    }

    public SaveFormat getActiveAnvilConverter() {
        return this.anvilConverterForAnvilFile;
    }

    public String getResourcePackUrl() {
        return this.resourcePackUrl;
    }

    public String getResourcePackHash() {
        return this.resourcePackHash;
    }

    public void setResourcePack(String url, String hash) {
        this.resourcePackUrl = url;
        this.resourcePackHash = hash;
    }

    public void fillSnooper(Snooper snooper) {
        snooper.addClientStat("whitelist_enabled", false);
        snooper.addClientStat("whitelist_count", 0);
        if (this.playerList != null) {
            snooper.addClientStat("players_current", this.getCurrentPlayerCount());
            snooper.addClientStat("players_max", this.getMaxPlayers());
            snooper.addClientStat("players_seen", this.getWorld(DimensionType.OVERWORLD).getSaveHandler().func_215771_d().length);
        }

        snooper.addClientStat("uses_auth", this.onlineMode);
        snooper.addClientStat("gui_state", this.getGuiEnabled() ? "enabled" : "disabled");
        snooper.addClientStat("run_time", (Util.milliTime() - snooper.getMinecraftStartTimeMillis()) / 60L * 1000L);
        snooper.addClientStat("avg_tick_ms", (int) (MathHelper.average(this.tickTimeArray) * 1.0E-6D));
        int i = 0;

        for (ServerWorld serverworld : this.getWorlds()) {
            if (serverworld != null) {
                WorldInfo worldinfo = serverworld.getWorldInfo();
                snooper.addClientStat("world[" + i + "][dimension]", serverworld.dimension.getType());
                snooper.addClientStat("world[" + i + "][mode]", worldinfo.getGameType());
                snooper.addClientStat("world[" + i + "][difficulty]", serverworld.getDifficulty());
                snooper.addClientStat("world[" + i + "][hardcore]", worldinfo.isHardcore());
                snooper.addClientStat("world[" + i + "][generator_name]", worldinfo.getGenerator().getName());
                snooper.addClientStat("world[" + i + "][generator_version]", worldinfo.getGenerator().getVersion());
                snooper.addClientStat("world[" + i + "][height]", this.buildLimit);
                snooper.addClientStat("world[" + i + "][chunks_loaded]", serverworld.getChunkProvider().getLoadedChunkCount());
                ++i;
            }
        }

        snooper.addClientStat("worlds", i);
    }

    public abstract boolean isDedicatedServer();

    public boolean isServerInOnlineMode() {
        return this.onlineMode;
    }

    public void setOnlineMode(boolean online) {
        this.onlineMode = online;
    }

    public boolean getPreventProxyConnections() {
        return this.preventProxyConnections;
    }

    public void setPreventProxyConnections(boolean p_190517_1_) {
        this.preventProxyConnections = p_190517_1_;
    }

    public boolean getCanSpawnAnimals() {
        return this.canSpawnAnimals;
    }

    public void setCanSpawnAnimals(boolean spawnAnimals) {
        this.canSpawnAnimals = spawnAnimals;
    }

    public boolean getCanSpawnNPCs() {
        return this.canSpawnNPCs;
    }

    public abstract boolean shouldUseNativeTransport();

    public void setCanSpawnNPCs(boolean spawnNpcs) {
        this.canSpawnNPCs = spawnNpcs;
    }

    public boolean isPVPEnabled() {
        return this.pvpEnabled;
    }

    public void setAllowPvp(boolean allowPvp) {
        this.pvpEnabled = allowPvp;
    }

    public boolean isFlightAllowed() {
        return this.allowFlight;
    }

    public void setAllowFlight(boolean allow) {
        this.allowFlight = allow;
    }

    public abstract boolean isCommandBlockEnabled();

    public String getMOTD() {
        return this.motd;
    }

    public void setMOTD(String motdIn) {
        this.motd = motdIn;
    }

    public int getBuildLimit() {
        return this.buildLimit;
    }

    public void setBuildLimit(int maxBuildHeight) {
        this.buildLimit = maxBuildHeight;
    }

    public boolean isServerStopped() {
        return this.serverStopped;
    }

    public PlayerList getPlayerList() {
        return this.playerList;
    }

    public void setPlayerList(PlayerList list) {
        this.playerList = list;
    }

    public abstract boolean getPublic();

    public void setGameType(GameType gameMode) {
        for (ServerWorld serverworld : this.getWorlds()) {
            serverworld.getWorldInfo().setGameType(gameMode);
        }

    }


    public NetworkSystem getNetworkSystem() {
        return this.networkSystem;
    }

    @OnlyIn(Dist.CLIENT)
    public boolean serverIsInRunLoop() {
        return this.serverIsRunning;
    }

    public boolean getGuiEnabled() {
        return false;
    }

    public abstract boolean shareToLAN(GameType gameMode, boolean cheats, int port);

    public int getTickCounter() {
        return this.tickCounter;
    }

    public void enableProfiling() {
        this.startProfiling = true;
    }

    @OnlyIn(Dist.CLIENT)
    public Snooper getSnooper() {
        return this.snooper;
    }

    public int getSpawnProtectionSize() {
        return 16;
    }

    public boolean isBlockProtected(World worldIn, BlockPos pos, PlayerEntity playerIn) {
        return false;
    }

    public void setForceGamemode(boolean force) {
        this.isGamemodeForced = force;
    }

    public boolean getForceGamemode() {
        return this.isGamemodeForced;
    }

    public int getMaxPlayerIdleMinutes() {
        return this.maxPlayerIdleMinutes;
    }

    public void setPlayerIdleTimeout(int idleTimeout) {
        this.maxPlayerIdleMinutes = idleTimeout;
    }

    public MinecraftSessionService getMinecraftSessionService() {
        return this.sessionService;
    }

    public GameProfileRepository getGameProfileRepository() {
        return this.profileRepo;
    }

    public PlayerProfileCache getPlayerProfileCache() {
        return this.profileCache;
    }

    public ServerStatusResponse getServerStatusResponse() {
        return this.statusResponse;
    }

    public void refreshStatusNextTick() {
        this.nanoTimeSinceStatusRefresh = 0L;
    }

    public int getMaxWorldSize() {
        return 29999984;
    }

    public boolean shouldDeferTasks() {
        return super.shouldDeferTasks() && !this.isServerStopped();
    }

    public Thread getExecutionThread() {
        return this.serverThread;
    }

    public int getNetworkCompressionThreshold() {
        return 256;
    }

    public long getServerTime() {
        return this.serverTime;
    }

    public DataFixer getDataFixer() {
        return this.dataFixer;
    }

    public int getSpawnRadius(ServerWorld worldIn) {
        return worldIn != null ? worldIn.getGameRules().getInt(GameRules.SPAWN_RADIUS) : 10;
    }

    public AdvancementManager getAdvancementManager() {
        return this.advancementManager;
    }

    public FunctionManager getFunctionManager() {
        return this.functionManager;
    }

    public void reload() {
        if (!this.isOnExecutionThread()) {
            this.execute(this::reload);
        } else {
            this.getPlayerList().saveAllPlayerData();
            this.resourcePacks.reloadPacksFromFinders();
            this.loadDataPacks(this.getWorld(DimensionType.OVERWORLD).getWorldInfo());
            this.getPlayerList().reloadResources();
        }
    }

    private void loadDataPacks(WorldInfo worldInfoIn) {
        List<ResourcePackInfo> list = Lists.newArrayList(this.resourcePacks.getEnabledPacks());

        for (ResourcePackInfo resourcepackinfo : this.resourcePacks.getAllPacks()) {
            if (!worldInfoIn.getDisabledDataPacks().contains(resourcepackinfo.getName()) && !list.contains(resourcepackinfo)) {
                LOGGER.info("Found new data pack {}, loading it automatically", (Object) resourcepackinfo.getName());
                resourcepackinfo.getPriority().func_198993_a(list, resourcepackinfo, (p_200247_0_) -> {
                    return p_200247_0_;
                }, false);
            }
        }

        this.resourcePacks.setEnabledPacks(list);
        List<IResourcePack> list1 = Lists.newArrayList();
        this.resourcePacks.getEnabledPacks().forEach((p_200244_1_) -> {
            list1.add(p_200244_1_.getResourcePack());
        });
        CompletableFuture<Unit> completablefuture = this.resourceManager.reloadResourcesAndThen(this.backgroundExecutor, this, list1, field_223713_i);
        this.driveUntil(completablefuture::isDone);

        try {
            completablefuture.get();
        } catch (Exception exception) {
            LOGGER.error("Failed to reload data packs", (Throwable) exception);
        }

        worldInfoIn.getEnabledDataPacks().clear();
        worldInfoIn.getDisabledDataPacks().clear();
        this.resourcePacks.getEnabledPacks().forEach((p_195562_1_) -> {
            worldInfoIn.getEnabledDataPacks().add(p_195562_1_.getName());
        });
        this.resourcePacks.getAllPacks().forEach((p_200248_2_) -> {
            if (!this.resourcePacks.getEnabledPacks().contains(p_200248_2_)) {
                worldInfoIn.getDisabledDataPacks().add(p_200248_2_.getName());
            }

        });
    }

    public void kickPlayersNotWhitelisted(CommandSource commandSourceIn) {
        if (this.isWhitelistEnabled()) {
            PlayerList playerlist = commandSourceIn.getServer().getPlayerList();
            WhiteList whitelist = playerlist.getWhitelistedPlayers();
            if (whitelist.isLanServer()) {
                for (ServerPlayerEntity serverplayerentity : Lists.newArrayList(playerlist.getPlayers())) {
                    if (!whitelist.isWhitelisted(serverplayerentity.getGameProfile())) {
                        serverplayerentity.connection.disconnect(new TranslationTextComponent("multiplayer.disconnect.not_whitelisted"));
                    }
                }

            }
        }
    }

    public IReloadableResourceManager getResourceManager() {
        return this.resourceManager;
    }

    public ResourcePackList<ResourcePackInfo> getResourcePacks() {
        return this.resourcePacks;
    }

    public Commands getCommandManager() {
        return this.commandManager;
    }

    public CommandSource getCommandSource() {
        return new CommandSource(this, this.getWorld(DimensionType.OVERWORLD) == null ? Vec3d.ZERO : new Vec3d(this.getWorld(DimensionType.OVERWORLD).getSpawnPoint()), Vec2f.ZERO, this.getWorld(DimensionType.OVERWORLD), 4, "Server", new StringTextComponent("Server"), this, (Entity) null);
    }

    public boolean shouldReceiveFeedback() {
        return true;
    }

    public boolean shouldReceiveErrors() {
        return true;
    }

    public RecipeManager getRecipeManager() {
        return this.recipeManager;
    }

    public NetworkTagManager getNetworkTagManager() {
        return this.networkTagManager;
    }

    public ServerScoreboard getScoreboard() {
        return this.scoreboard;
    }

    public LootTableManager getLootTableManager() {
        return this.lootTableManager;
    }

    public GameRules getGameRules() {
        return this.getWorld(DimensionType.OVERWORLD).getGameRules();
    }

    public CustomServerBossInfoManager getCustomBossEvents() {
        return this.customBossEvents;
    }

    public boolean isWhitelistEnabled() {
        return this.whitelistEnabled;
    }

    public void setWhitelistEnabled(boolean whitelistEnabledIn) {
        this.whitelistEnabled = whitelistEnabledIn;
    }

    public float getTickTime() {
        return this.tickTime;
    }

    public int getPermissionLevel(GameProfile profile) {
        if (this.getPlayerList().canSendCommands(profile)) {
            OpEntry opentry = this.getPlayerList().getOppedPlayers().getEntry(profile);
            if (opentry != null) {
                return opentry.getPermissionLevel();
            } else if (this.func_213199_b(profile)) {
                return 4;
            } else if (this.isSinglePlayer()) {
                return this.getPlayerList().commandsAllowedForAll() ? 4 : 0;
            } else {
                return this.getOpPermissionLevel();
            }
        } else {
            return 0;
        }
    }

    @OnlyIn(Dist.CLIENT)
    public FrameTimer getFrameTimer() {
        return this.frameTimer;
    }

    public DebugProfiler getProfiler() {
        return this.profiler;
    }

    public Executor getBackgroundExecutor() {
        return this.backgroundExecutor;
    }

    public abstract boolean func_213199_b(GameProfile p_213199_1_);

    public void func_223711_a(Path p_223711_1_) throws IOException {
    }

    private void func_223710_b(Path p_223710_1_) throws IOException {
    }

    private void func_223709_c(Path p_223709_1_) throws IOException {
    }

    private void func_223708_d(Path p_223708_1_) throws IOException {
    }

    private void func_223706_e(Path p_223706_1_) throws IOException {
    }

    private void func_223712_f(Path p_223712_1_) throws IOException {
    }
}
