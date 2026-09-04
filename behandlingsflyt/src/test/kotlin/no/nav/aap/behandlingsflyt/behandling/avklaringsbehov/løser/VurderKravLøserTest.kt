package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.VurderKravLøsning
import no.nav.aap.behandlingsflyt.behandling.underveis.KvoteService
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokument
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.KlageKravLøsningDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.MigrertKrav
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.MigrertRettighetstype
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.MigrertKravLøsningDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.RelevantKrav
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.RelevantKravLøsningDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.OverstyrMuligRettFra
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.OverstyrMuligRettFraÅrsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.Søknadsdato
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.SøknadsdatoÅrsak
import no.nav.aap.behandlingsflyt.help.opprettInMemorySak
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekst
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryBehandlingRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryKravRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryMottattDokumentRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemorySakRepository
import no.nav.aap.behandlingsflyt.test.februar
import no.nav.aap.behandlingsflyt.test.januar
import no.nav.aap.behandlingsflyt.test.mars
import no.nav.aap.komponenter.httpklient.exception.UgyldigForespørselException
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Bruker
import no.nav.aap.komponenter.verdityper.Tid
import no.nav.aap.verdityper.dokument.JournalpostId
import no.nav.aap.verdityper.dokument.Kanal
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.random.Random

class VurderKravLøserTest {

    private val løser = VurderKravLøser(
        InMemoryKravRepository,
        InMemoryMottattDokumentRepository,
        SakService(InMemorySakRepository, InMemoryBehandlingRepository),
    )

    @Test
    fun `skal feile hvis søknadsdato er ulik dato for mottatt søknad i samme behandling`() {
        val sakId = SakId(Random.nextLong())
        val behandlingId = BehandlingId(Random.nextLong())
        InMemoryMottattDokumentRepository.lagre(
            mottattDokument(behandlingId, sakId, journalpostId = "1112223", mottattTidspunkt = (15 januar 2026).atStartOfDay())
        )

        val løsning = VurderKravLøsning(
            kravVurderinger = setOf(
                RelevantKravLøsningDto(
                    journalpostId = JournalpostId("1112223"),
                    begrunnelse = "test",
                    søknadsdato = Søknadsdato(16 januar 2026, SøknadsdatoÅrsak.SøknadMottatt, begrunnelse = "Test"),
                    overstyrMuligRettFra = null,
                )
            )
        )

        assertThrows<UgyldigForespørselException> {
            løser.løs(kontekst(sakId, behandlingId), løsning)
        }
    }

    @Test
    fun `skal feile hvis søknadsdato er ulik dato for mottatt søknad i samme sak`() {
        val sakId = SakId(Random.nextLong())
        val gammelBehandlingId = BehandlingId(Random.nextLong())
        val behandlingId = BehandlingId(Random.nextLong())

        InMemoryMottattDokumentRepository.lagre(
            mottattDokument(gammelBehandlingId, sakId, journalpostId = "111122224", mottattTidspunkt = (10 januar 2026).atStartOfDay())
        )

        val løsning = VurderKravLøsning(
            kravVurderinger = setOf(
                RelevantKravLøsningDto(
                    journalpostId = JournalpostId("111122224"),
                    begrunnelse = "Ny søknad",
                    søknadsdato = Søknadsdato(15 januar 2026, SøknadsdatoÅrsak.SøknadMottatt, begrunnelse = "Test"),
                    overstyrMuligRettFra = null,
                )
            )
        )

        assertThrows<UgyldigForespørselException> {
            løser.løs(kontekst(sakId, behandlingId), løsning)
        }
    }


