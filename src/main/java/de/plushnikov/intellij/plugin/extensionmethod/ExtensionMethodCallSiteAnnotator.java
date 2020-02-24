package de.plushnikov.intellij.plugin.extensionmethod;

import com.intellij.codeInsight.daemon.JavaErrorBundle;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import de.plushnikov.intellij.plugin.psi.LombokLightMethodBuilder;
import de.plushnikov.intellij.plugin.util.PsiAnnotationUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

import static de.plushnikov.intellij.plugin.extensionmethod.LombokExtensionMethodElementFinder.EXTENSION_METHOD_ANNOTATION_FQN;

public class ExtensionMethodCallSiteAnnotator implements Annotator {

  @Override
  public void annotate(@NotNull PsiElement element, @NotNull AnnotationHolder holder) {
    if (!(element instanceof PsiMethodCallExpression)) {
      return;
    }

    PsiMethodCallExpression psiMethodCallExpression = (PsiMethodCallExpression) element;
    PsiMethod method = psiMethodCallExpression.resolveMethod();

    if (!(method instanceof LombokLightMethodBuilder)) {
      return;
    }

    LombokLightMethodBuilder lombokLightMethodBuilder = (LombokLightMethodBuilder) method;

    if (!lombokLightMethodBuilder.isExtension()) {
      return;
    }

    PsiClass parentClass = PsiTreeUtil.getParentOfType(element, PsiClass.class);

    if (parentClass == null) {
      return;
    }

    PsiAnnotation annotation = parentClass.getAnnotation(EXTENSION_METHOD_ANNOTATION_FQN);

    if (annotation == null) {
      annotateMethodCall(psiMethodCallExpression, holder);
      return;
    }

    PsiType[] typesInAnnotation = PsiAnnotationUtil
      .getAnnotationValues(parentClass.getAnnotation(EXTENSION_METHOD_ANNOTATION_FQN), "value", PsiType.class)
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

  private void annotateMethodCall(PsiMethodCallExpression psiMethodCallExpression, AnnotationHolder holder) {
    PsiReferenceExpression methodExpression = psiMethodCallExpression.getMethodExpression();
    PsiElement referenceNameElement = methodExpression.getReferenceNameElement();
    if (referenceNameElement != null) {
      TextRange textRange = referenceNameElement.getTextRange();
      TextRange range = new TextRange(textRange.getStartOffset(), textRange.getEndOffset());
      holder.createErrorAnnotation(range, JavaErrorBundle.message("cannot.resolve.method", methodExpression.getReferenceName()));
    }
  }

  /**
   * if (element instanceof PsiMethodCallExpression) {
   *                 PsiReferenceExpression methodExpression = ((PsiMethodCallExpression)element).getMethodExpression();
   *                 PsiElement member = methodExpression.resolve();
   *                 if (member instanceof ManLightMethodBuilder) {
   *                     Module callSiteModule = ModuleUtil.findModuleForPsiElement(element);
   *                     if (callSiteModule != null && ((ManLightMethodBuilder)member).getModules().stream().map(ManModule::getIjModule).noneMatch((extensionModule) -> {
   *                         return this.isAccessible(callSiteModule, extensionModule, methodExpression);
   *                     })) {
   *                         PsiElement methodElem = methodExpression.getReferenceNameElement();
   *                         if (methodElem != null) {
   *                             TextRange textRange = methodElem.getTextRange();
   *                             TextRange range = new TextRange(textRange.getStartOffset(), textRange.getEndOffset());
   *                             holder.createErrorAnnotation(range, JavaErrorMessages.message("cannot.resolve.method", new Object[]{methodExpression.getReferenceName()}));
   *                         }
   *                     }
   *                 }
   *             }
   */
}
