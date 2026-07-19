package dev.fusemc.lifecycle.io;

import org.graalvm.polyglot.io.FileSystem;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.io.Closeable;
import java.io.IOException;
import java.net.URI;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.*;
import java.nio.file.attribute.FileAttribute;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.StreamSupport;

/// An implementation of a virtual Polyglot [FileSystem].
///
/// ---
///
/// A **virtual file system** represents a file system that behaves like a shifted
/// view of another file system. It is defined in terms of a **virtual root** and
/// a **current directory**.
///
/// Any requested path is resolved against either the **cwd** if relative, or the **virtual root** if absolute. During the resolution, no path can escape the **virtual root**. If one attempts,
/// it is "_clamped_" at the virtual root.
///
/// All path access appears to the outer viewer as if the **virtual root** were the root of the associated file system.
///
/// @since `0.1.0`
public final class VirtualFileSystem implements CloseableFileSystem {

    private final @NotNull FileSystem delegate;
    private final @NotNull Path root;
    private final @NotNull Path cwd;

    public VirtualFileSystem(@NotNull FileSystem delegate,
                             @NotNull Path root,
                             @NotNull Path cwd) {
        Objects.requireNonNull(delegate);
        Objects.requireNonNull(root);
        Objects.requireNonNull(cwd);
        this.delegate = delegate;
        this.root = root.toAbsolutePath().normalize();
        this.cwd  = cwd.toAbsolutePath().normalize();
    }

    @Override
    public @NotNull Path parsePath(@NotNull URI uri) {
        Objects.requireNonNull(uri);
        return this.delegate.parsePath(uri);
    }

    @Override
    public @NotNull Path parsePath(@NotNull String path) {
        Objects.requireNonNull(path);
        return this.delegate.parsePath(path);
    }

    @Override
    public void checkAccess(@NotNull Path path,
                            @NotNull Set<? extends AccessMode> modes,
                            @NotNull LinkOption @NotNull ... options) throws IOException {
        Objects.requireNonNull(path);
        Objects.requireNonNull(modes);
        Objects.requireNonNull(options);
        this.delegate.checkAccess(this.resolveAbsolute(this.toRealPath(path, options)), modes, options);
    }

    @Override
    public void createDirectory(@NotNull Path path,
                                @NotNull FileAttribute<?> @NotNull ... attrs) throws IOException {
        Objects.requireNonNull(path);
        Objects.requireNonNull(attrs);
        this.delegate.createDirectory(this.resolveAbsolute(this.toSemiRealPath(path, false)), attrs);
    }

    @Override
    public void delete(@NotNull Path path) throws IOException {
        Objects.requireNonNull(path);
        this.delegate.delete(this.resolveAbsolute(this.toRealPath(path)));
    }

    @Override
    public @NotNull SeekableByteChannel newByteChannel(@NotNull Path path,
                                                       @NotNull Set<? extends OpenOption> options,
                                                       @NotNull FileAttribute<?> @NotNull ... attrs) throws IOException {
        Objects.requireNonNull(path);
        Objects.requireNonNull(options);
        Objects.requireNonNull(attrs);
        return this.delegate.newByteChannel(this.resolveAbsolute(this.toSemiRealPath(path, false)), options, attrs);
    }

    @Override
    public @NotNull DirectoryStream<Path> newDirectoryStream(@NotNull Path path,
                                                             @NotNull DirectoryStream.Filter<? super Path> filter) throws IOException {
        Objects.requireNonNull(path);
        Objects.requireNonNull(filter);
        return new DirectoryStream<>() {

            private final DirectoryStream<Path> delegate = VirtualFileSystem.this.delegate.newDirectoryStream(
                    VirtualFileSystem.this.resolveAbsolute(VirtualFileSystem.this.toRealPath(path)),
                    (_) -> true
            );

            @Override
            public @NonNull Iterator<Path> iterator() {
                return StreamSupport.stream(this.delegate.spliterator(), false)
                        .map(VirtualFileSystem.this::toVirtualPath)
                        .filter(candidate -> {
                            try {
                                return filter.accept(candidate);
                            } catch (IOException e) {
                                throw new DirectoryIteratorException(e);
                            }
                        })
                        .iterator();
            }

            @Override
            public void close() throws IOException {
                this.delegate.close();
            }
        };
    }

