package net.lax1dude.eaglercraft.profile;

import net.minecraft.util.ResourceLocation;

public enum DefaultSkins {
    DEFAULT_STEVE(0, "Steve", new ResourceLocation("eagler:skins/01.default_steve.png"), SkinModel.STEVE),
    DEFAULT_ALEX(1, "Alex", new ResourceLocation("eagler:skins/02.default_alex.png"), SkinModel.ALEX),
    ARI(2, "Ari", new ResourceLocation("eagler:skins/03.ari.png"), SkinModel.STEVE),
    EFE(3, "Efe", new ResourceLocation("eagler:skins/04.efe.png"), SkinModel.ALEX),
    KAI(4, "Kai", new ResourceLocation("eagler:skins/05.kai.png"), SkinModel.STEVE),
    MAKENA(5, "Makena", new ResourceLocation("eagler:skins/06.makena.png"), SkinModel.ALEX),
    NOOR(6, "Noor", new ResourceLocation("eagler:skins/07.noor.png"), SkinModel.STEVE),
    SUNNY(7, "Sunny", new ResourceLocation("eagler:skins/08.sunny.png"), SkinModel.ALEX),
    ZURI(8, "Zuri", new ResourceLocation("eagler:skins/09.zuri.png"), SkinModel.STEVE);
    public static final DefaultSkins[] defaultSkinsMap = new DefaultSkins[9];

    static {
        DefaultSkins[] skins = values();
        for (int i = 0; i < skins.length; ++i) {
            defaultSkinsMap[skins[i].id] = skins[i];
        }
    }

    public final int id;
    public final String name;
    public final ResourceLocation location;
    public final SkinModel model;

    private DefaultSkins(int id, String name, ResourceLocation location, SkinModel model) {
        this.id = id;
        this.name = name;
        this.location = location;
        this.model = model;
    }

    public static DefaultSkins getSkinFromId(int id) {
        DefaultSkins e = null;
        if (id >= 0 && id < defaultSkinsMap.length) {
            e = defaultSkinsMap[id];
        }
        if (e != null) {
            return e;
        } else {
            return DEFAULT_STEVE;
        }
    }
}