package ch.ethz.systems.netbench.core.run;


import ch.ethz.systems.netbench.core.Simulator;
import ch.ethz.systems.netbench.core.config.BaseAllowedProperties;
import ch.ethz.systems.netbench.core.config.NBProperties;
import ch.ethz.systems.netbench.core.config.TopologyServerExtender;
import ch.ethz.systems.netbench.core.config.exceptions.PropertyConflictException;
import ch.ethz.systems.netbench.core.config.exceptions.PropertyMissingException;
import ch.ethz.systems.netbench.core.config.exceptions.PropertyValueInvalidException;
import ch.ethz.systems.netbench.core.log.SimulationLogger;
import ch.ethz.systems.netbench.core.network.NetworkDevice;
import ch.ethz.systems.netbench.core.network.TransportLayer;
import ch.ethz.systems.netbench.core.run.infrastructure.BaseInitializer;
import ch.ethz.systems.netbench.core.run.routing.RoutingPopulator;
import ch.ethz.systems.netbench.core.run.traffic.TrafficPlanner;
import ch.ethz.systems.netbench.core.utility.UnitConverter;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;



public class MainFromProperties {

    /**
     * Main from properties file.
     *
     * @param args  Command line arguments
     */
    public static void main(String args[]) {
        // Perform run
        System.out.println("ACTUAL RUN\n==================");
        int total_sim_run = 1; //Simulator.getConfiguration().getPropertyOrFail("network_load").split(" ", 0).length ;
        // to see whether reconfiguration took place or not

        for (int sim_run=0; sim_run < total_sim_run; sim_run++) {

            // Load in the configuration properties
            NBProperties runConfiguration = generateRunConfigurationFromArgs(args);

            // General property: random seed
            long seed = runConfiguration.getLongPropertyOrFail("seed");

            // General property: running time in nanoseconds
            long runtimeNs = determineRuntimeNs(runConfiguration);
            SimulationLogger.setLoad_number(runConfiguration.getPropertyOrFail("network_load").split(" ")[sim_run]);
            // Setup simulator (it is now public known)
            Simulator.setup(seed, runConfiguration);

            // Copy configuration files for reproducibility
            SimulationLogger.copyRunConfiguration();

            // Manage topology (e.g. extend with servers if said by configuration)
            manageTopology(); // if server, topology original file will be taken from created temp/./extended_topology.txt

            // Initialization of the three components
            BaseInitializer initializer = generateInfrastructure(); // vertex and links are created
            populateRoutingState(initializer.getIdToNetworkDevice());
            Simulator.setBaselineInitializer(initializer);

            // Save analysis command
            String analysisCommand = Simulator.getConfiguration().getPropertyWithDefault("analysis_command", null);

            // Perform run
            System.out.println("ACTUAL RUN\n==================");
            int total_run_per_sim = 10;


            // to see whether reconfiguration took place or not
            for (int run_number = 1; run_number <= total_run_per_sim; run_number++) {
                planTraffic(0, runtimeNs / total_run_per_sim, initializer.getIdToTransportLayer(), sim_run);
                Simulator.runNs(run_number * runtimeNs / total_run_per_sim, Simulator.getConfiguration().getLongPropertyWithDefault("finish_when_first_flows_finish", -1));
                if (Simulator.getConfiguration().getPropertyOrFail("reconfig").equals("true") && run_number == total_run_per_sim / 3) {
                    trigger_reconfig();
                }
                System.out.println("Finished run " + run_number);
            }

            Simulator.reset(false);
            // Perform analysis
//            System.out.println("ANALYSIS\n==================");
//            if (analysisCommand != null) {
//                runCommand(analysisCommand + " " + SimulationLogger.getRunFolderFull(), true);
//                System.out.println("Finished analysis.");
//            } else {
//                System.out.println("No analysis command given; analysis is skipped.");
//            }
        }
    }


    private static String get_next_file(String current_topo, int run_nr) { // end=.topology or .txt
        String temp = current_topo.split("\\.")[0];
        for (int j = temp.length()-2; j > 1; j-- ){//+ Integer.toString(Integer.valueOf(current_topo.split("_")[0]))
            if (temp.charAt(j) ==  '_')
                return temp.substring(0, j)+ "_"+Integer.toString(run_nr) + "."+current_topo.split("\\.")[1];
        }
        return  null;
    }

