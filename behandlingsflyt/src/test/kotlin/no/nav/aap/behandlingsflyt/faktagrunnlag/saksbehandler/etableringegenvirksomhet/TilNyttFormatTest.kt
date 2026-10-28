package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.etableringegenvirksomhet

import no.nav.aap.behandlingsflyt.behandling.etableringegenvirksomhet.EtableringEgenVirksomhetService
import no.nav.aap.behandlingsflyt.behandling.etableringegenvirksomhet.VirksomhetEtableringResultat
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.ArbeidsevneNedsattValg
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.Sykdomsvurdering
import no.nav.aap.behandlingsflyt.help.opprettInMemorySakOgBehandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryBehandlingRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryBistandRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryEtableringEgenVirksomRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemorySykdomRepository
import no.nav.aap.behandlingsflyt.test.april
import no.nav.aap.behandlingsflyt.test.desember
import no.nav.aap.behandlingsflyt.test.februar
import no.nav.aap.behandlingsflyt.test.januar
import no.nav.aap.behandlingsflyt.test.mai
import no.nav.aap.behandlingsflyt.test.mars
import no.nav.aap.bistandsbehov.Bistandsvurdering
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Bruker
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class TilNyttFormatTest {
    @Test
    fun `V1 beholdes uendret mens V0 fortsatt konverteres i samme liste`() {
        val historisk = konstruerVurdering {
            utviklingsperioder = listOf(5 januar 2026 to (9 januar 2026))
        }
        val forventetHistorisk = historisk.copy(
            utviklingsPerioder = emptyList(),
            fase = EtableringFase.UTVIKLING,
            fom = 5 januar 2026,
            tom = 9 januar 2026,
        )

        listOf(EtableringFase.UTVIKLING, EtableringFase.OPPSTART, null).forEach { fase ->
            val ny = historisk.copy(
                versjon = EtableringVersjon.V1,
                fase = fase,
                fom = 2 februar 2026,
                tom = 6 februar 2026,
            )

            val resultat = listOf(ny, historisk).tilNyttFormat()

            assertThat(resultat).usingRecursiveComparison().isEqualTo(listOf(ny, forventetHistorisk))
            assertThat(resultat.first()).isSameAs(ny)
        }
    }

    @Test
    fun `nye fasevurderinger krever aktivitet og oppstart krever registrering`() {
        val utvikling = konstruerVurdering { fase = EtableringFase.UTVIKLING }
            .copy(versjon = EtableringVersjon.V1, jobberBrukerAktivMedVirksomheten = true)
        val oppstart = utvikling.copy(fase = EtableringFase.OPPSTART)

        assertThat(utvikling.evaluerVirksomhetVurdering()).isTrue()
        assertThat(utvikling.copy(jobberBrukerAktivMedVirksomheten = null).evaluerVirksomhetVurdering()).isFalse()
        assertThat(oppstart.evaluerVirksomhetVurdering()).isFalse()
        assertThat(service.validerFaseOgPeriode(oppstart))
            .isEqualTo(VirksomhetEtableringResultat.Ugyldig.MåOppgiRegistrertIOffentligeRegister)
        assertThat(oppstart.copy(erRegistrertINødvendigeOffentligeRegister = true).evaluerVirksomhetVurdering())
            .isTrue()
    }

    @Test
    fun `historiske positive vurderinger beholder utfallet uten å finne på svar`() {
        val historisk = konstruerVurdering {
            utviklingsperioder = listOf((5 januar 2026) to (9 januar 2026))
            oppstartsperioder = listOf((12 januar 2026) to (16 januar 2026))
        }
        assertThat(historisk.evaluerVirksomhetVurdering()).isTrue()

        val konverterte = listOf(historisk).tilNyttFormat()

        assertThat(konverterte.map { it.fase })
            .containsExactly(EtableringFase.UTVIKLING, EtableringFase.OPPSTART)
        assertThat(konverterte).allSatisfy {
            assertThat(it.versjon).isEqualTo(EtableringVersjon.V0)
            assertThat(it.jobberBrukerAktivMedVirksomheten).isNull()
            assertThat(it.erRegistrertINødvendigeOffentligeRegister).isNull()
            assertThat(it.evaluerVirksomhetVurdering()).isTrue()
            assertThat(it.copy(versjon = EtableringVersjon.V1).evaluerVirksomhetVurdering()).isFalse()
        }
        assertValideringUendret(listOf(historisk))
    }

    @Test
    fun `teller dager likt for gamle migrerte og blandede vurderinger`() {
        val utvikling = konstruerVurdering {
            fom = 2 februar 2026
            tom = 6 februar 2026
            utviklingsperioder = listOf(2 februar 2026 to (6 februar 2026))
        }
        val oppstart = konstruerVurdering {
            fom = 9 februar 2026
            tom = 13 februar 2026
            oppstartsperioder = listOf(9 februar 2026 to (13 februar 2026))
        }
        val gammel = listOf(utvikling, oppstart)
        val migrert = gammel.tilNyttFormat()
        val blandet = listOf(utvikling) + listOf(oppstart).tilNyttFormat()

        listOf(gammel, migrert, blandet).forEach { vurderinger ->
            assertThat(vurderinger.brukteDager()).isEqualTo(5 to 5)
            assertThat(vurderinger.tellDager { it.utviklingsPerioder }).isEqualTo(5)
            assertThat(vurderinger.tellDager { it.oppstartsPerioder }).isEqualTo(5)
        }
        assertThat(migrert).allSatisfy {
            assertThat(it.utviklingsPerioder).isEmpty()
            assertThat(it.oppstartsPerioder).isEmpty()
        }
        assertValideringUendret(blandet)
    }

    @Test
    fun `splitter oppstartsfaser i to vurderinger`() {
        val gammel = listOf(
            konstruerVurdering {
                oppstartsperioder = listOf(
                    (1 februar 2026) to (1 mars 2026),
                    (2 mars 2026) to (10 mars 2026),
                )
            })

        val forventet = listOf(konstruerVurdering {
            fom = 1 februar 2026
            tom = 1 mars 2026
            oppstartsperioder = listOf()
            fase = EtableringFase.OPPSTART
        }, konstruerVurdering {
            fom = 2 mars 2026
            tom = 10 mars 2026
            oppstartsperioder = listOf()
            fase = EtableringFase.OPPSTART
        })

        assertThat(gammel.tilNyttFormat()).usingRecursiveComparison().isEqualTo(forventet)
    }

    @Test
    fun `splitter utviklingsfaser i nye vurdering`() {
        val gammel = listOf(
            konstruerVurdering {
                utviklingsperioder = listOf(
                    (1 februar 2026) to (1 mars 2026),
                    (2 mars 2026) to (10 mars 2026),
                )
            })

        val forventet = listOf(konstruerVurdering {
            fom = 1 februar 2026
            tom = 1 mars 2026
            utviklingsperioder = listOf()
            fase = EtableringFase.UTVIKLING
        }, konstruerVurdering {
            fom = 2 mars 2026
            tom = 10 mars 2026
            utviklingsperioder = listOf()
            fase = EtableringFase.UTVIKLING
        })

        assertThat(gammel.tilNyttFormat()).usingRecursiveComparison().isEqualTo(forventet)
    }

    @Test
    fun `vurderinger med både oppstart og utvikling splittes i to`() {
        val gammel = listOf(
            konstruerVurdering {
                utviklingsperioder = listOf(
                    (1 februar 2026) to (1 mars 2026),
                    (2 mars 2026) to (10 mars 2026),
                )
                oppstartsperioder = listOf(
                    1 april 2026 to (1 mai 2026),
                )
            })

        val ny = gammel.tilNyttFormat()

        val forventet = listOf(
            konstruerVurdering {
                fom = 1 februar 2026
                tom = 1 mars 2026
                utviklingsperioder = listOf()
                fase = EtableringFase.UTVIKLING
            }, konstruerVurdering {
                fom = 2 mars 2026
                tom = 10 mars 2026
                utviklingsperioder = listOf()
                fase = EtableringFase.UTVIKLING
            }, konstruerVurdering {
                fom = 1 april 2026
                tom = 1 mai 2026
                utviklingsperioder = listOf()
                fase = EtableringFase.OPPSTART
            }
        )

        assertThat(ny).usingRecursiveComparison().ignoringCollectionOrder().isEqualTo(forventet)
    }

    @Test
    fun `mapper alle vurderinger når listen har flere elementer`() {
        val gammel = listOf(
            konstruerVurdering {
                utviklingsperioder = listOf((1 februar 2026) to (1 mars 2026))
            },
            konstruerVurdering {
                oppstartsperioder = listOf((2 mars 2026) to (10 mars 2026))
            },
            konstruerVurdering {
                fom = 1 april 2026
                tom = 1 mai 2026
                foreliggerFagligVurdering = false
            },
        )

        val forventet = listOf(
            konstruerVurdering {
                fom = 1 februar 2026
                tom = 1 mars 2026
                fase = EtableringFase.UTVIKLING
            },
            konstruerVurdering {
                fom = 2 mars 2026
                tom = 10 mars 2026
                fase = EtableringFase.OPPSTART
            },
            konstruerVurdering {
                fom = 1 april 2026
                tom = 1 mai 2026
                foreliggerFagligVurdering = false
            },
        )

        assertThat(gammel.tilNyttFormat()).usingRecursiveComparison().isEqualTo(forventet)
    }

    @Test
    fun `konvertere fra nytt format til gmamelt`() {
        val gammel = listOf(
            konstruerVurdering {
                fase = EtableringFase.OPPSTART
                fom = 1 februar 2026
                tom = 1 mars 2026
            })

        val mappetTilGammelt = gammel.map { it.tilGammeltFormat() }

        val forventet = listOf(
            konstruerVurdering {
                fase = EtableringFase.OPPSTART
                fom = 1 februar 2026
                tom = 1 mars 2026
                oppstartsperioder = listOf(1 februar 2026 to (1 mars 2026))
            })

        assertThat(mappetTilGammelt).usingRecursiveComparison().isEqualTo(forventet)
    }

    private val service = EtableringEgenVirksomhetService(
        InMemoryEtableringEgenVirksomRepository,
        InMemoryBehandlingRepository,
        InMemoryBistandRepository,
        InMemorySykdomRepository
    )

    private fun assertValideringUendret(gammel: List<EtableringEgenVirksomhetVurdering>) {
        val (_, behandling) = opprettInMemorySakOgBehandling(1 januar 2026)
        val oppfyltFra = 31 desember 2025
        InMemorySykdomRepository.lagre(
            behandling.id, listOf(
                Sykdomsvurdering(
                    begrunnelse = "...",
                    vurderingenGjelderFra = oppfyltFra,
                    vurderingenGjelderTil = null,
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
                    fom = oppfyltFra,
                    tom = null,
                    opprettet = Instant.now(),
                    vurdertIBehandling = behandling.id
                )
            )
        )

        val før = service.beregnOgValider(behandling.id, gammel)
        val etter = service.beregnOgValider(behandling.id, gammel.tilNyttFormat())

        assertThat(før).isInstanceOf(VirksomhetEtableringResultat.Gyldig::class.java)
        assertThat(etter).isInstanceOf(VirksomhetEtableringResultat.Gyldig::class.java)
    }

    private class VurderingBuilder(
        var fom: LocalDate? = 1 januar 2026,
        var tom: LocalDate? = 1 februar 2026,
        var foreliggerFagligVurdering: Boolean = true,
        var fase: EtableringFase? = null,
        var utviklingsperioder: List<Pair<LocalDate, LocalDate>>? = null,
        var oppstartsperioder: List<Pair<LocalDate, LocalDate>>? = null
    )

    private fun konstruerVurdering(apply: VurderingBuilder.() -> Unit = {}): EtableringEgenVirksomhetVurdering {

        val builder = VurderingBuilder().apply(apply)

        return EtableringEgenVirksomhetVurdering(
            begrunnelse = "dictumst",
            virksomhetNavn = "tritani",
            orgNr = "risus",
            foreliggerFagligVurdering = true,
            virksomhetErNy = true,
            brukerEierVirksomheten = EierVirksomhet.EIER_MINST_50_PROSENT,
            kanFøreTilSelvforsørget = true,
            utviklingsPerioder = listOf(),
            oppstartsPerioder = listOf(),
            vurdertAv = Bruker("fff"),
            opprettet = LocalDate.now().atStartOfDay().toInstant(ZoneOffset.UTC),
            vurdertIBehandling = BehandlingId(1L),
            fom = 1 januar 2026,
            tom = null,
            fase = null,
            erRegistrertINødvendigeOffentligeRegister = null,
            jobberBrukerAktivMedVirksomheten = null,
            versjon = EtableringVersjon.V0,
        ).run {
            copy(
                fom = builder.fom ?: this.fom,
                tom = builder.tom ?: this.tom,
                fase = builder.fase ?: this.fase,
                utviklingsPerioder = builder.utviklingsperioder?.map { Periode(it.first, it.second) }
                    ?: this.utviklingsPerioder,
                oppstartsPerioder = builder.oppstartsperioder?.map { Periode(it.first, it.second) }
                    ?: this.oppstartsPerioder,
                foreliggerFagligVurdering = builder.foreliggerFagligVurdering)
        }
    }
}
