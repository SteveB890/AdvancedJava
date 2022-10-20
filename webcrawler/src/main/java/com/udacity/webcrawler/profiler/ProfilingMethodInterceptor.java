package com.udacity.webcrawler.profiler;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * A method interceptor that checks whether {@link Method}s are annotated with the {@link Profiled}
 * annotation. If they are, the method interceptor records how long the method invocation took.
 */
final class ProfilingMethodInterceptor implements InvocationHandler {

  private final Clock clock;
  private final Object target;
  private final ProfilingState state;

  // TODO: You will need to add more instance fields and constructor arguments to this class.
  ProfilingMethodInterceptor(Clock clock,
                             Object target,
                             ProfilingState state) {
    this.clock = Objects.requireNonNull(clock);
    this.target = target;
    this.state = state;
  }

  @Override
  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
    Instant start;  // the start time of a profiled method
    Object result;  // the result of the method call

    // Only record timing for profiled methods.
    if ((method.isAnnotationPresent(Profiled.class))) {
      start = clock.instant(); // start the clock

      // try to call the method and throw an exception if it fails
      try {
        result = method.invoke(target, args);
      }
      catch (InvocationTargetException e) {
        System.out.println("------------------------------------------------------------------");
        System.out.println(e);
        throw e.getTargetException();
      }
      finally {
        // stop the clock, calculate elapsed time and record the results.
        Instant end = clock.instant();
        Duration duration = Duration.between(start, end);
        state.record(target.getClass(), method, duration);
      }
    }
    else
    {
      try {
        // try to call the method and throw an exception if it fails
        result = method.invoke(target, args);
      }
      catch (InvocationTargetException e) {
        System.out.println("***************************************************************");
        System.out.println(e);
        throw e.getTargetException();
      }
    }

    // return the results of the method call.
    return result;
  }
}
