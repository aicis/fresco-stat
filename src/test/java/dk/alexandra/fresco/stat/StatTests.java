package dk.alexandra.fresco.stat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.math3.stat.correlation.PearsonsCorrelation;
import org.apache.commons.math3.stat.inference.ChiSquareTest;
import org.apache.commons.math3.stat.inference.TTest;
import org.apache.commons.math3.stat.regression.RegressionResults;
import org.apache.commons.math3.stat.regression.SimpleRegression;

import dk.alexandra.fresco.framework.Application;
import dk.alexandra.fresco.framework.DRes;
import dk.alexandra.fresco.framework.TestThreadRunner.TestThread;
import dk.alexandra.fresco.framework.TestThreadRunner.TestThreadFactory;
import dk.alexandra.fresco.framework.builder.numeric.ProtocolBuilderNumeric;
import dk.alexandra.fresco.framework.sce.resources.ResourcePool;
import dk.alexandra.fresco.framework.util.Pair;
import dk.alexandra.fresco.framework.value.SInt;
import dk.alexandra.fresco.lib.collections.Matrix;
import dk.alexandra.fresco.lib.real.SReal;
import dk.alexandra.fresco.stat.tests.LinearRegression.LinearFunction;
import dk.alexandra.fresco.stat.utils.MatrixUtils;

public class StatTests {

  public static class TestTTest<ResourcePoolT extends ResourcePool>
      extends TestThreadFactory<ResourcePoolT, ProtocolBuilderNumeric> {

    @Override
    public TestThread<ResourcePoolT, ProtocolBuilderNumeric> next() {
      return new TestThread<>() {

        List<BigDecimal> data = List.of(65, 78, 88, 55, 48, 95, 66, 57, 79, 81).stream()
            .map(BigDecimal::valueOf).collect(Collectors.toList());
        double expectedMean = 75.0;

        @Override
        public void test() throws Exception {

          Application<BigDecimal, ProtocolBuilderNumeric> testApplication = builder -> {
            List<DRes<SReal>> input =
                data.stream().map(x -> builder.realNumeric().known(x)).collect(Collectors.toList());
            DRes<SReal> t = Statistics.using(builder).ttest(input, builder.realNumeric().known(BigDecimal.valueOf(expectedMean)));
            return builder.realNumeric().open(t);
          };

          BigDecimal t = runApplication(testApplication);
          double clearT = new TTest().t(expectedMean,
              data.stream().mapToDouble(BigDecimal::doubleValue).toArray());

          assertEquals(t.doubleValue(), clearT, 0.01);
        }
      };
    }
  }

  public static class TestTwoSampleTTest<ResourcePoolT extends ResourcePool>
      extends TestThreadFactory<ResourcePoolT, ProtocolBuilderNumeric> {

    @Override
    public TestThread<ResourcePoolT, ProtocolBuilderNumeric> next() {
      return new TestThread<>() {

        List<BigDecimal> data1 = List.of(42.1, 41.3, 42.4, 43.2, 41.8, 41.0, 41.8, 42.8, 42.3, 42.7)
            .stream().map(BigDecimal::valueOf).collect(Collectors.toList());
        List<BigDecimal> data2 = List.of(42.7, 43.8, 42.5, 43.1, 44.0, 43.6, 43.3, 43.5, 41.7, 44.1)
            .stream().map(BigDecimal::valueOf).collect(Collectors.toList());

        @Override
        public void test() throws Exception {

          Application<BigDecimal, ProtocolBuilderNumeric> testApplication = builder -> {
            List<DRes<SReal>> input1 = data1.stream().map(x -> builder.realNumeric().input(x, 1))
                .collect(Collectors.toList());
            List<DRes<SReal>> input2 = data2.stream().map(x -> builder.realNumeric().input(x, 2))
                .collect(Collectors.toList());
            DRes<SReal> t = Statistics.using(builder).ttest(input1, input2);
            return builder.realNumeric().open(t);
          };

          BigDecimal output = runApplication(testApplication);

          double clearT =
              new TTest().t(data1.stream().mapToDouble(BigDecimal::doubleValue).toArray(),
                  data2.stream().mapToDouble(BigDecimal::doubleValue).toArray());

          assertTrue(Math.abs(clearT - output.doubleValue()) < 0.01);

        }
      };
    }
  }

