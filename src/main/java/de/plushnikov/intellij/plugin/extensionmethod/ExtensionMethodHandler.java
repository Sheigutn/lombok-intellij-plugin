package de.plushnikov.intellij.plugin.extensionmethod;

import com.intellij.codeInsight.Nullability;
import com.intellij.codeInsight.NullableNotNullManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiIdentifier;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.util.PsiTreeUtil;
import de.plushnikov.intellij.plugin.psi.LombokLightMethodBuilder;

public class ExtensionMethodHandler {

  public static boolean isFirstParameterDeemedNullable(PsiElement psiElement) {
    PsiMethodCallExpression psiMethodCallExpression = getMethodCallExpression(psiElement);
    if (psiMethodCallExpression == null) {
      return false;
    }

    PsiMethod psiMethod = psiMethodCallExpression.resolveMethod();

    if (!(isExtensionMethod(psiMethod))) {
      return false;
    }

    LombokLightMethodBuilder lombokExtensionMethod = ((LombokLightMethodBuilder) psiMethod);
    PsiMethod navigationElement = ((PsiMethod) lombokExtensionMethod.getNavigationElement());

    return NullableNotNullManager.getNullability(navigationElement.getParameterList().getParameter(0)) == Nullability.NULLABLE;
  }

  public static boolean isExtensionMethodCall(PsiElement psiElement) {
    PsiMethodCallExpression psiMethodCallExpression = getMethodCallExpression(psiElement);
    if (psiMethodCallExpression == null) {
      return false;
    }

    PsiMethod psiMethod = psiMethodCallExpression.resolveMethod();

    return isExtensionMethod(psiMethod);
  }

  private static boolean isExtensionMethod(PsiMethod psiMethod) {
    if (!(psiMethod instanceof LombokLightMethodBuilder)) {
      return false;
    }

    return ((LombokLightMethodBuilder) psiMethod).isExtension();
  }

  private static PsiMethodCallExpression getMethodCallExpression(PsiElement psiElement) {
    if (!(psiElement instanceof PsiIdentifier)) {
      return null;
    }

    return PsiTreeUtil.getParentOfType(psiElement, PsiMethodCallExpression.class, false);
  }
}
