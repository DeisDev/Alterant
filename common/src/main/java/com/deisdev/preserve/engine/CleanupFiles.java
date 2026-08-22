package com.deisdev.preserve.engine;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;

/** Explicit cleanup I/O only. Call on an I/O worker with paths, never a live world or mutable store. */
public final class CleanupFiles {
    private static final long MAX_BYTES = 64L * 1024 * 1024;
    private static final int MAX_PATHS = 1_000_000;
    private CleanupFiles() {}

    public static void checkUnknownDimensions(Path dimensions, Set<Path> activeFiles) throws IOException {
        if (!Files.exists(dimensions)) { return; }
        var known = activeFiles.stream().map(path -> path.toAbsolutePath().normalize()).collect(java.util.stream.Collectors.toUnmodifiableSet());
        try (var paths = Files.walk(dimensions, 64)) {
            var iterator = paths.iterator();
            int visited = 0;
            while (iterator.hasNext()) {
                var path = iterator.next();
                if (++visited > MAX_PATHS) { throw new IOException("Dimension file scan exceeded its limit; cleanup is incomplete"); }
                if (Files.isSymbolicLink(path) || dimensions.relativize(path).getNameCount() >= 64 && Files.isDirectory(path)) {
                    throw new IOException("Cannot certify linked or excessively deep dimension storage at " + path);
                }
                if (!path.endsWith(Path.of("data", "deisdev", "treatments.dat")) || known.contains(path.toAbsolutePath().normalize())) { continue; }
                try { requireEmpty(path); }
                catch (IOException error) { throw new IOException("Restore the dimension that owns " + path + " before cleanup", error); }
            }
        }
    }

    public static void verifyEmpty(List<Path> files) throws IOException {
        for (var file : files) { requireEmpty(file); }
    }

    private static void requireEmpty(Path file) throws IOException {
        // A missing or unreadable expected file cannot prove a successful native save.
        var tag = NbtIo.readCompressed(file, NbtAccounter.create(MAX_BYTES));
        var data = tag.getCompound("data").orElseThrow(() -> new IOException("Missing Preserve payload at " + file));
        if (data.getInt("schema").orElse(-1) != TreatmentStore.SCHEMA
                || data.getList("records").filter(List::isEmpty).isEmpty()
                || data.contains("resuming") && data.getList("resuming").filter(List::isEmpty).isEmpty()) {
            throw new IOException("Preserve data is not empty or has an unsupported schema at " + file);
        }
    }
}
