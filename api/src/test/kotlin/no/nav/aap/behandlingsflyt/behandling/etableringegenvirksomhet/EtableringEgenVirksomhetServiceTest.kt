package no.nav.aap.behandlingsflyt.behandling.etableringegenvirksomhet

import no.nav.aap.bistandsbehov.Bistandsvurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.ArbeidsevneNedsattValg
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.Sykdomsvurdering
import no.nav.aap.behandlingsflyt.help.opprettInMemorySakOgBehandling
import no.nav.aap.behandlingsflyt.help.opprettInMemorySakOgRevurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.etableringegenvirksomhet.EierVirksomhet
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.etableringegenvirksomhet.EtableringEgenVirksomhetVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.etableringegenvirksomhet.avsluttUtviklingVedFørsteOppstart
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.etableringegenvirksomhet.EtableringFase
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.etableringegenvirksomhet.EtableringVersjon
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.etableringegenvirksomhet.perioderForFase
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.etableringegenvirksomhet.somFasetidslinje
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.etableringegenvirksomhet.tellDager
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.etableringegenvirksomhet.tilNyttFormat
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.test.januar
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryBehandlingRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryBistandRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryEtableringEgenVirksomRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemorySykdomRepository
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Bruker
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate

class EtableringEgenVirksomhetServiceTest {
    @Test
    fun `hjelpemetoden avkorter utvikling med og uten sluttdato`() {
        val utvikling = fasevurdering(EtableringFase.UTVIKLING, 2 januar 2026, 31 januar 2026)

        listOf(utvikling, utvikling.copy(tom = null)).forEach { vurdering ->
            assertThat(avsluttUtviklingVedFørsteOppstart(vurdering, 10 januar 2026))
                .isEqualTo(vurdering.copy(tom = 9 januar 2026))
        }
    }

    @Test
    fun `hjelpemetoden bevarer vurderinger som ikke skal avkortes`() {
        val utvikling = fasevurdering(EtableringFase.UTVIKLING, 2 januar 2026, 31 januar 2026)

        assertThat(avsluttUtviklingVedFørsteOppstart(utvikling, null)).isEqualTo(utvikling)
        listOf(
            utvikling.copy(tom = 5 januar 2026),
            utvikling.copy(fom = 10 januar 2026),
            utvikling.copy(fom = 11 januar 2026),
            utvikling.copy(fase = EtableringFase.OPPSTART),
            utvikling.copy(fase = null),
        ).forEach { vurdering ->
            assertThat(avsluttUtviklingVedFørsteOppstart(vurdering, 10 januar 2026))
                .isEqualTo(vurdering)
        }
    }

    @Test
    fun `avkorting uten oppstart endrer ikke vurderingene`() {
        val utvikling = fasevurdering(EtableringFase.UTVIKLING, 2 januar 2026, 31 januar 2026)

        assertThat(listOf(utvikling).somFasetidslinje().perioderForFase(EtableringFase.UTVIKLING))
            .containsExactly(Periode(2 januar 2026, 31 januar 2026))
    }

    @Test
    fun `avslutter utvikling til dagen før første oppstart uten å endre originalen`() {
        val utvikling = fasevurdering(EtableringFase.UTVIKLING, 2 januar 2026, 31 januar 2026)
        val oppstart = fasevurdering(EtableringFase.OPPSTART, 10 januar 2026, 20 januar 2026)

        val tidslinje = listOf(utvikling, oppstart).somFasetidslinje()

        assertThat(tidslinje.perioderForFase(EtableringFase.UTVIKLING))
            .containsExactly(Periode(2 januar 2026, 9 januar 2026))
        assertThat(tidslinje.perioderForFase(EtableringFase.OPPSTART))
            .containsExactly(Periode(10 januar 2026, 20 januar 2026))
    }

