package com.hbm.util;

import java.util.concurrent.atomic.AtomicLongArray;
import java.util.concurrent.atomic.LongAdder;

/**
 * Thread-safe bitset
 *
 * @author mlbv
 */
public class ConcurrentBitSet {
    private final AtomicLongArray words;
    private final int size;
    private final LongAdder bitCount = new LongAdder();

    public ConcurrentBitSet(int size) {
        this.size = size;
        int wordCount = (size + 63) >>> 6;
        this.words = new AtomicLongArray(wordCount);
    }

    public static ConcurrentBitSet fromLongArray(long[] data, int size) {
        ConcurrentBitSet bitSet = new ConcurrentBitSet(size);
        int wordsToCopy = Math.min(data.length, bitSet.words.length());
        long totalBits = 0;
        for (int i = 0; i < wordsToCopy; i++) {
            long word = data[i];
            bitSet.words.set(i, word);
            totalBits += Long.bitCount(word);
        }
        bitSet.bitCount.add(totalBits);
        return bitSet;
    }

    public void set(int bit) {
        if (bit < 0 || bit >= size) return;
        int wordIndex = bit >>> 6;
        long mask = 1L << (bit & 63);
        while (true) {
            long oldWord = words.get(wordIndex);
            long newWord = oldWord | mask;
            if (oldWord == newWord) return;
            if (words.compareAndSet(wordIndex, oldWord, newWord)) {
                bitCount.increment();
                return;
            }
        }
    }

    public void clear(int bit) {
        if (bit < 0 || bit >= size) return;
        int wordIndex = bit >>> 6;
        long mask = ~(1L << (bit & 63));
        while (true) {
            long oldWord = words.get(wordIndex);
            long newWord = oldWord & mask;
            if (oldWord == newWord) return;
            if (words.compareAndSet(wordIndex, oldWord, newWord)) {
                bitCount.decrement();
                return;
            }
        }
    }

    public int nextSetBit(int from) {
        if (from < 0) from = 0;
        int wordIndex = from >>> 6;
        if (wordIndex >= words.length()) return -1;
        long word = words.get(wordIndex) & (~0L << (from & 63));
        while (true) {
            if (word != 0) {
                int idx = (wordIndex << 6) + Long.numberOfTrailingZeros(word);
                return (idx < size) ? idx : -1;
            }
            wordIndex++;
            if (wordIndex >= words.length()) return -1;
            word = words.get(wordIndex);
        }
    }

    public boolean isEmpty() {
        return bitCount.sum() == 0;
    }

    public long cardinality() {
        return bitCount.sum();
    }

    public long[] toLongArray() {
        long[] result = new long[words.length()];
        for (int i = 0; i < result.length; i++) {
            result[i] = words.get(i);
        }
        return result;
    }
}