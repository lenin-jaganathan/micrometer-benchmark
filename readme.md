## Comparison of meter interactions
The following scenarios were considered,
* No Caching and the operation is directly called on the meter fluent builder.
* A static object is used to store the meter and operations are performed directly on it.
* Tags are used as meter-cache key.
* Meter.Id is used a cache-key.
* 2 Custom objects are used as Cache keys.

For caching scenarios, the cache and cache look-up time is not covered. Instead, the object creation costs and the memory allocations are considered. We might need to simulate that also to lay down an inclusive cache.

See the below benchmark results when one meter-filter to add a common tag is added to the registry,
1. Average Time ![Screenshot 2024-01-09 at 1.34.26 PM.png](..%2F..%2F..%2F..%2Fvar%2Ffolders%2Ffb%2Fz7d9zf497tv4qr_fmws8n1nr0000gq%2FT%2FTemporaryItems%2FNSIRD_screencaptureui_Dk4CDE%2FScreenshot%202024-01-09%20at%201.34.26%E2%80%AFPM.png)
2. Memory Allocation per interaction![Screenshot 2024-01-09 at 1.35.49 PM.png](..%2F..%2F..%2F..%2Fvar%2Ffolders%2Ffb%2Fz7d9zf497tv4qr_fmws8n1nr0000gq%2FT%2FTemporaryItems%2FNSIRD_screencaptureui_x1n97F%2FScreenshot%202024-01-09%20at%201.35.49%E2%80%AFPM.png)
