package ch.ethz.systems.netbench.core.run.infrastructure;

import ch.ethz.systems.netbench.core.Simulator;
import ch.ethz.systems.netbench.core.config.GraphDetails;
import ch.ethz.systems.netbench.core.network.Link;
import ch.ethz.systems.netbench.core.network.NetworkDevice;
import ch.ethz.systems.netbench.core.network.OutputPort;
import ch.ethz.systems.netbench.core.network.TransportLayer;
import ch.ethz.systems.netbench.ext.ecmp.EcmpSwitchRoutingInterface;
import ch.ethz.systems.netbench.xpt.sourcerouting.SourceRoutingSwitch;
import edu.asu.emit.algorithm.graph.Graph;
import edu.asu.emit.algorithm.graph.Vertex;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Initializes the entire infrastructure in the most
 * generic way possible using the given generators.
 *
 * Any differentiation between applications, transport layers,
 * output ports, links or nodes are decided by
 * their respective generators.
 */
public class BaseInitializer {

    // Mappings
    private final Map<Integer, NetworkDevice> idToNetworkDevice;
    private final Map<Integer, TransportLayer> idToTransportLayer;
    private Map<Integer, Link> idToLink;
    private int link_id =0;
    // Generators
    private final OutputPortGenerator outputPortGenerator;
    private final NetworkDeviceGenerator networkDeviceGenerator;
    private final LinkGenerator linkGenerator;
    private final TransportLayerGenerator transportLayerGenerator;

    // Validation variables
    private int runningNodeId;
    private boolean infrastructureAlreadyCreated;
    private List<Pair<Integer, Integer>> linkPairs;

    public BaseInitializer(
            OutputPortGenerator outputPortGenerator,
            NetworkDeviceGenerator networkDeviceGenerator,
            LinkGenerator linkGenerator,
            TransportLayerGenerator transportLayerGenerator
    ) {
        this.idToNetworkDevice = new HashMap<>();
        this.idToTransportLayer = new HashMap<>();
        this.idToLink = new HashMap<>();
        this.outputPortGenerator = outputPortGenerator;
        this.networkDeviceGenerator = networkDeviceGenerator;
        this.linkGenerator = linkGenerator;
        this.transportLayerGenerator = transportLayerGenerator;
        this.runningNodeId = 0;
        this.infrastructureAlreadyCreated = false;
        this.linkPairs = new ArrayList<>();
    }

    /**
     * Read the base network (servers, network devices, links) from the topology file.
     */
    public void createInfrastructure() {

        // The infrastructure can only be created once
        if (infrastructureAlreadyCreated) {
            throw new RuntimeException("Impossible to create infrastructure twice.");
        }

        // Fetch from configuration graph and its details
        Graph graph = Simulator.getConfiguration().getGraph();
        GraphDetails details = Simulator.getConfiguration().getGraphDetails();

        // Create nodes
        for (int i = 0; i < details.getNumNodes(); i++) {
            createNode(i, details.getServerNodeIds().contains(i));
        }

        // Create edges
        long edgeLine_rate = 10;
        for (Vertex v : graph.getVertexList()) {
            for (Vertex w : graph.getAdjacentVertices(v)) {
                long localBandwidthNs = graph.getEdgeWeight(v, w); // a single FSR link between v and w
                if (details.getServerNodeIds().contains(v.getId()) || details.getServerNodeIds().contains(w.getId()))
                    edgeLine_rate  = Simulator.getConfiguration().getLongPropertyOrFail("link_bandwidth_bit_per_ns");
                else
                    edgeLine_rate = Simulator.getConfiguration().getLongPropertyOrFail("tor_link_bandwidth_bit_per_ns");

                createEdge(v, w, localBandwidthNs, edgeLine_rate);
            }
        }

        // Check the links for bi-directionality
        for (int i = 0; i < linkPairs.size(); i++) {

            // Attempt to find the reverse
            boolean found = false;
            for (int j = 0; j < linkPairs.size(); j++) {
                if (i != j && linkPairs.get(j).equals(new ImmutablePair<>(linkPairs.get(i).getRight(), linkPairs.get(i).getLeft()))) {
                    found = true;
                    break;
                }
            }
            // If reverse not found, it is not bi-directional
            if (!found) {
                throw new IllegalArgumentException(
                        "Link was added which is not bi-directional: " +
                        linkPairs.get(i).getLeft() + " -> " + linkPairs.get(i).getRight()
                );
            }
        }

    }

