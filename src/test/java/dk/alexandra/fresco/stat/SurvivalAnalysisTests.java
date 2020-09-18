package dk.alexandra.fresco.stat;

import static org.junit.Assert.assertEquals;

import dk.alexandra.fresco.framework.Application;
import dk.alexandra.fresco.framework.DRes;
import dk.alexandra.fresco.framework.TestThreadRunner.TestThread;
import dk.alexandra.fresco.framework.TestThreadRunner.TestThreadFactory;
import dk.alexandra.fresco.framework.builder.numeric.ProtocolBuilderNumeric;
import dk.alexandra.fresco.framework.sce.resources.ResourcePool;
import dk.alexandra.fresco.framework.util.Pair;
import dk.alexandra.fresco.lib.real.SReal;
import dk.alexandra.fresco.stat.survival.SurvivalInfoDiscrete;
import dk.alexandra.fresco.stat.survival.cox.CoxGradientDiscrete;
import dk.alexandra.fresco.stat.survival.cox.CoxRegression;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class SurvivalAnalysisTests {

  public static List<SurvivalInfoDiscrete> survivalAnalysisDataset(ProtocolBuilderNumeric builder) {
    return survivalAnalysisDataset(builder, false);
  }

  public static List<SurvivalInfoDiscrete> survivalAnalysisDataset(ProtocolBuilderNumeric builder,
      boolean sorted) {

    // Dataset from https://www.statsdirect.com/help/survival_analysis/cox_regression.htm
    int[] group1 = new int[]{6, 19, 32, 42, 42, 43, 94, 126, 169, 207, 211, 227, 253, 255, 270,
        310, 316, 335, 346};
    int[] group1censor = new int[]{1, 1, 1, 1, 1, 0, 1, 0, 0, 1, 0, 0, 1, 0, 0, 0, 0, 0, 0};
    int[] group2 = new int[]{4, 6, 10, 11, 11, 11, 13, 17, 20, 20, 21, 22, 24, 24, 29, 30, 30,
        31, 33, 34, 35, 39, 40, 41, 43, 45, 46, 50, 56, 61, 61, 63, 68, 82, 85, 88, 89, 90,
        93, 104, 110, 134, 137, 160, 169, 171, 173, 175, 184, 201, 222, 235, 247, 260, 284,
        290, 291, 302, 304, 341, 345};
    int[] group2censor = new int[]{1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
        1, 1, 1, 1, 1, 1, 1, 0, 0, 1, 1, 1, 1, 0, 0, 1, 1, 1, 1, 1, 1, 1,
        1, 1, 1, 1, 1, 0, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0};

    int[][] combined = new int[group1.length + group2.length][];
    for (int i = 0; i < group1.length; i++) {
      combined[i] = new int[]{0, group1[i], group1censor[i]};
    }
    for (int i = 0; i < group2.length; i++) {
      combined[i + group1.length] = new int[]{1, group2[i], group2censor[i]};
    }

    if (sorted) {
      // Sort descending
      Arrays.sort(combined, Comparator.comparingInt(a -> -a[1]));
    }

    List<SurvivalInfoDiscrete> data = new ArrayList<>();
    for (int[] row : combined) {
      if (row[0] == 0) {
        data.add(new SurvivalInfoDiscrete(List.of(new Pair<>(
            List.of(builder.numeric().known(1), builder.numeric().known(0)),
            builder.numeric().known(row[0]))), builder.numeric().known(row[1]),
            builder.numeric().known(row[2])));
      } else {
        data.add(new SurvivalInfoDiscrete(List.of(new Pair<>(
            List.of(builder.numeric().known(0), builder.numeric().known(1)),
            builder.numeric().known(row[0]))), builder.numeric().known(row[1]),
            builder.numeric().known(row[2])));
      }
    }

    return data;
  }

  public static class TestCoxGradient<ResourcePoolT extends ResourcePool>
      extends TestThreadFactory<ResourcePoolT, ProtocolBuilderNumeric> {

    @Override
    public TestThread<ResourcePoolT, ProtocolBuilderNumeric> next() {
      return new TestThread<>() {

        @Override
        public void test() {

          Application<List<BigDecimal>, ProtocolBuilderNumeric> testApplication = builder -> builder
              .seq(seq -> {

                List<SurvivalInfoDiscrete> input = survivalAnalysisDataset(seq, true);
                DRes<List<DRes<SReal>>> beta = new CoxGradientDiscrete(input,
                    List.of(seq.realNumeric().known(1))).buildComputation(seq);
                return beta;
              }).seq((seq, beta) -> {
                List<DRes<BigDecimal>> openBeta =
                    beta.stream().map(seq.realNumeric()::open)
                        .collect(Collectors.toList());
                return () ->
                    openBeta.stream().map(DRes::out).collect(Collectors.toList());
              });

          List<BigDecimal> output = runApplication(testApplication);
          assertEquals(-0.25862161094245983, output.get(0).doubleValue(), 0.001);
        }
      };
    }
  }

  public static class TestCoxRegression<ResourcePoolT extends ResourcePool>
      extends TestThreadFactory<ResourcePoolT, ProtocolBuilderNumeric> {

    @Override
    public TestThread<ResourcePoolT, ProtocolBuilderNumeric> next() {
      return new TestThread<>() {

        @Override
        public void test() {
          Application<List<BigDecimal>, ProtocolBuilderNumeric> testApplication = builder -> builder
              .seq(seq -> {

                List<SurvivalInfoDiscrete> input = survivalAnalysisDataset(seq);
                DRes<List<DRes<SReal>>> beta = new CoxRegression(input, 5, 0.1,
                    new double[]{1}).buildComputation(seq);
                return beta;
              }).seq((seq, beta) -> {
                List<DRes<BigDecimal>> openBeta =
                    beta.stream().map(seq.realNumeric()::open)
                        .collect(Collectors.toList());
                return () ->
                    openBeta.stream().map(DRes::out).collect(Collectors.toList());
              });

          List<BigDecimal> output = runApplication(testApplication);
          assertEquals(0.9610201322467578, output.get(0).doubleValue(), 0.001);
        }
      };
    }
  }
}
