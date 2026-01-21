package io.blog.pipeline;

import io.cheshire.core.constant.Key;
import io.cheshire.query.engine.jdbc.JdbcQueryEngine;
import io.cheshire.query.engine.jdbc.SqlQueryEngineRequest;
import io.cheshire.query.engine.jdbc.SqlTemplateQueryBuilder;
import io.cheshire.runtime.CheshireRuntimeError;
import io.cheshire.source.jdbc.JdbcSourceProvider;
import io.cheshire.spi.pipeline.Context;
import io.cheshire.core.pipeline.MaterializedInput;
import io.cheshire.core.pipeline.MaterializedOutput;
import io.cheshire.spi.pipeline.exception.PipelineException;
import io.cheshire.spi.pipeline.step.Executor;
import io.cheshire.spi.query.engine.QueryEngine;
import io.cheshire.spi.query.exception.QueryEngineException;
import io.cheshire.spi.query.exception.QueryExecutionException;
import io.cheshire.spi.query.request.QueryEngineContext;
import io.cheshire.spi.query.result.QueryEngineResult;
import io.cheshire.spi.source.SourceProvider;
import io.blog.BlogApp;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Core executor for blog operations in the Cheshire pipeline.
 * <p>
 * This executor implements the second (execution) stage of the three-stage pipeline:
 * {@code PreProcessor → Executor → PostProcessor}
 * <p>
 * <strong>Responsibilities:</strong>
 * <ul>
 *   <li>Build SQL queries from domain-neutral DSL templates</li>
 *   <li>Resolve query parameters from input data</li>
 *   <li>Execute queries via JDBC Query Engine</li>
 *   <li>Convert query results to canonical output format</li>
 *   <li>Enrich metadata with execution details</li>
 * </ul>
 * <p>
 * <strong>Query Resolution Flow:</strong>
 * <ol>
 *   <li>Extract DSL template from configuration</li>
 *   <li>Resolve runtime parameters from input payload</li>
 *   <li>Build {@link SqlQueryEngineRequest} via {@link SqlTemplateQueryBuilder}</li>
 *   <li>Execute query through {@link JdbcQueryEngine}</li>
 *   <li>Map {@link QueryEngineResult} to {@link MaterializedOutput}</li>
 * </ol>
 * <p>
 * Templates follow the domain-neutral DSL specification supporting:
 * SELECT, INSERT, UPDATE, DELETE operations with filters, joins, aggregates, and pagination.
 *
 * @see Executor
 * @see SqlTemplateQueryBuilder
 * @see JdbcQueryEngine
 */
@Slf4j
public final class BlogExecutor implements Executor<MaterializedInput, MaterializedOutput> {

    private final String template;
    private final String name;

    /**
     * Constructs a BlogExecutor from pipeline configuration.
     * <p>
     * Configuration map contains:
     * <ul>
     *   <li>{@code template}: DSL_QUERY JSON template for query building</li>
     *   <li>{@code name}: executor identifier for logging and tracing</li>
     * </ul>
     *
     * @param config configuration map from pipeline YAML
     */
    public BlogExecutor(final Map<String, Object> config) {
        this.template = (String) config.get("template");
        this.name = (String) config.get("name");
    }

    /**
     * Executes the query operation and produces output.
     * <p>
     * <strong>Execution flow:</strong>
     * <ol>
     *   <li>Add execution timestamp to context</li>
     *   <li>Preserve input metadata and add executor info</li>
     *   <li>Build resolved SQL query from template + parameters</li>
     *   <li>Execute query via JDBC engine</li>
     *   <li>Convert query result to standard output format</li>
     * </ol>
     * <p>
     * Metadata enrichment includes:
     * <ul>
     *   <li>{@code executor-name}: processor name from config</li>
     *   <li>{@code executor-template-received}: original DSL template</li>
     *   <li>{@code executor-template-resolved}: resolved SQL with parameters</li>
     * </ul>
     *
     * @param executorInput preprocessed input with data and metadata
     * @param ctx          execution context for runtime state
     * @return MaterializedOutput containing query results and enriched metadata
     * @throws PipelineException if query building or execution fails
     */
    @Override
    public MaterializedOutput apply(final MaterializedInput executorInput, final Context ctx)
            throws PipelineException {

        try {
            log.debug("Executing materialized query {}", executorInput);
            ctx.putIfAbsent("executed-at", Instant.now().toString());

            final LinkedHashMap<String, Object> outputMetaData =
                    new LinkedHashMap<>(executorInput.metadata());
            outputMetaData.put("executor-name", name);

            final ResolvedQuery resolved =
                    buildResolvedQuery(template, executorInput.data());
            outputMetaData.put(
                    "executor-template-resolved",
                    resolved.resolvedSql()
            );

            final QueryEngineResult result =
                    executeJdbcQuery(resolved.request(), executorInput);

            final LinkedHashMap<String, Object> data = toMap(result);
            log.debug("Returning materialized output {}", data);
            return MaterializedOutput.of(data, outputMetaData);

        } catch (final QueryExecutionException e) {
            log.error("Query execution failed", e);
            throw new PipelineException("Query execution failed", e);
        } catch (final Exception e) {
            log.error("Unexpected execution failure", e);
            throw new PipelineException("Unexpected execution failure", e);
        }
    }

