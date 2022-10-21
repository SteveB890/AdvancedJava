package com.udacity.webcrawler.json;

import java.io.Reader;
import java.io.FileReader;
import java.nio.file.Path;
import java.util.Objects;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * A static utility class that loads a JSON configuration file.
 */
public final class ConfigurationLoader {

  private final Path _path;

  /**
   * Create a {@link ConfigurationLoader} that loads configuration from the given {@link Path}.
   */
  public ConfigurationLoader(Path path) {
    this._path = Objects.requireNonNull(path);
  }

  /**
   * Loads configuration from this {@link ConfigurationLoader}'s path
   *
   * @return the loaded {@link CrawlerConfiguration}.
   */
  public CrawlerConfiguration load() {
    CrawlerConfiguration retVal;    // value to be returned

    // create a file reader at the provided path and create a CrawlerConfiguration based on the contents.
    try (FileReader input = new FileReader(_path.toString())) {
      retVal = read(input);
      input.close();
    }
    catch (Exception e){
      retVal = null;
    }

    //return the return value
    return retVal;
  }

  /**
   * Loads crawler configuration from the given reader.
   *
   * @param reader a Reader pointing to a JSON string that contains crawler configuration.
   * @return a crawler configuration
   */
  public static CrawlerConfiguration read(Reader reader) {
    // configure the builder from the processed JSON
    try {
      // create a builder object
      CrawlerConfiguration.Builder newBuilder = new CrawlerConfiguration.Builder();

      final ObjectMapper objectMapper = new ObjectMapper();
      objectMapper.disable(com.fasterxml.jackson.core.JsonParser.Feature.AUTO_CLOSE_SOURCE);
      newBuilder = objectMapper.readValue(reader, CrawlerConfiguration.Builder.class);

      // build and return the CrawlerConfiguration object.
      return newBuilder.build();
    }
    catch (Exception e){
      return null;
    }

  }
}
