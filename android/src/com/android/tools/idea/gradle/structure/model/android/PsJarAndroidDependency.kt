/*
 * Copyright (C) 2018 The Android Open Source Project
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
package com.android.tools.idea.gradle.structure.model.android

import com.android.tools.idea.gradle.dsl.api.dependencies.DependencyModel
import com.android.tools.idea.gradle.dsl.api.dependencies.FileDependencyModel
import com.android.tools.idea.gradle.dsl.api.dependencies.FileTreeDependencyModel
import com.android.tools.idea.gradle.dsl.api.ext.ResolvedPropertyModel
import com.android.tools.idea.gradle.structure.model.PsDeclaredJarDependency
import com.android.tools.idea.gradle.structure.model.PsJarDependency
import com.android.tools.idea.gradle.structure.model.PsResolvedJarDependency
import com.android.tools.idea.gradle.structure.model.meta.ModelDescriptor
import com.android.tools.idea.gradle.structure.model.meta.ModelProperty
import com.android.tools.idea.gradle.structure.model.meta.asString
import com.android.tools.idea.gradle.structure.model.meta.getValue

class PsDeclaredJarAndroidDependency private constructor(
  parent: PsAndroidModule,
  containers: Collection<PsAndroidArtifact>,
  override val parsedModel: DependencyModel
) : PsJarAndroidDependency(parent, containers), PsDeclaredJarDependency {

  constructor (parent: PsAndroidModule, containers: Collection<PsAndroidArtifact>, parsedModel: FileDependencyModel)
    : this(parent, containers, parsedModel as DependencyModel)
  constructor (parent: PsAndroidModule, containers: Collection<PsAndroidArtifact>, parsedModel: FileTreeDependencyModel)
    : this(parent, containers, parsedModel as DependencyModel)

  override val descriptor by PsDeclaredJarAndroidDependency.Descriptor
  override val kind: PsJarDependency.Kind = when (parsedModel) {
    is FileDependencyModel -> PsJarDependency.Kind.FILE
    is FileTreeDependencyModel -> PsJarDependency.Kind.FILE_TREE
    else -> error("Unsupported dependency model: ${parsedModel.javaClass.name}")
  }
  override val filePath: String =
    (parsedModel as? FileDependencyModel)?.file()?.asString()
    ?: (parsedModel as? FileTreeDependencyModel)?.dir()?.asString()
    ?: ""

  private inline fun getFileTreeStringListProperty(propertyGetter: FileTreeDependencyModel.() -> ResolvedPropertyModel?) =
    (parsedModel as? FileTreeDependencyModel)?.propertyGetter()?.toList()?.map { it.resolve().asString().orEmpty() }.orEmpty()

  override val includes: List<String> = getFileTreeStringListProperty { includes() }
  override val excludes: List<String> = getFileTreeStringListProperty { excludes() }

  override val isDeclared: Boolean = true
  override val configurationName: String = parsedModel.configurationName()
  override val joinedConfigurationNames: String = configurationName

  object Descriptor : ModelDescriptor<PsDeclaredJarAndroidDependency, Nothing, DependencyModel> {
    override fun getResolved(model: PsDeclaredJarAndroidDependency): Nothing? = null
    override fun getParsed(model: PsDeclaredJarAndroidDependency): DependencyModel? = model.parsedModel
    override fun prepareForModification(model: PsDeclaredJarAndroidDependency) = Unit

    override fun setModified(model: PsDeclaredJarAndroidDependency) {
      TODO("NOTE: There is no need to re-index the declared dependency collection. Version is not a part of the key.")
      model.isModified = true
      // TODO(solodkyy): Make setModified() customizable at the property level since some properties will need to call resetDependencies().
      model.parent.resetResolvedDependencies()
      model.parent.fireDependencyModifiedEvent(lazy {
        model.parent.dependencies.findJarDependencies(model.filePath).firstOrNull { it.configurationName == model.configurationName }
      })
    }

    override val properties: Collection<ModelProperty<PsDeclaredJarAndroidDependency, *, *, *>> = listOf()
  }
}

class PsResolvedJarAndroidDependency(
  parent: PsAndroidModule,
  val collection: PsAndroidArtifactDependencyCollection,
  override val filePath: String,
  val artifact: PsAndroidArtifact,
  override val declaredDependencies: List<PsDeclaredJarAndroidDependency>
): PsJarAndroidDependency(parent, listOf(artifact)), PsResolvedJarDependency {
  override val isDeclared: Boolean get() = !declaredDependencies.isEmpty()
}

abstract class PsJarAndroidDependency internal constructor(
  parent: PsAndroidModule,
  containers: Collection<PsAndroidArtifact>
) : PsAndroidDependency(parent, containers), PsJarDependency {

  override val name: String get() = filePath

  override fun toText(): String = filePath

  override fun toString(): String = toText()

}