    @Test
    fun `skal feile hvis mulig rett fra dato er etter søknadsdato`() {
        val sakId = SakId(Random.nextLong())
        val behandlingId = BehandlingId(Random.nextLong())
        InMemoryMottattDokumentRepository.lagre(
            mottattDokument(behandlingId, sakId, journalpostId = "1112223", mottattTidspunkt = (15 januar 2026).atStartOfDay())
        )

        val løsning = VurderKravLøsning(
            kravVurderinger = setOf(
                RelevantKravLøsningDto(
                    journalpostId = JournalpostId("1112223"),
                    begrunnelse = "test",
                    søknadsdato = Søknadsdato(15 januar 2026, SøknadsdatoÅrsak.SøknadMottatt, begrunnelse = "Test"),
                    overstyrMuligRettFra = OverstyrMuligRettFra(
                        20 januar 2026,
                        OverstyrMuligRettFraÅrsak.IkkeIStandTilÅSøkeTidligere,
                        begrunnelse = "Overstyrt"
                    ),
                )
            )
        )

        assertThrows<UgyldigForespørselException> {
            løser.løs(kontekst(sakId, behandlingId), løsning)
        }
    }

    @Test
    fun `skal feile for kravtyper som ikke er implementert`() {
        val sakId = SakId(Random.nextLong())
        val behandlingId = BehandlingId(Random.nextLong())
        InMemoryMottattDokumentRepository.lagre(
            mottattDokument(behandlingId, sakId, journalpostId = "1112223")
        )

        val løsning = VurderKravLøsning(
            kravVurderinger = setOf(
                KlageKravLøsningDto(
                    journalpostId = JournalpostId("1112223"),
                    begrunnelse = "test",
                )
            )
        )

        assertThrows<UgyldigForespørselException> {
            løser.løs(kontekst(sakId, behandlingId), løsning)
        }
    }

    @Test
    fun `skal mappe nytt krav`() {
        val sakId = SakId(Random.nextLong())
        val behandlingId = BehandlingId(Random.nextLong())
        InMemoryMottattDokumentRepository.lagre(
            mottattDokument(behandlingId, sakId, journalpostId = "1112223", mottattTidspunkt = (15 januar 2026).atStartOfDay())
        )

        val løsning = VurderKravLøsning(
            kravVurderinger = setOf(
                RelevantKravLøsningDto(
                    journalpostId = JournalpostId("1112223"),
                    begrunnelse = "Gyldig krav",
                    søknadsdato = Søknadsdato(15 januar 2026, SøknadsdatoÅrsak.SøknadMottatt, begrunnelse = "Test"),
                    overstyrMuligRettFra = null,
                )
            )
        )

        val resultat = løser.løs(kontekst(sakId, behandlingId), løsning)

        val lagretVurdering = InMemoryKravRepository.hentHvisEksisterer(behandlingId)!!.vurderinger.single() as RelevantKrav
        assertThat(lagretVurdering.journalpostId).isEqualTo(JournalpostId("1112223"))
        assertThat(lagretVurdering.søknadsdato.dato).isEqualTo(15 januar 2026)
        assertThat(lagretVurdering.begrunnelse).isEqualTo("Gyldig krav")
        assertThat(resultat.begrunnelse).isEqualTo("Fullført")
    }
    
    @Test
    fun `skal ikke overstyre rettighetsperiode når krav ikke er migrert`() {
        val sak = opprettInMemorySak()
        val behandlingId = BehandlingId(Random.nextLong())
        InMemoryMottattDokumentRepository.lagre(
            mottattDokument(behandlingId, sak.id, journalpostId = "1112223", mottattTidspunkt = (15 januar 2026).atStartOfDay())
        )

        val løsning = VurderKravLøsning(
            kravVurderinger = setOf(
                RelevantKravLøsningDto(
                    journalpostId = JournalpostId("1112223"),
                    begrunnelse = "Gyldig krav",
                    søknadsdato = Søknadsdato(15 januar 2026, SøknadsdatoÅrsak.SøknadMottatt, begrunnelse = "Test"),
                    overstyrMuligRettFra = null,
                )
            )
        )

        løser.løs(kontekst(sak.id, behandlingId), løsning)

        assertThat(InMemorySakRepository.hent(sak.id).rettighetsperiode).isEqualTo(sak.rettighetsperiode)
    }