    /**
     * Executes a SQL query using the JDBC query engine.
     * <p>
     * <strong>Resolution sequence:</strong>
     * <ol>
     *   <li>Extract {@link JdbcQueryEngine} from input metadata</li>
     *   <li>Resolve {@link JdbcSourceProvider} from sources</li>
     *   <li>Execute query and return results</li>
     * </ol>
     * <p>
     * The engine is resolved from metadata using the {@link Key#ENGINE} key,
     * which is injected by the framework during capability initialization.
     *
     * @param query the resolved SQL query request with parameters
     * @param input materialized input containing engine and source metadata
     * @return query execution results as {@link QueryEngineResult}
     * @throws QueryExecutionException if the query execution fails
     * @throws PipelineException       if engine or source provider is not found
     */
    private QueryEngineResult executeJdbcQuery(
            final SqlQueryEngineRequest query,
            final MaterializedInput input
    ) throws QueryEngineException, PipelineException {

        final Optional<JdbcQueryEngine> engine =
                input.getMetadata(Key.ENGINE.key(), JdbcQueryEngine.class);

        final JdbcQueryEngine jdbc = engine.orElseThrow(() ->
                new QueryExecutionException(
                        "No JDBC engine found in input metadata"
                )
        );

        final JdbcSourceProvider provider =
                getSourceProvider(JdbcSourceProvider.class, input);

        QueryEngineContext ctx = new QueryEngineContext(
                "session-123",
                "user-456",
                "trace-789",
                Map.of(),
                List.of(provider),
                new ConcurrentHashMap<>(),
                Instant.now(),
                Instant.now()
        );

        return jdbc.execute(query, ctx);
    }

    /**
     * Generic query execution supporting multiple query engine types.
     * <p>
     * Currently supports {@link JdbcQueryEngine}. Can be extended for:
     * <ul>
     *   <li>CalciteQueryEngine (federated queries)</li>
     *   <li>Custom query engines via SPI</li>
     * </ul>
     * <p>
     * <strong>Note:</strong> This method demonstrates the extensibility pattern
     * but is not currently used. The framework uses {@link #executeJdbcQuery} directly.
     *
     * @param input materialized input with engine metadata
     * @param ctx   execution context
     * @return query results
     * @throws PipelineException if execution fails or engine type is unsupported
     */
    private QueryEngineResult executeQuery(final MaterializedInput input, final Context ctx)
            throws QueryEngineException, PipelineException {

        final QueryEngine<?> engine =
                input.require(Key.ENGINE.key(), QueryEngine.class);

        switch (engine) {
            case JdbcQueryEngine jdbc -> {
                log.debug("Preparing JDBC-specific execution path");
                final SqlQueryEngineRequest query =
                        new SqlQueryEngineRequest("SELECT 1", Map.of(), "postgres");
                try {
                    final JdbcSourceProvider provider =
                            getSourceProvider(
                                    JdbcSourceProvider.class,
                                    input
                            );
                    return jdbc.execute(query, QueryEngineContext.empty());
                } catch (final QueryExecutionException e) {
                    log.error("JDBC query execution failed", e);
                    throw new PipelineException(
                            "JDBC query execution failed",
                            e
                    );
                }
            }
            case null ->
                    throw new PipelineException(
                            "Engine key was provided but no implementation found"
                    );
            default -> {
                log.info(
                        "Using generic execution path for engine type: {}",
                        engine.getClass()
                );
                return new QueryEngineResult(List.of(), List.of());
            }
        }
    }

