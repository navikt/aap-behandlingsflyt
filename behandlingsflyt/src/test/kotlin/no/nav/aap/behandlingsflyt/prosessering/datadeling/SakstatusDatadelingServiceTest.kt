package no.nav.aap.behandlingsflyt.prosessering.datadeling

import no.nav.aap.behandlingsflyt.datadeling.SakStatus
import no.nav.aap.behandlingsflyt.help.finnEllerOpprettBehandling
import no.nav.aap.behandlingsflyt.help.sak
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.inMemoryRepositoryProvider
import no.nav.aap.behandlingsflyt.test.testGatewayProvider
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class SakstatusDatadelingServiceTest {
    @Test
    fun `utlede status, åpen førstegangbehandling`() {
        val sak = sak(inMemoryRepositoryProvider)
        val behandling = finnEllerOpprettBehandling(inMemoryRepositoryProvider, testGatewayProvider(), sak.saksnummer)

        val service = SakstatusDatadelingService(inMemoryRepositoryProvider, testGatewayProvider())

        val res = service.utledSakstatus(behandling.referanse)

        assertThat(res.status).isEqualTo(SakStatus.SakOgBehandlingstatus.SOKNAD_UNDER_BEHANDLING)

    }
}