    @Test
    fun `skal feile hvis flere enn ett migrert krav sendes inn`() {
        val sakId = SakId(Random.nextLong())
        val behandlingId = BehandlingId(Random.nextLong())

        val løsning = VurderKravLøsning(
            kravVurderinger = setOf(
                MigrertKravLøsningDto(
                    begrunnelse = "Migrert fra Arena",
                    virkningstidspunktArena = 1 januar 2024,
                    muligRettFra = 1 januar 2024,
                    arenaSaksnummer = "ARENA-1",
                    rettighetstype = MigrertRettighetstype.ORDINÆR,
                    resterendeKvoteOrdinaer = 0,
                ),
                MigrertKravLøsningDto(
                    begrunnelse = "Migrert fra Arena 2",
                    virkningstidspunktArena = 1 februar 2024,
                    muligRettFra = 1 februar 2024,
                    arenaSaksnummer = "ARENA-2",
                    rettighetstype = MigrertRettighetstype.ORDINÆR,
                    resterendeKvoteOrdinaer = 0,
                ),
            )
        )

        assertThrows<UgyldigForespørselException> {
            løser.løs(kontekst(sakId, behandlingId), løsning)
        }
    }

    @Test
    fun `skal mappe migrert krav og overstyre rettighetsperioden til sakens muligRettFra`() {
        val sak = opprettInMemorySak()
        val behandlingId = BehandlingId(Random.nextLong())
        val muligRettFra = 8 januar 2024

        val løsning = VurderKravLøsning(
            kravVurderinger = setOf(
                MigrertKravLøsningDto(
                    begrunnelse = "Migrert fra Arena",
                    virkningstidspunktArena = 1 januar 2024,
                    muligRettFra = muligRettFra,
                    arenaSaksnummer = "ARENA-4711",
                    rettighetstype = MigrertRettighetstype.ORDINÆR,
                    resterendeKvoteOrdinaer = 5,
                )
            )
        )

        val resultat = løser.løs(kontekst(sak.id, behandlingId), løsning)

        val lagretVurdering = InMemoryKravRepository.hentHvisEksisterer(behandlingId)!!.vurderinger.single() as MigrertKrav
        assertThat(lagretVurdering.journalpostId).isNull()
        assertThat(lagretVurdering.arenaSaksnummer).isEqualTo("ARENA-4711")
        assertThat(lagretVurdering.rettighetstype).isEqualTo(MigrertRettighetstype.ORDINÆR)
        assertThat(lagretVurdering.resterendeKvoteOrdinaer).isEqualTo(5)
        assertThat(lagretVurdering.muligRettFra).isEqualTo(muligRettFra)
        assertThat(resultat.begrunnelse).isEqualTo("Fullført")

        assertThat(InMemorySakRepository.hent(sak.id).rettighetsperiode).isEqualTo(Periode(muligRettFra, Tid.MAKS))
    }

    @Test
    fun `skal feile hvis muligRettFra for migrert krav er i fremtiden`() {
        val sak = opprettInMemorySak()
        val behandlingId = BehandlingId(Random.nextLong())

        val løsning = VurderKravLøsning(
            kravVurderinger = setOf(
                // 5 januar 2099 er en mandag, men langt fram i tid
                migrertKravLøsningDto(
                    muligRettFra = LocalDate.of(2099, 1, 5),
                    virkningstidspunktArena = 1 januar 2024,
                )
            )
        )

        assertThrows<UgyldigForespørselException> {
            løser.løs(kontekst(sak.id, behandlingId), løsning)
        }
    }

    @Test
    fun `skal feile hvis muligRettFra for migrert krav ikke er en mandag`() {
        val sak = opprettInMemorySak()
        val behandlingId = BehandlingId(Random.nextLong())

        val løsning = VurderKravLøsning(
            kravVurderinger = setOf(
                // 1 mars 2024 er en fredag
                migrertKravLøsningDto(
                    muligRettFra = 1 mars 2024,
                    virkningstidspunktArena = 1 januar 2024,
                )
            )
        )

        assertThrows<UgyldigForespørselException> {
            løser.løs(kontekst(sak.id, behandlingId), løsning)
        }
    }

