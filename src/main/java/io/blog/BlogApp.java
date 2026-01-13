package io.blog;

import io.cheshire.core.CheshireBootstrap;
import io.cheshire.core.CheshireSession;
import io.cheshire.runtime.CheshireRuntime;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;

/**
 * Main entry point for the Blog Application.
 * <p>
 * This application exposes blog operations (Authors, Articles, Comments) through the Cheshire framework
 * using multiple protocol exposures: REST API, MCP stdio, and MCP streamable HTTP.
 * <p>
 * The application follows the Cheshire Bootstrap architecture:
 * <ol>
 *   <li>Loads configuration from classpath based on CLI argument</li>
 *   <li>Builds a CheshireSession with capabilities, sources, and pipelines</li>
 *   <li>Starts the CheshireRuntime with the selected exposure (REST/MCP)</li>
 *   <li>Awaits termination via shutdown hooks</li>
 * </ol>
 * <p>
 * Configuration Selection:
 * <ul>
 *   <li>{@code --config <file>}: Main configuration file (default: blog-rest.yaml)</li>
 *   <li>{@code --log-file <path>}: Log file path (default: /tmp/blog-app.log)</li>
 *   <li>{@code --log-metrics}: Enable runtime metrics logging</li>
 *   <li>{@code --redirect-stderr}: Redirect stderr to log file</li>
 * </ul>
 * <p>
 * Configuration Files:
 * <ul>
 *   <li>{@code blog-rest.yaml}: Expose via REST API on HTTP</li>
 *   <li>{@code blog-mcp-stdio.yaml}: Expose via MCP stdio for CLI/AI assistants</li>
 *   <li>{@code blog-mcp-streamable-http.yaml}: Expose via MCP over HTTP with SSE</li>
 * </ul>
 *
 * @see CheshireBootstrap
 * @see CheshireSession
 * @see CheshireRuntime
 */
@Slf4j
@CommandLine.Command(name = "blog-app", mixinStandardHelpOptions = true, version = "1.0",
        description = "Starts the Cheshire Blog Application.")
public final class BlogApp implements Runnable {

    @CommandLine.Option(names = {"-c", "--config"}, description = "Path to the config file.", defaultValue = "blog-rest.yaml")
    private String configFile;

    @CommandLine.Option(names = {"-l", "--log-file"}, description = "Log file path.", defaultValue = "/tmp/blog-app.log")
    private String logFile;

    @CommandLine.Option(names = {"-m", "--log-metrics"}, description = "Enable runtime metrics logging.")
    private boolean logMetrics;

    @CommandLine.Option(names = {"-r", "--redirect-stderr"}, description = "Redirect stderr to log file.")
    private boolean redirectStdIO;

    private static final String DEFAULT_CONFIG = "blog-rest.yaml";
    private static final String DEFAULT_LOG_FILE ="blog-app.log";

    /**
     * Application entry point.
     * <p>
     * Accepts CLI arguments to select the exposure configuration:
     * <pre>
     * java -jar blog-app.jar                                      # REST API mode (default)
     * java -jar blog-app.jar --config blog-rest.yaml              # REST API mode (explicit)
     * java -jar blog-app.jar --config blog-mcp-stdio.yaml         # MCP stdio mode
     * java -jar blog-app.jar --config blog-mcp-streamable-http.yaml  # MCP HTTP mode
     * </pre>
     *
     * @param args command-line arguments for configuration selection
     */

    public static void main(final String[] args) {
        int exitCode = new CommandLine(new BlogApp()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public void run() {
        try {
            setupEnvironment();

            final CheshireSession session = CheshireBootstrap
                    .fromClasspath("config")
                    .build();

            final CheshireRuntime runtime = CheshireRuntime.expose(session).start();

            if (logMetrics) {
                log.debug("Metrics observation enabled.");
                logMetricsObserver(runtime);
            }

            log.info("Blog application started successfully using: {}", configFile);

            runtime.awaitTermination();
        } catch (Exception e) {
            log.error("Fatal startup error", e);
        }
    }

    private void setupEnvironment() throws IOException {
        System.setProperty("jackson.json.discovery", "true");
        System.setProperty("jackson.serialization.write_dates_as_timestamps", "false");
        System.setProperty("cheshire.config", configFile);

        if (redirectStdIO) {
            Path path = Paths.get(logFile);
            if (path.getParent() != null) Files.createDirectories(path.getParent());
            System.setErr(new PrintStream(new FileOutputStream(path.toFile(), true)));;
        }
    }


    /**
     * Starts a background metrics observer for runtime health monitoring.
     * <p>
     * Creates a virtual thread that periodically logs runtime metrics snapshots.
     * This method is currently unused but available for diagnostic purposes.
     * <p>
     * <strong>Note:</strong> Observer runs indefinitely until runtime stops or thread is interrupted.
     *
     * @param runtime the CheshireRuntime instance to monitor
     */
    private static void logMetricsObserver(final CheshireRuntime runtime) {
        Thread.ofVirtual().name("metrics-observer").start(() -> {
            while (runtime.isRunning()) {
                try {
                    Thread.sleep(Duration.ofSeconds(20));
                    log.debug("Runtime Health: {}", runtime.getMetrics().getSnapshot().toJson());
                } catch (final InterruptedException i) {
                    Thread.currentThread().interrupt();
                    log.error("Interrupted while waiting for health check", i);
                    break;
                } catch (final Exception e) {
                    Thread.currentThread().interrupt();
                    log.error("Observer unknown error: ", e);
                    break;
                }
            }
        });
    }

    private static final com.google.gson.Gson GSON =
            new com.google.gson.GsonBuilder().setPrettyPrinting().create();

    /**
     * Converts an object to a pretty-printed JSON string.
     * <p>
     * Used for debug logging and diagnostic output of complex objects.
     *
     * @param obj the object to serialize
     * @return JSON string representation with pretty printing
     */
    public static String stringify(final Object obj) {
        return GSON.toJson(obj);
    }
}

