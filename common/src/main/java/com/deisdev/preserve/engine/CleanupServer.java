package com.deisdev.preserve.engine;

public interface CleanupServer {
    CleanupJob preserve$cleanup();
    long preserve$saveFailures();
}
