package de.plushnikov.intellij.plugin.extensionmethod;

import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.LineMarkerProviderDescriptor;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.FunctionUtil;
import de.plushnikov.intellij.plugin.icon.LombokIcons;
import de.plushnikov.intellij.plugin.psi.LombokLightMethodBuilder;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ExtensionMethodLineMarkerProvider extends LineMarkerProviderDescriptor {

  @Override
  public LineMarkerInfo getLineMarkerInfo(@NotNull PsiElement element) {
    return null; //do nothing
  }

  @Override
  public void collectSlowLineMarkers(@NotNull List<PsiElement> elements,
                                     @NotNull Collection<LineMarkerInfo> result) {
    final Set<PsiStatement> statements = new HashSet<PsiStatement>();

    for (PsiElement element : elements) {
      ProgressManager.checkCanceled();
      if (element instanceof PsiMethodCallExpression) {
        final PsiMethodCallExpression methodCall = (PsiMethodCallExpression)element;
        final PsiStatement statement = PsiTreeUtil.getParentOfType(methodCall, PsiStatement.class, true, PsiMethod.class);
        if (!statements.contains(statement) && isExtensionMethodCall(methodCall)) {
          statements.add(statement);
          result.add(new ExtensionMethodCallMarkerInfo(methodCall, methodCall.getTextRange()));
        }
      }
    }
  }

  public static boolean isExtensionMethodCall(@NotNull PsiMethodCallExpression methodCall) {
    PsiMethod method = methodCall.resolveMethod();

    if (!(method instanceof LombokLightMethodBuilder)) {
      return false;
    }

    return ((LombokLightMethodBuilder) method).isExtension();
  }

  @Nls(capitalization = Nls.Capitalization.Sentence)
  @Nullable("null means disabled")
  @Override
  public String getName() {
    return "Reference to extension method call";
  }

  @Nullable
  @Override
  public Icon getIcon() {
    return LombokIcons.PUZZLE_ICON;
  }

  private static class ExtensionMethodCallMarkerInfo extends LineMarkerInfo<PsiElement> {
    private ExtensionMethodCallMarkerInfo(@NotNull PsiElement psiElement, TextRange textRange) {
      super(psiElement,
        textRange,
        LombokIcons.PUZZLE_ICON,
        FunctionUtil.constant("Reference to extension method call"),
        null,
        GutterIconRenderer.Alignment.RIGHT
      );
    }

    @Override
    public GutterIconRenderer createGutterRenderer() {
      if (myIcon == null) return null;
      return new LineMarkerGutterIconRenderer<PsiElement>(this){
        @Override
        public AnAction getClickAction() {
          return null; // to place breakpoint on mouse click
        }
      };
    }
  }
}
