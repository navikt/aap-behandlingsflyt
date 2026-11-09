package no.nav.aap.behandlingsflyt.architecture

import com.tngtech.archunit.core.domain.JavaClass
import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.lang.ArchCondition
import com.tngtech.archunit.lang.ConditionEvents
import com.tngtech.archunit.lang.SimpleConditionEvent
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes
import no.nav.aap.komponenter.repository.Repository
import org.junit.jupiter.api.Test
import java.nio.file.Path

class RepositoryArchitectureTest {
    @Test
    fun `repository implementations belong in repository module and have a corresponding test`() {
        val modules = listOf("app", "api", "behandlingsflyt", "repository", "kontrakt")
        val productionPaths = modules.map {
            Path.of("..", it, "build", "classes", "kotlin", "main").toAbsolutePath().normalize()
        }
        val productionClasses = ClassFileImporter().importPaths(productionPaths)
        val repositoryClasses = ClassFileImporter().importPath(Path.of("build/classes/kotlin/main"))
        val testClasses = ClassFileImporter().importPath(Path.of("build/classes/kotlin/test"))
        val testNames = testClasses.map { it.simpleName }.toSet()

        classes()
            .that().areAssignableTo(Repository::class.java)
            .and().areNotInterfaces()
            .should(object : ArchCondition<JavaClass>("belong in repository module and have a corresponding test") {
                override fun check(item: JavaClass, events: ConditionEvents) {
                    events.add(
                        SimpleConditionEvent(
                            item,
                            repositoryClasses.contain(item.name),
                            "${item.name} must belong to the repository module"
                        )
                    )
                    events.add(
                        SimpleConditionEvent(
                            item,
                            "${item.simpleName}Test" in testNames,
                            "${item.name} must have a corresponding ${item.simpleName}Test in the repository module"
                        )
                    )
                }
            })
            .check(productionClasses)
    }
}
