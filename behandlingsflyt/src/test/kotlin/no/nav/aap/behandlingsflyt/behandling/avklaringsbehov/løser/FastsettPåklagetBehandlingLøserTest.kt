package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.mockk
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.FastsettPåklagetBehandlingLøsning
import no.nav.aap.behandlingsflyt.behandling.tilbakekrevingsbehandling.TilbakekrevingBehandlingsstatus
import no.nav.aap.behandlingsflyt.behandling.tilbakekrevingsbehandling.TilbakekrevingRepository
import no.nav.aap.behandlingsflyt.behandling.tilbakekrevingsbehandling.Tilbakekrevingsbehandling
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.påklagetbehandling.PåklagetBehandlingVurderingLøsningDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.påklagetbehandling.PåklagetVedtakType
import no.nav.aap.behandlingsflyt.help.avklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovOgÅrsak
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.ÅrsakTilOpprettelse
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryBehandlingRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryPåklagetBehandlingRepository
import no.nav.aap.komponenter.httpklient.exception.UgyldigForespørselException
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Beløp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.net.URI
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import kotlin.random.Random

class FastsettPåklagetBehandlingLøserTest {

    private val tilbakekrevingRepositoryMock = mockk<TilbakekrevingRepository>()

    private val løser = FastsettPåklagetBehandlingLøser(
        påklagetBehandlingRepository = InMemoryPåklagetBehandlingRepository,
        behandlingRepository = InMemoryBehandlingRepository,
        tilbakekrevingRepository = tilbakekrevingRepositoryMock,
    )

    @AfterEach
    fun afterEach() {
        checkUnnecessaryStub(tilbakekrevingRepositoryMock)
    }

    @Test
    fun `KELVIN_BEHANDLING lagrer vurdering med intern BehandlingId`() {
        val påklagetBehandling = opprettBehandling(TypeBehandling.Førstegangsbehandling, Status.AVSLUTTET)
        val klageBehandling = opprettBehandling(TypeBehandling.Klage, Status.OPPRETTET)

        val resultat = løser.løs(
            kontekst = avklaringsbehovKontekst { behandling = klageBehandling },
            løsning = løsning(
                påklagetBehandling = påklagetBehandling.referanse.referanse,
                påklagetVedtakType = PåklagetVedtakType.KELVIN_BEHANDLING
            )
        )

        assertThat(resultat.begrunnelse).isEqualTo("Vurdert påklaget behandling")

        val grunnlag = InMemoryPåklagetBehandlingRepository.hentHvisEksisterer(klageBehandling.id)!!
        assertThat(grunnlag.vurdering.påklagetVedtakType).isEqualTo(PåklagetVedtakType.KELVIN_BEHANDLING)
        assertThat(grunnlag.vurdering.påklagetBehandling).isEqualTo(påklagetBehandling.id)
        assertThat(grunnlag.vurdering.påklagetTilbakekrevingsbehandling).isNull()
    }

    @Test
    fun `TILBAKEKREVING lagrer vurdering med ekstern tilbakekrevings-UUID`() {
        val tilbakekrevingsReferanse = UUID.randomUUID()
        val klageBehandling = opprettBehandling(TypeBehandling.Klage, Status.OPPRETTET)

        every { tilbakekrevingRepositoryMock.hent(tilbakekrevingsReferanse) } returns
            tilbakekrevingsbehandling(tilbakekrevingsReferanse, TilbakekrevingBehandlingsstatus.AVSLUTTET)

        val resultat = løser.løs(
            kontekst = avklaringsbehovKontekst { behandling = klageBehandling },
            løsning = løsning(
                påklagetBehandling = tilbakekrevingsReferanse,
                påklagetVedtakType = PåklagetVedtakType.TILBAKEKREVING
            )
        )

        assertThat(resultat.begrunnelse).isEqualTo("Vurdert påklaget behandling")

        val grunnlag = InMemoryPåklagetBehandlingRepository.hentHvisEksisterer(klageBehandling.id)!!
        assertThat(grunnlag.vurdering.påklagetVedtakType).isEqualTo(PåklagetVedtakType.TILBAKEKREVING)
        assertThat(grunnlag.vurdering.påklagetBehandling).isNull()
        assertThat(grunnlag.vurdering.påklagetTilbakekrevingsbehandling).isEqualTo(tilbakekrevingsReferanse)
    }

