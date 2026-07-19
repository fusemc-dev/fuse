package dev.fusemc.lifecycle.io;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.URI;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.*;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.spi.FileSystemProvider;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/// An implementation of a [CloseableFileSystem] whose **handle** is shared
/// across multiple references.
///
/// ---
///
/// A **shared file system** wraps a NIO [FileSystem] **handle** and counts references to it.
/// An attempt to [#close(boolean)] a shared file system only succeeds once all previously acquired
/// references have been released.
///
/// @since 0.1.0
public final class SharedFileSystem implements CloseableFileSystem {

    private final @NotNull FileSystem handle;
    private final @NotNull FileSystemProvider provider;
    private final @NotNull AtomicInteger count;
    private final @NotNull AtomicBoolean open;

    public SharedFileSystem(@NotNull FileSystem handle) {
        this.handle   = Objects.requireNonNull(handle);
        this.provider = handle.provider();
        this.count    = new AtomicInteger(0);
        this.open     = new AtomicBoolean(true);
    }

    @Override
    public Path parsePath(URI uri) {
        return this.provider.getPath(uri);
    }

    @Override
    public Path parsePath(String path) {
        return this.handle.getPath(path);
    }

    @Override
    public void checkAccess(Path path, Set<? extends AccessMode> modes, LinkOption... options) throws IOException {
        if (this.open.get()) {
            this.provider.checkAccess(path.toRealPath(options), modes.toArray(AccessMode[]::new));
            return;
        }
        throw new ClosedFileSystemException();
    }

    @Override
    public void createDirectory(Path dir, FileAttribute<?>... attrs) throws IOException {
        if (this.open.get()) {
            this.provider.createDirectory(dir, attrs);
            return;
        }
        throw new ClosedFileSystemException();
    }

    @Override
    public void delete(Path path) throws IOException {
        if (this.open.get()) {
            this.provider.delete(path);
            return;
        }
        throw new ClosedFileSystemException();
    }

    @Override
    public SeekableByteChannel newByteChannel(Path path, Set<? extends OpenOption> options, FileAttribute<?>... attrs) throws IOException {
        if (this.open.get())
            return this.provider.newByteChannel(path, options, attrs);
        throw new ClosedFileSystemException();
    }

    @Override
    public DirectoryStream<Path> newDirectoryStream(Path dir, DirectoryStream.Filter<? super Path> filter) throws IOException {
        if (this.open.get())
            return this.provider.newDirectoryStream(dir, filter);
        throw new ClosedFileSystemException();
    }

    @Override
    public Path toAbsolutePath(Path path) {
        return path.toAbsolutePath();
    }

    @Override
    public Path toRealPath(Path path, LinkOption... options) throws IOException {
        return path.toRealPath(options);
    }

    @Override
    public Map<String, Object> readAttributes(Path path, String attributes, LinkOption... options) throws IOException {
        if (this.open.get())
            return this.provider.readAttributes(path, attributes, options);
        throw new ClosedFileSystemException();
    }

    /// Acquire the file system.
    ///
    /// ---
    ///
    /// Increments the reference count of this shared file system and returns itself.
    ///
    /// @since 0.1.0
    public @NotNull SharedFileSystem acquire() {
        if (this.open.get()) {
            this.count.incrementAndGet();
            return this;
        }
        throw new ClosedFileSystemException();
    }

    /// Release the file system.
    ///
    /// ---
    ///
    /// Decrements the reference count of this shared file system and closes the underlying
    /// [FileSystem] if it reaches 0. The disposal of the resource may be forced.
    ///
    /// @since 0.1.0
    @Override
    public boolean close(boolean force) throws IOException {
        if (this.open.get()) {
            if (this.count.decrementAndGet() <= 0 || force) {
                this.handle.close();
                return true;
            }
            return false;
        }
        return false;
    }
}
