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

package net.deltaclient;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import net.lax1dude.eaglercraft.internal.PlatformAssets;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.RecipeManager;
import net.minecraft.util.ResourceLocation;

/**
 I had to write this class because the
 goddamn recipes wont load in WASM. :>
 */
public class RecipeLoaderWASM {

	private static final Logger logger = LogManager.getLogger("RecipeLoaderWASM");
	private static final Gson gson = (new GsonBuilder()).setPrettyPrinting().disableHtmlEscaping().create();

	/**
	 * Loads all non-dynamic recipes from EPK assets via PlatformAssets.
	 * Returns an empty list if no recipe data is available (e.g. on platforms
	 * that don't have EPK assets loaded).
	 */
	public static Collection<IRecipe<?>> loadRecipesFromAssets() {
		List<IRecipe<?>> recipes = new ArrayList<>();

		List<String> recipePaths = PlatformAssets.getAllResourcePaths("data/minecraft/recipes");
		if (recipePaths.isEmpty()) {
			logger.debug("No recipe files found in assets (data/minecraft/recipes/)");
			return recipes;
		}

		logger.info("Loading {} recipe files from EPK assets", recipePaths.size());

		for (String path : recipePaths) {
			// Path format: "data/<namespace>/recipes/<name>.json"
			// Strip "data/" prefix -> "<namespace>/recipes/<name>.json"
			String relPath = path.startsWith("data/") ? path.substring(5) : path;
			int slashIdx = relPath.indexOf('/');
			if (slashIdx == -1) {
				continue;
			}
			String namespace = relPath.substring(0, slashIdx);
			String remaining = relPath.substring(slashIdx + 1);
			// "recipes/<name>.json" -> ResourceLocation namespace:name
			if (!remaining.startsWith("recipes/")) {
				continue;
			}
			String recipeName = remaining.substring(8);
			if (!recipeName.endsWith(".json")) {
				continue;
			}
			recipeName = recipeName.substring(0, recipeName.length() - 5);

			byte[] data = PlatformAssets.getResourceBytes(path);
			if (data == null) {
				logger.warn("Recipe file {} returned null data", path);
				continue;
			}

			String jsonStr = new String(data, StandardCharsets.UTF_8);
			try {
				JsonObject json = gson.fromJson(jsonStr, JsonObject.class);
				if (json == null) {
					logger.warn("Recipe {} is empty or invalid JSON", path);
					continue;
				}
				ResourceLocation id = new ResourceLocation(namespace, recipeName);

				// Skip dynamic recipes (they are handled programmatically)
				String type = json.get("type").getAsString();
				if ("minecraft:crafting_special_armordye".equals(type)
						|| type.contains("special")) {
					continue;
				}

				IRecipe<?> recipe = RecipeManager.deserializeRecipe(id, json);
				if (recipe != null && !recipe.isDynamic()) {
					recipes.add(recipe);
				}
			} catch (Exception e) {
				logger.error("Failed to load recipe {}: {}", path, e.toString());
			}
		}

		logger.info("Loaded {} recipes from EPK assets", recipes.size());
		return recipes;
	}
}
