package arrow.meta.extensions

import arrow.meta.higherkind.KindAwareTypeChecker
import arrow.meta.utils.NoOp3
import arrow.meta.utils.NoOp6
import arrow.meta.utils.NullableOp1
import arrow.meta.utils.cli
import arrow.meta.utils.ide
import arrow.meta.utils.setFinalStatic
import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.analyzer.ModuleInfo
import org.jetbrains.kotlin.backend.common.BackendContext
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.codegen.ClassBuilder
import org.jetbrains.kotlin.codegen.ClassBuilderFactory
import org.jetbrains.kotlin.codegen.DelegatingClassBuilder
import org.jetbrains.kotlin.codegen.ImplementationBodyCodegen
import org.jetbrains.kotlin.codegen.StackValue
import org.jetbrains.kotlin.codegen.extensions.ClassBuilderInterceptorExtension
import org.jetbrains.kotlin.codegen.extensions.ExpressionCodegenExtension
import org.jetbrains.kotlin.com.intellij.mock.MockProject
import org.jetbrains.kotlin.com.intellij.openapi.extensions.Extensions
import org.jetbrains.kotlin.com.intellij.openapi.project.Project
import org.jetbrains.kotlin.com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.com.intellij.testFramework.LightVirtualFile
import org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.container.ComponentProvider
import org.jetbrains.kotlin.container.StorageComponentContainer
import org.jetbrains.kotlin.container.registerSingleton
import org.jetbrains.kotlin.container.useInstance
import org.jetbrains.kotlin.context.ProjectContext
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor
import org.jetbrains.kotlin.descriptors.PackageFragmentProvider
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.DiagnosticSink
import org.jetbrains.kotlin.extensions.CollectAdditionalSourcesExtension
import org.jetbrains.kotlin.extensions.CompilerConfigurationExtension
import org.jetbrains.kotlin.extensions.DeclarationAttributeAltererExtension
import org.jetbrains.kotlin.extensions.PreprocessedVirtualFileFactoryExtension
import org.jetbrains.kotlin.extensions.StorageComponentContainerContributor
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtImportInfo
import org.jetbrains.kotlin.psi.KtModifierListOwner
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.checkers.DeclarationChecker
import org.jetbrains.kotlin.resolve.checkers.DeclarationCheckerContext
import org.jetbrains.kotlin.resolve.diagnostics.DiagnosticSuppressor
import org.jetbrains.kotlin.resolve.extensions.ExtraImportsProviderExtension
import org.jetbrains.kotlin.resolve.extensions.SyntheticResolveExtension
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOrigin
import org.jetbrains.kotlin.resolve.jvm.extensions.AnalysisHandlerExtension
import org.jetbrains.kotlin.resolve.jvm.extensions.PackageFragmentProviderExtension
import org.jetbrains.kotlin.resolve.lazy.LazyClassContext
import org.jetbrains.kotlin.resolve.lazy.declarations.ClassMemberDeclarationProvider
import org.jetbrains.kotlin.resolve.lazy.declarations.PackageMemberDeclarationProvider
import org.jetbrains.kotlin.resolve.scopes.SyntheticScope
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.synthetic.JavaSyntheticPropertiesScope
import org.jetbrains.kotlin.synthetic.SyntheticScopeProviderExtension
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.checker.KotlinTypeChecker
import org.jetbrains.org.objectweb.asm.AnnotationVisitor
import org.jetbrains.org.objectweb.asm.FieldVisitor
import org.jetbrains.org.objectweb.asm.MethodVisitor
import java.util.*

interface MetaComponentRegistrar : ComponentRegistrar {

  fun intercept(): List<Pair<Name, List<ExtensionPhase>>>

  fun meta(vararg phases: ExtensionPhase): List<ExtensionPhase> =
    phases.toList()

  fun updateConfig(updateConfiguration: CompilerContext.(configuration: CompilerConfiguration) -> Unit): ExtensionPhase.Config =
    object : ExtensionPhase.Config {
      override fun CompilerContext.updateConfiguration(configuration: CompilerConfiguration) {
        updateConfiguration(configuration)
      }
    }

  fun additionalSources(
    collectAdditionalSourcesAndUpdateConfiguration: CompilerContext.(
      knownSources: Collection<KtFile>,
      configuration: CompilerConfiguration,
      project: Project
    ) -> Collection<KtFile>
  ): ExtensionPhase.CollectAdditionalSources =
    object : ExtensionPhase.CollectAdditionalSources {
      override fun CompilerContext.collectAdditionalSourcesAndUpdateConfiguration(knownSources: Collection<KtFile>, configuration: CompilerConfiguration, project: Project): Collection<KtFile> =
        collectAdditionalSourcesAndUpdateConfiguration(knownSources, configuration, project)
    }

