# uv run graph_from_logs.py /home/josh/code/advanced-os/utd-aos-p2/project/p2/logger_outputs_req minimal-2nodes
import sys
from datetime import datetime
from pathlib import Path

import matplotlib.pyplot as plt
from read_csv import CSVData, read_csv


def average(l: list):
    return sum(l) / len(l)


# Directory where the CSVs are located
csvs_location = (
    sys.argv[1] if len(sys.argv) > 1 else "../logger_outputs/"
)  # this might be ../project/p2/logger_outputs !!
# /home/josh/code/advanced-os/utd-aos-p2/project/p2/logger_outputs
csvs_location = Path(csvs_location)

# Name of the input file for which we want to compare logs
input_file_name = sys.argv[2] if len(sys.argv) > 2 else "minimal-2nodes"

# INSTRUCTIONS SAID: Experimental Evaluation:
# Evaluate the performance of your implementation with respect to
#   message complexity, response time and system throughput
# using experiments for various values of system parameters,
#   namely n, d and c.
# Display the results of your experiments by plotting appropriate graphs.
# Note that each point in the graph should be obtained by averaging over multiple runs.

# Get all CSVs in a given logger_outputs directory,
# group them by the title (which would contain Maekawa1 or Maekawa2),
# read their CSVs and collect the metrics (MaxIdleTime, AvgIdleTime, %ofTimeInCS) for each group,
# then graph.

files = [
    p
    for p in csvs_location.iterdir()
    # if p.is_file()  # ignore subdirs
    # and input_file_name
    # in p.as_posix()  # Only logs that correspond to the same inputfile
]
csv_datas = [read_csv(p) for p in files]

# The CSV logs will have entries out of order (chronologically speaking). Sort them by logger time:
# rowses = [sorted(rows, key=lambda row: row.rows.logger_time) for rows in rowses]
csv_datas = [
    CSVData(sorted(csv_data.rows, key=lambda row: row.logger_time), csv_data.msg_count)
    for csv_data in csv_datas
]

# Find n, d, c based on the file name. This will be used in the graph title
# logger_outputs/2026-11-17T03:55:07.343700Z_n=9_d=20_c=10_Maekawa1_sample-input_log.csv
params_info = (
    files[0]
    .name[files[0].name.index("_") + 1 : files[0].name.index("_log.csv")]
    .replace("_", " ")
)

# MC is just the total num of msgs for that whole run
# RTs are the time between requestion a lock and entering CS
# ST is the total number of CSs per second
#   (... Maybe technically it'd be nicer to scale it by ST/c or something since that's the CS execution time?)

# Just calc the average MC, RT, ST over all runs
mcs, avg_rts, sts = [], [], []
for csv_data in csv_datas:
    print()
    rows = csv_data.rows
    # Find MC:
    print(f"Memory address of csv_data: {id(csv_data)}")
    print(f"Message counts: {csv_data.msg_count}")
    mc = sum(csv_data.msg_count) # Let the total number of msgs be our metric for message complexity.

    # Find RT:
    rts = []
    # for i in range(1, len(rows) - 2):
    #     rt = rows[i + 1].logger_time - rows[i].logger_time

    # rts.append(rt.total_seconds() * 1000)
    # Keep a map of each node to its last request time, and when we see a "request" event, we can calculate the RT for that node.
    node_lastreqs: dict[int, datetime] = {}

    for row in rows:
        if row.event == "request":
            node_lastreqs[row.node_id] = row.logger_time
        elif (
            row.event == "enter"
        ):  # and row.node_id in node_lastreqs: (idk if you need this second half)
            entertime = row.logger_time
            reqtime = node_lastreqs[row.node_id]
            rt = entertime - reqtime
            rts.append(rt.total_seconds() * 1000)
            del node_lastreqs[row.node_id]

    avg_rt = average(rts)

    # Find ST:
    # The number of rows is 2 * number of CSes
    # so ST could be, (nrows/2) / ("last row's time" - "first row's time")
    num_cses = len(rows) // 2
    total_time = rows[-1].logger_time - rows[0].logger_time
    total_time_ms = total_time.total_seconds() * 1000
    st = num_cses / total_time_ms
    print(f"{num_cses=} {total_time_ms=} => {st=} critical sections/millisecond")

    mcs.append(mc)
    avg_rts.append(avg_rt)
    sts.append(st)

avg_mc, avg_avg_rt, avg_st = average(mcs), average(avg_rts), average(sts)
min_mc, min_avg_rt, min_st = min(mcs), min(avg_rts), min(sts)
max_mc, max_avg_rt, max_st = max(mcs), max(avg_rts), max(sts)

print(f"\n{avg_mc, avg_avg_rt, avg_st=}")
print(f"{min_mc, min_avg_rt, min_st=}")
print(f"{max_mc, max_avg_rt, max_st=}")

fig, (ax_mc, ax_rt, ax_st) = plt.subplots(1, 3, layout="constrained")
fig.suptitle("Maekawa with " + params_info)

import random
# ax_mc.margins(y=0.2)
# ax_rt.margins(y=0.2)
# ax_st.margins(y=0.2)
ax_mc.margins(y=random.uniform(0.1, 0.5))
ax_rt.margins(y=random.uniform(0.1, 0.5))
ax_st.margins(y=random.uniform(0.1, 0.5))
# ax_mc.set_ylim(0, 200)
# ax_rt.set_ylim(0, 20)
# ax_st.set_ylim(0, 0.2)

# fig.set_size_inches(20, 10)

# Graph the min, avg, max for each metric:
ax_mc.bar("Min Message Complexity", min_mc)
ax_mc.bar("Avg Message Complexity", avg_mc)
ax_mc.bar("Max Message Complexity", max_mc)

ax_rt.bar("Min Avg Response Time (ms)", min_avg_rt)
ax_rt.bar("Avg Avg Response Time (ms)", avg_avg_rt)
ax_rt.bar("Max Avg Response Time (ms)", max_avg_rt)

ax_st.bar("Min System Throughput (kHz)", min_st)
ax_st.bar("Avg System Throughput (kHz)", avg_st)
ax_st.bar("Max System Throughput (kHz)", max_st)

# Add ylabels for each subplot:
ax_mc.set_ylabel("Message Complexity (msgs)")
ax_rt.set_ylabel("Response Time (ms)")
ax_st.set_ylabel("System Throughput (CSes/ms aka kHz)")

# Add bar labels for all 9 bars:
for ax in [ax_mc, ax_rt, ax_st]:
    for bar in ax.patches:
        ax.annotate(
            f"{bar.get_height():.2f}",
            (bar.get_x() + bar.get_width() / 2, bar.get_height()),
            ha="center",
            va="bottom",
        )

# add a legend:
ax_mc.legend(["Min", "Avg", "Max"], loc="upper right")
ax_rt.legend(["Min", "Avg", "Max"], loc="upper right")
ax_st.legend(["Min", "Avg", "Max"], loc="upper right")

plt.show()
