package no.nav.aap.behandlingsflyt.behandling.etableringegenvirksomhet

import no.nav.aap.behandlingsflyt.behandling.underveis.regler.Hverdager.Companion.antallHverdager
import no.nav.aap.bistandsbehov.BistandRepository
import no.nav.aap.bistandsbehov.Bistandsvurdering
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
    ): String? {
        val fase = vurdering.fase ?: return null

        return validerOppstartRegistrering(fase, vurdering)
            ?: validerOppstartKommerEtterSisteUtvikling(fase, vurdering, historikk)
            ?: validerGyldigPeriode(vurdering)
    }

    private fun validerOppstartRegistrering(
        fase: EtableringFase,
        vurdering: EtableringEgenVirksomhetVurdering
    ): String? {
        if (fase != EtableringFase.OPPSTART) return null
        return if (vurdering.erRegistrertINødvendigeOffentligeRegister != true) {
            "Må ha satt om virksomheten er registrert i nødvendige offentlige register for oppstartsfasen"
        } else null
    }

    private fun validerOppstartKommerEtterSisteUtvikling(
        fase: EtableringFase,
        vurdering: EtableringEgenVirksomhetVurdering,
        historikk: List<EtableringEgenVirksomhetVurdering>
    ): String? {
        if (fase != EtableringFase.OPPSTART) return null
        val sisteUtviklingTom = historikk
            .filter { it.fase == EtableringFase.UTVIKLING }
            .maxByOrNull { it.fom }
            ?.tom
            ?: return null

        return if (!vurdering.fom.isAfter(sisteUtviklingTom)) {
            "Oppstartsperioden kan ikke være før utviklingsfase"
        } else null
    }

    private fun validerGyldigPeriode(vurdering: EtableringEgenVirksomhetVurdering): String? =
        if (vurdering.tom == null) "Må ha gyldig periode" else null

    fun beregnOgValider(
        behandlingId: BehandlingId,
        nyeVurderinger: List<EtableringEgenVirksomhetVurdering>
    ):BeregningResultat {
        val beregning = try {
            beregnVurderinger(behandlingId, nyeVurderinger)
        } catch (e: IllegalArgumentException) {
            return BeregningResultat.Ugyldig(e.message ?: "Ugyldig fase-/periode-konfigurasjon")
        }

        val feilmelding = validerBeregning(behandlingId, beregning)
        return if (feilmelding == null) BeregningResultat.Gyldig(beregning) else BeregningResultat.Ugyldig(feilmelding)
    }

    data class Beregning(
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

        val gamleVurderingerUtenOverstyrte = gamleVurderinger.filterNot { gammel ->
            justerteVurderinger.any { ny -> ny.fase == gammel.fase && ny.fom == gammel.fom }
        }

        val aktiveVurderingerForValidering = (gamleVurderingerUtenOverstyrte + justerteVurderinger)

        val skalValideres = justerteVurderinger.filter { it.vurdertIBehandling == behandlingId }.toSet()

        val sisteVurderingPerFase = aktiveVurderingerForValidering
            .filter { it.fase != null }
            .groupBy { it.fase }
            .mapNotNull { (_, vurderinger) -> vurderinger.maxByOrNull { it.fom } }
            .toSet()

        val beregnedeVurderinger = aktiveVurderingerForValidering.map { vurdering ->
            when {
                vurdering !in skalValideres -> vurdering
                vurdering.fase == null -> vurdering
                vurdering in sisteVurderingPerFase -> vurdering.copy(
                    tom = beregnTomForSistePeriode(
                        vurderinger = aktiveVurderingerForValidering,
                        sisteVurdering = vurdering
                    )
                )

                else -> vurdering
            }
        }

        val gjeldendeVurderinger = (gamleVurderingerUtenOverstyrte + beregnedeVurderinger)
            .gjeldendeVurderinger()
            .verdier().toSet()

        return Beregning(gamleVurderinger, beregnedeVurderinger, gjeldendeVurderinger)
    }

    private fun validerBeregning(
        behandlingId: BehandlingId,
        beregning: Beregning
    ): String? {
        val gyldighetPeriode = utledGyldighetsPeriode(behandlingId)

        val alleUtviklingsPerioder = beregning.gjeldendeVurderinger.perioderForFase(EtableringFase.UTVIKLING)
        val alleOppstartsPerioder = beregning.gjeldendeVurderinger.perioderForFase(EtableringFase.OPPSTART)

        validerGyldighetsperiodeFinnes(gyldighetPeriode)?.let { return it }
        val førsteMuligeDato = gyldighetPeriode.first().fom

        return validerEtterFørsteMuligeDato(førsteMuligeDato, beregning.beregnedeVurderinger)
            ?: validerInnenforGyldighetsperiode(gyldighetPeriode, beregning.beregnedeVurderinger)
            ?: validerFaseOgPerioderForAlle(beregning)
            ?: validerOppstartEtterUtvikling(alleUtviklingsPerioder, alleOppstartsPerioder)
            ?: validerDagkvoter(alleUtviklingsPerioder, alleOppstartsPerioder)
    }

    private fun validerGyldighetsperiodeFinnes(gyldighetPeriode: List<Periode>): String? {
        if (gyldighetPeriode.isEmpty()) {
            return "11-5 & 11-6b må være oppfylt i minst én periode"
        }
        return null
    }

    private fun validerInnenforGyldighetsperiode(
        gyldighetPeriode: List<Periode>,
        vurderinger: List<EtableringEgenVirksomhetVurdering>
    ): String? {
        if (!vurderinger.all { vurdering -> gyldighetPeriode.any { it.inneholder(vurdering.fom) } }) {
            return "Vurderte perioder må falle innen en periode med oppfylt 11-5 & 11-6b"
        }
        return null
    }

    private fun validerEtterFørsteMuligeDato(
        førsteMuligeDato: LocalDate?,
        vurderinger: List<EtableringEgenVirksomhetVurdering>
    ): String? {
        if (førsteMuligeDato == null) {
            return "Kan ikke vurdere virksomhet før første dag i periode med oppfylt 11-5 & 11-6b"
        }
        if (!vurderinger.all { it.fom.isAfter(førsteMuligeDato) || it.fom.isEqual(førsteMuligeDato) }) {
            return "Vurderingen kan tidligst gjelde fra dagen etter første mulige dag med AAP"
        }
        return null
    }

    private fun validerFaseOgPerioderForAlle(beregning: Beregning): String? =
        beregning.beregnedeVurderinger.firstNotNullOfOrNull { vurdering ->
            val historikk = (beregning.gamleVurderinger + beregning.beregnedeVurderinger)
                .filter { it != vurdering }
            validerFaseOgPeriode(vurdering, historikk)
        }

    private fun validerOppstartEtterUtvikling(
        alleUtviklingsPerioder: List<Periode>,
        alleOppstartsPerioder: List<Periode>
    ): String? {
        val sisteUtviklingsPeriodeTom = alleUtviklingsPerioder.maxOfOrNull { it.tom } ?: return null
        if (alleOppstartsPerioder.any { it.fom.isBefore(sisteUtviklingsPeriodeTom) }) {
            return "Oppstartsperiode kan ikke ligge før en utviklingsperiode"
        }
        return null
    }

    private fun validerDagkvoter(
        alleUtviklingsPerioder: List<Periode>,
        alleOppstartsPerioder: List<Periode>
    ): String? {
        val bruktUtviklingsDager =
            alleUtviklingsPerioder.somTidslinje { it }.komprimer().segmenter()
                .sumOf { it.periode.antallHverdager().asInt }
        val bruktOppstartsdager =
            alleOppstartsPerioder.somTidslinje { it }.komprimer().segmenter()
                .sumOf { it.periode.antallHverdager().asInt }

        if (bruktUtviklingsDager > MAKS_UTVIKLING_HVERDAGER) {
            return "Oppsatte utviklingsdager overstiger gjenværende dager: $bruktUtviklingsDager / $MAKS_UTVIKLING_HVERDAGER"
        }
        if (bruktOppstartsdager > MAKS_OPPSTART_HVERDAGER) {
            return "Oppsatte oppstartsdager overstiger gjenværende dager: $bruktOppstartsdager / $MAKS_OPPSTART_HVERDAGER"
        }
        return null
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

sealed interface BeregningResultat {
    data class Gyldig(val beregning: EtableringEgenVirksomhetService.Beregning) : BeregningResultat
    data class Ugyldig(val feilmelding: String) : BeregningResultat
}