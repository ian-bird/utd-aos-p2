import sys
import matplotlib.pyplot as plt
from read_csv import read_csv


# Directory where the CSVs are located
csvs_location = sys.argv[1] if len(sys.argv) > 1 else "./logger_outputs/"

# INSTRUCTIONS SAID: Experimental Evaluation:
# Evaluate the performance of your implementation with respect to
#   message complexity, response time and system throughput
# using experiments for various values of system parameters,
#   namely n, d and c.
# Display the results of your experiments by plotting appropriate graphs.
# Note that each point in the graph should be obtained by averaging over multiple runs.

rows = read_csv("../logger_outputs/beep.csv")



# species = ("Adelie", "Chinstrap", "Gentoo")
# metrics = ("MaxIdleTime", "AvgIdleTime", "%ofTimeInCS")
# penguin_means = {
#     'Bill Depth': (18.35, 18.43, 14.98),
#     'Bill Length': (38.79, 48.83, 47.50),
#     'Flipper Length': (189.95, 195.82, 217.19),
# }

# fig, ax = plt.subplots(layout='constrained')

# res = ax.grouped_bar(penguin_means, tick_labels=species, group_spacing=1)
# for container in res.bar_containers:
#     ax.bar_label(container, padding=3)

# # Add some text for labels, title, etc.
# ax.set_ylabel('Length (mm)')
# ax.set_title('Penguin attributes by species')
# ax.legend(loc='upper left', ncols=3)
# ax.set_ylim(0, 250)

plt.show()




# Plot:
# - Longest idle time
#     - (Idle time is time spent where nobody was using the CS)
#     - For idle times, compare logger node times from "0 exited" to "1 entered" bc that's the more accurate version for that
# - Avg idle time
# - \% of time spent in CS
#     - For CS times, compare the node's reported enter and exit times (since network quality could change a bit between enter and exit msgs)
# (Perhaps compare MaekawaStrategies side by side ?? -- Maybe by taking in 2 or more logfile paths, compare them)

# |                    2
# |  1                 2            1
# |  1_2             1_2            1_2
# |  1_2             1_2            1_2
# |__1_2_____________1_2____________1_2______
# MaxIdleTime    AvgIdleTime     %ofTimeInCS

