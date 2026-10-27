package no.nav.aap.behandlingsflyt

import no.nav.aap.behandlingsflyt.behandling.ansattinfo.AnsattInfoService
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.EtableringEgenVirksomhetLøser
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.EtableringEgenVirksomhetLøsning
import no.nav.aap.behandlingsflyt.behandling.etableringegenvirksomhet.EtableringEgenVirksomhetService
import no.nav.aap.behandlingsflyt.behandling.etableringegenvirksomhet.EtableringEgenVirksomhetVurderingResponse
import no.nav.aap.behandlingsflyt.behandling.etableringegenvirksomhet.VirksomhetEtableringResultat
import no.nav.aap.behandlingsflyt.behandling.vurdering.VurdertAvService
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.etableringegenvirksomhet.EierVirksomhet
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.etableringegenvirksomhet.EtableringEgenVirksomhetLøsningDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.etableringegenvirksomhet.EtableringEgenVirksomhetRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.etableringegenvirksomhet.EtableringFase
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.etableringegenvirksomhet.EtableringVersjon
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.etableringegenvirksomhet.tilNyttFormat
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.etableringegenvirksomhet.somFasetidslinje
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.etableringegenvirksomhet.erFunksjoneltLik
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.Hverdager.Companion.antallHverdager
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.gjeldendeVurderinger
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.ArbeidsevneNedsattValg
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykdomRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.Sykdomsvurdering
import no.nav.aap.behandlingsflyt.help.avklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.help.finnEllerOpprettBehandling
import no.nav.aap.behandlingsflyt.help.opprettInMemorySakOgBehandling
import no.nav.aap.behandlingsflyt.help.opprettInMemorySakOgRevurdering
import no.nav.aap.behandlingsflyt.help.sak
import no.nav.aap.behandlingsflyt.repository.postgresRepositoryRegistry
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.test.FakeAnsattInfoGateway
import no.nav.aap.behandlingsflyt.test.FakeEnhetGateway
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryAvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryBehandlingRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryBistandRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryEtableringEgenVirksomRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemorySykdomRepository
import no.nav.aap.bistandsbehov.BistandRepository
import no.nav.aap.bistandsbehov.Bistandsvurdering
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.TestDataSource
import no.nav.aap.komponenter.httpklient.exception.UgyldigForespørselException
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Bruker
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Instant
import java.time.LocalDate

class Pr3599ReviewTest {
    private val service = EtableringEgenVirksomhetService(
        InMemoryEtableringEgenVirksomRepository,
        InMemoryBehandlingRepository,
        InMemoryBistandRepository,
        InMemorySykdomRepository,
    )
    private val løser = EtableringEgenVirksomhetLøser(
        InMemoryEtableringEgenVirksomRepository, InMemoryBehandlingRepository, service,
    )
    private val start = LocalDate.of(2026, 1, 5)

    @Test
    fun `legacy overrides retain their full validity after saving mixed history`() {
        val (_, forrige, behandling) = opprettInMemorySakOgRevurdering(start.minusDays(4))
        oppfyllSykdomOgBistand(behandling.id)
        val tidligere = dto(tom = start.plusDays(100))
            .toEtableringEgenVirksomhetVurdering(Bruker("saks"), forrige.id)
        InMemoryEtableringEgenVirksomRepository.lagre(forrige.id, listOf(tidligere))
        val nyPeriode = Periode(start.plusDays(14), start.plusDays(18))
        val input = dto(fom = nyPeriode.fom, fase = null).copy(
            utviklingsPerioder = listOf(nyPeriode),
            jobberBrukerAktivMedVirksomheten = null,
        )
        løser.løs(
            avklaringsbehovKontekst { this.behandling = behandling },
            EtableringEgenVirksomhetLøsning(listOf(input)),
        )
        val grunnlag = requireNotNull(
            InMemoryEtableringEgenVirksomRepository.hentHvisEksisterer(behandling.id)
        )
        assertThat(grunnlag.vurderinger).hasSize(2)
        assertThat(grunnlag.vurderinger.first()).isEqualTo(tidligere)
        assertThat(grunnlag.vurderinger.last().tom).isNull()
        assertThat(grunnlag.gjeldendeVurderingerSomTidslinje().segmenter().map { it.periode })
            .containsExactly(Periode(start, nyPeriode.fom.minusDays(1)), nyPeriode)
    }

