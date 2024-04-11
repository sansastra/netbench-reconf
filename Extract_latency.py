import os
import csv
import scipy.io


# extract latency and fct
def get_results(foldername: str, pd: list, arr_rate: list, result_type: int, combine_csv: int):     # combine_csv = 0, output the fct and latency in terms of different lambda
    predict_label = scipy.io.loadmat('pred_label_64.mat')['pred_label'][0]                          # conbine_csv = 1, output the fct and latency reduction(%) in terms of different clusters with a specific lambda
    # print(predict_label[1])
    latency_cluster_0 = []
    latency_cluster_1 = []
    latency_cluster_2 = []
    latency_cluster_3 = []
    latency_cluster_4 = []
    fct_cluster_0 = []
    fct_cluster_1 = []
    fct_cluster_2 = []
    fct_cluster_3 = []
    fct_cluster_4 = []
    for k in range(len(pd)):
        # entries = os.listdir(foldername + '/')
        # entries.sort()
        # print(entries)
        # arr_rate = [10000, 20000, 40000, 60000, 80000, 100000, 120000]

        listostring = ['results_pd_' + str(pd[k]) + '_' + str(arr_rate[i]) for i in range(len(arr_rate))]
        print(listostring)
        latency = []
        fct = []
        # os.chdir(os.path.abspath(os.getcwd()) + '/' + foldername + '/' + listostring[0])
        for i in range(len(listostring)):
            print(os.path.abspath(os.getcwd()))
            os.chdir(os.path.abspath(os.getcwd()) + '/' + foldername + '/' + listostring[i])
            print(os.path.abspath(os.getcwd()))
            file = open('statistics.log', 'r')
            print('got it')
            lines = file.read().splitlines()
            value = ""
            print(lines[0][0])
            if lines[0][0] == 'T':
                for j in range(len(lines[6])):
                    if j >= 30:
                        value = value + lines[6][j]
            else:
                for j in range(len(lines[8])):
                    if j >= 30:
                        value = value + lines[8][j]

            latency.append(float(value))    # if no need to combine
            os.chdir('../')
            os.chdir('../')
            print(os.path.abspath(os.getcwd()))
            print(value)

        if result_type == 1:
            string = 'all2all'
        else:
            string = 'reconfig'
        print(latency_cluster_3)
        print(predict_label[k])
        if combine_csv == 1:
            if predict_label[k] == 0:
                latency_cluster_0.append(float(value))
            if predict_label[k] == 1:
                latency_cluster_1.append(float(value))
            if predict_label[k] == 2:
                latency_cluster_2.append(float(value))
            if predict_label[k] == 3:
                latency_cluster_3.append(float(value))
            if predict_label[k] == 4:
                latency_cluster_4.append(float(value))
        else:
            with open('latency_' + string + '_' + str(pd[k]) + '.csv', 'w', newline='') as csvfile:
                writer = csv.writer(csvfile)
                # writer.writerows(latency)
                writer.writerows(map(lambda x: [x], latency))
        # print(latency_cluster_3)
        # print(latency_cluster_2)
        ## get the fct
        for i in range(len(listostring)):
            # print(os.path.abspath(os.getcwd()))
            os.chdir(os.path.abspath(os.getcwd()) + '/' + foldername + '/' + listostring[i] + '/' + 'analysis')
            file = open('flow_completion.statistics', 'r')
            lines = file.read().splitlines()
            value = ""
            for j in range(len(lines[0])):
                if j >= 18:
                    value = value + lines[0][j]
            fct.append(float(value))   # if no need to combine
            os.chdir('../')
            os.chdir('../')
            os.chdir('../')
            print(value)

        if combine_csv == 1:
            if predict_label[k] == 0:
                fct_cluster_0.append(float(value))
            if predict_label[k] == 1:
                fct_cluster_1.append(float(value))
            if predict_label[k] == 2:
                fct_cluster_2.append(float(value))
            if predict_label[k] == 3:
                fct_cluster_3.append(float(value))
            if predict_label[k] == 4:
                fct_cluster_4.append(float(value))
        else:
            with open('fct_' + string + '_' + str(pd[k]) + '.csv', 'w', newline='') as csvfile:
                writer = csv.writer(csvfile)
                #  writer.writerows(latency)
                writer.writerows(map(lambda x: [x], fct))

    if combine_csv == 1:
        with open('latency_cluster_0_' + string + '.csv', 'w', newline='') as csvfile:
            writer = csv.writer(csvfile)
            # writer.writerows(latency)
            writer.writerows(map(lambda x: [x], latency_cluster_0))
        with open('latency_cluster_1_' + string + '.csv', 'w', newline='') as csvfile:
            writer = csv.writer(csvfile)
            # writer.writerows(latency)
            writer.writerows(map(lambda x: [x], latency_cluster_1))
        with open('latency_cluster_2_' + string + '.csv', 'w', newline='') as csvfile:
            writer = csv.writer(csvfile)
            # writer.writerows(latency)
            writer.writerows(map(lambda x: [x], latency_cluster_2))
        with open('latency_cluster_3_' + string + '.csv', 'w', newline='') as csvfile:
            writer = csv.writer(csvfile)
            # writer.writerows(latency)
            writer.writerows(map(lambda x: [x], latency_cluster_3))
        with open('latency_cluster_4_' + string + '.csv', 'w', newline='') as csvfile:
            writer = csv.writer(csvfile)
            # writer.writerows(latency)
            writer.writerows(map(lambda x: [x], latency_cluster_4))
        with open('fct_cluster_0' + string + '.csv', 'w', newline='') as csvfile:
            writer = csv.writer(csvfile)
            # writer.writerows(latency)
            writer.writerows(map(lambda x: [x], fct_cluster_0))
        with open('fct_cluster_1' + string + '.csv', 'w', newline='') as csvfile:
            writer = csv.writer(csvfile)
            # writer.writerows(latency)
            writer.writerows(map(lambda x: [x], fct_cluster_1))
        with open('fct_cluster_2' + string + '.csv', 'w', newline='') as csvfile:
            writer = csv.writer(csvfile)
            # writer.writerows(latency)
            writer.writerows(map(lambda x: [x], fct_cluster_2))
        with open('fct_cluster_3' + string + '.csv', 'w', newline='') as csvfile:
            writer = csv.writer(csvfile)
            # writer.writerows(latency)
            writer.writerows(map(lambda x: [x], fct_cluster_3))
        with open('fct_cluster_4' + string + '.csv', 'w', newline='') as csvfile:
            writer = csv.writer(csvfile)
            # writer.writerows(latency)
            writer.writerows(map(lambda x: [x], fct_cluster_4))


