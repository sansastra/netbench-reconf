package ch.ethz.systems.netbench.core.run.infrastructure;

import ch.ethz.systems.netbench.core.network.NetworkDevice;
import ch.ethz.systems.netbench.core.network.Link;
import edu.asu.emit.algorithm.graph.Vertex;

public abstract class LinkGenerator {
    public abstract Link generate(Vertex start, Vertex end, int id);
    public abstract Link generate(Vertex start, Vertex end,  int id, long localBandwidthBitPerNs);

}