  fun analysys(
    doAnalysis: CompilerContext.(
      project: Project,
      module: ModuleDescriptor,
      projectContext: ProjectContext,
      files: Collection<KtFile>,
      bindingTrace: BindingTrace,
      componentProvider: ComponentProvider
    ) -> AnalysisResult?,
    analysisCompleted: CompilerContext.(
      project: Project,
      module: ModuleDescriptor,
      bindingTrace: BindingTrace,
      files: Collection<KtFile>
    ) -> AnalysisResult?
  ): ExtensionPhase.AnalysisHandler =
    object : ExtensionPhase.AnalysisHandler {
      override fun CompilerContext.doAnalysis(
        project: Project,
        module: ModuleDescriptor,
        projectContext: ProjectContext,
        files: Collection<KtFile>,
        bindingTrace: BindingTrace,
        componentProvider: ComponentProvider
      ): AnalysisResult? {
        ctx.module = module
        ctx.files = files
        ctx.componentProvider = componentProvider
        return doAnalysis(project, module, projectContext, files, bindingTrace, componentProvider)
      }

      override fun CompilerContext.analysisCompleted(
        project: Project,
        module: ModuleDescriptor,
        bindingTrace: BindingTrace,
        files: Collection<KtFile>
      ): AnalysisResult? =
        analysisCompleted(project, module, bindingTrace, files)
    }

  fun storageComponent(
    registerModuleComponents: CompilerContext.(container: StorageComponentContainer, moduleDescriptor: ModuleDescriptor) -> Unit,
    check: CompilerContext.(declaration: KtDeclaration, descriptor: DeclarationDescriptor, context: DeclarationCheckerContext) -> Unit
  ): ExtensionPhase.StorageComponentContainer =
    object : ExtensionPhase.StorageComponentContainer {
      override fun CompilerContext.check(
        declaration: KtDeclaration,
        descriptor: DeclarationDescriptor,
        context: DeclarationCheckerContext
      ) {
        check(declaration, descriptor, context)
      }

      override fun CompilerContext.registerModuleComponents(container: StorageComponentContainer, moduleDescriptor: ModuleDescriptor) {
        registerModuleComponents(container, moduleDescriptor)
      }
    }

  fun codegen(
    applyFunction: CompilerContext.(receiver: StackValue, resolvedCall: ResolvedCall<*>, c: ExpressionCodegenExtension.Context) -> StackValue?,
    applyProperty: CompilerContext.(receiver: StackValue, resolvedCall: ResolvedCall<*>, c: ExpressionCodegenExtension.Context) -> StackValue?,
    generateClassSyntheticParts: CompilerContext.(codegen: ImplementationBodyCodegen) -> Unit
  ): ExtensionPhase.Codegen =
    object : ExtensionPhase.Codegen {
      override fun CompilerContext.applyFunction(
        receiver: StackValue,
        resolvedCall: ResolvedCall<*>,
        c: ExpressionCodegenExtension.Context
      ): StackValue? =
        applyFunction(receiver, resolvedCall, c)

      override fun CompilerContext.applyProperty(
        receiver: StackValue,
        resolvedCall: ResolvedCall<*>,
        c: ExpressionCodegenExtension.Context
      ): StackValue? =
        applyProperty(receiver, resolvedCall, c)

      override fun CompilerContext.generateClassSyntheticParts(codegen: ImplementationBodyCodegen) =
        generateClassSyntheticParts(codegen)
    }

  fun IrGeneration(generate: (compilerContext: CompilerContext, file: IrFile, backendContext: BackendContext, bindingContext: BindingContext) -> Unit): ExtensionPhase.IRGeneration =
    object : ExtensionPhase.IRGeneration {
      override fun CompilerContext.generate(
        file: IrFile,
        backendContext: BackendContext,
        bindingContext: BindingContext
      ) {
        generate(this, file, backendContext, bindingContext)
      }
    }

  fun declarationAttributeAlterer(
    refineDeclarationModality: CompilerContext.(
      modifierListOwner: KtModifierListOwner,
      declaration: DeclarationDescriptor?,
      containingDeclaration: DeclarationDescriptor?,
      currentModality: Modality,
      bindingContext: BindingContext,
      isImplicitModality: Boolean
    ) -> Modality?
  ): ExtensionPhase.DeclarationAttributeAlterer =
    object : ExtensionPhase.DeclarationAttributeAlterer {
      override fun CompilerContext.refineDeclarationModality(
        modifierListOwner: KtModifierListOwner,
        declaration: DeclarationDescriptor?,
        containingDeclaration: DeclarationDescriptor?,
        currentModality: Modality,
        bindingContext: BindingContext,
        isImplicitModality: Boolean
      ): Modality? =
        refineDeclarationModality(
          modifierListOwner,
          declaration,
          containingDeclaration,
          currentModality,
          bindingContext,
          isImplicitModality
        )
    }

