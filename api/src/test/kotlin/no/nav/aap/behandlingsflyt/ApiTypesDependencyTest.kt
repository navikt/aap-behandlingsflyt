package no.nav.aap.behandlingsflyt

import com.tngtech.archunit.base.DescribedPredicate
import com.tngtech.archunit.core.domain.JavaClass
import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.nio.file.Paths

class ApiTypesDependencyTest {

    private val apiClasses by lazy {
        ClassFileImporter().importPath(Paths.get("build/classes/kotlin/main"))
    }

    @Test
    fun `Request-, Response- og Dto-typer i api-modulen skal ikke referere til typer fra behandlingsflyt-modulen`() {
        val behandlingsflytClasses = ClassFileImporter()
            .importPath(Paths.get("../behandlingsflyt/build/classes/kotlin/main"))

        val behandlingsflytClassNames = behandlingsflytClasses.map { it.name }.toSet()

        val erBehandlingsflytKlasse = object : DescribedPredicate<JavaClass>("fra behandlingsflyt-modulen") {
            override fun test(clazz: JavaClass) = clazz.name in behandlingsflytClassNames
        }

        noClasses()
            .that().haveSimpleNameEndingWith("Dto")
            .or().haveSimpleNameEndingWith("Request")
            .or().haveSimpleNameEndingWith("Response")
            .should().dependOnClassesThat(erBehandlingsflytKlasse)
            .check(apiClasses)
    }

    @Test
    fun `HTTP-typer eksponert i endepunkter skal hete noe som slutter på Response eller Request`() {
        val apiClassNames = apiClasses.map { it.name }.toSet()

        // getGrunnlag, authorizedGet/Post/Put er inline+reified, så type-parametere
        // kompileres til class-literaler (ldc) i *ApiKt-filene — nøyaktig de typene
        // vi ønsker å navngi riktig.
        val violations = apiClasses
            .filter { it.simpleName.endsWith("ApiKt") }
            .flatMap { apiFile -> apiFile.codeUnits.flatMap { cu -> cu.referencedClassObjects.map { it.rawType } } }
            .filter { clazz: JavaClass -> clazz.name in apiClassNames }
            .distinctBy { clazz: JavaClass -> clazz.name }
            .filter { clazz: JavaClass -> !clazz.simpleName.endsWith("Response") && !clazz.simpleName.endsWith("Request") }

        assertThat(violations.map { clazz: JavaClass -> clazz.simpleName })
            .describedAs("Klasser brukt som HTTP-typer i endepunkter skal slutte på 'Response' eller 'Request'")
            .isEmpty()
    }
}
