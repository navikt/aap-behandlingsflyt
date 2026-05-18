package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.TrekkSøknadLøsning
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokument
import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovMedPeriode
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovOgÅrsak
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.ÅrsakTilOpprettelse
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekst
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryBehandlingRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryMottattDokumentRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryTrukketSøknadRepository
import no.nav.aap.komponenter.verdityper.Bruker
import no.nav.aap.verdityper.dokument.JournalpostId
import no.nav.aap.verdityper.dokument.Kanal
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.time.LocalDateTime
import kotlin.random.Random

class TrekkSøknadLøserTest {
    private val trekkSøknadLøser = TrekkSøknadLøser(
        InMemoryMottattDokumentRepository,
        InMemoryTrukketSøknadRepository,
        InMemoryBehandlingRepository,
    )

    @ParameterizedTest
    @EnumSource(TypeBehandling::class, mode = EnumSource.Mode.EXCLUDE, names = ["Førstegangsbehandling"])
    fun `Kan kun trekke førstegangsbehandling`(type: TypeBehandling) {
        val (sakId, behandlingId) = opprettBehandling(type)

        assertThrows<IllegalArgumentException> {
            trekkSøknadLøser.løs(kontekst(sakId, behandlingId), enLøsning())
        }
    }

    @ParameterizedTest
    @EnumSource(Status::class, mode = EnumSource.Mode.EXCLUDE, names = ["OPPRETTET", "UTREDES"])
    fun `Kan ikke trekke behandling med ugyldig status`(status: Status) {
        val (sakId, behandlingId) = opprettBehandling(TypeBehandling.Førstegangsbehandling)
        InMemoryBehandlingRepository.oppdaterBehandlingStatus(behandlingId, status)

        assertThrows<IllegalArgumentException> {
            trekkSøknadLøser.løs(kontekst(sakId, behandlingId), enLøsning())
        }
    }

    @ParameterizedTest
    @EnumSource(Status::class, mode = EnumSource.Mode.INCLUDE, names = ["OPPRETTET", "UTREDES"])
    fun `Kan trekke behandling med gyldig status`(status: Status) {
        val (sakId, behandlingId) = opprettBehandling(TypeBehandling.Førstegangsbehandling)
        InMemoryBehandlingRepository.oppdaterBehandlingStatus(behandlingId, status)
        InMemoryMottattDokumentRepository.lagre(mottattDokument(InnsendingType.SØKNAD, sakId, behandlingId))

        trekkSøknadLøser.løs(kontekst(sakId, behandlingId), enLøsning())
    }

    @Test
    fun `Lagrer vurdering med riktig begrunnelse og journalpost for søknad`() {
        val (sakId, behandlingId) = opprettBehandling(TypeBehandling.Førstegangsbehandling)
        InMemoryMottattDokumentRepository.lagre(mottattDokument(InnsendingType.SØKNAD, sakId, behandlingId, journalpostId = "JP-001"))

        val resultat = trekkSøknadLøser.løs(
            kontekst(sakId, behandlingId),
            enLøsning(begrunnelse = "Søker ønsker å trekke", skalTrekkes = true),
        )

        val vurderinger = InMemoryTrukketSøknadRepository.hentTrukketSøknadVurderinger(behandlingId)
        assertThat(vurderinger).hasSize(1)
        assertThat(vurderinger.first().begrunnelse).isEqualTo("Søker ønsker å trekke")
        assertThat(vurderinger.first().skalTrekkes).isTrue()
        assertThat(vurderinger.first().journalpostId).isEqualTo(JournalpostId("JP-001"))
        assertThat(resultat.begrunnelse).isEqualTo("Søker ønsker å trekke")
    }

    @Test
    fun `Lagrer vurdering for alle søknader når det finnes flere`() {
        val (sakId, behandlingId) = opprettBehandling(TypeBehandling.Førstegangsbehandling)
        InMemoryMottattDokumentRepository.lagre(mottattDokument(InnsendingType.SØKNAD, sakId, behandlingId, journalpostId = "JP-111"))
        InMemoryMottattDokumentRepository.lagre(mottattDokument(InnsendingType.SØKNAD, sakId, behandlingId, journalpostId = "JP-222"))

        trekkSøknadLøser.løs(kontekst(sakId, behandlingId), enLøsning())

        val vurderinger = InMemoryTrukketSøknadRepository.hentTrukketSøknadVurderinger(behandlingId)
        assertThat(vurderinger).hasSize(2)
        assertThat(vurderinger.map { it.journalpostId })
            .containsExactlyInAnyOrder(JournalpostId("JP-111"), JournalpostId("JP-222"))
    }

