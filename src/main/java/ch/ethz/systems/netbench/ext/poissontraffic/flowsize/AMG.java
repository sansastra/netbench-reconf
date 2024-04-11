package ch.ethz.systems.netbench.ext.poissontraffic.flowsize;

import ch.ethz.systems.netbench.core.log.SimulationLogger;

public class AMG extends FlowSizeDistribution {

    public AMG() {
        super();
        SimulationLogger.logInfo("Flow planner flow size dist.", "pFabric web search upper bound discrete");
    }

    @Override
    public long generateFlowSizeByte() {

        double outcome = independentRng.nextDouble();
        if (outcome <= 0.67) {
            return 11;
        } else if (outcome <= 0.70) {
            return 14;
        } else if (outcome <= 0.74) {
            return 18;
        } else if (outcome <= 0.77) {
            return 25;
        } else if (outcome <= 0.81) {
            return 36;
        } else if (outcome <= 0.85) {
            return 58;
        } else if (outcome <= 0.89) {
            return 196;
        } else if (outcome <= 0.92) {
            return 428;
        }else {
            return 1599;
        }

    }

}