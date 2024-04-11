# -*- coding: utf-8 -*-
# @Time    : 20-09-2021
# @Author  : sansingh
import numpy as np
import sys
# from graph_topology import get_topo_reconfig
import csv
import os
import matplotlib.pyplot as plt


##################################
# Setup
#

print("NetBench python analysis tool v0.01")

# Usage print
def print_usage():
    print("Usage: python3 analyze.py /path/to/run/folder")

# Check length of arguments
if len(sys.argv) != 2:
    print("Number of arguments must be exactly two: analyze.py and /path/to/run/folder.")
    print_usage()
    exit()

# Check run folder path given as first argument
run_folder_path = sys.argv[1]
if not os.path.isdir(run_folder_path):
    print("The run folder path does not exist: " + run_folder_path)
    print_usage()
    exit()

# Create analysis folder
analysis_folder_path = run_folder_path + '/reconfig_topo'
if not os.path.exists(analysis_folder_path):
    os.makedirs(analysis_folder_path)

def get_topo_reconfig(traffic_matrix, num_port, wave_capacity):
    num_tors = traffic_matrix.shape[0]
    margin = 0.05 # capacity on a link shouldn 't be less than the margin, also with a unit of giga-packet per second, which means when a channel is fully loaded, the average packet latency would be 1/margin ns

    num_inport = num_port * np.ones(num_tors)
    num_outport = num_port * np.ones(num_tors)
    # determine the topology
    weight_matrix = traffic_matrix.copy()
    topology = np.zeros(shape=(num_tors, num_tors))
    # tp = 1
    # te = 1
    record_node = np.zeros(shape=(num_tors, num_tors))
    ii = 0
    weight_matrix[weight_matrix == 0] = -sys.maxsize
    while (sum(num_inport>0) > 1 and sum(num_outport>0) > 1):
    # for src_node in range(num_tors):
    #     for ii in range(num_port):
        if np.max(np.max(weight_matrix)) == -sys.maxsize:
            break
        src_node = int(np.argmax(weight_matrix)/num_tors)
        # src_node

        if num_outport[src_node] > 0 and num_inport[src_node] > 0:
            w1 = weight_matrix[src_node,:]*num_inport #
            w1[w1==0] = -sys.maxsize
            w2 = np.transpose(weight_matrix[:, src_node])* num_outport
            w2[w2 == 0] = -sys.maxsize
            # w1 = weight_matrix[src_node,:]
            # w2 = weight_matrix[:, src_node]
            if np.max(w1) >= np.max(w2):
                print("here_1!")
                w = w1.copy()
                w[num_inport == 0] = -sys.maxsize # if num_inport is equal to zero for a node, then set the weight function as -inf
                w[src_node] = -sys.maxsize # the weight function for the source node is also set as -inf
                v = np.max(w) # get the weight function the largest value and index[dst_node]
                dst_node = np.argmax(w)

                if v <= 0 and v != -sys.maxsize:
                    w = weight_matrix[src_node, :].copy()
                    w[num_inport != 0] = w[num_inport != 0] / num_inport[num_inport != 0]
                    w[num_inport == 0] = -sys.maxsize
                    w[src_node] = -sys.maxsize
                    w[w == 0] = -sys.maxsize
                    v = np.max(w)
                    dst_node = np.argmax(w)
                    if record_node[src_node, dst_node] == 0:
                        record_node[src_node, dst_node] = 1
                        record_node[dst_node, src_node] = 1

                    print("v<=0")
                    # continue

                if v == -sys.maxsize:
                    # disp["v==-inf, continue"]
                    break

                if record_node[src_node, dst_node] == 0:
                    record_node[src_node, dst_node] = 1
                    record_node[dst_node, src_node] = 1

                num_inport[dst_node] = num_inport[dst_node] - 1
                num_outport[dst_node] = num_outport[dst_node] - 1
                num_outport[src_node] = num_outport[src_node] - 1
                num_inport[src_node] = num_inport[src_node] - 1
                # num_inport
                # num_outport
                topology[src_node, dst_node] = topology[src_node, dst_node] + 1
                topology[dst_node, src_node] = topology[dst_node, src_node] + 1
                # topology
                weight_matrix[src_node, dst_node] = weight_matrix[src_node, dst_node] - [wave_capacity - margin]
                weight_matrix[dst_node, src_node] = weight_matrix[dst_node, src_node] - [wave_capacity - margin]
                # te = src_node

            else:
                # if record_node[src_node, te] == 0 & record_node[te, src_node] == 0
                w = w2.copy()
                # disp["here_2!"]
                dst_node = src_node
                w[dst_node] = -sys.maxsize
                w[num_outport == 0] = -sys.maxsize # if num_inport is equal to zero for a node, then set the weight function as -inf
                v = np.max(w) # get the weight function the largest value and index[dst_node]
                temp = np.argmax(w)
                # temp
                # v
                if v <= 0 and v != -sys.maxsize:
                    w = weight_matrix[:, dst_node].copy()
                    w[num_outport != 0] = w[num_outport != 0]/ num_outport[num_outport != 0]
                    w[num_outport == 0] = -sys.maxsize
                    w[dst_node] = -sys.maxsize
                    w[w == 0] = -sys.maxsize
                    v = np.max(w)
                    temp = np.argmax(w)
                    if record_node[temp, dst_node] == 0:
                        record_node[temp, dst_node] = 1
                        record_node[dst_node, temp] = 1

                if v == -sys.maxsize:
                    print("v==-inf, continue")
                    continue

                if record_node[temp, dst_node] == 0:
                    record_node[temp, dst_node] = 1
                    record_node[dst_node, temp] = 1


                topology[temp, dst_node] = topology[temp, dst_node] + 1
                topology[dst_node, temp] = topology[dst_node, temp] + 1

                num_inport[dst_node] = num_inport[dst_node] - 1
                num_outport[dst_node] = num_outport[dst_node] - 1
                num_outport[temp] = num_outport[temp] - 1
                num_inport[temp] = num_inport[temp] - 1
                weight_matrix[temp, dst_node] = weight_matrix[temp, dst_node] - (wave_capacity - margin)
                weight_matrix[dst_node, temp] = weight_matrix[dst_node, temp] - (wave_capacity - margin)
                # tp = dst_node
        elif num_outport[src_node]==0:
            weight_matrix[src_node,:] = -sys.maxsize
            weight_matrix[ :,src_node] = -sys.maxsize
        ii += 1
        if ii == num_port:
            ii = 0
    return topology