  public static class TestChiSquareTest<ResourcePoolT extends ResourcePool>
      extends TestThreadFactory<ResourcePoolT, ProtocolBuilderNumeric> {

    @Override
    public TestThread<ResourcePoolT, ProtocolBuilderNumeric> next() {
      return new TestThread<>() {

        List<Double> expected = List.of(58.0, 34.5, 7.0, 0.5);
        List<Integer> observed = List.of(56, 36, 8, 0);

        @Override
        public void test() throws Exception {

          Application<BigDecimal, ProtocolBuilderNumeric> testApplication = builder -> {
            List<DRes<SReal>> e = expected.stream().map(x -> builder.realNumeric().input(x, 1))
                .collect(Collectors.toList());
            List<DRes<SInt>> o = observed.stream().map(x -> builder.numeric().input(x, 2))
                .collect(Collectors.toList());
            DRes<SReal> x = Statistics.using(builder).chiSquare(o, e);
            return builder.realNumeric().open(x);
          };

          BigDecimal output = runApplication(testApplication);

          double[] e = expected.stream().mapToDouble(i -> i).toArray();
          long[] o = observed.stream().mapToLong(i -> i).toArray();
          double clearQ = new ChiSquareTest().chiSquare(e, o);

          assertEquals(output.doubleValue(), clearQ, 0.01);

        }
      };
    }
  }

  public static class TestChiSquareTestWithKnownBuckets<ResourcePoolT extends ResourcePool>
      extends TestThreadFactory<ResourcePoolT, ProtocolBuilderNumeric> {

    @Override
    public TestThread<ResourcePoolT, ProtocolBuilderNumeric> next() {
      return new TestThread<>() {

        double[] data = new double[] {.1, .5, .7, .3, .9, .5, 3.4, .5, -.1, -.3};
        double[] buckets = new double[] {.0, .5, 1.0};
        double[] expected = new double[] {2.1, 4.9, 2.1, 0.9};

        @Override
        public void test() {

          Application<BigDecimal, ProtocolBuilderNumeric> testApplication = builder -> {
            List<DRes<SReal>> secretData = Arrays.stream(data).mapToObj(builder.realNumeric()::known)
                .collect(Collectors.toList());
            DRes<SReal> x = Statistics.using(builder).chiSquare(secretData, buckets, expected);
            return builder.realNumeric().open(x);
          };

          BigDecimal output = runApplication(testApplication);

          long[] o = new long[] {2, 5, 2, 1};
          double clearQ = new ChiSquareTest().chiSquare(expected, o);

          assertEquals(output.doubleValue(), clearQ, 0.01);
        }
      };
    }
  }

  public static class TestChiSquareTestKnown<ResourcePoolT extends ResourcePool>
      extends TestThreadFactory<ResourcePoolT, ProtocolBuilderNumeric> {

    @Override
    public TestThread<ResourcePoolT, ProtocolBuilderNumeric> next() {
      return new TestThread<>() {

        double[] expected = new double[] {58.0, 34.5, 7.0, 0.5};
        List<Integer> observed = List.of(56, 36, 8, 0);

        @Override
        public void test() throws Exception {

          Application<BigDecimal, ProtocolBuilderNumeric> testApplication = builder -> {
            List<DRes<SInt>> o = observed.stream().map(x -> builder.numeric().input(x, 2))
                .collect(Collectors.toList());
            DRes<SReal> x = Statistics.using(builder).chiSquare(o, expected);
            return builder.realNumeric().open(x);
          };

          BigDecimal output = runApplication(testApplication);

          long[] o = observed.stream().mapToLong(i -> i).toArray();
          double clearQ = new ChiSquareTest().chiSquare(expected, o);

          assertEquals(output.doubleValue(), clearQ, 0.01);

        }
      };
    }
  }

