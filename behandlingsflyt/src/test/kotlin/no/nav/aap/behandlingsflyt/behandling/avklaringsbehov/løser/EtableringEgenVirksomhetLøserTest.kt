package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.EtableringEgenVirksomhetLøsning
import no.nav.aap.behandlingsflyt.behandling.etableringegenvirksomhet.EtableringEgenVirksomhetService
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.Hverdager
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.bistand.Bistandsvurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.etableringegenvirksomhet.EierVirksomhet
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.etableringegenvirksomhet.EtableringEgenVirksomhetLøsningDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.etableringegenvirksomhet.EtableringEgenVirksomhetVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.etableringegenvirksomhet.EtableringFase
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.etableringegenvirksomhet.MAKS_OPPSTART_HVERDAGER
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.etableringegenvirksomhet.MAKS_UTVIKLING_HVERDAGER
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.gjeldendeVurderinger
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.ArbeidsevneNedsattValg
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.Sykdomsvurdering
import no.nav.aap.behandlingsflyt.help.avklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.help.opprettInMemorySakOgBehandling
import no.nav.aap.behandlingsflyt.help.opprettInMemorySakOgRevurdering
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryBehandlingRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryBistandRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryEtableringEgenVirksomRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemorySykdomRepository
import no.nav.aap.komponenter.httpklient.exception.UgyldigForespørselException
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Bruker
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import java.time.Instant
import java.time.LocalDate

class EtableringEgenVirksomhetLøserTest {

    private val løser = EtableringEgenVirksomhetLøser(
        InMemoryEtableringEgenVirksomRepository,
        InMemoryBehandlingRepository,
        EtableringEgenVirksomhetService(
            InMemoryEtableringEgenVirksomRepository,
            InMemoryBehandlingRepository,
            InMemoryBistandRepository,
            InMemorySykdomRepository
        )
    )

    private fun oppfyltVurdering(
        fom: LocalDate,
        fase: EtableringFase = EtableringFase.UTVIKLING,
        erRegistrertINødvendigeOffentligeRegister: Boolean? = null,
        tom: LocalDate? = null,
    ): EtableringEgenVirksomhetLøsningDto {
        val beregnetTom = tom ?: when (fase) {
            EtableringFase.UTVIKLING -> fom.plusMonths(6).minusDays(1)
            EtableringFase.OPPSTART -> fom.plusMonths(3).minusDays(1)
        }

        return EtableringEgenVirksomhetLøsningDto(
            begrunnelse = "meee",
            fom = fom,
            tom = beregnetTom,
            virksomhetNavn = "peppas peppers",
            orgNr = null,
            foreliggerFagligVurdering = true,
            virksomhetErNy = true,
            brukerEierVirksomheten = EierVirksomhet.EIER_MINST_50_PROSENT,
            kanFøreTilSelvforsørget = true,
            jobberBrukerAktivMedVirksomheten = true,
            fase = fase,
            erRegistrertINødvendigeOffentligeRegister = erRegistrertINødvendigeOffentligeRegister,
        )
    }

    @Test
    fun `Må ha definert minst én periode i tidsplanen dersom vilkåret er oppfylt for en periode`() {
        val (sak, behandling) = opprettInMemorySakOgBehandling(LocalDate.now())
        oppfyllSykdomOgBistand(behandling)

        val kontekst = avklaringsbehovKontekst { this.behandling = behandling }
        val løsning = EtableringEgenVirksomhetLøsning(
            listOf(oppfyltVurdering(fom = sak.rettighetsperiode.fom.plusDays(1)))
        )

        assertDoesNotThrow { løser.løs(kontekst, løsning) }
    }

    @Test
    fun `Oppstart må være minst én dag etter første dag med innvilget AAP`() {
        val (sak, behandling) = opprettInMemorySakOgBehandling(LocalDate.now())
        oppfyllSykdomOgBistand(behandling)

        val kontekst = avklaringsbehovKontekst { this.behandling = behandling }
        val løsning = EtableringEgenVirksomhetLøsning(
            listOf(
                oppfyltVurdering(
                    fom = sak.rettighetsperiode.fom,
                    fase = EtableringFase.OPPSTART,
                    erRegistrertINødvendigeOffentligeRegister = true,
                )
            )
        )

        val feil = assertThrows<UgyldigForespørselException> { løser.løs(kontekst, løsning) }
        assertThat(feil.message).contains("Vurderingen kan tidligst gjelde fra dagen etter første mulige dag med AAP")
    }

