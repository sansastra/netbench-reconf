package ch.ethz.systems.netbench.ext.poissontraffic.flowsize;

import ch.ethz.systems.netbench.core.log.SimulationLogger;

public class Nekbone extends FlowSizeDistribution {

    public Nekbone() {
        super();
        SimulationLogger.logInfo("Flow planner flow size dist.", "pFabric web search upper bound discrete");
    }

    @Override
    public long generateFlowSizeByte() {
        double outcome = independentRng.nextDouble();
        if ( outcome <= 0.001) {
            return 7;
        }  else if ( outcome <= 0.2) {
            return 79;
        } else if ( outcome <= 0.39) {
            return 151;
        } else if ( outcome <= 0.72) {
            return 1519;
        } else if ( outcome <= 0.89) {
            return 2887;
        } else {
            return 33599;
        }

    }

}