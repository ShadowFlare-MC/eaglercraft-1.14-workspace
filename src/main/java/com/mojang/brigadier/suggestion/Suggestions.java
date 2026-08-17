package com.mojang.brigadier.suggestion;

import com.mojang.brigadier.context.StringRange;
import net.deltaclient.CompletableFuture;

import java.util.*;

public class Suggestions {
    private static final Suggestions EMPTY = new Suggestions(StringRange.at(0), new ArrayList());
    private final StringRange range;
    private final List<Suggestion> suggestions;

    public Suggestions(StringRange range, List<Suggestion> suggestions) {
        this.range = range;
        this.suggestions = suggestions;
    }

    public static CompletableFuture<Suggestions> empty() {
        return CompletableFuture.completedFuture(EMPTY);
    }

    public static Suggestions merge(String command, Collection<Suggestions> input) {
        if (input.isEmpty()) {
            return EMPTY;
        } else if (input.size() == 1) {
            return (Suggestions) input.iterator().next();
        } else {
            Set<Suggestion> texts = new HashSet();
            Iterator var3 = input.iterator();

            while (var3.hasNext()) {
                Suggestions suggestions = (Suggestions) var3.next();
                texts.addAll(suggestions.getList());
            }

            return create(command, texts);
        }
    }

    public static Suggestions create(String command, Collection<Suggestion> suggestions) {
        if (suggestions.isEmpty()) {
            return EMPTY;
        } else {
            int start = Integer.MAX_VALUE;
            int end = Integer.MIN_VALUE;

            Suggestion suggestion;
            for (Iterator var4 = suggestions.iterator(); var4.hasNext(); end = Math.max(suggestion.getRange().getEnd(), end)) {
                suggestion = (Suggestion) var4.next();
                start = Math.min(suggestion.getRange().getStart(), start);
            }

            StringRange range = new StringRange(start, end);
            Set<Suggestion> texts = new HashSet();
            Iterator var6 = suggestions.iterator();

            while (var6.hasNext()) {
                suggestion = (Suggestion) var6.next();
                texts.add(suggestion.expand(command, range));
            }

            List<Suggestion> sorted = new ArrayList(texts);
            sorted.sort((a, b) -> {
                return a.compareToIgnoreCase(b);
            });
            return new Suggestions(range, sorted);
        }
    }

    public StringRange getRange() {
        return this.range;
    }

    public List<Suggestion> getList() {
        return this.suggestions;
    }

    public boolean isEmpty() {
        return this.suggestions.isEmpty();
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (!(o instanceof Suggestions)) {
            return false;
        } else {
            Suggestions that = (Suggestions) o;
            return Objects.equals(this.range, that.range) && Objects.equals(this.suggestions, that.suggestions);
        }
    }

    public int hashCode() {
        return Objects.hash(new Object[]{this.range, this.suggestions});
    }

    public String toString() {
        return "Suggestions{range=" + this.range + ", suggestions=" + this.suggestions + '}';
    }
}
