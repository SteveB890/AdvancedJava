package com.udacity.webcrawler.profiler;

import javax.inject.Inject;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Clock;
import java.time.ZonedDateTime;
import java.util.Objects;

import static java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME;

/**
 * Concrete implementation of the {@link Profiler}.
 */
final class ProfilerImpl implements Profiler {

  private final Clock clock;
  private final ProfilingState state = new ProfilingState();
  private final ZonedDateTime startTime;

  @Inject
  ProfilerImpl(Clock clock) {
    this.clock = Objects.requireNonNull(clock);
    this.startTime = ZonedDateTime.now(clock);
  }

  @Override
  public <T> T wrap(Class<T> klass, T delegate) {
    Objects.requireNonNull(klass);

    // get all of klass' methods
    Method[] methods = klass.getMethods();
    int profiledMethodsCount = 0;

    // check to see if any one of klass' mehtods has the @Profiled annotation
    for (Method currentMethod : methods) {
      if ((currentMethod.isAnnotationPresent(Profiled.class))) {
        profiledMethodsCount++;
      }
    }

    // if no @Profiled methods are found, throw an exception
    if (profiledMethodsCount == 0){
      throw new IllegalArgumentException();
    }

    // create a new proxy class that wraps klass and will call ProfilingMethodInterceptor as the handler
    Object proxy = Proxy.newProxyInstance(klass.getClassLoader(),
            new Class[]{klass},
            new ProfilingMethodInterceptor(clock, delegate, state));

    // return the proxy class masquarading as <T>
    return (T) proxy;
  }

  @Override
  public void writeData(Path path) throws IOException {
    // Create or append the file at path
    Writer writer = Files.newBufferedWriter(path, StandardOpenOption.CREATE, StandardOpenOption.APPEND);

    // writeData to the writer
    writeData(writer);

    // flush the writer
    writer.flush();
  }

  @Override
  public void writeData(Writer writer) throws IOException {
    writer.write("Run at " + RFC_1123_DATE_TIME.format(startTime));
    writer.write(System.lineSeparator());
    state.write(writer);
    writer.write(System.lineSeparator());
  }
}
