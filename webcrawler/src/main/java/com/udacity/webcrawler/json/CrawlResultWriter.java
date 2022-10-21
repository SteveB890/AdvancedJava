package com.udacity.webcrawler.json;

import java.io.Writer;
import java.io.FileWriter;
import java.nio.file.Path;
import java.util.Objects;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Utility class to write a {@link CrawlResult} to file.
 */
public final class CrawlResultWriter {
  private final CrawlResult result;

  /**
   * Creates a new {@link CrawlResultWriter} that will write the given {@link CrawlResult}.
   */
  public CrawlResultWriter(CrawlResult result) {
    this.result = Objects.requireNonNull(result);
  }

  /**
   * Formats the {@link CrawlResult} as JSON and writes it to the given {@link Path}.
   *
   * <p>If a file already exists at the path, the existing file should not be deleted; new data
   * should be appended to it.
   *
   * @param path the file path where the crawl result data should be written.
   */
  public void write(Path path) {
    CrawlerConfiguration retVal;    // value to be returned

    // create a file reader at the provided path and create a CrawlerConfiguration based on the contents.
    try (FileWriter output = new FileWriter(path.toString(), true)) {
      write(output);
      output.close();
    }
    catch (Exception e){
      // do nothing
    }
    finally {
      return;
    }
  }

  /**
   * Formats the {@link CrawlResult} as JSON and writes it to the given {@link Writer}.
   *
   * @param writer the destination where the crawl result data should be written.
   */
  public void write(Writer writer) {
    // configure the builder from the processed JSON
    try {
      // create a builder object
      CrawlerConfiguration.Builder newBuilder = new CrawlerConfiguration.Builder();

      final ObjectMapper objectMapper = new ObjectMapper();
      objectMapper.disable(com.fasterxml.jackson.core.JsonGenerator.Feature.AUTO_CLOSE_TARGET);
      objectMapper.writeValue(writer, result);

      // build and return the CrawlerConfiguration object.
      return;
    }
    catch (Exception e){
      // do nothing
    }
    finally{
      return;
    }
  }
}
