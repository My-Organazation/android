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
package com.android.tools.idea.tests.gui.npw;

import com.android.tools.idea.flags.StudioFlags;
import com.android.tools.idea.gradle.dsl.api.GradleBuildModel;
import com.android.tools.idea.gradle.dsl.api.ProjectBuildModel;
import com.android.tools.idea.gradle.dsl.api.android.productFlavors.externalNativeBuild.CMakeOptionsModel;
import com.android.tools.idea.gradle.dsl.api.ext.ResolvedPropertyModel;
import com.android.tools.idea.sdk.IdeSdks;
import com.android.tools.idea.tests.gui.emulator.EmulatorTestRule;
import com.android.tools.idea.tests.gui.framework.GuiTestRule;
import com.android.tools.idea.tests.gui.framework.fixture.ExecutionToolWindowFixture;
import com.android.tools.idea.tests.gui.framework.fixture.IdeFrameFixture;
import com.android.tools.idea.tests.gui.framework.fixture.ProjectViewFixture;
import com.android.tools.idea.tests.gui.framework.fixture.npw.NewProjectWizardFixture;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import org.fest.swing.timing.Wait;
import org.fest.swing.util.PatternTextMatcher;
import org.fest.swing.util.StringTextMatcher;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

public class NewCppProjectTestUtil {

  private static final String APP_NAME = "app";
  private static final Pattern LOCAL_PATH_OUTPUT = Pattern.compile(".*adb shell am start .*myapplication\\.MainActivity.*", Pattern.DOTALL);
  private static final Pattern RUN_OUTPUT = Pattern.compile(".*Connected to process.*", Pattern.DOTALL);

  protected static void createNewProjectWithCpp(boolean hasExceptionSupport, boolean hasRuntimeInformation, GuiTestRule guiTest) throws Exception {
    createCppProject(hasExceptionSupport, hasRuntimeInformation, guiTest);

    IdeFrameFixture ideFrame = guiTest.ideFrame();

    // TODO remove the following hack: b/110174414
    File androidSdk = IdeSdks.getInstance().getAndroidSdkPath();
    File ninja = new File(androidSdk, "cmake/3.10.4819442/bin/ninja");

    AtomicReference<IOException> buildGradleFailure = new AtomicReference<>();
    ApplicationManager.getApplication().invokeAndWait(() -> {
      WriteCommandAction.runWriteCommandAction(ideFrame.getProject(), () -> {
        ProjectBuildModel pbm = ProjectBuildModel.get(ideFrame.getProject());
        GradleBuildModel buildModel = pbm.getModuleBuildModel(ideFrame.getModule("app"));
        CMakeOptionsModel cmakeModel = buildModel
          .android()
          .defaultConfig()
          .externalNativeBuild()
          .cmake();

        ResolvedPropertyModel cmakeArgsModel = cmakeModel.arguments();
        try {
          cmakeArgsModel.setValue("-DCMAKE_MAKE_PROGRAM=" + ninja.getCanonicalPath());
          buildModel.applyChanges();
        }
        catch (IOException failureToWrite) {
          buildGradleFailure.set(failureToWrite);
        }
      });
    });
    IOException errorsWhileModifyingBuild = buildGradleFailure.get();
    if(errorsWhileModifyingBuild != null) {
      throw errorsWhileModifyingBuild;
    }
    // TODO end hack for b/110174414

    String gradleCppFlags = ideFrame.getEditor()
                                    .open("app/build.gradle")
                                    .moveBetween("cppFlags \"", "")
                                    .getCurrentLine();

    String cppFlags = String.format("%s %s", hasRuntimeInformation ? "-frtti" : "",  hasExceptionSupport ? "-fexceptions" : "").trim();
    Assert.assertEquals(String.format("cppFlags \"%s\"", cppFlags), gradleCppFlags.trim());

    runAppOnEmulator(ideFrame);
  }

  protected static void createCppProject(boolean hasExceptionSupport, boolean hasRuntimeInformation, GuiTestRule guiTest) {
    NewProjectWizardFixture newProjectWizard = guiTest.welcomeFrame()
                                                      .createNewProject();
    if (StudioFlags.NPW_DYNAMIC_APPS.get()) {
      newProjectWizard
        .getChooseAndroidProjectStep()
        .chooseActivity("Native C++")
        .wizard()
        .clickNext()
        .getConfigureNewAndroidProjectStep()
        .enterPackageName("com.example.myapplication")
        .wizard()
        .clickNext();
    }
    else {
      newProjectWizard.getConfigureAndroidProjectStep()
                      .enterPackageName("com.example.myapplication")
                      .setCppSupport(true); // Default "App name", "company domain" and "package name"
      newProjectWizard.clickNext();
      newProjectWizard.clickNext(); // Skip "Select minimum SDK Api" step
      newProjectWizard.clickNext(); // Skip "Add Activity" step
      newProjectWizard.clickNext(); // Use default activity names
    }

    newProjectWizard.getConfigureCppStepFixture()
                    .setExceptionsSupport(hasExceptionSupport)
                    .setRuntimeInformationSupport(hasRuntimeInformation);

    newProjectWizard.clickFinish();

    guiTest.ideFrame().waitForGradleProjectSyncToFinish(Wait.seconds(60));
    guiTest.waitForBackgroundTasks();
  }

  protected static void runAppOnEmulator(@NotNull IdeFrameFixture ideFrame) {
    ideFrame.runApp(APP_NAME)
            .selectDevice(new StringTextMatcher("Google Nexus 5X"))
            .clickOk();

    // Make sure the right app is being used. This also serves as the sync point for the package to get uploaded to the device/emulator.
    ExecutionToolWindowFixture.ContentFixture contentFixture = ideFrame.getRunToolWindow().findContent(APP_NAME);
    contentFixture.waitForOutput(new PatternTextMatcher(LOCAL_PATH_OUTPUT), EmulatorTestRule.DEFAULT_EMULATOR_WAIT_SECONDS);
    contentFixture.waitForOutput(new PatternTextMatcher(RUN_OUTPUT), EmulatorTestRule.DEFAULT_EMULATOR_WAIT_SECONDS);
  }

  @NotNull
  protected static String getExternalNativeBuildRegExp() {
    return "(externalNativeBuild.*\n" + // externalNativeBuild {
           ".*  cmake .*\n" +           //   cmake {
           ".*    path.*\n" +           //     path "CMakeLists.txt"
           ".*\n" +                     //   }
           ".*)";
  }

  protected static void assertAndroidPanePath(boolean expectedToExist, GuiTestRule guiTest, @NotNull String... path) {
    ProjectViewFixture.PaneFixture androidPane = guiTest.ideFrame().getProjectView()
                                                        .selectAndroidPane();

    try {
      androidPane.clickPath(path);
    }
    catch (Throwable ex) {
      if (expectedToExist) {
        throw ex;
      }
    }
  }
}