# tm = np.array([[0.0, 10.0, 10.0, 10.0], [10.0, 0.0, 10.0, 10.0], [10.0, 10.0, 0.0, 10.0], [10.0, 10.0, 10.0, 0.0]])
# tm = np.array([[0.0, 0.01, 0.4, 0.0], [0.01, 0.0, 0.01, 0.01], [0.4, 0.01, 0.0, 0.07], [0.0, 0.01, 0.07, 0.0]])
dst_path = "/home/sandeep/work/netbench_reconfig/reconfig_topo/"
# src_path = "/home/sandeep/work/Python_work/resources/"
src_path = "/home/sandeep/work/netbench_reconfig/pair_distribution/"
with open(src_path+"pair_data4.csv", "r") as f:
    tm = pd.read_csv(f, header=None, dtype=float) #f.read()
    tm = np.array(tm)
    tm = tm/np.sum(np.sum(tm))
num_tors = tm.shape[0]
num_port = num_tors #- 1
wave_capacity = 10 # Gbps
nr_edges = np.sum(np.sum(tm> 0))

# tm_g_0 = reshape(tm, num_tors, num_tors).'
topo = get_topo_reconfig(tm, num_port, wave_capacity)
topo1 = np.ones(shape=(num_tors, num_tors))
np.fill_diagonal(topo1, 0)

# output to the link file
f = open(dst_path+'topo_4.topology','w') # for all2all topo=topo1, 'topo_a2a.topology'
f.write('# Reconfigured topology \n\n')
f.write('# Details \n')
f.write('|V|='+ str(num_tors)+'\n')
f.write('|E|='+ str(nr_edges) + '\n')
f.write('ToRs=set(')
for i in range(num_tors):
    if i != num_tors-1:
        f.write(str(i) + ",")
    elif i == num_tors-1:
            f.write(str(i) + ')')

f.write('\n')

f.write('Servers=set(')
for i in range(num_tors):
    if i != num_tors-1:
        f.write(str(i) + ',')
    elif i == num_tors-1:
        f.write(str(i) + ')')


f.write('\n')
f.write('Switches=set() \n\n')
f.write('# Links \n')

for i in range(num_tors):
    for j in range(num_tors):
        if topo1[i,j] != 0 and i < j:
            f.write(str(i) + ' ' + str(j) + ' ' + str(int(topo[i, j]*10+topo1[i, j]*10)) + '\n')
            f.write(str(j) + ' ' + str(i) + ' ' + str(int(topo[j, i]*10+topo1[j, i]*10)) + '\n')
f.close()