/*
 * Copyright (c) 2019-2022 GeyserMC. http://geysermc.org
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 * @author GeyserMC
 * @link https://github.com/GeyserMC/Geyser
 */

package org.geysermc.geyser.translator.protocol.java;

import com.google.common.base.Suppliers;
import it.unimi.dsi.fastutil.Hash;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenCustomHashMap;
import lombok.Getter;
import lombok.ToString;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.format.NamedTextColor;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.cloudburstmc.protocol.bedrock.data.command.CommandData;
import org.cloudburstmc.protocol.bedrock.data.command.CommandEnumConstraint;
import org.cloudburstmc.protocol.bedrock.data.command.CommandEnumData;
import org.cloudburstmc.protocol.bedrock.data.command.CommandOverloadData;
import org.cloudburstmc.protocol.bedrock.data.command.CommandParam;
import org.cloudburstmc.protocol.bedrock.data.command.CommandParamData;
import org.cloudburstmc.protocol.bedrock.data.command.CommandPermission;
import org.cloudburstmc.protocol.bedrock.packet.AvailableCommandsPacket;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.api.event.java.ServerDefineCommandsEvent;
import org.geysermc.geyser.api.util.PlatformType;
import org.geysermc.geyser.command.CommandRegistry;
import org.geysermc.geyser.registry.java.BuiltInJavaRegistries;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.registry.java.JavaRegistries;
import org.geysermc.geyser.translator.protocol.PacketTranslator;
import org.geysermc.geyser.translator.protocol.Translator;
import org.geysermc.geyser.util.EntityUtils;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.network.protocol.game.ClientboundCommandsPacket;

import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import org.geysermc.geyser.vanilla.protocol.VanillaCommands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Objects;
import org.geysermc.geyser.util.MinecraftKey;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

@SuppressWarnings("removal") // We know. This is our doing.
@Translator(packet = ClientboundCommandsPacket.class)
public class JavaCommandsTranslator extends PacketTranslator<ClientboundCommandsPacket> {

    /**
     * Wait until the registries load before getting all the block names.
     */
    private static final Supplier<String[]> ALL_BLOCK_NAMES = Suppliers.memoize(() -> BuiltInJavaRegistries.BLOCK.keys().stream().map(Key::asString).toArray(String[]::new));
    private static final String[] ALL_EFFECT_IDENTIFIERS = EntityUtils.getAllEffectIdentifiers();
    private static final String[] ATTRIBUTES = net.minecraft.core.registries.BuiltInRegistries.ATTRIBUTE.keySet().stream().map(Object::toString).toArray(String[]::new);
    private static final String[] ENUM_BOOLEAN = {"true", "false"};
    private static final String[] VALID_COLORS;
    private static final String[] VALID_SCOREBOARD_SLOTS;

    private static final Hash.Strategy<BedrockCommandInfo> PARAM_STRATEGY = new Hash.Strategy<>() {
        @Override
        public int hashCode(BedrockCommandInfo o) {
            int paramHash = Arrays.deepHashCode(o.paramData());
            if ("help".equals(o.name())) {
                paramHash = 31 * paramHash + 1;
            }
            return 31 * paramHash + o.description().hashCode();
        }

        @Override
        public boolean equals(BedrockCommandInfo a, BedrockCommandInfo b) {
            if (a == b) return true;
            if (a == null || b == null) return false;
            if ("help".equals(a.name()) && !"help".equals(b.name())) {
                // Merging this causes Bedrock to fallback to its own help command
                // Tested on Paper 1.20.4 with Essentials and Bedrock 1.21
                // https://github.com/GeyserMC/Geyser/issues/2573
                return false;
            }
            if (!a.description().equals(b.description())) return false;
            if (a.paramData().length != b.paramData().length) return false;
            for (int i = 0; i < a.paramData().length; i++) {
                CommandParamData[] a1 = a.paramData()[i].getOverloads();
                CommandParamData[] b1 = b.paramData()[i].getOverloads();
                if (a1.length != b1.length) return false;

                for (int j = 0; j < a1.length; j++) {
                    if (!a1[j].equals(b1[j])) return false;
                }
            }
            return true;
        }
    };

