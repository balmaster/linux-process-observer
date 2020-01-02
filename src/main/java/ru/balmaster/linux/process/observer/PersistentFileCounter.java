package ru.balmaster.linux.process.observer;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.concurrent.atomic.AtomicLong;

public class PersistentFileCounter implements Closeable {
    private final FileChannel channel;
    private final AtomicLong counter = new AtomicLong();
    private final File file;
    private long resetDate;

    public PersistentFileCounter(File file) throws IOException {
        this.file = file;
        this.channel = (new RandomAccessFile(file, "rw")).getChannel();
        read();
        write();
    }

    private void read() throws IOException {
        if (channel.size() >= 8L) {
            ByteBuffer buffer = ByteBuffer.allocate(16);
            channel.read(buffer);
            buffer.flip();
            counter.set(buffer.getLong());
            if (channel.size() >= 16) {
                resetDate = buffer.getLong();
            } else {
                resetDate = TimeUtils.getNetTime().toEpochMilli();
            }
        } else {
            resetDate = TimeUtils.getNetTime().toEpochMilli();
            counter.set(0);
        }
    }

    private void write() throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(16);
        buffer.putLong(counter.get());
        buffer.putLong(resetDate);
        buffer.flip();
        channel.position(0L);

        while(buffer.hasRemaining()) {
            channel.write(buffer);
        }

        channel.force(true);
    }

    public void reset() throws IOException {
        counter.set(0L);
        resetDate = TimeUtils.getNetTime().toEpochMilli();
        write();
    }

    public void inc(long value) throws IOException {
        this.counter.addAndGet(value);
        write();
    }

    public long get() {
        return this.counter.get();
    }

    public long getResetDate() {
        return resetDate;
    }

    public void close() throws IOException {
        this.channel.close();
    }
}