    /**
     * Builds a resolved SQL query from a DSL template and runtime parameters.
     * <p>
     * <strong>Resolution process:</strong>
     * <ol>
     *   <li>Extract query parameters from input data payload</li>
     *   <li>Parse DSL_QUERY JSON template</li>
     *   <li>Bind runtime parameters to template placeholders</li>
     *   <li>Generate executable {@link SqlQueryEngineRequest}</li>
     * </ol>
     * <p>
     * Parameters are extracted from the {@link Key#PAYLOAD_PARAMETERS} key
     * in the input data map. The template uses parameterized placeholders
     * to prevent SQL injection.
     * <p>
     * Template format follows the DSL_QUERY specification with operation types:
     * SELECT, INSERT, UPDATE, DELETE with filters, joins, aggregates, and sorting.
     *
     * @param template   DSL_QUERY JSON string from pipeline configuration
     * @param parameters input data map containing payload and parameters
     * @return resolved query with bound parameters and generated SQL
     * @throws PipelineException if template parsing or parameter binding fails
     * @see SqlTemplateQueryBuilder#buildQuery
     */
    private ResolvedQuery buildResolvedQuery(
            final String template,
            final Map<String, Object> parameters
    ) throws PipelineException {

        try {
            @SuppressWarnings("unchecked")
            final Map<String, Object> queryParameters =
                    (Map<String, Object>) parameters.getOrDefault(
                            Key.PAYLOAD_PARAMETERS.key(),
                            new HashMap<String, Object>()
                    );

            log.debug(
                    "Resolving SQL template with parameters: {}",
                    BlogApp.stringify(queryParameters)
            );

            final SqlQueryEngineRequest request =
                    SqlTemplateQueryBuilder.buildQuery(
                            template,
                            queryParameters
                    );

            return new ResolvedQuery(request, request.sqlQuery());

        } catch (final Exception e) {
            log.error("Failed to resolve SQL template");
            throw new PipelineException(
                    "Failed to resolve SQL query template",
                    e
            );
        }
    }

    /**
     * Resolves a source provider from input metadata by type.
     * <p>
     * Source providers are injected by the framework during capability initialization.
     * They are stored in metadata under the {@link Key#SOURCES} key as a map of
     * source name to provider instance.
     * <p>
     * <strong>Resolution flow:</strong>
     * <ol>
     *   <li>Extract sources map from metadata</li>
     *   <li>Filter by target type (e.g., {@link JdbcSourceProvider})</li>
     *   <li>Return first matching provider</li>
     * </ol>
     * <p>
     * <strong>Type safety:</strong> Uses generics to return the specific provider type
     * without unchecked casts.
     *
     * @param <T>          source provider type
     * @param targetSource class of the desired provider type
     * @param input        materialized input containing source metadata
     * @return resolved source provider instance
     * @throws PipelineException       if sources map is not found in metadata
     * @throws CheshireRuntimeError if no provider of the target type is found
     */
    private <T extends SourceProvider<?>> T getSourceProvider(
            final Class<T> targetSource,
            final MaterializedInput input
    ) throws PipelineException {

        final Map<String, SourceProvider<?>> sources =
                input.getMetadata(Key.SOURCES.key(), Map.class)
                        .orElseThrow(() ->
                                new PipelineException(
                                        "No sources found in input metadata"
                                )
                        );

        return sources.values().stream()
                .filter(targetSource::isInstance)
                .map(targetSource::cast)
                .findFirst()
                .orElseThrow(() ->
                        new CheshireRuntimeError(
                                "No source found of type: "
                                        + targetSource.getSimpleName()
                        )
                );
    }

    /**
     * Converts {@link QueryEngineResult} to a standardized output format.
     * <p>
     * <strong>Output structure:</strong>
     * <pre>{@code
     * {
     *   "data": [...rows...],
     *   "count": {
     *     "totalRows": <total count>,
     *     "pageSize": <returned rows>
     *   },
     *   "columns": [...column metadata...]
     * }
     * }</pre>
     * <p>
     * This format provides both data and metadata, supporting:
     * <ul>
     *   <li>Pagination information</li>
     *   <li>Column type information</li>
     *   <li>Result set statistics</li>
     * </ul>
     *
     * @param result query execution result from engine
     * @return map suitable for serialization to JSON response
     */
    private LinkedHashMap<String, Object> toMap(final QueryEngineResult result) {

        final LinkedHashMap<String, Object> count = new LinkedHashMap<>();
        count.put("total_found",
                result.rows().stream().findFirst()
                        .map(row -> row.get("total_found"))
                        .orElse(result.rows().size())
        );
        count.put("page_size", result.rows().size());

        final LinkedHashMap<String, Object> response = new LinkedHashMap<>();
        response.put("count", count);
        response.put("data", result.rows());
        response.put("columns", result.columns());

        return response;
    }

    /**
     * Internal record holding a resolved query with its SQL representation.
     * <p>
     * Used to pass both the parameterized {@link SqlQueryEngineRequest} and the
     * resolved SQL string through the execution pipeline for logging and debugging.
     *
     * @param request     the executable SQL query request with bound parameters
     * @param resolvedSql the final SQL string after parameter resolution
     */
    private record ResolvedQuery(
            SqlQueryEngineRequest request,
            String resolvedSql
    ) {}
}