    static {
        VALID_COLORS = NamedTextColor.NAMES.keys().toArray(new String[0]);

        List<String> teamOptions = new ArrayList<>(Arrays.asList("list", "sidebar", "belowName"));
        for (String color : NamedTextColor.NAMES.keys()) {
            teamOptions.add("sidebar.team." + color);
        }
        VALID_SCOREBOARD_SLOTS = teamOptions.toArray(new String[0]);
    }

    @Override
    public void translate(GeyserSession session, ClientboundCommandsPacket packet) {
        // Don't send command suggestions if they are disabled
        if (!session.getGeyser().config().gameplay().commandSuggestions()) {
            session.getGeyser().getLogger().debug("Not sending translated command suggestions as they are disabled.");

            // Send a mostly empty packet so Bedrock doesn't override /help with its own, built-in help command.
            AvailableCommandsPacket emptyPacket = new AvailableCommandsPacket();
            emptyPacket.getCommands().add(createFakeHelpCommand());
            session.sendUpstreamPacket(emptyPacket);
            return;
        }

        CommandRegistry registry = session.getGeyser().commandRegistry();
        CommandNode<?> rootNode = VanillaCommands.root(packet, session.vanillaRegistries(), session.getEnabledFeatures());
        List<CommandData> commandData = new ArrayList<>();
        Set<String> knownAliases = new HashSet<>();
        Map<BedrockCommandInfo, Set<String>> commands = new Object2ObjectOpenCustomHashMap<>(PARAM_STRATEGY);


        List<String> knownCommands = new ArrayList<>();
        List<String> restrictedCommands = new ArrayList<>();
        // Loop through the root nodes to get all commands
        for (CommandNode<?> node : rootNode.getChildren()) {

            // Make sure we don't have duplicated commands (happens if there is more than 1 root node)
            if (!knownAliases.add(node.getName().toLowerCase(Locale.ROOT))) continue;

            CommandOverloadData[] params = getParams(session, node);

            // Insert the alias name into the command list
            String name = node.getName().toLowerCase(Locale.ROOT);
            String description = registry.description(name, session.locale());
            BedrockCommandInfo info = new BedrockCommandInfo(name, description, params);
            commands.computeIfAbsent(info, $ -> new HashSet<>()).add(name);

            // Add the command to the command lists
            knownCommands.add(name);
            if (VanillaCommands.restricted(node)) { // Name is a bit confusing - this is what we want
                restrictedCommands.add(name);
            }
        }

        session.setKnownCommands(knownCommands);
        session.setRestrictedCommands(restrictedCommands);

        var eventBus = session.getGeyser().eventBus();

        var event = new ServerDefineCommandsEvent(session, commands.keySet());
        eventBus.fire(event);
        if (event.isCancelled()) {
            return;
        }

        var oldEvent = new org.geysermc.geyser.api.event.downstream.ServerDefineCommandsEvent(session, commands.keySet());
        eventBus.fire(oldEvent);
        if (oldEvent.isCancelled()) {
            return;
        }

        // The command flags, set to NOT_CHEAT so known commands can be used while achievements are enabled.
        Set<CommandData.Flag> flags = Set.of(CommandData.Flag.NOT_CHEAT);

        boolean helpAdded = false;

        // Loop through all the found commands
        for (Map.Entry<BedrockCommandInfo, Set<String>> entry : commands.entrySet()) {
            String commandName = entry.getValue().iterator().next(); // We know this has a value

            LinkedHashMap<String, Set<CommandEnumConstraint>> values = new LinkedHashMap<>();
            // Is this right?
            for (String s : entry.getValue()) {
                values.put(s, EnumSet.of(CommandEnumConstraint.ALLOW_ALIASES));
            }

            // Create a basic alias
            CommandEnumData aliases = new CommandEnumData(commandName + "Aliases", values, false);

            // Fetch command description
            String description = entry.getKey().description();

            // Command suggestion list is illegible if a command description contains line breaks
            int lineBreak = description.indexOf('\n');
            if (lineBreak >= 0) {
                description = description.substring(0, lineBreak);
            }

            // Since 1.21.130, the maximum description length is 1000 characters
            // https://www.minecraft.net/en-us/article/minecraft-1-21-130-bedrock-changelog
            // As issues are still experienced at that length, truncate descriptions at 950 characters
            if (description.length() > 950) {
                description = description.substring(0, 947) + "...";
            }

            // Build the completed command and add it to the final list
            CommandData data = new CommandData(commandName, description, flags, CommandPermission.ANY, aliases, Collections.emptyList(), entry.getKey().paramData());
            commandData.add(data);

            if (commandName.equals("help")) {
                helpAdded = true;
            }
        }

        if (!helpAdded) {
            // https://github.com/GeyserMC/Geyser/issues/2573 if Brigadier does not send the help command.
            commandData.add(createFakeHelpCommand());
        }

        if (session.getGeyser().platformType() == PlatformType.STANDALONE) {
            session.getGeyser().commandRegistry().export(session, commandData, knownAliases);
        }

        // Add our commands to the AvailableCommandsPacket for the bedrock client
        AvailableCommandsPacket availableCommandsPacket = new AvailableCommandsPacket();
        availableCommandsPacket.getCommands().addAll(commandData);

        session.getGeyser().getLogger().debug("Sending command packet of " + commandData.size() + " commands");

        // Finally, send the commands to the client
        session.sendUpstreamPacket(availableCommandsPacket);
    }

