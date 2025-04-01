package blue.endless.earlytags;

import blue.endless.earlytags.impl.DataInspector;
import net.fabricmc.api.ModInitializer;
import net.minecraft.util.Identifier;

public class EarlyTags implements ModInitializer{
	
	
	@Override
	public void onInitialize() {
		EarlyTag woolTag = get("block", Identifier.of("minecraft", "wool"));
		System.out.println(woolTag);
		System.out.println("contains lime wool? "+woolTag.contains(Identifier.of("minecraft", "lime_wool")));
		
		EarlyTag buttonsTag = get("block", Identifier.of("minecraft", "buttons"));
		System.out.println(buttonsTag);
	}
	
	public static EarlyTag get(String category, Identifier tagId) {
		return DataInspector.get(category, tagId);
	}
	
	public static EarlyTag getBannerPatternTag(Identifier tagId) {
		return DataInspector.get("banner_pattern", tagId);
	}
	
	public static EarlyTag getBlockTag(Identifier tagId) {
		return DataInspector.get("block", tagId);
	}
	
	public static EarlyTag getCatVariantTag(Identifier tagId) {
		return DataInspector.get("cat_variant", tagId);
	}
	
	public static EarlyTag getDamageTypeTag(Identifier tagId) {
		return DataInspector.get("damage_type", tagId);
	}
	
	public static EarlyTag getEnchantmentTag(Identifier tagId) {
		return DataInspector.get("enchantment", tagId);
	}
	
	public static EarlyTag getEntityTypeTag(Identifier tagId) {
		return DataInspector.get("entity_type", tagId);
	}
	
	public static EarlyTag getFluidTag(Identifier tagId) {
		return DataInspector.get("fluid", tagId);
	}
	
	public static EarlyTag getGameEventTag(Identifier tagId) {
		return DataInspector.get("game_event", tagId);
	}
	
	public static EarlyTag getInstrumentTag(Identifier tagId) {
		return DataInspector.get("instrument", tagId);
	}
	
	public static EarlyTag getItemTag(Identifier tagId) {
		return DataInspector.get("item", tagId);
	}
	
	public static EarlyTag getPaintingVariantTag(Identifier tagId) {
		return DataInspector.get("painting_variant", tagId);
	}
	
	public static EarlyTag getPointOfInterestTypeTag(Identifier tagId) {
		return DataInspector.get("point_of_interest_type", tagId);
	}
	
	public static EarlyTag getWorldgenTag(Identifier tagId) {
		return DataInspector.get("worldgen", tagId);
	}
	
}