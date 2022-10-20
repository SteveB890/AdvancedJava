package com.udacity.webcrawler.main;

import com.google.inject.Guice;
import com.udacity.webcrawler.WebCrawler;
import com.udacity.webcrawler.WebCrawlerModule;
import com.udacity.webcrawler.json.ConfigurationLoader;
import com.udacity.webcrawler.json.CrawlResult;
import com.udacity.webcrawler.json.CrawlResultWriter;
import com.udacity.webcrawler.json.CrawlerConfiguration;
import com.udacity.webcrawler.profiler.Profiler;
import com.udacity.webcrawler.profiler.ProfilerModule;

import javax.inject.Inject;
import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.file.Path;
import java.util.Objects;

public final class WebCrawlerMain {

  private final CrawlerConfiguration config;

  private WebCrawlerMain(CrawlerConfiguration config) {
    this.config = Objects.requireNonNull(config);
  }

  @Inject
  private WebCrawler crawler;

  @Inject
  private Profiler profiler;

  private void run() throws Exception {
    Guice.createInjector(new WebCrawlerModule(config), new ProfilerModule()).injectMembers(this);

    CrawlResult result = crawler.crawl(config.getStartPages());
    CrawlResultWriter resultWriter = new CrawlResultWriter(result);

    String pathString = config.getResultPath();
    // check to see if a path is provided
    if (pathString.isBlank() || pathString == null){
      //if there is no path output to stdout.
      BufferedWriter standardOut = new BufferedWriter(new OutputStreamWriter(System.out));
      resultWriter.write(standardOut);
    }
    else
    {
      //if there is a path output to the identified file.
      Path path = Path.of(config.getResultPath());
      resultWriter.write(path);
    }

    // write the profile data
    String profilePath = config.getProfileOutputPath();
    // check to see if a path is provided
    if (profilePath.isBlank() || profilePath == null){
      //if there is no path output to stdout.
      BufferedWriter standardOut = new BufferedWriter(new OutputStreamWriter(System.out));
      profiler.writeData(standardOut);
    }
    else
    {
      //if there is a path output to the identified file.
      Path path = Path.of(config.getProfileOutputPath());
      profiler.writeData(path);
    }

  }

  public static void main(String[] args) throws Exception {
    if (args.length != 1) {
      System.out.println("Usage: WebCrawlerMain [starting-url]");
      return;
    }

    CrawlerConfiguration config = new ConfigurationLoader(Path.of(args[0])).load();
    new WebCrawlerMain(config).run();
  }
}
