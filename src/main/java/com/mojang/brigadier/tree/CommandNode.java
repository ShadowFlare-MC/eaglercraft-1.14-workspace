package com.mojang.brigadier.tree;

import com.mojang.brigadier.AmbiguityConsumer;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.RedirectModifier;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.context.CommandContextBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.deltaclient.CompletableFuture;

import java.util.*;
import java.util.Map.Entry;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public abstract class CommandNode<S> implements Comparable<CommandNode<S>> {
    private final Predicate<S> requirement;
    private final CommandNode<S> redirect;
    private final RedirectModifier<S> modifier;
    private final boolean forks;
    private Map<String, CommandNode<S>> children = new LinkedHashMap();
    private Map<String, LiteralCommandNode<S>> literals = new LinkedHashMap();
    private Map<String, ArgumentCommandNode<S, ?>> arguments = new LinkedHashMap();
    private Command<S> command;

    protected CommandNode(Command<S> command, Predicate<S> requirement, CommandNode<S> redirect, RedirectModifier<S> modifier, boolean forks) {
        this.command = command;
        this.requirement = requirement;
        this.redirect = redirect;
        this.modifier = modifier;
        this.forks = forks;
    }

    public Command<S> getCommand() {
        return this.command;
    }

    public Collection<CommandNode<S>> getChildren() {
        return this.children.values();
    }

    public CommandNode<S> getChild(String name) {
        return (CommandNode) this.children.get(name);
    }

    public CommandNode<S> getRedirect() {
        return this.redirect;
    }

    public RedirectModifier<S> getRedirectModifier() {
        return this.modifier;
    }

    public boolean canUse(S source) {
        return this.requirement.test(source);
    }

    public void addChild(CommandNode<S> node) {
        if (node instanceof RootCommandNode) {
            throw new UnsupportedOperationException("Cannot add a RootCommandNode as a child to any other CommandNode");
        } else {
            CommandNode<S> child = (CommandNode) this.children.get(node.getName());
            if (child != null) {
                if (node.getCommand() != null) {
                    child.command = node.getCommand();
                }

                Iterator var3 = node.getChildren().iterator();

                while (var3.hasNext()) {
                    CommandNode<S> grandchild = (CommandNode) var3.next();
                    child.addChild(grandchild);
                }
            } else {
                this.children.put(node.getName(), node);
                if (node instanceof LiteralCommandNode) {
                    this.literals.put(node.getName(), (LiteralCommandNode) node);
                } else if (node instanceof ArgumentCommandNode) {
                    this.arguments.put(node.getName(), (ArgumentCommandNode) node);
                }
            }

            this.children = (Map) this.children.entrySet().stream().sorted(Entry.comparingByValue()).collect(Collectors.toMap(Entry::getKey, Entry::getValue, (e1, e2) -> {
                return e1;
            }, LinkedHashMap::new));
        }
    }

    public void findAmbiguities(AmbiguityConsumer<S> consumer) {
        Set<String> matches = new HashSet();

        CommandNode child;
        label40:
        for (Iterator var3 = this.children.values().iterator(); var3.hasNext(); child.findAmbiguities(consumer)) {
            child = (CommandNode) var3.next();
            Iterator var5 = this.children.values().iterator();

            while (true) {
                CommandNode sibling;
                do {
                    if (!var5.hasNext()) {
                        continue label40;
                    }

                    sibling = (CommandNode) var5.next();
                } while (child == sibling);

                Iterator var7 = child.getExamples().iterator();

                while (var7.hasNext()) {
                    String input = (String) var7.next();
                    if (sibling.isValidInput(input)) {
                        matches.add(input);
                    }
                }

                if (matches.size() > 0) {
                    consumer.ambiguous(this, child, sibling, matches);
                    matches = new HashSet();
                }
            }
        }

    }

    protected abstract boolean isValidInput(String var1);

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (!(o instanceof CommandNode)) {
            return false;
        } else {
            CommandNode<S> that = (CommandNode) o;
            if (!this.children.equals(that.children)) {
                return false;
            } else {
                if (this.command != null) {
                    if (!this.command.equals(that.command)) {
                        return false;
                    }
                } else if (that.command != null) {
                    return false;
                }

                return true;
            }
        }
    }

    public int hashCode() {
        return 31 * this.children.hashCode() + (this.command != null ? this.command.hashCode() : 0);
    }

    public Predicate<S> getRequirement() {
        return this.requirement;
    }

    public abstract String getName();

    public abstract String getUsageText();

    public abstract void parse(StringReader var1, CommandContextBuilder<S> var2) throws CommandSyntaxException;

    public abstract CompletableFuture<Suggestions> listSuggestions(CommandContext<S> var1, SuggestionsBuilder var2) throws CommandSyntaxException;

    public abstract ArgumentBuilder<S, ?> createBuilder();

    protected abstract String getSortedKey();

    public Collection<? extends CommandNode<S>> getRelevantNodes(StringReader input) {
        if (this.literals.size() <= 0) {
            return this.arguments.values();
        } else {
            int cursor = input.getCursor();

            while (input.canRead() && input.peek() != ' ') {
                input.skip();
            }

            String text = input.getString().substring(cursor, input.getCursor());
            input.setCursor(cursor);
            LiteralCommandNode<S> literal = (LiteralCommandNode) this.literals.get(text);
            return (Collection) (literal != null ? Collections.singleton(literal) : this.arguments.values());
        }
    }

    public int compareTo(CommandNode<S> o) {
        if (this instanceof LiteralCommandNode == (o instanceof LiteralCommandNode)) {
            return this.getSortedKey().compareTo(o.getSortedKey());
        } else {
            return o instanceof LiteralCommandNode ? 1 : -1;
        }
    }

    public boolean isFork() {
        return this.forks;
    }

    public abstract Collection<String> getExamples();
}
