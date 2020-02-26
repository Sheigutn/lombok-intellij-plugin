package de.plushnikov.intellij.plugin.extensionmethod;

import com.intellij.psi.*;
import com.intellij.psi.impl.light.LightClass;
import de.plushnikov.intellij.plugin.psi.LombokLightMethodBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class LightClassWithExtensions extends LightClass {

  private final PsiClass[] extensionClasses;

  public LightClassWithExtensions(@NotNull PsiClass delegate, @NotNull PsiClass[] extensionClasses) {
    super(delegate);
    this.extensionClasses = extensionClasses;
  }

  @NotNull
  @Override
  public PsiMethod[] getMethods() {
    PsiMethod[] delegateMethods = getDelegateMethods();

    PsiMethod[] extensionMethods = generateExtensionMethods();

    if (extensionMethods.length == 0) {
      return delegateMethods;
    }

    return Stream.concat(Arrays.stream(delegateMethods), Arrays.stream(extensionMethods)).toArray(PsiMethod[]::new);
  }

  private PsiMethod[] generateExtensionMethods() {
    List<PsiMethod> extensionMethods = new ArrayList<>();
    for (PsiClass psiClass : extensionClasses) {

      PsiMethod[] allMethods;

      if (psiClass instanceof LightClassWithExtensions) {
        allMethods = ((LightClassWithExtensions) psiClass).getDelegateMethods();
      }
      else if (psiClass != null) {
        allMethods = psiClass.getMethods();
      }
      else {
        allMethods = PsiMethod.EMPTY_ARRAY;
      }

      List<PsiMethod> staticMethods = Arrays.stream(allMethods)
        .filter(psiMethod -> psiMethod.getModifierList().hasModifierProperty(PsiModifier.PUBLIC))
        .filter(psiMethod -> psiMethod.getModifierList().hasModifierProperty(PsiModifier.STATIC))
        .collect(Collectors.toList());

      for (PsiMethod staticMethod : staticMethods) {

        PsiParameterList psiParameterList = staticMethod.getParameterList();

        if (psiParameterList.isEmpty()) continue;

        PsiParameter firstParameter = psiParameterList.getParameter(0);
        //noinspection ConstantConditions
        PsiType firstParameterType = firstParameter.getType();

        if (!(firstParameterType instanceof PsiClassType)) continue;

        PsiClass parameterClass = ((PsiClassType) firstParameterType).resolve();

        boolean firstParameterIsTypeParameter = parameterClass instanceof PsiTypeParameter;

        if (firstParameterIsTypeParameter) {
          PsiTypeParameter typeParameter = (PsiTypeParameter) parameterClass;
          PsiClassType[] referencedTypes = typeParameter.getExtendsList().getReferencedTypes();
          boolean isSubtypeOf = true;
          PsiType thisClassType = JavaPsiFacade.getElementFactory(getProject()).createType(this);
          for (PsiClassType subType : referencedTypes) {
            if (!subType.isAssignableFrom(thisClassType)) {
              isSubtypeOf = false;
              break;
            }
          }
          if (!isSubtypeOf) {
            parameterClass = null;
          }
        }

        if (!(firstParameterIsTypeParameter && parameterClass != null) && (parameterClass == null || !(parameterClass.isEquivalentTo(this)))) continue;

        PsiSubstitutor psiSubstitutor = PsiSubstitutor.EMPTY;
        if (firstParameterIsTypeParameter) {
          psiSubstitutor = psiSubstitutor.put(((PsiTypeParameter) parameterClass), JavaPsiFacade.getElementFactory(getProject()).createType(this));
        }

        LombokLightMethodBuilder methodBuilder = new LombokLightMethodBuilder(psiClass.getManager(), staticMethod.getName())
          .withMethodReturnType(psiSubstitutor.substitute(staticMethod.getReturnType()))
          .withAnnotations(Arrays.stream(staticMethod.getAnnotations()).map(PsiAnnotation::getQualifiedName).collect(Collectors.toList()))
          .withContainingClass(this)
          .withExtension(true)
          .withDocComment(staticMethod.getDocComment())
          .withNavigationElement(staticMethod)
          .withModifier(getAllModifiers(staticMethod, PsiModifier.STATIC))
          .withModifier(PsiModifier.FINAL);

        for (int i = 1; i < psiParameterList.getParametersCount(); i++) {
          PsiParameter parameterAtIndex = psiParameterList.getParameter(i);
          methodBuilder.withParameter(parameterAtIndex.getName(), psiSubstitutor.substitute(parameterAtIndex.getType()));
        }

        for (PsiClassType exceptionType : staticMethod.getThrowsList().getReferencedTypes()) {
          methodBuilder.withException(exceptionType);
        }

        extensionMethods.add(methodBuilder);
      }
    }

    return extensionMethods.toArray(PsiMethod.EMPTY_ARRAY);
  }

  private PsiMethod[] getDelegateMethods() {
    return getDelegate().getMethods();
  }

  @NotNull
  private String[] getAllModifiers(PsiModifierListOwner psiModifierListOwner, String... excludes) {
    PsiModifierList psiModifierList = psiModifierListOwner.getModifierList();

    if (psiModifierList == null) {
      return new String[0];
    }

    List<String> excludesList = Arrays.asList(excludes);
    return Arrays.stream(PsiModifier.MODIFIERS)
      .filter(psiModifierList::hasModifierProperty)
      .filter(modifier -> !excludesList.contains(modifier))
      .toArray(String[]::new);
  }
}
