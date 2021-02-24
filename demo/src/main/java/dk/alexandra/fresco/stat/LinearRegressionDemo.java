package dk.alexandra.fresco.stat;

import dk.alexandra.fresco.framework.Application;
import dk.alexandra.fresco.framework.DRes;
import dk.alexandra.fresco.framework.Party;
import dk.alexandra.fresco.framework.builder.numeric.ProtocolBuilderNumeric;
import dk.alexandra.fresco.framework.builder.numeric.field.MersennePrimeFieldDefinition;
import dk.alexandra.fresco.framework.configuration.NetworkConfiguration;
import dk.alexandra.fresco.framework.configuration.NetworkConfigurationImpl;
import dk.alexandra.fresco.framework.network.Network;
import dk.alexandra.fresco.framework.network.socket.SocketNetwork;
import dk.alexandra.fresco.framework.sce.SecureComputationEngine;
import dk.alexandra.fresco.framework.sce.SecureComputationEngineImpl;
import dk.alexandra.fresco.framework.sce.evaluator.BatchedProtocolEvaluator;
import dk.alexandra.fresco.framework.sce.evaluator.EvaluationStrategy;
import dk.alexandra.fresco.framework.util.AesCtrDrbg;
import dk.alexandra.fresco.framework.util.Pair;
import dk.alexandra.fresco.lib.fixed.FixedNumeric;
import dk.alexandra.fresco.lib.fixed.SFixed;
import dk.alexandra.fresco.stat.Statistics;
import dk.alexandra.fresco.suite.spdz.SpdzProtocolSuite;
import dk.alexandra.fresco.suite.spdz.SpdzResourcePool;
import dk.alexandra.fresco.suite.spdz.SpdzResourcePoolImpl;
import dk.alexandra.fresco.suite.spdz.storage.SpdzDataSupplier;
import dk.alexandra.fresco.suite.spdz.storage.SpdzDummyDataSupplier;
import dk.alexandra.fresco.suite.spdz.storage.SpdzOpenedValueStoreImpl;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

public class LinearRegressionDemo {

  public static void main(String[] arguments) throws IOException {
    if (arguments.length != 1) {
      throw new IllegalArgumentException("Usage: java Demo [id]");
    }

    final int myId = Integer.parseInt(arguments[0]);
    final int noParties = 2;
    final int otherId = 3 - myId;
    final int modBitLength = 256;
    final int maxBitLength = 180;
//    final int prgSeedLength = 256;
    final int maxBatchSize = 4096;

    Party me = new Party(myId, "localhost", 9000 + myId);
    Party other = new Party(myId, "localhost", 9000 + otherId);
    NetworkConfiguration networkConfiguration = new NetworkConfigurationImpl(myId,
        Map.of(myId, me, otherId, other));
    Network network = new SocketNetwork(networkConfiguration);

//    byte[] seed = new byte[prgSeedLength / 8];
//    new Random(myId).nextBytes(seed);
//    Drbg drbg = AesCtrDrbgFactory.fromDerivedSeed(seed);
//
//    Map<Integer, RotList> seedOts = new HashMap<>();
//    for (int id = 1; id <= noParties; id++) {
//      if (myId != otherId) {
//        DHParameterSpec dhSpec = DhParameters.getStaticDhParams();
//        Ot ot = new NaorPinkasOt(otherId, drbg, network, dhSpec);
//        RotList currentSeedOts = new RotList(drbg, prgSeedLength);
//        if (myId < otherId) {
//          currentSeedOts.send(ot);
//          currentSeedOts.receive(ot);
//        } else {
//          currentSeedOts.receive(ot);
//          currentSeedOts.send(ot);
//        }
//        seedOts.put(otherId, currentSeedOts);
//      }
//    }

    MersennePrimeFieldDefinition definition = MersennePrimeFieldDefinition.find(modBitLength);
//    FieldElement ssk = SpdzMascotDataSupplier.createRandomSsk(definition, prgSeedLength);
    SpdzProtocolSuite suite = new SpdzProtocolSuite(maxBitLength);

//    SpdzDataSupplier preprocessedValuesDataSupplier
//        = SpdzMascotDataSupplier.createSimpleSupplier(myId, noParties,
//            () -> network, modBitLength, definition, null, seedOts, drbg, ssk);
//    SpdzResourcePool preprocessedValuesResourcePool = new SpdzResourcePoolImpl(myId, noParties,
//        new SpdzOpenedValueStoreImpl(),
//        preprocessedValuesDataSupplier, AesCtrDrbg::new);

    SpdzDataSupplier supplier = new SpdzDummyDataSupplier(myId, noParties, definition, BigInteger.valueOf(1234));
//    SpdzDataSupplier supplier = SpdzMascotDataSupplier
//        .createSimpleSupplier(myId, noParties, () -> network,
//            modBitLength, definition, pipeLength -> {
//              DRes<List<DRes<SInt>>> pipe = createPipe(pipeLength, network,
//                  preprocessedValuesResourcePool, suite);
//              return computeSInts(pipe);
//            }, seedOts, drbg, ssk);

    SpdzResourcePool resourcePool = new SpdzResourcePoolImpl(myId, noParties,
        new SpdzOpenedValueStoreImpl(), supplier,
        AesCtrDrbg::new);

    BatchedProtocolEvaluator<SpdzResourcePool> evaluator =
        new BatchedProtocolEvaluator<>(EvaluationStrategy.SEQUENTIAL_BATCHED.getStrategy(), suite,
            maxBatchSize);
    SecureComputationEngine<SpdzResourcePool, ProtocolBuilderNumeric> sce = new SecureComputationEngineImpl<>(
        suite, evaluator);

    Reader in = new FileReader("real-estate-" + myId + ".csv");
    Iterable<CSVRecord> records = CSVFormat.DEFAULT.withRecordSeparator(",").parse(in);
    List<List<String>> data = StreamSupport.stream(records.spliterator(), false).map(
        record -> StreamSupport.stream(record.spliterator(), false).collect(Collectors.toList())).collect(Collectors.toList());

    // Party 1 has all independent variables, party 2 has the dependant variable
    List<List<Double>> x = new ArrayList<>();
    List<Double> y = new ArrayList<>();
    int n = data.size();
    int p = 3;
    if (myId == 1) {
      for (List<String> row : data) {
        x.add(row.stream().mapToDouble(Double::parseDouble).boxed()
            .collect(Collectors.toList()));
      }
      y.addAll(Collections.nCopies(n, null));
    } else {
      x.addAll(Collections.nCopies(n, null));
      for (List<String> row : data) {
        y.add(Double.parseDouble(row.get(0)));
      }
    }

    System.out.println("id = " + myId);
    System.out.println("n  = " + n);
    System.out.println("x  = " + x.subList(0, 5) + " ...");
    System.out.println("y  = " + y.subList(0, 5) + " ...");

    Instant start = Instant.now();

    List<BigDecimal> out = sce
        .runApplication(new LinearRegressionApplication(x, y, n, p),
            resourcePool, network);

    System.out.println("b  = " + out);
    System.out.println("Took " + Duration.between(start, Instant.now()));
  }

//  private static DRes<List<DRes<SInt>>> createPipe(int pipeLength, Network network,
//      SpdzResourcePool resourcePool, SpdzProtocolSuite protocolSuite) {
//    ProtocolBuilderNumeric sequential = protocolSuite.init(resourcePool).createSequential();
//    Application<List<DRes<SInt>>, ProtocolBuilderNumeric> expPipe = builder ->
//        new DefaultPreprocessedValues(builder).getExponentiationPipe(pipeLength);
//    DRes<List<DRes<SInt>>> exponentiationPipe = expPipe.buildComputation(sequential);
//    evaluate(sequential, resourcePool, network, protocolSuite);
//    return exponentiationPipe;
//  }
//
//  private static SpdzSInt[] computeSInts(DRes<List<DRes<SInt>>> pipe) {
//    List<DRes<SInt>> out = pipe.out();
//    SpdzSInt[] result = new SpdzSInt[out.size()];
//    for (int i = 0; i < out.size(); i++) {
//      DRes<SInt> sIntResult = out.get(i);
//      result[i] = (SpdzSInt) sIntResult.out();
//    }
//    return result;
//  }
//
//  private static void evaluate(ProtocolBuilderNumeric spdzBuilder,
//      SpdzResourcePool tripleResourcePool,
//      Network network, SpdzProtocolSuite protocolSuite) {
//    BatchedStrategy<SpdzResourcePool> batchedStrategy = new BatchedStrategy<>();
//    BatchedProtocolEvaluator<SpdzResourcePool> batchedProtocolEvaluator =
//        new BatchedProtocolEvaluator<>(batchedStrategy, protocolSuite);
//    batchedProtocolEvaluator.eval(spdzBuilder.build(), tripleResourcePool, network);
//  }