    @Test
    fun `avslutter utvikling uten sluttdato til dagen før oppstart`() {
        val utvikling = fasevurdering(EtableringFase.UTVIKLING, 2 januar 2026, null)
        val oppstart = fasevurdering(EtableringFase.OPPSTART, 10 januar 2026, 20 januar 2026)

        assertThat(listOf(utvikling, oppstart).somFasetidslinje().perioderForFase(EtableringFase.UTVIKLING))
            .containsExactly(Periode(2 januar 2026, 9 januar 2026))
    }

    @Test
    fun `forlenger ikke utvikling som slutter før oppstart`() {
        val utvikling = fasevurdering(EtableringFase.UTVIKLING, 2 januar 2026, 5 januar 2026)
        val oppstart = fasevurdering(EtableringFase.OPPSTART, 10 januar 2026, 20 januar 2026)

        assertThat(listOf(utvikling, oppstart).somFasetidslinje().perioderForFase(EtableringFase.UTVIKLING))
            .containsExactly(Periode(2 januar 2026, 5 januar 2026))
    }

    @Test
    fun `bruker første oppstart uavhengig av rekkefølgen i listen`() {
        val utvikling = fasevurdering(EtableringFase.UTVIKLING, 2 januar 2026, 31 januar 2026)
        val førsteOppstart = fasevurdering(EtableringFase.OPPSTART, 10 januar 2026, 15 januar 2026)
        val sisteOppstart = fasevurdering(EtableringFase.OPPSTART, 20 januar 2026, 25 januar 2026)

        val tidslinje = listOf(sisteOppstart, utvikling, førsteOppstart).somFasetidslinje()

        assertThat(tidslinje.perioderForFase(EtableringFase.UTVIKLING))
            .containsExactly(Periode(2 januar 2026, 9 januar 2026))
    }

    @Test
    fun `arvet utvikling avsluttes bare i tidslinjen og gjenoppstår ikke etter oppstart`() {
        val utvikling = fasevurdering(EtableringFase.UTVIKLING, 2 januar 2026, 31 januar 2026)
        val oppstart = fasevurdering(EtableringFase.OPPSTART, 10 januar 2026, 20 januar 2026)
            .copy(vurdertIBehandling = BehandlingId(2L), opprettet = Instant.parse("2026-01-02T00:00:00Z"))
        val legacyUtvikling = utvikling.copy(
            fase = null,
            versjon = EtableringVersjon.V0,
            utviklingsPerioder = listOf(Periode(2 januar 2026, 31 januar 2026)),
        )

        listOf(utvikling, legacyUtvikling).forEach { arvet ->
            val vurderinger = listOf(arvet, oppstart)
            val tidslinje = vurderinger.somFasetidslinje()

            assertThat(tidslinje.perioderForFase(EtableringFase.UTVIKLING))
                .containsExactly(Periode(2 januar 2026, 9 januar 2026))
            assertThat(tidslinje.perioderForFase(EtableringFase.OPPSTART))
                .containsExactly(Periode(10 januar 2026, 20 januar 2026))
            assertThat(arvet.tom).isEqualTo(31 januar 2026)
        }
    }

    @Test
    fun `overstyrt oppstart avslutter ikke utvikling`() {
        val utvikling = fasevurdering(EtableringFase.UTVIKLING, 2 januar 2026, 31 januar 2026)
        val oppstart = fasevurdering(EtableringFase.OPPSTART, 10 januar 2026, 20 januar 2026)
        val overstyring = oppstart.copy(
            fase = null,
            jobberBrukerAktivMedVirksomheten = false,
            vurdertIBehandling = BehandlingId(2L),
            opprettet = Instant.parse("2026-01-02T00:00:00Z"),
        )

        val tidslinje = listOf(utvikling, oppstart, overstyring).somFasetidslinje()

        assertThat(tidslinje.perioderForFase(EtableringFase.OPPSTART)).isEmpty()
        assertThat(tidslinje.perioderForFase(EtableringFase.UTVIKLING))
            .containsExactly(Periode(2 januar 2026, 9 januar 2026), Periode(21 januar 2026, 31 januar 2026))
    }

