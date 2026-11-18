import csv
from dataclasses import dataclass
from datetime import datetime
from pathlib import Path


@dataclass
class Row:
    node_id: int
    event: str
    logger_time: datetime
    node_time: datetime


@dataclass
class CSVData:
    rows: list[Row]
    msg_count: list[int]


def read_csv(path: str | Path) -> CSVData:
    rows = []
    msg_counts = []

    with open(path, mode="r", newline="", encoding="utf-8") as file:
        reader = csv.reader(file)
        _header = next(reader)  # Skip the header row!

        for row in reader:
            row = [x.strip() for x in row]  # Strip whitespace from each column
            if "messageCount" == row[1]:
                msg_counts.append(int(row[3]))
            else:
                rows.append(
                    Row(
                        int(row[0]),
                        row[1],
                        datetime.fromisoformat(row[2]),
                        datetime.fromisoformat(row[3]),
                    )
                )
    print(f"Read {len(rows)} rows from {path}")
    # print("Message counts:", msg_counts)
    return CSVData(rows, msg_counts)


if __name__ == "__main__":
    import sys

    if len(sys.argv) != 2:
        print("Usage: python read_csv.py <path_to_csv>")
        sys.exit(1)

    path = sys.argv[1]
    rows = read_csv(path)
