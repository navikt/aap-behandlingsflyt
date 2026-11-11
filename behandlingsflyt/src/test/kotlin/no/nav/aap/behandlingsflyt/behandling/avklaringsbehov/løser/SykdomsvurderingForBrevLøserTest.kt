package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.SykdomsvurderingForBrevLøsning
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdomsvurderingbrev.Dokumentkilde
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdomsvurderingbrev.SykdomsvurderingForBrev
import no.nav.aap.behandlingsflyt.help.avklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.help.opprettInMemorySakOgBehandling
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemorySykdomsvurderingForBrevRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.inMemoryRepositoryProvider
import no.nav.aap.komponenter.httpklient.exception.UgyldigForespørselException
import no.nav.aap.komponenter.verdityper.Bruker
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate

class SykdomsvurderingForBrevLøserTest {
    private val repository = InMemorySykdomsvurderingForBrevRepository
    private val løser = SykdomsvurderingForBrevLøser(inMemoryRepositoryProvider)
    private val kilde = Dokumentkilde("123", "456", "Legeerklæring", LocalDate.of(2026, 1, 20))

    @Test
    fun `lagrer begrunnelse og ordnede kilder separat`() {
        val (_, behandling) = opprettInMemorySakOgBehandling()
        val kilder = listOf(kilde, kilde.copy(dokumentInfoId = "789", dokumentnavn = "Vedlegg"))
        val kontekst = avklaringsbehovKontekst { this.behandling = behandling }

        løser.løs(kontekst, SykdomsvurderingForBrevLøsning(vurdering = "Min begrunnelse", kilder = kilder))

        val lagret = requireNotNull(repository.hent(behandling.id))
        assertThat(lagret.vurdering).isEqualTo("Min begrunnelse")
        assertThat(lagret.kilder).containsExactlyElementsOf(kilder)
        assertThat(lagret.vurdertAv).isEqualTo(kontekst.bruker)
    }

    @Test
    fun `manglende kildefelt bevarer tidligere valg og tom liste fjerner alle`() {
        val (_, behandling) = opprettInMemorySakOgBehandling()
        val kontekst = avklaringsbehovKontekst { this.behandling = behandling }
        løser.løs(kontekst, SykdomsvurderingForBrevLøsning(vurdering = "Første", kilder = listOf(kilde)))

        løser.løs(kontekst, SykdomsvurderingForBrevLøsning(vurdering = "Oppdatert"))
        assertThat(repository.hent(behandling.id)?.kilder).containsExactly(kilde)

        løser.løs(kontekst, SykdomsvurderingForBrevLøsning(vurdering = "Oppdatert", kilder = emptyList()))
        assertThat(repository.hent(behandling.id)?.kilder).isEmpty()
    }

    @Test
    fun `oppgitt liste erstatter tidligere valg`() {
        val (_, behandling) = opprettInMemorySakOgBehandling()
        val kontekst = avklaringsbehovKontekst { this.behandling = behandling }
        løser.løs(kontekst, SykdomsvurderingForBrevLøsning(vurdering = "Første", kilder = listOf(kilde)))
        val nyKilde = kilde.copy(dokumentInfoId = "789", dokumentnavn = "Annen dokumentasjon")

        løser.løs(kontekst, SykdomsvurderingForBrevLøsning(vurdering = "Oppdatert", kilder = listOf(nyKilde)))

        assertThat(repository.hent(behandling.id)?.kilder).containsExactly(nyKilde)
    }

    @Test
    fun `kilder er frivillige også ved første lagring`() {
        val (_, behandling) = opprettInMemorySakOgBehandling()

        løser.løs(
            avklaringsbehovKontekst { this.behandling = behandling },
            SykdomsvurderingForBrevLøsning(vurdering = "Begrunnelse uten kilder")
        )

        assertThat(repository.hent(behandling.id)?.kilder).isEmpty()
    }

    @Test
    fun `avviser dupliserte dokumentreferanser uten å erstatte vurderingen`() {
        val (_, behandling) = opprettInMemorySakOgBehandling()
        val eksisterende = SykdomsvurderingForBrev(
            behandling.id, "Eksisterende", Bruker("Veileder"), kilder = listOf(kilde)
        )
        repository.lagre(behandling.id, eksisterende)

        assertThrows<UgyldigForespørselException> {
            løser.løs(
                avklaringsbehovKontekst { this.behandling = behandling },
                SykdomsvurderingForBrevLøsning(
                    vurdering = "Ny",
                    kilder = listOf(kilde, kilde.copy(dokumentnavn = "Et annet navn"))
                )
            )
        }

        assertThat(repository.hent(behandling.id)).isEqualTo(eksisterende)
    }

    @Test
    fun `avviser blanke dokumentreferanser og navn`() {
        val (_, behandling) = opprettInMemorySakOgBehandling()
        val ugyldigeKilder = listOf(
            kilde.copy(journalpostId = " "),
            kilde.copy(dokumentInfoId = ""),
            kilde.copy(dokumentnavn = "\n")
        )

        ugyldigeKilder.forEach { ugyldig ->
            assertThrows<UgyldigForespørselException> {
                løser.løs(
                    avklaringsbehovKontekst { this.behandling = behandling },
                    SykdomsvurderingForBrevLøsning(vurdering = "Ny", kilder = listOf(ugyldig))
                )
            }
        }
        assertThat(repository.hent(behandling.id)).isNull()
    }
}
