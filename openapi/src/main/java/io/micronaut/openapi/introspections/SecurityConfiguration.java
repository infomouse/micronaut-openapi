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
package io.micronaut.openapi.introspections;

import io.micronaut.core.annotation.Introspected;


import io.swagger.v3.oas.models.security.OAuthFlow;
import io.swagger.v3.oas.models.security.OAuthFlows;
import io.swagger.v3.oas.models.security.Scopes;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

/**
 * OpenApi introspection configuration for Swagger-model.
 * Adds introspection of the io.swagger.v3.oas.models.security package
 *
 * @author Henrique Mota
 */
@Introspected(classes = {
    OAuthFlow.class,
    OAuthFlows.class,
    Scopes.class,
    SecurityRequirement.class,
    SecurityScheme.class,
})
public class SecurityConfiguration {
}
