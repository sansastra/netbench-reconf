package ch.ethz.systems.netbench.core.network;

import ch.ethz.systems.netbench.core.log.SimulationLogger;

import java.util.*;

/**
 * Abstraction for a network device.
 *
 * All nodes in the network are instances of this abstraction.
 * It takes care of the definition of network connections and
 * forces its subclasses to be able to handle packets it receives.
 * A network device is a server iff it has a
 * {@link TransportLayer transport layer}.
 *
 * It enables additional modification of packets by placement of a
 * {@link Intermediary intermediary}
 * in between the network device and the transport layer.
 */
public abstract class NetworkDevice {

    private final TransportLayer transportLayer;
    private final boolean hasTransportLayer;
    protected final int identifier;
    protected final Map<Integer, Integer> connectedTo;//List<Integer> connectedTo;
    protected final Map<Integer, List<OutputPort>> targetIdToOutputPort;
    protected final Intermediary intermediary;

    /**
     * Constructor of a network device.
     *
     * @param identifier        Network device identifier
     * @param transportLayer    Transport layer instance (null, if only router and not a server)
     * @param intermediary      Flowlet intermediary instance (takes care of flowlet support)
     */
    protected NetworkDevice(int identifier, TransportLayer transportLayer, Intermediary intermediary) {

        // Permanent unique identifier assigned
        this.identifier = identifier;

        // Initialize internal data structures
        this.connectedTo = new HashMap<>(); //new ArrayList<>();
        this.targetIdToOutputPort = new HashMap<>();

        // Set the server and whether it exists
        this.transportLayer = transportLayer;
        this.hasTransportLayer = (transportLayer != null);

        // Add intermediary
        this.intermediary = intermediary;
        this.intermediary.setNetworkDevice(this);

    }

    /**
     * Retrieve the automatically generated unique
     * network device identifier.
     *
     * @return  Unique network device identifier
     */
    public int getIdentifier() {
        return identifier;
    }

    /**
     * Add a port which is a connection to another network device.
     *
     * param outputPort    Output port instance
     */
    public void addConnection(List<OutputPort> listOutputPorts) {

        // Port does not originate from here
        if (getIdentifier() != listOutputPorts.get(0).getOwnId()) {
            throw new IllegalArgumentException("Impossible to add output port not originating from " + getIdentifier() + " (origin given: " + listOutputPorts.get(0).getOwnId() + ")");
        }

        // Port going there already exists
        if (connectedTo.containsKey(listOutputPorts.get(0).getTargetId())) {
            throw new IllegalArgumentException("Impossible to add a duplicate port from " + listOutputPorts.get(0).getOwnId() + " to " + listOutputPorts.get(0).getTargetId() + ".");
        }

        // Add to mappings
        connectedTo.put(listOutputPorts.get(0).getTargetId(), listOutputPorts.get(0).getTargetId());
        targetIdToOutputPort.put(listOutputPorts.get(0).getTargetId(), listOutputPorts);
    }

    /**
     * modify output port of the network device: modify connection
     */
    public void modifyConnection(int numPortRemove, int targetId, List<OutputPort> listOutputPorts ) {

        if (numPortRemove > 0) {
//            if (this.targetIdToOutputPort.get(targetId).size() < numPortRemove) {
//                System.out.println("number of ports to removed is larger");
//                numPortRemove = this.targetIdToOutputPort.get(targetId).size();
//            }
            this.targetIdToOutputPort.get(targetId).sort(Comparator.comparing(OutputPort::getBufferOccupiedBits));
            for (int i=0; i < numPortRemove; i++) {
                int packets_dropped = (int) Math.ceil(((double) this.targetIdToOutputPort.get(targetId).get(0).getBufferOccupiedBits()) / (1500.0 * 8));
               // System.out.println("bits dropped" + this.targetIdToOutputPort.get(targetId).get(0).getBufferOccupiedBits());
                for (int j = 0; j < packets_dropped; j++)
                    SimulationLogger.increaseStatisticCounter("PACKETS_DROPPED");
                // clear buffer
                this.targetIdToOutputPort.get(targetId).get(0).decreaseBufferOccupiedBits(this.targetIdToOutputPort.get(targetId).get(0).getBufferOccupiedBits());
                // remove tor switch connection to a node if all its link/output ports are removed
                this.targetIdToOutputPort.get(targetId).remove(0);
            }
            if (connectedTo.containsKey(targetId) && this.targetIdToOutputPort.get(targetId).size()==0)
                connectedTo.remove(targetId);
        }else { // < 0 condition is already called with add new outputPorts
            if (!this.hasConnection(listOutputPorts.get(0).getTargetId()))
                connectedTo.put(listOutputPorts.get(0).getTargetId(), listOutputPorts.get(0).getTargetId());
            for (OutputPort outputPort: listOutputPorts)
                this.targetIdToOutputPort.get(targetId).add(outputPort);
              //  throw new IllegalArgumentException("Impossible to modify and add output port not originating from this device");
        }
    }

