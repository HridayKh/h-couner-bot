package in.HridayKh.hCounterBot;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;
import java.time.Duration;

@ApplicationScoped
public class RedditBot {
	private static final Logger LOG = Logger.getLogger(RedditBot.class);

	private volatile boolean running = true;

	void onStart(@Observes StartupEvent ev) {
		// Thread.ofVirtual().name("h-counter-bot-main-loop").start(this::runLoop);
	}

	private void runLoop() {
		while (running) {
			try {
				// We call a separate method so @WithSpan can wrap each iteration
				int delay = executeIteration();
				Thread.sleep(Duration.ofSeconds(delay));
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				running = false;
			} catch (Exception e) {
				LOG.error("Iteration failed", e);
				try {
					Thread.sleep(Duration.ofSeconds(3));
				} catch (InterruptedException ignored) {
				}
			}
		}
	}

	@WithSpan("reddit-bot-iteration") // Each loop cycle is now a unique Trace/Span
	protected int executeIteration() {
		// This is your "Wide Log" container for this specific loop
		Span span = Span.current();
		int delay = processRedditPosts();

		// Add "Wide Log" data to the span
		span.setAttribute("bot.iteration.delay_applied", delay);
		span.setAttribute("bot.status", "healthy");

		return delay;
	}

	@Transactional
	@WithSpan("process-reddit-posts")
	protected int processRedditPosts() {
		// Any data added here will be nested under the "Iteration" span
		Span.current().setAttribute("reddit.api.action", "polling");

		LOG.info("Polling Reddit API...");
		// logic...

		return 5; // delay in seconds until next iteration
	}

	void onStop(@Observes ShutdownEvent ev) {
		running = false;
	}
}