    /**
     * Build the command parameter array for the given command
     *
     * @param session the session
     * @param commandNode The command to build the parameters for
     * @return An array of parameter option arrays
     */
    private static CommandOverloadData[] getParams(GeyserSession session, CommandNode<?> commandNode) {
        // Check if the command is an alias and redirect it
        if (commandNode.getRedirect() != null) {
            GeyserImpl.getInstance().getLogger().debug("Redirecting command " + commandNode.getName() + " to " + commandNode.getRedirect().getName());
            commandNode = commandNode.getRedirect();
        }

        if (!commandNode.getChildren().isEmpty()) {
            // Create the root param node and build all the children
            ParamInfo rootParam = new ParamInfo(commandNode, null);
            rootParam.buildChildren(new CommandBuilderContext(session), Collections.newSetFromMap(new IdentityHashMap<>()));

            List<CommandOverloadData> treeData = rootParam.getTree();

            return treeData.toArray(new CommandOverloadData[0]);
        }

        return new CommandOverloadData[0];
    }

    /**
     * Convert Java edition command types to Bedrock edition
     *
     * @param context the session's command context
     * @param node Command type to convert
     * @return Bedrock parameter data type
     */
    private static Object mapCommandType(CommandBuilderContext context, CommandNode<?> node) {
        if (!(node instanceof ArgumentCommandNode<?, ?> argumentNode)) {
            return CommandParam.STRING;
        }
        var argument = VanillaCommands.describe(argumentNode.getType());
        return switch (argument.type().toString()) {
            case "brigadier:float", "minecraft:rotation", "brigadier:double" -> CommandParam.FLOAT;
            case "brigadier:integer", "brigadier:long" -> CommandParam.INT;
            case "minecraft:entity", "minecraft:game_profile" -> CommandParam.TARGET;
            case "minecraft:block_pos" -> CommandParam.BLOCK_POSITION;
            case "minecraft:column_pos", "minecraft:vec3" -> CommandParam.POSITION;
            case "minecraft:message" -> CommandParam.MESSAGE;
            case "minecraft:nbt_compound_tag", "minecraft:nbt_tag", "minecraft:nbt_path" -> CommandParam.JSON;
            case "minecraft:resource_location", "minecraft:function" -> CommandParam.FILE_PATH;
            case "brigadier:bool" -> ENUM_BOOLEAN;
            case "minecraft:operation" -> CommandParam.OPERATOR;
            case "minecraft:block_state" -> ALL_BLOCK_NAMES.get();
            case "minecraft:item_stack" -> context.getItemNames();
            case "minecraft:team_color" -> VALID_COLORS;
            case "minecraft:scoreboard_slot" -> VALID_SCOREBOARD_SLOTS;
            case "minecraft:resource", "minecraft:resource_key" -> handleResource(context, MinecraftKey.key(argument.registry().orElseThrow().toString()), false);
            case "minecraft:resource_or_tag", "minecraft:resource_or_tag_key" -> handleResource(context, MinecraftKey.key(argument.registry().orElseThrow().toString()), true);
            case "minecraft:dimension" -> context.session.getLevels();
            case "minecraft:team" -> context.getTeams();
            default -> CommandParam.STRING;
        };
    }

