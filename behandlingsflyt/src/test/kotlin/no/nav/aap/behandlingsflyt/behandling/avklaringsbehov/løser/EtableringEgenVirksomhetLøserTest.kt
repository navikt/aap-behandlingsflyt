package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.EtableringEgenVirksomhetLøsning
import no.nav.aap.behandlingsflyt.behandling.etableringegenvirksomhet.EtableringEgenVirksomhetService
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.Hverdager
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.Hverdager.Companion.antallHverdager
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.etableringegenvirksomhet.EierVirksomhet
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.etableringegenvirksomhet.EtableringEgenVirksomhetLøsningDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.etableringegenvirksomhet.EtableringEgenVirksomhetVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.etableringegenvirksomhet.EtableringFase
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.etableringegenvirksomhet.EtableringVersjon
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.etableringegenvirksomhet.TilOgMedBeregning
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.etableringegenvirksomhet.beregnTomForSistePeriode
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.etableringegenvirksomhet.erFunksjoneltLik
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.etableringegenvirksomhet.tilNyttFormat
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
import no.nav.aap.bistandsbehov.Bistandsvurdering
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

    @Test
    fun `endring i legacy-perioder er en funksjonell endring`() {
        val (sak, behandling) = opprettInMemorySakOgBehandling(LocalDate.now())
        val fom = sak.rettighetsperiode.fom.plusDays(1)
        val før = oppfyltVurdering(fom, tom = fom.plusDays(30)).copy(
            fase = null,
            utviklingsPerioder = listOf(Periode(fom, fom.plusDays(4))),
            oppstartsPerioder = listOf(Periode(fom.plusDays(7), fom.plusDays(11))),
        ).toEtableringEgenVirksomhetVurdering(Bruker("saks"), behandling.id)
        val endringer = listOf(
            før.copy(utviklingsPerioder = listOf(Periode(fom, fom.plusDays(5)))),
            før.copy(oppstartsPerioder = listOf(Periode(fom.plusDays(7), fom.plusDays(12)))),
        )

        assertThat(listOf(før).erFunksjoneltLik(listOf(før.copy()))).isTrue()
        assertThat(endringer.map { endret -> listOf(før).erFunksjoneltLik(listOf(endret)) })
            .describedAs("Endring av henholdsvis utviklings- og oppstartsperiode")
            .containsExactly(false, false)
    }

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
            EtableringFase.UTVIKLING -> fom.plusDays(EtableringFase.UTVIKLING.maksHverdager.toLong())
            EtableringFase.OPPSTART -> fom.plusDays(EtableringFase.OPPSTART.maksHverdager.toLong())
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
                    tom = sak.rettighetsperiode.fom.plusDays(130),
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
                    jobberBrukerAktivMedVirksomheten = true,
                    versjon = EtableringVersjon.V1
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
        assertThat(feil.message).contains("Oppstartsperiode kan ikke ligge før en utviklingsperiode.")
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
                    versjon = EtableringVersjon.V1,
                    fase = EtableringFase.OPPSTART,
                    erRegistrertINødvendigeOffentligeRegister = true,
                    jobberBrukerAktivMedVirksomheten = true,
                )
            )
        )

        val forventetTom = Hverdager(EtableringFase.OPPSTART.maksHverdager).fraOgMed(vurderingFom)

        val løsning = EtableringEgenVirksomhetLøsning(
            listOf(
                oppfyltVurdering(
                    fom = vurderingFom,
                    fase = EtableringFase.OPPSTART,
                    erRegistrertINødvendigeOffentligeRegister = true,
                    tom = forventetTom,
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
        // tom beregnes av beregnTomForSistePeriode ut fra virkedagskvoten, ikke kalendermåneder.
        assertThat(gjeldendeVurdering.tom).isEqualTo(forventetTom)
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
                    tom = Hverdager(EtableringFase.OPPSTART.maksHverdager).fraOgMed(forrigeOppstartFom),
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
                    jobberBrukerAktivMedVirksomheten = true,
                    versjon = EtableringVersjon.V1
                )
            )
        )

        val løsning = EtableringEgenVirksomhetLøsning(
            listOf(
                oppfyltVurdering(
                    fase = EtableringFase.OPPSTART,
                    fom = sak.rettighetsperiode.fom.plusMonths(7),
                    tom = null,
                    erRegistrertINødvendigeOffentligeRegister = true,
                )
            )
        )

        val feil = assertThrows<UgyldigForespørselException> { løser.løs(kontekst, løsning) }
        assertThat(feil.message).contains("Oppsatte oppstartsdager overstiger gjenværende dager: 66 / 66")
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
                    tom = Hverdager(EtableringFase.UTVIKLING.maksHverdager).fraOgMed(forrigeUtviklingFom),
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
                    jobberBrukerAktivMedVirksomheten = true,
                    versjon = EtableringVersjon.V1
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
        assertThat(feil.message).contains("Oppsatte utviklingsdager overstiger gjenværende dager: 131 / 131")
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

    @Test
    fun `kvoteberegning teller bare gjeldende deler av historiske vurderinger med samme startdato`() {
        val (_, førsteBehandling, revurdering) = opprettInMemorySakOgRevurdering(LocalDate.now())
        val førsteFom = LocalDate.now().plusDays(1)
        val nesteFom = førsteFom.plusDays(28)
        val eldre = oppfyltVurdering(førsteFom)
            .toEtableringEgenVirksomhetVurdering(Bruker("saks"), førsteBehandling.id)
            .copy(tom = førsteFom.plusDays(20), opprettet = Instant.parse("2025-01-01T00:00:00Z"))
        val nyere = eldre.copy(
            vurdertIBehandling = revurdering.id,
            tom = førsteFom.plusDays(6),
            opprettet = Instant.parse("2025-02-01T00:00:00Z"),
        )
        val siste = nyere.copy(fom = nesteFom, tom = null)

        val resultat = beregnTomForSistePeriode(listOf(eldre, nyere, siste), siste)

        val brukteDager = Periode(førsteFom, førsteFom.plusDays(20)).antallHverdager().asInt
        assertThat(resultat).isEqualTo(
            TilOgMedBeregning.Beregnet(Hverdager(131 - brukteDager).fraOgMed(nesteFom))
        )
    }

    @Test
    fun `kvoteberegning forlenger ikke avsluttede historiske perioder over opphold`() {
        val (_, behandling) = opprettInMemorySakOgBehandling(LocalDate.now())
        val førsteFom = LocalDate.now().plusDays(1)
        val tidligere = oppfyltVurdering(førsteFom)
            .toEtableringEgenVirksomhetVurdering(Bruker("saks"), behandling.id)
            .copy(tom = førsteFom.plusDays(6))
        val siste = tidligere.copy(fom = førsteFom.plusDays(28), tom = null)

        val resultat = beregnTomForSistePeriode(listOf(tidligere, siste), siste)

        assertThat(resultat).isEqualTo(
            TilOgMedBeregning.Beregnet(Hverdager(126).fraOgMed(siste.fom))
        )
    }

    @Test
    fun `beregner flere utviklingsvurderinger uten sluttdato fra gjeldende segmenter`() {
        val (sak, behandling) = opprettInMemorySakOgBehandling(LocalDate.now())
        oppfyllSykdomOgBistand(behandling)
        val førsteFom = sak.rettighetsperiode.fom.plusDays(1)
        val andreFom = førsteFom.plusDays(14)
        val kontekst = avklaringsbehovKontekst { this.behandling = behandling }

        løser.løs(
            kontekst,
            EtableringEgenVirksomhetLøsning(
                listOf(
                    oppfyltVurdering(andreFom).copy(tom = null),
                    oppfyltVurdering(førsteFom).copy(tom = null),
                )
            )
        )

        val vurderinger = requireNotNull(
            InMemoryEtableringEgenVirksomRepository.hentHvisEksisterer(behandling.id)
        ).vurderinger
        val segmenter = vurderinger.gjeldendeVurderinger().segmenter().toList()
        val forventetTom = Hverdager(EtableringFase.UTVIKLING.maksHverdager).fraOgMed(førsteFom)

        assertThat(vurderinger).allSatisfy { assertThat(it.tom).isEqualTo(forventetTom) }
        assertThat(segmenter.map { it.periode }).containsExactly(
            Periode(førsteFom, andreFom.minusDays(1)),
            Periode(andreFom, forventetTom),
        )
        assertThat(segmenter.sumOf { it.periode.antallHverdager().asInt }).isEqualTo(131)
    }

    @Test
    fun `oppstart overstyrer utvikling og valideres mot segmentets sluttdato`() {
        val (sak, behandling) = opprettInMemorySakOgBehandling(LocalDate.now())
        oppfyllSykdomOgBistand(behandling)
        val utviklingFom = sak.rettighetsperiode.fom.plusDays(1)
        val oppstartFom = utviklingFom.plusDays(14)

        løser.løs(
            avklaringsbehovKontekst { this.behandling = behandling },
            EtableringEgenVirksomhetLøsning(
                listOf(
                    oppfyltVurdering(utviklingFom).copy(tom = null),
                    oppfyltVurdering(
                        oppstartFom,
                        fase = EtableringFase.OPPSTART,
                        erRegistrertINødvendigeOffentligeRegister = true,
                    ).copy(tom = null),
                    oppfyltVurdering(
                        Hverdager(EtableringFase.OPPSTART.maksHverdager).fraOgMed(oppstartFom).plusDays(1)
                    ).copy(
                        fase = null,
                        virksomhetErNy = false,
                        tom = null,
                    ),
                )
            )
        )

        val segmenter = requireNotNull(
            InMemoryEtableringEgenVirksomRepository.hentHvisEksisterer(behandling.id)
        ).gjeldendeVurderingerSomTidslinje().segmenter().toList()

        assertThat(segmenter.first().periode).isEqualTo(Periode(utviklingFom, oppstartFom.minusDays(1)))
        assertThat(segmenter.filter { it.verdi.fase == EtableringFase.OPPSTART }
            .sumOf { it.periode.antallHverdager().asInt }).isEqualTo(66)
    }

    @Test
    fun `vurdering uten fase stopper kvoteforbruket`() {
        val (sak, behandling) = opprettInMemorySakOgBehandling(LocalDate.now())
        oppfyllSykdomOgBistand(behandling)
        val førsteFom = sak.rettighetsperiode.fom.plusDays(1)
        val avslagFom = førsteFom.plusDays(7)
        val andreFom = førsteFom.plusDays(14)

        løser.løs(
            avklaringsbehovKontekst { this.behandling = behandling },
            EtableringEgenVirksomhetLøsning(
                listOf(
                    oppfyltVurdering(førsteFom).copy(tom = null),
                    oppfyltVurdering(avslagFom).copy(
                        fase = null,
                        virksomhetErNy = false,
                        tom = null,
                    ),
                    oppfyltVurdering(andreFom).copy(tom = null),
                )
            )
        )

        val vurderinger = requireNotNull(
            InMemoryEtableringEgenVirksomRepository.hentHvisEksisterer(behandling.id)
        ).vurderinger
        val brukteDager = Periode(førsteFom, avslagFom.minusDays(1)).antallHverdager().asInt
        assertThat(vurderinger.single { it.fom == andreFom }.tom)
            .isEqualTo(Hverdager(131 - brukteDager).fraOgMed(andreFom))
        assertThat(vurderinger.gjeldendeVurderinger().segmenter()
            .filter { it.verdi.fase == EtableringFase.UTVIKLING }
            .sumOf { it.periode.antallHverdager().asInt }).isEqualTo(131)
    }

    @Test
    fun `sluttdatoberegning teller gamle migrerte og blandede perioder likt`() {
        val fom = LocalDate.of(2026, 1, 5)
        val (_, behandling) = opprettInMemorySakOgBehandling(fom)
        val første = oppfyltVurdering(fom, tom = fom.plusDays(4)).copy(
            fase = null,
            utviklingsPerioder = listOf(Periode(fom, fom.plusDays(4))),
            jobberBrukerAktivMedVirksomheten = null,
        ).toEtableringEgenVirksomhetVurdering(Bruker("saks"), behandling.id)
        val andre = første.copy(
            fom = fom.plusDays(7),
            tom = fom.plusDays(11),
            utviklingsPerioder = listOf(Periode(fom.plusDays(7), fom.plusDays(11))),
        )
        val gamle = listOf(første, andre)
        val ny = oppfyltVurdering(fom.plusDays(14))
            .toEtableringEgenVirksomhetVurdering(Bruker("saks"), behandling.id)
        val forventet = TilOgMedBeregning.Beregnet(
            Hverdager(EtableringFase.UTVIKLING.maksHverdager - 10).fraOgMed(ny.fom)
        )

        listOf(gamle, gamle.tilNyttFormat(), listOf(første) + listOf(andre).tilNyttFormat())
            .forEach { historikk ->
                assertThat(beregnTomForSistePeriode(historikk + ny, ny))
                    .usingRecursiveComparison().isEqualTo(forventet)
            }
    }

    @Test
    fun `sluttdatoberegning teller ikke overstyrte gamle perioder etter et opphold`() {
        val fom = LocalDate.of(2026, 1, 5)
        val (_, behandling) = opprettInMemorySakOgBehandling(fom)
        val eldre = oppfyltVurdering(fom).copy(
            fase = null,
            tom = null,
            utviklingsPerioder = listOf(Periode(fom, fom.plusDays(25))),
            jobberBrukerAktivMedVirksomheten = null,
        ).toEtableringEgenVirksomhetVurdering(Bruker("saks"), behandling.id)
        val nyere = eldre.copy(
            fom = fom.plusDays(7),
            utviklingsPerioder = listOf(Periode(fom.plusDays(7), fom.plusDays(11))),
        )
        val ny = oppfyltVurdering(fom.plusDays(28))
            .toEtableringEgenVirksomhetVurdering(Bruker("saks"), behandling.id)
        val forventet = TilOgMedBeregning.Beregnet(
            Hverdager(EtableringFase.UTVIKLING.maksHverdager - 10).fraOgMed(ny.fom)
        )

        assertThat(beregnTomForSistePeriode(listOf(eldre, nyere, ny), ny))
            .usingRecursiveComparison().isEqualTo(forventet)
    }

    @Test
    fun `legacy innsending bevarer alle utviklingsperioder og oppstartsperioder`() {
        val (sak, behandling) = opprettInMemorySakOgBehandling(LocalDate.now())
        oppfyllSykdomOgBistand(behandling)
        val fom = sak.rettighetsperiode.fom.plusDays(1)
        val utviklingsperioder = listOf(
            Periode(fom, fom.plusDays(4)),
            Periode(fom.plusDays(7), fom.plusDays(11)),
        )
        val oppstartsperioder = listOf(
            Periode(fom.plusDays(14), fom.plusDays(18)),
            Periode(fom.plusDays(21), fom.plusDays(25)),
        )
        val vurdering = oppfyltVurdering(fom, tom = fom.plusDays(25)).copy(
            fase = null,
            utviklingsPerioder = utviklingsperioder,
            oppstartsPerioder = oppstartsperioder,
            jobberBrukerAktivMedVirksomheten = null,
        )

        løser.løs(
            avklaringsbehovKontekst { this.behandling = behandling },
            EtableringEgenVirksomhetLøsning(listOf(vurdering)),
        )

        val lagret = requireNotNull(
            InMemoryEtableringEgenVirksomRepository.hentHvisEksisterer(behandling.id)
        ).vurderinger.single()
        assertThat(lagret.utviklingsPerioder).containsExactlyElementsOf(utviklingsperioder)
        assertThat(lagret.oppstartsPerioder).containsExactlyElementsOf(oppstartsperioder)
    }

    @Test
    fun `Legacy frontend uten fase skal fortsatt kunne vurdere virksomhet via oppstartsPerioder`() {
        val (sak, behandling) = opprettInMemorySakOgBehandling(LocalDate.now())
        oppfyllSykdomOgBistand(behandling)

        val fom = sak.rettighetsperiode.fom.plusDays(1)
        val tom = fom.plusDays(4)

        val kontekst = avklaringsbehovKontekst { this.behandling = behandling }
        val løsning = EtableringEgenVirksomhetLøsning(
            listOf(
                EtableringEgenVirksomhetLøsningDto(
                    begrunnelse = "legacy innsending",
                    fom = fom,
                    tom = tom,
                    virksomhetNavn = "peppas peppers",
                    orgNr = null,
                    foreliggerFagligVurdering = true,
                    virksomhetErNy = true,
                    brukerEierVirksomheten = EierVirksomhet.EIER_MINST_50_PROSENT,
                    kanFøreTilSelvforsørget = true,
                    jobberBrukerAktivMedVirksomheten = null,
                    fase = null,
                    erRegistrertINødvendigeOffentligeRegister = null,
                    oppstartsPerioder = listOf(Periode(fom, tom)),
                )
            )
        )

        assertDoesNotThrow { løser.løs(kontekst, løsning) }

        val lagretVurdering = requireNotNull(
            InMemoryEtableringEgenVirksomRepository.hentHvisEksisterer(behandling.id)
        ).vurderinger.single()

        assertThat(lagretVurdering.fase).isNull()
        assertThat(lagretVurdering.versjon).isEqualTo(EtableringVersjon.V0)
        assertThat(lagretVurdering.fom).isEqualTo(fom)
        assertThat(lagretVurdering.tom).isEqualTo(tom)
        assertThat(lagretVurdering.oppstartsPerioder).containsExactly(Periode(fom, tom))
        assertThat(lagretVurdering.erRegistrertINødvendigeOffentligeRegister).isNull()
        assertThat(lagretVurdering.jobberBrukerAktivMedVirksomheten).isNull()
        assertThat(lagretVurdering.evaluerVirksomhetVurdering()).isTrue()
    }

}