    @Override
    public @NotNull Map<String, Object> readAttributes(@NotNull Path path, @NotNull String attributes, @NotNull LinkOption... options) throws IOException {
        Objects.requireNonNull(path);
        Objects.requireNonNull(attributes);
        Objects.requireNonNull(options);
        return this.delegate.readAttributes(this.resolveAbsolute(this.toRealPath(path, options)), attributes, options);
    }

    /// Converts the given [Path] to an absolute path.
    ///
    /// ---
    ///
    /// In a virtual file system, an absolute path is a [Path] that,
    /// when resolved against the **root** points to the same location as the given one.
    ///
    /// If the given [Path] is already absolute, it is returned as is.
    ///
    /// @since 0.1.0
    @Override
    public @NotNull Path toAbsolutePath(@NotNull Path path) {
        Objects.requireNonNull(path);
        if (path.isAbsolute())
            return path;
        var resolved = this.cwd.resolve(path).normalize();
        if (resolved.startsWith(this.root)) {
            var relative = this.root.relativize(resolved);
            var root     = this.root.getRoot();
            return root.resolve(relative);
        }
        return this.root.getRoot();
    }

    /// Converts the given [Path] to a real path.
    ///
    /// ---
    /// A 'real path' is an ill-defined concept. In a virtual file system, a real path is an absolute
    /// path with all of its symlinks resolved. If at any point during the resolution the **virtual root**
    /// is escaped, the process continues from the virtual root.
    ///
    /// This method is a shorthand for `toSemiRealPath(path, true, options)`, and thus requires the final
    /// segment of the path to exist.
    ///
    /// @since `0.1.0`
    @Override
    public @NotNull Path toRealPath(@NotNull Path path, @NotNull LinkOption... options) throws IOException {
        Objects.requireNonNull(path);
        Objects.requireNonNull(options);
        return this.toSemiRealPath(path, true, options);
    }

    /// Converts the given [Path] to a semi-real path.
    ///
    /// ---
    /// A 'real path' is an ill-defined concept. In a virtual file system, a real path is an absolute
    /// path with all of its symlinks resolved. If at any point during the resolution the **virtual root**
    /// is escaped, the process continues from the virtual root.
    ///
    /// Whether the final segment of the [Path] must exist is determined by the `mustExist` parameter.
    ///
    /// @apiNote  Funnily enough, after the first few tests it became apparent that symlinks are completely forbidden in our environment. Well, we'll handle them regardless.
    ///
    /// @since `0.1.0`
    public @NotNull Path toSemiRealPath(@NotNull Path path,
                                        boolean mustExist,
                                        @NotNull LinkOption... options) throws IOException {
        Objects.requireNonNull(path);
        Objects.requireNonNull(options);
        var absolute = this.toAbsolutePath(path);
        var length   = absolute.getNameCount();
        var buffer   = absolute.getRoot();
        for (var i = 1; i <= length; i++) {
            var subpath = absolute.subpath(0, i);
            // We can safely ignore the current buffer here: if the subpath
            // contains any symlinks, they'll be resolved by the nio.Files operations.
            //
            // Thus, we can just build the buffer incrementally.
            var host    = this.root.resolve(subpath);
            if ((i == length && !mustExist) || Files.exists(host, options)) {
                // No need to precompute shouldFollowLinks due to short-circuiting
                if (Files.isSymbolicLink(host) && (i < length || VirtualFileSystem.shouldFollowLinks(options))) {
                    buffer = this.toVirtualPath(host.resolveSibling(Files.readSymbolicLink(host)));
                    continue;
                }
                buffer = buffer.resolve(subpath.getFileName());
                continue;
            }
            throw new NoSuchFileException(absolute.toString());
        }
        return buffer;
    }

    public @NotNull Path resolveAbsolute(@NotNull Path path) {
        if (path.isAbsolute()) {
            var root = path.getRoot();
            var relative = root.relativize(path);
            return this.root.resolve(relative);
        }
        throw new AssertionError();
    }

    public @NotNull Path toVirtualPath(@NotNull Path path) {
        var normalized = path.toAbsolutePath().normalize();
        if (normalized.startsWith(this.root)) {
            var relative = this.root.relativize(normalized);
            var root     = normalized.getRoot();
            return root.resolve(relative);
        }
        return this.root.getRoot();
    }

    private static boolean shouldFollowLinks(@NotNull LinkOption... options) {
        for (var option : options) {
            if (option == LinkOption.NOFOLLOW_LINKS)
                return false;
        }
        return true;
    }

    @Override
    public boolean close(boolean force) throws IOException {
        if (this.delegate instanceof CloseableFileSystem closeable)
            return closeable.close(force);
        return false;
    }
}

