package net.minecraft.client.renderer.texture;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.mojang.blaze3d.platform.TextureUtil;
import java.io.Closeable;
import java.io.IOException;
import java.util.*;

import net.deltaclient.CompletableFuture;
import java.util.stream.Collectors;

import net.lax1dude.eaglercraft.IOUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.StitcherException;
import net.minecraft.client.resources.data.AnimationMetadataSection;
import net.minecraft.crash.CrashReport;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.crash.ReportedException;
import net.minecraft.profiler.IProfiler;
import net.minecraft.resources.IResource;
import net.minecraft.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@OnlyIn(Dist.CLIENT)
public class AtlasTexture extends Texture implements ITickableTextureObject {
   private static final Logger LOGGER = LogManager.getLogger();
   public static final ResourceLocation LOCATION_BLOCKS_TEXTURE = new ResourceLocation("textures/atlas/blocks.png");
   public static final ResourceLocation LOCATION_PARTICLES_TEXTURE = new ResourceLocation("textures/atlas/particles.png");
   public static final ResourceLocation LOCATION_PAINTINGS_TEXTURE = new ResourceLocation("textures/atlas/paintings.png");
   public static final ResourceLocation LOCATION_EFFECTS_TEXTURE = new ResourceLocation("textures/atlas/mob_effects.png");
   private final List<TextureAtlasSprite> listAnimatedSprites = Lists.newArrayList();
   private final Set<ResourceLocation> sprites = Sets.newHashSet();
   private final Map<ResourceLocation, TextureAtlasSprite> mapUploadedSprites = Maps.newHashMap();
   private final String basePath;
   private final int field_215265_o;
   private int mipmapLevels;
   private final TextureAtlasSprite missingImage = MissingTextureSprite.func_217790_a();
   private boolean isEaglerPBRMode = false;
   public int eaglerPBRMaterialTexture = -1;

   public AtlasTexture(String basePathIn) {
      this.basePath = basePathIn;
      this.field_215265_o = Minecraft.getGLMaximumTextureSize();
   }

   public void loadTexture(IResourceManager manager) throws IOException {
   }

   public void upload(AtlasTexture.SheetData p_215260_1_) {
      this.sprites.clear();
      this.sprites.addAll(p_215260_1_.field_217805_a);
      LOGGER.info("Created: {}x{} {}-atlas", p_215260_1_.width, p_215260_1_.height, this.basePath);
      TextureUtil.prepareImage(this.getGlTextureId(), this.mipmapLevels, p_215260_1_.width, p_215260_1_.height);
      this.clear();

      int spriteIdx = 0;
      for(TextureAtlasSprite textureatlassprite : p_215260_1_.sprites) {
         this.mapUploadedSprites.put(textureatlassprite.getName(), textureatlassprite);

          try {
            textureatlassprite.uploadMipmaps();
         } catch (Throwable throwable) {
            CrashReport crashreport = CrashReport.makeCrashReport(throwable, "Stitching texture atlas");
            CrashReportCategory crashreportcategory = crashreport.makeCategory("Texture being stitched together");
            crashreportcategory.addDetail("Atlas path", this.basePath);
            crashreportcategory.addDetail("Sprite", textureatlassprite);
            throw new ReportedException(crashreport);
         }
         ++spriteIdx;

         if (textureatlassprite.hasAnimationMetadata()) {
            this.listAnimatedSprites.add(textureatlassprite);
         }
      }

   }