  public static class TestLinearRegression<ResourcePoolT extends ResourcePool>
      extends TestThreadFactory<ResourcePoolT, ProtocolBuilderNumeric> {

    @Override
    public TestThread<ResourcePoolT, ProtocolBuilderNumeric> next() {
      return new TestThread<>() {

        List<Double> x = Arrays.asList(1.0, 2.0, 3.0, 4.0, 5.0);
        List<Double> y = Arrays.asList(1.0, 2.0, 1.3, 3.75, 2.25);

        @Override
        public void test() throws Exception {

          Application<Pair<BigDecimal, BigDecimal>, ProtocolBuilderNumeric> testApplication =
              builder -> {
                List<DRes<SReal>> xSecret = x.stream().map(x -> builder.realNumeric().input(x, 1))
                    .collect(Collectors.toList());
                List<DRes<SReal>> ySecret = y.stream().map(y -> builder.realNumeric().input(y, 2))
                    .collect(Collectors.toList());
                DRes<LinearFunction> f = Statistics.using(builder).linearRegression(xSecret, ySecret);
                return builder.par(par -> {
                  Pair<DRes<BigDecimal>, DRes<BigDecimal>> result =
                      new Pair<>(par.realNumeric().open(f.out().getA()),
                          par.realNumeric().open(f.out().getB()));
                  return () -> new Pair<>(result.getFirst().out(), result.getSecond().out());
                });
              };

          SimpleRegression simpleRegression = new SimpleRegression();
          for (int i = 0; i < x.size(); i++) {
            simpleRegression.addData(x.get(i), y.get(i));
          }
          RegressionResults result = simpleRegression.regress();

          Pair<BigDecimal, BigDecimal> output = runApplication(testApplication);
          assertTrue(output.getFirst().subtract(BigDecimal.valueOf(result.getParameterEstimate(0)))
              .abs().compareTo(BigDecimal.valueOf(0.001)) < 0);
          assertTrue(output.getSecond().subtract(BigDecimal.valueOf(result.getParameterEstimate(1)))
              .abs().compareTo(BigDecimal.valueOf(0.001)) < 0);
        }
      };
    }
  }

  public static class TestCorrelation<ResourcePoolT extends ResourcePool>
      extends TestThreadFactory<ResourcePoolT, ProtocolBuilderNumeric> {

    @Override
    public TestThread<ResourcePoolT, ProtocolBuilderNumeric> next() {
      return new TestThread<>() {

        List<Double> x = Arrays.asList(1.0, 2.0, 3.0, 4.0, 5.0);
        List<Double> y = Arrays.asList(1.0, 2.0, 1.3, 3.75, 2.25);

        @Override
        public void test() throws Exception {

          Application<BigDecimal, ProtocolBuilderNumeric> testApplication = builder -> {
            List<DRes<SReal>> xSecret =
                x.stream().map(x -> builder.realNumeric().input(x, 1)).collect(Collectors.toList());
            List<DRes<SReal>> ySecret =
                y.stream().map(y -> builder.realNumeric().input(y, 2)).collect(Collectors.toList());
            DRes<SReal> r = Statistics.using(builder).correlation(xSecret, ySecret);
            return builder.realNumeric().open(r);
          };

          double[] xArray = x.stream().mapToDouble(i -> i).toArray();
          double[] yArray = y.stream().mapToDouble(i -> i).toArray();

          PearsonsCorrelation correlation = new PearsonsCorrelation();
          double expected = correlation.correlation(xArray, yArray);

          BigDecimal output = runApplication(testApplication);
          System.out.println(output + " ~ " + expected);
          assertTrue(Math.abs(expected - output.doubleValue()) < 0.01);
        }
      };
    }
  }


  public static class TestHistogramInt<ResourcePoolT extends ResourcePool>
      extends TestThreadFactory<ResourcePoolT, ProtocolBuilderNumeric> {

    @Override
    public TestThread<ResourcePoolT, ProtocolBuilderNumeric> next() {
      return new TestThread<>() {

        List<Integer> x = Arrays.asList(1, 5, 7, 3, 9, 5, 34, 5, -1, -3);
        List<Integer> buckets = Arrays.asList(0, 5, 10);
        List<Integer> expected = Arrays.asList(2, 5, 2, 1);

        @Override
        public void test() throws Exception {

          Application<List<BigInteger>, ProtocolBuilderNumeric> testApplication = builder -> {
            return builder.seq(seq -> {
              List<DRes<SInt>> xSecret =
                  x.stream().map(x -> seq.numeric().input(x, 1)).collect(Collectors.toList());
              List<DRes<SInt>> bSecret =
                  buckets.stream().map(b -> seq.numeric().input(b, 2)).collect(Collectors.toList());
              DRes<List<DRes<SInt>>> h = Statistics.using(seq).histogramInt(bSecret, xSecret);
              return h;
            }).seq((seq, h) -> {
              List<DRes<BigInteger>> out =
                  h.stream().map(seq.numeric()::open).collect(Collectors.toList());
              return () -> out.stream().map(DRes::out).collect(Collectors.toList());
            });
          };

          List<BigInteger> output = runApplication(testApplication);
          for (int i = 0; i < output.size(); i++) {
            assertEquals(expected.get(i).intValue(), output.get(i).intValue());
          }
        }
      };
    }
  }