    @Test
    fun `positive legacy input must include at least one assessed period`() {
        val (_, behandling) = opprettInMemorySakOgBehandling(start.minusDays(4))
        oppfyllSykdomOgBistand(behandling.id)
        val input = dto(fase = null).copy(utviklingsPerioder = emptyList(), oppstartsPerioder = emptyList())
        val feil = assertThrows<UgyldigForespørselException> {
            løser.løs(
                avklaringsbehovKontekst { this.behandling = behandling },
                EtableringEgenVirksomhetLøsning(listOf(input)),
            )
        }
        assertThat(feil.message).contains("minst én periode")
    }

    @Test
    fun `changing legacy periods is a functional change`() {
        val (_, behandling) = opprettInMemorySakOgBehandling(start.minusDays(4))
        val før = dto(fase = null, tom = start.plusDays(30))
            .copy(utviklingsPerioder = listOf(Periode(start, start.plusDays(4))))
            .toEtableringEgenVirksomhetVurdering(Bruker("saks"), behandling.id)
        val etter = før.copy(utviklingsPerioder = listOf(Periode(start, start.plusDays(11))))
        assertThat(listOf(før).erFunksjoneltLik(listOf(etter))).isFalse()
    }

    @Test
    fun `new phase input with a finite end still requires an active work answer`() {
        val (_, behandling) = opprettInMemorySakOgBehandling(start.minusDays(4))
        oppfyllSykdomOgBistand(behandling.id)
        val input = dto(tom = start.plusDays(4)).copy(jobberBrukerAktivMedVirksomheten = null)
        val feil = assertThrows<UgyldigForespørselException> {
            løser.løs(
                avklaringsbehovKontekst { this.behandling = behandling },
                EtableringEgenVirksomhetLøsning(listOf(input)),
            )
        }
        assertThat(feil.message).contains("Må oppgi om brukeren jobber aktivt")
        assertThat(InMemoryEtableringEgenVirksomRepository.hentHvisEksisterer(behandling.id)).isNull()
    }

    @Test
    fun `new input without a phase is not classified as historical`() {
        val (_, behandling) = opprettInMemorySakOgBehandling(start.minusDays(4))
        val vurdering = dto(fase = null).copy(virksomhetErNy = false)
            .toEtableringEgenVirksomhetVurdering(Bruker("saks"), behandling.id)
        assertThat(vurdering.versjon).isEqualTo(EtableringVersjon.V1)
    }

    @Test
    fun `database migration commits when both validation results are successful`() {
        TestDataSource().use { dataSource ->
            val (behandlingId, forventet) = dataSource.transaction { connection ->
                val behandlingId = finnEllerOpprettBehandling(connection, sak(connection)).id
                oppfyllSykdomOgBistand(behandlingId)
                val provider = postgresRepositoryRegistry.provider(connection)
                provider.provide<SykdomRepository>().lagre(
                    behandlingId,
                    requireNotNull(InMemorySykdomRepository.hentHvisEksisterer(behandlingId)).sykdomsvurderinger,
                )
                provider.provide<BistandRepository>().lagre(
                    behandlingId,
                    requireNotNull(InMemoryBistandRepository.hentHvisEksisterer(behandlingId)).vurderinger,
                )
                val gammel = dto(fase = null)
                    .toEtableringEgenVirksomhetVurdering(Bruker("saks"), behandlingId)
                    .copy(
                        utviklingsPerioder = listOf(Periode(start, start.plusDays(4))),
                        versjon = EtableringVersjon.V0,
                    )
                provider.provide<EtableringEgenVirksomhetRepository>().lagre(behandlingId, listOf(gammel))
                val dbService = EtableringEgenVirksomhetService(provider)
                val konverterte = listOf(gammel).tilNyttFormat()
                assertThat(dbService.beregnOgValider(behandlingId, listOf(gammel)))
                    .isInstanceOf(VirksomhetEtableringResultat.Gyldig::class.java)
                assertThat(dbService.beregnOgValider(behandlingId, konverterte))
                    .isInstanceOf(VirksomhetEtableringResultat.Gyldig::class.java)
                behandlingId to konverterte
            }
            val backfill = BackfillEtableringVurderinger(dataSource, dryRun = false)
            repeat(2) {
                dataSource.transaction { backfill.migrerBehandling(it, behandlingId) }
                dataSource.transaction { connection ->
                    val lagret = postgresRepositoryRegistry.provider(connection)
                        .provide<EtableringEgenVirksomhetRepository>()
                        .hentHvisEksisterer(behandlingId)?.vurderinger
                    assertThat(lagret).usingRecursiveComparison()
                        .ignoringFields("opprettet").isEqualTo(forventet)
                }
            }
        }
    }

