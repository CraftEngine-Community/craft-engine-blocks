package cn.gtemc.craftengine.plugin.context.function;

import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.item.ItemBuildContext;
import net.momirealms.craftengine.core.item.processor.ItemProcessor;
import net.momirealms.craftengine.core.item.processor.ItemProcessors;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.context.Condition;
import net.momirealms.craftengine.core.plugin.context.Context;
import net.momirealms.craftengine.core.plugin.context.function.AbstractConditionalFunction;
import net.momirealms.craftengine.core.plugin.context.function.FunctionFactory;
import net.momirealms.craftengine.core.plugin.context.parameter.DirectContextParameters;

import java.util.ArrayList;
import java.util.List;

public final class ApplyDataFunction<CTX extends Context> extends AbstractConditionalFunction<CTX> {
    private final ItemProcessor[] processors;

    public ApplyDataFunction(List<Condition<CTX>> predicates, ItemProcessor[] processors) {
        super(predicates);
        this.processors = processors;
    }

    @Override
    protected void runInternal(CTX ctx) {
        Player player = ctx.getOptionalParameter(DirectContextParameters.PLAYER).orElse(null);
        ctx.getOptionalParameter(DirectContextParameters.ITEM_IN_HAND).ifPresent(item -> {
            for (ItemProcessor processor : this.processors) {
                processor.apply(item, ItemBuildContext.of(player));
            }
        });
    }

    public static <CTX extends Context> FunctionFactory<CTX, ApplyDataFunction<CTX>> factory(java.util.function.Function<ConfigSection, Condition<CTX>> factory) {
        return new Factory<>(factory);
    }

    public static class Factory<CTX extends Context> extends AbstractFactory<CTX, ApplyDataFunction<CTX>> {

        public Factory(java.util.function.Function<ConfigSection, Condition<CTX>> factory) {
            super(factory);
        }

        @Override
        public ApplyDataFunction<CTX> create(ConfigSection section) {
            List<ItemProcessor> processors = new ArrayList<>();
            ItemProcessors.collectProcessors(section.getNonNullSection("data"), processors::add);
            return new ApplyDataFunction<>(getPredicates(section), processors.toArray(new ItemProcessor[0]));
        }
    }
}
