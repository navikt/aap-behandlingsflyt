package no.nav.aap.behandlingsflyt.arena

import io.mockk.every
import io.mockk.mockk
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.ArenaMigrering
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.behandlingsflyt.test.FakeArenaOppslagGateway
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryArenaMigreringsdataRepository
import no.nav.aap.komponenter.json.DefaultJsonMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class ArenaMigreringServiceTest {

    private val sakId = SakId(1)
    private val behandlingId = BehandlingId(42)

    private val service = ArenaMigreringService(
        arenaMigreringRepository = mockk {
            every { hentForSakHvisEksisterer(sakId) } returns ArenaMigrering(
                sakId = sakId,
                saksnummerArena = "2018-123456",
                ident = "12345678910",
                migrertTidspunkt = LocalDateTime.now(),
            )
        },
        arenaMigreringsdataRepository = InMemoryArenaMigreringsdataRepository,
        arenaOppslagGateway = FakeArenaOppslagGateway(),
    )

    @AfterEach
    fun reset() = InMemoryArenaMigreringsdataRepository.reset()

    @Test
    fun `hentSykdomsvurdering returnerer svaret fra Arena`() {
        val respons = service.hentSykdomsvurdering(sakId)

        assertThat(respons).isEqualTo(FakeArenaOppslagGateway().hentSykdomsvurdering("2018-123456"))
        assertThat(
            InMemoryArenaMigreringsdataRepository.hentAktivHvisEksisterer(behandlingId, StegType.AVKLAR_SYKDOM)
        ).isNull()
        assertThat(
            InMemoryArenaMigreringsdataRepository.hentAktivHvisEksisterer(behandlingId, StegType.VURDER_BISTANDSBEHOV)
        ).isNull()
    }

    @Test
    fun `hentKravDataForSak returnerer svaret fra Arena`() {
        val respons = service.hentKravDataForSak(sakId)
        assertThat(respons).isEqualTo(FakeArenaOppslagGateway().hentKravDataForSak("2018-123456"))
    }


    @Test
    fun `lagreMigreringsdataForSporing lagrer data for behandling og steg`() {
        val respons = service.hentSykdomsvurdering(sakId)

        service.lagreMigreringsdataForSporing(behandlingId, StegType.AVKLAR_SYKDOM, respons)

        val lagret = InMemoryArenaMigreringsdataRepository
            .hentAktivHvisEksisterer(behandlingId, StegType.AVKLAR_SYKDOM)

        assertThat(lagret).isNotNull
        assertThat(lagret!!.data).isEqualTo(DefaultJsonMapper.toJson(respons))
        assertThat(
            InMemoryArenaMigreringsdataRepository.hentAktivHvisEksisterer(behandlingId, StegType.VURDER_BISTANDSBEHOV)
        ).isNull()
    }
}
