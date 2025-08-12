package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.flyt.testutil.FakeBrevbestillingGateway
import no.nav.aap.behandlingsflyt.integrasjon.createGatewayProvider
import no.nav.aap.behandlingsflyt.repository.postgresRepositoryRegistry
import no.nav.aap.behandlingsflyt.test.FakeUnleash
import no.nav.aap.behandlingsflyt.test.MockConnection
import no.nav.aap.lookup.repository.RepositoryProvider
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test

internal class AvklaringsbehovsLøserTest {

    @Test
    fun `alle subtyper skal ha unik verdi`() {
        val utledSubtypes = AvklaringsbehovsLøser::class.sealedSubclasses
        val repositoryProvider = postgresRepositoryRegistry.provider(MockConnection().toDBConnection())

        val løsningSubtypes = utledSubtypes.map {
            val con = it.constructors
                .find { it.parameters.firstOrNull()?.type?.classifier == RepositoryProvider::class }!!
            if (con.parameters.size == 1) {
                con.call(repositoryProvider).forBehov()
            } else {
                con.call(
                    repositoryProvider,
                    createGatewayProvider {
                        register<FakeUnleash>()
                        register<FakeBrevbestillingGateway>()
                    }
                ).forBehov()
            }

        }.toSet()

        Assertions.assertThat(løsningSubtypes).hasSize(utledSubtypes.size)

    }
}