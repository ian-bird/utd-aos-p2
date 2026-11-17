from dataclasses import dataclass
from datetime import datetime
import csv

@dataclass
class Row:
    node_id: int
    event: str
    logger_time: datetime
    node_time: datetime

def read_csv(path: str) -> list[Row]:
    rows = []
    with open(path, mode='r', newline='', encoding='utf-8') as file:
        reader = csv.reader(file)
        header = next(reader)  # Skip the header row!
        
        for row in reader:

            row = [x.strip() for x in row]  # Strip whitespace from each column
            # print(row)
            rows.append(Row(int(row[0]),
                            row[1], 
                            datetime.fromisoformat(row[2]),
                            datetime.fromisoformat(row[3])))
            # print(rows[-1])
            

    print(f"Read {len(rows)} rows from {path}")
    return rows


if __name__ == "__main__":
    import sys
    if len(sys.argv) != 2:
        print("Usage: python read_csv.py <path_to_csv>")
        sys.exit(1)

    path = sys.argv[1]
    rows = read_csv(path)