def get_all_port_utilization(foldername: str, pd: list, arr_rate: str):    # throughput comparison: all2all and reconfig with cluster improvement (%)
    predict_label = scipy.io.loadmat('pred_label_64.mat')['pred_label'][0]
    util_cluster_0 = []
    util_cluster_1 = []
    util_cluster_2 = []
    util_cluster_3 = []
    util_cluster_4 = []

    for k in range(len(pd)):
        # entries = os.listdir(foldername + '/')
        # entries.sort()
        # print(entries)
        # arr_rate = [10000, 20000, 40000, 60000, 80000, 100000, 120000]
        util = []
        listostring = ['results_pd_' + str(pd[k]) + '_' + arr_rate]
        print(listostring)
        os.chdir(
        os.path.abspath(os.getcwd()) + '/' + foldername + '/' + listostring[0] + '/' + 'analysis')
        file = open('port_utilization.statistics', 'r')
        lines = file.read().splitlines()
        value = ""
        for j in range(len(lines[0])):
            if j >= 28:
                value = value + lines[0][j]
                print(value)
        # util.append(float(value))
        os.chdir('../')
        os.chdir('../')
        os.chdir('../')
        # print(util)

        if predict_label[k] == 0:
            util_cluster_0.append(float(value))
        if predict_label[k] == 1:
            util_cluster_1.append(float(value))
        if predict_label[k] == 2:
            util_cluster_2.append(float(value))
        if predict_label[k] == 3:
            util_cluster_3.append(float(value))
        if predict_label[k] == 4:
            util_cluster_4.append(float(value))

    with open('util_cluster_0.csv', 'w', newline='') as csvfile:
        writer = csv.writer(csvfile)
        # writer.writerows(latency)
        writer.writerows(map(lambda x: [x], util_cluster_0))
    with open('util_cluster_1.csv', 'w', newline='') as csvfile:
        writer = csv.writer(csvfile)
        # writer.writerows(latency)
        writer.writerows(map(lambda x: [x], util_cluster_1))
    with open('util_cluster_2.csv', 'w', newline='') as csvfile:
        writer = csv.writer(csvfile)
        # writer.writerows(latency)
        writer.writerows(map(lambda x: [x], util_cluster_2))
    with open('util_cluster_3.csv', 'w', newline='') as csvfile:
        writer = csv.writer(csvfile)
        # writer.writerows(latency)
        writer.writerows(map(lambda x: [x], util_cluster_3))
    with open('util_cluster_4.csv', 'w', newline='') as csvfile:
        writer = csv.writer(csvfile)
        # writer.writerows(latency)
        writer.writerows(map(lambda x: [x], util_cluster_4))

