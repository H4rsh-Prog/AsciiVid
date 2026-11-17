## Test Method
- Endpoint: GET /profileTest (Controller.profileTest)
- Test clip: <name, resolution, fps, duration> — 304 frames processed
- Profiler: <VisualVM/JProfiler> CPU sampling, single run
- Artifacts: hotSpots.csv, forwardCalls.csv

## Results
Total request time: 137,977 ms (118,262 ms CPU)

| Component | Wall | Self | Share |
|---|---|---|---|
| playProcessedObject | 79,427 ms | 75,631 ms | 54.8% |
| generateAsciiArt | 55,056 ms | 4,929 ms | 39.9% |
| processVideoObject (I/O) | 57,970 ms | 2,809 ms | 2.0% |

## Bottleneck
1. playProcessedObject — 54.8% of wall time is self time inside this
   method (console/output writing suspected). Dominant cost.
2. generateAsciiArt — 18.24M UByteRawIndexer.get() calls costing ~50s
   (~2.75 µs/pixel, ~300× raw array access). Indexer abstraction
   overhead, not the mapping logic (self time only 4.9s).
3. Video I/O — ruled out. processVideoObject self time is 2%.
   Character mapping algorithm — ruled out (self time ~5s).