package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.repository.postgresRepositoryRegistry
import no.nav.aap.behandlingsflyt.test.FakeUnleash
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.InitTestDatabase
import no.nav.aap.komponenter.gateway.GatewayRegistry
import no.nav.aap.lookup.repository.RepositoryProvider
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class AvklaringsbehovsLøserTest {

    @BeforeEach
    fun setUp() {
        GatewayRegistry.register<FakeUnleash>()
    }

    @Test
    fun `alle subtyper skal ha unik verdi`() {
        val utledSubtypes = AvklaringsbehovsLøser::class.sealedSubclasses
        InitTestDatabase.freshDatabase().transaction { dbConnection ->
            val repositoryProvider = postgresRepositoryRegistry.provider(dbConnection)

            val løsningSubtypes = utledSubtypes.map {
                it.constructors
                    .find { it.parameters.singleOrNull()?.type?.classifier == RepositoryProvider::class }!!
                    .call(repositoryProvider).forBehov()
            }.toSet()

            Assertions.assertThat(løsningSubtypes).hasSize(utledSubtypes.size)
        }
    }
}