   public AtlasTexture.SheetData stitch(IResourceManager p_215254_1_, Iterable<ResourceLocation> p_215254_2_, IProfiler p_215254_3_) {
      Set<ResourceLocation> set = Sets.newHashSet();
      p_215254_3_.startSection("preparing");
      p_215254_2_.forEach((p_215253_1_) -> {
         if (p_215253_1_ == null) {
            throw new IllegalArgumentException("Location cannot be null!");
         } else {
            set.add(p_215253_1_);
         }
      });
      set.add(this.missingImage.getName());
      int i = this.field_215265_o;
      Stitcher stitcher = new Stitcher(i, i, this.mipmapLevels);
      int j = Integer.MAX_VALUE;
      int k = 1 << this.mipmapLevels;
      p_215254_3_.endStartSection("extracting_frames");

      for(TextureAtlasSprite textureatlassprite : this.func_215256_a(p_215254_1_, set)) {
         j = Math.min(j, Math.min(textureatlassprite.getWidth(), textureatlassprite.getHeight()));
         int l = Math.min(Integer.lowestOneBit(textureatlassprite.getWidth()), Integer.lowestOneBit(textureatlassprite.getHeight()));
         if (l < k) {
            LOGGER.warn("Texture {} with size {}x{} limits mip level from {} to {}", textureatlassprite.getName(), textureatlassprite.getWidth(), textureatlassprite.getHeight(), MathHelper.log2(k), MathHelper.log2(l));
            k = l;
         }

         stitcher.addSprite(textureatlassprite);
      }

      int i1 = Math.min(j, k);
      int j1 = MathHelper.log2(i1);
      if (j1 < this.mipmapLevels) {
         LOGGER.warn("{}: dropping miplevel from {} to {}, because of minimum power of two: {}", this.basePath, this.mipmapLevels, j1, i1);
         this.mipmapLevels = j1;
      }

      p_215254_3_.endStartSection("mipmapping");
      this.missingImage.generateMipmaps(this.mipmapLevels);
      p_215254_3_.endStartSection("register");
      stitcher.addSprite(this.missingImage);
      p_215254_3_.endStartSection("stitching");

      try {
         stitcher.doStitch();
      } catch (StitcherException stitcherexception) {
         CrashReport crashreport = CrashReport.makeCrashReport(stitcherexception, "Stitching");
         CrashReportCategory crashreportcategory = crashreport.makeCategory("Stitcher");
         crashreportcategory.addDetail("Sprites", stitcherexception.func_225331_a().stream().map((p_224739_0_) -> {
            return String.format("%s[%dx%d]", p_224739_0_.getName(), p_224739_0_.getWidth(), p_224739_0_.getHeight());
         }).collect(Collectors.joining(",")));
         crashreportcategory.addDetail("Max Texture Size", i);
         throw new ReportedException(crashreport);
      }

      p_215254_3_.endStartSection("loading");
      List<TextureAtlasSprite> list = this.func_215259_a(p_215254_1_, stitcher);
      p_215254_3_.endSection();
      return new AtlasTexture.SheetData(set, stitcher.getCurrentWidth(), stitcher.getCurrentHeight(), list);
   }

   private Collection<TextureAtlasSprite> func_215256_a(IResourceManager p_215256_1_, Set<ResourceLocation> p_215256_2_) {
      List<CompletableFuture<?>> list = new ArrayList<>();
      List<TextureAtlasSprite> concurrentlinkedqueue = Collections.synchronizedList(new ArrayList<>());

      for(ResourceLocation resourcelocation : p_215256_2_) {
         if (!this.missingImage.getName().equals(resourcelocation)) {
            list.add(CompletableFuture.runAsync(() -> {
               ResourceLocation resourcelocation1 = this.getSpritePath(resourcelocation);

               TextureAtlasSprite textureatlassprite;
               try (IResource iresource = p_215256_1_.getResource(resourcelocation1)) {
                  PngSizeInfo pngsizeinfo = new PngSizeInfo(iresource.toString(), iresource.getInputStream());
                  AnimationMetadataSection animationmetadatasection = iresource.getMetadata(AnimationMetadataSection.SERIALIZER);
                  textureatlassprite = new TextureAtlasSprite(resourcelocation, pngsizeinfo, animationmetadatasection);
               } catch (RuntimeException runtimeexception) {
                  LOGGER.error("Unable to parse metadata from {} : {}", resourcelocation1, runtimeexception);
                  runtimeexception.printStackTrace();
                  return;
               } catch (IOException ioexception) {
                  LOGGER.error("Using missing texture, unable to load {} : {}", resourcelocation1, ioexception);
                  ioexception.printStackTrace();
                  return;
               }

               concurrentlinkedqueue.add(textureatlassprite);
            }, Util.getServerExecutor()));
         } else {
            concurrentlinkedqueue.add(this.missingImage);
         }
      }

      CompletableFuture.allOf(list.toArray(new CompletableFuture[0])).join();
      return concurrentlinkedqueue;
   }

