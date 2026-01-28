package in.HridayKh.hCounterBot.reddit.bot;

import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.quarkus.logging.Log;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import java.time.Duration;

@ApplicationScoped
public class BotRunner {

	private volatile boolean running = true;

	@Inject
	Tracer tracer;

	@Inject
	BotLoop botLoop;

	void onStart(@Observes StartupEvent ev) {
		try {
			// Delay before starting so logs appear correctly when restarting in dev mode
			Thread.sleep(Duration.ofSeconds(5));
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			running = false;
		}

		Thread.ofVirtual().name("h-counter-bot-main-loop").start(this::runLoop);
	}

	private void runLoop() {
		while (running) {
			try {
				Log.info("Reddit Bot iteration started.");

				botLoop.executeIteration();

				// // Temporary: Stop after one iteration for testing
				// running = false;

				Log.info("Reddit Bot iteration completed.");
			} catch (Exception e) {
				Log.error("Iteration failed", e);
				tracer.spanBuilder("Failed Iteration").startSpan().recordException(e).setStatus(
						StatusCode.ERROR, "Iteration failed due to unknown exception").end();
				try {
					Thread.sleep(Duration.ofSeconds(3));
				} catch (InterruptedException ignored) {
				}
			}

		}
	}

	void onStop(@Observes ShutdownEvent ev) {
		running = false;
	}

}