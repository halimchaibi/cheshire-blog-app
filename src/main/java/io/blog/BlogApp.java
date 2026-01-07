package io.blog;

import io.cheshire.core.CheshireBootstrap;
import io.cheshire.core.CheshireSession;
import io.cheshire.runtime.CheshireRuntime;
import lombok.extern.slf4j.Slf4j;

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
 *   <li>{@code --rest}: REST API on /api/v1 (default)</li>
 *   <li>{@code --mcp-stdio}: MCP via standard I/O for direct integration</li>
 *   <li>{@code --mcp-http}: MCP over HTTP on /mcp/v1</li>
 * </ul>
 *
 * @see CheshireBootstrap
 * @see CheshireSession
 * @see CheshireRuntime
 */
@Slf4j
public final class BlogApp {

    private static final String DEFAULT_CONFIG = "blog-rest.yaml";
    private static final String REST_CONFIG = "blog-rest.yaml";
    private static final String MCP_STDIO_CONFIG = "blog-mcp-stdio.yaml";
    private static final String MCP_HTTP_CONFIG = "blog-mcp-streamable-http.yaml";

    /**
     * Application entry point.
     * <p>
     * Accepts CLI arguments to select the exposure configuration:
     * <pre>
     * java -jar blog-app.jar --rest       # REST API mode (default)
     * java -jar blog-app.jar --mcp-stdio  # MCP stdio mode
     * java -jar blog-app.jar --mcp-http   # MCP HTTP mode
     * </pre>
     *
     * @param args command-line arguments for configuration selection
     */
    public static void main(final String[] args) {
        log.info("Starting Blog Application...");

        final String configFile = selectConfigFromArgs(args);
        System.setProperty("cheshire.config", configFile);
        log.info("Using configuration: {}", configFile);

        try {
            final CheshireSession session =
                    CheshireBootstrap
                            .fromClasspath("config")
                            .build();

            final CheshireRuntime runtime =
                    CheshireRuntime.expose(session).start();

            log.info("Blog application started successfully");
            runtime.awaitTermination();

        } catch (final Exception e) {
            log.error("Fatal startup error", e);
            System.exit(1);
        }
    }

    /**
     * Selects the configuration file based on command-line arguments.
     * <p>
     * Scans arguments for {@code --rest}, {@code --mcp-stdio}, or {@code --mcp-http} flags.
     * Falls back to {@link #DEFAULT_CONFIG} if no valid argument is found.
     *
     * @param args command-line arguments array
     * @return selected configuration filename
     */
    private static String selectConfigFromArgs(final String[] args) {
        if (args.length == 0) {
            log.info("No arguments provided, using default configuration: {}", DEFAULT_CONFIG);
            return DEFAULT_CONFIG;
        }

        final Optional<String> configArg = Arrays.stream(args)
                .filter(arg -> arg.startsWith("--"))
                .findFirst();

        return configArg
                .map(BlogApp::mapArgToConfig)
                .orElse(DEFAULT_CONFIG);
    }

    /**
     * Maps a command-line argument to its corresponding configuration file.
     * <p>
     * Supported mappings:
     * <ul>
     *   <li>{@code --rest} → {@link #REST_CONFIG}</li>
     *   <li>{@code --mcp-stdio} → {@link #MCP_STDIO_CONFIG}</li>
     *   <li>{@code --mcp-http} → {@link #MCP_HTTP_CONFIG}</li>
     * </ul>
     *
     * @param arg command-line argument starting with {@code --}
     * @return configuration filename, or {@link #DEFAULT_CONFIG} if argument is unrecognized
     */
    private static String mapArgToConfig(final String arg) {
        return switch (arg) {
            case "--rest" -> {
                log.info("REST API mode selected");
                yield REST_CONFIG;
            }
            case "--mcp-stdio" -> {
                log.info("MCP stdio mode selected");
                yield MCP_STDIO_CONFIG;
            }
            case "--mcp-http" -> {
                log.info("MCP HTTP mode selected");
                yield MCP_HTTP_CONFIG;
            }
            default -> {
                log.warn("Unknown argument: {}, using default configuration", arg);
                yield DEFAULT_CONFIG;
            }
        };
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
    private static void startMetricsObserver(final CheshireRuntime runtime) {
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

