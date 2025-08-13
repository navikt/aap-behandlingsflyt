package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.flyt.testutil.FakeBrevbestillingGateway
import no.nav.aap.behandlingsflyt.integrasjon.brev.BrevGateway
import no.nav.aap.behandlingsflyt.repository.postgresRepositoryRegistry
import no.nav.aap.behandlingsflyt.test.FakeUnleash
import no.nav.aap.behandlingsflyt.test.MockConnection
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.gateway.GatewayRegistry
import no.nav.aap.lookup.repository.RepositoryProvider
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class AvklaringsbehovsLøserTest {

    @BeforeEach
    fun setUp() {
        GatewayRegistry
            .register<FakeUnleash>()
            .register<FakeBrevbestillingGateway>()
    }

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
                con.call(repositoryProvider, GatewayProvider).forBehov()
            }

        }.toSet()

        Assertions.assertThat(løsningSubtypes).hasSize(utledSubtypes.size)

    }
}