    private static Object handleResource(CommandBuilderContext context, Key resource, boolean tags) {
        return switch (resource.asString()) {
            case "minecraft:attribute" -> ATTRIBUTES;
            case "minecraft:enchantment" -> context.getEnchantments();
            case "minecraft:entity_type" -> context.getEntityTypes();
            case "minecraft:mob_effect" -> ALL_EFFECT_IDENTIFIERS;
            case "minecraft:worldgen/biome" -> tags ? context.getBiomesWithTags() : context.getBiomes();
            default -> CommandParam.STRING;
        };
    }

    private CommandData createFakeHelpCommand() {
        CommandEnumData aliases = new CommandEnumData("helpAliases", Map.of("help", EnumSet.of(CommandEnumConstraint.ALLOW_ALIASES)), false);
        return new CommandData("help", "", Set.of(CommandData.Flag.NOT_CHEAT), CommandPermission.ANY, aliases, Collections.emptyList(), new CommandOverloadData[0]);
    }

    /**
     * Stores the command description and parameter data for best optimizing the Bedrock commands packet.
     */
    private record BedrockCommandInfo(String name, String description, CommandOverloadData[] paramData) implements
            org.geysermc.geyser.api.event.downstream.ServerDefineCommandsEvent.CommandInfo,
            ServerDefineCommandsEvent.CommandInfo
    {
    }

    /**
     * Stores command completions so we don't have to rebuild the same values multiple times.
     */
    @MonotonicNonNull
    private static class CommandBuilderContext {
        private final GeyserSession session;
        private Object biomesWithTags;
        private Object biomesNoTags;
        private String[] enchantments;
        private String[] entityTypes;
        private String[] itemNames;
        private CommandEnumData teams;

        CommandBuilderContext(GeyserSession session) {
            this.session = session;
        }

        private Object getBiomes() {
            if (biomesNoTags != null) {
                return biomesNoTags;
            }

            String[] identifiers = session.getGeyser().getWorldManager().getBiomeIdentifiers(false);
            return (biomesNoTags = identifiers != null ? identifiers : CommandParam.STRING);
        }

        private Object getBiomesWithTags() {
            if (biomesWithTags != null) {
                return biomesWithTags;
            }

            String[] identifiers = session.getGeyser().getWorldManager().getBiomeIdentifiers(true);
            return (biomesWithTags = identifiers != null ? identifiers : CommandParam.STRING);
        }

        private String[] getEnchantments() {
            if (enchantments != null) {
                return enchantments;
            }
            return (enchantments = session.javaRegistries().registry(JavaRegistries.ENCHANTMENT).keys().stream().map(Key::asString).toArray(String[]::new));
        }

        private String[] getEntityTypes() {
            if (entityTypes != null) {
                return entityTypes;
            }
            return (entityTypes = BuiltInJavaRegistries.ENTITY_TYPE.keys().stream().map(Key::asString).toArray(String[]::new));
        }

        public String[] getItemNames() {
            if (itemNames != null) {
                return itemNames;
            }
            return (itemNames = BuiltInJavaRegistries.ITEM.keys().stream().map(Key::asString).toArray(String[]::new));
        }

        private CommandEnumData getTeams() {
            if (teams != null) {
                return teams;
            }
            return (teams = new CommandEnumData("Geyser_Teams",
                    session.getWorldCache().getScoreboard().getTeamNames(), true
            ));
        }
    }

    @Getter
    @ToString
    private static class ParamInfo {
        private final CommandNode<?> paramNode;
        private final CommandParamData paramData;
        private final List<ParamInfo> children;

        /**
         * Create a new parameter info object
         *
         * @param paramNode CommandNode the parameter is for
         * @param paramData The existing parameters for the command
         */
        public ParamInfo(CommandNode<?> paramNode, CommandParamData paramData) {
            this.paramNode = paramNode;
            this.paramData = paramData;
            this.children = new ArrayList<>();
        }