  fun extraImports(extraImports: (ktFile: KtFile) -> Collection<KtImportInfo>): ExtensionPhase.ExtraImports =
    object : ExtensionPhase.ExtraImports {
      override fun CompilerContext.extraImports(ktFile: KtFile): Collection<KtImportInfo> =
        extraImports(ktFile)
    }

  fun packageFragmentProvider(getPackageFragmentProvider: CompilerContext.(project: Project, module: ModuleDescriptor, storageManager: StorageManager, trace: BindingTrace, moduleInfo: ModuleInfo?, lookupTracker: LookupTracker) -> PackageFragmentProvider?): ExtensionPhase.PackageProvider =
    object : ExtensionPhase.PackageProvider {
      override fun CompilerContext.getPackageFragmentProvider(
        project: Project,
        module: ModuleDescriptor,
        storageManager: StorageManager,
        trace: BindingTrace,
        moduleInfo: ModuleInfo?,
        lookupTracker: LookupTracker
      ): PackageFragmentProvider? =
        getPackageFragmentProvider(
          project,
          module,
          storageManager,
          trace,
          moduleInfo,
          lookupTracker
        )
    }

  fun syntheticScopes(getSyntheticScopes: CompilerContext.(moduleDescriptor: ModuleDescriptor, javaSyntheticPropertiesScope: JavaSyntheticPropertiesScope) -> List<SyntheticScope>): ExtensionPhase.SyntheticScopeProvider =
    object : ExtensionPhase.SyntheticScopeProvider {
      override fun CompilerContext.getSyntheticScopes(
        moduleDescriptor: ModuleDescriptor,
        javaSyntheticPropertiesScope: JavaSyntheticPropertiesScope
      ): List<SyntheticScope> =
        getSyntheticScopes(moduleDescriptor, javaSyntheticPropertiesScope)
    }

  fun preprocessedVirtualFileFactory(
    createPreprocessedFile: CompilerContext.(file: VirtualFile?) -> VirtualFile?,
    createPreprocessedLightFile: CompilerContext.(file: LightVirtualFile?) -> LightVirtualFile? = NullableOp1()
  ): ExtensionPhase.PreprocessedVirtualFileFactory =
    object : ExtensionPhase.PreprocessedVirtualFileFactory {
      override fun CompilerContext.isPassThrough(): Boolean = false

      override fun CompilerContext.createPreprocessedFile(file: VirtualFile?): VirtualFile? =
        createPreprocessedFile(file)

      override fun CompilerContext.createPreprocessedLightFile(file: LightVirtualFile?): LightVirtualFile? =
        createPreprocessedLightFile(file)
    }

  fun diagnosticsSuppressor(isSuppressed: CompilerContext.(diagnostic: Diagnostic) -> Boolean): ExtensionPhase.DiagnosticsSuppressor =
    object : ExtensionPhase.DiagnosticsSuppressor {
      override fun CompilerContext.isSuppressed(diagnostic: Diagnostic): Boolean =
        isSuppressed(diagnostic)
    }

  private fun registerKindAwareTypeChecker(): ExtensionPhase.StorageComponentContainer =
    storageComponent(
      registerModuleComponents = { container, moduleDescriptor ->
        val defaultTypeChecker = KotlinTypeChecker.DEFAULT
        if (defaultTypeChecker !is KindAwareTypeChecker) { //nasty hack ahead to circumvent the ability to replace the Kotlin type checker
          val defaultTypeCheckerField = KotlinTypeChecker::class.java.getDeclaredField("DEFAULT")
          setFinalStatic(defaultTypeCheckerField, KindAwareTypeChecker(defaultTypeChecker))
        }
      },
      check = { _, _, _ ->
      }
    )

  fun compilerContextService(): ExtensionPhase.StorageComponentContainer =
    storageComponent(
      registerModuleComponents = { container, moduleDescriptor ->
        container.useInstance(this)
      },
      check = { declaration, descriptor, context ->
      }
    )

  fun Project.registerIdeHooks(): ExtensionPhase =
    ide {
      storageComponent(
        registerModuleComponents = { container, moduleDescriptor ->
          container.registerSingleton(Class.forName("arrow.meta.plugin.idea.MetaIdeAnalyzer"))
          //
        },
        check = { declaration, descriptor, context ->
        }
      )
    } ?: ExtensionPhase.Empty



