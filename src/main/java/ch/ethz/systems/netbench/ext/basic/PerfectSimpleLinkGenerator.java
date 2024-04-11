package ch.ethz.systems.netbench.ext.basic;

import ch.ethz.systems.netbench.core.log.SimulationLogger;
import ch.ethz.systems.netbench.core.network.Link;
import ch.ethz.systems.netbench.core.network.NetworkDevice;
import ch.ethz.systems.netbench.core.run.infrastructure.LinkGenerator;
import edu.asu.emit.algorithm.graph.Vertex;

public class PerfectSimpleLinkGenerator extends LinkGenerator {

    private   long delayNs;
    private   long bandwidthBitPerNs;


    public PerfectSimpleLinkGenerator( long delayNs, long bandwidthBitPerNs) {
        this.delayNs = delayNs;
        this.bandwidthBitPerNs = bandwidthBitPerNs;

        SimulationLogger.logInfo("Link", "PERFECT_SIMPLE_LINK(delayNs=" + delayNs + ", bandwidthBitPerNs=" + bandwidthBitPerNs + ")");
    }


    public Link generate(Vertex start, Vertex end, int id) {
        return new PerfectSimpleLink(start, end, id, delayNs, bandwidthBitPerNs);
    }


    public Link generate(Vertex start, Vertex end, int id, long localBandwidthBitPerNs) {
        return new PerfectSimpleLink(start, end, id, delayNs, localBandwidthBitPerNs);
    }

}
