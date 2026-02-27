# ECS 251 — Concurrency Model Benchmark

**Repository:** https://github.com/Vg34100/ecs251-project

## Project Overview

This project evaluates three concurrency models under controlled CPU-heavy,
IO-heavy, and mixed workloads on the JVM (Java 21). The goal is to measure
and compare throughput, latency, tail latency (p95/p99), and CPU utilization
across models to clarify when each is most appropriate in practice.

The three models evaluated are:

| Model | Class | Description |
|---|---|---|
| Fixed Thread Pool | `FixedThreadPoolModel` | Fixed number of OS threads via `Executors.newFixedThreadPool` |
| Work-Stealing | `WorkStealingModel` | `ForkJoinPool` with `asyncMode=true` for FIFO independent-task scheduling |
| Virtual Threads | `VirtualThreadModel` | Java 21 Project Loom virtual threads via `Executors.newVirtualThreadPerTaskExecutor` |

## How the Proposal Maps to the Code

| Proposal Concept | Code Location |
|---|---|
| Fixed-size thread pool model | `src/models/FixedThreadPoolModel.java` |
| Work-stealing scheduler model | `src/models/WorkStealingModel.java` |
| Coroutine-based runtime | `src/models/VirtualThreadModel.java` |
| CPU-heavy microbenchmark | `src/workloads/cpu/CpuBusyWorkload.java` |
| IO-heavy microbenchmark (simulated) | `src/workloads/io/SleepIoWorkload.java` |
| IO-heavy microbenchmark (real file IO) | `src/workloads/io/FileRWWorkload.java` |
| Mixed/application-style workload | `src/workloads/mixed/MixedWorkload.java` |
| Benchmark runner (throughput + latency) | `src/bench/BenchmarkRunner.java` |
| p95/p99 tail latency | `src/bench/BenchmarkRunner.java` (`percentile()`) |
| CPU utilization sampling | `src/bench/BenchmarkRunner.java` (`cpuSampler` thread) |
| Results recording | `src/bench/CsvWriter.java` |
| Main experiment entry point | `src/bench/Main.java` |
| IO parameter sweep entry point | `src/bench/IoExperimentMain.java` |

## Project Structure

```
src/
  bench/               # Benchmark runner, timing, CSV output, entry points
    BenchmarkRunner.java   # Runs tasks, measures wall time, latency, p95/p99, CPU load
    TimedTask.java         # Wraps Runnable to record per-task start/end time
    RunResult.java         # Holds all metrics for one benchmark run
    CsvWriter.java         # Writes results to CSV files
    Main.java              # Main experiment: all models x all workloads
    IoExperimentMain.java  # IO parameter sweep: concurrency level x IO size

  models/              # Concurrency model implementations
    ConcurrencyModel.java      # Interface: name() + runAll(List<Runnable>)
    FixedThreadPoolModel.java
    WorkStealingModel.java
    VirtualThreadModel.java
    SequentialModel.java       # Baseline: single-threaded sequential execution

  workloads/           # Workload definitions
    Workload.java              # Base interface
    TaskFactoryWorkload.java   # Extends Workload with buildTasks(n)
    cpu/
      CpuBusyWorkload.java     # XORshift busy loop, configurable iters
      TestRunCpuWorkload.java  # Quick sanity runner for CPU workload
    io/
      SleepIoWorkload.java     # Sleep + small CPU work, simulates blocking IO
      FileRWWorkload.java      # Real file write + read per task
      TestRunIoWorkload.java   # Sanity checks for IO workloads
    mixed/
      MixedWorkload.java       # Sleep + CPU work combined (application-style)

results/
  results-io-raw.csv   # Raw IO sweep results (concurrency x IO size x model x trial)

src/results.csv        # Raw CPU experiment results (model x task count x task size x trial)
```

## Requirements

- Java 21 or later (required for virtual threads / Project Loom)
- No external dependencies

## How to Run

### Main benchmark (all models, all workloads)

Run `bench.Main` from your IDE, or from the command line:

```bash
mkdir -p /tmp/ecs251_out
javac -d /tmp/ecs251_out $(find src -name '*.java')
java -cp /tmp/ecs251_out bench.Main
```

Output is printed to stdout and appended to `results.csv`.

### IO parameter sweep

```bash
java -cp /tmp/ecs251_out bench.IoExperimentMain
```

Output is written to `results/results-io-raw.csv`.

### CPU workload quick test

```bash
java -cp /tmp/ecs251_out workloads.cpu.TestRunCpuWorkload
```

### IO workload sanity check

```bash
java -cp /tmp/ecs251_out workloads.io.TestRunIoWorkload
```

## Measurement Methodology

- Each workload configuration runs a warm-up pass before recorded trials to reduce JIT noise.
- Each configuration is measured for 3 trials; results are saved individually for averaging downstream.
- Wall-clock time is measured with `System.nanoTime()`.
- Per-task latency is recorded by wrapping each `Runnable` in a `TimedTask`.
- p95 and p99 are computed using the nearest-rank method on the sorted per-task latency array.
- CPU utilization is sampled every 50ms during each run using `com.sun.management.OperatingSystemMXBean.getProcessCpuLoad()`. Runs shorter than ~50ms may report -1 (unavailable).

## Hardware Notes

Results collected on multiple machines. When comparing across result files, note:

- `src/results.csv` — CPU experiments, collected on an Intel Core i7-11370H @ 3.30GHz, 16GB RAM (Pablo)
- `results/results-io-raw.csv` — IO sweep, collected on a separate machine (Manami — add your specs here)
- Any additional result files — note machine specs alongside the file
