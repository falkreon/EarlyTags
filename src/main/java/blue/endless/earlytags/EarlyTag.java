package blue.endless.earlytags;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableSet;

import net.minecraft.util.Identifier;

public record EarlyTag(Identifier id, ImmutableSet<Identifier> members) {
	
	public boolean contains(Identifier value) {
		return members.contains(value);
	}
	
	public static EarlyTag empty(Identifier id) {
		return new EarlyTag(id, ImmutableSet.of());
	}
	
	public static class Builder {
		private final Identifier id;
		private ImmutableSet.Builder<Identifier> setBuilder = new ImmutableSet.Builder<Identifier>();
		private Set<Identifier> incorporate = new HashSet<>();
		
		public Builder(Identifier id) {
			this.id = id;
		}
		
		public Identifier getId() {
			return id;
		}
		
		public void add(Identifier id) {
			setBuilder.add(id);
		}
		
		public void addReference(Identifier id) {
			incorporate.add(id);
		}
		
		public void clear() {
			setBuilder = new ImmutableSet.Builder<Identifier>();
			incorporate.clear();
		}
		
		public EarlyTag build() {
			return new EarlyTag(id, setBuilder.build());
		}
		
		public boolean incorporate(Map<Identifier, EarlyTag> tagMap) {
			Set<Identifier> toResolve = new HashSet<>();
			for(Identifier id : incorporate) {
				EarlyTag toIncorporate = tagMap.get(id);
				if (toIncorporate != null) {
					toResolve.add(id);
					setBuilder.addAll(toIncorporate.members());
				}
			}
			
			boolean result = !toResolve.isEmpty();
			incorporate.removeAll(toResolve);
			return result;
		}
		
		public boolean isReady() {
			return incorporate.isEmpty();
		}
		
		@Override
		public String toString() {
			return setBuilder.build().toString();
		}
	}
}
