package no.nav.aap.behandlingsflyt.behandling

import no.nav.aap.behandlingsflyt.SYSTEMBRUKER
import no.nav.aap.behandlingsflyt.behandling.søknad.TrukketSøknadService
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.Kravreferanse
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.RelevantKrav
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.Søknadsdato
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.SøknadsdatoÅrsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.stønadsperiode.RelevantKravType
import no.nav.aap.behandlingsflyt.help.opprettInMemorySakOgBehandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryKravRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryStønadsperiodeRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryTrukketSøknadRepository
import no.nav.aap.behandlingsflyt.test.januar
import no.nav.aap.komponenter.verdityper.Bruker
import no.nav.aap.verdityper.dokument.JournalpostId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

class BackfillKravServiceTest {

    private lateinit var service: BackfillKravService

    @BeforeEach
    fun setup() {
        InMemoryStønadsperiodeRepository.reset()
        service = BackfillKravService(
            kravRepository = InMemoryKravRepository,
            stønadsperiodeRepository = InMemoryStønadsperiodeRepository,
            trukketSøknadService = TrukketSøknadService(InMemoryTrukketSøknadRepository),
        )
    }

    @Test
    fun `behandling uten krav gir NullKrav og ingen stønadsperiode`() {
        val (sak, behandling) = opprettInMemorySakOgBehandling(søknadsdato = 10 januar 2024)

        val resultat = service.backfillBehandling(behandling)

        assertThat(resultat).isEqualTo(BackfillBehandlingResultat.NullKrav)
        assertThat(InMemoryStønadsperiodeRepository.hentHvisEksisterer(behandling.id)).isNull()
    }

    @Test
    fun `krav uten stønadsperiodevurdering får automatisk vurdering med startDato lik muligRettFra`() {
        val søknadsdato = 10 januar 2024
        val (sak, behandling) = opprettInMemorySakOgBehandling(søknadsdato = søknadsdato)
        val krav = lagRelevantKrav(behandling.id, muligRettFra = søknadsdato)
        InMemoryKravRepository.lagre(behandling.id, setOf(krav))

        val resultat = service.backfillBehandling(behandling)

        assertThat(resultat).isEqualTo(BackfillBehandlingResultat.Backfilled)
        val vurderinger = InMemoryStønadsperiodeRepository.hentHvisEksisterer(behandling.id)!!.vurderinger
        assertThat(vurderinger).hasSize(1)
        val vurdering = vurderinger.single()
        assertThat(vurdering.referanse).isEqualTo(krav.referanse)
        assertThat(vurdering.startDato).isEqualTo(søknadsdato)
        assertThat(vurdering.vurdertAv).isEqualTo(SYSTEMBRUKER)
        assertThat(vurdering.relevantKravType).isEqualTo(RelevantKravType.NY_STØNADSPERIODE)
    }

    @Test
    fun `backfill av stønadsperiode er idempotent`() {
        val søknadsdato = 10 januar 2024
        val (sak, behandling) = opprettInMemorySakOgBehandling(søknadsdato = søknadsdato)
        val krav = lagRelevantKrav(behandling.id, muligRettFra = søknadsdato)
        InMemoryKravRepository.lagre(behandling.id, setOf(krav))

        service.backfillBehandling(behandling)
        val vurderingerFørst = InMemoryStønadsperiodeRepository.hentHvisEksisterer(behandling.id)!!.vurderinger

        service.backfillBehandling(behandling)
        val vurderingerAndre = InMemoryStønadsperiodeRepository.hentHvisEksisterer(behandling.id)!!.vurderinger

        assertThat(vurderingerAndre).isEqualTo(vurderingerFørst)
    }

    private fun lagRelevantKrav(
        behandlingId: BehandlingId,
        muligRettFra: LocalDate,
        referanse: Kravreferanse = Kravreferanse.ny(),
    ) = RelevantKrav(
        referanse = referanse,
        journalpostId = JournalpostId("JP-${UUID.randomUUID()}"),
        vurdertAv = Bruker("Z999999"),
        begrunnelse = "Testkrav",
        vurdertIBehandling = behandlingId,
        opprettet = Instant.now(),
        søknadsdato = Søknadsdato(muligRettFra, SøknadsdatoÅrsak.SøknadMottatt, "Testbegrunnelse"),
        overstyrMuligRettFra = null,
        muligRettFra = muligRettFra,
    )
}
