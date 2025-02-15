package io.micronaut.openapi.generator.assertions;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import io.swagger.parser.OpenAPIParser;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.parser.core.models.ParseOptions;

import org.openapitools.codegen.CodegenModel;
import org.openapitools.codegen.InlineModelResolver;
import org.openapitools.codegen.model.ModelMap;
import org.openapitools.codegen.model.ModelsMap;
import org.openapitools.codegen.utils.ModelUtils;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.google.common.collect.ImmutableMap;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

//CHECKSTYLE:OFF
public class TestUtils {

    /**
     * Helper method for parsing specs into an intermediary OpenAPI structure for pre-processing.
     * <p>
     * Use this method only for tests targeting processing helpers such as {@link ModelUtils}
     * or {@link InlineModelResolver}. Using this for testing generators will mean you're not testing the OpenAPI document
     * in a state the generator will be presented at runtime.
     *
     * @param specFilePath The path to the specification file
     * @return A "raw" OpenAPI document
     */
    public static OpenAPI parseSpec(String specFilePath) {
        OpenAPI openAPI = new OpenAPIParser().readLocation(specFilePath, null, new ParseOptions()).getOpenAPI();
        // Invoke helper function to get the original swagger version.
        // See https://github.com/swagger-api/swagger-parser/pull/1374
        // Also see https://github.com/swagger-api/swagger-parser/issues/1369.
        ModelUtils.getOpenApiVersion(openAPI, specFilePath, null);
        return openAPI;
    }

    public static OpenAPI parseContent(String jsonOrYaml) {
        OpenAPI openAPI = new OpenAPIParser().readContents(jsonOrYaml, null, null).getOpenAPI();
        // Invoke helper function to get the original swagger version.
        ModelUtils.getOpenApiVersion(openAPI, jsonOrYaml, null);
        return openAPI;
    }

    public static OpenAPI createOpenAPI() {
        OpenAPI openAPI = new OpenAPI();
        openAPI.setComponents(new Components());
        openAPI.setPaths(new Paths());

        final Info info = new Info();
        info.setDescription("API under test");
        info.setVersion("1.0.7");
        info.setTitle("My title");
        openAPI.setInfo(info);

        final Server server = new Server();
        server.setUrl("https://localhost:9999/root");
        openAPI.setServers(Collections.singletonList(server));
        return openAPI;
    }

    public static OpenAPI createOpenAPIWithOneSchema(String name, Schema<?> schema) {
        OpenAPI openAPI = createOpenAPI();
        openAPI.setComponents(new Components());
        openAPI.getComponents().addSchemas(name, schema);
        return openAPI;
    }

    public static void validateJavaSourceFiles(Map<String, String> fileMap) {
        fileMap.forEach( (fileName, fileContents) -> {
                if (fileName.endsWith(".java")) {
                    assertValidJavaSourceCode(fileContents, fileName);
                }
            }
        );
    }

    public static void validateJavaSourceFiles(List<File> files) {
        files.forEach( f -> {
                    String fileName = f.getName();
                    if (fileName.endsWith(".java")) {
                        String fileContents = "";
                        try {
                            fileContents = Files.readString(f.toPath());
                        } catch (IOException ignored) {

                        }
                        assertValidJavaSourceCode(fileContents, fileName);
                    }
                }
        );
    }

