from read_csv import read_csv

def main():
    # rows = read_csv("../logger_outputs/beep.csv")
    rows = read_csv("../logger_outputs/test_check_me.csv")
    # Each row should be:
    # X enter t1
    # X exit t2
    # Y enter t3
    # Y exit t4
    # where t1 < t2, etc...
    print("Checking mutual exclusion...")
    # print(rows[0:2])

    for i in range(0, len(rows) - 1, 2):
        r1 = rows[i]
        r2 = rows[i + 1]

        print(f"{r1.node_id} {r1.event} -> {r2.node_id} {r2.event} at {r1.logger_time} and {r2.logger_time}")
        if not (r1.event == "enter" and r2.event == "exit" and r1.node_id == r2.node_id):
            print(f"Mutual exclusion violated: {r1.node_id} and {r2.node_id} both entered at {r1.logger_time} and {r2.logger_time}")
            return

if __name__ == "__main__":
    main()
