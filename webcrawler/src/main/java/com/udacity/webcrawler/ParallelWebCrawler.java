package com.udacity.webcrawler;

import com.udacity.webcrawler.json.CrawlResult;
import com.udacity.webcrawler.parser.PageParser;
import com.udacity.webcrawler.parser.PageParserFactory;

import java.util.concurrent.RecursiveAction;
import java.util.concurrent.RecursiveTask;

import javax.inject.Inject;
import javax.inject.Provider;
import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * A concrete implementation of {@link WebCrawler} that runs multiple threads on a
 * {@link ForkJoinPool} to fetch and process multiple web pages in parallel.
 */
final class ParallelWebCrawler implements WebCrawler {
    private final Clock clock;
    private final Duration timeout;
    private final int popularWordCount;
    private final ForkJoinPool pool;
    private final List<Pattern> ignoredUrls;

    private Instant deadline;
    private final int maxDepth;
    private Map<String, Integer> counts;
    private Set<String> visitedUrls;

    // used to protect multi-threaded data.
    private final ReentrantLock lock;


    /** ParallelWebCrawler construction
     * @param clock - the current time.
     * @param timeout - how long the crawl is allowed to run
     * @param popularWordCount - counts of the most popular words
     * @param threadCount - how many parallel processes are to be used
     * @param maxDepth - how deep is the crawl allowed to go
     * @param ignoredUrls - URLs matching these patterns will be ignored.
     */
    @Inject
  ParallelWebCrawler(
      Clock clock,
      @Timeout Duration timeout,
      @PopularWordCount int popularWordCount,
      @TargetParallelism int threadCount,
      @MaxDepth int maxDepth,
      @IgnoredUrls List<Pattern> ignoredUrls) {
    this.clock = clock;
    this.timeout = timeout;
    this.popularWordCount = popularWordCount;
    this.pool = new ForkJoinPool(Math.min(threadCount, getMaxParallelism()));
    this.maxDepth = maxDepth;
    this.ignoredUrls = ignoredUrls;

    // initialize the lock
    lock = new ReentrantLock();
  }

  // get the page parser via dependency injection
  @Inject private PageParserFactory parserFactory;

    /** crawl - crawl through the pages provided in startingURLs
     * @param startingUrls - the URLs to crawl
     * @return - CrawlResult with the results of the crawl
     */
  @Override
  public CrawlResult crawl (List<String> startingUrls) {
    this.deadline = clock.instant().plus(timeout);
    this.counts = new HashMap<>();
    this.visitedUrls = new HashSet<>();

    // invoke a CrawlInternalAction for each URL requested in startingURLs
    for (String url : startingUrls) {
      PageParser.Result result = parserFactory.get(url).parse();
      pool.invoke(new CrawlInternalAction(url, maxDepth));
    }

    // if no words were found, then return counts unmodified.
    if (counts.isEmpty()) {
      return new CrawlResult.Builder()
              .setWordCounts(counts)
              .setUrlsVisited(visitedUrls.size())
              .build();
    }

    // if words were found then update the return value to show what was found.
    return new CrawlResult.Builder()
          .setWordCounts(WordCounts.sort(counts, popularWordCount))
          .setUrlsVisited(visitedUrls.size())
          .build();
  }

    /** getMaxParallelism
     * @return - the max number of processors available
     */
  @Override
  public int getMaxParallelism() {
    return Runtime.getRuntime().availableProcessors();
  }


    /** CrawlInternalAction - extends RecursiveAction to provide parsing starting at the provided URL.
     *
     */
  public final class CrawlInternalAction extends RecursiveAction {
      private final String url;
      private final int localMaxDepth;

      /** CrawlInternalAction - Constructor for CrawlInternalAction
       * @param url - the URL to be parsed first
       * @param localMaxDepth - the meximum depth to be crawled by this object.
       */
      public CrawlInternalAction(String url, int localMaxDepth) {
          this.url = url;
          this.localMaxDepth = localMaxDepth;
      }

      /** compute - this runs the action for the RecursiveAction. It will determine
       *            if the provided url should be parsed and if so parse and record the results
       *            in counts provided by the parent class.
       */
    @Override
    protected void compute() {
        // if this page is beyond our maxDepth, has exceeded the timeout time,
        // has already been visited, or matches an ignored pattern then return wihtout taking action.
        if (localMaxDepth == 0 || clock.instant().isAfter(deadline)) {
            return;
        }
        for (Pattern pattern : ignoredUrls) {
            if (pattern.matcher(url).matches()) {
                return;
            }
        }

        // add the page to the list of visited pages and parse the page (return if the URL has already been added)
        try {
            lock.lock();

            // if the url has been visited, return
            if (visitedUrls.contains(url)) {
                return;
            }

            // add the url to the visited list and process the page.
            visitedUrls.add(url);
        } finally {
            lock.unlock();
        }

        // parse the page
        PageParser.Result result = parserFactory.get(url).parse();

        // update the counts map for keywords that were found on the page.
        // use synchronized to ensure that only one process has access to counts at a time.
        synchronized (counts) {
            for (Map.Entry<String, Integer> e : result.getWordCounts().entrySet()) {
                if (counts.containsKey(e.getKey())) {
                    counts.put(e.getKey(), e.getValue() + counts.get(e.getKey()));
                } else {
                    counts.put(e.getKey(), e.getValue());
                }
            }
        }

        // generate a list of RecurssiveActions to take based on the links parsed from the page
        List<CrawlInternalAction> subtasks = new ArrayList<CrawlInternalAction>();
        for (String link : result.getLinks()) {
            subtasks.add(new CrawlInternalAction(link, localMaxDepth - 1));
        }

        // Invoke all of the identified tasks at the next depth
        invokeAll(subtasks);
    }
  }
}
