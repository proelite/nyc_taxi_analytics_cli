package com.xiaodi.taxi.etl;

import java.io.IOException;
import java.nio.file.Path;
import java.util.stream.Stream;

public interface DirectoryScanner {
    Stream<Path> listParquetFiles(Path inputDir) throws IOException;
}
