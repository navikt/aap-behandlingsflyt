package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdomsvurderingbrev.Dokumentkilde
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdomsvurderingbrev.SykdomsvurderingForBrev
import no.nav.aap.behandlingsflyt.help.opprettInMemorySakOgBehandling
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemorySykdomsvurderingForBrevRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.inMemoryRepositoryProvider
import no.nav.aap.komponenter.verdityper.Bruker
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

class SykdomsvurderingForBrevEndretTest {
    private val kilde = Dokumentkilde("123", "456", "Legeerklæring", LocalDate.of(2026, 1, 20))
    private val annenKilde = kilde.copy(dokumentInfoId = "789", dokumentnavn = "Vedlegg")
    private val repository = InMemorySykdomsvurderingForBrevRepository
    private val service = VurderingEndretService(inMemoryRepositoryProvider)
    private val definisjon = Definisjon.SKRIV_SYKDOMSVURDERING_BREV
    private val behov = Avklaringsbehov(
        id = 1, definisjon = definisjon, funnetISteg = definisjon.løsesISteg, kreverToTrinn = null
    )

    @Test
    fun `kildevalg metadata og rekkefølge inngår i endringskontrollen`() {
        val (_, behandling) = opprettInMemorySakOgBehandling()
        val tidspunkt = LocalDateTime.of(2026, 2, 1, 12, 0)
        val opprinnelig = SykdomsvurderingForBrev(
            behandling.id, "Begrunnelse", Bruker("Veileder"), tidspunkt, listOf(kilde, annenKilde)
        )
        repository.lagre(behandling.id, opprinnelig)

        val endringer = listOf(
            emptyList(),
            listOf(kilde),
            listOf(kilde, annenKilde, kilde.copy(journalpostId = "321")),
            listOf(annenKilde, kilde),
            listOf(kilde.copy(dokumentnavn = "Oppdatert tittel"), annenKilde),
            listOf(kilde.copy(journalføringsdato = kilde.journalføringsdato.plusDays(1)), annenKilde),
            listOf(kilde.copy(journalpostId = "321"), annenKilde),
            listOf(kilde.copy(dokumentInfoId = "654"), annenKilde)
        )
        endringer.forEachIndexed { index, kilder ->
            repository.lagre(
                behandling.id,
                opprinnelig.copy(kilder = kilder, vurdertTidspunkt = tidspunkt.plusMinutes(index + 1L))
            )
            assertThat(service.endretSidenTidspunkt(behandling.id, behov, tidspunkt))
                .describedAs("Endring i kilder: $kilder")
                .isTrue()
        }
    }

    @Test
    fun `nytt vurderingstidspunkt og ny vurderer alene er ikke en funksjonell endring`() {
        val (_, behandling) = opprettInMemorySakOgBehandling()
        val tidspunkt = LocalDateTime.of(2026, 2, 1, 12, 0)
        val opprinnelig = SykdomsvurderingForBrev(
            behandling.id, "Begrunnelse", Bruker("Veileder"), tidspunkt, listOf(kilde)
        )
        repository.lagre(behandling.id, opprinnelig)
        repository.lagre(
            behandling.id,
            opprinnelig.copy(vurdertAv = Bruker("Annen"), vurdertTidspunkt = tidspunkt.plusMinutes(1))
        )

        assertThat(service.endretSidenTidspunkt(behandling.id, behov, tidspunkt)).isFalse()
    }

    @Test
    fun `tilbakestilling oppdages uten å miste historiske kilder`() {
        val (_, behandling) = opprettInMemorySakOgBehandling()
        val tidspunkt = LocalDateTime.of(2026, 2, 1, 12, 0)
        val opprinnelig = SykdomsvurderingForBrev(
            behandling.id, "Begrunnelse", Bruker("Veileder"), tidspunkt, listOf(kilde)
        )
        repository.lagre(behandling.id, opprinnelig)
        repository.deaktiverEksisterende(behandling.id)

        assertThat(service.endretSidenTidspunkt(behandling.id, behov, tidspunkt)).isTrue()
        assertThat(repository.hentSykdomsvurderingForBrevPåTidspunkt(behandling.id, tidspunkt)?.kilder)
            .containsExactly(kilde)
    }
}