        /**
         * Build the array of all the child parameters (recursive)
         *
         * @param context the session's command builder context
         * @param path nodes already visited on this path
         */
        public void buildChildren(CommandBuilderContext context, Set<CommandNode<?>> path) {
            path.add(paramNode);
            for (CommandNode<?> paramNode : paramNode.getChildren()) {

                if (path.contains(paramNode)) {
                    // Fixes a StackOverflowError when an argument has itself as a child
                    continue;
                }

                if (!(paramNode instanceof ArgumentCommandNode<?, ?>)) {
                    boolean foundCompatible = false;
                    for (int i = 0; i < children.size(); i++) {
                        ParamInfo enumParamInfo = children.get(i);
                        // Check to make sure all descending nodes of this command are compatible - otherwise, create a new overload
                        if (isCompatible(enumParamInfo.getParamNode(), paramNode, new IdentityHashMap<>())) {
                            foundCompatible = true;
                            // TODO: Check this
                            // Extend the current list of enum values
                            // String[] enumOptions = Arrays.copyOf(enumParamInfo.getParamData().getEnumData().getValues(), enumParamInfo.getParamData().getEnumData().getValues().size() + 1);
                            // enumOptions[enumOptions.length - 1] = paramNode.getName();

                            Map<String, Set<CommandEnumConstraint>> values = new LinkedHashMap<>(enumParamInfo.getParamData().getEnumData().getValues());
                            values.put(paramNode.getName(), Set.of());

                            // Re-create the command using the updated values
                            CommandEnumData enumData = new CommandEnumData(enumParamInfo.getParamData().getEnumData().getName(), values, false);
                            CommandParamData commandParamData = new CommandParamData();
                            commandParamData.setName(enumParamInfo.getParamData().getName());
                            commandParamData.setOptional((this.paramNode.getCommand() != null));
                            commandParamData.setEnumData(enumData);

                            children.set(i, new ParamInfo(enumParamInfo.getParamNode(), commandParamData));
                            break;
                        }
                    }

                    if (!foundCompatible) {
                        // Create a new subcommand with this exact type
                        LinkedHashMap<String, Set<CommandEnumConstraint>> map = new LinkedHashMap<>();
                        map.put(paramNode.getName(), Set.of());
                        CommandEnumData enumData = new CommandEnumData(paramNode.getName(), map, false);

                        // On setting optional:
                        // isExecutable is defined as a node "constitutes a valid command."
                        // Therefore, any children of the parameter must simply be optional.
                        CommandParamData commandParamData = new CommandParamData();
                        commandParamData.setName(paramNode.getName());
                        commandParamData.setOptional((this.paramNode.getCommand() != null));
                        commandParamData.setEnumData(enumData);

                        children.add(new ParamInfo(paramNode, commandParamData));
                    }
                } else {
                    // Put the non-enum param into the list
                    Object mappedType = mapCommandType(context, paramNode);
                    CommandEnumData enumData = null;
                    CommandParam type = null;
                    boolean optional = (this.paramNode.getCommand() != null);
                    if (mappedType instanceof CommandEnumData) {
                        // Likely to specify isSoft, to be possibly updated later.
                        enumData = (CommandEnumData) mappedType;
                    } else if (mappedType instanceof String[]) {
                        LinkedHashMap<String, Set<CommandEnumConstraint>> map = new LinkedHashMap<>();
                        for (String s : (String[]) mappedType) {
                            map.put(s, Set.of());
                        }

                        enumData = new CommandEnumData(getEnumDataName(paramNode).toLowerCase(Locale.ROOT), map, false);
                    } else {
                        type = (CommandParam) mappedType;
                        // Bedrock throws a fit if an optional message comes after a string or target
                        // Example vanilla commands: ban-ip, ban, and kick
                        if (optional && type == CommandParam.MESSAGE && paramData != null && (paramData.getType() == CommandParam.STRING || paramData.getType() == CommandParam.TARGET)) {
                            optional = false;
                        }
                    }
                    // IF enumData != null:
                    // In game, this will show up like <paramNode.getName(): enumData.getName()>
                    // So if paramNode.getName() == "value" and enumData.getName() == "bool": <value: bool>
                    CommandParamData commandParamData = new CommandParamData();
                    commandParamData.setName(paramNode.getName());
                    commandParamData.setOptional(optional);
                    commandParamData.setEnumData(enumData);
                    commandParamData.setType(type);

                    children.add(new ParamInfo(paramNode, commandParamData));
                }
            }

            // Recursively build all child options
            for (ParamInfo child : children) {
                child.buildChildren(context, path);
            }
            path.remove(paramNode);
        }

        /**
         * Mitigates <a href="https://github.com/GeyserMC/Geyser/issues/3411">issue 3411</a>. Not a perfect solution.
         */
        private static String getEnumDataName(CommandNode<?> node) {
            var argument = VanillaCommands.describe(((ArgumentCommandNode<?, ?>) node).getType());
            return argument.registry().map(net.minecraft.resources.Identifier::getPath).orElse(argument.type().getPath());
        }

