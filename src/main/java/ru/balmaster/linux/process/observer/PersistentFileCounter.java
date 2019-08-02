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
    private final AtomicLong counter;
    private final File file;

    public PersistentFileCounter(File file) throws IOException {
        this.file = file;
        this.channel = (new RandomAccessFile(file, "rw")).getChannel();
        this.counter = new AtomicLong(this.read());
    }

    private long read() throws IOException {
        if (this.channel.size() >= 8L) {
            ByteBuffer buffer = ByteBuffer.allocate(8);
            this.channel.read(buffer);
            buffer.flip();
            return buffer.getLong();
        } else {
            return 0L;
        }
    }

    private void write() throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(8);
        buffer.putLong(this.counter.get());
        buffer.flip();
        this.channel.position(0L);

        while(buffer.hasRemaining()) {
            this.channel.write(buffer);
        }

        this.channel.force(true);
    }

    public void reset() throws IOException {
        this.counter.set(0L);
        this.write();
    }

    public void inc(long value) throws IOException {
        this.counter.addAndGet(value);
        this.write();
    }

    public long get() {
        return this.counter.get();
    }

    public void close() throws IOException {
        this.channel.close();
    }
}
