package no.nav.aap.behandlingsflyt.behandling.etableringegenvirksomhet

import no.nav.aap.behandlingsflyt.behandling.underveis.regler.Hverdager.Companion.antallHverdager
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.bistand.BistandRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.bistand.Bistandsvurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.etableringegenvirksomhet.EierVirksomhet
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.etableringegenvirksomhet.EtableringEgenVirksomhetRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.etableringegenvirksomhet.EtableringEgenVirksomhetVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.etableringegenvirksomhet.EtableringFase
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.etableringegenvirksomhet.beregnTomForSistePeriode
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.etableringegenvirksomhet.justerEtableringPerioder
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.gjeldendeVurderinger
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykdomRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.Sykdomsvurdering
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.tidslinje.orEmpty
import no.nav.aap.komponenter.tidslinje.somTidslinje
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.lookup.repository.RepositoryProvider
import org.slf4j.LoggerFactory


class EtableringEgenVirksomhetService(
    private val etableringEgenVirksomhetRepository: EtableringEgenVirksomhetRepository,
    private val behandlingRepository: BehandlingRepository,
    private val bistandRepository: BistandRepository,
    private val sykdomRepository: SykdomRepository,

) {
    private val log = LoggerFactory.getLogger(javaClass)

    constructor(repositoryProvider: RepositoryProvider) : this(
        etableringEgenVirksomhetRepository = repositoryProvider.provide(),
        behandlingRepository = repositoryProvider.provide(),
        bistandRepository = repositoryProvider.provide(),
        sykdomRepository = repositoryProvider.provide()
    )

    private val maksUtviklingsdager = 131
    private val maksOppstartsdager = 66

    fun validerFaseOgPeriode(
        vurdering: EtableringEgenVirksomhetVurdering,
        historikk: List<EtableringEgenVirksomhetVurdering>
    ) {
        val fase = vurdering.fase ?: return

        if (fase == EtableringFase.OPPSTART) {
            require(vurdering.erRegistrertINødvendigeOffentligeRegister == true) {
                "Må ha satt om virksomheten er registrert i nødvendige offentlige register for oppstartsfasen"
            }

            val sisteUtviklingsVurdering = historikk.filter { it.fase == EtableringFase.UTVIKLING }.maxByOrNull { it.fom }
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
        val behandling = behandlingRepository.hent(behandlingId)
        val justerteVurderinger = justerEtableringPerioder(nyeVurderinger)

        val gamleVurderinger =
            justerEtableringPerioder(
                behandling.forrigeBehandlingId
                    ?.let { etableringEgenVirksomhetRepository.hentHvisEksisterer(it) }
                    ?.vurderinger
                    .orEmpty())

        val beregnedeVurderinger = justerteVurderinger.map { vurdering ->
            vurdering.takeIf { it.tom != null }
                ?: vurdering.copy(
                    tom = beregnTomForSistePeriode(
                        vurderinger = gamleVurderinger + justerteVurderinger,
                        sisteVurdering = vurdering
                    )
                )
        }

        log.info("Gamle vurderinger: {}", gamleVurderinger)
        log.info("Justerte vurderinger: {}", justerteVurderinger)
        log.info("Beregnede vurderinger: {}", beregnedeVurderinger)

        val gjeldendeVurderinger = (gamleVurderinger + beregnedeVurderinger)
            .gjeldendeVurderinger()
            .verdier().toSet()

        val gyldighetPeriode = utledGyldighetsPeriode(behandlingId)
        if (gyldighetPeriode.isEmpty()) {
            return VirksomhetEtableringIkkeGyldig(
                "11-5 & 11-6b må være oppfylt i minst én periode"
            )
        }

        if (beregnedeVurderinger.any { vurdering ->
                gyldighetPeriode.none { gyldighetPeriode -> gyldighetPeriode.inneholder(vurdering.fom) }
            }
        ) {
            return VirksomhetEtableringIkkeGyldig(
                "Vurderte perioder må falle innen en periode med oppfylt 11-5 & 11-6b"
            )
        }

        val førsteMuligeDato = gyldighetPeriode.first().fom

        if (gjeldendeVurderinger.isNotEmpty() && gjeldendeVurderinger.none { it.fom.isAfter(førsteMuligeDato) }) {
            return VirksomhetEtableringIkkeGyldig(
                "Vurderingen kan tidligst gjelde fra dagen etter første mulige dag med AAP"
            )
        }

        beregnedeVurderinger.forEach { vurdering ->
            val historikk = (gamleVurderinger + beregnedeVurderinger)
                .filter { it != vurdering }

            try {
                validerFaseOgPeriode(vurdering, historikk)
            } catch (e: IllegalArgumentException) {
                return VirksomhetEtableringIkkeGyldig(
                    e.message ?: "Ugyldig fase-/periode-konfigurasjon"
                )
            }
        }

        val alleUtviklingsPerioder = gjeldendeVurderinger.flatMap {
            if (it.tom != null && it.fase == EtableringFase.UTVIKLING) {
                listOf(Periode(it.fom, it.tom))
            } else {
                emptyList()
            }
        }

        val alleOppstartsPerioder = gjeldendeVurderinger.flatMap {
            if (it.tom != null && it.fase == EtableringFase.OPPSTART) {
                listOf(Periode(it.fom, it.tom))
            } else {
                emptyList()
            }
        }

        val sisteUtviklingsPeriodeTom = alleUtviklingsPerioder.maxOfOrNull { it.tom }
        if (sisteUtviklingsPeriodeTom != null && alleOppstartsPerioder.any { it.fom.isBefore(sisteUtviklingsPeriodeTom) }) {
            return VirksomhetEtableringIkkeGyldig(
                "Oppstartsperiode kan ikke ligge før en utviklingsperiode"
            )
        }

        val bruktUtviklingsDager =
            alleUtviklingsPerioder.somTidslinje { it }.komprimer().segmenter()
                .sumOf { it.periode.antallHverdager().asInt }
        val bruktOppstartsdager =
            alleOppstartsPerioder.somTidslinje { it }.komprimer().segmenter()
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
        return vurdering.fase != null && vurdering.virksomhetErNy == true && vurdering.kanFøreTilSelvforsørget == true && vurdering.foreliggerFagligVurdering && vurdering.brukerEierVirksomheten in listOf(
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