  fun syntheticResolver(
    addSyntheticSupertypes: CompilerContext.(
      thisDescriptor: ClassDescriptor,
      supertypes: MutableList<KotlinType>
    ) -> Unit = NoOp3,
    /**
     * For a given package fragment it iterates over all the package declaration
     * allowing the user to contribute new synthetic declarations.
     * The result mutable set includes the descriptors as seen from the Kotlin compiler
     * initial analysis and allows us to mutate it to add new descriptor or change the existing ones
     */
    generatePackageSyntheticClasses: CompilerContext.(
      thisDescriptor: PackageFragmentDescriptor,
      name: Name,
      ctx: LazyClassContext,
      declarationProvider: PackageMemberDeclarationProvider,
      result: MutableSet<ClassDescriptor>
    ) -> Unit = NoOp6,
    generateSyntheticClasses: CompilerContext.(
      thisDescriptor: ClassDescriptor,
      name: Name,
      ctx: LazyClassContext,
      declarationProvider: ClassMemberDeclarationProvider,
      result: MutableSet<ClassDescriptor>
    ) -> Unit = NoOp6,
    generateSyntheticMethods: CompilerContext.(
      thisDescriptor: ClassDescriptor,
      name: Name,
      bindingContext: BindingContext,
      fromSupertypes: List<SimpleFunctionDescriptor>,
      result: MutableCollection<SimpleFunctionDescriptor>
    ) -> Unit = NoOp6,
    generateSyntheticProperties: CompilerContext.(
      thisDescriptor: ClassDescriptor,
      name: Name,
      bindingContext: BindingContext,
      fromSupertypes: ArrayList<PropertyDescriptor>,
      result: MutableSet<PropertyDescriptor>
    ) -> Unit = NoOp6,
    getSyntheticCompanionObjectNameIfNeeded: CompilerContext.(
      thisDescriptor: ClassDescriptor
    ) -> Name? = NullableOp1(),
    getSyntheticFunctionNames: CompilerContext.(
      thisDescriptor: ClassDescriptor
    ) -> List<Name>? = NullableOp1(),
    getSyntheticNestedClassNames: CompilerContext.(
      thisDescriptor: ClassDescriptor
    ) -> List<Name>? = NullableOp1()
  ): ExtensionPhase.SyntheticResolver =
    object : ExtensionPhase.SyntheticResolver {
      override fun CompilerContext.addSyntheticSupertypes(
        thisDescriptor: ClassDescriptor,
        supertypes: MutableList<KotlinType>
      ) {
        addSyntheticSupertypes(thisDescriptor, supertypes)
      }

      override fun CompilerContext.generateSyntheticClasses(
        thisDescriptor: ClassDescriptor,
        name: Name,
        ctx: LazyClassContext,
        declarationProvider: ClassMemberDeclarationProvider,
        result: MutableSet<ClassDescriptor>
      ) {
        generateSyntheticClasses(thisDescriptor, name, ctx, declarationProvider, result)
      }

      override fun CompilerContext.generatePackageSyntheticClasses(
        thisDescriptor: PackageFragmentDescriptor,
        name: Name,
        ctx: LazyClassContext,
        declarationProvider: PackageMemberDeclarationProvider,
        result: MutableSet<ClassDescriptor>
      ) {
        generatePackageSyntheticClasses(thisDescriptor, name, ctx, declarationProvider, result)
      }

      override fun CompilerContext.generateSyntheticMethods(
        thisDescriptor: ClassDescriptor,
        name: Name,
        bindingContext: BindingContext,
        fromSupertypes: List<SimpleFunctionDescriptor>,
        result: MutableCollection<SimpleFunctionDescriptor>
      ) {
        generateSyntheticMethods(thisDescriptor, name, bindingContext, fromSupertypes, result)
      }

      override fun CompilerContext.generateSyntheticProperties(
        thisDescriptor: ClassDescriptor,
        name: Name,
        bindingContext: BindingContext,
        fromSupertypes: ArrayList<PropertyDescriptor>,
        result: MutableSet<PropertyDescriptor>
      ) {
        generateSyntheticProperties(thisDescriptor, name, bindingContext, fromSupertypes, result)
      }

      override fun CompilerContext.getSyntheticCompanionObjectNameIfNeeded(thisDescriptor: ClassDescriptor): Name? =
        getSyntheticCompanionObjectNameIfNeeded(thisDescriptor)

      override fun CompilerContext.getSyntheticFunctionNames(thisDescriptor: ClassDescriptor): List<Name> =
        getSyntheticFunctionNames(thisDescriptor) ?: emptyList()

      override fun CompilerContext.getSyntheticNestedClassNames(thisDescriptor: ClassDescriptor): List<Name> =
        getSyntheticNestedClassNames(thisDescriptor) ?: emptyList()
    }

  fun enableIr(): ExtensionPhase =
    cli {
      updateConfig { configuration ->
        configuration.put(JVMConfigurationKeys.IR, true)
      }
    } ?: ExtensionPhase.Empty

