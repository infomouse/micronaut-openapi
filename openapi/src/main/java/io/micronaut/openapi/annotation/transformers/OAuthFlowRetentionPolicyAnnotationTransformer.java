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
package io.micronaut.openapi.annotation.transformers;

import io.swagger.v3.oas.annotations.security.OAuthFlow;

/**
 * Changes the Retention Policy of the annotation to SOURCE.
 *
 * @since 2.1
 * @author croudet
 */
public class OAuthFlowRetentionPolicyAnnotationTransformer extends AbstractRetentionPolicyAnnotationTransformer<OAuthFlow> {

    /**
     * Changes the Retention Policy of the annotation to SOURCE.
     */
    public OAuthFlowRetentionPolicyAnnotationTransformer() {
        super(OAuthFlow.class);
    }
}