    /**
     * modify output port of the network device: modify connection
     */
    public void blockPortsToBeReconfigured(int numPortRemove, int targetId) {
        this.targetIdToOutputPort.get(targetId).sort(Comparator.comparing(OutputPort::getBufferOccupiedBits));
        for (int i=0; i < numPortRemove; i++) {
            this.targetIdToOutputPort.get(targetId).get(i).setBlocked(true);
            SimulationLogger.logBytesDropDuringReconf(this.targetIdToOutputPort.get(targetId).get(i).getBufferOccupiedBits()/8);
            int packets_dropped = (int) Math.ceil(((double) this.targetIdToOutputPort.get(targetId).get(i).getBufferOccupiedBits()) / (1500.0 * 8));
            // System.out.println("bits dropped" + this.targetIdToOutputPort.get(targetId).get(0).getBufferOccupiedBits());
            for (int j = 0; j < packets_dropped; j++)
                SimulationLogger.increaseStatisticCounter("PACKETS_DROPPED");
            // clear buffer
            this.targetIdToOutputPort.get(targetId).get(i).decreaseBufferOccupiedBits(this.targetIdToOutputPort.get(targetId).get(i).getBufferOccupiedBits());
        }
    }

    public int getnumPortsToTarget(int targetId){
        if (this.targetIdToOutputPort.containsKey(targetId))
            return  this.targetIdToOutputPort.get(targetId).size();
        else
            return 0;
    }


    /**
     * Check whether this network device has an outgoing port to the target.
     *
     * @param target    Target network device identifier
     *
     * @return  True iff an outgoing port from this network device exists to the target
     */
    public boolean hasConnection(int target) {
        return connectedTo.containsKey(target);
    }

    /**
     * Reception of a packet by the network device from another network device.
     * // TODO: make it protected-local?
     *
     * @param genericPacket    Packet instance
     */
    public abstract void receive(Packet genericPacket);

    /**
     * Reception of a packet by the network device from the underlying transport layer (adapted by flowlet
     * intermediary if necessary).
     *
     * @param genericPacket     Packet instance (with flowlet modified)
     */
    protected abstract void receiveFromIntermediary(Packet genericPacket);

    /**
     * Pass a packet to the underlying intermediary, which adapts it if needed
     * and forwards it to the transport layer of the network device.
     *
     * @param genericPacket     Packet instance
     */
    protected void passToIntermediary(Packet genericPacket) {
        intermediary.adaptIncoming(genericPacket);
        transportLayer.receive(genericPacket);
    }

    /**
     * Reception of a packet by the network device from the underlying transport layer.
     * Adapts it via the intermediary and then sends it on to the switch.
     * Do not override. // TODO: make it package-local?
     *
     * @param genericPacket    Packet instance
     */
    public void receiveFromTransportLayer(Packet genericPacket) {
        intermediary.adaptOutgoing(genericPacket);
        this.receiveFromIntermediary(genericPacket);
    }

    /**
     * Check whether the network device has a transport layer and is thus a potential server.
     * A server is a node which is able to send and/or receive traffic in the network, in essence
     * it can be a source or sink whereas normal routing nodes without transport layer cannot be.
     *
     * @return  True iff the network device is a server
     */
	public boolean isServer() {
		return hasTransportLayer;
	}

    /**
     * Retrieve the underlying transport layer of the network device.
     * Must not be called if there is no underlying transport layer (so the network device is not a server).
     *
     * @return  Transport layer
     */
    public TransportLayer getTransportLayer() {
        assert(transportLayer != null);
        return transportLayer;
    }

}
