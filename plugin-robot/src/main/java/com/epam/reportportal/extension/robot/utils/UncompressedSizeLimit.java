/*
 * Copyright 2026 EPAM Systems
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.epam.reportportal.extension.robot.utils;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Shared budget of bytes that may be read from the entries of one archive. Counts bytes actually read, so it can't be
 * bypassed by fake sizes in zip headers.
 */
public class UncompressedSizeLimit {

  private final long maxBytes;
  private long remaining;

  public UncompressedSizeLimit(long maxBytes) {
    this.maxBytes = maxBytes;
    this.remaining = maxBytes;
  }

  public InputStream wrap(InputStream in) {
    return new FilterInputStream(in) {
      @Override
      public int read() throws IOException {
        int b = super.read();
        if (b >= 0) {
          consume(1);
        }
        return b;
      }

      @Override
      public int read(byte[] buffer, int off, int len) throws IOException {
        int n = super.read(buffer, off, len);
        if (n > 0) {
          consume(n);
        }
        return n;
      }

      @Override
      public long skip(long n) throws IOException {
        long skipped = super.skip(n);
        consume(skipped);
        return skipped;
      }
    };
  }

  private void consume(long bytes) throws LimitExceededException {
    remaining -= bytes;
    if (remaining < 0) {
      throw new LimitExceededException(maxBytes);
    }
  }

  /**
   * Thrown when the archive content exceeds the limit.
   */
  public static class LimitExceededException extends IOException {

    public LimitExceededException(long maxBytes) {
      super("Uncompressed size of the archive exceeds " + (maxBytes / (1024 * 1024)) + " MB");
    }
  }
}
