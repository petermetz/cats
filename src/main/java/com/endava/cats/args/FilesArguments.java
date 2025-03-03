package com.endava.cats.args;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.github.ludovicianul.prettylogger.PrettyLogger;
import io.github.ludovicianul.prettylogger.PrettyLoggerFactory;
import jakarta.inject.Singleton;
import lombok.Getter;
import lombok.Setter;
import org.fusesource.jansi.Ansi;
import picocli.CommandLine;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Holds all arguments related to different files used by CATS like: headers, reference data, etc.
 */
@Singleton
public class FilesArguments {
    private static final String ALL = "all";
    private final PrettyLogger log = PrettyLoggerFactory.getLogger(this.getClass());
    private Map<String, Map<String, Object>> headers;
    private Map<String, Map<String, Object>> queryParams;
    private Map<String, Map<String, Object>> refData;

    @Getter
    private Map<String, Map<String, Object>> customFuzzerDetails = new HashMap<>();
    @Getter
    private Map<String, Map<String, Object>> securityFuzzerDetails = new HashMap<>();

    @CommandLine.Option(names = {"--urlParams"},
            description = "A comma separated list of @|bold name:value|@ pairs of parameters to be replaced inside the URLs", split = ",")
    @Getter
    private List<String> params;

    @CommandLine.Option(names = {"--headers"},
            description = "Specifies custom headers that will be passed along with request. This can be used to pass oauth or JWT tokens for authentication purposed for example")
    @Getter
    @Setter
    private File headersFile;

    @CommandLine.Option(names = {"-H"},
            description = "Specifies the headers that will be passed along with the request. When supplied it will be applied to ALL paths. For per-path control use the `--headers` arg that requires a file.")
    @Setter
    @Getter
    Map<String, Object> headersMap;

    @CommandLine.Option(names = {"--queryParams"},
            description = "Specifies additional query parameters that will be passed along with request. This can be used to pass non-documented query params")
    @Getter
    @Setter
    private File queryFile;

    @CommandLine.Option(names = {"--refData"},
            description = "Specifies the file with fields that must have a fixed value in order for requests to succeed. " +
                    "If this is supplied when @|bold FunctionalFuzzer|@ is also enabled, the @|bold FunctionalFuzzer|@ will consider it a @|bold refData|@ template and try to replace any variables")
    @Getter
    @Setter
    private File refDataFile;

    @CommandLine.Option(names = {"--functionalFuzzerFile"},
            description = "Specifies the file used by the @|bold FunctionalFuzzer|@ that will be used to create user-supplied payloads and tests")
    @Getter
    @Setter
    private File customFuzzerFile;

    @CommandLine.Option(names = {"--securityFuzzerFile"},
            description = "Specifies the file used by the @|bold SecurityFuzzer|@ that will be used to inject special strings in order to exploit possible vulnerabilities")
    @Getter
    @Setter
    private File securityFuzzerFile;

    @CommandLine.Option(names = {"--createRefData"},
            description = "This is only applicable when enabling the @|bold FunctionalFuzzer |@. It will instruct the @|bold FunctionalFuzzer|@ to create a @|bold,underline --refData|@ file " +
                    "with all the paths defined in the @|bold,underline customFuzzerFile|@ and the corresponding @|bold output|@ variables")
    @Getter
    @Setter
    private boolean createRefData;

    /**
     * Loads all supplied files for --securityFuzzerFile, --customFuzzerFile, --refData, --urlParams and --headers.
     *
     * @throws IOException if something happens during file processing
     */
    public void loadConfig() throws IOException {
        loadSecurityFuzzerFile();
        loadCustomFuzzerFile();
        loadRefData();
        loadURLParams();
        loadHeaders();
        loadQueryParams();
    }

    /**
     * Loads the supplied security fuzzer file into a Map.
     *
     * @throws IOException if something happens while reading the file
     */
    public void loadSecurityFuzzerFile() throws IOException {
        if (securityFuzzerFile == null) {
            log.debug("No SecurityFuzzer file provided. SecurityFuzzer will be skipped!");
        } else {
            log.config(Ansi.ansi().bold().a("Security Fuzzer file: {}").reset().toString(),
                    Ansi.ansi().fg(Ansi.Color.BLUE).a(securityFuzzerFile.getCanonicalPath()));
            securityFuzzerDetails = parseYaml(securityFuzzerFile.getCanonicalPath());
        }
    }

    /**
     * Loads the supplied functional fuzzer file into a Map.
     *
     * @throws IOException if something happens while reading the file
     */
    public void loadCustomFuzzerFile() throws IOException {
        if (customFuzzerFile == null) {
            log.debug("No FunctionalFuzzer file provided. FunctionalFuzzer will be skipped!");
        } else {
            log.config(Ansi.ansi().bold().a("Functional Fuzzer file: {}").reset().toString(),
                    Ansi.ansi().fg(Ansi.Color.BLUE).a(customFuzzerFile.getCanonicalPath()));
            customFuzzerDetails = parseYaml(customFuzzerFile.getCanonicalPath());
        }
    }

    /**
     * Loads the supplied reference data file into a Map.
     *
     * @throws IOException if something happens while reading the file
     */
    public void loadRefData() throws IOException {
        this.refData = this.loadFileAsMapOfMapsOfStrings(refDataFile, "Reference Data");
    }

