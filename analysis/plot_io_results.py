from pathlib import Path
import csv
import statistics

import matplotlib.pyplot as plt


ROOT = Path(__file__).resolve().parents[1]
INPUT_CSV = ROOT / "results" / "results-io-raw.csv"
OUT_DIR = ROOT / "analysis"


def io_label(bytes_value: int) -> str:
    kib = 1024
    mib = 1024 * 1024
    if bytes_value % mib == 0:
        return f"{bytes_value // mib}MB"
    return f"{bytes_value // kib}KB"


STYLE = {
    "fixed": {"color": "#1f77b4", "linewidth": 2.0, "alpha": 0.9, "zorder": 2},
    "work": {"color": "#2ca02c", "linewidth": 2.0, "alpha": 0.9, "zorder": 2},
    "virtual": {"color": "#d62728", "linewidth": 2.8, "alpha": 1.0, "zorder": 3},
    "default": {"color": "#555555", "linewidth": 2.0, "alpha": 0.9, "zorder": 1},
}


def model_family(model: str) -> str:
    if model.startswith("fixed-pool"):
        return "fixed-pool"
    if model.startswith("work-stealing"):
        return "work-stealing"
    if model.startswith("virtual-threads"):
        return "virtual-threads"
    return model


def style_for_model(model: str):
    if model.startswith("virtual-threads"):
        return STYLE["virtual"]
    if model.startswith("fixed-pool"):
        return STYLE["fixed"]
    if model.startswith("work-stealing"):
        return STYLE["work"]
    return STYLE["default"]


def read_rows(path: Path):
    rows = []
    with path.open(newline="") as f:
        reader = csv.DictReader(f)
        for r in reader:
            rows.append(
                {
                    "trial": int(r["trial"]),
                    "model": r["model"],
                    "concurrencyLevel": int(r["concurrencyLevel"]),
                    "ioBytesPerTask": int(r["ioBytesPerTask"]),
                    "throughputTasksPerSec": float(r["throughputTasksPerSec"]),
                    "avgLatencyMicros": float(r["avgLatencyMicros"]),
                }
            )
    return rows


def aggregate(rows):
    groups = {}
    for r in rows:
        key = (model_family(r["model"]), r["concurrencyLevel"], r["ioBytesPerTask"])
        groups.setdefault(key, {"throughput": [], "latency": [], "trials": 0})
        groups[key]["throughput"].append(r["throughputTasksPerSec"])
        groups[key]["latency"].append(r["avgLatencyMicros"])
        groups[key]["trials"] += 1

    agg = []
    for (family, concurrency, io_size), values in groups.items():
        tp = values["throughput"]
        lat = values["latency"]
        agg.append(
            {
                "model": family,
                "concurrencyLevel": concurrency,
                "ioBytesPerTask": io_size,
                "mean_throughput": statistics.mean(tp),
                "std_throughput": statistics.stdev(tp) if len(tp) > 1 else 0.0,
                "mean_avg_latency_us": statistics.mean(lat),
                "std_avg_latency_us": statistics.stdev(lat) if len(lat) > 1 else 0.0,
                "trials": values["trials"],
            }
        )
    agg.sort(key=lambda x: (x["ioBytesPerTask"], x["concurrencyLevel"], x["model"]))
    return agg


def write_csv(path: Path, rows, fieldnames):
    with path.open("w", newline="") as f:
        writer = csv.DictWriter(f, fieldnames=fieldnames)
        writer.writeheader()
        writer.writerows(rows)