  public static class TestHistogramFixed<ResourcePoolT extends ResourcePool>
      extends TestThreadFactory<ResourcePoolT, ProtocolBuilderNumeric> {

    @Override
    public TestThread<ResourcePoolT, ProtocolBuilderNumeric> next() {
      return new TestThread<>() {

        List<Double> x = Arrays.asList(.1, .5, .7, .3, .9, .5, 3.4, .5, -.1, -.3);
        List<Double> buckets = Arrays.asList(.0, .5, 1.0);
        List<Integer> expected = Arrays.asList(2, 5, 2, 1);

        @Override
        public void test() throws Exception {

          Application<List<BigInteger>, ProtocolBuilderNumeric> testApplication = builder -> {
            return builder.seq(seq -> {
              List<DRes<SReal>> xSecret =
                  x.stream().map(x -> seq.realNumeric().input(x, 1)).collect(Collectors.toList());
              List<DRes<SReal>> bSecret = buckets.stream().map(b -> seq.realNumeric().input(b, 2))
                  .collect(Collectors.toList());
              DRes<List<DRes<SInt>>> h = Statistics.using(seq).histogramReal(bSecret, xSecret);
              return h;
            }).seq((seq, h) -> {
              List<DRes<BigInteger>> out =
                  h.stream().map(seq.numeric()::open).collect(Collectors.toList());
              return () -> out.stream().map(DRes::out).collect(Collectors.toList());
            });
          };

          List<BigInteger> output = runApplication(testApplication);
          for (int i = 0; i < output.size(); i++) {
            assertEquals(expected.get(i).intValue(), output.get(i).intValue());
          }
        }
      };
    }
  }

  public static class TestTwoDimHistogram<ResourcePoolT extends ResourcePool>
      extends TestThreadFactory<ResourcePoolT, ProtocolBuilderNumeric> {

    @Override
    public TestThread<ResourcePoolT, ProtocolBuilderNumeric> next() {
      return new TestThread<>() {

        List<Integer> x = Arrays.asList(1, 3, 5, 6, 7, 8);
        List<Integer> y = Arrays.asList(2, 4, 5, 8, 9, 10);
        List<Integer> bucketsX = Arrays.asList(1, 4, 9);
        List<Integer> bucketsY = Arrays.asList(1,4,9);

        @Override
        public void test() throws Exception {

          Application<Matrix<BigInteger>, ProtocolBuilderNumeric> testApplication = builder -> {
            return builder.seq(seq -> {
              Pair<List<DRes<SInt>>, List<DRes<SInt>>> buckets = new Pair<>(
                  bucketsX.stream().map(x -> seq.numeric().input(x, 1)).collect(Collectors.toList()),
                  bucketsY.stream().map(x -> seq.numeric().input(x, 1)).collect(Collectors.toList())
                  );              
              List<Pair<DRes<SInt>, DRes<SInt>>> data = IntStream.range(0, x.size()).mapToObj(i -> new Pair<>(seq.numeric().input(x.get(i), 1), seq.numeric().input(y.get(i), 1))).collect(Collectors.toList());
                  x.stream().map(x -> seq.realNumeric().input(x, 1)).collect(Collectors.toList());
                 
                  DRes<Matrix<DRes<SInt>>> histogram = Statistics.using(seq).twoDimensionalHistogramInt(buckets, data);                  
              return histogram;
            }).seq((seq, histogram) -> {
              Matrix<DRes<BigInteger>> opened = MatrixUtils.map(histogram, seq.numeric()::open);
              return () -> MatrixUtils.map(opened, DRes::out);
            });
          };

          Matrix<BigInteger> output = runApplication(testApplication);
          assertEquals(BigInteger.valueOf(0), output.getRow(0).get(0));
          assertEquals(BigInteger.valueOf(1), output.getRow(1).get(1));
          assertEquals(BigInteger.valueOf(3), output.getRow(2).get(2));
        }
      };
    }
  }
}