    @Test
    fun `skal feile hvis muligRettFra for migrert krav ikke er etter virkningstidspunktArena`() {
        val sak = opprettInMemorySak()
        val behandlingId = BehandlingId(Random.nextLong())

        val løsning = VurderKravLøsning(
            kravVurderinger = setOf(
                migrertKravLøsningDto(
                    muligRettFra = 8 januar 2024,
                    virkningstidspunktArena = 8 januar 2024,
                )
            )
        )

        assertThrows<UgyldigForespørselException> {
            løser.løs(kontekst(sak.id, behandlingId), løsning)
        }
    }

    @Test
    fun `skal feile hvis resterendeKvoteOrdinaer for migrert krav er 0 eller lavere`() {
        val sak = opprettInMemorySak()
        val behandlingId = BehandlingId(Random.nextLong())

        val løsning = VurderKravLøsning(
            kravVurderinger = setOf(
                migrertKravLøsningDto(resterendeKvoteOrdinaer = 0)
            )
        )

        assertThrows<UgyldigForespørselException> {
            løser.løs(kontekst(sak.id, behandlingId), løsning)
        }
    }

    @Test
    fun `skal feile hvis resterendeKvoteOrdinaer for migrert krav er høyere enn standardKvoter ordinærKvote`() {
        val sak = opprettInMemorySak()
        val behandlingId = BehandlingId(Random.nextLong())
        val maksKvote = KvoteService.standardKvoter.ordinærkvote.asInt

        val løsning = VurderKravLøsning(
            kravVurderinger = setOf(
                migrertKravLøsningDto(resterendeKvoteOrdinaer = maksKvote + 1)
            )
        )

        assertThrows<UgyldigForespørselException> {
            løser.løs(kontekst(sak.id, behandlingId), løsning)
        }
    }

    @Test
    fun `skal godta migrert krav med resterendeKvoteOrdinaer lik standardKvoter ordinærKvote`() {
        val sak = opprettInMemorySak()
        val behandlingId = BehandlingId(Random.nextLong())
        val maksKvote = KvoteService.standardKvoter.ordinærkvote.asInt

        val løsning = VurderKravLøsning(
            kravVurderinger = setOf(
                migrertKravLøsningDto(resterendeKvoteOrdinaer = maksKvote)
            )
        )

        løser.løs(kontekst(sak.id, behandlingId), løsning)

        val lagretVurdering = InMemoryKravRepository.hentHvisEksisterer(behandlingId)!!.vurderinger.single() as MigrertKrav
        assertThat(lagretVurdering.resterendeKvoteOrdinaer).isEqualTo(maksKvote)
    }

    private fun migrertKravLøsningDto(
        muligRettFra: LocalDate = 8 januar 2024,
        virkningstidspunktArena: LocalDate = 1 januar 2024,
        resterendeKvoteOrdinaer: Int = 5,
    ) = MigrertKravLøsningDto(
        begrunnelse = "Migrert fra Arena",
        virkningstidspunktArena = virkningstidspunktArena,
        muligRettFra = muligRettFra,
        arenaSaksnummer = "ARENA-4711",
        rettighetstype = MigrertRettighetstype.ORDINÆR,
        resterendeKvoteOrdinaer = resterendeKvoteOrdinaer,
    )

    private fun kontekst(sakId: SakId, behandlingId: BehandlingId): AvklaringsbehovKontekst =
        AvklaringsbehovKontekst(
            Bruker("Z123456"),
            FlytKontekst(sakId, behandlingId, null, TypeBehandling.Førstegangsbehandling)
        )

    private fun mottattDokument(
        behandlingId: BehandlingId,
        sakId: SakId,
        journalpostId: String = "1112223",
        mottattTidspunkt: LocalDateTime = LocalDateTime.now(),
    ) = MottattDokument(
        referanse = InnsendingReferanse(JournalpostId(journalpostId)),
        sakId = sakId,
        behandlingId = behandlingId,
        mottattTidspunkt = mottattTidspunkt,
        type = InnsendingType.SØKNAD,
        kanal = Kanal.DIGITAL,
        strukturertDokument = null,
    )
}