   private List<TextureAtlasSprite> func_215259_a(IResourceManager p_215259_1_, Stitcher p_215259_2_) {
      List<TextureAtlasSprite> concurrentlinkedqueue = Collections.synchronizedList(new ArrayList<>());
      List<CompletableFuture<?>> list = new ArrayList<>();

      for(TextureAtlasSprite textureatlassprite : p_215259_2_.getStichSlots()) {
         if (textureatlassprite == this.missingImage) {
            concurrentlinkedqueue.add(textureatlassprite);
         } else {
            list.add(CompletableFuture.runAsync(() -> {
               if (this.loadSprite(p_215259_1_, textureatlassprite)) {
                  concurrentlinkedqueue.add(textureatlassprite);
               }

            }, Util.getServerExecutor()));
         }
      }

      CompletableFuture.allOf(list.toArray(new CompletableFuture[0])).join();
      return new ArrayList<>(concurrentlinkedqueue);
   }

   private boolean loadSprite(IResourceManager manager, TextureAtlasSprite sprite) {
      ResourceLocation resourcelocation = this.getSpritePath(sprite.getName());
      IResource iresource = null;

      label62: {
         boolean flag;
         try {
            iresource = manager.getResource(resourcelocation);
            sprite.loadSpriteFrames(iresource, this.mipmapLevels + 1);
            break label62;
         } catch (RuntimeException runtimeexception) {
            LOGGER.error("Unable to parse metadata from {}", resourcelocation, runtimeexception);
            runtimeexception.printStackTrace();
            flag = false;
         } catch (IOException ioexception) {
            LOGGER.error("Using missing texture, unable to load {}", resourcelocation, ioexception);
            ioexception.printStackTrace();
            flag = false;
            return flag;
         } finally {
            IOUtils.closeQuietly((Closeable)iresource);
         }

         return flag;
      }

      try {
         sprite.generateMipmaps(this.mipmapLevels);
         return true;
      } catch (Throwable throwable) {
         CrashReport crashreport = CrashReport.makeCrashReport(throwable, "Applying mipmap");
         CrashReportCategory crashreportcategory = crashreport.makeCategory("Sprite being mipmapped");
         crashreportcategory.addDetail("Sprite name", () -> {
            return sprite.getName().toString();
         });
         crashreportcategory.addDetail("Sprite size", () -> {
            return sprite.getWidth() + " x " + sprite.getHeight();
         });
         crashreportcategory.addDetail("Sprite frames", () -> {
            return sprite.getFrameCount() + " frames";
         });
         crashreportcategory.addDetail("Mipmap levels", this.mipmapLevels);
         throw new ReportedException(crashreport);
      }
   }

   private ResourceLocation getSpritePath(ResourceLocation location) {
      return new ResourceLocation(location.getNamespace(), String.format("%s/%s%s", this.basePath, location.getPath(), ".png"));
   }

   public TextureAtlasSprite getAtlasSprite(String iconName) {
      return this.getSprite(new ResourceLocation(iconName));
   }

   public void updateAnimations() {
      this.bindTexture();

      for(TextureAtlasSprite textureatlassprite : this.listAnimatedSprites) {
         textureatlassprite.updateAnimation();
      }

   }

   public void tick() {
      this.updateAnimations();
   }

   public void setMipmapLevels(int mipmapLevelsIn) {
      this.mipmapLevels = mipmapLevelsIn;
   }

   public TextureAtlasSprite getSprite(ResourceLocation location) {
      TextureAtlasSprite textureatlassprite = this.mapUploadedSprites.get(location);
      return textureatlassprite == null ? this.missingImage : textureatlassprite;
   }

   public void clear() {
      for(TextureAtlasSprite textureatlassprite : this.mapUploadedSprites.values()) {
         textureatlassprite.clearFramesTextureData();
      }

      this.mapUploadedSprites.clear();
      this.listAnimatedSprites.clear();
   }


   public void setEnablePBREagler(boolean enable) {
      this.isEaglerPBRMode = enable;
   }

   @OnlyIn(Dist.CLIENT)
   public static class SheetData {
      final Set<ResourceLocation> field_217805_a;
      public final int width;
      public final int height;
      public final List<TextureAtlasSprite> sprites;

      public SheetData(Set<ResourceLocation> p_i49874_1_, int widthIn, int heightIn, List<TextureAtlasSprite> spritesIn) {
         this.field_217805_a = p_i49874_1_;
         this.width = widthIn;
         this.height = heightIn;
         this.sprites = spritesIn;
      }
   }
}