    @Test
    fun `unmigrated stored assessments retain their periods and consume quota`() {
        val (_, forrige, behandling) = opprettInMemorySakOgRevurdering(start.minusDays(4))
        oppfyllSykdomOgBistand(behandling.id)
        val periode = Periode(start, start.plusDays(200))
        val gammel = dto(fase = null, tom = periode.tom)
            .toEtableringEgenVirksomhetVurdering(Bruker("saks"), forrige.id)
            .copy(utviklingsPerioder = listOf(periode), versjon = EtableringVersjon.V0)
        InMemoryEtableringEgenVirksomRepository.lagre(forrige.id, listOf(gammel))
        val response = EtableringEgenVirksomhetVurderingResponse.fraDomene(
            gammel,
            VurdertAvService(
                InMemoryAvklaringsbehovRepository,
                AnsattInfoService(FakeAnsattInfoGateway(), FakeEnhetGateway()),
            )
        )
        assertThat(response.utviklingsPeriode).containsExactly(periode)
        assertThat(service.beregnOgValider(behandling.id, emptyList())).isEqualTo(
            VirksomhetEtableringResultat.Ugyldig.OverstigerKvoter(
                EtableringFase.UTVIKLING, periode.antallHverdager().asInt
            )
        )
    }

    @Test
    fun `fully superseded startup does not block a correction to development`() {
        val (_, forrige, behandling) = opprettInMemorySakOgRevurdering(start.minusDays(4))
        oppfyllSykdomOgBistand(behandling.id)
        val tidligere = dto(tom = start.plusDays(4), fase = EtableringFase.OPPSTART)
            .toEtableringEgenVirksomhetVurdering(Bruker("saks"), forrige.id)
        InMemoryEtableringEgenVirksomRepository.lagre(forrige.id, listOf(tidligere))
        val ny = dto().toEtableringEgenVirksomhetVurdering(Bruker("saks"), behandling.id)
        assertThat(service.beregnOgValider(behandling.id, listOf(ny)))
            .isInstanceOf(VirksomhetEtableringResultat.Gyldig::class.java)
    }

    @Test
    fun `ordinary development followed by startup is accepted without artificial rejection tail`() {
        val (_, behandling) = opprettInMemorySakOgBehandling(start.minusDays(4))
        oppfyllSykdomOgBistand(behandling.id)
        val vurderinger = listOf(dto(), dto(start.plusDays(14), fase = EtableringFase.OPPSTART))
            .map { it.toEtableringEgenVirksomhetVurdering(Bruker("saks"), behandling.id) }
        val resultat = service.beregnOgValider(behandling.id, vurderinger)
        assertThat(resultat).isInstanceOf(VirksomhetEtableringResultat.Gyldig::class.java)
        val beregning = (resultat as VirksomhetEtableringResultat.Gyldig).beregning
        assertThat(beregning.beregnedeVurderinger.first().tom).isEqualTo(start.plusDays(13))
        assertThat(beregning.beregnedeVurderinger.somFasetidslinje().segmenter().map { it.verdi.fase })
            .containsExactly(EtableringFase.UTVIKLING, EtableringFase.OPPSTART)
    }

