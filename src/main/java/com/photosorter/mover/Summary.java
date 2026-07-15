package com.photosorter.mover;

/**
 * Summary of a completed sort operation.
 */
public record Summary(int scanned, int moved, int skipped, int errors) {
}
