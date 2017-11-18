/*
 * Copyright (C) 2017 The Android Open Source Project
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
package com.android.tools.idea.naveditor.scene.draw

import com.android.tools.adtui.common.SwingCoordinate
import com.android.tools.idea.common.scene.SceneContext
import java.awt.BasicStroke
import java.awt.Graphics2D
import java.awt.Rectangle

class DrawRectangle(@SwingCoordinate private val myRectangle: Rectangle,
                    @SwingCoordinate private val myColor: DrawColor,
                    @SwingCoordinate private val myBrushThickness: Int,
                    @SwingCoordinate private val myArcSize: Int) : NavBaseDrawCommand() {

  constructor(myRectangle: Rectangle, myColor: DrawColor, myBrushThickness: Int) : this(myRectangle, myColor, myBrushThickness, 0)

  private constructor(sp: Array<String>) : this(NavBaseDrawCommand.stringToRect(sp[0]), DrawColor.valueOf(sp[1]),
      sp[2].toInt(), sp[3].toInt())

  constructor(s: String) : this(parse(s, 4))

  override fun getLevel(): Int {
    return NavBaseDrawCommand.DRAW_FRAME_LEVEL
  }

  override fun getProperties(): Array<Any> {
    return arrayOf(NavBaseDrawCommand.rectToString(myRectangle), myColor, myBrushThickness, myArcSize)
  }

  override fun onPaint(g: Graphics2D, sceneContext: SceneContext) {
    g.color = myColor.color(sceneContext)
    g.stroke = BasicStroke(myBrushThickness.toFloat())
    g.drawRoundRect(myRectangle.x, myRectangle.y, myRectangle.width, myRectangle.height, myArcSize, myArcSize)
  }
}