    @Test
    fun `Trekker legeerklæring når ingen søknad finnes og behandling er opprettet fra legeerklæring`() {
        val (sakId, behandlingId) = opprettBehandling(
            type = TypeBehandling.Førstegangsbehandling,
            årsak = ÅrsakTilOpprettelse.HELSEOPPLYSNINGER,
            vurderingsbehov = listOf(VurderingsbehovMedPeriode(Vurderingsbehov.MOTTATT_LEGEERKLÆRING)),
        )
        InMemoryMottattDokumentRepository.lagre(mottattDokument(InnsendingType.LEGEERKLÆRING, sakId, behandlingId, journalpostId = "JP-LEGE"))

        trekkSøknadLøser.løs(kontekst(sakId, behandlingId), enLøsning())

        val vurderinger = InMemoryTrukketSøknadRepository.hentTrukketSøknadVurderinger(behandlingId)
        assertThat(vurderinger).hasSize(1)
        assertThat(vurderinger.first().journalpostId).isEqualTo(JournalpostId("JP-LEGE"))
    }

    @Test
    fun `Trekker ikke legeerklæring når årsak til opprettelse ikke er legeerklæringsrelatert`() {
        val (sakId, behandlingId) = opprettBehandling(
            type = TypeBehandling.Førstegangsbehandling,
            årsak = ÅrsakTilOpprettelse.SØKNAD,
            vurderingsbehov = listOf(VurderingsbehovMedPeriode(Vurderingsbehov.MOTTATT_LEGEERKLÆRING)),
        )

        trekkSøknadLøser.løs(kontekst(sakId, behandlingId), enLøsning())

        val vurderinger = InMemoryTrukketSøknadRepository.hentTrukketSøknadVurderinger(behandlingId)
        assertThat(vurderinger).isEmpty()
    }

    @Test
    fun `Trekker ikke legeerklæring når ingen legeerklæring er funnet`() {
        val (sakId, behandlingId) = opprettBehandling(
            type = TypeBehandling.Førstegangsbehandling,
            årsak = ÅrsakTilOpprettelse.HELSEOPPLYSNINGER,
            vurderingsbehov = listOf(VurderingsbehovMedPeriode(Vurderingsbehov.MOTTATT_LEGEERKLÆRING)),
        )

        trekkSøknadLøser.løs(kontekst(sakId, behandlingId), enLøsning())

        val vurderinger = InMemoryTrukketSøknadRepository.hentTrukketSøknadVurderinger(behandlingId)
        assertThat(vurderinger).isEmpty()
    }

    private fun opprettBehandling(
        type: TypeBehandling,
        årsak: ÅrsakTilOpprettelse = ÅrsakTilOpprettelse.SØKNAD,
        vurderingsbehov: List<VurderingsbehovMedPeriode> = listOf(VurderingsbehovMedPeriode(Vurderingsbehov.MOTTATT_SØKNAD)),
    ): Pair<SakId, BehandlingId> {
        val sakId = SakId(Random.nextLong())
        val behandling = InMemoryBehandlingRepository.opprettBehandling(
            sakId = sakId,
            typeBehandling = type,
            forrigeBehandlingId = null,
            vurderingsbehovOgÅrsak = VurderingsbehovOgÅrsak(
                vurderingsbehov = vurderingsbehov,
                årsak = årsak,
            ),
        )
        return sakId to behandling.id
    }

    private fun kontekst(sakId: SakId, behandlingId: BehandlingId): AvklaringsbehovKontekst = AvklaringsbehovKontekst(
        Bruker("Z123456"),
        FlytKontekst(sakId, behandlingId, null, TypeBehandling.Førstegangsbehandling)
    )

    private fun enLøsning(
        begrunnelse: String = "Testbegrunnelse",
        skalTrekkes: Boolean = true,
    ) = TrekkSøknadLøsning(begrunnelse = begrunnelse, skalTrekkes = skalTrekkes)

    private fun mottattDokument(
        type: InnsendingType,
        sakId: SakId,
        behandlingId: BehandlingId,
        journalpostId: String = Random.nextLong().toString(),
    ) = MottattDokument(
        referanse = InnsendingReferanse(JournalpostId(journalpostId)),
        sakId = sakId,
        behandlingId = behandlingId,
        mottattTidspunkt = LocalDateTime.now(),
        type = type,
        kanal = Kanal.DIGITAL,
        strukturertDokument = null,
    )
}
