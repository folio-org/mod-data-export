package org.folio.domain.reader;

import java.io.IOException;
import java.util.stream.Stream;

public interface SourceStreamReader<T> {

  Stream<T> getSourceStream() throws IOException;
}
