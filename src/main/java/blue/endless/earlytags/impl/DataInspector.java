package blue.endless.earlytags.impl;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import blue.endless.earlytags.EarlyTag;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.util.Identifier;

public class DataInspector {
	// This Object also serves as the mutex for tagMap
	private static final Set<String> UNCHECKED_CONTAINERS = Set.of(
			"minecraft",
			"java",
			"mixinextras",
			"fabricloader",
			"fabric-api"
			);
	private static final int MAX_RESOLVE_TRIES = 100;
	
	private static ImmutableMap<String, ImmutableMap<Identifier, EarlyTag>> tagMap = null;
	
	private static void parseTag(String json, EarlyTag.Builder builder) {
		JsonElement rootElem = JsonParser.parseString(json);
		if (rootElem instanceof JsonObject rootObj) {
			JsonElement replaceField = rootObj.get("replace");
			boolean replace = (replaceField != null && replaceField instanceof JsonPrimitive prim) ? prim.getAsBoolean() : true;
			JsonElement valuesField = rootObj.get("values");
			if (valuesField instanceof JsonArray valuesArr) {
				if (replace) builder.clear();
				
				for(JsonElement arrElem : valuesArr) {
					if (arrElem instanceof JsonPrimitive arrPrim && arrPrim.isString()) {
						String value = arrPrim.getAsString();
						if (value.startsWith("#")) {
							// Handle differently
							builder.addReference(Identifier.tryParse(value.substring(1)));
						} else {
							Identifier id = Identifier.tryParse(value);
							if (id != null) {
								builder.add(id);
							}
						}
					} else if (arrElem instanceof JsonObject) {
						// This is typically a "required:false" thing, let's skip it for now.
					}
				}
			} else {
				return; // No values to add or replace
			}
		}
	}
	
	private static void processTagNode(String category, String namespace, String basePath, Path base, Map<Identifier, EarlyTag.Builder> categoryMap) {
		if (!Files.exists(base)) return;
		
		if (Files.isDirectory(base)) {
			try {
				Files.list(base).forEachOrdered(tagPath -> {
					String elemName = tagPath.getName(tagPath.getNameCount() - 1).toString();
					String subPath = (basePath.isBlank()) ? elemName : basePath + '/' + elemName;
					processTagNode(category, namespace, subPath, tagPath, categoryMap);
				});
			} catch (IOException ex) {}
		} else {
			if (basePath.endsWith(".json")) {
				String tagPath = basePath.substring(0, basePath.lastIndexOf("."));
				Identifier tagId = Identifier.of(namespace, tagPath);
				
				try {
					String json = Files.readString(base, StandardCharsets.UTF_8);
					
					EarlyTag.Builder builder = categoryMap.computeIfAbsent(tagId, EarlyTag.Builder::new);
					
					DataInspector.parseTag(json, builder);
					
					
				} catch (IOException e) {
					e.printStackTrace();
				}
				
			}
		}
	}
	
