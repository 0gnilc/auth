package com.gnilc.test.cleanup;

public enum CleanupMode {
    NONE,
    TRANSACTION_ROLLBACK,
    REDIS_CLEAN,
    BASELINE_RESET
}
