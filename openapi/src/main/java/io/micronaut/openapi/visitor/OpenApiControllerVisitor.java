/*
 * Copyright 2017-2023 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micronaut.openapi.visitor;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import javax.annotation.processing.SupportedOptions;

import io.micronaut.context.RequiresCondition;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.env.Environment;
import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.core.util.ArrayUtils;
import io.micronaut.core.util.CollectionUtils;
import io.micronaut.core.util.StringUtils;
import io.micronaut.http.HttpMethod;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Consumes;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.HttpMethodMapping;
import io.micronaut.http.annotation.Produces;
import io.micronaut.http.annotation.UriMapping;
import io.micronaut.http.uri.UriMatchTemplate;
import io.micronaut.inject.ast.ClassElement;
import io.micronaut.inject.ast.Element;
import io.micronaut.inject.ast.MethodElement;
import io.micronaut.inject.visitor.TypeElementVisitor;
import io.micronaut.inject.visitor.VisitorContext;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.servers.Server;

import com.fasterxml.jackson.annotation.JsonAnySetter;

import static io.micronaut.openapi.visitor.OpenApiConfigProperty.MICRONAUT_OPENAPI_ENABLED;
import static io.micronaut.openapi.visitor.ConfigUtils.getActiveEnvs;
import static io.micronaut.openapi.visitor.ConfigUtils.getEnv;
import static io.micronaut.openapi.visitor.Utils.DEFAULT_MEDIA_TYPES;

/**
 * A {@link TypeElementVisitor} the builds the Swagger model from Micronaut controllers at compile time.
 *
 * @author graemerocher
 * @since 1.0
 */
@SupportedOptions(MICRONAUT_OPENAPI_ENABLED)
public class OpenApiControllerVisitor extends AbstractOpenApiEndpointVisitor implements TypeElementVisitor<Object, HttpMethodMapping> {

    private final String customUri;
    private final List<AnnotationValue<Tag>> additionalTags;
    private final List<AnnotationValue<io.swagger.v3.oas.annotations.security.SecurityRequirement>> additionalSecurityRequirements;

    public OpenApiControllerVisitor() {
        additionalTags = Collections.emptyList();
        additionalSecurityRequirements = Collections.emptyList();
        customUri = null;
    }

    public OpenApiControllerVisitor(List<AnnotationValue<Tag>> additionalTags,
                                    List<AnnotationValue<io.swagger.v3.oas.annotations.security.SecurityRequirement>> additionalSecurityRequirements,
                                    String customUri) {
        this.additionalTags = additionalTags;
        this.additionalSecurityRequirements = additionalSecurityRequirements;
        this.customUri = customUri;
    }

