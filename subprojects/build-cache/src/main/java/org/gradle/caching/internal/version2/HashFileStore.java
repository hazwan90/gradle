/*
 * Copyright 2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.caching.internal.version2;

import org.gradle.api.NonNullApi;
import org.gradle.internal.hash.HashCode;
import org.gradle.internal.io.IoAction;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;

/**
 * File store that is indexed via {@link HashCode}.
 */
@NonNullApi
public interface HashFileStore {
    /**
     * Puts an entry into the store, using the given action to produce the file.
     */
    File put(HashCode key, IoAction<File> addAction) throws IOException;

    /**
     * Gets an entry into the store, using the given action to produce the file.
     */
    @Nullable
    File get(HashCode key) throws IOException;;
}