    /**
     * Create the implementation of a node in the network.
     *
     * @param id        Node identifier
     * @param isServer  True iff it is a server (a.k.a. will have a transport layer)
     */
    private void createNode(int id, boolean isServer) {

        // Make sure that the node identifiers are in sequence
        if (id != runningNodeId) {
            throw new IllegalArgumentException(
                    "A node identifier has been skipped. " +
                    "Expected " + runningNodeId + " but next was " + id + ". Please check input topology file.");
        }
        runningNodeId++;

        // Create corresponding network device
        NetworkDevice networkDevice;
        if (isServer) {

            // Create server
            TransportLayer transportLayer = transportLayerGenerator.generate(id);

            // Create network device
            networkDevice = networkDeviceGenerator.generate(id, transportLayer);
            idToTransportLayer.put(id, transportLayer);

            // Link transport layer to network device
            transportLayer.setNetworkDevice(networkDevice);

        } else {
            networkDevice = networkDeviceGenerator.generate(id);
        }

        // Add to mappings
        idToNetworkDevice.put(id, networkDevice);

    }

    /**
     * Create the implementation of a directed edge in the network.
     *
     * @param startVertex     Origin vertex identifier
     * @param endVertex      Destination vertex identifier
     */
    private void createEdge(Vertex startVertex, Vertex endVertex, long localBandwidthNs, long link_rate) {
        int startVertexId = startVertex.getId();
        int endVertexId = endVertex.getId();
        // Select network devices
        NetworkDevice devA = idToNetworkDevice.get(startVertexId);
        NetworkDevice devB = idToNetworkDevice.get(endVertexId);

        // Add connection
        if (localBandwidthNs <= 0.0) {
//            Link link = linkGenerator.generate( startVertex, endVertex, link_id);
//            OutputPort portAtoB = outputPortGenerator.generate(devA, devB, link, link_id);
//            idToLink.put(link_id, link);
       // devA.addConnection(portAtoB);
        } else {
            //List: although single link initial, but after reconfiguration, there might be more,so function call should be compatible
            List<OutputPort> listOfPortsToTargetIds = new ArrayList();
            // 2 times is for ToR-ToR link with 20G, ToR-server is 10 G
            for (int id=0; id < localBandwidthNs/link_rate; id++) {
                Link link = linkGenerator.generate(startVertex, endVertex, link_id, link_rate);
                OutputPort portAtoB = outputPortGenerator.generate(devA, devB, link, link_id);
                listOfPortsToTargetIds.add(portAtoB);
                idToLink.put(link_id, link);
                link_id++;
            }
            if (listOfPortsToTargetIds.size()==0)
                System.out.println(localBandwidthNs+link_rate);

           // listOfPortsToTargetIds.sort(Comparator.comparing(OutputPort::getBufferOccupiedBits));

            devA.addConnection(listOfPortsToTargetIds);
        }


        // Duplicate link
        if (linkPairs.contains(new ImmutablePair<>(startVertexId, endVertexId))) {
            throw new IllegalArgumentException(
                    "Duplicate link (" + startVertexId + " -> + " + endVertexId +
                    ") defined - Please check input topology file."
            );
        } else {
            linkPairs.add(new ImmutablePair<>(startVertexId, endVertexId));
        }

    }

