package org.folio.utils;

import java.io.FileWriter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import com.google.common.collect.Iterators;

import au.com.bytecode.opencsv.CSVWriter;

/**
 * Generates a CSV file with dummy UUID data.
 * Approximate file size for 10 000 000 records is ~390 mb.
 */
public class CsvGenerator {

  private static final int WRITER_BATCH_SIZE = 100;
  private static final String CSV_PATH = "sample-data/data.csv";
  private static CSVWriter writer;

  public static void main(String[] args) throws Exception {

    writer = new CSVWriter(new FileWriter(CSV_PATH));

    Stream<String[]> stream = Stream.generate(() -> UUID.randomUUID().toString())
      .limit(WRITER_BATCH_SIZE * 1000L)
      .map(CsvGenerator::stringToArrayMapper);

    Iterators.partition(stream.iterator(), WRITER_BATCH_SIZE)
      .forEachRemaining(CsvGenerator::writeArrays);

    writer.close();
  }

  private static void writeArrays(List<String[]> strings) {
    writer.writeAll(strings);
  }

  private static String[] stringToArrayMapper(String string) {
    return new String[]{string};
  }
}
