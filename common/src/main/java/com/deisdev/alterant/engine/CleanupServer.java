package com.deisdev.alterant.engine;

public interface CleanupServer {
    CleanupJob alterant$cleanup();
    long alterant$saveFailures();
}