    private boolean ignoreByRequires(Element element, VisitorContext context) {
        List<AnnotationValue<Requires>> requiresAnnotations = element.getDeclaredAnnotationValuesByType(Requires.class);
        if (CollectionUtils.isEmpty(requiresAnnotations)) {
            return false;
        }
        Environment environment = getEnv(context);
        Set<String> activeEnvs;
        if (environment != null) {
            activeEnvs = environment.getActiveNames();
        } else {
            activeEnvs = new HashSet<>(getActiveEnvs(context));
        }
        if (CollectionUtils.isEmpty(activeEnvs)) {
            return false;
        }

        // check env and notEnv
        for (AnnotationValue<Requires> requiresAnn : requiresAnnotations) {
            String[] reqEnvs = requiresAnn.stringValues(RequiresCondition.MEMBER_ENV);
            if (reqEnvs.length != 0) {
                boolean result = Arrays.stream(reqEnvs).anyMatch(activeEnvs::contains);
                if (!result) {
                    return true;
                }
            }
            String[] reqNotEnvs = requiresAnn.stringValues(RequiresCondition.MEMBER_NOT_ENV);
            if (reqNotEnvs.length != 0) {
                boolean result = Arrays.stream(reqNotEnvs).noneMatch(activeEnvs::contains);
                if (!result) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    protected boolean ignore(ClassElement element, VisitorContext context) {
        boolean isParentClass = context.get(IS_PROCESS_PARENT_CLASS, Boolean.class).orElse(false);

        return (!isParentClass && !element.isAnnotationPresent(Controller.class))
            || element.isAnnotationPresent(Hidden.class)
            || ignoreByRequires(element, context);
    }

    @Override
    protected boolean ignore(MethodElement element, VisitorContext context) {

        AnnotationValue<Operation> operationAnn = element.getAnnotation(Operation.class);
        boolean isHidden = operationAnn != null && operationAnn.booleanValue("hidden").orElse(false);
        AnnotationValue<JsonAnySetter> jsonAnySetterAnn = element.getAnnotation(JsonAnySetter.class);

        return isHidden
            || ignore(element.getDeclaringType(), context)
            || element.isPrivate()
            || element.isStatic()
            || element.isAnnotationPresent(Hidden.class)
            || (jsonAnySetterAnn != null && jsonAnySetterAnn.booleanValue("enabled").orElse(true))
            || ignoreByRequires(element, context);
    }

    @Override
    protected HttpMethod httpMethod(MethodElement element) {
        Optional<Class<? extends Annotation>> httpMethodOpt = element
                .getAnnotationTypeByStereotype(HttpMethodMapping.class);
        if (httpMethodOpt.isEmpty()) {
            return null;
        }
        try {
            return HttpMethod.valueOf(httpMethodOpt.get().getSimpleName().toUpperCase(Locale.ENGLISH));
        } catch (IllegalArgumentException e) {
            // ignore
        }
        return null;
    }

    @Override
    protected List<MediaType> consumesMediaTypes(MethodElement element) {
        return mediaTypes(element, Consumes.class);
    }

    @Override
    protected List<MediaType> producesMediaTypes(MethodElement element) {
        return mediaTypes(element, Produces.class);
    }

    @Override
    public int getOrder() {
        return 50;
    }

    private List<MediaType> mediaTypes(MethodElement element, Class<? extends Annotation> ann) {
        String[] values = element.stringValues(ann);
        if (ArrayUtils.isEmpty(values)) {
            return DEFAULT_MEDIA_TYPES;
        }
        return Arrays.stream(values)
            .map(MediaType::of)
            .distinct()
            .toList();
    }

    @Override
    protected List<UriMatchTemplate> uriMatchTemplates(MethodElement element, VisitorContext context) {
        String controllerValue = element.getOwningType().getValue(UriMapping.class, String.class).orElse(element.getDeclaringType().getValue(UriMapping.class, String.class).orElse("/"));
        String childClassPath = context.get(CONTEXT_CHILD_PATH, String.class).orElse(null);
        if (childClassPath != null) {
            controllerValue = childClassPath;
        }
        if (StringUtils.isNotEmpty(customUri)) {
            controllerValue = customUri;
        }

        controllerValue = OpenApiApplicationVisitor.replacePlaceholders(controllerValue, context);

        UriMatchTemplate matchTemplate = UriMatchTemplate.of(controllerValue);
        // check if we have multiple uris
        String[] uris = element.stringValues(HttpMethodMapping.class, "uris");
        if (ArrayUtils.isEmpty(uris)) {
            String methodValue = element.getValue(HttpMethodMapping.class, String.class).orElse("/");
            methodValue = OpenApiApplicationVisitor.replacePlaceholders(methodValue, context);
            return Collections.singletonList(matchTemplate.nest(methodValue));
        }
        List<UriMatchTemplate> matchTemplates = new ArrayList<>(uris.length);
        for (String methodValue : uris) {
            methodValue = OpenApiApplicationVisitor.replacePlaceholders(methodValue, context);
            matchTemplates.add(matchTemplate.nest(methodValue));
        }
        return matchTemplates;
    }

    @Override
    protected String description(MethodElement element) {
        return null;
    }

    @Override
    protected List<io.swagger.v3.oas.models.tags.Tag> classTags(ClassElement element, VisitorContext context) {
        return readTags(additionalTags, context);
    }

    @Override
    protected List<Server> methodServers(MethodElement element, VisitorContext context) {
        return processOpenApiAnnotation(
                element,
                context,
                io.swagger.v3.oas.annotations.servers.Server.class,
                Server.class,
                Collections.emptyList()
        );
    }

    @Override
    protected List<SecurityRequirement> methodSecurityRequirements(MethodElement element, VisitorContext context) {
        List<SecurityRequirement> securityRequirements = readSecurityRequirements(element);
        if (!additionalSecurityRequirements.isEmpty()) {
            securityRequirements = new ArrayList<>(securityRequirements);
            securityRequirements.addAll(readSecurityRequirements(additionalSecurityRequirements));
        }
        return securityRequirements;
    }
}
