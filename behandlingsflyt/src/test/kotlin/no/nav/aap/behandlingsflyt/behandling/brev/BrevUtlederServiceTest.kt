package no.nav.aap.behandlingsflyt.behandling.brev

import io.mockk.every
import io.mockk.mockk
import no.nav.aap.behandlingsflyt.behandling.ResultatUtleder
import no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.TilkjentYtelseRepository
import no.nav.aap.behandlingsflyt.behandling.vedtak.VedtakRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.aktivitetsplikt.Aktivitetsplikt11_7Grunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.aktivitetsplikt.Aktivitetsplikt11_7Repository
import no.nav.aap.behandlingsflyt.faktagrunnlag.aktivitetsplikt.Aktivitetsplikt11_7Vurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.aktivitetsplikt.Utfall
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.BeregningsgrunnlagRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.resultat.KlageresultatUtleder
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.BeregningVurderingRepository
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.ÅrsakTilOpprettelse
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.behandlingsflyt.unleash.UnleashGateway
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime

class BrevUtlederServiceTest {
    val behandlingRepository = mockk<BehandlingRepository>()
    val aktivitetspliktRepository = mockk<Aktivitetsplikt11_7Repository>()
    val brevUtlederService = BrevUtlederService(
        resultatUtleder = mockk<ResultatUtleder>(),
        klageresultatUtleder = mockk<KlageresultatUtleder>(),
        behandlingRepository = behandlingRepository,
        vedtakRepository = mockk<VedtakRepository>(),
        beregningsgrunnlagRepository = mockk<BeregningsgrunnlagRepository>(),
        beregningVurderingRepository = mockk<BeregningVurderingRepository>(),
        tilkjentYtelseRepository = mockk<TilkjentYtelseRepository>(),
        underveisRepository = mockk<UnderveisRepository>(),
        aktivitetsplikt11_7Repository = aktivitetspliktRepository,
        unleashGateway = mockk<UnleashGateway>(),
    )

    @Test
    fun `skal feile ved utleding av brevtype dersom det aktivitetsplikt mangler`() {
        every { behandlingRepository.hent(any<BehandlingId>()) } returns aktivitetspliktBehandling
        every { aktivitetspliktRepository.hentHvisEksisterer(aktivitetspliktBehandling.id) } returns null
        assertThrows<IllegalStateException> {
            brevUtlederService.utledBehovForMeldingOmVedtak(aktivitetspliktBehandling.id)
        }

    }


    @Test
    fun `skal utlede brevtype VedtakAktivitetsplikt11_7 når det iverksettes brudd på 11_7`() {
        every { behandlingRepository.hent(any<BehandlingId>()) } returns aktivitetspliktBehandling
        every { aktivitetspliktRepository.hentHvisEksisterer(aktivitetspliktBehandling.id) } returns Aktivitetsplikt11_7Grunnlag(
            vurderinger = listOf(aktivitetspliktBrudd(aktivitetspliktBehandling.id))
        )

        assertThat(brevUtlederService.utledBehovForMeldingOmVedtak(aktivitetspliktBehandling.id)).isEqualTo(
            VedtakAktivitetsplikt11_7
        )
    }

    @Test
    fun `skal utlede brevtype VedtakEndring når et aktivitetsplikt brudd omgjøres`() {
        every { behandlingRepository.hent(any<BehandlingId>()) } returns aktivitetspliktBehandling
        every { aktivitetspliktRepository.hentHvisEksisterer(aktivitetspliktBehandling.id) } returns Aktivitetsplikt11_7Grunnlag(
            vurderinger = listOf(aktivitetspliktBruddOppfylt(aktivitetspliktBehandling.id))
        )

        assertThat(brevUtlederService.utledBehovForMeldingOmVedtak(aktivitetspliktBehandling.id)).isEqualTo(
            VedtakEndring
        )
    }

    @Test
    fun `skal utlede brevtype VedtakEndring når nyeste aktivitetsplikt brudd omgjøres`() {
        every { behandlingRepository.hent(any<BehandlingId>()) } returns aktivitetspliktBehandling

        val gammelBruddDato = LocalDate.now().minusDays(100)
        every { aktivitetspliktRepository.hentHvisEksisterer(aktivitetspliktBehandling.id) } returns Aktivitetsplikt11_7Grunnlag(
            vurderinger = listOf(
                aktivitetspliktBrudd(BehandlingId(100), fra = gammelBruddDato),
                aktivitetspliktBrudd(BehandlingId(101), fra = gammelBruddDato.plusDays(1)),
                aktivitetspliktBrudd(BehandlingId(102)),
                aktivitetspliktBruddOppfylt(aktivitetspliktBehandling.id))
        )

        assertThat(brevUtlederService.utledBehovForMeldingOmVedtak(aktivitetspliktBehandling.id)).isEqualTo(
            VedtakEndring
        )
    }

    @Test
    fun `skal utlede brevtype Aktivitetspliktbrudd når nyeste aktivitetsplikt brudd er stans`() {

        every { behandlingRepository.hent(any<BehandlingId>()) } returns aktivitetspliktBehandling

        val gammelBruddDato = LocalDate.now().minusDays(100)
        every { aktivitetspliktRepository.hentHvisEksisterer(aktivitetspliktBehandling.id) } returns Aktivitetsplikt11_7Grunnlag(
            vurderinger = listOf(
                aktivitetspliktBrudd(BehandlingId(100), fra = gammelBruddDato),
                aktivitetspliktBrudd(BehandlingId(101), fra = gammelBruddDato.plusDays(1)),
                aktivitetspliktBruddOppfylt(BehandlingId(102)),
                aktivitetspliktBrudd(aktivitetspliktBehandling.id))
        )

        assertThat(brevUtlederService.utledBehovForMeldingOmVedtak(aktivitetspliktBehandling.id)).isEqualTo(
            VedtakAktivitetsplikt11_7
        )
    }

    private fun aktivitetspliktBruddOppfylt(id: BehandlingId, fra: LocalDate = LocalDate.now()): Aktivitetsplikt11_7Vurdering {
        return aktivitetspliktBrudd(id, fra).copy(erOppfylt = true, utfall = null)
    }

    private fun aktivitetspliktBrudd(id: BehandlingId, fra: LocalDate = LocalDate.now()): Aktivitetsplikt11_7Vurdering {
        return Aktivitetsplikt11_7Vurdering(
            begrunnelse = "",
            erOppfylt = false,
            utfall = Utfall.STANS,
            vurdertAv = "",
            gjelderFra = fra,
            opprettet = Instant.now(),
            vurdertIBehandling = id,
            skalIgnorereVarselFrist = false
        )
    }

    val aktivitetspliktBehandling = Behandling(
        id = BehandlingId(1L),
        forrigeBehandlingId = null,
        referanse = BehandlingReferanse(),
        sakId = SakId(1L),
        typeBehandling = TypeBehandling.Aktivitetsplikt,
        status = no.nav.aap.behandlingsflyt.kontrakt.behandling.Status.OPPRETTET,
        vurderingsbehov = listOf(),
        stegTilstand = null,
        årsakTilOpprettelse = ÅrsakTilOpprettelse.AKTIVITETSPLIKT,
        opprettetTidspunkt = LocalDateTime.now(),
        versjon = 1
    )

}
