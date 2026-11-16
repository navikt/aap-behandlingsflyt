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
import no.nav.aap.behandlingsflyt.test.november
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
        return EtableringEgenVirksomhetLøsningDto(
            versjon = EtableringVersjon.V1,
            begrunnelse = "meee",
            fom = fom,
            tom = tom,
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
    fun `krever svar om aktivt arbeid når grunnvilkårene er oppfylt`() {
        val (sak, behandling) = opprettInMemorySakOgBehandling(LocalDate.now())
        oppfyllSykdomOgBistand(behandling)
        val vurdering = oppfyltVurdering(sak.rettighetsperiode.fom.plusDays(1)).copy(
            versjon = EtableringVersjon.V1,
            jobberBrukerAktivMedVirksomheten = null,
        )

        val feil = assertThrows<UgyldigForespørselException> {
            løser.løs(
                avklaringsbehovKontekst { this.behandling = behandling },
                EtableringEgenVirksomhetLøsning(listOf(vurdering)),
            )
        }

        assertThat(feil.message).contains("Må oppgi om brukeren jobber aktivt med virksomheten.")
        assertThat(InMemoryEtableringEgenVirksomRepository.hentHvisEksisterer(behandling.id)).isNull()
    }

    @Test
    fun `krever fase når grunnvilkårene og aktivt arbeid er oppfylt`() {
        val (sak, behandling) = opprettInMemorySakOgBehandling(LocalDate.now())
        oppfyllSykdomOgBistand(behandling)
        val vurdering = oppfyltVurdering(sak.rettighetsperiode.fom.plusDays(1)).copy(
            versjon = EtableringVersjon.V1,
            fase = null,
        )

        val feil = assertThrows<UgyldigForespørselException> {
            løser.løs(
                avklaringsbehovKontekst { this.behandling = behandling },
                EtableringEgenVirksomhetLøsning(listOf(vurdering)),
            )
        }


        assertThat(feil.message).contains("Må ha definert minst én periode når vilkåret er oppfylt.")
        assertThat(InMemoryEtableringEgenVirksomRepository.hentHvisEksisterer(behandling.id)).isNull()
    }

    @Test
    fun `validering tillater ingen fase og nei på aktivt arbeid`() {
        val (sak, behandling) = opprettInMemorySakOgBehandling(LocalDate.now())
        oppfyllSykdomOgBistand(behandling)
        val vurdering = oppfyltVurdering(sak.rettighetsperiode.fom.plusDays(1)).copy(
            versjon = EtableringVersjon.V1,
            fase = null,
            jobberBrukerAktivMedVirksomheten = false,
        )

        løser.løs(
            avklaringsbehovKontekst { this.behandling = behandling },
            EtableringEgenVirksomhetLøsning(listOf(vurdering)),
        )

        val lagret = requireNotNull(
            InMemoryEtableringEgenVirksomRepository.hentHvisEksisterer(behandling.id)
        ).vurderinger.single()
        assertThat(lagret.versjon).isEqualTo(EtableringVersjon.V1)
        assertThat(lagret.evaluerVirksomhetVurdering()).isFalse()
    }

    @Test
    fun `ikke oppfylte grunnvilkår krever ikke svar om aktivt arbeid eller fase`() {
        val (sak, behandling) = opprettInMemorySakOgBehandling(LocalDate.now())
        oppfyllSykdomOgBistand(behandling)
        val vurdering = oppfyltVurdering(sak.rettighetsperiode.fom.plusDays(1)).copy(
            versjon = EtableringVersjon.V1,
            fase = null,
            virksomhetErNy = false,
            jobberBrukerAktivMedVirksomheten = null,
        )

        løser.løs(
            avklaringsbehovKontekst { this.behandling = behandling },
            EtableringEgenVirksomhetLøsning(listOf(vurdering)),
        )

        val lagret = requireNotNull(
            InMemoryEtableringEgenVirksomRepository.hentHvisEksisterer(behandling.id)
        ).vurderinger.single()
        assertThat(lagret.versjon).isEqualTo(EtableringVersjon.V1)
        assertThat(lagret.evaluerVirksomhetVurdering()).isFalse()
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
                    versjon = EtableringVersjon.V1,
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
    fun `revurdering uten nye vurderinger viderefører historikken nøyaktig én gang`() {
        val (_, forrige, revurdering) = opprettInMemorySakOgRevurdering(LocalDate.now())
        oppfyllSykdomOgBistand(revurdering)
        val fom = LocalDate.now().plusDays(1)
        val tidligere = oppfyltVurdering(fom, tom = fom.plusDays(4))
            .toEtableringEgenVirksomhetVurdering(Bruker("saks"), forrige.id)
        InMemoryEtableringEgenVirksomRepository.lagre(forrige.id, listOf(tidligere))

        løser.løs(
            avklaringsbehovKontekst { this.behandling = revurdering },
            EtableringEgenVirksomhetLøsning(emptyList()),
        )

        val lagret = requireNotNull(
            InMemoryEtableringEgenVirksomRepository.hentHvisEksisterer(revurdering.id)
        ).vurderinger
        assertThat(lagret).usingRecursiveComparison().isEqualTo(listOf(tidligere))
    }

    @Test
    fun `oppstart endrer ikke lagrede datoer i utviklingsvurdering fra tidligere behandling`() {
        val idag = LocalDate.now()
        val (_, forrige, revurdering) = opprettInMemorySakOgRevurdering(idag)
        oppfyllSykdomOgBistand(revurdering)
        val fom = idag.plusDays(1)
        val tidligere = oppfyltVurdering(fom, tom = fom.plusDays(25))
            .toEtableringEgenVirksomhetVurdering(Bruker("saks"), forrige.id)
        InMemoryEtableringEgenVirksomRepository.lagre(forrige.id, listOf(tidligere))
        val oppstart = oppfyltVurdering(
            fom.plusDays(14),
            fase = EtableringFase.OPPSTART,
            erRegistrertINødvendigeOffentligeRegister = true,
            tom = null
        )

        løser.løs(
            avklaringsbehovKontekst { this.behandling = revurdering },
            EtableringEgenVirksomhetLøsning(listOf(oppstart)),
        )

        val historikk = requireNotNull(
            InMemoryEtableringEgenVirksomRepository.hentHvisEksisterer(revurdering.id)
        ).vurderinger.filter { it.vurdertIBehandling == forrige.id }
        assertThat(historikk).usingRecursiveComparison().isEqualTo(listOf(tidligere))
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

        assertThat(lagredeVurderinger).hasSize(2)

        val gjeldendeVurderinger = lagredeVurderinger.gjeldendeVurderinger()
        assertThat(gjeldendeVurderinger.segmenter()).hasSize(1)
        val gjeldendeVurdering = gjeldendeVurderinger.segmenter().single().verdi
        assertThat(gjeldendeVurdering.fase).isEqualTo(EtableringFase.OPPSTART)
        assertThat(gjeldendeVurdering.fom).isEqualTo(vurderingFom)
        // tom beregnes av beregnTomForSistePeriode ut fra antall hverdager, ikke kalendermåneder.
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
                    versjon = EtableringVersjon.V1,
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
                    versjon = EtableringVersjon.V1,
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

        // 20 dager
        val eldreTom = førsteFom.plusDays(20)
        val eldre = oppfyltVurdering(førsteFom, tom = eldreTom)
            .toEtableringEgenVirksomhetVurdering(Bruker("saks"), førsteBehandling.id)

        // 6 ekstra dager
        val nyere = eldre.copy(
            vurdertIBehandling = revurdering.id,
            tom = førsteFom.plusDays(6),
        )

        val nesteFom = førsteFom.plusDays(28)
        val siste = nyere.copy(fom = nesteFom, tom = null)

        // Vil beregne sluttdato for 'siste', uten å telle med eldre/nyere dobbelt (siden de
        // overlapper.
        val resultat = beregnTomForSistePeriode(
            vurderinger = listOf(eldre, nyere, siste),
            sisteVurdering = siste
        )

        val maksHverdager = EtableringFase.UTVIKLING.maksHverdager
        val brukteDager = Periode(førsteFom, eldreTom).antallHverdager().asInt

        // Sjekker at siste sin sluttdato er maks-kvote minus antall hverager fra de to første
        // vurderingene.
        assertThat(resultat).isEqualTo(
            TilOgMedBeregning.Beregnet(Hverdager(maksHverdager - brukteDager).fraOgMed(nesteFom))
        )
    }

    @Test
    fun `kvoteberegning forlenger ikke avsluttede historiske perioder over opphold`() {
        val søknadsdato = 12 november 2026
        val (_, behandling) = opprettInMemorySakOgBehandling(søknadsdato)

        val førsteFom = søknadsdato.plusDays(1)
        val tidligere = oppfyltVurdering(førsteFom, tom = førsteFom.plusDays(6))
            .toEtableringEgenVirksomhetVurdering(Bruker("saks"), behandling.id)

        val siste = tidligere.copy(fom = førsteFom.plusDays(28), tom = null)

        val resultat = beregnTomForSistePeriode(listOf(tidligere, siste), siste)

        val maksHverdager = EtableringFase.UTVIKLING.maksHverdager

        // 5 hverdager etter førsteFom til førsteFom.plusDays(6)
        val hverdagerBrukt = 5
        assertThat(resultat).isEqualTo(
            TilOgMedBeregning.Beregnet(Hverdager(maksHverdager - hverdagerBrukt).fraOgMed(siste.fom))
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

        val førsteVurdering = vurderinger.single { it.fom == førsteFom }
        val andreVurdering = vurderinger.single { it.fom == andreFom }

        assertThat(førsteVurdering.tom).isEqualTo(andreFom.minusDays(1))
        assertThat(andreVurdering.tom).isEqualTo(forventetTom)

        assertThat(segmenter.map { it.periode }).containsExactly(
            Periode(førsteFom, andreFom.minusDays(1)),
            Periode(andreFom, forventetTom),
        )
        val maksHverdager = EtableringFase.UTVIKLING.maksHverdager
        assertThat(segmenter.sumOf { it.periode.antallHverdager().asInt }).isEqualTo(maksHverdager)
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
                    oppfyltVurdering(utviklingFom, tom = null),
                    oppfyltVurdering(
                        oppstartFom,
                        fase = EtableringFase.OPPSTART,
                        erRegistrertINødvendigeOffentligeRegister = true,
                        tom = null
                    ),
                    // Ikke oppfylt vurdering
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
        // Beregner slutt-dato for oppstart-fase

        assertThat(segmenter.filter { it.verdi.fase == EtableringFase.OPPSTART }
            .sumOf { it.periode.antallHverdager().asInt }).isEqualTo(EtableringFase.OPPSTART.maksHverdager)
    }

    @Test
    fun `ikke oppfylt vurdering stopper kvoteforbruket`() {
        val (sak, behandling) = opprettInMemorySakOgBehandling(LocalDate.now())
        oppfyllSykdomOgBistand(behandling)
        val førsteFom = sak.rettighetsperiode.fom.plusDays(1)
        val avslagFom = førsteFom.plusDays(7)
        val andreFom = førsteFom.plusDays(14)

        løser.løs(
            avklaringsbehovKontekst { this.behandling = behandling },
            EtableringEgenVirksomhetLøsning(
                listOf(
                    oppfyltVurdering(førsteFom),
                    // Ikke oppfylt vurdering
                    oppfyltVurdering(avslagFom).copy(
                        fase = null,
                        virksomhetErNy = false,
                    ),
                    oppfyltVurdering(andreFom),
                )
            )
        )

        val vurderinger = requireNotNull(
            InMemoryEtableringEgenVirksomRepository.hentHvisEksisterer(behandling.id)
        ).vurderinger
        val brukteDager = Periode(førsteFom, avslagFom.minusDays(1)).antallHverdager().asInt
        val maksHverager = EtableringFase.UTVIKLING.maksHverdager
        assertThat(vurderinger.single { it.fom == andreFom }.tom)
            .isEqualTo(Hverdager(maksHverager - brukteDager).fraOgMed(andreFom))
        assertThat(
            vurderinger.gjeldendeVurderinger().segmenter()
                .filter { it.verdi.fase == EtableringFase.UTVIKLING }
                .sumOf { it.periode.antallHverdager().asInt }).isEqualTo(maksHverager)
    }
}
