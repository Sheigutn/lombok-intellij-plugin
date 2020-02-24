package de.plushnikov.intellij.plugin.completion;

import com.intellij.codeInsight.completion.*;
import com.intellij.openapi.project.Project;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ProcessingContext;
import de.plushnikov.intellij.plugin.psi.LombokLightMethodBuilder;
import de.plushnikov.intellij.plugin.util.PsiAnnotationSearchUtil;
import de.plushnikov.intellij.plugin.util.PsiAnnotationUtil;
import lombok.experimental.ExtensionMethod;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class LombokCompletionContributor extends CompletionContributor {

  public LombokCompletionContributor() {
    extend(CompletionType.BASIC,
      PlatformPatterns.psiElement( PsiIdentifier.class ),
      new CompletionProvider<CompletionParameters>()
      {
        public void addCompletions(@NotNull CompletionParameters parameters,
                                   @NotNull ProcessingContext context,
                                   @NotNull CompletionResultSet resultSet)
        {
          PsiType typeToSearchFor = null;
          PsiElement elementAtPosition = parameters.getPosition();
          Project project = elementAtPosition.getProject();
          if (elementAtPosition instanceof PsiIdentifier) {
            PsiElement parent = elementAtPosition.getParent();
            if (parent instanceof PsiReferenceExpression) {
              PsiReferenceExpression psiReferenceExpression = (PsiReferenceExpression) parent;
              PsiElement firstChild = psiReferenceExpression.getFirstChild();
              if (firstChild instanceof PsiReferenceExpression) {
                PsiReferenceExpression firstChildExpression = (PsiReferenceExpression) firstChild;
                typeToSearchFor = firstChildExpression.getType();
              }
              else if (firstChild instanceof PsiLiteralExpression) {
                String text = firstChild.getText();
                if ("null".equals(text)) {
                  typeToSearchFor = PsiType.getJavaLangObject(elementAtPosition.getManager(), GlobalSearchScope.allScope(elementAtPosition.getProject()));
                }
                else {
                  typeToSearchFor = PsiType.getJavaLangString(elementAtPosition.getManager(), GlobalSearchScope.allScope(elementAtPosition.getProject()));
                }
              }
            }
          }
          if (typeToSearchFor != null) {
            System.out.println(typeToSearchFor);
          }
          PsiClass containedClass = PsiTreeUtil.getParentOfType(elementAtPosition, PsiClass.class);
          Collection<PsiType> types = null;
          if (containedClass != null && PsiAnnotationSearchUtil.isAnnotatedWith(containedClass, ExtensionMethod.class)) {
            PsiAnnotation psiAnnotation = Objects.requireNonNull(containedClass.getAnnotation("lombok.experimental.ExtensionMethod"));
            types = PsiAnnotationUtil.getAnnotationValues(psiAnnotation, "value", PsiType.class);
          }
          if (types != null) {
            List<PsiMethod> allStaticMethods = types.stream()
              .filter(type -> type instanceof PsiClassType)
              .map(type -> ((PsiClassType) type).resolve())
              .filter(Objects::nonNull)
              .flatMap(clazz -> Stream.of(clazz.getMethods()))
              .filter(method -> method.getModifierList().hasModifierProperty(PsiModifier.PUBLIC))
              .filter(method -> method.getModifierList().hasModifierProperty(PsiModifier.STATIC))
              .filter(method -> !method.getParameterList().isEmpty())
              .collect(Collectors.toList());

            for (PsiMethod psiMethod : allStaticMethods) {
              PsiParameter[] methodParameters = psiMethod.getParameterList().getParameters();
              PsiParameter firstParameter = methodParameters[0];
              PsiType firstParameterType = firstParameter.getType();
              PsiTypeParameter[] typeParameters = psiMethod.getTypeParameters();
              PsiSubstitutor psiSubstitutor = PsiSubstitutor.EMPTY;

              /*if (typeParameters.length > 0) {
                if (firstParameter.getType() instanceof PsiClassType) {
                  PsiClass psiClass = ((PsiClassType) firstParameter.getType()).resolve();
                  if (psiClass instanceof PsiTypeParameter) {
                    PsiTypeParameter firstTypeParameter = (PsiTypeParameter) psiClass;
                    psiSubstitutor = psiSubstitutor.put(firstTypeParameter, typeToSearchFor);
                    if (!GenericsUtil.isTypeArgumentsApplicable(new PsiTypeParameter[]{firstTypeParameter}, psiSubstitutor, null)) {
                      psiSubstitutor = PsiSubstitutor.EMPTY;
                    }
                  }
                }
              }*/

              if (typeToSearchFor != null && (psiSubstitutor.substitute(firstParameterType).isAssignableFrom(typeToSearchFor))) {
                List<String> modifiers = getModifiers(psiMethod);
                modifiers.remove(PsiModifier.PUBLIC);
                modifiers.remove(PsiModifier.STATIC);

                LombokLightMethodBuilder methodBuilder = new LombokLightMethodBuilder(elementAtPosition.getManager(), psiMethod.getName())
                  .withModifier(modifiers.toArray(new String[0]))
                  .withNavigationElement(psiMethod)
                  .withMethodReturnType(psiSubstitutor.substitute(psiMethod.getReturnType()));

                for (int index = 1; index < methodParameters.length; index++) {
                  PsiParameter parameter = methodParameters[index];
                  methodBuilder.withParameter(parameter.getName(), psiSubstitutor.substitute(parameter.getType()));
                }

                resultSet.addElement(
                  new JavaMethodCallElement(methodBuilder)
                );
              }
            }
          }
        }
      });
  }

  private List<String> getModifiers(PsiMethod method) {
    PsiModifierList modifierList = method.getModifierList();
    return Arrays.stream(PsiModifier.MODIFIERS).filter(modifierList::hasModifierProperty).collect(Collectors.toList());
  }
}
