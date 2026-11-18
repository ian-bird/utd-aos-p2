from read_csv import read_csv


def main():
    # rows = read_csv("../logger_outputs/beep.csv")
    rows = read_csv("../logger_outputs/test_check_me.csv")

    # Each row should be:
    # X enter t1
    # X exit t2
    # Y enter t3
    # Y exit t4
    # -- where t1 < t2 < t3 < t4!
    print("Checking mutual exclusion...")

    # Sort all rows by logger_time first, since their order in the logfile is not necessarily chronological
    rows = sorted(rows, key=lambda row: row.logger_time)

    for i in range(0, len(rows) - 1, 2):
        r1, r2 = rows[i : i + 2]

        print(
            f"{r1.node_id} {r1.event} -> {r2.node_id} {r2.event} at {r1.logger_time} and {r2.logger_time}"
        )

        conditions = [
            r1.event == "enter" and r2.event == "exit",
            r1.node_id == r2.node_id,
            r1.logger_time < r2.logger_time,
            r1.node_time < r2.node_time,
        ]

        if not all(conditions):
            print(f"Mutual exclusion violated:\n{r1=}\n{r2=}")
            return


if __name__ == "__main__":
    main()
