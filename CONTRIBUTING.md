# Contributing

This project compares different concurrency models using shared workloads and a common benchmark runner. 
The notes below are just to help us avoid merge conflicts and keep things organized.

## Project structure

```

src/
bench/        # Benchmark runner and main entry point
models/       # Concurrency model implementations
workloads/    # Workload definitions (CPU / IO)

```

## Adding workloads
- Add new workload classes in `src/workloads/`
- Examples: `CpuHeavyWorkload.java`, `IoHeavyWorkload.java`
- Try to keep workloads focused on *what the task does*, not how it is scheduled
- It’s usually better to add new files instead of changing existing ones

## Adding concurrency models
- Add new models in `src/models/`
- Each model should implement the `ConcurrencyModel` interface
- Models should only handle scheduling/execution, not define workloads

## Benchmark / runner code
- The benchmark runner lives in `src/bench/`
- For now, it’s probably best not to change this unless we talk about it first

## General notes
- Small, focused commits are encouraged
- Clear commit messages help a lot (e.g., `meeting1: add CPU workload`)
- If something feels unclear, it’s totally fine to ask before changing things
