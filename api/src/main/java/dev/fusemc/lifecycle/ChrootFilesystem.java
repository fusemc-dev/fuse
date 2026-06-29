package dev.fusemc.lifecycle;

import org.graalvm.polyglot.io.FileSystem;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.URI;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.*;
import java.nio.file.attribute.FileAttribute;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class ChrootFilesystem implements FileSystem {

    private final @NotNull Path root;

    private ChrootFilesystem(@NotNull Path root) {
        Objects.requireNonNull(root);
        this.root = root.toAbsolutePath();
    }

    @Override
    public @NotNull Path parsePath(@NotNull URI uri) {
        Objects.requireNonNull(uri);
        return Path.of(uri);
    }

    @Override
    public @NotNull Path parsePath(@NotNull String path) {
        Objects.requireNonNull(path);
        return Path.of(path);
    }

    @Override
    public void checkAccess(@NotNull Path path,
                            @NotNull Set<? extends AccessMode> modes,
                            @NotNull LinkOption @NotNull ... linkOptions) throws IOException {
        Objects.requireNonNull(path);
        Objects.requireNonNull(modes);
        Objects.requireNonNull(linkOptions);
        if (this.isSubpath(path, linkOptions)) {
            if (Files.exists(path, linkOptions)) {
                for (var mode : modes) {
                    var allowed = switch (mode) {
                        case READ    -> Files.isReadable(path);
                        case WRITE   -> Files.isWritable(path);
                        case EXECUTE -> Files.isExecutable(path);
                    };
                    if (allowed)
                        continue;
                    throw new SecurityException();
                }
            }
            throw new NoSuchFileException(this.toAbsolutePath(path).toString());
        }
        throw new SecurityException();
    }

    @Override
    public void createDirectory(@NotNull Path path,
                                @NotNull FileAttribute<?> @NotNull ... attrs) throws IOException {
        Objects.requireNonNull(path);
        Objects.requireNonNull(attrs);
        if (this.isSubpath(path)) {
            Files.createDirectories(path, attrs);
            return;
        }
        throw new SecurityException();
    }

    @Override
    public void delete(@NotNull Path path) throws IOException {
        Objects.requireNonNull(path);
        if (this.isSubpath(path)) {
            Files.delete(path);
            return;
        }
        throw new SecurityException();
    }

    @Override
    public @NotNull SeekableByteChannel newByteChannel(@NotNull Path path,
                                                       @NotNull Set<? extends OpenOption> options,
                                                       @NotNull FileAttribute<?> @NotNull ... attrs) throws IOException {
        Objects.requireNonNull(path);
        Objects.requireNonNull(options);
        Objects.requireNonNull(attrs);
        if (this.isSubpath(path))
            return Files.newByteChannel(path, options, attrs);
        throw new SecurityException();
    }

    @Override
    public @NotNull DirectoryStream<Path> newDirectoryStream(@NotNull Path dir,
                                                             @NotNull DirectoryStream.Filter<? super Path> filter) throws IOException {
        Objects.requireNonNull(dir);
        Objects.requireNonNull(filter);
        if (this.isSubpath(dir))
            return Files.newDirectoryStream(dir, filter);
        throw new SecurityException();
    }

    @Override
    public @NotNull Path toAbsolutePath(@NotNull Path path) {
        Objects.requireNonNull(path);
        if (path.isAbsolute()) {
            if (path.startsWith(this.root))
                return path;
            throw new SecurityException();
        }
        var resolved = path.toAbsolutePath();
        if (resolved.startsWith(this.root))
            return resolved;
        throw new SecurityException();
    }

    @Override
    public @NotNull Path toRealPath(@NotNull Path path, @NotNull LinkOption... linkOptions) throws IOException {
        Objects.requireNonNull(path);
        var resolved = path.toRealPath(linkOptions);
        if (resolved.startsWith(this.root))
            return resolved;
        throw new SecurityException();
    }

    @Override
    public @NotNull Map<String, Object> readAttributes(@NotNull Path path, @NotNull String attributes, @NotNull LinkOption... options) throws IOException {
        Objects.requireNonNull(path);
        Objects.requireNonNull(attributes);
        Objects.requireNonNull(options);
        if (this.isSubpath(path, options))
            return Files.readAttributes(path, attributes, options);
        throw new SecurityException();
    }

    private boolean isSubpath(@NotNull Path path, @NotNull LinkOption... options) throws IOException {
        return path.toRealPath(options)
                .normalize()
                .startsWith(this.root);
    }
}
