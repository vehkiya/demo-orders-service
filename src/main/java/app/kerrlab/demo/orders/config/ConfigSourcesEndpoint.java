package app.kerrlab.demo.orders.config;

import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.context.properties.source.ConfigurationProperty;
import org.springframework.boot.context.properties.source.ConfigurationPropertyName;
import org.springframework.boot.context.properties.source.ConfigurationPropertySource;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.boot.context.properties.source.IterableConfigurationPropertySource;
import org.springframework.boot.origin.Origin;
import org.springframework.boot.origin.PropertySourceOrigin;
import org.springframework.boot.origin.SystemEnvironmentOrigin;
import org.springframework.boot.origin.TextResourceOrigin;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

/**
 * {@code /actuator/configsources}: every property under the application's prefix with its effective value and the
 * layer it came from (jar default, env-wide override, service override, environment variable / secret).
 * <p>
 * Mounted override files are mapped back to their path in the demo-gitops repo, so the output points straight at the
 * file to change. Sensitive values are replaced by a fingerprint.
 */
@Component
@Endpoint(id = "configsources")
public class ConfigSourcesEndpoint {

    // Resources may be wrapped (e.g. OriginTrackedResource), so match on the description rather than the type
    private static final String CLASS_PATH_PREFIX = "class path resource [";

    private static final String SERVICE_OVERRIDE = "service override";

    private static final String ENV_OVERRIDE = "env override";

    private final ConfigurableEnvironment environment;

    public ConfigSourcesEndpoint(ConfigurableEnvironment environment) {
        this.environment = environment;
    }

    @ReadOperation
    public Report report() {
        String application = environment.getRequiredProperty("spring.application.name");
        String env = environment.getProperty("demo.environment", "local");
        ConfigurationPropertyName root = ConfigurationPropertyName.of(application);
        Map<String, Entry> properties = new TreeMap<>();
        // Sources are iterated from highest to lowest precedence, so the first hit is the effective value
        for (ConfigurationPropertySource source : ConfigurationPropertySources.get(environment)) {
            if (source instanceof IterableConfigurationPropertySource iterable) {
                iterable.stream()
                    .filter(root::isAncestorOf)
                    .forEach(name -> properties.computeIfAbsent(name.toString(),
                            key -> entry(key, iterable.getConfigurationProperty(name), application, env)));
            }
        }
        return new Report(application, env, properties);
    }

    private Entry entry(String key, ConfigurationProperty property, String application, String env) {
        String value = environment.resolvePlaceholders(String.valueOf(property.getValue()));
        if (Secrets.isSensitive(key)) {
            value = Secrets.fingerprint(value);
        }
        Origin origin = unwrap(property.getOrigin());
        if (origin instanceof SystemEnvironmentOrigin envOrigin) {
            return new Entry(value, "environment variable", "env " + envOrigin.getProperty());
        }
        if (origin instanceof TextResourceOrigin text) {
            String line = (text.getLocation() != null) ? ":" + (text.getLocation().getLine() + 1) : "";
            return fromFile(value, text.getResource(), line, sourceName(property), application, env);
        }
        return new Entry(value, sourceName(property), String.valueOf(origin));
    }

    private Entry fromFile(String value, Resource resource, String line, String sourceName, String application,
            String env) {
        String description = resource.getDescription();
        if (description.startsWith(CLASS_PATH_PREFIX)) {
            // Documents after the first one are only active conditionally (e.g. on-cloud-platform: kubernetes)
            boolean conditional = sourceName.contains("(document #") && !sourceName.contains("(document #0)");
            return new Entry(value, conditional ? "jar default (kubernetes)" : "jar default",
                    "classpath:" + description.substring(CLASS_PATH_PREFIX.length(), description.length() - 1) + line);
        }
        String path = path(resource);
        if (path.endsWith("/config/service.yaml")) {
            return new Entry(value, SERVICE_OVERRIDE, "envs/%s/%s/application.yaml%s".formatted(env, application, line));
        }
        if (path.endsWith("/config/env.yaml")) {
            return new Entry(value, ENV_OVERRIDE, "envs/%s/application.yaml%s".formatted(env, line));
        }
        int envs = path.indexOf("envs/");
        if (envs >= 0) {
            // Running locally against a demo-gitops checkout
            String repoPath = path.substring(envs);
            String layer = (repoPath.split("/").length > 3) ? SERVICE_OVERRIDE : ENV_OVERRIDE;
            return new Entry(value, layer, repoPath + line);
        }
        return new Entry(value, "external file", path + line);
    }

    /** Adapted sources wrap the real origin (file + line, env var) in a {@link PropertySourceOrigin}. */
    private static Origin unwrap(Origin origin) {
        Origin current = origin;
        while (current instanceof PropertySourceOrigin wrapper && wrapper.getOrigin() != null) {
            current = wrapper.getOrigin();
        }
        return current;
    }

    private static String path(Resource resource) {
        try {
            return resource.getURI().getPath();
        }
        catch (IOException ex) {
            return resource.getDescription();
        }
    }

    private static String sourceName(ConfigurationProperty property) {
        return (property.getSource().getUnderlyingSource() instanceof PropertySource<?> source) ? source.getName()
                : String.valueOf(property.getSource().getUnderlyingSource());
    }

    public record Report(String application, String environment, Map<String, Entry> properties) {
    }

    public record Entry(String value, String layer, String origin) {
    }
}
