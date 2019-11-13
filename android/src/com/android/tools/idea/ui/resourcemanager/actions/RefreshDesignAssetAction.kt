/*
 * Copyright (C) 2019 The Android Open Source Project
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
package com.android.tools.idea.ui.resourcemanager.actions

import com.android.resources.ResourceType
import com.android.tools.idea.ui.resourcemanager.model.DesignAsset
import com.android.tools.idea.ui.resourcemanager.model.RESOURCE_DESIGN_ASSETS_KEY
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DataKey
import java.util.EnumSet

/**
 * Action that calls the given [refreshAssetsCallback] when there are supported [DesignAsset]s under the [RESOURCE_DESIGN_ASSETS_KEY]
 * [DataKey].
 */
class RefreshDesignAssetAction(private val refreshAssetsCallback: (Array<DesignAsset>) -> Unit)
  : AnAction("Refresh Preview", "Refresh the preview for the selected resources", null) {
  private val supportedResourceTypes = EnumSet.of(ResourceType.DRAWABLE, ResourceType.MIPMAP, ResourceType.LAYOUT, ResourceType.MENU)

  override fun actionPerformed(e: AnActionEvent) {
    val assets = e.getData(RESOURCE_DESIGN_ASSETS_KEY)
    if (assets != null && canRefresh(assets)) {
      refreshAssetsCallback(assets)
    }
  }

  override fun update(e: AnActionEvent) {
    e.presentation.isEnabledAndVisible = canRefresh(e.getData(RESOURCE_DESIGN_ASSETS_KEY))
  }

  private fun canRefresh(assets: Array<DesignAsset>?): Boolean = assets?.all { supportedResourceTypes.contains(it.type) } ?: false
}