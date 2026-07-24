package cn.gtemc.craftengine.plugin.context.function;

import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.config.ConfigValue;
import net.momirealms.craftengine.core.plugin.context.CommonFunctions;
import net.momirealms.craftengine.core.plugin.context.Condition;
import net.momirealms.craftengine.core.plugin.context.Context;
import net.momirealms.craftengine.core.plugin.context.function.AbstractConditionalFunction;
import net.momirealms.craftengine.core.plugin.context.function.Function;
import net.momirealms.craftengine.core.plugin.context.function.FunctionFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public final class WeightedFunction<CTX extends Context> extends AbstractConditionalFunction<CTX> {
    private final Function<CTX>[] functions;
    private final double[] cumulativeWeights;
    private final double totalWeight;
    private final boolean allWeightsEqual;

    private WeightedFunction(List<Condition<CTX>> predicates, Function<CTX>[] functions, double[] weights) {
        super(predicates);
        this.functions = functions;
        int functionCount = functions.length;
        boolean weightsAreEqual = true;
        double firstWeight = functionCount > 0 ? weights[0] : 0;
        double weightSum = 0;
        for (double weight : weights) {
            weightSum += weight;
            if (weight != firstWeight) weightsAreEqual = false;
        }
        this.totalWeight = weightSum;
        this.allWeightsEqual = weightsAreEqual;
        if (this.allWeightsEqual) {
            this.cumulativeWeights = null;
        } else {
            double[] cumulativeWeightArray = new double[functionCount];
            double accumulatedWeight = 0;
            for (int i = 0; i < functionCount; i++) {
                accumulatedWeight += weights[i];
                cumulativeWeightArray[i] = accumulatedWeight;
            }
            this.cumulativeWeights = cumulativeWeightArray;
        }
    }

    @Override
    protected void runInternal(CTX ctx) {
        int functionCount = this.functions.length;
        if (functionCount == 0) return;
        if (functionCount == 1) {
            this.functions[0].run(ctx);
            return;
        }
        int selectedIndex;
        if (this.allWeightsEqual) {
            selectedIndex = ThreadLocalRandom.current().nextInt(functionCount);
        } else {
            assert this.cumulativeWeights != null; // 理论上不应该
            selectedIndex = lowerBound(this.cumulativeWeights, ThreadLocalRandom.current().nextDouble(this.totalWeight));
        }
        this.functions[selectedIndex].run(ctx);
    }

    private static int lowerBound(double[] cumulativeWeights, double randomValue) {
        int lowIndex = 0, highIndex = cumulativeWeights.length - 1;
        while (lowIndex < highIndex) {
            int middleIndex = (lowIndex + highIndex) >>> 1;
            if (cumulativeWeights[middleIndex] > randomValue) {
                highIndex = middleIndex;
            } else {
                lowIndex = middleIndex + 1;
            }
        }
        return lowIndex;
    }

    public static <CTX extends Context> FunctionFactory<CTX, WeightedFunction<CTX>> factory(java.util.function.Function<ConfigSection, Condition<CTX>> factory) {
        return new Factory<>(factory);
    }

    public static class Factory<CTX extends Context> extends AbstractFactory<CTX, WeightedFunction<CTX>> {

        public Factory(java.util.function.Function<ConfigSection, Condition<CTX>> factory) {
            super(factory);
        }

        @SuppressWarnings("unchecked")
        @Override
        public WeightedFunction<CTX> create(ConfigSection section) {
            List<ConfigSection> sectionList = section.getList("functions", ConfigValue::getAsSection);
            List<Function<CTX>> functionList = new ArrayList<>();
            List<Double> weightList = new ArrayList<>();
            for (ConfigSection configSection : sectionList) {
                double weight = configSection.getDouble("weight", 1.0);
                Function<CTX> function = (Function<CTX>) CommonFunctions.fromConfig(configSection);
                functionList.add(function);
                weightList.add(weight);
            }
            Function<CTX>[] functions = functionList.toArray(new Function[0]);
            double[] weights = weightList.stream().mapToDouble(Double::doubleValue).toArray();
            return new WeightedFunction<>(getPredicates(section), functions, weights);
        }
    }
}