  public static class LinearRegressionApplication implements
      Application<List<BigDecimal>, ProtocolBuilderNumeric> {

    private final List<List<Double>> myX;
    private final List<Double> y;
    private final int n;
    private final int p;

    public LinearRegressionApplication(List<List<Double>> myX, List<Double> y, int n, int p) {
      this.myX = myX;
      this.y = y;
      this.n = n;
      this.p = p;
    }

    @Override
    public DRes<List<BigDecimal>> buildComputation(ProtocolBuilderNumeric builder) {
      return builder.par(par -> {
        int id = par.getBasicNumericContext().getMyId();
        FixedNumeric fixedNumeric = FixedNumeric.using(par);
        List<ArrayList<DRes<SFixed>>> observations = new ArrayList<>();
        ArrayList<DRes<SFixed>> dependants = new ArrayList<>();

        if (id == 1) {
          for (List<Double> observation : myX) {
            ArrayList<DRes<SFixed>> row = new ArrayList<>();
            row.add(fixedNumeric.known(1.0));
            for (double xi : observation) {
              row.add(fixedNumeric.input(xi, 1));
            }
            observations.add(row);

          }
          for (int i = 0; i < n; i++) {
            dependants.add(fixedNumeric.input(null, 2));
          }
        } else if (id == 2) {
          for (int i = 0; i < n; i++) {
            ArrayList<DRes<SFixed>> row = new ArrayList<>();
            row.add(fixedNumeric.known(1.0));
            for (int j = 0; j < p; j++) {
              row.add(fixedNumeric.input(null, 1));
            }
            observations.add(row);
          }

          for (Double yi : y) {
            dependants.add(fixedNumeric.input(yi, 2));
          }
        } else {
          throw new IllegalArgumentException("Id must be 1 or 2 but was " + id);
        }
        return Pair.lazy(observations, dependants);
      }).seq((seq, data) -> Statistics.using(seq).linearRegression(data.getFirst(),
          data.getSecond())).seq((seq, result) -> {
        FixedNumeric fixedNumeric = FixedNumeric.using(seq);
        return DRes.of(result.stream().map(fixedNumeric::open).collect(Collectors.toList()));
      }).seq((seq, data) -> DRes.of(data.stream().map(DRes::out).collect(Collectors.toList())));
    }
  }


}