package emu.grasscutter.scripts;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.script.Bindings;
import javax.script.CompiledScript;
import javax.script.ScriptException;

import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.jse.CoerceJavaToLua;

import emu.grasscutter.Grasscutter;
import emu.grasscutter.data.GameData;
import emu.grasscutter.data.def.MonsterData;
import emu.grasscutter.data.def.WorldLevelData;
import emu.grasscutter.game.entity.EntityGadget;
import emu.grasscutter.game.entity.EntityMonster;
import emu.grasscutter.game.entity.GameEntity;
import emu.grasscutter.game.world.Scene;
import emu.grasscutter.scripts.constants.EventType;
import emu.grasscutter.scripts.constants.ScriptGadgetState;
import emu.grasscutter.scripts.constants.ScriptRegionShape;
import emu.grasscutter.scripts.data.SceneBlock;
import emu.grasscutter.scripts.data.SceneConfig;
import emu.grasscutter.scripts.data.SceneGadget;
import emu.grasscutter.scripts.data.SceneGroup;
import emu.grasscutter.scripts.data.SceneInitConfig;
import emu.grasscutter.scripts.data.SceneMonster;
import emu.grasscutter.scripts.data.SceneSuite;
import emu.grasscutter.scripts.data.SceneTrigger;
import emu.grasscutter.scripts.data.ScriptArgs;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

public class SceneScriptManager {
	private final Scene scene;
	private final ScriptLib scriptLib;
	private final LuaValue scriptLibLua;
	private Bindings bindings;
	
	private SceneConfig config;
	private List<SceneBlock> blocks;
	private Int2ObjectOpenHashMap<Set<SceneTrigger>> triggers;
	private boolean isInit;
	
	public SceneScriptManager(Scene scene) {
		this.scene = scene;
		this.scriptLib = new ScriptLib(this);
		this.scriptLibLua = CoerceJavaToLua.coerce(this.scriptLib);
		this.triggers = new Int2ObjectOpenHashMap<>();
		
		// TEMPORARY
		if (this.getScene().getId() < 10) {
			return;
		}
		
		// Create
		this.init();
	}
	
	public Scene getScene() {
		return scene;
	}

	public ScriptLib getScriptLib() {
		return scriptLib;
	}
	
	public LuaValue getScriptLibLua() {
		return scriptLibLua;
	}

	public Bindings getBindings() {
		return bindings;
	}

	public SceneConfig getConfig() {
		return config;
	}

	public List<SceneBlock> getBlocks() {
		return blocks;
	}

	public Set<SceneTrigger> getTriggersByEvent(int eventId) {
		return triggers.computeIfAbsent(eventId, e -> new HashSet<>());
	}
	
	public void registerTrigger(SceneTrigger trigger) {
		getTriggersByEvent(trigger.event).add(trigger);
	}
	
	public void deregisterTrigger(SceneTrigger trigger) {
		getTriggersByEvent(trigger.event).remove(trigger);
	}
	
	// TODO optimize
	public SceneGroup getGroupById(int groupId) {
		for (SceneBlock block : this.getScene().getLoadedBlocks()) {
			for (SceneGroup group : block.groups) {
				if (group.id == groupId) {
					return group;
				}
			}
		}
		return null;
	}

	private void init() {
		// Get compiled script if cached
		CompiledScript cs = ScriptLoader.getScriptByPath(
			Grasscutter.getConfig().SCRIPTS_FOLDER + "Scene/" + getScene().getId() + "/scene" + getScene().getId() + "." + ScriptLoader.getScriptType());
		
		if (cs == null) {
			Grasscutter.getLogger().warn("No script found for scene " + getScene().getId());
			return;
		}
		
		// Create bindings
		bindings = ScriptLoader.getEngine().createBindings();
		
		// Set variables
		bindings.put("EventType", new EventType()); // TODO - make static class to avoid instantiating a new class every scene
		bindings.put("GadgetState", new ScriptGadgetState());
		bindings.put("RegionShape", new ScriptRegionShape());
		bindings.put("ScriptLib", getScriptLib());
		
		// Eval script
		try {
			cs.eval(getBindings());
			
			this.config = ScriptLoader.getSerializer().toObject(SceneConfig.class, bindings.get("scene_config"));
			
			// TODO optimize later
			// Create blocks
			List<Integer> blockIds = ScriptLoader.getSerializer().toList(Integer.class, bindings.get("blocks"));
			List<SceneBlock> blocks = ScriptLoader.getSerializer().toList(SceneBlock.class, bindings.get("block_rects"));
			
			for (int i = 0; i < blocks.size(); i++) {
				SceneBlock block = blocks.get(0);
				block.id = blockIds.get(i);
				
				loadBlockFromScript(block);
			}
			
			this.blocks = blocks;
		} catch (ScriptException e) {
			Grasscutter.getLogger().error("Error running script", e);
			return;
		}
		
		// TEMP
		this.isInit = true;
	}

	public boolean isInit() {
		return isInit;
	}
	
