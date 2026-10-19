package no.nav.aap.behandlingsflyt.behandling.etableringegenvirksomhet

import no.nav.aap.bistandsbehov.BistandRepository
import no.nav.aap.bistandsbehov.Bistandsvurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.etableringegenvirksomhet.EtableringEgenVirksomhetRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.etableringegenvirksomhet.EtableringEgenVirksomhetVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.etableringegenvirksomhet.EtableringFase
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.etableringegenvirksomhet.tellDager
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.gjeldendeVurderinger
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykdomRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.Sykdomsvurdering
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.tidslinje.orEmpty
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Tid
import no.nav.aap.lookup.repository.RepositoryProvider

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
        val gjeldendeVurderinger = (gamleVurderinger + nyeVurderinger)
            .gjeldendeVurderinger()
            .verdier().toSet()

        val gyldighetPeriode = utledGyldighetsPeriode(behandlingId)
        if (gyldighetPeriode.isEmpty()) {
            return VirksomhetEtableringResultat.Ugyldig.SykdomOgBistandMåVæreOppfylt
        }

        val perioderMedSykdomBistandOppfylt = tidslinjeSykdomOgBistandOppfylt(behandlingId).perioder().toList()
        if (nyeVurderinger.any { vurdering ->
                perioderMedSykdomBistandOppfylt.none { oppfyltPeriode -> oppfyltPeriode.inneholder(vurdering.fom) }
            }
        ) {
            return VirksomhetEtableringResultat.Ugyldig.SykdomOgBistandMåVæreOppfyltOverlapp
        }

        val førsteMuligeDato = gyldighetPeriode.first().fom

        if (nyeVurderinger.any { it.evaluerVirksomhetVurdering() && it.utviklingsPerioder.isEmpty() && it.oppstartsPerioder.isEmpty() }) {
            return VirksomhetEtableringResultat.Ugyldig.KreverPeriodeHvisVilkårOppfylt
        }

        if (gjeldendeVurderinger.isNotEmpty() && gjeldendeVurderinger.any { it.fom.isBefore(førsteMuligeDato) }) {
            return VirksomhetEtableringResultat.Ugyldig.FørFørsteMuligeStartdato
        }

        val alleUtviklingsPerioder = gjeldendeVurderinger.flatMap { it.utviklingsPerioder }
        val alleOppstartsPerioder = gjeldendeVurderinger.flatMap { it.oppstartsPerioder }

        val sisteUtviklingsPeriodeTom = alleUtviklingsPerioder.maxOfOrNull { it.tom }
        if (sisteUtviklingsPeriodeTom != null && alleOppstartsPerioder.any { it.fom.isBefore(sisteUtviklingsPeriodeTom) }) {
            return VirksomhetEtableringResultat.Ugyldig.OppstartFørUtvikling
        }

        val bruktUtviklingsDager = gjeldendeVurderinger.tellDager { it.utviklingsPerioder }
        val bruktOppstartsdager = gjeldendeVurderinger.tellDager { it.oppstartsPerioder }

        if (bruktUtviklingsDager > maksUtviklingsdager) {
            return VirksomhetEtableringResultat.Ugyldig.OverstigerKvoter(EtableringFase.UTVIKLING, bruktUtviklingsDager)
        }
        if (bruktOppstartsdager > maksOppstartsdager)
            return VirksomhetEtableringResultat.Ugyldig.OverstigerKvoter(EtableringFase.OPPSTART, bruktOppstartsdager)
        return VirksomhetEtableringGyldig
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

data object VirksomhetEtableringGyldig : VirksomhetEtableringResultat

sealed interface VirksomhetEtableringResultat {
    sealed interface Ugyldig : VirksomhetEtableringResultat {
        val melding: String

        data class OverstigerKvoter(val fase: EtableringFase, val brukteDager: Int) : Ugyldig {
            private val dagerStr = when (fase) {
                EtableringFase.UTVIKLING -> "utviklingsdager"
                EtableringFase.OPPSTART -> "oppstartsdager"
            }
            override val melding =
                "Oppsatte $dagerStr overstiger gjenværende dager: $brukteDager / ${fase.maksHverdager}"
        }

        data object OppstartFørUtvikling : Ugyldig {
            override val melding: String = "Oppstartsperiode kan ikke ligge før en utviklingsperiode."
        }

        data object FørFørsteMuligeStartdato : Ugyldig {
            override val melding: String = "Vurderingen kan tidligst gjelde fra dagen etter første mulige dag med AAP."
        }

        data object SykdomOgBistandMåVæreOppfylt : Ugyldig {
            override val melding: String = "11-5 & 11-6b må være oppfylt i minst én periode."
        }

        data object SykdomOgBistandMåVæreOppfyltOverlapp : Ugyldig {
            override val melding: String = "Vurderte perioder må falle innen en periode med oppfylt 11-5 & 11-6b."
        }

        data object KreverPeriodeHvisVilkårOppfylt : Ugyldig {
            override val melding: String =
                "Må ha definert minst én periode i tidsplanen dersom vilkåret er oppfylt for en periode"
        }
    }
}