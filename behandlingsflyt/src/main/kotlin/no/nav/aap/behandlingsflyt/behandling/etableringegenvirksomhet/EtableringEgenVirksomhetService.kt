package no.nav.aap.behandlingsflyt.behandling.etableringegenvirksomhet

import no.nav.aap.behandlingsflyt.behandling.underveis.regler.Hverdager.Companion.antallHverdager
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.bistand.BistandRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.bistand.Bistandsvurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.etableringegenvirksomhet.EierVirksomhet
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.etableringegenvirksomhet.EtableringEgenVirksomhetRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.etableringegenvirksomhet.EtableringEgenVirksomhetVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.etableringegenvirksomhet.EtableringFase
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.etableringegenvirksomhet.MAKS_OPPSTART_HVERDAGER
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.etableringegenvirksomhet.MAKS_UTVIKLING_HVERDAGER
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.etableringegenvirksomhet.beregnTomForSistePeriode
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.etableringegenvirksomhet.justerEtableringPerioder
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.gjeldendeVurderinger
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykdomRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.Sykdomsvurdering
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.tidslinje.orEmpty
import no.nav.aap.komponenter.tidslinje.somTidslinje
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Tid
import no.nav.aap.lookup.repository.RepositoryProvider
import java.time.LocalDate


class EtableringEgenVirksomhetService(
    private val etableringEgenVirksomhetRepository: EtableringEgenVirksomhetRepository,
    private val behandlingRepository: BehandlingRepository,
    private val bistandRepository: BistandRepository,
    private val sykdomRepository: SykdomRepository
) {
    constructor(repositoryProvider: RepositoryProvider) : this(
        etableringEgenVirksomhetRepository = repositoryProvider.provide(),
        behandlingRepository = repositoryProvider.provide(),
        bistandRepository = repositoryProvider.provide(),
        sykdomRepository = repositoryProvider.provide()
    )

    fun validerFaseOgPeriode(
        vurdering: EtableringEgenVirksomhetVurdering,
        historikk: List<EtableringEgenVirksomhetVurdering>
    ) {
        val fase = vurdering.fase ?: return

        if (fase == EtableringFase.OPPSTART) {
            require(vurdering.erRegistrertINødvendigeOffentligeRegister == true) {
                "Må ha satt om virksomheten er registrert i nødvendige offentlige register for oppstartsfasen"
            }

            val sisteUtviklingsVurdering =
                historikk.filter { it.fase == EtableringFase.UTVIKLING }.maxByOrNull { it.fom }
            if (sisteUtviklingsVurdering != null) {
                val sisteUtviklingTom = sisteUtviklingsVurdering.tom
                require(sisteUtviklingTom == null || vurdering.fom.isAfter(sisteUtviklingTom)) {
                    "Oppstartsperioden kan ikke være før utviklingsfase"
                }
            }
        }

        require(vurdering.tom != null) {
            "Må ha gyldig periode"
        }
    }

    fun erVurderingerGyldig(
        behandlingId: BehandlingId,
        nyeVurderinger: List<EtableringEgenVirksomhetVurdering>
    ): VirksomhetEtableringResultat {
        val beregning = beregnVurderinger(behandlingId, nyeVurderinger)
        val gyldighetPeriode = utledGyldighetsPeriode(behandlingId)
        val førsteMuligeDato = gyldighetPeriode.first().fom

        val alleUtviklingsPerioder = beregning.gjeldendeVurderinger.perioderForFase(EtableringFase.UTVIKLING)
        val alleOppstartsPerioder = beregning.gjeldendeVurderinger.perioderForFase(EtableringFase.OPPSTART)

        return try {
            validerGyldighetsperiodeFinnes(gyldighetPeriode)
            validerEtterFørsteMuligeDato(førsteMuligeDato, beregning.beregnedeVurderinger)
            validerInnenforGyldighetsperiode(gyldighetPeriode, beregning.beregnedeVurderinger)
            validerFaseOgPerioderForAlle(beregning)
            validerOppstartEtterUtvikling(alleUtviklingsPerioder, alleOppstartsPerioder)
            validerDagkvoter(alleUtviklingsPerioder, alleOppstartsPerioder)
            VirksomhetEtableringGyldig
        } catch (e: IllegalArgumentException) {
            VirksomhetEtableringIkkeGyldig(e.message ?: "Ugyldig fase-/periode-konfigurasjon")
        }
    }

    private data class Beregning(
        val gamleVurderinger: List<EtableringEgenVirksomhetVurdering>,
        val beregnedeVurderinger: List<EtableringEgenVirksomhetVurdering>,
        val gjeldendeVurderinger: Set<EtableringEgenVirksomhetVurdering>
    )

    private fun beregnVurderinger(
        behandlingId: BehandlingId,
        nyeVurderinger: List<EtableringEgenVirksomhetVurdering>
    ): Beregning {
        val behandling = behandlingRepository.hent(behandlingId)
        val justerteVurderinger = justerEtableringPerioder(nyeVurderinger)

        val gamleVurderinger =
            justerEtableringPerioder(
                behandling.forrigeBehandlingId
                    ?.let { etableringEgenVirksomhetRepository.hentHvisEksisterer(it) }
                    ?.vurderinger
                    .orEmpty())

        val skalValideres = justerteVurderinger.filter { it.vurdertIBehandling == behandlingId }.toSet()

        val beregnedeVurderinger = justerteVurderinger.map { vurdering ->
            if (vurdering !in skalValideres) {
                vurdering
            } else {
                vurdering.takeIf { it.tom != null || it.fase == null }
                    ?: vurdering.copy(
                        tom = beregnTomForSistePeriode(
                            vurderinger = gamleVurderinger + justerteVurderinger,
                            sisteVurdering = vurdering
                        )
                    )
            }
        }

        val gjeldendeVurderinger = (gamleVurderinger + beregnedeVurderinger)
            .gjeldendeVurderinger()
            .verdier().toSet()

        return Beregning(gamleVurderinger, beregnedeVurderinger, gjeldendeVurderinger)
    }

    private fun validerGyldighetsperiodeFinnes(gyldighetPeriode: List<Periode>) {
        require(gyldighetPeriode.isNotEmpty()) {
            "11-5 & 11-6b må være oppfylt i minst én periode"
        }
    }

    private fun validerInnenforGyldighetsperiode(
        gyldighetPeriode: List<Periode>,
        vurderinger: List<EtableringEgenVirksomhetVurdering>
    ) {
        require(vurderinger.all { vurdering -> gyldighetPeriode.any { it.inneholder(vurdering.fom) } }) {
            "Vurderte perioder må falle innen en periode med oppfylt 11-5 & 11-6b"
        }
    }

    private fun validerEtterFørsteMuligeDato(
        førsteMuligeDato: LocalDate?,
        vurderinger: List<EtableringEgenVirksomhetVurdering>
    ) {
        requireNotNull(førsteMuligeDato) {
            "Kan ikke vurdere virksomhet før første dag i periode med oppfylt 11-5 & 11-6b"
        }
        require(vurderinger.all { it.fom.isAfter(førsteMuligeDato) || it.fom.isEqual(førsteMuligeDato) }) {
            "Vurderingen kan tidligst gjelde fra dagen etter første mulige dag med AAP"
        }
    }

    private fun validerFaseOgPerioderForAlle(beregning: Beregning) {
        beregning.beregnedeVurderinger.forEach { vurdering ->
            val historikk = (beregning.gamleVurderinger + beregning.beregnedeVurderinger)
                .filter { it != vurdering }

            validerFaseOgPeriode(vurdering, historikk)
        }
    }

    private fun validerOppstartEtterUtvikling(
        alleUtviklingsPerioder: List<Periode>,
        alleOppstartsPerioder: List<Periode>
    ) {
        val sisteUtviklingsPeriodeTom = alleUtviklingsPerioder.maxOfOrNull { it.tom } ?: return
        require(alleOppstartsPerioder.none { it.fom.isBefore(sisteUtviklingsPeriodeTom) }) {
            "Oppstartsperiode kan ikke ligge før en utviklingsperiode"
        }
    }

    private fun validerDagkvoter(
        alleUtviklingsPerioder: List<Periode>,
        alleOppstartsPerioder: List<Periode>
    ) {
        val bruktUtviklingsDager =
            alleUtviklingsPerioder.somTidslinje { it }.komprimer().segmenter()
                .sumOf { it.periode.antallHverdager().asInt }
        val bruktOppstartsdager =
            alleOppstartsPerioder.somTidslinje { it }.komprimer().segmenter()
                .sumOf { it.periode.antallHverdager().asInt }

        require(bruktUtviklingsDager <= MAKS_UTVIKLING_HVERDAGER) {
            "Oppsatte utviklingsdager overstiger gjenværende dager: $bruktUtviklingsDager / $MAKS_UTVIKLING_HVERDAGER"
        }
        require(bruktOppstartsdager <= MAKS_OPPSTART_HVERDAGER) {
            "Oppsatte oppstartsdager overstiger gjenværende dager: $bruktOppstartsdager / $MAKS_OPPSTART_HVERDAGER"
        }
    }

    private fun Collection<EtableringEgenVirksomhetVurdering>.perioderForFase(fase: EtableringFase): List<Periode> =
        mapNotNull { vurdering -> if (vurdering.fase == fase) vurdering.tom?.let { Periode(vurdering.fom, it) } else null }

    fun evaluerVirksomhetVurdering(vurdering: EtableringEgenVirksomhetVurdering): Boolean {
        return vurdering.fase != null && vurdering.jobberBrukerAktivMedVirksomheten == true && vurdering.virksomhetErNy == true && vurdering.kanFøreTilSelvforsørget == true && vurdering.foreliggerFagligVurdering && vurdering.brukerEierVirksomheten in listOf(
            EierVirksomhet.EIER_MINST_50_PROSENT,
            EierVirksomhet.EIER_MINST_50_PROSENT_MED_FLER
        )
    }

    fun utledGyldighetsPeriode(
        behandlingId: BehandlingId
    ): List<Periode> {
        val førsteDagIOppfyltPeriode =
            tidslinjeSykdomOgBistandOppfylt(behandlingId).perioder().toList().firstOrNull()?.fom ?: return emptyList()
        return tidslinjeSykdomOgBistandOppfylt(behandlingId).begrensetTil(
            Periode(
                førsteDagIOppfyltPeriode.plusDays(1),
                Tid.MAKS
            )
        ).perioder().toList()
    }

    private fun tidslinjeSykdomOgBistandOppfylt(behandlingId: BehandlingId) = sykdomOgBistandTidslinje(behandlingId)
        .filter {
            it.verdi.first?.erOppfyltForOrdinærEllerYrkesskadeSettBortIfraÅrsakssammenheng() == true && it.verdi.second?.erBehovForArbeidsrettetTiltak == true
        }

    fun utledIkkeVurderbarePerioder(behandlingId: BehandlingId): List<Periode> {
        val førsteDagIOppfyltPeriode =
            tidslinjeSykdomOgBistandOppfylt(behandlingId).perioder().toList().firstOrNull()?.fom ?: return emptyList()

        return sykdomOgBistandTidslinje(behandlingId)
            .filter {
                it.verdi.first?.erOppfyltForOrdinærEllerYrkesskadeSettBortIfraÅrsakssammenheng() != true
                        || it.verdi.second?.erBehovForArbeidsrettetTiltak != true
            }.perioder().plus(Periode(førsteDagIOppfyltPeriode, førsteDagIOppfyltPeriode)).toList()
    }

    private fun sykdomOgBistandTidslinje(behandlingId: BehandlingId): Tidslinje<Pair<Sykdomsvurdering?, Bistandsvurdering?>> {
        val sykdomGrunnlag = sykdomRepository.hentHvisEksisterer(behandlingId)
        val bistandGrunnlag = bistandRepository.hentHvisEksisterer(behandlingId)

        return Tidslinje.zip2(
            sykdomGrunnlag?.somSykdomsvurderingstidslinje().orEmpty(),
            bistandGrunnlag?.somBistandsvurderingstidslinje().orEmpty(),
        )
    }
}

sealed interface VirksomhetEtableringResultat

data class VirksomhetEtableringIkkeGyldig(val feilmelding: String) : VirksomhetEtableringResultat

data object VirksomhetEtableringGyldig : VirksomhetEtableringResultat