    /**
     * Generate the configuration from the arguments.
     *
     * Command-line template:
     * java -jar NetBench.jar /path/to/run_config.properties param1=val1 param2=val2
     *
     * @param args  Command-line arguments
     *
     * @return Final run configuration
     */
    private static NBProperties generateRunConfigurationFromArgs(String[] args) {

        // Argument length of at least one
        if (args.length < 1) {
            throw new RuntimeException("Expecting first argument to be configuration properties file for the run.");
        }

        // Load in the configuration properties
        NBProperties runConfiguration = new NBProperties(
                args[0],
                BaseAllowedProperties.LOG,
                BaseAllowedProperties.PROPERTIES_RUN,
                BaseAllowedProperties.EXTENSION,
                BaseAllowedProperties.EXPERIMENTAL
        );

        // Dynamic overwrite of temporary config using arguments given from command line
        for (int i = 1; i < args.length; i++) {
            int index = args[i].indexOf('=');
            assert( index != -1);
            String param = args[i].substring(0, index);
            String value = args[i].substring(index + 1);
            runConfiguration.overrideProperty(param, value);
        }

        return runConfiguration;

    }

    /**
     * Determine the amount of running time in nanoseconds.
     *
     * @param runConfiguration  Run configuration
     *
     * @return Running time in nanoseconds
     */
    private static long determineRuntimeNs(NBProperties runConfiguration) {

        if (runConfiguration.isPropertyDefined("run_time_s") && runConfiguration.isPropertyDefined("run_time_ns")) {
            throw new PropertyConflictException(runConfiguration, "run_time_s", "run_time_ns");

        } else if (runConfiguration.isPropertyDefined("run_time_s")) {
            return UnitConverter.convertSecondsToNanoseconds(runConfiguration.getDoublePropertyOrFail("run_time_s"));

        } else if (runConfiguration.isPropertyDefined("run_time_ns")) {
            return runConfiguration.getLongPropertyOrFail("run_time_ns");

        } else {
            throw new PropertyMissingException(runConfiguration, "run_time_s");
        }

    }

    /**
     * Generate the infrastructure (network devices, output ports,
     * links and transport layers) of the run.
     *
     * @return  Initializer of the infrastructure
     */
    private static BaseInitializer generateInfrastructure() {

        // Start infrastructure
        System.out.println("\nINFRASTRUCTURE\n==================");

        // 1.1) Generate nodes
        BaseInitializer initializer = new BaseInitializer(
                InfrastructureSelector.selectOutputPortGenerator(),
                InfrastructureSelector.selectNetworkDeviceGenerator(), // graph is created first time here
                InfrastructureSelector.selectLinkGenerator(),
                InfrastructureSelector.selectTransportLayerGenerator()
        );

        // 1.2) Generate the links from the topology between the nodes
        initializer.createInfrastructure();

        // Finished infrastructure
        System.out.println("Finished creating infrastructure.\n");

        return initializer;

    }

    public static void trigger_reconfig() {
        if (Simulator.getConfiguration().getBooleanPropertyOrFail("reconfig")) {
            System.out.println(" reconfiguration trigerred");
            Simulator.getConfiguration().setPropertyOrFail("scenario_topology_file",
                    Simulator.getConfiguration().getPropertyOrFail("reconfig_topo_files").split(" ", 0)[0]);
            System.out.println("new topology file = " + Simulator.getConfiguration().getPropertyOrFail("scenario_topology_file"));
            reManageTopology();
            Simulator.scheduleReconfig();
            Simulator.getBaselineInitializer().blockPortsToBeRemoved();
        }
    }


    /**
     * Populate the routing state.
     *
     * @param idToNetworkDevice     Mapping of identifier to network device
     */
    public static void populateRoutingState(Map<Integer, NetworkDevice> idToNetworkDevice) {

        // Start routing
        System.out.println("ROUTING STATE\n==================");

        // 2.1) Populate the routing tables in the switches uidToNetworkDevice = {HashMap@1161}  size = 9sing the topology defined
        RoutingPopulator populator = RoutingSelector.selectPopulator(idToNetworkDevice);
        populator.populateRoutingTables();
        // Simulator.setRoutingPopulator(populator);

        // Finish routing
        System.out.println("Finished routing state setup.\n");

    }

    /**
     * Plan the traffic.
     *
     * @param durationNs             Running time in nanoseconds
     * @param idToTransportLayer    Mapping from node identifier to transport layer
     */
    private static void planTraffic(long timeFromNow, long durationNs, Map<Integer, TransportLayer> idToTransportLayer, int run_nr) {

        // Start traffic generation
        System.out.println("TRAFFIC\n==================");

        // 3.1) Create flow plan for the simulator
        TrafficPlanner planner = TrafficSelector.selectPlanner(idToTransportLayer, run_nr);
        if (Simulator.getConfiguration().getPropertyOrFail("traffic").equals("real_trace"))
            planner.createPlan(durationNs);
        else
            planner.createPlan(timeFromNow, durationNs);

        // Finish traffic generation
        System.out.println("Finished generating traffic flow starts.\n");

    }

    private static void replanTraffic(long runtimeNs, Map<Integer, TransportLayer> idToTransportLayer, int run_nr) {

        // Start traffic generation
        System.out.println("TRAFFIC REPLANNING\n==================");

        // 3.1) Create flow plan for the simulator
        TrafficPlanner planner = TrafficSelector.selectPlanner(idToTransportLayer, run_nr);
        planner.createPlan(runtimeNs);

        // Finish traffic generation
        System.out.println("Finished generating traffic flow starts.\n");

    }