    @Test
    fun `ny utvikling etter oppstart beholdes og avvises av valideringen`() {
        val (_, behandling) = opprettInMemorySakOgBehandling()
        oppfyllSykdomOgBistand(behandling)
        val fom = LocalDate.now().plusDays(1)
        val oppstart = fasevurdering(EtableringFase.OPPSTART, fom, fom.plusDays(5))
            .copy(vurdertIBehandling = behandling.id)
        val utvikling = fasevurdering(EtableringFase.UTVIKLING, fom.plusDays(6), fom.plusDays(10))
            .copy(vurdertIBehandling = behandling.id)
        val vurderinger = listOf(oppstart, utvikling)
        val service = EtableringEgenVirksomhetService(
            InMemoryEtableringEgenVirksomRepository,
            InMemoryBehandlingRepository,
            InMemoryBistandRepository,
            InMemorySykdomRepository,
        )

        assertThat(vurderinger.somFasetidslinje().perioderForFase(EtableringFase.UTVIKLING))
            .containsExactly(Periode(utvikling.fom, requireNotNull(utvikling.tom)))
        assertThat(service.validerVurdering(behandling.id, vurderinger))
            .isEqualTo(VirksomhetEtableringResultat.Ugyldig.OppstartFørUtvikling)
    }

    @Test
    fun `kvoter inkluderer migrerte vurderinger fra forrige behandling`() {
        val (_, forrige, behandling) = opprettInMemorySakOgRevurdering()
        oppfyllSykdomOgBistand(behandling)
        val periode = Periode(LocalDate.now().plusDays(1), LocalDate.now().plusDays(200))
        val gammel = EtableringEgenVirksomhetVurdering(
            begrunnelse = "Historisk vurdering",
            virksomhetNavn = "Virksomhet",
            foreliggerFagligVurdering = true,
            virksomhetErNy = true,
            brukerEierVirksomheten = EierVirksomhet.EIER_MINST_50_PROSENT,
            kanFøreTilSelvforsørget = true,
            utviklingsPerioder = listOf(periode),
            oppstartsPerioder = emptyList(),
            vurdertAv = Bruker("Z123456"),
            opprettet = Instant.now(),
            vurdertIBehandling = forrige.id,
            fom = periode.fom,
            tom = periode.tom,
            fase = null,
            erRegistrertINødvendigeOffentligeRegister = null,
            jobberBrukerAktivMedVirksomheten = null,
            versjon = EtableringVersjon.V0,
        )
        val service = EtableringEgenVirksomhetService(
            InMemoryEtableringEgenVirksomRepository,
            InMemoryBehandlingRepository,
            InMemoryBistandRepository,
            InMemorySykdomRepository,
        )
        val forventet = VirksomhetEtableringResultat.Ugyldig.OverstigerKvoter(
            EtableringFase.UTVIKLING, listOf(gammel).tellDager { it.utviklingsPerioder }
        )
        assertThat(forventet.brukteDager).isGreaterThan(131)

        listOf(listOf(gammel), listOf(gammel).tilNyttFormat()).forEach { vurderinger ->
            InMemoryEtableringEgenVirksomRepository.lagre(forrige.id, vurderinger)
            assertThat(service.beregnOgValider(behandling.id, emptyList())).isEqualTo(forventet)
        }
    }

    @Test
    fun `tom liste av vurderinger gyldig`() {
        val (_, behandling) = opprettInMemorySakOgBehandling(LocalDate.now())
        oppfyllSykdomOgBistand(behandling)

        val service = EtableringEgenVirksomhetService(
            InMemoryEtableringEgenVirksomRepository,
            InMemoryBehandlingRepository,
            InMemoryBistandRepository,
            InMemorySykdomRepository
        )

        val res = service.beregnOgValider(behandling.id, emptyList())

        assertThat(res).isInstanceOf(VirksomhetEtableringResultat.Gyldig::class.java)
    }