    @Test
    fun `legacy payload preserves every development and startup period`() {
        val (_, behandling) = opprettInMemorySakOgBehandling(start.minusDays(4))
        oppfyllSykdomOgBistand(behandling.id)
        val utvikling = listOf(
            Periode(start, start.plusDays(4)),
            Periode(start.plusDays(14), start.plusDays(18)),
        )
        val oppstart = listOf(Periode(start.plusDays(28), start.plusDays(32)))
        løser.løs(
            avklaringsbehovKontekst { this.behandling = behandling },
            EtableringEgenVirksomhetLøsning(
                listOf(dto().copy(fase = null, utviklingsPerioder = utvikling, oppstartsPerioder = oppstart))
            ),
        )
        val lagret = requireNotNull(
            InMemoryEtableringEgenVirksomRepository.hentHvisEksisterer(behandling.id)
        ).vurderinger
        assertThat(lagret.single().utviklingsPerioder).containsExactlyElementsOf(utvikling)
        assertThat(lagret.single().oppstartsPerioder).containsExactlyElementsOf(oppstart)
        val faser = lagret.somFasetidslinje().segmenter()
        assertThat(faser).hasSize(3)
        assertThat(faser.map { it.periode })
            .containsExactlyInAnyOrderElementsOf(utvikling + oppstart)
        assertThat(faser.map { it.verdi.fase })
            .containsExactly(EtableringFase.UTVIKLING, EtableringFase.UTVIKLING, EtableringFase.OPPSTART)
    }

    @Test
    fun `legacy submission keeps the explicitly assessed end date`() {
        val (_, behandling) = opprettInMemorySakOgBehandling(start.minusDays(4))
        oppfyllSykdomOgBistand(behandling.id)
        val tom = start.plusDays(4)
        løser.løs(
            avklaringsbehovKontekst { this.behandling = behandling },
            EtableringEgenVirksomhetLøsning(
                listOf(dto(tom = tom).copy(fase = null, utviklingsPerioder = listOf(Periode(start, tom))))
            ),
        )
        val lagret = requireNotNull(
            InMemoryEtableringEgenVirksomRepository.hentHvisEksisterer(behandling.id)
        ).vurderinger.single()
        assertThat(lagret.tom).isEqualTo(tom)
        assertThat(lagret.versjon).isEqualTo(EtableringVersjon.V0)
    }

    @Test
    fun `API phase periods use effective segment start when an older assessment resumes`() {
        val (_, forrige, behandling) = opprettInMemorySakOgRevurdering(start.minusDays(4))
        val eldre = dto(tom = start.plusDays(25))
            .toEtableringEgenVirksomhetVurdering(Bruker("saks"), forrige.id)
        val nyere = dto(start.plusDays(7), start.plusDays(11), fase = null)
            .copy(virksomhetErNy = false)
            .toEtableringEgenVirksomhetVurdering(Bruker("saks"), behandling.id)
        val response = EtableringEgenVirksomhetVurderingResponse.fraDomene(
            listOf(eldre, nyere).gjeldendeVurderinger(),
            VurdertAvService(
                InMemoryAvklaringsbehovRepository,
                AnsattInfoService(FakeAnsattInfoGateway(), FakeEnhetGateway()),
            )
        )
        assertThat(response.flatMap { it.utviklingsPeriode.orEmpty() }).containsExactly(
            Periode(start, start.plusDays(6)),
            Periode(start.plusDays(12), start.plusDays(25)),
        )
    }

    @Test
    fun `revurdering carries previous assessments forward exactly once`() {
        val (_, forrige, behandling) = opprettInMemorySakOgRevurdering(start.minusDays(4))
        oppfyllSykdomOgBistand(behandling.id)
        val tidligere = dto(tom = start.plusDays(4))
            .toEtableringEgenVirksomhetVurdering(Bruker("saks"), forrige.id)
        InMemoryEtableringEgenVirksomRepository.lagre(forrige.id, listOf(tidligere))
        løser.løs(
            avklaringsbehovKontekst { this.behandling = behandling },
            EtableringEgenVirksomhetLøsning(emptyList()),
        )
        assertThat(InMemoryEtableringEgenVirksomRepository.hentHvisEksisterer(behandling.id)?.vurderinger)
            .containsExactly(tidligere)
    }