  override fun registerProjectComponents(
    project: MockProject,
    configuration: CompilerConfiguration
  ) = registerIdeProjectComponents(project, configuration)

  fun registerIdeProjectComponents(
    project: Project,
    configuration: CompilerConfiguration
  ) {
    println("Project allowed extensions: ${Extensions.getArea(project).extensionPoints.toList().joinToString("\n")}")
    cli {
      println("it's the CLI plugin")
    }
    ide {
      println("it's the IDEA plugin")
    }
    val messageCollector: MessageCollector? =
      cli { configuration.get(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY, MessageCollector.NONE) }

    val ctx = CompilerContext(project, messageCollector)
    registerPostAnalysisContextEnrichment(project, ctx)

    val initialPhases = listOf(Name.identifier("Initial setup") to meta(
      enableIr(),
      compilerContextService(),
      registerKindAwareTypeChecker(),
      project.registerIdeHooks()
    ))
    (initialPhases + intercept()).forEach { (plugin, phases) ->
      println("Registering plugin: $plugin extensions: $phases")
      phases.forEach { currentPhase ->
        fun registerPhase(phase: ExtensionPhase): Unit {
          if (phase is ExtensionPhase.ExtraImports) registerExtraImports(project, phase, ctx)
          if (phase is ExtensionPhase.PreprocessedVirtualFileFactory) registerPreprocessedVirtualFileFactory(project, phase, ctx)
          if (phase is ExtensionPhase.Config) registerCompilerConfiguration(project, phase, ctx)
          if (phase is ExtensionPhase.StorageComponentContainer) registerStorageComponentContainer(project, phase, ctx)
          if (phase is ExtensionPhase.CollectAdditionalSources) registerCollectAdditionalSources(project, phase, ctx)
          if (phase is ExtensionPhase.AnalysisHandler) registerAnalysisHandler(project, phase, ctx)
          if (phase is ExtensionPhase.ClassBuilder) registerClassBuilder(project, phase, ctx)
          if (phase is ExtensionPhase.Codegen) registerCodegen(project, phase, ctx)
          if (phase is ExtensionPhase.DeclarationAttributeAlterer) registerDeclarationAttributeAlterer(project, phase, ctx)
          if (phase is ExtensionPhase.PackageProvider) packageFragmentProvider(project, phase, ctx)
          if (phase is ExtensionPhase.SyntheticResolver) registerSyntheticResolver(project, phase, ctx)
          if (phase is ExtensionPhase.IRGeneration) registerIRGeneration(project, phase, ctx)
          if (phase is ExtensionPhase.SyntheticScopeProvider) registerSyntheticScopeProvider(project, phase, ctx)
          if (phase is ExtensionPhase.DiagnosticsSuppressor) registerDiagnosticSuppressor(project, phase, ctx)
          if (phase is ExtensionPhase.CompositePhase) phase.phases.map(::registerPhase)
        }
        registerPhase(currentPhase)
      }
    }
  }

  fun registerDiagnosticSuppressor(
    project: Project,
    phase: ExtensionPhase.DiagnosticsSuppressor,
    ctx: CompilerContext
  ) {
    Extensions.getArea(project).getExtensionPoint(DiagnosticSuppressor.EP_NAME)
      .registerExtension(object : DiagnosticSuppressor {
        override fun isSuppressed(diagnostic: Diagnostic): Boolean =
          phase.run { ctx.isSuppressed(diagnostic) }
      })
  }

  fun registerExtraImports(project: Project, phase: ExtensionPhase.ExtraImports, ctx: CompilerContext) {
    ExtraImportsProviderExtension.registerExtension(project, object : ExtraImportsProviderExtension {
      override fun getExtraImports(ktFile: KtFile): Collection<KtImportInfo> =
        phase.run { ctx.extraImports(ktFile) }

    })
  }

  fun registerPreprocessedVirtualFileFactory(project: Project, phase: ExtensionPhase.PreprocessedVirtualFileFactory, ctx: CompilerContext) {
    PreprocessedVirtualFileFactoryExtension.registerExtension(project, object : PreprocessedVirtualFileFactoryExtension {
      override fun createPreprocessedFile(file: VirtualFile?): VirtualFile? =
        phase.run { ctx.createPreprocessedFile(file) }

      override fun createPreprocessedLightFile(file: LightVirtualFile?): LightVirtualFile? =
        phase.run { ctx.createPreprocessedLightFile(file) }

      override fun isPassThrough(): Boolean =
        phase.run { ctx.isPassThrough() }
    })
  }

