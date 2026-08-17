package net.minecraft.util;


import java.io.DataOutputStream;
import net.lax1dude.eaglercraft.internal.vfs2.VFile2;
import java.io.OutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.ServerSocket;
import java.net.URL;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import net.deltaclient.CompletableFuture;
import java.util.concurrent.Executors;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.lax1dude.eaglercraft.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class HTTPUtil {
   private static final Logger LOGGER = LogManager.getLogger();

   @OnlyIn(Dist.CLIENT)
   public static CompletableFuture<?> downloadResourcePack(VFile2 saveFile, String packUrl, Map<String, String> p_180192_2_, int maxSize,  IProgressUpdate p_180192_4_, Proxy p_180192_5_) {
      return CompletableFuture.supplyAsync(() -> {
         HttpURLConnection httpurlconnection = null;
         InputStream inputstream = null;
         OutputStream outputstream = null;
         if (p_180192_4_ != null) {
            p_180192_4_.resetProgressAndMessage(new TranslationTextComponent("resourcepack.downloading"));
            p_180192_4_.displayLoadingString(new TranslationTextComponent("resourcepack.requesting"));
         }

         try {
            try {
               byte[] abyte = new byte[4096];
               URL url = new URL(packUrl);
               httpurlconnection = (HttpURLConnection) url.openConnection(p_180192_5_);
               httpurlconnection.setInstanceFollowRedirects(true);
               float f = 0.0F;
               float f1 = (float) p_180192_2_.entrySet().size();

               for (Entry<String, String> entry : p_180192_2_.entrySet()) {
                  httpurlconnection.setRequestProperty(entry.getKey(), entry.getValue());
                  if (p_180192_4_ != null) {
                     p_180192_4_.setLoadingProgress((int) (++f / f1 * 100.0F));
                  }
               }

               inputstream = httpurlconnection.getInputStream();
               f1 = (float) httpurlconnection.getContentLength();
               int i = httpurlconnection.getContentLength();
               if (p_180192_4_ != null) {
                  p_180192_4_.displayLoadingString(new TranslationTextComponent("resourcepack.progress", String.format(Locale.ROOT, "%.2f", f1 / 1000.0F / 1000.0F)));
               }

               if (saveFile.exists()) {
                  long j = saveFile.length();
                  if (j == (long) i) {
                     if (p_180192_4_ != null) {
                        p_180192_4_.setDoneWorking();
                     }

                     Object object1 = null;
                     return object1;
                  }

                  LOGGER.warn("Deleting {} as it does not match what we currently have ({} vs our {}).", saveFile, i, j);
               } else if ((saveFile.getParent() != null ? new net.lax1dude.eaglercraft.internal.vfs2.VFile2(saveFile.getParent()) : null) != null) {
/* mkdirs removed (                  (saveFile.getParent() != null ? new net.lax1dude.eaglercraft.internal.vfs2.VFile2(saveFile.getParent()) : null) ) */
               }

               outputstream = new DataOutputStream(saveFile.getOutputStream());
               if (maxSize > 0 && f1 > (float) maxSize) {
                  if (p_180192_4_ != null) {
                     p_180192_4_.setDoneWorking();
                  }

                  throw new IOException("Filesize is bigger than maximum allowed (file is " + f + ", limit is " + maxSize + ")");
               }

               int k;
               while ((k = inputstream.read(abyte)) >= 0) {
                  f += (float) k;
                  if (p_180192_4_ != null) {
                     p_180192_4_.setLoadingProgress((int) (f / f1 * 100.0F));
                  }

                  if (maxSize > 0 && f > (float) maxSize) {
                     if (p_180192_4_ != null) {
                        p_180192_4_.setDoneWorking();
                     }

                     throw new IOException("Filesize was bigger than maximum allowed (got >= " + f + ", limit was " + maxSize + ")");
                  }

                  if (Thread.interrupted()) {
                     LOGGER.error("INTERRUPTED");
                     if (p_180192_4_ != null) {
                        p_180192_4_.setDoneWorking();
                     }

                     Object object = null;
                     return object;
                  }

                  outputstream.write(abyte, 0, k);
               }

               if (p_180192_4_ != null) {
                  p_180192_4_.setDoneWorking();
                  return null;
               }
            } catch (Throwable throwable) {
               throwable.printStackTrace();
               if (httpurlconnection != null) {
                  InputStream inputstream1 = httpurlconnection.getErrorStream();

                  LOGGER.error("Naaah");
               }

               if (p_180192_4_ != null) {
                  p_180192_4_.setDoneWorking();
                  return null;
               }
            }

            return null;
         } finally {
            IOUtils.closeQuietly(inputstream);
            IOUtils.closeQuietly(outputstream);
         }
      });

   }}