    private void modifyEdge(Vertex startVertex, Vertex endVertex, long localBandwidthNs, long link_rate) {
        int startVertexId = startVertex.getId();
        int endVertexId = endVertex.getId();
        // Select network devices
        NetworkDevice devA = idToNetworkDevice.get(startVertexId);
        NetworkDevice devB = idToNetworkDevice.get(endVertexId);

        // modify connection
        int numPortRemove = (int) (devA.getnumPortsToTarget(endVertexId) -  localBandwidthNs/link_rate);
        if (numPortRemove > 0){ // remove ports
            devA.modifyConnection( numPortRemove, endVertexId, null ) ;
            Simulator.setIfReconfigs();
        }else if (numPortRemove < 0){ // add ports
            Simulator.setIfReconfigs();
            List<OutputPort> listOfPortsToTargetIds = new ArrayList();
            for (int id=0; id > numPortRemove; id--) {
                Link link = linkGenerator.generate(startVertex, endVertex, link_id, link_rate);
                OutputPort portAtoB = outputPortGenerator.generate(devA, devB, link, link_id);
                listOfPortsToTargetIds.add(portAtoB);
                idToLink.put(link_id, link);
                link_id++;
            }

            devA.modifyConnection(numPortRemove, endVertexId, listOfPortsToTargetIds ) ;
        } // if numPortremove == 0 there is no need to modify
    }

    // block output ports that will be reconfigured from accepting new packets
    public void blockPortsToBeRemoved() {
        Simulator.getConfiguration().findFutureGraph(Simulator.getConfiguration().getPropertyOrFail("scenario_topology_file"));
        // Fetch from configuration graph and its details
        Graph graph_future = Simulator.getConfiguration().getGraph_future();
        GraphDetails details_future = Simulator.getConfiguration().getGraphDetails_future();
        Graph graph_now = Simulator.getConfiguration().getGraph();
        GraphDetails details_now = Simulator.getConfiguration().getGraphDetails();
        // modify edges
        long edgeLine_rate =10;
        for (Vertex v : graph_now.getVertexList()) {
            for (Vertex w : graph_now.getAdjacentVertices(v)) {
                long localBandwidthNs = graph_now.getEdgeWeight(v, w);
                // Select network devices

                NetworkDevice devA = idToNetworkDevice.get(v.getId());
                // NetworkDevice devB = idToNetworkDevice.get(endVertexId);

                // modify connection between tors

                if (!(details_now.getServerNodeIds().contains(v.getId()) || details_now.getServerNodeIds().contains(w.getId()))) {
                    if (graph_future.getEdgeWeight(v, w)< localBandwidthNs){
                        edgeLine_rate = Simulator.getConfiguration().getLongPropertyOrFail("tor_link_bandwidth_bit_per_ns");
                        int numPortRemove = (int) (devA.getnumPortsToTarget(w.getId()) - graph_future.getEdgeWeight(v, w) / edgeLine_rate);
                        if (numPortRemove > 0)
                            devA.blockPortsToBeReconfigured(numPortRemove, w.getId());
                    }
                }
            }

        } // if numPortremove == 0 there is no need to modify
    }

    public void reset_nodes_next_hop(){
        Graph graph_now = Simulator.getConfiguration().getGraph();
        GraphDetails details_now = Simulator.getConfiguration().getGraphDetails();
        if (Simulator.getConfiguration().getPropertyOrFail("network_device_routing").equals("ecmp")) {
            for (Vertex v : graph_now.getVertexList()) {
                ((EcmpSwitchRoutingInterface) idToNetworkDevice.get(v.getId())).resetDestinationToNextSwitch(details_now.getNumNodes());
            }
        } else if (Simulator.getConfiguration().getPropertyOrFail("network_device_routing").equals("k_shortest_paths")) {
            for (Vertex v : graph_now.getVertexList()) {
                ((SourceRoutingSwitch) idToNetworkDevice.get(v.getId())).resetPathsToDestination(details_now.getNumNodes());
            }
        }
    }