    @Test
    fun `migrating historical positive assessments does not make them negative`() {
        val (_, behandling) = opprettInMemorySakOgBehandling(start.minusDays(4))
        val gammel = dto(fase = null)
            .toEtableringEgenVirksomhetVurdering(Bruker("saks"), behandling.id)
            .copy(
                utviklingsPerioder = listOf(Periode(start, start.plusDays(4))),
                oppstartsPerioder = listOf(Periode(start.plusDays(7), start.plusDays(11))),
                jobberBrukerAktivMedVirksomheten = null,
                erRegistrertINødvendigeOffentligeRegister = null,
                versjon = EtableringVersjon.V0,
            )
        val migrerte = listOf(gammel).tilNyttFormat()
        assertThat(migrerte.map { it.evaluerVirksomhetVurdering() }).containsExactly(true, true)
        assertThat(migrerte).allSatisfy {
            assertThat(it.versjon).isEqualTo(EtableringVersjon.V0)
            assertThat(it.jobberBrukerAktivMedVirksomheten).isNull()
            assertThat(it.erRegistrertINødvendigeOffentligeRegister).isNull()
        }
    }

    @Test
    fun `new format does not fabricate an affirmative active work answer`() {
        val (_, behandling) = opprettInMemorySakOgBehandling(start.minusDays(4))
        val vurdering = dto().copy(jobberBrukerAktivMedVirksomheten = null)
            .toEtableringEgenVirksomhetVurdering(Bruker("saks"), behandling.id)
        assertThat(vurdering.evaluerVirksomhetVurdering()).isFalse()
    }

    private fun dto(
        fom: LocalDate = start,
        tom: LocalDate? = null,
        fase: EtableringFase? = EtableringFase.UTVIKLING,
    ) = EtableringEgenVirksomhetLøsningDto(
        begrunnelse = "Regresjon",
        fom = fom,
        tom = tom,
        virksomhetNavn = "Testvirksomhet",
        foreliggerFagligVurdering = true,
        virksomhetErNy = true,
        brukerEierVirksomheten = EierVirksomhet.EIER_MINST_50_PROSENT,
        kanFøreTilSelvforsørget = true,
        fase = fase,
        erRegistrertINødvendigeOffentligeRegister = true,
        jobberBrukerAktivMedVirksomheten = true,
    )

    private fun oppfyllSykdomOgBistand(behandlingId: BehandlingId) {
        val fom = start.minusDays(4)
        InMemorySykdomRepository.lagre(
            behandlingId, listOf(
                Sykdomsvurdering(
                    begrunnelse = "Regresjon",
                    vurderingenGjelderFra = fom,
                    vurderingenGjelderTil = null,
                    harSkadeSykdomEllerLyte = true,
                    harNedsattArbeidsevne = ArbeidsevneNedsattValg.JA,
                    erSkadeSykdomEllerLyteVesentligdel = true,
                    erNedsettelseIArbeidsevneMerEnnHalvparten = true,
                    erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense = false,
                    yrkesskadeBegrunnelse = null,
                    diagnose = null,
                    vurdertIBehandling = behandlingId,
                    opprettet = Instant.now(),
                    vurdertAv = Bruker("saks"),
                )
            )
        )
        InMemoryBistandRepository.lagre(
            behandlingId, listOf(
                Bistandsvurdering(
                    begrunnelse = "Regresjon",
                    erBehovForAktivBehandling = true,
                    erBehovForArbeidsrettetTiltak = true,
                    erBehovForAnnenOppfølging = false,
                    overgangBegrunnelse = null,
                    skalVurdereAapIOvergangTilArbeid = false,
                    vurdertAv = Bruker("saks"),
                    fom = fom,
                    tom = null,
                    opprettet = Instant.now(),
                    vurdertIBehandling = behandlingId,
                )
            )
        )
    }
}