  fun registerSyntheticScopeProvider(project: Project, phase: ExtensionPhase.SyntheticScopeProvider, ctx: CompilerContext) {
    SyntheticScopeProviderExtension.registerExtension(project, object : SyntheticScopeProviderExtension {
      override fun getScopes(moduleDescriptor: ModuleDescriptor, javaSyntheticPropertiesScope: JavaSyntheticPropertiesScope): List<SyntheticScope> =
        phase.run { ctx.getSyntheticScopes(moduleDescriptor, javaSyntheticPropertiesScope) }
    })
  }

  fun registerIRGeneration(
    project: Project,
    phase: ExtensionPhase.IRGeneration,
    compilerContext: CompilerContext
  ) {
    IrGenerationExtension.registerExtension(project, object : IrGenerationExtension {
      override fun generate(
        file: IrFile,
        backendContext: BackendContext,
        bindingContext: BindingContext
      ) {
        phase.run { compilerContext.generate(file, backendContext, bindingContext) }
      }
    })
  }

  fun registerSyntheticResolver(
    project: Project,
    phase: ExtensionPhase.SyntheticResolver,
    compilerContext: CompilerContext
  ) {
    SyntheticResolveExtension.registerExtension(project, object : SyntheticResolveExtension {
      override fun addSyntheticSupertypes(thisDescriptor: ClassDescriptor, supertypes: MutableList<KotlinType>) {
        phase.run { compilerContext.addSyntheticSupertypes(thisDescriptor, supertypes) }
      }

      override fun generateSyntheticClasses(
        thisDescriptor: ClassDescriptor,
        name: Name,
        ctx: LazyClassContext,
        declarationProvider: ClassMemberDeclarationProvider,
        result: MutableSet<ClassDescriptor>
      ) {
        phase.run {
          compilerContext.generateSyntheticClasses(
            thisDescriptor,
            name,
            ctx,
            declarationProvider,
            result
          )
        }
      }

      override fun generateSyntheticClasses(
        thisDescriptor: PackageFragmentDescriptor,
        name: Name,
        ctx: LazyClassContext,
        declarationProvider: PackageMemberDeclarationProvider,
        result: MutableSet<ClassDescriptor>
      ) {
        phase.run {
          compilerContext.generatePackageSyntheticClasses(
            thisDescriptor,
            name,
            ctx,
            declarationProvider,
            result
          )
        }
      }

      override fun generateSyntheticMethods(
        thisDescriptor: ClassDescriptor,
        name: Name,
        bindingContext: BindingContext,
        fromSupertypes: List<SimpleFunctionDescriptor>,
        result: MutableCollection<SimpleFunctionDescriptor>
      ) {
        phase.run {
          compilerContext.generateSyntheticMethods(
            thisDescriptor,
            name,
            bindingContext,
            fromSupertypes,
            result
          )
        }
      }

      override fun generateSyntheticProperties(
        thisDescriptor: ClassDescriptor,
        name: Name,
        bindingContext: BindingContext,
        fromSupertypes: ArrayList<PropertyDescriptor>,
        result: MutableSet<PropertyDescriptor>
      ) {
        phase.run {
          compilerContext.generateSyntheticProperties(
            thisDescriptor,
            name,
            bindingContext,
            fromSupertypes,
            result
          )
        }
      }

      override fun getSyntheticCompanionObjectNameIfNeeded(thisDescriptor: ClassDescriptor): Name? {
        return phase.run { compilerContext.getSyntheticCompanionObjectNameIfNeeded(thisDescriptor) }
      }

      override fun getSyntheticFunctionNames(thisDescriptor: ClassDescriptor): List<Name> {
        return phase.run { compilerContext.getSyntheticFunctionNames(thisDescriptor) }
      }

      override fun getSyntheticNestedClassNames(thisDescriptor: ClassDescriptor): List<Name> {
        return phase.run { compilerContext.getSyntheticNestedClassNames(thisDescriptor) }
      }
    })
  }

  fun packageFragmentProvider(
    project: Project,
    phase: ExtensionPhase.PackageProvider,
    ctx: CompilerContext
  ) {
    PackageFragmentProviderExtension.registerExtension(
      project,
      object : PackageFragmentProviderExtension {
        override fun getPackageFragmentProvider(
          project: Project,
          module: ModuleDescriptor,
          storageManager: StorageManager,
          trace: BindingTrace,
          moduleInfo: ModuleInfo?,
          lookupTracker: LookupTracker
        ): PackageFragmentProvider? {
          return phase.run {
            ctx.getPackageFragmentProvider(
              project,
              module,
              storageManager,
              trace,
              moduleInfo,
              lookupTracker
            )
          }
        }
      })
  }

