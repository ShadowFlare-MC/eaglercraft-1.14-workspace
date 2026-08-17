/*
 * Copyright (c) 2024 lax1dude. All Rights Reserved.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * 
 */

package net.lax1dude.eaglercraft.internal.wasm_gc_teavm;

import java.io.PrintStream;

import net.deltaclient.RecipeLoaderWASM;
import net.lax1dude.eaglercraft.Filesystem;
import net.lax1dude.eaglercraft.internal.IEaglerFilesystem;
import net.lax1dude.eaglercraft.internal.vfs2.VFile2;
import net.lax1dude.eaglercraft.sp.relay.RelayManager;
import net.minecraft.client.Minecraft;
import net.peyton.eagler.fs.WorldsDB;
import org.teavm.interop.Import;
import org.teavm.jso.JSObject;
import org.teavm.jso.browser.Window;

import net.lax1dude.eaglercraft.EagRuntime;
import net.lax1dude.eaglercraft.internal.ContextLostError;
import net.lax1dude.eaglercraft.internal.PlatformApplication;
import net.lax1dude.eaglercraft.internal.PlatformRuntime;
import net.lax1dude.eaglercraft.internal.wasm_gc_teavm.opts.JSEaglercraftXOptsRoot;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.RecipeManager;

public class ClientMain {

	private static final PrintStream systemOut = System.out;
	private static final PrintStream systemErr = System.err;
	public static String configLocalesFolder = null;