def get_all_results(foldername: str, pd: list, arr_rate: str, result_type: int):   # latency, fct, throughput comparison: all2all, SB, reconfig with whole series improvement(%)
    latency = []
    fct = []
    util = []
    if result_type == 1:
        string = 'all2all'
    if result_type == 2:
        string = 'SB'
    if result_type == 3:
        string = 'reconfig'

    for k in range(len(pd)):
        # entries = os.listdir(foldername + '/')
        # entries.sort()
        # print(entries)
        # arr_rate = [10000, 20000, 40000, 60000, 80000, 100000, 120000]

        listostring = ['results_pd_' + str(pd[k]) + '_' + arr_rate]
        print(listostring)

        # os.chdir(os.path.abspath(os.getcwd()) + '/' + foldername + '/' + listostring[0])

        print(os.path.abspath(os.getcwd()))
        os.chdir(os.path.abspath(os.getcwd()) + '/' + foldername + '/' + listostring[0])
        print(os.path.abspath(os.getcwd()))
        file = open('statistics.log', 'r')
        print('got it')
        lines = file.read().splitlines()
        value = ""
        print(lines[0][0])
        if lines[0][0] == 'T':
            for j in range(len(lines[6])):
                if j >= 30:
                    value = value + lines[6][j]
        else:
            for j in range(len(lines[8])):
                if j >= 30:
                    value = value + lines[8][j]

        latency.append(float(value))    # if no need to combine
        os.chdir('../')
        os.chdir('../')
        print(os.path.abspath(os.getcwd()))
        print(value)

        # print(latency_cluster_2)
        ## get the fct
        for i in range(len(listostring)):
            # print(os.path.abspath(os.getcwd()))
            os.chdir(os.path.abspath(os.getcwd()) + '/' + foldername + '/' + listostring[i] + '/' + 'analysis')
            file = open('flow_completion.statistics', 'r')
            lines = file.read().splitlines()
            value = ""
            for j in range(len(lines[0])):
                if j >= 18:
                    value = value + lines[0][j]
            fct.append(float(value))   # if no need to combine
            os.chdir('../')
            os.chdir('../')
            os.chdir('../')
            print(value)

       # get all port utilization
        for i in range(len(listostring)):
            # print(os.path.abspath(os.getcwd()))
            os.chdir(os.path.abspath(os.getcwd()) + '/' + foldername + '/' + listostring[i] + '/' + 'analysis')
            file = open('port_utilization.statistics', 'r')
            lines = file.read().splitlines()
            value = ""
            for j in range(len(lines[0])):
                if j >= 28:
                    value = value + lines[0][j]
                    print(value)
            util.append(float(value))
            os.chdir('../')
            os.chdir('../')
            os.chdir('../')

    with open('latency_' + string + '_' + str(pd[k]) + '.csv', 'w', newline='') as csvfile:
        writer = csv.writer(csvfile)
        writer.writerows(map(lambda x: [x], latency))
    with open('fct_' + string + '_' + str(pd[k]) + '.csv', 'w', newline='') as csvfile:
        writer = csv.writer(csvfile)
        writer.writerows(map(lambda x: [x], fct))
    with open('util_' + string + '_' + str(pd[k]) + '.csv', 'w', newline='') as csvfile:
        writer = csv.writer(csvfile)
        writer.writerows(map(lambda x: [x], util))


get_results('all2all_64Tors_results', [2], [10000, 20000, 30000, 40000, 50000, 60000, 70000, 80000, 90000, 100000], 1, 0)
get_results('reconfig_64Tors_results', [2], [10000, 20000, 30000, 40000, 50000, 60000, 70000, 80000, 90000, 100000], 2, 0)

get_results('all2all_64Tors_allresults', [i+1 for i in range(150)], [60000], 1, 1)
get_results('reconfig_64Tors_allresults', [i+1 for i in range(150)], [60000], 2, 1)


get_all_port_utilization('all2all_16Tors_UtilResults', [i+1 for i in range(150)], '60000')


predict_label = scipy.io.loadmat('pred_label_64.mat')['pred_label'][0]
arr_rate = [None] * len(predict_label)
for i in range(len(predict_label)):
    if predict_label[i] == 0:
        arr_rate[i] = '140000'
    if predict_label[i] == 3:
        arr_rate[i] = '50000'
    if predict_label[i] == 4:
        arr_rate[i] = '130000'
    if predict_label[i] == 2:  # default, need to change
        arr_rate[i] = '50000'
    if predict_label[i] == 1:
        arr_rate[i] = '90000'


for i in range(len(arr_rate)):
    print("'"+arr_rate[i]+"'"+" ",end="", sep="")

for i in range(len(predict_label)):
    print("'"+str(predict_label[i])+"'"+" ",end="", sep="")




