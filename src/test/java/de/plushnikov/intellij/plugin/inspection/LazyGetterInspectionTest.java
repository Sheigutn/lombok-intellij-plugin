package de.plushnikov.intellij.plugin.inspection;

import com.intellij.codeInspection.InspectionProfileEntry;
import de.plushnikov.intellij.plugin.inspection.getter.LazyGetterFieldUsageInspection;
import org.jetbrains.annotations.Nullable;

public class LazyGetterInspectionTest extends LombokInspectionTest {

  @Override
  protected String getTestDataPath() {
    return TEST_DATA_INSPECTION_DIRECTORY + "/lazyGetter";
  }

  @Nullable
  @Override
  protected InspectionProfileEntry getInspection() {
    return new LazyGetterFieldUsageInspection();
  }

  public void testGetterLazyMethodCall() {
    doTest();
  }
}