        /**
         * Comparing CommandNode type a and b, determine if they are in the same overload.
         * <p>
         * Take the <code>gamerule</code> command, and let's present three "subcommands" you can perform:
         *
         * <ul>
         *     <li><code>gamerule doDaylightCycle true</code></li>
         *     <li><code>gamerule announceAdvancements false</code></li>
         *     <li><code>gamerule randomTickSpeed 3</code></li>
         * </ul>
         *
         * While all three of them are indeed part of the same command, the command setting randomTickSpeed parses an int,
         * while the others use boolean. In Bedrock, this should be presented as a separate overload to indicate that this
         * does something a little different.
         * <p>
         * Therefore, this function will return <code>true</code> if the first two are compared, as they use the same
         * parsers. If the third is compared with either of the others, this function will return <code>false</code>.
         * <p>
         * Here's an example of how the above would be presented to Bedrock (as of 1.16.200). Notice how the top two <code>CommandParamData</code>
         * classes of each array are identical in type, but the following class is different:
         * <pre>
         *     overloads=[
         *         [
         *            CommandParamData(name=doDaylightCycle, optional=false, enumData=CommandEnumData(name=announceAdvancements, values=[announceAdvancements, doDaylightCycle], isSoft=false), type=STRING, postfix=null, options=[])
         *            CommandParamData(name=value, optional=false, enumData=CommandEnumData(name=value, values=[true, false], isSoft=false), type=null, postfix=null, options=[])
         *         ]
         *         [
         *            CommandParamData(name=randomTickSpeed, optional=false, enumData=CommandEnumData(name=randomTickSpeed, values=[randomTickSpeed], isSoft=false), type=STRING, postfix=null, options=[])
         *            CommandParamData(name=value, optional=false, enumData=null, type=INT, postfix=null, options=[])
         *         ]
         *     ]
         * </pre>
         *
         * @return if these two can be merged into one overload.
         */
        private boolean isCompatible(CommandNode<?> a, CommandNode<?> b, Map<CommandNode<?>, Set<CommandNode<?>>> visited) {
            if (a == b) return true;
            var aArgument = a instanceof ArgumentCommandNode<?, ?> argument ? VanillaCommands.describe(argument.getType()) : null;
            var bArgument = b instanceof ArgumentCommandNode<?, ?> argument ? VanillaCommands.describe(argument.getType()) : null;
            if (!Objects.equals(aArgument, bArgument)) return false;
            if (a.getChildren().size() != b.getChildren().size()) return false;
            if ((a.getCommand() != null) != (b.getCommand() != null)) return false;
            if (!visited.computeIfAbsent(a, ignored -> Collections.newSetFromMap(new IdentityHashMap<>())).add(b)) return true;

            try {
                for (CommandNode<?> aChild : a.getChildren()) {
                    boolean hasSimilarity = false;
                    for (CommandNode<?> bChild : b.getChildren()) {
                        if (isCompatible(aChild, bChild, visited)) {
                            hasSimilarity = true;
                            break;
                        }
                    }
                    if (!hasSimilarity) return false;
                }
                return true;
            } finally {
                visited.get(a).remove(b);
            }
        }

        /**
         * Get the tree of every parameter node (recursive)
         *
         * @return List of parameter options arrays for the command
         */
        public List<CommandOverloadData> getTree() {
            List<CommandOverloadData> treeParamData = new ArrayList<>();

            for (ParamInfo child : children) {
                // Get the tree from the child
                List<CommandOverloadData> childTree = child.getTree();

                // Un-pack the tree append the child node to it and push into the list
                for (CommandOverloadData subChildData : childTree) {
                    CommandParamData[] subChild = subChildData.getOverloads();
                    CommandParamData[] tmpTree = new CommandParamData[subChild.length + 1];
                    tmpTree[0] = child.getParamData();
                    System.arraycopy(subChild, 0, tmpTree, 1, subChild.length);

                    treeParamData.add(new CommandOverloadData(false, tmpTree));
                }

                // If we have no more child parameters just the child
                if (childTree.size() == 0) {
                    treeParamData.add(new CommandOverloadData(false, new CommandParamData[] { child.getParamData() }));
                }
            }

            return treeParamData;
        }
    }
}
