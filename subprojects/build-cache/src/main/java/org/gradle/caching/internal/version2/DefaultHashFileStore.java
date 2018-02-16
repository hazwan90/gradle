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

import com.google.common.base.Throwables;
import org.gradle.api.NonNullApi;
import org.gradle.internal.hash.HashCode;
import org.gradle.internal.io.IoAction;
import org.gradle.util.GFileUtils;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;

@NonNullApi
public class DefaultHashFileStore implements HashFileStore {
    private File baseDir;

    public DefaultHashFileStore(File baseDir) {
        this.baseDir = baseDir;
        GFileUtils.mkdirs(baseDir);
    }

    @Override
    public File put(HashCode key, IoAction<File> action) throws IOException {
        File destination = getFile(key);
        if (!destination.exists()) {
            File tempFile = File.createTempFile("build-cache-", ".tmp", baseDir);
            try {
                action.execute(tempFile);
                if (!tempFile.renameTo(destination)) {
                    throw new IOException(String.format("Cannot rename %s to %s", tempFile, destination));
                }
                tempFile = null;
            } catch (Exception ex) {
                deletedFileQuietly(destination);
                Throwables.propagateIfInstanceOf(ex, IOException.class);
                throw Throwables.propagate(ex);
            } finally {
                if (tempFile != null) {
                    deletedFileQuietly(tempFile);
                }
            }
        }
        return destination;
    }

    @Nullable
    @Override
    public File get(HashCode key) {
        File file = getFile(key);
        if (file.exists()) {
            return file;
        } else {
            return null;
        }
    }

    private File getFile(HashCode key) {
        return new File(baseDir, key.toString());
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private static void deletedFileQuietly(File file) {
        file.delete();
    }
}
