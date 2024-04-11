reconfig="false true"
for i in "$reconfig"; do
    java -jar -ea netbench_reconfig_1FSR_ondm.jar reconfigecmp_mpod_v1.properties reconfig=$reconfig  run_folder_name=mpd16_reconfig_$reconfig
done