    @Test
    fun `Skal ikke kunne legge oppstartsperioder før utviklingsperioden`() {
        val (sak, førstegangsbehandling, revurdering) = opprettInMemorySakOgRevurdering(LocalDate.now())
        oppfyllSykdomOgBistand(revurdering)

        val kontekst = avklaringsbehovKontekst { this.behandling = revurdering }
        InMemoryEtableringEgenVirksomRepository.lagre(
            førstegangsbehandling.id,
            listOf(
                EtableringEgenVirksomhetVurdering(
                    begrunnelse = "Tidligere utvikling",
                    fom = sak.rettighetsperiode.fom.plusDays(10),
                    tom = sak.rettighetsperiode.fom.plusDays(30),
                    vurdertAv = Bruker("saks"),
                    opprettet = Instant.now(),
                    vurdertIBehandling = førstegangsbehandling.id,
                    virksomhetNavn = "peppas peppers",
                    foreliggerFagligVurdering = true,
                    virksomhetErNy = true,
                    brukerEierVirksomheten = EierVirksomhet.EIER_MINST_50_PROSENT,
                    kanFøreTilSelvforsørget = true,
                    erRegistrertINødvendigeOffentligeRegister = true,
                    fase = EtableringFase.UTVIKLING,
                    jobberBrukerAktivMedVirksomheten = true
                )
            )
        )

        val løsning = EtableringEgenVirksomhetLøsning(
            listOf(
                oppfyltVurdering(
                    fom = sak.rettighetsperiode.fom.plusDays(5),
                    fase = EtableringFase.OPPSTART,
                    erRegistrertINødvendigeOffentligeRegister = true,
                    tom = sak.rettighetsperiode.fom.plusDays(20),
                )
            )
        )

        val feil = assertThrows<UgyldigForespørselException> { løser.løs(kontekst, løsning) }
        assertThat(feil.message).contains("Oppstartsperioden kan ikke være før utviklingsfase")
    }

    @Test
    fun `Ny vurdering erstatter tidligere perioder med samme fom`() {
        val (sak, førstegangsbehandling, revurdering) = opprettInMemorySakOgRevurdering(LocalDate.now())
        oppfyllSykdomOgBistand(revurdering)
        val vurderingFom = sak.rettighetsperiode.fom.plusDays(1)
        InMemoryEtableringEgenVirksomRepository.lagre(
            førstegangsbehandling.id,
            listOf(
                EtableringEgenVirksomhetVurdering(
                    begrunnelse = "Opprinnelig vurdering",
                    fom = vurderingFom,
                    tom = vurderingFom.plusMonths(2).minusDays(1),
                    vurdertAv = Bruker("saks"),
                    opprettet = Instant.now(),
                    vurdertIBehandling = førstegangsbehandling.id,
                    virksomhetNavn = "peppas peppers",
                    foreliggerFagligVurdering = true,
                    virksomhetErNy = true,
                    brukerEierVirksomheten = EierVirksomhet.EIER_MINST_50_PROSENT,
                    kanFøreTilSelvforsørget = true,
                    erRegistrertINødvendigeOffentligeRegister = true,
                    jobberBrukerAktivMedVirksomheten = true,
                    fase = EtableringFase.OPPSTART
                )
            )
        )

        val løsning = EtableringEgenVirksomhetLøsning(
            listOf(
                oppfyltVurdering(
                    fom = vurderingFom,
                    fase = EtableringFase.OPPSTART,
                    erRegistrertINødvendigeOffentligeRegister = true,
                    tom = vurderingFom.plusMonths(3).minusDays(1),
                )
            )
        )
        val kontekst = avklaringsbehovKontekst { behandling = revurdering }

        assertDoesNotThrow { løser.løs(kontekst, løsning) }

        val lagredeVurderinger = InMemoryEtableringEgenVirksomRepository
            .hentHvisEksisterer(revurdering.id)
            ?.vurderinger
            .orEmpty()
        val gjeldendeVurdering = lagredeVurderinger.gjeldendeVurderinger().segmenter().single().verdi

        assertThat(lagredeVurderinger).hasSize(2)
        assertThat(gjeldendeVurdering.fase).isEqualTo(EtableringFase.OPPSTART)
        assertThat(gjeldendeVurdering.fom).isEqualTo(vurderingFom)
        assertThat(gjeldendeVurdering.tom).isEqualTo(vurderingFom.plusMonths(3).minusDays(1))
    }