	public static void _main() {
		try {
			systemOut.println("ClientMain: [INFO] eaglercraftx wasm gc is starting...");
			JSObject opts = getEaglerXOpts();

			if(opts == null) {
				systemErr.println("ClientMain: [ERROR] the \"window.eaglercraftXOpts\" variable is undefined");
				systemErr.println("ClientMain: [ERROR] eaglercraftx cannot start");
				Window.alert("ERROR: game cannot start, the \"window.eaglercraftXOpts\" variable is undefined");
				return;
			}

			try {
				JSEaglercraftXOptsRoot eaglercraftOpts = (JSEaglercraftXOptsRoot)opts;
				
				configLocalesFolder = eaglercraftOpts.getLocalesURI("lang");
				if(configLocalesFolder.endsWith("/")) {
					configLocalesFolder = configLocalesFolder.substring(0, configLocalesFolder.length() - 1);
				}
				
				((WASMGCClientConfigAdapter)WASMGCClientConfigAdapter.instance).loadNative(eaglercraftOpts);
				
				systemOut.println("ClientMain: [INFO] configuration was successful");
			}catch(Throwable t) {
				systemErr.println("ClientMain: [ERROR] the \"window.eaglercraftXOpts\" variable is invalid");
				EagRuntime.debugPrintStackTraceToSTDERR(t);
				systemErr.println("ClientMain: [ERROR] eaglercraftx cannot start");
				Window.alert("ERROR: game cannot start, the \"window.eaglercraftXOpts\" variable is invalid: " + t.toString());
				return;
			}

			systemOut.println("ClientMain: [INFO] initializing eaglercraftx runtime");

			try {
				EagRuntime.create();
			}catch(ContextLostError t) {
				systemErr.println("ClientMain: [ERROR] webgl context lost during initialization!");
				PlatformRuntime.showContextLostScreen(EagRuntime.getStackTrace(t));
				return;
			}catch(Throwable t) {
				systemErr.println("ClientMain: [ERROR] eaglercraftx's runtime could not be initialized!");
				EagRuntime.debugPrintStackTraceToSTDERR(t);
				PlatformRuntime.writeCrashReport("EaglercraftX's runtime could not be initialized!\n\n" + EagRuntime.getStackTrace(t));
				systemErr.println("ClientMain: [ERROR] eaglercraftx cannot start");
				return;
			}
			//yee
			RecipeManager.setRecipeFallback(() -> {
				java.util.Collection<IRecipe<?>> ret = RecipeLoaderWASM.loadRecipesFromAssets();
				systemOut.println("ClientMain: [INFO] loaded " + ret.size() + " recipes from EPK assets via fallback loader");
				return ret;
			});

			systemOut.println("ClientMain: [INFO] launching eaglercraftx main thread");
			try {
				IEaglerFilesystem worldsFS = Filesystem.getHandleFor(
						((WASMGCClientConfigAdapter)WASMGCClientConfigAdapter.instance).getWorldsDB());
				WorldsDB.setWorldsDBProvider(() -> worldsFS);
				systemOut.println("ClientMain: [INFO] worlds database filesystem initialized");
			}catch(Throwable t) {
				systemErr.println("ClientMain: [WARN] could not initialize worlds database filesystem!");
				EagRuntime.debugPrintStackTraceToSTDERR(t);
			}
			RelayManager.relayManager.load(EagRuntime.getStorage("r"));

			if (RelayManager.relayManager.count() <= 0) {
				RelayManager.relayManager.loadDefaults();
				RelayManager.relayManager.save();
			}

			try {
				net.minecraft.client.GameConfiguration.UserInformation userInfo = new net.minecraft.client.GameConfiguration.UserInformation(
						new net.minecraft.util.Session("Player", "1", "-", "legacy"),
						new com.mojang.authlib.properties.PropertyMap(),
						new com.mojang.authlib.properties.PropertyMap()
				);

				net.minecraft.client.renderer.ScreenSize displayInfo = new net.minecraft.client.renderer.ScreenSize(854, 480, java.util.OptionalInt.of(1920), java.util.OptionalInt.of(1080), false);

				net.minecraft.client.GameConfiguration.FolderInformation folderInfo = new net.minecraft.client.GameConfiguration.FolderInformation(
						new VFile2("run"),
						new VFile2("resourcepacks"),
						new VFile2("assets"),
						null
				);

				net.minecraft.client.GameConfiguration.GameInformation gameInfo = new net.minecraft.client.GameConfiguration.GameInformation(
						false,
						"1.14.4",
						"release"
				);

				net.minecraft.client.GameConfiguration.ServerInformation serverInfo = new net.minecraft.client.GameConfiguration.ServerInformation(
						null,
						25565
				);

				net.minecraft.client.GameConfiguration gameConfig = new net.minecraft.client.GameConfiguration(userInfo, displayInfo, folderInfo, gameInfo, serverInfo);
				new Minecraft(gameConfig).run();
			}catch(ContextLostError t) {
				systemErr.println("ClientMain: [ERROR] webgl context lost!");
				PlatformRuntime.showContextLostScreen(EagRuntime.getStackTrace(t));
			}catch(Throwable t) {
				systemErr.println("ClientMain: [ERROR] unhandled exception caused main thread to exit");
				EagRuntime.debugPrintStackTraceToSTDERR(t);
				PlatformRuntime.writeCrashReport("Unhandled exception caused main thread to exit!\n\n" + EagRuntime.getStackTrace(t));
			}
		}finally {
			systemErr.println("ClientMain: [ERROR] eaglercraftx main thread has exited");
		}
	}

	/**
	 * Defined here to match the JS runtime
	 */
	public static void resetSettings() {
		boolean y = false;
		if (Window.confirm("Do you want to reset client settings?")) {
			PlatformApplication.setLocalStorage("g", null);
			PlatformApplication.setLocalStorage("p", null);
			y = true;
		}
		if (Window.confirm("Do you want to reset servers and relays?")) {
			PlatformApplication.setLocalStorage("r", null);
			PlatformApplication.setLocalStorage("s", null);
			y = true;
		}
		if (y) {
			Window.alert("Settings reset.");
		}
	}

	@Import(module = "platformRuntime", name = "getEaglercraftXOpts")
	private static native JSObject getEaglerXOpts();

}