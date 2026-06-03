package no.nav.aap.behandlingsflyt.behandling.etableringegenvirksomhet

import no.nav.aap.behandlingsflyt.behandling.underveis.regler.Hverdager.Companion.antallHverdager
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.bistand.BistandRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.bistand.Bistandsvurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.etableringegenvirksomhet.EierVirksomhet
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.etableringegenvirksomhet.EtableringEgenVirksomhetRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.etableringegenvirksomhet.EtableringEgenVirksomhetVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykdomRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.Sykdomsvurdering
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.tidslinje.orEmpty
import no.nav.aap.komponenter.tidslinje.somTidslinje
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.lookup.repository.RepositoryProvider
import kotlin.collections.orEmpty
import kotlin.collections.plus

class EtableringEgenVirksomhetService(
    private val etableringEgenVirksomhetRepository: EtableringEgenVirksomhetRepository,
    private val behandlingRepository: BehandlingRepository,
    private val bistandRepository: BistandRepository,
    private val sykdomRepository: SykdomRepository,
) {
    constructor(repositoryProvider: RepositoryProvider) : this(
        etableringEgenVirksomhetRepository = repositoryProvider.provide(),
        behandlingRepository = repositoryProvider.provide(),
        bistandRepository = repositoryProvider.provide(),
        sykdomRepository = repositoryProvider.provide()
    )

    private val maksUtviklingsdager = 131
    private val maksOppstartsdager = 66

    fun erVurderingerGyldig(
        behandlingId: BehandlingId,
        nyeVurderinger: List<EtableringEgenVirksomhetVurdering>
    ): VirksomhetEtableringResultat {
        val behandling = behandlingRepository.hent(behandlingId)

        val gamleVurderinger =
            behandling.forrigeBehandlingId?.let { etableringEgenVirksomhetRepository.hentHvisEksisterer(it) }?.vurderinger.orEmpty()
        val alleVurderinger = gamleVurderinger + nyeVurderinger

        val gyldighetPeriode = utledGyldighetsPeriode(behandlingId)
        if (gyldighetPeriode.isEmpty()) {
            return VirksomhetEtableringIkkeGyldig(
                "11-5 & 11-6b må være oppfylt i minst én periode"
            )
        }

        if (nyeVurderinger.any { vurdering ->
                gyldighetPeriode.none { gyldighetPeriode -> gyldighetPeriode.inneholder(vurdering.vurderingenGjelderFra) }
            }
        ) {
            return VirksomhetEtableringIkkeGyldig(
                "Vurderte perioder må falle innen en periode med oppfylt 11-5 & 11-6b"
            )
        }

        val førsteMuligeDato = gyldighetPeriode.first().fom

        if (nyeVurderinger.any { evaluerVirksomhetVurdering(it) && it.utviklingsPerioder.isEmpty() && it.oppstartsPerioder.isEmpty() }) {
            return VirksomhetEtableringIkkeGyldig(
                "Må ha definert minst én periode i tidsplanen dersom vilkåret er oppfylt for en periode"
            )
        }

        if (alleVurderinger.isNotEmpty() && alleVurderinger.none { it.vurderingenGjelderFra.isAfter(førsteMuligeDato) }) {
            return VirksomhetEtableringIkkeGyldig(
                "vurderingenGjelderFra må være minst én dag etter første mulige dag med AAP"
            )
        }

        val alleUtviklingsPerioder = alleVurderinger.flatMap { it.utviklingsPerioder }
        val alleOppstartsPerioder = alleVurderinger.flatMap { it.oppstartsPerioder }

        val sisteUtviklingsPeriodeTom = alleUtviklingsPerioder.maxOfOrNull { it.tom }
        if (sisteUtviklingsPeriodeTom != null && alleOppstartsPerioder.any { it.fom.isBefore(sisteUtviklingsPeriodeTom) }) {
            return VirksomhetEtableringIkkeGyldig("Oppstartsperioder kan ikke ligge før en utviklingsperiode")
        }

        val bruktUtviklingsDager =
            (alleVurderinger).flatMap { it.utviklingsPerioder }.somTidslinje { it }.komprimer().segmenter()
                .sumOf { it.periode.antallHverdager().asInt }
        val bruktOppstartsdager =
            (alleVurderinger).flatMap { it.oppstartsPerioder }.somTidslinje { it }.komprimer().segmenter()
                .sumOf { it.periode.antallHverdager().asInt }

        if (bruktUtviklingsDager > maksUtviklingsdager) {
            return VirksomhetEtableringIkkeGyldig(
                "Oppsatte utviklingsdager overstiger gjenværende dager: $bruktUtviklingsDager / $maksUtviklingsdager"
            )
        }
        if (bruktOppstartsdager > maksOppstartsdager)
            return VirksomhetEtableringIkkeGyldig(
                "Oppsatte oppstartsdager overstiger gjenværende dager: $bruktOppstartsdager / $maksOppstartsdager"
            )

        return VirksomhetEtableringGyldig
    }

    fun evaluerVirksomhetVurdering(vurdering: EtableringEgenVirksomhetVurdering): Boolean {
        return vurdering.virksomhetErNy == true && vurdering.kanFøreTilSelvforsørget == true && vurdering.foreliggerFagligVurdering && vurdering.brukerEierVirksomheten in listOf(
            EierVirksomhet.EIER_MINST_50_PROSENT,
            EierVirksomhet.EIER_MINST_50_PROSENT_MED_FLER
        )
    }

    fun utledGyldighetsPeriode(
        behandlingId: BehandlingId
    ): List<Periode> {
        val mapped = sykdomOgBistandTidslinje(behandlingId)
            .filter {
                it.verdi.first?.erOppfyltForOrdinærEllerYrkesskadeSettBortIfraÅrsakssammenheng() == true
                        && it.verdi.second?.erBehovForArbeidsrettetTiltak == true
            }
        return mapped.perioder().toList()
    }

    fun utledIkkeVurderbarePerioder(behandlingId: BehandlingId): List<Periode> {
        val førsteDagIOppfyltPeriode = sykdomOgBistandTidslinje(behandlingId)
            .filter {
                it.verdi.first?.erOppfyltForOrdinærEllerYrkesskadeSettBortIfraÅrsakssammenheng() == true || it.verdi.second?.erBehovForBistand() != true
            }.perioder().toList().firstOrNull()?.fom

        if (førsteDagIOppfyltPeriode == null) return emptyList()

        val mapped = sykdomOgBistandTidslinje(behandlingId)
            .filter {
                it.verdi.first?.erOppfyltForOrdinærEllerYrkesskadeSettBortIfraÅrsakssammenheng() != true
                        || it.verdi.second?.erBehovForArbeidsrettetTiltak != true
            }

        return mapped.perioder().plus(Periode(førsteDagIOppfyltPeriode, førsteDagIOppfyltPeriode)).toList()
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