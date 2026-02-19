package no.nav.aap.behandlingsflyt.behandling.etableringegenvirksomhet

import no.nav.aap.behandlingsflyt.behandling.underveis.regler.Hverdager.Companion.antallHverdager
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.bistand.BistandRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.etableringegenvirksomhet.EierVirksomhet
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.etableringegenvirksomhet.EtableringEgenVirksomhetRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.etableringegenvirksomhet.EtableringEgenVirksomhetVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykdomRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.tidslinje.orEmpty
import no.nav.aap.komponenter.tidslinje.somTidslinje
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.lookup.repository.RepositoryProvider
import java.time.LocalDate
import kotlin.collections.orEmpty
import kotlin.collections.plus

class EtableringEgenVirksomhetService(
    private val etableringEgenVirksomhetRepository: EtableringEgenVirksomhetRepository,
    private val behandlingRepository: BehandlingRepository,
    private val bistandRepository: BistandRepository,
    private val sykdomRepository: SykdomRepository,
    private val sakRepository: SakRepository,
) {
    constructor(repositoryProvider: RepositoryProvider) : this(
        etableringEgenVirksomhetRepository = repositoryProvider.provide(),
        behandlingRepository = repositoryProvider.provide(),
        bistandRepository = repositoryProvider.provide(),
        sykdomRepository = repositoryProvider.provide(),
        sakRepository = repositoryProvider.provide()
    )

    private val maksUtviklingsdager = 131
    private val maksOppstartsdager = 66

    fun erVurderingerGyldig(
        behandlingId: BehandlingId,
        nyeVurderinger: List<EtableringEgenVirksomhetVurdering>
    ): VirksomhetEtableringGyldig {
        val behandling = behandlingRepository.hent(behandlingId)

        val gamleVurderinger =
            behandling.forrigeBehandlingId?.let { etableringEgenVirksomhetRepository.hentHvisEksisterer(it) }?.vurderinger.orEmpty()
        val alleVurderinger = gamleVurderinger + nyeVurderinger
        val rettighetsperiode = sakRepository.hent(behandling.sakId).rettighetsperiode

        val gyldighetPeriode =
            utledGyldighetsPeriode(behandlingId, rettighetsperiode.fom.plusDays(1))
        if (gyldighetPeriode.isEmpty()) {
            return VirksomhetEtableringGyldig(
                false,
                "11-5 & 11-6b må være oppfylt i minst én periode"
            )
        }

        if (nyeVurderinger.any { vurdering ->
                gyldighetPeriode.none { gyldighetPeriode -> gyldighetPeriode.inneholder(vurdering.vurderingenGjelderFra) }
            }
        ) {
            return VirksomhetEtableringGyldig(
                false,
                "Vurderte perioder må falle innen en periode med oppfylt 11-5 & 11-6b")
        }

        val førsteMuligeDato = gyldighetPeriode.first().fom

        if (nyeVurderinger.any { evaluerVirksomhetVurdering(it) && it.utviklingsPerioder.isEmpty() && it.oppstartsPerioder.isEmpty() }) {
            return VirksomhetEtableringGyldig(
                false,
                "Må ha definert minst én periode i tidsplanen dersom vilkåret er oppfylt for en periode"
            )
        }

        if (alleVurderinger.none { it.vurderingenGjelderFra.isAfter(førsteMuligeDato) }) {
            return VirksomhetEtableringGyldig(
                false,
                "vurderingenGjelderFra må være minst én dag etter første mulige dag med AAP"
            )
        }

        val alleUtviklingsPerioder = alleVurderinger.flatMap { it.utviklingsPerioder }
        val alleOppstartsPerioder = alleVurderinger.flatMap { it.oppstartsPerioder }

        alleUtviklingsPerioder.maxOfOrNull { uPeriode -> uPeriode.tom }.let {
            if (alleOppstartsPerioder.any { oPeriode -> oPeriode.fom.isBefore(it) }) {
                return VirksomhetEtableringGyldig(
                    false,
                    "Oppstartsperioder kan ikke ligge før en utviklingsperiode"
                )
            }
        }

        val bruktUtviklingsDager =
            (alleVurderinger).flatMap { it.utviklingsPerioder }.somTidslinje { it }.komprimer().segmenter()
                .sumOf { it.periode.antallHverdager().asInt }
        val bruktOppstartsdager =
            (alleVurderinger).flatMap { it.oppstartsPerioder }.somTidslinje { it }.komprimer().segmenter()
                .sumOf { it.periode.antallHverdager().asInt }

        if (bruktUtviklingsDager > maksUtviklingsdager) {
            return VirksomhetEtableringGyldig(
                false,
                "Oppsatte utviklingsdager overstiger gjenværende dager: $bruktUtviklingsDager / $maksUtviklingsdager"
            )
        }
        if (bruktOppstartsdager > maksOppstartsdager)
            return VirksomhetEtableringGyldig(
                false,
                "Oppsatte oppstartsdager overstiger gjenværende dager: $bruktOppstartsdager / $maksOppstartsdager"
            )

        return VirksomhetEtableringGyldig(true)
    }

    fun evaluerVirksomhetVurdering(vurdering: EtableringEgenVirksomhetVurdering): Boolean {
        return vurdering.virksomhetErNy == true && vurdering.kanFøreTilSelvforsørget == true && vurdering.foreliggerFagligVurdering && vurdering.brukerEierVirksomheten in listOf(
            EierVirksomhet.EIER_MINST_50_PROSENT,
            EierVirksomhet.EIER_MINST_50_PROSENT_MED_FLER
        )
    }

    fun utledGyldighetsPeriode(
        behandlingId: BehandlingId,
        fom: LocalDate
    ): List<Periode> {
        val sykdomGrunnlag = sykdomRepository.hentHvisEksisterer(behandlingId)
        val bistandGrunnlag = bistandRepository.hentHvisEksisterer(behandlingId)

        val sykdomsvurderinger = sykdomGrunnlag?.somSykdomsvurderingstidslinje().orEmpty()
        val bistandsvurderinger =
            bistandGrunnlag?.somBistandsvurderingstidslinje().orEmpty()

        val mapped = Tidslinje.zip2(sykdomsvurderinger, bistandsvurderinger)
            .filter {
                it.verdi.first?.erOppfyltForYrkesskadeSettBortIfraÅrsakssammenheng(
                    fom,
                    it.periode
                ) == true && it.verdi.second?.erBehovForArbeidsrettetTiltak == true
            }
        return mapped.perioder().toList()
    }
}

data class VirksomhetEtableringGyldig(
    val erOppfylt: Boolean,
    val feilmelding: String? = null
)