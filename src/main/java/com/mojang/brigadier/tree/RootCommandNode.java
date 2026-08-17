package com.mojang.brigadier.tree;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.context.CommandContextBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.deltaclient.CompletableFuture;

import java.util.Collection;
import java.util.Collections;

public class RootCommandNode<S> extends CommandNode<S> {
    public RootCommandNode() {
        super((Command) null, (c) -> {
            return true;
        }, (CommandNode) null, (s) -> {
            return Collections.singleton(s.getSource());
        }, false);
    }

    public String getName() {
        return "";
    }

    public String getUsageText() {
        return "";
    }

    public void parse(StringReader reader, CommandContextBuilder<S> contextBuilder) throws CommandSyntaxException {
    }

    public CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        return Suggestions.empty();
    }

    public boolean isValidInput(String input) {
        return false;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else {
            return !(o instanceof RootCommandNode) ? false : super.equals(o);
        }
    }

    public ArgumentBuilder<S, ?> createBuilder() {
        throw new IllegalStateException("Cannot convert root into a builder");
    }

    protected String getSortedKey() {
        return "";
    }

    public Collection<String> getExamples() {
        return Collections.emptyList();
    }

    public String toString() {
        return "<root>";
    }
}
