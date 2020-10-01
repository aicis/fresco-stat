package dk.alexandra.fresco.stat.sampling;

import dk.alexandra.fresco.framework.DRes;
import dk.alexandra.fresco.framework.builder.Computation;
import dk.alexandra.fresco.framework.builder.numeric.ProtocolBuilderNumeric;
import dk.alexandra.fresco.framework.value.SInt;
import dk.alexandra.fresco.lib.common.math.AdvancedNumeric;
import dk.alexandra.fresco.lib.fixed.AdvancedFixedNumeric;
import dk.alexandra.fresco.lib.fixed.FixedNumeric;
import dk.alexandra.fresco.lib.fixed.SFixed;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Sample an element from a catagorical distribution.
 *
 * @author Jonas Lindstrøm (jonas.lindstrom@alexandra.dk)
 */
public class SampleCatagoricalDistribution implements Computation<SInt, ProtocolBuilderNumeric> {

  private List<DRes<SFixed>> probabilities;
  private boolean normalized;
  private double[] knownProbabilities;

  /**
   * @param propabilities The i'th element of this list is the propabily of drawing i from this
   *                      distribution.
   * @param normalized    Does the propabilities sum to 1?
   */
  public SampleCatagoricalDistribution(List<DRes<SFixed>> propabilities, boolean normalized) {
    this.probabilities = propabilities;
    this.normalized = normalized;
  }

  public SampleCatagoricalDistribution(double[] propabilities) {
    double sum = Arrays.stream(propabilities).sum();
    this.knownProbabilities = Arrays.stream(propabilities).map(p -> p / sum).toArray();
    this.normalized = true;
  }

  /**
   * @param probabilities The i'th element of this list is the propabily of drawing i from this
   *                      distribution. The probabilities should have been normalized such that they
   *                      sum to 1.
   */
  public SampleCatagoricalDistribution(List<DRes<SFixed>> probabilities) {
    this(probabilities, true);
  }

  @Override
  public DRes<SInt> buildComputation(ProtocolBuilderNumeric root) {
    return root.seq(builder -> {

      /*
       * Let p_0,...,p_{n-1} be the probabilities of drawing 0, ..., n-1 resp.
       *
       * Now sample r uniformly in [0,1). Let c_i = p_0 + ... + p_i and let t_i = 0 if c_i <= r and
       * 1 otherwise.
       *
       * We return Sum_{j=0}^n t_j which will be i with probability p_i
       */

      DRes<SFixed> r = new SampleUniformDistribution().buildComputation(builder);

      FixedNumeric numeric = FixedNumeric.using(builder);
      if (!normalized) {
        DRes<SFixed> sum = AdvancedFixedNumeric.using(builder).sum(probabilities);
        r = numeric.mult(sum, r);
      }

      if (knownProbabilities != null) {

        double c = knownProbabilities[0];
        List<DRes<SInt>> terms = new ArrayList<>();
        for (int i = 0; i < knownProbabilities.length; i++) {
          if (i > 0) {
            c += knownProbabilities[i];
          }
          terms.add(
              numeric
                  .leq(numeric.known(BigDecimal.valueOf(c)), r));
        }
        return AdvancedNumeric.using(builder).sum(terms);

      } else {

        DRes<SFixed> c = probabilities.get(0);
        List<DRes<SInt>> terms = new ArrayList<>();
        for (int i = 0; i < probabilities.size(); i++) {
          if (i > 0) {
            c = numeric.add(c, probabilities.get(i));
          }
          terms.add(numeric.leq(c, r));
        }
        return AdvancedNumeric.using(builder).sum(terms);

      }
    });
  }

}