    @Test
    fun `utleder ikke vurderbare perioder`() {
        val (_, behandling) = opprettInMemorySakOgBehandling(LocalDate.of(2026, 1, 1))

        InMemorySykdomRepository.lagre(
            behandling.id, listOf(
                Sykdomsvurdering(
                    begrunnelse = "...",
                    vurderingenGjelderFra = LocalDate.of(2026, 1, 1),
                    vurderingenGjelderTil = LocalDate.of(2026, 1, 6),
                    harSkadeSykdomEllerLyte = true,
                    erSkadeSykdomEllerLyteVesentligdel = true,
                    erNedsettelseIArbeidsevneMerEnnHalvparten = true,
                    erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense = false,
                    yrkesskadeBegrunnelse = null,
                    harNedsattArbeidsevne = ArbeidsevneNedsattValg.JA,
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
                    erBehovForAktivBehandling = false,
                    erBehovForArbeidsrettetTiltak = true,
                    erBehovForAnnenOppfølging = false,
                    overgangBegrunnelse = null,
                    skalVurdereAapIOvergangTilArbeid = false,
                    vurdertAv = Bruker("saks"),
                    fom = LocalDate.of(2026, 1, 1),
                    tom = LocalDate.of(2026, 1, 3),
                    opprettet = Instant.now(),
                    vurdertIBehandling = behandling.id
                ),
                Bistandsvurdering(
                    begrunnelse = "...",
                    erBehovForAktivBehandling = false,
                    erBehovForArbeidsrettetTiltak = false,
                    erBehovForAnnenOppfølging = false,
                    overgangBegrunnelse = null,
                    skalVurdereAapIOvergangTilArbeid = false,
                    vurdertAv = Bruker("saks"),
                    fom = LocalDate.of(2026, 1, 4),
                    tom = LocalDate.of(2026, 1, 6),
                    opprettet = Instant.now(),
                    vurdertIBehandling = behandling.id
                )
            )
        )

        val service = EtableringEgenVirksomhetService(
            InMemoryEtableringEgenVirksomRepository,
            InMemoryBehandlingRepository,
            InMemoryBistandRepository,
            InMemorySykdomRepository
        )

        val ikkeVurderbarePerioder = service.utledIkkeVurderbarePerioder(behandling.id)

        assertThat(ikkeVurderbarePerioder).containsExactly(
            Periode(LocalDate.of(2026, 1, 4), LocalDate.of(2026, 1, 6)),
            Periode(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 1))
        )
    }

    private fun fasevurdering(
        fase: EtableringFase,
        fom: LocalDate,
        tom: LocalDate?
    ) = EtableringEgenVirksomhetVurdering(
        begrunnelse = "Vurdering av fase",
        virksomhetNavn = "Virksomhet",
        foreliggerFagligVurdering = true,
        virksomhetErNy = true,
        brukerEierVirksomheten = EierVirksomhet.EIER_MINST_50_PROSENT,
        kanFøreTilSelvforsørget = true,
        vurdertAv = Bruker("saks"),
        opprettet = Instant.parse("2026-01-01T00:00:00Z"),
        vurdertIBehandling = BehandlingId(1L),
        fom = fom,
        tom = tom,
        fase = fase,
        erRegistrertINødvendigeOffentligeRegister = fase == EtableringFase.OPPSTART,
        jobberBrukerAktivMedVirksomheten = true,
        versjon = EtableringVersjon.V1,
    )

    private fun oppfyllSykdomOgBistand(behandling: Behandling) {
        InMemorySykdomRepository.lagre(
            behandling.id, listOf(
                Sykdomsvurdering(
                    begrunnelse = "...",
                    vurderingenGjelderFra = LocalDate.now(),
                    vurderingenGjelderTil = LocalDate.now().plusMonths(6),
                    harSkadeSykdomEllerLyte = true,
                    erSkadeSykdomEllerLyteVesentligdel = true,
                    erNedsettelseIArbeidsevneMerEnnHalvparten = true,
                    erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense = false,
                    yrkesskadeBegrunnelse = null,
                    harNedsattArbeidsevne = ArbeidsevneNedsattValg.JA,
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