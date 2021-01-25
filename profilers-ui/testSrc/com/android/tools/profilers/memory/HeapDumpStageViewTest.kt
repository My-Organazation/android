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
package com.android.tools.profilers.memory

import com.android.tools.adtui.TreeWalker
import com.android.tools.adtui.model.AspectModel
import com.android.tools.adtui.model.FakeTimer
import com.android.tools.idea.transport.faketransport.FakeGrpcChannel
import com.android.tools.idea.transport.faketransport.FakeTransportService
import com.android.tools.idea.transport.faketransport.FakeTransportService.FAKE_DEVICE_NAME
import com.android.tools.idea.transport.faketransport.FakeTransportService.FAKE_PROCESS_NAME
import com.android.tools.profilers.FakeIdeProfilerComponents
import com.android.tools.profilers.FakeIdeProfilerServices
import com.android.tools.profilers.FakeProfilerService
import com.android.tools.profilers.ProfilerClient
import com.android.tools.profilers.StudioProfilers
import com.android.tools.profilers.StudioProfilersView
import com.android.tools.profilers.cpu.FakeCpuService
import com.android.tools.profilers.event.FakeEventService
import com.android.tools.profilers.memory.adapters.CaptureObject
import com.android.tools.profilers.memory.adapters.FakeCaptureObject
import com.android.tools.profilers.network.FakeNetworkService
import com.google.common.truth.Truth.assertThat
import com.google.common.util.concurrent.MoreExecutors
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.util.function.Supplier
import javax.swing.JLabel


@RunWith(Parameterized::class)
class HeapDumpStageViewTest(val useNewEventPipeline: Boolean) {

  private lateinit var profilers: StudioProfilers
  private lateinit var mockLoader: FakeCaptureObjectLoader
  private val myTimer = FakeTimer()
  private lateinit var ideProfilerServices: FakeIdeProfilerServices
  private lateinit var profilersView: StudioProfilersView

  private val transportService = FakeTransportService(myTimer)
  private val service = FakeMemoryService(transportService)

  @Rule
  @JvmField
  val grpcChannel = FakeGrpcChannel("MemoryProfilerStageViewTestChannel", transportService, service,
                                    FakeProfilerService(myTimer),
                                    FakeCpuService(), FakeEventService(),
                                    FakeNetworkService.Builder().build())

  @Before
  fun setupBase() {
    ideProfilerServices = FakeIdeProfilerServices()
    profilers = StudioProfilers(ProfilerClient(grpcChannel.channel), ideProfilerServices, myTimer)
    profilers.setPreferredProcess(FAKE_DEVICE_NAME, FAKE_PROCESS_NAME, null)
    ideProfilerServices.enableEventsPipeline(useNewEventPipeline)
    profilersView = StudioProfilersView(profilers, FakeIdeProfilerComponents())
    mockLoader = FakeCaptureObjectLoader()

    // Advance the clock to make sure StudioProfilers has a chance to select device + process.
    myTimer.tick(FakeTimer.ONE_SECOND_IN_NS)
  }

  @Test
  fun testNavigationButtonNameIsCaptureInNewUi() {
    ideProfilerServices.enableSeparateHeapDumpUi(true)
    // Load a fake capture
    val fakeCapture = FakeCaptureObject.Builder().setCaptureName("DUMMY_CAPTURE1").setStartTime(0).setEndTime(
      10).setInfoMessage("Foo").build()
    val stage = createStageWithCaptureLoaded(fakeCapture)
    val view = HeapDumpStageView(profilersView, stage)
    val walker = TreeWalker(view.toolbar)
    val label = walker.descendantStream().filter { c -> c is JLabel }.findFirst().get() as JLabel
    assertThat(label.text).startsWith(fakeCapture.name)
  }

  private fun createStageWithCaptureLoaded(capture: CaptureObject) = HeapDumpStage(
    profilers,
    mockLoader,
    CaptureDurationData(1, false, false, CaptureEntry(Any(), Supplier { capture })),
    MoreExecutors.directExecutor()
  ).apply {
    enter()
    captureSelection.refreshSelectedHeap()
  }

  companion object {
    @JvmStatic
    @Parameterized.Parameters
    fun useNewEventPipelineParameter(): Collection<Array<Boolean>> {
      return listOf(arrayOf(false), arrayOf(true))
    }
  }
}