    public static void assertValidJavaSourceCode(String javaSourceCode, String filename) {
        ParserConfiguration config = new ParserConfiguration();
        config.setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_11);
        JavaParser parser = new JavaParser(config);
        ParseResult<CompilationUnit> parseResult = parser.parse(javaSourceCode);
        assertTrue(parseResult.isSuccessful(), String.valueOf(parseResult.getProblems()));
    }

    public static void assertFileContains(Path path, String... lines) {
        try {
            String generatedFile = Files.readString(path);
            String file = linearize(generatedFile);
            assertNotNull(file);
            for (String line : lines) {
                assertTrue(file.contains(linearize(line)), "File does not contain line [" + line + "]");
            }
        } catch (IOException e) {
            fail("Unable to evaluate file " + path);
        }
    }

    public static String linearize(String target) {
        return target.replaceAll("\r?\n", "").replaceAll("\\s+", "\\s");
    }

    public static void assertFileNotContains(Path path, String... lines) {
        String generatedFile = null;
        try {
            generatedFile = Files.readString(path);
        } catch (IOException e) {
            fail("Unable to evaluate file " + path);
        }
        String file = linearize(generatedFile);
        assertNotNull(file);
        for (String line : lines) {
            assertFalse(file.contains(linearize(line)));
        }
    }

    public static void assertFileNotExists(Path path) {
        try {
            Files.readString(path);
            fail("File exists when it should not: " + path);
        } catch (IOException e) {
            // File exists, pass.
            assertTrue(true);
        }
    }

    public static void assertFileExists(Path path) {
        try {
            Files.readString(path);
            // File exists, pass.
            assertTrue(true);
        } catch (IOException e) {
            fail("File does not exist when it should: " + path);
        }
    }

    public static void assertExtraAnnotationFiles(String baseOutputPath) {

        JavaFileAssert.assertThat(java.nio.file.Paths.get(baseOutputPath + "/EmployeeEntity.java"))
                .assertTypeAnnotations()
                    .containsWithName("javax.persistence.Entity")
                    .containsWithNameAndAttributes("javax.persistence.Table", ImmutableMap.of("name", "\"employees\""))
                .toType()
                .hasProperty("assignments")
                    .assertPropertyAnnotations()
                    .containsWithNameAndAttributes("javax.persistence.OneToMany", ImmutableMap.of("mappedBy", "\"employee\""))
                    .toProperty()
                .toType();

        JavaFileAssert.assertThat(java.nio.file.Paths.get(baseOutputPath + "/Employee.java"))
                .assertTypeAnnotations()
                    .containsWithName("javax.persistence.MappedSuperclass")
                .toType()
                .hasProperty("id")
                    .assertPropertyAnnotations()
                    .containsWithName("javax.persistence.Id")
                    .toProperty()
                .toType()
                .hasProperty("email")
                    .assertPropertyAnnotations()
                    .containsWithName("org.hibernate.annotations.Formula")
                    .toProperty()
                .toType()
                .hasProperty("hasAcceptedTerms")
                    .assertPropertyAnnotations()
                    .containsWithName("javax.persistence.Transient")
                    .toProperty()
                .toType();

        JavaFileAssert.assertThat(java.nio.file.Paths.get(baseOutputPath + "/SurveyGroupEntity.java"))
                .assertTypeAnnotations()
                    .containsWithName("javax.persistence.Entity")
                    .containsWithNameAndAttributes("javax.persistence.Table", ImmutableMap.of("name", "\"survey_groups\""))
                .toType()
                .hasProperty("assignments")
                    .assertPropertyAnnotations()
                    .containsWithName("javax.persistence.OneToMany")
                    .containsWithNameAndAttributes("javax.persistence.JoinColumn", ImmutableMap.of("name", "\"survey_group_id\""))
                    .toProperty()
                .toType()
                .hasProperty("disabled")
                    .assertPropertyAnnotations()
                    .containsWithNameAndAttributes("javax.persistence.Column", ImmutableMap.of("nullable", "false"))
                    .toProperty()
                .toType();

        JavaFileAssert.assertThat(java.nio.file.Paths.get(baseOutputPath + "/SurveyGroup.java"))
                .assertTypeAnnotations()
                    .containsWithName("javax.persistence.MappedSuperclass")
                    .containsWithName("javax.persistence.EntityListeners")
                .toType()
                .hasProperty("id")
                    .assertPropertyAnnotations()
                    .containsWithName("javax.persistence.Id")
                    .containsWithNameAndAttributes("javax.persistence.GeneratedValue", ImmutableMap.of("generator", "\"UUID\""))
                    .containsWithNameAndAttributes("org.hibernate.annotations.GenericGenerator", ImmutableMap.of("name", "\"UUID\"","strategy", "\"org.hibernate.id.UUIDGenerator\""))
                    .containsWithNameAndAttributes("javax.persistence.Column", ImmutableMap.of("name", "\"id\"","updatable", "false","nullable", "false"))
                    .toProperty()
                .toType()
                .hasProperty("createdDate")
                    .assertPropertyAnnotations()
                    .containsWithName("org.springframework.data.annotation.CreatedDate")
                    .toProperty()
                .toType()
                .hasProperty("createdBy")
                    .assertPropertyAnnotations()
                    .containsWithName("org.springframework.data.annotation.CreatedBy")
                    .toProperty()
                .toType()
                .hasProperty("modifiedDate")
                    .assertPropertyAnnotations()
                    .containsWithName("org.springframework.data.annotation.LastModifiedDate")
                    .toProperty()
                .toType()
                .hasProperty("modifiedBy")
                    .assertPropertyAnnotations()
                    .containsWithName("org.springframework.data.annotation.LastModifiedBy")
                    .toProperty()
                .toType()
                .hasProperty("opportunityId")
                    .assertPropertyAnnotations()
                    .containsWithNameAndAttributes("javax.persistence.Column", ImmutableMap.of("unique", "true"))
                    .toProperty()
                .toType()
                .hasProperty("submissionStatus")
                    .assertPropertyAnnotations()
                    .containsWithName("javax.persistence.Transient")
                    .toProperty()
                .toType();

        JavaFileAssert.assertThat(java.nio.file.Paths.get(baseOutputPath + "/CompanyDto.java"))
            .hasProperty("priceCategory")
                .assertPropertyAnnotations()
                .containsWithNameAndAttributes("IgnoreForRoles", ImmutableMap.of("value", "\"MEDIA_ADMIN\""));
    }

    public static ModelsMap createCodegenModelWrapper(CodegenModel cm) {
        ModelsMap objs = new ModelsMap();
        List<ModelMap> modelMaps = new ArrayList<>();
        ModelMap modelMap = new ModelMap();
        modelMap.setModel(cm);
        modelMaps.add(modelMap);
        objs.setModels(modelMaps);
        return objs;
    }
}
