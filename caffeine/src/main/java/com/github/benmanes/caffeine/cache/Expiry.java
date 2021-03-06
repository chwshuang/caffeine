/*
 * Copyright 2017 Ben Manes. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.benmanes.caffeine.cache;

import static java.util.Objects.requireNonNull;

import java.io.Serializable;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;

/**
 * Calculates when cache entries expire. A single expiration time is retained so that the lifetime
 * of an entry may be extended or reduced by subsequent evaluations.
 *
 * @author ben.manes@gmail.com (Ben Manes)
 */
@ThreadSafe
interface Expiry<K, V> {

  /**
   * Specifies that the entry should be automatically removed from the cache once the duration has
   * elapsed after the entry's creation. To indicate no expiration an entry may be given an
   * excessively long period, such as {@code Long#MAX_VALUE}.
   *
   * @param key the key represented by this entry
   * @param value the value represented by this entry
   * @param currentTime the current time, in nanoseconds
   * @return the length of time before the entry expires, in nanoseconds
   */
  long expireAfterCreate(K key, V value, long currentTime);

  /**
   * Specifies that the entry should be automatically removed from the cache once the duration has
   * elapsed after the replacement of its value. To indicate no expiration an entry may be given an
   * excessively long period, such as {@code Long#MAX_VALUE}. The {@code currentDuration} may be
   * returned to not modify the expiration time.
   *
   * @param key the key represented by this entry
   * @param value the value represented by this entry
   * @param currentTime the current time, in nanoseconds
   * @param currentDuration the current duration, in nanoseconds
   * @return the length of time before the entry expires, in nanoseconds
   */
  long expireAfterUpdate(K key, V value, long currentTime, long currentDuration);

  /**
   * Specifies that the entry should be automatically removed from the cache once the duration has
   * elapsed after its last read. To indicate no expiration an entry may be given an excessively
   * long period, such as {@code Long#MAX_VALUE}. The {@code currentDuration} may be returned to not
   * modify the expiration time.
   *
   * @param key the key represented by this entry
   * @param value the value represented by this entry
   * @param currentTime the current time, in nanoseconds
   * @param currentDuration the current duration, in nanoseconds
   * @return the length of time before the entry expires, in nanoseconds
   */
  long expireAfterRead(K key, V value, long currentTime, long currentDuration);

  /**
   * Returns an expiry where entries never expire.
   *
   * @param <K> the type of keys
   * @param <V> the type of values
   * @return an expiry where entries never expire
   */
  @SuppressWarnings("unchecked")
  static <K, V> Expiry<K, V> eternalExpiry() {
    return (Expiry<K, V>) EternalExpiry.INSTANCE;
  }

  /**
   * Returns an expiry where the minimum duration is non-negative.
   *
   * @param delegate the expiry to calculating the expiration time with
   * @param <K> the type of keys
   * @param <V> the type of values
   * @return a weigher that enforces that the weight is non-negative
   */
  @Nonnull
  static <K, V> Expiry<K, V> boundedExpiry(@Nonnull Expiry<K, V> delegate) {
    return new BoundedExpiry<>(delegate);
  }
}

enum EternalExpiry implements Expiry<Object, Object> {
  INSTANCE;

  @Override
  public long expireAfterCreate(Object key, Object value, long currentTime) {
    return Long.MAX_VALUE;
  }

  @Override
  public long expireAfterUpdate(Object key, Object value, long currentTime, long currentDuration) {
    return Long.MAX_VALUE;
  }

  @Override
  public long expireAfterRead(Object key, Object value, long currentTime, long currentDuration) {
    return Long.MAX_VALUE;
  }
}

final class BoundedExpiry<K, V> implements Expiry<K, V>, Serializable {
  static final long serialVersionUID = 1;
  final Expiry<? super K, ? super V> delegate;

  BoundedExpiry(Expiry<? super K, ? super V> delegate) {
    this.delegate = requireNonNull(delegate);
  }

  @Override
  public long expireAfterCreate(K key, V value, long currentTime) {
    long duration = delegate.expireAfterCreate(key, value, currentTime);
    return (duration < 0) ? 0 : duration;
  }

  @Override
  public long expireAfterUpdate(K key, V value, long currentTime, long currentDuration) {
    long duration = delegate.expireAfterUpdate(key, value, currentTime, currentDuration);
    return (duration < 0) ? 0 : duration;
  }

  @Override
  public long expireAfterRead(K key, V value, long currentTime, long currentDuration) {
    long duration = delegate.expireAfterUpdate(key, value, currentTime, currentDuration);
    return (duration < 0) ? 0 : duration;
  }

  Object writeReplace() {
    return delegate;
  }
}

