package no.nav.aap.behandlingsflyt.behandling.avslag11_27

import no.nav.aap.behandlingsflyt.behandling.samordning.Ytelse
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.KravGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.Kravreferanse
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.RelevantKrav
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.Søknadsdato
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.SøknadsdatoÅrsak
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.test.april
import no.nav.aap.behandlingsflyt.test.januar
import no.nav.aap.behandlingsflyt.test.mars
import no.nav.aap.komponenter.verdityper.Bruker
import no.nav.aap.verdityper.dokument.JournalpostId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate
import java.util.*

class Avslag11_27GrunnlagTest {

    private val ref1 = Kravreferanse(UUID.randomUUID())
    private val ref2 = Kravreferanse(UUID.randomUUID())
    private val behandlingId = BehandlingId(1L)

    private fun vurdering(
        referanse: Kravreferanse = ref1,
        skalAvslås: Boolean = true,
        vurdertTidspunkt: Instant = Instant.now(),
    ) = Avslag11_27Vurdering(
        referanse = referanse,
        begrunnelse = "begrunnelse",
        harAnnenFullYtelse = skalAvslås,
        brukersYtelse = if (skalAvslås) Ytelse.SYKEPENGER else null,
        harSykepengegrunnlagOver2G = null,
        skalAvslås1127 = skalAvslås,
        vurdertIBehandling = behandlingId,
        opprettet = vurdertTidspunkt,
        vurdertAv = Bruker("testBruker"),
    )

    private fun relevantKrav(
        referanse: Kravreferanse = ref1,
        muligRettFra: LocalDate = 1 januar 2026,
    ) = RelevantKrav(
        referanse = referanse,
        journalpostId = JournalpostId("jp-${referanse.verdi}"),
        vurdertAv = Bruker("testBruker"),
        begrunnelse = "begrunnelse nyttKrav",
        vurdertIBehandling = behandlingId,
        opprettet = Instant.now(),
        søknadsdato = Søknadsdato(muligRettFra, SøknadsdatoÅrsak.SøknadMottatt),
        overstyrMuligRettFra = null,
        muligRettFra = muligRettFra,
    )

    // ── nyesteVurderingPerKrav ────────────────────────────────────────────────

    @Test
    fun `nyesteVurderingPerKrav - én vurdering per referanse returneres`() {
        val grunnlag = Avslag11_27Grunnlag(listOf(vurdering(ref1)))
        assertThat(grunnlag.gjeldendeVurderinger()).hasSize(1)
    }

    @Test
    fun `nyesteVurderingPerKrav - kun nyeste beholdes ved flere vurderinger for samme referanse`() {
        val gammel = vurdering(ref1, skalAvslås = false, vurdertTidspunkt = Instant.now().minusSeconds(100))
        val ny = vurdering(ref1, skalAvslås = true, vurdertTidspunkt = Instant.now())
        val grunnlag = Avslag11_27Grunnlag(listOf(gammel, ny))

        val resultat = grunnlag.gjeldendeVurderinger()
        assertThat(resultat).hasSize(1)
        assertThat(resultat.first().skalAvslås1127).isTrue()
    }

    @Test
    fun `nyesteVurderingPerKrav - to ulike referanser gir to vurderinger`() {
        val grunnlag = Avslag11_27Grunnlag(listOf(vurdering(ref1), vurdering(ref2)))
        assertThat(grunnlag.gjeldendeVurderinger()).hasSize(2)
    }

    @Test
    fun `nyesteVurderingPerKrav - tom liste gir tom liste`() {
        assertThat(Avslag11_27Grunnlag(emptyList()).gjeldendeVurderinger()).isEmpty()
    }

    // ── tilTidslinje ─────────────────────────────────────────────────────────

    @Test
    fun `tilTidslinje - krav uten vurdering inkluderes ikke`() {
        val kravGrunnlag = KravGrunnlag(vurderinger = setOf(relevantKrav(ref1)))
        val grunnlag = Avslag11_27Grunnlag(emptyList())

        assertThat(grunnlag.tilTidslinje(kravGrunnlag).segmenter()).isEmpty()
    }

    @Test
    fun `tilTidslinje - ett krav med vurdering gir ett segment`() {
        val kravGrunnlag = KravGrunnlag(vurderinger = setOf(relevantKrav(ref1, 1 januar 2026)))
        val grunnlag = Avslag11_27Grunnlag(listOf(vurdering(ref1)))

        val segmenter = grunnlag.tilTidslinje(kravGrunnlag).segmenter()
        assertThat(segmenter).hasSize(1)
        assertThat(segmenter.first().periode.fom).isEqualTo(1 januar 2026)
        assertThat(segmenter.first().verdi.referanse).isEqualTo(ref1)
    }

    @Test
    fun `tilTidslinje - to krav med vurderinger gir to segmenter med riktige perioder`() {
        val kravGrunnlag = KravGrunnlag(
            vurderinger = setOf(
                relevantKrav(ref1, 1 januar 2026),
                relevantKrav(ref2, 1 april 2026),
            )
        )
        val grunnlag = Avslag11_27Grunnlag(listOf(vurdering(ref1), vurdering(ref2)))

        val segmenter = grunnlag.tilTidslinje(kravGrunnlag).segmenter()
            .sortedBy { it.periode.fom }

        assertThat(segmenter).hasSize(2)
        // Første krav løper til dagen før neste kravs muligRettFra
        assertThat(segmenter[0].periode.fom).isEqualTo(1 januar 2026)
        assertThat(segmenter[0].periode.tom).isEqualTo(31 mars 2026)
        assertThat(segmenter[1].periode.fom).isEqualTo(1 april 2026)
    }

    @Test
    fun `tilTidslinje - kun nyeste vurdering per krav brukes`() {
        val gammel = vurdering(ref1, skalAvslås = false, vurdertTidspunkt = Instant.now().minusSeconds(100))
        val ny = vurdering(ref1, skalAvslås = true, vurdertTidspunkt = Instant.now())
        val kravGrunnlag = KravGrunnlag(vurderinger = setOf(relevantKrav(ref1)))
        val grunnlag = Avslag11_27Grunnlag(listOf(gammel, ny))

        val segmenter = grunnlag.tilTidslinje(kravGrunnlag).segmenter()
        assertThat(segmenter).hasSize(1)
        assertThat(segmenter.first().verdi.skalAvslås1127).isTrue()
    }

    @Test
    fun `tilTidslinje - vurdering uten matchende krav inkluderes ikke`() {
        val kravGrunnlag = KravGrunnlag(vurderinger = setOf(relevantKrav(ref1)))
        val grunnlag = Avslag11_27Grunnlag(listOf(vurdering(ref2)))

        assertThat(grunnlag.tilTidslinje(kravGrunnlag).segmenter()).isEmpty()
    }
}