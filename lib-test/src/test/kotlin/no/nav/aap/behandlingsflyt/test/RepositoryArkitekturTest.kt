package no.nav.aap.behandlingsflyt.test

import com.tngtech.archunit.core.domain.JavaClass
import com.tngtech.archunit.core.domain.JavaModifier
import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.lang.ArchCondition
import com.tngtech.archunit.lang.ConditionEvents
import com.tngtech.archunit.lang.SimpleConditionEvent
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes
import no.nav.aap.behandlingsflyt.repository.postgresRepositoryRegistry
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemorySakRepository
import no.nav.aap.komponenter.repository.Repository
import org.junit.jupiter.api.Test

class RepositoryArkitekturTest {
    private val klasser = ClassFileImporter().importUrls(
        listOf(
            SakRepository::class.java.protectionDomain.codeSource.location,
            InMemorySakRepository::class.java.protectionDomain.codeSource.location
        )
    )

    private val repositoryGrensesnitt = classes()
        .that().areInterfaces()
        .and().areAssignableTo(Repository::class.java)
        .and().doNotHaveFullyQualifiedName(no.nav.aap.lookup.repository.Repository::class.java.name)

    @Test
    fun `alle repository-grensesnitt er registrert i Postgres`() {
        repositoryGrensesnitt.should(object : ArchCondition<JavaClass>("være registrert i Postgres") {
            override fun check(item: JavaClass, events: ConditionEvents) {
                try {
                    postgresRepositoryRegistry.provider(MockConnection().toDBConnection())
                        .provide(item.reflect().asSubclass(Repository::class.java).kotlin)
                } catch (exception: IllegalStateException) {
                    events.add(
                        SimpleConditionEvent.violated(
                            item,
                            "${item.name} må være registrert i postgresRepositoryRegistry: $exception"
                        )
                    )
                }
            }
        }).check(klasser)
    }

    @Test
    fun `alle repository-grensesnitt har en in-memory-implementasjon`() {
        repositoryGrensesnitt.should(object : ArchCondition<JavaClass>("ha en in-memory-implementasjon") {
            override fun check(item: JavaClass, events: ConditionEvents) {
                val harInMemoryImplementasjon = klasser.any {
                    it.packageName == InMemorySakRepository::class.java.packageName &&
                        !it.isInterface &&
                        JavaModifier.ABSTRACT !in it.modifiers &&
                        it.isAssignableTo(item.name)
                }
                events.add(
                    SimpleConditionEvent(
                        item,
                        harInMemoryImplementasjon,
                        "${item.name} må ha en konkret implementasjon i ${InMemorySakRepository::class.java.packageName}"
                    )
                )
            }
        }).check(klasser)
    }
}
