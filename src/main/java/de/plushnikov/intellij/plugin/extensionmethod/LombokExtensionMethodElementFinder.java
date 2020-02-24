package de.plushnikov.intellij.plugin.extensionmethod;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiElementFinder;
import com.intellij.psi.PsiType;
import com.intellij.psi.impl.file.impl.JavaFileManager;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.searches.AnnotatedElementsSearch;
import com.intellij.util.Query;
import de.plushnikov.intellij.plugin.settings.ProjectSettings;
import de.plushnikov.intellij.plugin.util.PsiAnnotationUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

public class LombokExtensionMethodElementFinder extends PsiElementFinder {

  public static final String EXTENSION_METHOD_ANNOTATION_FQN = "lombok.experimental.ExtensionMethod";

  private final Project project;
  private final JavaFileManager myFileManager;

  public LombokExtensionMethodElementFinder(Project project) {
    this.project = project;
    myFileManager = JavaFileManager.getInstance(project);
  }

  @Nullable
  @Override
  public PsiClass findClass(@NotNull String qualifiedName, @NotNull GlobalSearchScope scope) {
    if (!ProjectSettings.isLombokEnabledInProject(project)) {
      return null;
    }

    PsiClass javaClass = myFileManager.findClass(qualifiedName, scope);

    if (javaClass == null) {
      return null;
    }

    PsiClass[] extensionClasses = findExtensionClasses(scope);

    if (extensionClasses.length == 0) {
      return null;
    }

    return new LightClassWithExtensions(javaClass, extensionClasses);
  }

  @NotNull
  @Override
  public PsiClass[] findClasses(@NotNull String qualifiedName, @NotNull GlobalSearchScope scope) {
    if (!ProjectSettings.isLombokEnabledInProject(project)) {
      return PsiClass.EMPTY_ARRAY;
    }

    PsiClass[] classes = myFileManager.findClasses(qualifiedName, scope);

    if (classes.length == 0) {
      return classes;
    }

    PsiClass[] extensionClasses = findExtensionClasses(scope);

    if (extensionClasses.length == 0) {
      return PsiClass.EMPTY_ARRAY;
    }

    return Arrays.stream(classes).map(clazz -> new LightClassWithExtensions(clazz, extensionClasses)).toArray(PsiClass[]::new);
  }

  private PsiClass[] findExtensionClasses(@NotNull GlobalSearchScope scope) {
    PsiClass extensionMethodClass = myFileManager.findClass(EXTENSION_METHOD_ANNOTATION_FQN, scope);
    if (extensionMethodClass == null) {
      return PsiClass.EMPTY_ARRAY;
    }

    Query<PsiClass> psiClasses = AnnotatedElementsSearch.searchPsiClasses(extensionMethodClass, scope);

    return psiClasses.findAll()
      .stream()
      .flatMap(psiClass -> {
        //noinspection ConstantConditions
        return PsiAnnotationUtil.getAnnotationValues(psiClass.getAnnotation(EXTENSION_METHOD_ANNOTATION_FQN), "value", PsiType.class).stream();
      })
      .filter(psiType -> psiType instanceof PsiClassType)
      .map(psiType -> ((PsiClassType) psiType).resolve())
      .distinct()
      .toArray(PsiClass[]::new);
  }
}