    @Test
    fun `Skal ikke kunne overstige oppstartsperiodens kvote på 66 dager`() {
        val (sak, førstegangsbehandling, revurdering) = opprettInMemorySakOgRevurdering(LocalDate.now())
        oppfyllSykdomOgBistand(revurdering)

        val kontekst = avklaringsbehovKontekst { this.behandling = revurdering }
        val forrigeOppstartFom = sak.rettighetsperiode.fom.plusDays(1)
        InMemoryEtableringEgenVirksomRepository.lagre(
            førstegangsbehandling.id,
            listOf(
                EtableringEgenVirksomhetVurdering(
                    begrunnelse = "Brukte opp hele oppstartskvoten",
                    fom = forrigeOppstartFom,
                    tom = Hverdager(MAKS_OPPSTART_HVERDAGER).fraOgMed(forrigeOppstartFom),
                    vurdertAv = Bruker("saks"),
                    opprettet = Instant.now(),
                    vurdertIBehandling = førstegangsbehandling.id,
                    virksomhetNavn = "peppas peppers",
                    foreliggerFagligVurdering = true,
                    virksomhetErNy = true,
                    brukerEierVirksomheten = EierVirksomhet.EIER_MINST_50_PROSENT,
                    kanFøreTilSelvforsørget = true,
                    erRegistrertINødvendigeOffentligeRegister = true,
                    fase = EtableringFase.OPPSTART,
                    jobberBrukerAktivMedVirksomheten = true
                )
            )
        )

        val løsning = EtableringEgenVirksomhetLøsning(
            listOf(
                oppfyltVurdering(
                    fase = EtableringFase.OPPSTART,
                    fom = sak.rettighetsperiode.fom.plusMonths(6),
                    tom = null,
                    erRegistrertINødvendigeOffentligeRegister = true,
                )
            )
        )

        val feil = assertThrows<UgyldigForespørselException> { løser.løs(kontekst, løsning) }
        assertThat(feil.message).contains("Kvoten for OPPSTART er brukt opp")
    }

    @Test
    fun `Skal ikke kunne overstige utviklingsperiodens kvote på 131 dager`() {
        val (sak, førstegangsbehandling, revurdering) = opprettInMemorySakOgRevurdering(LocalDate.now())
        oppfyllSykdomOgBistand(revurdering)

        val kontekst = avklaringsbehovKontekst { this.behandling = revurdering }
        val forrigeUtviklingFom = sak.rettighetsperiode.fom.plusDays(1)
        InMemoryEtableringEgenVirksomRepository.lagre(
            førstegangsbehandling.id,
            listOf(
                EtableringEgenVirksomhetVurdering(
                    begrunnelse = "Brukte opp hele utviklingskvoten",
                    fom = forrigeUtviklingFom,
                    tom = Hverdager(MAKS_UTVIKLING_HVERDAGER).fraOgMed(forrigeUtviklingFom),
                    vurdertAv = Bruker("saks"),
                    opprettet = Instant.now(),
                    vurdertIBehandling = førstegangsbehandling.id,
                    virksomhetNavn = "peppas peppers",
                    foreliggerFagligVurdering = true,
                    virksomhetErNy = true,
                    brukerEierVirksomheten = EierVirksomhet.EIER_MINST_50_PROSENT,
                    kanFøreTilSelvforsørget = true,
                    erRegistrertINødvendigeOffentligeRegister = true,
                    fase = EtableringFase.UTVIKLING,
                    jobberBrukerAktivMedVirksomheten = true
                )
            )
        )

        val løsning = EtableringEgenVirksomhetLøsning(
            listOf(
                oppfyltVurdering(
                    fase = EtableringFase.UTVIKLING,
                    fom = sak.rettighetsperiode.fom.plusMonths(10),
                    tom = null,
                )
            )
        )

        val feil = assertThrows<UgyldigForespørselException> { løser.løs(kontekst, løsning) }
        assertThat(feil.message).contains("Kvoten for UTVIKLING er brukt opp")
    }

    private fun oppfyllSykdomOgBistand(behandling: Behandling) {
        InMemorySykdomRepository.lagre(
            behandling.id, listOf(
                Sykdomsvurdering(
                    begrunnelse = "...",
                    vurderingenGjelderFra = LocalDate.now(),
                    vurderingenGjelderTil = LocalDate.now().plusMonths(6),
                    harSkadeSykdomEllerLyte = true,
                    harNedsattArbeidsevne = ArbeidsevneNedsattValg.JA,
                    erSkadeSykdomEllerLyteVesentligdel = true,
                    erNedsettelseIArbeidsevneMerEnnHalvparten = true,
                    erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense = false,
                    yrkesskadeBegrunnelse = null,
                    diagnose = null,
                    vurdertIBehandling = behandling.id,
                    opprettet = Instant.now(),
                    vurdertAv = Bruker("saks")
                )
            )
        )
        InMemoryBistandRepository.lagre(
            behandling.id, listOf(
                Bistandsvurdering(
                    begrunnelse = "...",
                    erBehovForAktivBehandling = true,
                    erBehovForArbeidsrettetTiltak = true,
                    erBehovForAnnenOppfølging = false,
                    overgangBegrunnelse = null,
                    skalVurdereAapIOvergangTilArbeid = false,
                    vurdertAv = Bruker("saks"),
                    fom = LocalDate.now(),
                    tom = LocalDate.now().plusMonths(6),
                    opprettet = Instant.now(),
                    vurdertIBehandling = behandling.id
                )
            )
        )
    }
}