  private fun registerPostAnalysisContextEnrichment(project: Project, ctx: CompilerContext) {
    cli {
      AnalysisHandlerExtension.registerExtension(project, object : AnalysisHandlerExtension {
        override fun doAnalysis(
          project: Project,
          module: ModuleDescriptor,
          projectContext: ProjectContext,
          files: Collection<KtFile>,
          bindingTrace: BindingTrace,
          componentProvider: ComponentProvider
        ): AnalysisResult? {
          ctx.module = module
          ctx.files = files
          ctx.componentProvider = componentProvider
          return null
        }
      })
    }
    ide {
      PackageFragmentProviderExtension.registerExtension(project, object : PackageFragmentProviderExtension {
        override fun getPackageFragmentProvider(project: Project, module: ModuleDescriptor, storageManager: StorageManager, trace: BindingTrace, moduleInfo: ModuleInfo?, lookupTracker: LookupTracker): PackageFragmentProvider? {
          println("getPackageFragmentProvider")
          return null
        }
      })
      StorageComponentContainerContributor.registerExtension(
        project,
        object : StorageComponentContainerContributor {
          override fun registerModuleComponents(
            container: StorageComponentContainer,
            platform: TargetPlatform,
            moduleDescriptor: ModuleDescriptor
          ) {
            ctx.module = moduleDescriptor
            ctx.componentProvider = container
            super.registerModuleComponents(container, platform, moduleDescriptor)
          }
        }
      )
    }
  }

  fun registerDeclarationAttributeAlterer(
    project: Project,
    phase: ExtensionPhase.DeclarationAttributeAlterer,
    ctx: CompilerContext
  ) {
    DeclarationAttributeAltererExtension.registerExtension(
      project,
      object : DeclarationAttributeAltererExtension {
        override fun refineDeclarationModality(
          modifierListOwner: KtModifierListOwner,
          declaration: DeclarationDescriptor?,
          containingDeclaration: DeclarationDescriptor?,
          currentModality: Modality,
          bindingContext: BindingContext,
          isImplicitModality: Boolean
        ): Modality? {
          return phase.run {
            ctx.refineDeclarationModality(
              modifierListOwner,
              declaration,
              containingDeclaration,
              currentModality,
              bindingContext,
              isImplicitModality
            )
          }
        }
      })
  }

  fun registerCodegen(project: Project, phase: ExtensionPhase.Codegen, ctx: CompilerContext) {
    ExpressionCodegenExtension.registerExtension(project, object : ExpressionCodegenExtension {
      override fun applyFunction(
        receiver: StackValue,
        resolvedCall: ResolvedCall<*>,
        c: ExpressionCodegenExtension.Context
      ): StackValue? {
        return phase.run { ctx.applyFunction(receiver, resolvedCall, c) }
      }

      override fun applyProperty(
        receiver: StackValue,
        resolvedCall: ResolvedCall<*>,
        c: ExpressionCodegenExtension.Context
      ): StackValue? {
        return phase.run { ctx.applyProperty(receiver, resolvedCall, c) }
      }

      override fun generateClassSyntheticParts(codegen: ImplementationBodyCodegen) {
        phase.run { ctx.generateClassSyntheticParts(codegen) }
      }
    })
  }

  class DelegatingContributorChecker(val phase: ExtensionPhase.StorageComponentContainer, val ctx: CompilerContext) : StorageComponentContainerContributor, DeclarationChecker {

    override fun registerModuleComponents(container: StorageComponentContainer, platform: TargetPlatform, moduleDescriptor: ModuleDescriptor) {
      phase.run { ctx.registerModuleComponents(container, moduleDescriptor) }
    }

    override fun check(declaration: KtDeclaration, descriptor: DeclarationDescriptor, context: DeclarationCheckerContext) {
      phase.run { ctx.check(declaration, descriptor, context) }
    }
  }

  fun registerStorageComponentContainer(
    project: Project,
    phase: ExtensionPhase.StorageComponentContainer,
    ctx: CompilerContext
  ) {
    StorageComponentContainerContributor.registerExtension(
      project,
      DelegatingContributorChecker(phase, ctx)
    )
  }

  fun registerCollectAdditionalSources(
    project: Project,
    phase: ExtensionPhase.CollectAdditionalSources,
    ctx: CompilerContext
  ) {
    cli {
      CollectAdditionalSourcesExtension.registerExtension(
        project,
        object : CollectAdditionalSourcesExtension {
          override fun collectAdditionalSourcesAndUpdateConfiguration(
            knownSources: Collection<KtFile>,
            configuration: CompilerConfiguration,
            project: Project
          ): Collection<KtFile> = phase.run {
            ctx.collectAdditionalSourcesAndUpdateConfiguration(knownSources, configuration, project)
          }
        }
      )
    }
  }