    /**
     * Manage the topology, meaning that the topology can be extended with servers.
     *
     * It uses the following properties:
     * scenario_topology_file=/path/to/topology.txt
     * scenario_topology_extend_with_servers=regular
     * scenario_topology_extend_servers_per_tl_node=4
     *
     * It will override the scenario_topology_file in the existing configuration.
     */
    private static void manageTopology() {

        // Copy of original topology to the run folder
        SimulationLogger.copyFileToRunFolder(Simulator.getConfiguration().getPropertyOrFail("scenario_topology_file"), "original_topology.txt");

        // Topology extension
        if (Simulator.getConfiguration().isPropertyDefined("scenario_topology_extend_with_servers")) {
            if (Simulator.getConfiguration().getPropertyWithDefault("scenario_topology_extend_with_servers", "").equals("regular")) {

                // Number of servers to add to each transport layer node

                int serversPerNodeToExtendWith = Simulator.getConfiguration().getIntegerPropertyOrFail("scenario_topology_extend_servers_per_tl_node");

                // Extend topology
                new TopologyServerExtender(
                        Simulator.getConfiguration().getTopologyFileNameOrFail(),
                        SimulationLogger.getRunFolderFull() + "/extended_topology.txt"
                ).extendRegular(serversPerNodeToExtendWith);

                // Log info about extension
                SimulationLogger.logInfo("OVERRODE_TOPOLOGY_FILE_WITH_SERVER_EXTENSION", "servers/node=" + serversPerNodeToExtendWith);

            } else {
                throw new PropertyValueInvalidException(Simulator.getConfiguration(), "scenario_topology_extend_with_servers");
            }

            // Override configuration property
            Simulator.getConfiguration().overrideProperty("scenario_topology_file", SimulationLogger.getRunFolderFull() + "/extended_topology.txt");
            SimulationLogger.logInfo("ARG_OVERRIDE_PARAM(scenario_topology_file)", SimulationLogger.getRunFolderFull() + "/extended_topology.txt");

        }

    }

    private static void reManageTopology() {

        // Copy of original topology to the run folder
        SimulationLogger.copyFileToRunFolder(Simulator.getConfiguration().getPropertyOrFail("scenario_topology_file"), "original_topology.txt");

        // Topology extension
        if (Simulator.getConfiguration().isPropertyDefined("scenario_topology_extend_with_servers")) {
            if (Simulator.getConfiguration().getPropertyWithDefault("scenario_topology_extend_with_servers", "").equals("regular")) {

                // Number of servers to add to each transport layer node

                int serversPerNodeToExtendWith = Simulator.getConfiguration().getIntegerPropertyOrFail("scenario_topology_extend_servers_per_tl_node");

                // Extend topology
                new TopologyServerExtender(
                        Simulator.getConfiguration().getTopologyFileNameOrFail(),
                        SimulationLogger.getRunFolderFull() + "/extended_topology.txt"
                ).extendRegular(serversPerNodeToExtendWith);

                // Log info about extension
                SimulationLogger.logInfo("OVERRODE_TOPOLOGY_FILE_WITH_SERVER_EXTENSION", "servers/node=" + serversPerNodeToExtendWith);

            } else {
                throw new PropertyValueInvalidException(Simulator.getConfiguration(), "scenario_topology_extend_with_servers");
            }

            // Override configuration property
            Simulator.getConfiguration().overrideProperty("scenario_topology_file", SimulationLogger.getRunFolderFull() + "/extended_topology.txt");
            SimulationLogger.logInfo("ARG_OVERRIDE_PARAM(scenario_topology_file)", SimulationLogger.getRunFolderFull() + "/extended_topology.txt");

        }

    }

    /**
     * Run a command in the prompt (e.g. to call a python script).
     * Error write output is always shown.
     *
     * @param cmd           Command
     * @param showOutput    Whether to show the normal write output from the command in the console
     */
    public static void runCommand(String cmd, boolean showOutput) {

        Process p;
        try {

            System.out.println("Running command \"" + cmd + "\"...");

            // Start process
            p = Runtime.getRuntime().exec(cmd);

            // Fetch input streams
            BufferedReader stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));
            BufferedReader stdError = new BufferedReader(new InputStreamReader(p.getErrorStream()));

            // Read the output from the command
            String s;
            while ((s = stdInput.readLine()) != null && !showOutput) {
                System.out.println(s);
            }

            // Read any errors from the attempted command
            while ((s = stdError.readLine()) != null) {
                System.out.println(s);
            }

            // Wait for the command thread to be ended
            p.waitFor();
            p.destroy();

            System.out.println("... command has been executed (any output is shown above).");

        } catch (Exception e) {
            throw new RuntimeException("Command failed: " + cmd);
        }

    }

}
