package arrow.meta.dsl.ide.editor.icon

import arrow.meta.phases.ExtensionPhase
import arrow.meta.plugin.idea.IdeMetaPlugin
import com.intellij.ide.IconProvider
import com.intellij.openapi.project.DumbAware
import com.intellij.psi.PsiElement
import javax.swing.Icon

interface IconProviderSyntax {
  /**
   * For emitting FileIcons or Icons in the StructureView
   */
  fun IdeMetaPlugin.addIcon(
    icon: Icon? = null,
    matchOn: (psiElement: PsiElement, flag: Int) -> Boolean =
      { _, _ -> false }
  ): ExtensionPhase =
    extensionProvider(
      IconProvider.EXTENSION_POINT_NAME,
      object : IconProvider(), DumbAware {
        override fun getIcon(p0: PsiElement, p1: Int): Icon? =
          if (matchOn(p0, p1)) icon else null
      }
    )
}