  fun registerAnalysisHandler(
    project: Project,
    phase: ExtensionPhase.AnalysisHandler,
    ctx: CompilerContext
  ) {
    cli {
      AnalysisHandlerExtension.registerExtension(project, object : AnalysisHandlerExtension {
        override fun analysisCompleted(
          project: Project,
          module: ModuleDescriptor,
          bindingTrace: BindingTrace,
          files: Collection<KtFile>
        ): AnalysisResult? = phase.run { ctx.analysisCompleted(project, module, bindingTrace, files) }

        override fun doAnalysis(
          project: Project,
          module: ModuleDescriptor,
          projectContext: ProjectContext,
          files: Collection<KtFile>,
          bindingTrace: BindingTrace,
          componentProvider: ComponentProvider
        ): AnalysisResult? {
          return phase.run {
            ctx.doAnalysis(
              project,
              module,
              projectContext,
              files,
              bindingTrace,
              componentProvider
            )
          }
        }
      })
    }
  }

  fun registerClassBuilder(
    project: Project,
    phase: ExtensionPhase.ClassBuilder,
    ctx: CompilerContext
  ) {
    ClassBuilderInterceptorExtension.registerExtension(
      project,
      object : ClassBuilderInterceptorExtension {
        override fun interceptClassBuilderFactory(
          interceptedFactory: ClassBuilderFactory,
          bindingContext: BindingContext,
          diagnostics: DiagnosticSink
        ): ClassBuilderFactory =
          phase.run {
            ctx.interceptClassBuilder(interceptedFactory, bindingContext, diagnostics)
          }
      })
  }

  fun registerCompilerConfiguration(
    project: Project,
    phase: ExtensionPhase.Config,
    ctx: CompilerContext
  ) {
    CompilerConfigurationExtension.registerExtension(
      project,
      object : CompilerConfigurationExtension {
        override fun updateConfiguration(configuration: CompilerConfiguration) {
          phase.run { ctx.updateConfiguration(configuration) }
        }
      })
  }

}

internal class NewMethodClassBuilder(
  private val builder: ClassBuilder,
  val f: (origin: JvmDeclarationOrigin, access: Int, name: String, desc: String, signature: String?, exceptions: Array<out String>?) -> Unit
) : DelegatingClassBuilder() {
  override fun getDelegate(): ClassBuilder = builder

  override fun newMethod(
    origin: JvmDeclarationOrigin,
    access: Int,
    name: String,
    desc: String,
    signature: String?,
    exceptions: Array<out String>?
  ): MethodVisitor {
    //delegate to the parent method visitor for construction
    val original: MethodVisitor = super.newMethod(origin, access, name, desc, signature, exceptions)
    f(origin, access, name, desc, signature, exceptions)
    return original
  }

}

internal class NewFieldClassBuilder(
  private val builder: ClassBuilder,
  val f: (origin: JvmDeclarationOrigin, access: Int, name: String, desc: String, signature: String?, value: Any?) -> Unit
) : DelegatingClassBuilder() {
  override fun getDelegate(): ClassBuilder = builder

  override fun newField(
    origin: JvmDeclarationOrigin,
    access: Int,
    name: String,
    desc: String,
    signature: String?,
    value: Any?
  ): FieldVisitor {
    //delegate to the parent method visitor for construction
    val original: FieldVisitor = super.newField(origin, access, name, desc, signature, value)
    f(origin, access, name, desc, signature, value)
    return original
  }

}

internal class NewAnnotationClassBuilder(
  private val builder: ClassBuilder,
  val f: (desc: String, visible: Boolean) -> Unit
) : DelegatingClassBuilder() {
  override fun getDelegate(): ClassBuilder = builder

  override fun newAnnotation(desc: String, visible: Boolean): AnnotationVisitor {
    val original: AnnotationVisitor = super.newAnnotation(desc, visible)
    f(desc, visible)
    return original
  }

}

internal class DefineClassBuilder(
  private val builder: ClassBuilder,
  private val bindingContext: BindingContext,
  private val diagnostics: DiagnosticSink,
  val f: (bindingContext: BindingContext, diagnostics: DiagnosticSink, classDef: ClassDefinition) -> ClassDefinition
) : DelegatingClassBuilder() {
  override fun getDelegate(): ClassBuilder = builder

  override fun defineClass(
    origin: PsiElement?,
    version: Int,
    access: Int,
    name: String,
    signature: String?,
    superName: String,
    interfaces: Array<out String>
  ) {
    val classDef = f(
      bindingContext, diagnostics,
      ClassDefinition(
        origin,
        version,
        access,
        name,
        signature,
        superName,
        interfaces.toList()
      )
    )
    super.defineClass(
      classDef.origin,
      classDef.version,
      classDef.access,
      classDef.name,
      classDef.signature,
      classDef.superName,
      classDef.interfaces.toTypedArray()
    )
  }
}

data class ClassDefinition(
  val origin: PsiElement?,
  val version: Int,
  val access: Int,
  val name: String,
  val signature: String?,
  val superName: String,
  val interfaces: List<String>
)