	private void loadBlockFromScript(SceneBlock block) {
		CompiledScript cs = ScriptLoader.getScriptByPath(
			Grasscutter.getConfig().SCRIPTS_FOLDER + "Scene/" + getScene().getId() + "/scene" + getScene().getId() + "_block" + block.id + "." + ScriptLoader.getScriptType());
	
		if (cs == null) {
			return;
		}
		
		// Eval script
		try {
			cs.eval(getBindings());
			
			// Set groups
			block.groups = ScriptLoader.getSerializer().toList(SceneGroup.class, bindings.get("groups"));
			block.groups.forEach(g -> g.block_id = block.id);
		} catch (ScriptException e) {
			Grasscutter.getLogger().error("Error loading block " + block.id + " in scene " + getScene().getId(), e);
		}
	}
	
	public void loadGroupFromScript(SceneGroup group) {
		// Set flag here so if there is no script, we dont call this function over and over again.
		group.setLoaded(true);
		
		CompiledScript cs = ScriptLoader.getScriptByPath(
			Grasscutter.getConfig().SCRIPTS_FOLDER + "Scene/" + getScene().getId() + "/scene" + getScene().getId() + "_group" + group.id + "." + ScriptLoader.getScriptType());
	
		if (cs == null) {
			return;
		}
		
		// Eval script
		try {
			cs.eval(getBindings());

			// Set
			group.monsters = ScriptLoader.getSerializer().toList(SceneMonster.class, bindings.get("monsters"));
			group.gadgets = ScriptLoader.getSerializer().toList(SceneGadget.class, bindings.get("gadgets"));
			group.triggers = ScriptLoader.getSerializer().toList(SceneTrigger.class, bindings.get("triggers"));
			group.suites = ScriptLoader.getSerializer().toList(SceneSuite.class, bindings.get("suites"));
			group.init_config = ScriptLoader.getSerializer().toObject(SceneInitConfig.class, bindings.get("init_config"));
		} catch (ScriptException e) {
			Grasscutter.getLogger().error("Error loading group " + group.id + " in scene " + getScene().getId(), e);
		}
	}

	public void onTick() {
		checkTriggers();
	}
	
	public void checkTriggers() {

	}
	
	public void spawnGadgetsInGroup(SceneGroup group) {
		for (SceneGadget g : group.gadgets) {
			EntityGadget entity = new EntityGadget(getScene(), g.gadget_id, g.pos);
			
			if (entity.getGadgetData() == null) continue;
			
			entity.setBlockId(group.block_id);
			entity.setConfigId(g.config_id);
			entity.setGroupId(group.id);
			entity.getRotation().set(g.rot);
			entity.setState(g.state);
			
			getScene().addEntity(entity);
			this.callEvent(EventType.EVENT_GADGET_CREATE, new ScriptArgs(entity.getConfigId()));
		}
	}
	
	public void spawnMonstersInGroup(SceneGroup group) {
		List<GameEntity> toAdd = new ArrayList<>();
		
		for (SceneMonster monster : group.monsters) {
			MonsterData data = GameData.getMonsterDataMap().get(monster.monster_id);
			
			if (data == null) {
				continue;
			}
			
			// Calculate level
			int level = monster.level;
			
			if (getScene().getDungeonData() != null) {
				level = getScene().getDungeonData().getShowLevel();
			} else if (getScene().getWorld().getWorldLevel() > 0) {
				WorldLevelData worldLevelData = GameData.getWorldLevelDataMap().get(getScene().getWorld().getWorldLevel());
				
				if (worldLevelData != null) {
					level = worldLevelData.getMonsterLevel();
				}
			}
			
			// Spawn mob
			EntityMonster entity = new EntityMonster(getScene(), data, monster.pos, level);
			entity.getRotation().set(monster.rot);
			entity.setGroupId(group.id);
			entity.setConfigId(monster.config_id);
			
			toAdd.add(entity);
		}
		
		if (toAdd.size() > 0) {
			getScene().addEntities(toAdd);
			
			for (GameEntity entity : toAdd) {
				callEvent(EventType.EVENT_ANY_MONSTER_LIVE, new ScriptArgs(entity.getConfigId()));
			}
		}
	}
	
	// Events
	
	public void callEvent(int eventType, ScriptArgs params) {
		for (SceneTrigger trigger : this.getTriggersByEvent(eventType)) {
			LuaValue condition = null;
			
			if (trigger.condition != null && !trigger.condition.isEmpty()) {
				condition = (LuaValue) this.getBindings().get(trigger.condition);
			}
			
			LuaValue ret = LuaValue.TRUE;
			
			if (condition != null) {
				LuaValue args = LuaValue.NIL;
				
				if (params != null) {
					args = CoerceJavaToLua.coerce(params);
				}
				
				ret = condition.call(this.getScriptLibLua(), args);
			}
			
			if (ret.checkboolean() == true) {
				LuaValue action = (LuaValue) this.getBindings().get(trigger.action);
				action.call(this.getScriptLibLua(), LuaValue.NIL);
			}
		}
	}
}
