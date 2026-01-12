package io.blog.pipeline;

import io.cheshire.spi.pipeline.Context;
import io.cheshire.core.pipeline.MaterializedOutput;
import io.cheshire.spi.pipeline.step.PostProcessor;
import io.cheshire.spi.query.result.MapQueryResult;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Post-processor for blog operations in the Cheshire pipeline.
 * <p>
 * This processor implements the third (final) stage of the three-stage pipeline:
 * {@code PreProcessor → Executor → PostProcessor}
 * <p>
 * <strong>Responsibilities:</strong>
 * <ul>
 *   <li>Format and enrich output before returning to client</li>
 *   <li>Apply protocol-specific transformations</li>
 *   <li>Add execution metadata and timestamps</li>
 *   <li>Log output structure for debugging</li>
 * </ul>
 * <p>
 * <strong>Current Implementation:</strong>
 * Pass-through processor that preserves executor output while adding metadata.
 * This design allows the executor's standardized output format to flow directly
 * to the protocol adapter (REST/MCP).
 * <p>
 * <strong>Extensibility:</strong>
 * Can be extended for:
 * <ul>
 *   <li>Field name transformation (database → API naming)</li>
 *   <li>Data masking for sensitive fields</li>
 *   <li>Response pagination envelope wrapping</li>
 *   <li>Protocol-specific formatting</li>
 * </ul>
 *
 * @see PostProcessor
 * @see MaterializedOutput
 */
@Slf4j
public final class BlogOutputProcessor implements PostProcessor<MaterializedOutput> {

    /**
     * Applies post-processing to the output.
     * <p>
     * <strong>Processing steps:</strong>
     * <ol>
     *   <li>Log output structure for diagnostics</li>
     *   <li>Add post-processor execution timestamp to context</li>
     *   <li>Return output (currently pass-through)</li>
     * </ol>
     * <p>
     * <strong>Note:</strong> Output data is not transformed in current implementation
     * to avoid data loss. The standardized format from {@link BlogExecutor} is
     * preserved through to the protocol adapter.
     *
     * @param postInput the output data and metadata from executor
     * @param ctx       execution context for runtime state
     * @return processed MaterializedOutput with enriched context metadata
     */
    @Override
    public MaterializedOutput apply(final MaterializedOutput postInput, final Context ctx) {

        final Object dataPayload = postInput.data().get("data");

        log.debug("Received keys: {}", postInput.data().keySet());

        if (dataPayload instanceof java.util.List<?> list) {
            log.debug("[PASS-THROUGH] Data is a List. Size: {}", list.size());
        } else if (dataPayload != null) {
            log.debug("[PASS-THROUGH] Data is type: {}", dataPayload.getClass().getName());
        } else {
            log.warn("[PASS-THROUGH] Data key is NULL or missing!");
        }

        ctx.putIfAbsent("post-processor-at", Instant.now().toString());

        return postInput;
    }

    /**
     * Converts {@link MapQueryResult} to enriched output format with pagination.
     * <p>
     * <strong>Note:</strong> This method is currently unused but demonstrates
     * an alternative output format with enhanced pagination metadata.
     * <p>
     * <strong>Output structure:</strong>
     * <pre>{@code
     * {
     *   "data": [...rows...],
     *   "pagination": {
     *     "totalRows": <count>,
     *     "pageSize": <size>,
     *     "currentPage": 1,
     *     "totalPages": 1
     *   },
     *   "columns": [...metadata...]
     * }
     * }</pre>
     * <p>
     * Can be activated if enhanced pagination is needed for specific protocols.
     *
     * @param result query execution result from engine
     * @return map with pagination envelope
     */
    private LinkedHashMap<String, Object> toMap(final MapQueryResult result) {

        final Map<String, Object> pagination = new LinkedHashMap<>();
        pagination.put("totalRows", result.rowCount());
        pagination.put("pageSize", result.rows().size());
        pagination.put("currentPage", 1);
        pagination.put("totalPages", 1);

        final LinkedHashMap<String, Object> response = new LinkedHashMap<>();
        response.put("data", result.rows());
        response.put("pagination", pagination);
        response.put("columns", result.columns());

        return response;
    }
}

