package de.plushnikov.intellij.plugin.extensionmethod;

import com.intellij.codeInsight.daemon.JavaErrorBundle;
import com.intellij.codeInspection.AbstractBaseJavaLocalInspectionTool;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import de.plushnikov.intellij.plugin.psi.LombokLightMethodBuilder;
import de.plushnikov.intellij.plugin.util.PsiAnnotationUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

import static de.plushnikov.intellij.plugin.extensionmethod.LombokExtensionMethodElementFinder.EXTENSION_METHOD_ANNOTATION_FQN;

public class ExtensionMethodCallSiteInspection extends AbstractBaseJavaLocalInspectionTool {

  @NotNull
  @Override
  public PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
    return new JavaElementVisitor() {
      @Override
      public void visitMethodCallExpression(PsiMethodCallExpression psiMethodCallExpression) {
        PsiMethod method = psiMethodCallExpression.resolveMethod();

        if (!(method instanceof LombokLightMethodBuilder)) {
          return;
        }

        LombokLightMethodBuilder lombokLightMethodBuilder = (LombokLightMethodBuilder) method;

        if (!lombokLightMethodBuilder.isExtension()) {
          return;
        }

        PsiClass parentClass = PsiTreeUtil.getParentOfType(psiMethodCallExpression, PsiClass.class);

        if (parentClass == null) {
          return;
        }

        PsiAnnotation psiAnnotation = parentClass.getAnnotation(EXTENSION_METHOD_ANNOTATION_FQN);

        if (psiAnnotation == null) {
          annotateMethodCall(psiMethodCallExpression, holder);
          return;
        }

        PsiType[] typesInAnnotation = PsiAnnotationUtil
          .getAnnotationValues(psiAnnotation, "value", PsiType.class)
          .toArray(PsiType.EMPTY_ARRAY);

        PsiMethod referencedMethod = ((PsiMethod) lombokLightMethodBuilder.getNavigationElement());
        PsiClass referencedClass = referencedMethod.getContainingClass();

        boolean methodInAnyClass = Arrays.stream(typesInAnnotation)
          .filter(psiType -> psiType instanceof PsiClassType)
          .map(psiType -> ((PsiClassType) psiType).resolve())
          .anyMatch(referencedClass::isEquivalentTo);

        if (!methodInAnyClass) {
          annotateMethodCall(psiMethodCallExpression, holder);
        }
      }

      private void annotateMethodCall(PsiMethodCallExpression psiMethodCallExpression, ProblemsHolder holder) {
        PsiReferenceExpression methodExpression = psiMethodCallExpression.getMethodExpression();
        holder.registerProblem(methodExpression, JavaErrorBundle.message("cannot.resolve.method", methodExpression.getReferenceName()), ProblemHighlightType.ERROR);
      }
    };
  }
}