    // modify infrastructure for reconfiguration
    public void modifyInfrastructure() {
        // first remove the deleted connections between nodes
        Simulator.getConfiguration().findFutureGraph(Simulator.getConfiguration().getPropertyOrFail("scenario_topology_file"));
        // Fetch from configuration graph and its details
        Graph graph_future = Simulator.getConfiguration().getGraph_future();
        GraphDetails details_future = Simulator.getConfiguration().getGraphDetails_future();
        Graph graph_now = Simulator.getConfiguration().getGraph();
        GraphDetails details_now = Simulator.getConfiguration().getGraphDetails();
        // modify edges
        long edgeLine_rate = Simulator.getConfiguration().getLongPropertyOrFail("tor_link_bandwidth_bit_per_ns");
        for (Vertex v : graph_now.getVertexList()) {
            for (Vertex w : graph_now.getAdjacentVertices(v)) {
                long localBandwidthNs = graph_now.getEdgeWeight(v, w);
                // Select network devices

                NetworkDevice devA = idToNetworkDevice.get(v.getId());
                // NetworkDevice devB = idToNetworkDevice.get(endVertexId);

                // modify connection between tor, no need to check servers

                if (!(details_now.getServerNodeIds().contains(v.getId()) || details_now.getServerNodeIds().contains(w.getId()))) {
                    if (graph_future.getEdgeWeight(v, w) == 0) { // remove the link directly if no link in future
                        // Select network devices
                        // modify connection
                        int numPortRemove = (int) (devA.getnumPortsToTarget(w.getId()) - graph_future.getEdgeWeight(v, w) / edgeLine_rate);
                        if (numPortRemove > 0) // remove ports
                            devA.modifyConnection(numPortRemove, w.getId(), null);
                    }
                }
            }
        }
        // System.out.println("reseting switching info");
        // reset next hope info in network devices before new routing
        reset_nodes_next_hop();

        // now modify edges of new graph
        Simulator.getConfiguration().readGraph();
        // Simulator.getConfiguration().modifyGraph(Simulator.getConfiguration().getPropertyOrFail("scenario_topology_file"));
        // Fetch from configuration graph and its details
        Graph graph = Simulator.getConfiguration().getGraph();
        GraphDetails details = Simulator.getConfiguration().getGraphDetails();
        // modify edges

        for (Vertex v : graph.getVertexList()) {
            for (Vertex w : graph.getAdjacentVertices(v)) {
                long localBandwidthNs = graph.getEdgeWeight(v, w);
                // if server-tor bandwidth is unchanged
                if (details.getServerNodeIds().contains(v.getId()) || details.getServerNodeIds().contains(w.getId()))
                    edgeLine_rate  = Simulator.getConfiguration().getLongPropertyOrFail("link_bandwidth_bit_per_ns");
                else
                    edgeLine_rate = Simulator.getConfiguration().getLongPropertyOrFail("tor_link_bandwidth_bit_per_ns");
                modifyEdge(v, w, localBandwidthNs, edgeLine_rate);
            }
        }
        /** old method
        for (Map.Entry<Integer, Link> entry: idToLink.entrySet()){
            Link val_link = entry.getValue();
            long newWeight = graph.getEdgeWeight(val_link.getStartVertex(), val_link.getEndVertex());
            val_link.setBandwidthBitPerNs(newWeight);
        }

        */

        // Check the links for bi-directionality
        for (int i = 0; i < linkPairs.size(); i++) {

            // Attempt to find the reverse
            boolean found = false;
            for (int j = 0; j < linkPairs.size(); j++) {
                if (i != j && linkPairs.get(j).equals(new ImmutablePair<>(linkPairs.get(i).getRight(), linkPairs.get(i).getLeft()))) {
                    found = true;
                    break;
                }
            }
            // If reverse not found, it is not bi-directional
            if (!found) {
                throw new IllegalArgumentException(
                        "Link was added which is not bi-directional: " +
                                linkPairs.get(i).getLeft() + " -> " + linkPairs.get(i).getRight()
                );
            }
        }

    }

    /**
     * Retrieve identifier to network device mapping.
     *
     * @return  Mapping of node identifier to its network device
     */
    public Map<Integer, NetworkDevice> getIdToNetworkDevice() {
        return idToNetworkDevice;
    }

    /**
     * Retrieve identifier to transport layer mapping.
     *
     * @return  Mapping of node identifier to its transport layer
     */
    public Map<Integer, TransportLayer> getIdToTransportLayer() {
        return idToTransportLayer;
    }

}
