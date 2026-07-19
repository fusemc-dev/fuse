package dev.fusemc.lifecycle.io;

import org.graalvm.polyglot.io.FileSystem;

import java.io.IOException;

public interface CloseableFileSystem extends FileSystem {

    boolean close(boolean force) throws IOException;
}
