package de.hype.bingonet.fabric.command.argumentTypes;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import de.hype.bingonet.client.common.client.BingoNet;
import de.hype.bingonet.client.common.client.NeuRepoManager;
import io.github.moulberry.repo.constants.Islands;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class SkyblockWarpArgumentType implements ArgumentType<String> {
    private static final List<String> warps = BingoNet.neuRepoManager.getWarps().stream().map(Islands.Warp::getWarp).toList();

    private SkyblockWarpArgumentType() {
    }

    public static SkyblockWarpArgumentType warptype() {
        return new SkyblockWarpArgumentType();
    }

    public static String getWarpString(final CommandContext<?> context, final String name) {
        return context.getArgument(name, String.class);
    }

    @Override
    public String parse(final StringReader reader) throws CommandSyntaxException {
        return reader.readString();
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(final CommandContext<S> context, final SuggestionsBuilder builder) {
        String current = builder.getRemainingLowerCase();
        if (!current.isEmpty()) {
            new ArrayList<>(warps).forEach(v -> {
                if (v.toLowerCase().contains(current)) builder.suggest(v);
            });
        }else {
            for (String materialId : warps) {
                builder.suggest(materialId);
            }
        }
        try {
            return builder.buildFuture();
        } catch (Exception e) {
            return Suggestions.empty();
        }
    }

    @Override
    public Collection<String> getExamples() {
        return warps;
    }
}
