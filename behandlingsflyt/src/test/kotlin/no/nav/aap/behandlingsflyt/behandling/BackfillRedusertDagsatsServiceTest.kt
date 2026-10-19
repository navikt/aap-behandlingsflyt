package no.nav.aap.behandlingsflyt.behandling

import io.mockk.every
import io.mockk.mockk
import no.nav.aap.behandlingsflyt.behandling.rettighetsperiode.VurderRettighetsperiodeRepository
import no.nav.aap.behandlingsflyt.behandling.søknad.TrukketSøknadService
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokument
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.KravVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.RelevantKrav
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.gjeldendeVurderinger
import no.nav.aap.behandlingsflyt.help.opprettInMemorySakOgBehandling
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryKravRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryMottattDokumentRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryStønadsperiodeRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryTilkjentYtelseRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryTrukketSøknadRepository
import no.nav.aap.behandlingsflyt.test.januar
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.verdityper.dokument.Kanal
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.*

// Beregningfokuserte tester for backfill redusertDagsats av BackfillRedusertDagsatsServiceTest
// er det unleash/runner integrasjonstest og repo/persist test som er viktigst for backfill?
class BackfillRedusertDagsatsServiceTest {

    private lateinit var rettighetsperiodeRepository: VurderRettighetsperiodeRepository
    private lateinit var service: BackfillRedusertDagsatsService


    @BeforeEach
    fun setup() {
        InMemoryStønadsperiodeRepository.reset()
        rettighetsperiodeRepository = mockk {
            every { hentVurdering(any()) } returns null
        }
        service = BackfillRedusertDagsatsService(
            kravRepository = InMemoryKravRepository,
            stønadsperiodeRepository = InMemoryStønadsperiodeRepository,
            mottattDokumentRepository = InMemoryMottattDokumentRepository,
            rettighetsperiodeRepository = rettighetsperiodeRepository,
            trukketSøknadService = TrukketSøknadService(InMemoryTrukketSøknadRepository),
            tilkjentYtelseRepository = InMemoryTilkjentYtelseRepository,
        )
    }

    @Test
    fun `er beregning korrekt, unødvendig test?`() {
        val søknadsdato = 10 januar 2024
        val (sak, behandling) = opprettSakOgBehandlingMedSøknad(søknadsdato)

        service.backfillBehandling(sak, listOf(behandling), behandling, erNyesteBehandling = true)

        val krav = InMemoryKravRepository.hent(behandling.id)
        val relevantKrav = assertHarNøyaktigEttRelevantKrav(krav.vurderinger)
        assertThat(relevantKrav.muligRettFra).isEqualTo(søknadsdato)
        assertThat(relevantKrav.søknadsdato.dato).isEqualTo(søknadsdato)
        assertThat(relevantKrav.overstyrMuligRettFra).isNull()
    }

    private fun opprettSakOgBehandlingMedSøknad(
        søknadsdato: LocalDate,
        rettighetsperiodeFom: LocalDate = søknadsdato,
    ): Pair<Sak, Behandling> {
        val (sak, behandling) = opprettInMemorySakOgBehandling(søknadsdato = søknadsdato)
        leggTilSøknad(behandling, søknadsdato)

        val sakMedRettighetsperiode = lagSakMedRettighetsperiode(sak, rettighetsperiodeFom)
        return sakMedRettighetsperiode to behandling
    }

    private fun leggTilSøknad(behandling: Behandling, dato: LocalDate) {
        InMemoryMottattDokumentRepository.lagre(
            MottattDokument(
                referanse = InnsendingReferanse(InnsendingReferanse.Type.JOURNALPOST, UUID.randomUUID().toString()),
                sakId = behandling.sakId,
                behandlingId = behandling.id,
                mottattTidspunkt = dato.atStartOfDay(),
                type = InnsendingType.SØKNAD,
                kanal = Kanal.DIGITAL,
                strukturertDokument = null,
            )
        )
    }

    private fun lagSakMedRettighetsperiode(sak: Sak, fom: LocalDate): Sak {
        return Sak(
            id = sak.id,
            saksnummer = sak.saksnummer,
            person = sak.person,
            rettighetsperiode = Periode(fom, fom.plusYears(1)),
            opprettetTidspunkt = sak.opprettetTidspunkt,
        )
    }

    private fun assertHarNøyaktigEttRelevantKrav(vurderinger: Set<KravVurdering>): RelevantKrav {
        val relevanteKrav = vurderinger.gjeldendeVurderinger().filterIsInstance<RelevantKrav>()
        assertThat(relevanteKrav).hasSize(1)
        return relevanteKrav.single()
    }

}
