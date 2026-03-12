import csv
import collections
import matplotlib.pyplot as plt
import matplotlib.ticker as ticker
import numpy as np
import sys
import os
# I renamed it pablo-results.csv afterwards
CSV_PATH = "results.csv" if len(sys.argv) < 2 else sys.argv[1]

# model key -> (display label, color, marker, linestyle)
MODEL_STYLE = {
    "fixed-pool-8":    ("Fixed Thread Pool",  "#2166ac", "o", "solid"),
    "work-stealing-8": ("Work-Stealing Pool", "#4dac26", "s", "dashed"),
    "virtual-threads": ("Virtual Threads",    "#d01c8b", "^", "solid"),
}

IO_WORKLOAD = "io-sleep-10ms-cpu5000"

# Accumulate total times per (model, taskCount)
# Each key maps to a list of totalTime values across all trials/runs
data = collections.defaultdict(list)

with open(CSV_PATH, newline="") as f:
    reader = csv.reader(f)
    for row in reader:
        if len(row) < 3:
            continue
        label = row[0].strip()
        try:
            task_count = int(row[1].strip())
            total_time = float(row[2].strip())
        except ValueError:
            continue

        # Parse model and workload from label
        # Format: <model>-<workload>
        # model is one of: fixed-pool-8, work-stealing-8, virtual-threads
        # workload contains the rest
        matched_model = None
        matched_workload = None
        for model_key in MODEL_STYLE:
            prefix = model_key + "-"
            if label.startswith(prefix):
                matched_model = model_key
                matched_workload = label[len(prefix):]
                break

        if matched_model is None or matched_workload != IO_WORKLOAD:
            continue

        data[(matched_model, task_count)].append(total_time)

if not data:
    print("No IO data found. Check that the CSV path is correct and the file contains io-sleep-10ms-cpu5000 rows.")
    sys.exit(1)

# Compute averages
task_counts = sorted(set(tc for (_, tc) in data.keys()))
avg = {}
for (model, tc), times in data.items():
    avg[(model, tc)] = np.mean(times)

fig, ax = plt.subplots(figsize=(6, 4))

for model_key, (label, color, marker, linestyle) in MODEL_STYLE.items():
    ys = []
    xs = []
    for tc in task_counts:
        key = (model_key, tc)
        if key in avg:
            xs.append(tc)
            ys.append(avg[key])
    if xs:
        ax.plot(xs, ys, marker=marker, color=color, linewidth=2,
                linestyle=linestyle, markersize=7, label=label)

ax.set_xlabel("Number of Tasks", fontsize=11)
ax.set_ylabel("Total Wall-Clock Time (s)", fontsize=11)
ax.set_title("Simulated IO Workload: Total Time vs. Task Count\n"
             "(10 ms sleep + 5,000 CPU iters per task, 8-core machine)",
             fontsize=10)
ax.set_xticks(task_counts)
ax.xaxis.set_major_formatter(ticker.FuncFormatter(lambda x, _: f"{int(x):,}"))
ax.yaxis.set_major_formatter(ticker.FuncFormatter(lambda y, _: f"{y:.3f}"))
ax.grid(True, linestyle="--", alpha=0.5)
ax.set_ylim(bottom=0)

# Annotation: arrow from virtual threads at 200 tasks up to pool models at 200 tasks
vt_y = avg.get(("virtual-threads", 200), None)
fp_y = avg.get(("fixed-pool-8", 200), None)
ws_y = avg.get(("work-stealing-8", 200), None)

if vt_y is not None and fp_y is not None and ws_y is not None:
    pool_y = (fp_y + ws_y) / 2
    mid_y = (vt_y + pool_y) / 2
    ax.annotate(
        "",
        xy=(200, pool_y),
        xytext=(200, vt_y),
        arrowprops=dict(
            arrowstyle="<->",
            color="#333333",
            lw=1.5,
        ),
    )
    speedup = pool_y / vt_y
    ax.text(
        207, mid_y,
        f"~{speedup:.0f}x slower",
        fontsize=9,
        color="#333333",
        va="center",
    )

ax.legend(fontsize=10)
plt.tight_layout()

out_path = os.path.join(os.path.dirname(CSV_PATH), "io_simulated_total_time.png")
plt.savefig(out_path, dpi=150)
print(f"Saved to {out_path}")
plt.show()