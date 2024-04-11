package ch.ethz.systems.netbench.ext.poissontraffic.flowsize;

import ch.ethz.systems.netbench.core.log.SimulationLogger;

public class AMR extends FlowSizeDistribution {

    public AMR() {
        super();
        SimulationLogger.logInfo("Flow planner flow size dist.", "pFabric web search upper bound discrete");
    }

    @Override
    public long generateFlowSizeByte() {
        double outcome = independentRng.nextDouble();
        if ( outcome <= 0.07) {
            return 15;
        }  else if ( outcome <= 0.17) {
            return 79;
        } else if ( outcome <= 0.22) {
            return 255;
        } else if ( outcome <= 0.32) {
            return 263;
        } else if ( outcome <= 0.38) {
            return 455;
        } else if ( outcome <= 0.43) {
            return 511;
        } else if ( outcome <= 0.58) {
            return 2047;
        } else if ( outcome <= 0.64) {
            return 8191;
        } else if ( outcome <= 0.74) {
            return 8447;
        } else if ( outcome <= 0.79) {
            return 8975;
        } else if ( outcome <= 0.84) {
            return 16383;
        } else if ( outcome <= 0.90) {
            return 40959;
        } else {
            return 327678;
        }

    }

}
