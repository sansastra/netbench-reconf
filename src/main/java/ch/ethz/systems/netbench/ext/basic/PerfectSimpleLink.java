package ch.ethz.systems.netbench.ext.basic;

import ch.ethz.systems.netbench.core.network.Link;
import edu.asu.emit.algorithm.graph.Vertex;

public class PerfectSimpleLink extends Link {

    private final long delayNs;
    private  long bandwidthBitPerNs;
    private final int id;
    private final Vertex startVertex;
    private final Vertex endVertex;

    /**
     * Perfect simple link that never drops a packet.
     *
     * @param delayNs               Delay of each packet in nanoseconds
     * @param bandwidthBitPerNs     Bandwidth of the link (maximum line rate) in bits/ns
     */
    PerfectSimpleLink(Vertex start, Vertex end, int id, long delayNs, long bandwidthBitPerNs) {
        this.startVertex = start;
        this.endVertex = end;
        this.id = id;
        this.delayNs = delayNs;
        this.bandwidthBitPerNs = bandwidthBitPerNs;
    }

    @Override
    public long getDelayNs() {
        return delayNs;
    }

    @Override
    public long getBandwidthBitPerNs() {
        return bandwidthBitPerNs;
    }

    @Override
    public boolean doesNextTransmissionFail(long packetSizeBits) {
        return false;
    }

    @Override
    public int getLinkId() {return id;}

    @Override
    public Vertex getEndVertex() {
        return endVertex;
    }

    @Override
    public Vertex getStartVertex() {
        return startVertex;
    }

    @Override
    public void setBandwidthBitPerNs(long bandwidthBitPerNs) {
        this.bandwidthBitPerNs = bandwidthBitPerNs;
    }


}
