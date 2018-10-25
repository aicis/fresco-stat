package dk.alexandra.fresco.stat.descriptive;

import dk.alexandra.fresco.framework.DRes;
import dk.alexandra.fresco.framework.builder.Computation;
import dk.alexandra.fresco.framework.builder.numeric.ProtocolBuilderNumeric;
import dk.alexandra.fresco.lib.real.SReal;
import java.math.BigDecimal;
import java.util.List;

public class Mean implements Computation<SReal, ProtocolBuilderNumeric> {

  private List<DRes<SReal>> observed;

  public Mean(List<DRes<SReal>> observed) {
    this.observed = observed;
  }
  
  @Override
  public DRes<SReal> buildComputation(ProtocolBuilderNumeric builder) {
    return builder.seq(seq -> {
      DRes<SReal> sum = seq.realAdvanced().sum(observed);
      return seq.realNumeric().div(sum, BigDecimal.valueOf(observed.size()));
    });
  }

}