    @Test
    fun `TILBAKEKREVING kaster exception når tilbakekrevingsbehandlingen ikke er avsluttet`() {
        val tilbakekrevingsReferanse = UUID.randomUUID()
        val klageBehandling = opprettBehandling(TypeBehandling.Klage, Status.OPPRETTET)

        every { tilbakekrevingRepositoryMock.hent(tilbakekrevingsReferanse) } returns
            tilbakekrevingsbehandling(tilbakekrevingsReferanse, TilbakekrevingBehandlingsstatus.TIL_BEHANDLING)

        assertThrows<UgyldigForespørselException> {
            løser.løs(
                kontekst = avklaringsbehovKontekst { behandling = klageBehandling },
                løsning = løsning(
                    påklagetBehandling = tilbakekrevingsReferanse,
                    påklagetVedtakType = PåklagetVedtakType.TILBAKEKREVING
                )
            )
        }

        assertThat(InMemoryPåklagetBehandlingRepository.hentHvisEksisterer(klageBehandling.id)).isNull()
    }

    @Test
    fun `ARENA_VEDTAK lagrer vurdering uten noen referanse`() {
        val klageBehandling = opprettBehandling(TypeBehandling.Klage, Status.OPPRETTET)

        val resultat = løser.løs(
            kontekst = avklaringsbehovKontekst { behandling = klageBehandling },
            løsning = løsning(
                påklagetBehandling = null,
                påklagetVedtakType = PåklagetVedtakType.ARENA_VEDTAK
            )
        )

        assertThat(resultat.begrunnelse).isEqualTo("Vurdert påklaget behandling")

        val grunnlag = InMemoryPåklagetBehandlingRepository.hentHvisEksisterer(klageBehandling.id)!!
        assertThat(grunnlag.vurdering.påklagetVedtakType).isEqualTo(PåklagetVedtakType.ARENA_VEDTAK)
        assertThat(grunnlag.vurdering.påklagetBehandling).isNull()
        assertThat(grunnlag.vurdering.påklagetTilbakekrevingsbehandling).isNull()
    }

    private fun opprettBehandling(type: TypeBehandling, status: Status): Behandling {
        val behandling = InMemoryBehandlingRepository.opprettBehandling(
            sakId = SakId(Random.nextLong()),
            typeBehandling = type,
            forrigeBehandlingId = null,
            vurderingsbehovOgÅrsak = VurderingsbehovOgÅrsak(
                vurderingsbehov = emptyList(),
                årsak = ÅrsakTilOpprettelse.SØKNAD,
            ),
        )
        InMemoryBehandlingRepository.oppdaterBehandlingStatus(behandling.id, status)
        return InMemoryBehandlingRepository.hent(behandling.id)
    }

    private fun løsning(
        påklagetBehandling: UUID?,
        påklagetVedtakType: PåklagetVedtakType
    ) = FastsettPåklagetBehandlingLøsning(
        påklagetBehandlingVurdering = PåklagetBehandlingVurderingLøsningDto(
            påklagetBehandling = påklagetBehandling,
            påklagetVedtakType = påklagetVedtakType
        )
    )

    private fun tilbakekrevingsbehandling(
        referanse: UUID,
        status: TilbakekrevingBehandlingsstatus
    ) = Tilbakekrevingsbehandling(
        tilbakekrevingBehandlingId = referanse,
        eksternFagsakId = "EKS123",
        hendelseOpprettet = LocalDateTime.now(),
        eksternBehandlingId = UUID.randomUUID().toString(),
        sakOpprettet = LocalDateTime.now(),
        varselSendt = null,
        venteGrunn = null,
        gjenopptas = null,
        behandlingsstatus = status,
        totaltFeilutbetaltBeløp = Beløp(1000),
        saksbehandlingURL = URI.create("https://nav.no/behandling/$referanse"),
        fullstendigPeriode = Periode(LocalDate.now().minusYears(1), LocalDate.now()),
        vedtaksdato = LocalDate.now(),
    )
}
