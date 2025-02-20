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
package io.micronaut.openapi.swagger.core.jackson.mixin;

import java.util.Map;

import io.micronaut.core.annotation.Internal;
import io.micronaut.openapi.swagger.core.jackson.PathsSerializer;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * This class is copied from swagger-core library.
 *
 * @since 4.6.0
 */
@Internal
public abstract class OpenAPIMixin {

    @JsonAnyGetter
    @JsonInclude
    public abstract Map<String, Object> getExtensions();

    @JsonAnySetter
    public abstract void addExtension(String name, Object value);

    @JsonSerialize(using = PathsSerializer.class)
    public abstract Paths getPaths();

    @JsonIgnore
    public abstract Map<String, PathItem> getWebhooks();
}