	private static void processPath(String modid, Path base, Map<String, Map<Identifier, EarlyTag.Builder>> builderMap) {
		Path dataPath = base.resolve("data");
		if (Files.exists(dataPath)) {
			try {
				Files.list(dataPath).forEachOrdered(namespacePath -> {
					String namespace = namespacePath.getName(namespacePath.getNameCount() - 1).toString();
					
					Path tagsPath = namespacePath.resolve("tags");
					if (Files.isDirectory(tagsPath)) {
						
						try {
							Files.list(tagsPath).forEachOrdered(tagPath -> {
								String category = tagPath.getName(tagPath.getNameCount() - 1).toString();
								Map<Identifier, EarlyTag.Builder> categoryMap = builderMap.computeIfAbsent(category, it -> new HashMap<>());
								
								DataInspector.processTagNode(category, namespace, "", tagPath, categoryMap);
							});
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				});
				
			} catch (IOException e) {}
		}
	}
	
	private static Map<String, Map<Identifier, EarlyTag.Builder>> createBuilderMap() {
		Map<String, Map<Identifier, EarlyTag.Builder>> builderMap = new HashMap<>();
		
		// First, apply Minecraft
		Optional<ModContainer> maybeContainer = FabricLoader.getInstance().getModContainer("minecraft");
		if (maybeContainer.isPresent()) {
			for(Path path : maybeContainer.get().getRootPaths()) {
				//System.out.println("Processing Minecraft data: "+path.toString());
				DataInspector.processPath("minecraft", path, builderMap);
			}
		}
		
		// Next, apply mods
		for(ModContainer container : FabricLoader.getInstance().getAllMods()) {
			String modId = container.getMetadata().getId();
			if (UNCHECKED_CONTAINERS.contains(modId)) continue;
			
			//System.out.println("Processing Mod data: "+modId);
			for(Path path : container.getRootPaths()) {
				DataInspector.processPath(modId, path, builderMap);
			}
		}
		
		// Finally, apply "required" datapacks
		Path path = FabricLoader.getInstance().getGameDir().resolve("global_packs").resolve("required_data");
		if (Files.exists(path)) { // Supports symbolic links
			try {
				Files.list(path).forEachOrdered(sub -> {
					//System.out.println("Datapack: "+sub.toString());
					if (Files.isDirectory(sub)) {
						//System.out.println("Processing folder datapack");
						DataInspector.processPath("required_data", sub, builderMap);
					} else {
						String filename = sub.getName(sub.getNameCount() - 1).toString();
						if (filename.endsWith(".zip")) {
							// TODO: Process zipped datapacks
						}
					}
				});
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		return builderMap;
	}
	
	private static boolean resolveOneStep(Map<Identifier, EarlyTag.Builder> builderMap, Map<Identifier, EarlyTag> resolvedMap) {
		boolean hasDoneAnything = false;
		Set<Identifier> resolved = new HashSet<>();
		for(EarlyTag.Builder builder : builderMap.values()) {
			hasDoneAnything |= builder.incorporate(resolvedMap);
			if (builder.isReady()) {
				resolved.add(builder.getId());
				resolvedMap.put(builder.getId(), builder.build());
				hasDoneAnything = true;
			}
		}
		
		return hasDoneAnything;
	}
	
	private static ImmutableMap<Identifier, EarlyTag> resolve(Map<Identifier, EarlyTag.Builder> builderMap) {
		Map<Identifier, EarlyTag> result = new HashMap<>();
		for(int i=0; i<MAX_RESOLVE_TRIES; i++) {
			if (!resolveOneStep(builderMap, result)) break;
		}
		
		return ImmutableMap.copyOf(result);
	}
	
	private static ImmutableMap<String, ImmutableMap<Identifier, EarlyTag>> resolveAll(Map<String, Map<Identifier, EarlyTag.Builder>> builderMap) {
		HashMap<String, ImmutableMap<Identifier, EarlyTag>> result = new HashMap<>();
		for(Map.Entry<String, Map<Identifier, EarlyTag.Builder>> entry : builderMap.entrySet()) {
			result.put(entry.getKey(), resolve(entry.getValue()));
		}
		
		return ImmutableMap.copyOf(result);
	}
	
	private static void ensureStaticMap() {
		synchronized(UNCHECKED_CONTAINERS) {
			if (tagMap == null) {
				Map<String, Map<Identifier, EarlyTag.Builder>> builderMap = DataInspector.createBuilderMap();
				tagMap = DataInspector.resolveAll(builderMap);
			}
		}
	}
	
	public static EarlyTag get(String category, Identifier tagId) {
		ensureStaticMap();
		
		ImmutableMap<Identifier, EarlyTag> categoryMap = tagMap.get(category);
		if (categoryMap == null) {
			return EarlyTag.empty(tagId);
		}
		
		EarlyTag result = categoryMap.get(tagId);
		return (result != null) ? result : EarlyTag.empty(tagId);
	}
}
