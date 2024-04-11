package ch.ethz.systems.netbench.core.network;

import ch.ethz.systems.netbench.core.Simulator;
import ch.ethz.systems.netbench.core.run.MainFromProperties;

/**
 * Event for the network reconfiguration, i.e. adding and removing links.
 */
public class ReconfigEvent extends Event {

    /**
     * Packet dispatched event constructor.
     *
     * @param timeFromNowNs     Time in simulation nanoseconds from now
     */
    public ReconfigEvent(long timeFromNowNs) {
        super(timeFromNowNs);
    }

    @Override
    public void trigger() {
        Simulator.getBaselineInitializer().modifyInfrastructure();
        MainFromProperties.populateRoutingState(Simulator.getBaselineInitializer().getIdToNetworkDevice());
        //Simulator.getRoutingPopulator().populateRoutingTables();
        Simulator.setReconfig_in_progress(false);
    }

    @Override
    public String toString() {
        return  "ReconfigEvent";
    }

}
