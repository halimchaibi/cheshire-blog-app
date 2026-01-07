package io.blog.pipeline;

import io.cheshire.spi.pipeline.Context;
import io.cheshire.spi.pipeline.MaterializedInput;
import io.cheshire.spi.pipeline.step.PreProcessor;
import io.blog.BlogApp;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BinaryOperator;
import java.util.stream.Collectors;

/**
 * Pre-processor for blog operations in the Cheshire pipeline.
 * <p>
 * This processor implements the first stage of the three-stage pipeline architecture:
 * {@code PreProcessor → Executor → PostProcessor}
 * <p>
 * <strong>Responsibilities:</strong>
 * <ul>
 *   <li>Validate input parameters and structure</li>
 *   <li>Transform and normalize data before execution</li>
 *   <li>Filter entries based on business rules</li>
 *   <li>Enrich metadata with execution timestamps</li>
 * </ul>
 * <p>
 * The processor operates on {@link MaterializedInput} which contains both data payload
 * and metadata. It preserves immutability by creating new transformed maps.
 * <p>
 * Configuration is loaded from pipeline YAML definitions via the {@code template} and
 * {@code name} fields.
 *
 * @see PreProcessor
 * @see MaterializedInput
 */
@Slf4j
public final class BlogInputProcessor implements PreProcessor<MaterializedInput> {

    private final String template;
    private final String name;
    // TODO: Placeholder for key collision handling - currently overwrites with new value
    private final BinaryOperator<Object> overwriteWithNew = (_, replacement) -> replacement;

    /**
     * Constructs a BlogInputProcessor from pipeline configuration.
     * <p>
     * Configuration map typically contains:
     * <ul>
     *   <li>{@code template}: JSON validation/transformation rules</li>
     *   <li>{@code name}: processor identifier for logging and tracing</li>
     * </ul>
     *
     * @param config configuration map from pipeline YAML
     */
    public BlogInputProcessor(final Map<String, Object> config) {
        this.template = (String) config.get("template");
        this.name = (String) config.get("name");
    }

    /**
     * Applies pre-processing transformations to the input.
     * <p>
     * <strong>Processing steps:</strong>
     * <ol>
     *   <li>Filter data entries via {@link #filterByEntry}</li>
     *   <li>Transform keys via {@link #transformKey}</li>
     *   <li>Transform values via {@link #transformValue}</li>
     *   <li>Filter and preserve metadata</li>
     *   <li>Add execution timestamp to metadata</li>
     * </ol>
     * <p>
     * All transformations maintain immutability by creating new map instances.
     *
     * @param preInput the input data and metadata before preprocessing
     * @param ctx      execution context for accessing runtime state
     * @return transformed MaterializedInput with enriched metadata
     */
    @Override
    public MaterializedInput apply(final MaterializedInput preInput, final Context ctx) {

        final Map<String, Object> transformedData = preInput.data().entrySet().stream()
                .filter(this::filterByEntry)
                .collect(Collectors.toMap(
                        entry -> transformKey(entry.getKey()),
                        entry -> transformValue(entry.getValue()),
                        overwriteWithNew,
                        HashMap::new
                ));

        final Map<String, Object> transformedMetaData = preInput.metadata().entrySet().stream()
                .filter(this::filterByEntry)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        overwriteWithNew,
                        HashMap::new
                ));

        transformedMetaData.put("pre-processor-executed-at", Instant.now().toString());

        return MaterializedInput.of(transformedData, transformedMetaData);
    }

    /**
     * Filters map entries based on business rules.
     * <p>
     * TODO: Placeholder for entry-level filtering logic
     * <p>
     * Current implementation accepts all entries. Can be extended to:
     * <ul>
     *   <li>Remove null or empty values</li>
     *   <li>Filter based on field naming patterns</li>
     *   <li>Apply security-based field filtering</li>
     * </ul>
     *
     * @param entry the map entry to evaluate
     * @return {@code true} to keep the entry, {@code false} to filter it out
     */
    private boolean filterByEntry(final Map.Entry<String, Object> entry) {
        return true;
    }

    /**
     * Transforms values before passing to the executor.
     * <p>
     * TODO: Placeholder for value transformation logic
     * <p>
     * Current implementation is pass-through. Can be extended for:
     * <ul>
     *   <li>Type coercion (String → Integer, Date formatting)</li>
     *   <li>Value normalization (trim strings, lowercase)</li>
     *   <li>Default value injection</li>
     * </ul>
     *
     * @param value the value to transform
     * @return transformed value
     */
    private Object transformValue(final Object value) {
        return value;
    }

    /**
     * Transforms keys (field names) during preprocessing.
     * <p>
     * TODO: Placeholder for key transformation logic
     * <p>
     * Current implementation is pass-through. Can be extended for:
     * <ul>
     *   <li>Case transformation (camelCase ↔ snake_case)</li>
     *   <li>Field aliasing</li>
     *   <li>Prefix/suffix handling</li>
     * </ul>
     *
     * @param key the key to transform
     * @return transformed key
     */
    private String transformKey(final String key) {
        return key;
    }
}

