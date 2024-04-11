package ch.ethz.systems.netbench.xpt.voijslav.ports;

import ch.ethz.systems.netbench.core.log.SimulationLogger;
import ch.ethz.systems.netbench.core.network.Link;
import ch.ethz.systems.netbench.core.network.NetworkDevice;
import ch.ethz.systems.netbench.core.network.OutputPort;
import ch.ethz.systems.netbench.core.run.infrastructure.OutputPortGenerator;

public class PriorityOutputPortGenerator  extends OutputPortGenerator {

    public PriorityOutputPortGenerator() {
        SimulationLogger.logInfo("Port", "PRIORITY_PORT");
    }

    @Override
    public OutputPort generate(NetworkDevice ownNetworkDevice, NetworkDevice towardsNetworkDevice, Link link, int port_id) {
        return new PriorityOutputPort(ownNetworkDevice, towardsNetworkDevice, link, port_id);
    }

}