    /**
     * Loads the supplied query params file into a Map.
     *
     * @throws IOException if something happens while reading the file
     */
    public void loadQueryParams() throws IOException {
        this.queryParams = this.loadFileAsMapOfMapsOfStrings(queryFile, "Query Params");
    }

    /**
     * Loads the supplied url params into a Map.
     */
    public void loadURLParams() {
        if (params == null) {
            log.debug("No URL parameters provided!");
        } else {
            log.config(Ansi.ansi().bold().a("URL parameters: {}").reset().toString(),
                    Ansi.ansi().fg(Ansi.Color.BLUE).a(params));
        }
    }

    /**
     * Loads the supplied headers file into a Map. The method also merges the headers supplied
     * in the file with the ones supplied via the {@code -H} argument.
     *
     * @throws IOException if something happens while reading the file
     */
    public void loadHeaders() throws IOException {
        this.headers = this.loadFileAsMapOfMapsOfStrings(headersFile, "Headers");

        /*Merge headers from file with the ones supplied using the -H argument*/
        if (headersMap != null) {
            headers.merge(ALL, headersMap, (stringStringMap, stringStringMap2) -> {
                Map<String, Object> mergedMap = new HashMap<>(stringStringMap);
                mergedMap.putAll(stringStringMap2);
                return mergedMap;
            });
        }
    }

    /**
     * Returns a list of the --urlParams supplied. The list will contain strings in the {@code name:value} pairs.
     *
     * @return a name:value list of url params to be replaced when calling the service
     */
    public List<String> getUrlParamsList() {
        return Optional.ofNullable(this.params).orElse(Collections.emptyList());
    }

    /**
     * Replaces the current URL parameters with the {@code --urlParams} arguments supplied.
     * The URL parameters are expected to be included in curly brackets.
     *
     * @param startingUrl the base url: {@code http://localhost:8080/{petId}}
     * @return the initial URL with the parameters replaced with --urlParams supplied values: {@code http://localhost:8080/123}
     */
    public String replacePathWithUrlParams(String startingUrl) {
        for (String line : this.getUrlParamsList()) {
            String[] urlParam = line.split(":", -1);
            String pathVar = "{" + urlParam[0] + "}";
            if (startingUrl.contains(pathVar)) {
                startingUrl = startingUrl.replace(pathVar, urlParam[1]);
            }
        }
        return startingUrl;
    }

    /**
     * Returns the header values supplied in the --headers argument.
     * <p>
     * The map keys are the contract paths or {@code all} if headers are applied to all paths.
     *
     * @return a Map representation of the --headers file with paths being the Map keys
     */
    public Map<String, Object> getHeaders(String path) {
        return mergePathAndAll(headers, path);
    }

    /**
     * Returns the reference data from the --refData file supplied as argument.
     * <p>
     * It returns the reference data for both the given path and the {@code all} key.
     *
     * @param currentPath the current API path
     * @return a Map with the supplied --refData
     */
    public Map<String, Object> getRefData(String currentPath) {
        return mergePathAndAll(refData, currentPath);
    }

    /**
     * Returns a map with key-value pairs for all additional query parameters corresponding
     * to the given path as well as the ALL entry.
     *
     * @param path the given path
     * @return a key-value map with all additional query params
     */
    public Map<String, Object> getAdditionalQueryParamsForPath(String path) {
        return mergePathAndAll(queryParams, path);
    }


    static Map<String, Object> mergePathAndAll(Map<String, Map<String, Object>> collection, String path) {
        return collection.entrySet().stream()
                .filter(entry -> entry.getKey().equalsIgnoreCase(path) || entry.getKey().equalsIgnoreCase(ALL))
                .map(Map.Entry::getValue).collect(HashMap::new, Map::putAll, Map::putAll);
    }

    private Map<String, Map<String, Object>> loadFileAsMapOfMapsOfStrings(File file, String fileType) throws IOException {
        Map<String, Map<String, Object>> fromFile = new HashMap<>();
        if (file == null) {
            log.debug("No {} file provided!", fileType);
        } else {
            log.config(Ansi.ansi().bold().a("{} file: {}").reset().toString(), fileType,
                    Ansi.ansi().fg(Ansi.Color.BLUE).a(file.getCanonicalPath()));
            fromFile = parseYaml(file.getCanonicalPath());
            log.debug("{} file loaded successfully: {}", fileType, fromFile);
        }
        return fromFile;
    }

    static Map<String, Map<String, Object>> parseYaml(String yaml) throws IOException {
        Map<String, Map<String, Object>> result = new LinkedHashMap<>();
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        try (Reader reader = new InputStreamReader(new FileInputStream(yaml), StandardCharsets.UTF_8)) {
            JsonNode node = mapper.reader().readTree(reader);
            Map<String, Object> paths = mapper.convertValue(node, Map.class);

            for (Map.Entry<String, Object> entry : Optional.ofNullable(paths).orElse(Collections.emptyMap()).entrySet()) {
                Map<String, Object> properties = mapper.convertValue(entry.getValue(), LinkedHashMap.class);
                result.put(entry.getKey(), properties);
            }
        }
        return result;
    }
}
