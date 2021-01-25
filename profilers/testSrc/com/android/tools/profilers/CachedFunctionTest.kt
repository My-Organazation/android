/*
 * Copyright (C) 2020 The Android Open Source Project
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
package com.android.tools.profilers

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import kotlin.math.sin

class CachedFunctionTest {

  @Test
  fun `cached function computes same as uncached`() {
    var invocationCount = 0
    fun sinWithEffect(x: Double) = sin(x).also { invocationCount++ }
    val cachedSin = CachedFunction(::sinWithEffect)
    val x = Math.random()
    for (i in 1 .. 5) {
      assertThat(cachedSin(x)).isEqualTo(sin(x))
    }
    assertThat(invocationCount).isEqualTo(1)
  }

  @Test
  fun `cached function recomputes when invalidated`() {
    var invocationCount = 0
    fun sinWithEffect(x: Double) = sin(x).also { invocationCount++ }
    val cachedSin = CachedFunction(::sinWithEffect)
    val x = Math.random()
    val tries = 5
    for (i in 1 .. tries) {
      assertThat(cachedSin(x)).isEqualTo(sin(x))
      cachedSin.invalidate()
    }
    assertThat(invocationCount).isEqualTo(tries)
  }
}