def make_lineplot(rows, value_key: str, ylabel: str, out_path: Path, log_y: bool = False) -> None:
    io_sizes = sorted({r["ioBytesPerTask"] for r in rows})
    fig, axes = plt.subplots(2, 2, figsize=(12, 8), sharex=True)
    axes = axes.ravel()
    y_min = min(r[value_key] for r in rows)
    y_max = max(r[value_key] for r in rows)

    for idx, io_size in enumerate(io_sizes):
        ax = axes[idx]
        panel = [r for r in rows if r["ioBytesPerTask"] == io_size]
        models = sorted(
            {r["model"] for r in panel},
            key=lambda m: (0 if m.startswith("fixed-pool") else 1 if m.startswith("work-stealing") else 2, m),
        )
        for model in models:
            model_rows = sorted(
                [r for r in panel if r["model"] == model],
                key=lambda x: x["concurrencyLevel"],
            )
            style = style_for_model(model)
            ax.plot(
                [r["concurrencyLevel"] for r in model_rows],
                [r[value_key] for r in model_rows],
                marker="o",
                color=style["color"],
                linewidth=style["linewidth"],
                alpha=style["alpha"],
                zorder=style["zorder"],
                label=model,
            )
        ax.set_title(f"ioBytesPerTask = {io_label(io_size)}")
        ax.set_xlabel("concurrencyLevel")
        ax.set_ylabel(ylabel)
        ax.set_xticks([1, 2, 4, 8])
        if log_y:
            ax.set_yscale("log")
            ax.set_ylim(bottom=max(1e-6, y_min * 0.9), top=y_max * 1.1)
        else:
            ax.set_ylim(bottom=0, top=y_max * 1.08)
        ax.grid(alpha=0.3)

    handles, labels = axes[0].get_legend_handles_labels()
    fig.legend(handles, labels, loc="upper center", ncol=3, frameon=False)
    fig.tight_layout(rect=[0, 0, 1, 0.92])
    fig.savefig(out_path, dpi=220)
    plt.close(fig)


def main() -> None:
    OUT_DIR.mkdir(parents=True, exist_ok=True)
    raw = read_rows(INPUT_CSV)
    agg = aggregate(raw)

    table_a = [
        {
            "model": r["model"],
            "concurrencyLevel": r["concurrencyLevel"],
            "ioBytesPerTask": r["ioBytesPerTask"],
            "mean_throughput": f"{r['mean_throughput']:.6f}",
            "std_throughput": f"{r['std_throughput']:.6f}",
            "trials": r["trials"],
        }
        for r in agg
    ]
    table_b = [
        {
            "model": r["model"],
            "concurrencyLevel": r["concurrencyLevel"],
            "ioBytesPerTask": r["ioBytesPerTask"],
            "mean_avg_latency_us": f"{r['mean_avg_latency_us']:.6f}",
            "std_avg_latency_us": f"{r['std_avg_latency_us']:.6f}",
            "trials": r["trials"],
        }
        for r in agg
    ]

    write_csv(
        OUT_DIR / "table_a_throughput_by_io_and_concurrency.csv",
        table_a,
        [
            "model",
            "concurrencyLevel",
            "ioBytesPerTask",
            "mean_throughput",
            "std_throughput",
            "trials",
        ],
    )
    write_csv(
        OUT_DIR / "table_b_avg_latency_by_io_and_concurrency.csv",
        table_b,
        [
            "model",
            "concurrencyLevel",
            "ioBytesPerTask",
            "mean_avg_latency_us",
            "std_avg_latency_us",
            "trials",
        ],
    )

    make_lineplot(
        agg,
        value_key="mean_throughput",
        ylabel="throughput (tasks/s)",
        out_path=OUT_DIR / "figure_throughput_vs_concurrency.png",
    )
    make_lineplot(
        agg,
        value_key="mean_avg_latency_us",
        ylabel="avg latency (us)",
        out_path=OUT_DIR / "figure_avg_latency_vs_concurrency.png",
    )
    make_lineplot(
        agg,
        value_key="mean_avg_latency_us",
        ylabel="avg latency (us, log scale)",
        out_path=OUT_DIR / "figure_avg_latency_vs_concurrency_log.png",
        log_y=True,
    )

    print("Wrote:")
    print(OUT_DIR / "table_a_throughput_by_io_and_concurrency.csv")
    print(OUT_DIR / "table_b_avg_latency_by_io_and_concurrency.csv")
    print(OUT_DIR / "figure_throughput_vs_concurrency.png")
    print(OUT_DIR / "figure_avg_latency_vs_concurrency.png")
    print(OUT_DIR / "figure_avg_latency_vs_concurrency_log.png")


if __name__ == "__main__":
    main()
