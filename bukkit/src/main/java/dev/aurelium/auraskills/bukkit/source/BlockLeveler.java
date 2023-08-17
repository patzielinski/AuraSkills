package dev.aurelium.auraskills.bukkit.source;

import dev.aurelium.auraskills.api.ability.Abilities;
import dev.aurelium.auraskills.api.skill.Skill;
import dev.aurelium.auraskills.api.source.type.BlockXpSource;
import dev.aurelium.auraskills.bukkit.AuraSkills;
import dev.aurelium.auraskills.bukkit.skills.excavation.ExcavationAbilities;
import dev.aurelium.auraskills.bukkit.skills.farming.FarmingAbilities;
import dev.aurelium.auraskills.bukkit.skills.foraging.ForagingAbilities;
import dev.aurelium.auraskills.bukkit.skills.mining.MiningAbilities;
import dev.aurelium.auraskills.common.source.SourceTag;
import dev.aurelium.auraskills.common.source.SourceType;
import dev.aurelium.auraskills.common.user.User;
import dev.aurelium.auraskills.common.util.data.Pair;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class BlockLeveler extends SourceLeveler {

    public BlockLeveler(AuraSkills plugin) {
        super(plugin, SourceType.BLOCK);
    }

    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        if (disabled()) return;

        Player player = event.getPlayer();
        User user = plugin.getUser(player);

        Pair<BlockXpSource, Skill> sourcePair = getSource(event.getBlock(), BlockXpSource.BlockTriggers.BREAK);
        if (sourcePair == null) {
            return;
        }

        BlockXpSource source = sourcePair.first();
        Skill skill = sourcePair.second();

        // Check for player placed blocks
        if (source.checkReplace() && plugin.getRegionManager().isPlacedBlock(event.getBlock())) {
            return;
        }

        if (failsChecks(event, player, event.getBlock().getLocation(), skill)) return;

        plugin.getLevelManager().addXp(user, skill, source.getXp());
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (disabled()) return;
        Player player = event.getPlayer();
        User user = plugin.getUser(player);

        Block block = event.getClickedBlock();
        if (block == null) return;

        Pair<BlockXpSource, Skill> sourcePair = getSource(block, BlockXpSource.BlockTriggers.INTERACT);
        if (sourcePair == null) {
            return;
        }

        BlockXpSource source = sourcePair.first();
        Skill skill = sourcePair.second();

        if (failsChecks(event, player, block.getLocation(), skill)) return;

        plugin.getLevelManager().addXp(user, skill, source.getXp());
        applyAbilities(skill, player, user, block, source);
    }

    private void applyAbilities(Skill skill, Player player, User user, Block block, BlockXpSource source) {
        var abilities = plugin.getAbilityManager();
        var tags = plugin.getSkillManager();
        if (skill.getAbilities().contains(Abilities.BOUNTIFUL_HARVEST) && tags.hasTag(source, SourceTag.BOUNTIFUL_HARVEST_APPLICABLE)) {
            abilities.getAbilityImpl(FarmingAbilities.class).bountifulHarvest(player, user, block);
        }
        if (skill.getAbilities().contains(Abilities.TRIPLE_HARVEST) && tags.hasTag(source, SourceTag.TRIPLE_HARVEST_APPLICABLE)) {
            abilities.getAbilityImpl(FarmingAbilities.class).tripleHarvest(player, user, block);
        }
        if (skill.getAbilities().contains(Abilities.LUMBERJACK) && tags.hasTag(source, SourceTag.LUMBERJACK_APPLICABLE)) {
            abilities.getAbilityImpl(ForagingAbilities.class).lumberjack(player, user, block);
        }
        if (skill.getAbilities().contains(Abilities.LUCKY_MINER) && tags.hasTag(source, SourceTag.LUCKY_MINER_APPLICABLE)) {
            abilities.getAbilityImpl(MiningAbilities.class).luckyMiner(player, user, block);
        }
        if (skill.getAbilities().contains(Abilities.BIGGER_SCOOP) && tags.hasTag(source, SourceTag.BIGGER_SCOOP_APPLICABLE)) {
            abilities.getAbilityImpl(ExcavationAbilities.class).biggerScoop(player, user, block);
        }
    }

    public boolean matchesSource(Block block, BlockXpSource source, BlockXpSource.BlockTriggers trigger) {
        var sourcePair = getSource(block, trigger);
        if (sourcePair == null) {
            return false;
        }
        return sourcePair.first().equals(source);
    }

    @Nullable
    public Pair<BlockXpSource, Skill> getSource(Block block, BlockXpSource.BlockTriggers trigger) {
        Map<BlockXpSource, Skill> sources = plugin.getSkillManager().getSourcesOfType(BlockXpSource.class);
        sources = filterByTrigger(sources, trigger);
        for (Map.Entry<BlockXpSource, Skill> entry : sources.entrySet()) {
            BlockXpSource source = entry.getKey();

            // Check block type (material)
            boolean blockMatches = false;
            for (String blockName : source.getBlocks()) {
                if (block.getType().name().equalsIgnoreCase(blockName)) {
                    blockMatches = true;
                    break;
                }
            }
            if (!blockMatches) {
                continue;
            }

            // Check block state
            boolean anyStateMatches = true;
            if (source.getStates() != null) {
                anyStateMatches = false;
                // Convert block data to json
                String blockDataString = block.getBlockData().getAsString(true);
                Map<String, Object> blockDataMap = parseFromBlockData(blockDataString);
                // Check if block data matches defined states
                for (BlockXpSource.BlockXpSourceState state : source.getStates()) {
                    if (state == null) continue;
                    boolean stateMatches = true;
                    for (Map.Entry<String, Object> stateEntry : state.getStateMap().entrySet()) {
                        String key = stateEntry.getKey();
                        Object value = stateEntry.getValue();
                        if (!blockDataMap.containsKey(key)) {
                            stateMatches = false;
                            break;
                        }
                        if (!blockDataMap.get(key).equals(value)) {
                            stateMatches = false;
                            break;
                        }
                    }
                    // If one state matches, then the block matches and we can stop checking
                    if (stateMatches) {
                        anyStateMatches = true;
                        break;
                    }
                }
            }
            // Skip if no state matches
            if (!anyStateMatches) {
                continue;
            }

            return Pair.fromEntry(entry);
        }
        return null;
    }

    private Map<BlockXpSource, Skill> filterByTrigger(Map<BlockXpSource, Skill> sources, BlockXpSource.BlockTriggers trigger) {
        Map<BlockXpSource, Skill> filtered = new HashMap<>();
        for (Map.Entry<BlockXpSource, Skill> entry : sources.entrySet()) {
            BlockXpSource source = entry.getKey();
            Skill skill = entry.getValue();
            // Check if trigger matches any of the source triggers
            for (BlockXpSource.BlockTriggers sourceTrigger : source.getTriggers()) {
                if (sourceTrigger == trigger) {
                    filtered.put(source, skill);
                    break;
                }
            }
        }
        return filtered;
    }

    private Map<String, Object> parseFromBlockData(String input) {
        Map<String, Object> result = new HashMap<>();
        // Check if the input is valid
        if (input == null || input.isEmpty()) {
            return result;
        }
        // Find the index of the first bracket
        int bracketIndex = input.indexOf("[");
        // Check if the bracket exists
        if (bracketIndex == -1) {
            return result;
        }
        // Get the part of the input after the bracket and remove the closing bracket
        String data = input.substring(bracketIndex + 1).replace("]", "");
        // Find the index of the first comma
        int commaIndex = data.indexOf(",");
        // Loop until there are no more commas
        while (commaIndex != -1) {
            // Get the pair before the comma
            String pair = data.substring(0, commaIndex);
            // Find the index of the equal sign
            int equalIndex = pair.indexOf("=");
            // Check if the equal sign exists
            if (equalIndex != -1) {
                // Get the key and value and trim any whitespace
                String key = pair.substring(0, equalIndex).trim();
                String value = pair.substring(equalIndex + 1).trim();
                // Parse the value and put it in the result map with the key
                result.put(key, parseValue(value));
            }
            // Remove the pair and the comma from the data
            data = data.substring(commaIndex + 1);
            // Find the next comma index
            commaIndex = data.indexOf(",");
        }
        // Check if there is any remaining data
        if (!data.isEmpty()) {
            // Find the index of the equal sign
            int equalIndex = data.indexOf("=");
            // Check if the equal sign exists
            if (equalIndex != -1) {
                // Get the key and value and trim any whitespace
                String key = data.substring(0, equalIndex).trim();
                String value = data.substring(equalIndex + 1).trim();
                // Parse the value and put it in the result map with the key
                result.put(key, parseValue(value));
            }
        }
        // Return the result map
        return result;
    }

    private Object parseValue(String value) {
        // Try to parse as an int
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            // Ignore and continue
        }
        // Try to parse as a double
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException ignored) {
            // Ignore and continue
        }
        // Try to parse as a boolean
        if (value.equals("true")) {
            return true;
        }
        if (value.equals("false")) {
            return false;
        }
        // Return as a String otherwise
        return value;
    }

}
