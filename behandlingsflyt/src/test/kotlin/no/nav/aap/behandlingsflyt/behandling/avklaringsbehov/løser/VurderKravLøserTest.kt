package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.VurderKravLøsning
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokument
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.GjenopptakKravLøsningDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.MuligRettFra
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.MuligRettFraÅrsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.NyttKrav
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.NyttKravLøsningDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.Søknadsdato
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.SøknadsdatoÅrsak
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekst
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryKravRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryMottattDokumentRepository
import no.nav.aap.behandlingsflyt.test.januar
import no.nav.aap.komponenter.httpklient.exception.UgyldigForespørselException
import no.nav.aap.komponenter.verdityper.Bruker
import no.nav.aap.verdityper.dokument.JournalpostId
import no.nav.aap.verdityper.dokument.Kanal
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDateTime
import kotlin.random.Random

class VurderKravLøserTest {

    private val løser = VurderKravLøser(InMemoryKravRepository, InMemoryMottattDokumentRepository)

    @Test
    fun `skal feile hvis søknadsdato er ulik dato for mottatt søknad`() {
        val behandlingId = BehandlingId(Random.nextLong())
        InMemoryMottattDokumentRepository.lagre(
            mottattDokument(behandlingId, journalpostId = "1112223", mottattTidspunkt = (15 januar 2026).atStartOfDay())
        )

        val løsning = VurderKravLøsning(
            kravVurderinger = setOf(
                NyttKravLøsningDto(
                    journalpostId = JournalpostId("1112223"),
                    begrunnelse = "test",
                    søknadsdato = Søknadsdato(16 januar 2026, SøknadsdatoÅrsak.SøknadMottatt),
                    muligRettFra = null,
                )
            )
        )

        assertThrows<UgyldigForespørselException> {
            løser.løs(kontekst(behandlingId), løsning)
        }
    }

    @Test
    fun `skal feile hvis mulig rett fra dato er etter søknadsdato`() {
        val behandlingId = BehandlingId(Random.nextLong())
        InMemoryMottattDokumentRepository.lagre(
            mottattDokument(behandlingId, journalpostId = "1112223", mottattTidspunkt = (15 januar 2026).atStartOfDay())
        )

        val løsning = VurderKravLøsning(
            kravVurderinger = setOf(
                NyttKravLøsningDto(
                    journalpostId = JournalpostId("1112223"),
                    begrunnelse = "test",
                    søknadsdato = Søknadsdato(15 januar 2026, SøknadsdatoÅrsak.SøknadMottatt),
                    muligRettFra = MuligRettFra(
                        20 januar 2026,
                        MuligRettFraÅrsak.IkkeIStandTilÅSøkeTidligere
                    ),
                )
            )
        )

        assertThrows<UgyldigForespørselException> {
            løser.løs(kontekst(behandlingId), løsning)
        }
    }

    @Test
    fun `skal feile for kravtyper som ikke er implementert`() {
        val behandlingId = BehandlingId(Random.nextLong())
        InMemoryMottattDokumentRepository.lagre(
            mottattDokument(behandlingId, journalpostId = "1112223")
        )

        val løsning = VurderKravLøsning(
            kravVurderinger = setOf(
                GjenopptakKravLøsningDto(
                    journalpostId = JournalpostId("1112223"),
                    begrunnelse = "test",
                    søknadsdato = Søknadsdato(15 januar 2026, SøknadsdatoÅrsak.SøknadMottatt),
                    muligRettFra = null,
                )
            )
        )

        assertThrows<UgyldigForespørselException> {
            løser.løs(kontekst(behandlingId), løsning)
        }
    }

    @Test
    fun `skal mappe nytt krav`() {
        val behandlingId = BehandlingId(Random.nextLong())
        InMemoryMottattDokumentRepository.lagre(
            mottattDokument(behandlingId, journalpostId = "1112223", mottattTidspunkt = (15 januar 2026).atStartOfDay())
        )

        val løsning = VurderKravLøsning(
            kravVurderinger = setOf(
                NyttKravLøsningDto(
                    journalpostId = JournalpostId("1112223"),
                    begrunnelse = "Gyldig krav",
                    søknadsdato = Søknadsdato(15 januar 2026, SøknadsdatoÅrsak.SøknadMottatt),
                    muligRettFra = null,
                )
            )
        )

        val resultat = løser.løs(kontekst(behandlingId), løsning)

        val lagretVurdering = InMemoryKravRepository.hentHvisEksisterer(behandlingId)!!.vurderinger.single() as NyttKrav
        assertThat(lagretVurdering.journalpostId).isEqualTo(JournalpostId("1112223"))
        assertThat(lagretVurdering.søknadsdato.dato).isEqualTo(15 januar 2026)
        assertThat(lagretVurdering.begrunnelse).isEqualTo("Gyldig krav")
        assertThat(resultat.begrunnelse).isEqualTo("Fullført")
    }

    private fun kontekst(behandlingId: BehandlingId): AvklaringsbehovKontekst =
        AvklaringsbehovKontekst(
            Bruker("Z123456"),
            FlytKontekst(SakId(Random.nextLong()), behandlingId, null, TypeBehandling.Førstegangsbehandling)
        )

    private fun mottattDokument(
        behandlingId: BehandlingId,
        journalpostId: String = "1112223",
        mottattTidspunkt: LocalDateTime = LocalDateTime.now(),
    ) = MottattDokument(
        referanse = InnsendingReferanse(JournalpostId(journalpostId)),
        sakId = SakId(Random.nextLong()),
        behandlingId = behandlingId,
        mottattTidspunkt = mottattTidspunkt,
        type = InnsendingType.SØKNAD,
        kanal = Kanal.DIGITAL,
        strukturertDokument = null,
    )
}
