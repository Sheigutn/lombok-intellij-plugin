package de.plushnikov.intellij.plugin.inspection.importstatic;

import com.intellij.codeInsight.intention.impl.AddOnDemandStaticImportAction;
import com.intellij.codeInspection.*;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import de.plushnikov.intellij.plugin.processor.modifier.UtilityClassModifierProcessor;
import de.plushnikov.intellij.plugin.psi.LombokLightClassBuilder;
import de.plushnikov.intellij.plugin.psi.LombokLightFieldBuilder;
import de.plushnikov.intellij.plugin.psi.LombokLightMethodBuilder;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

import static com.intellij.psi.PsiModifier.STATIC;

public class HighlightStaticImportedLombokMembersInspection extends AbstractBaseJavaLocalInspectionTool {

  @NotNull
  @Override
  public PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
    return new StaticImportedLombokMembersVisitor(holder);
  }

  private static class StaticImportedLombokMembersVisitor extends JavaElementVisitor {

    private final ProblemsHolder holder;

    public StaticImportedLombokMembersVisitor(ProblemsHolder holder) {
      this.holder = holder;
    }

    @Override
    public void visitImportStaticReferenceElement(PsiImportStaticReferenceElement reference) {
      PsiElement resolvedElement = reference.resolve();
      if (resolvedElement instanceof PsiMethod || resolvedElement instanceof PsiField || resolvedElement instanceof PsiClass) {
        PsiModifierListOwner modifierListOwner = (PsiModifierListOwner) resolvedElement;
        if ((((resolvedElement instanceof LombokLightMethodBuilder)
          || (resolvedElement instanceof LombokLightFieldBuilder)
          || (resolvedElement instanceof LombokLightClassBuilder))
          && modifierListOwner.hasModifierProperty(STATIC)
        ) || UtilityClassModifierProcessor.isModifierListSupported(Objects.requireNonNull(modifierListOwner.getModifierList()))) {
          holder.registerProblem(reference.getParent(),
            "Due to a peculiar way javac processes static imports, trying to do a non-star static import of any static members marked or generated by Lombok won't work.\n" +
              "Either use a star static import: `import static `" + reference.getQualifier().getText() + ".*;` or don't statically import any of the members.",
            ProblemHighlightType.ERROR,
            new AddOnDemandStaticImportQuickFix(reference));
        }
      }
    }
  }

  private static class AddOnDemandStaticImportQuickFix implements LocalQuickFix {

    private final PsiImportStaticReferenceElement reference;

    public AddOnDemandStaticImportQuickFix(PsiImportStaticReferenceElement reference) {
      this.reference = reference;
    }

    @Nls(capitalization = Nls.Capitalization.Sentence)
    @NotNull
    @Override
    public String getName() {
      return "Add static import for " + reference.getQualifier().getText() + ".*";
    }

    @Nls(capitalization = Nls.Capitalization.Sentence)
    @NotNull
    @Override
    public String getFamilyName() {
      return "Add static import";
    }

    @Override
    public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
      PsiElement toProcess;
      if ((toProcess = reference.getFirstChild()) != null && (toProcess = toProcess.getFirstChild()) != null) {
        new AddOnDemandStaticImportAction().invoke(project, null, toProcess);
        reference.getParent().delete();
      }
    }
  }
}
