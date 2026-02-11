package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.EtableringEgenVirksomhetLøsning
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.Hverdager.Companion.antallHverdager
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.bistand.BistandGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.bistand.BistandRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.etableringegenvirksomhet.EtableringEgenVirksomhetRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykdomGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykdomRepository
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.unleash.BehandlingsflytFeature
import no.nav.aap.behandlingsflyt.unleash.UnleashGateway
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.tidslinje.orEmpty
import no.nav.aap.komponenter.tidslinje.somTidslinje
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.lookup.repository.RepositoryProvider
import java.time.LocalDate
import kotlin.collections.orEmpty

class EtableringEgenVirksomhetLøser(
    private val etableringEgenVirksomhetRepository: EtableringEgenVirksomhetRepository,
    private val behandlingRepository: BehandlingRepository,
    private val bistandRepository: BistandRepository,
    private val sykdomRepository: SykdomRepository,
    private val unleashGateway: UnleashGateway
) : AvklaringsbehovsLøser<EtableringEgenVirksomhetLøsning> {
    constructor(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider) : this(
        etableringEgenVirksomhetRepository = repositoryProvider.provide(),
        behandlingRepository = repositoryProvider.provide(),
        bistandRepository = repositoryProvider.provide(),
        sykdomRepository = repositoryProvider.provide(),
        unleashGateway = gatewayProvider.provide()
    )

    override fun løs(
        kontekst: AvklaringsbehovKontekst,
        løsning: EtableringEgenVirksomhetLøsning
    ): LøsningsResultat {
        if (unleashGateway.isDisabled(BehandlingsflytFeature.VirksomhetsEtablering)) {
            return LøsningsResultat(begrunnelse = "Vurdert etablering egen virksomhet")
        }

        val behandling = behandlingRepository.hent(kontekst.kontekst.behandlingId)

        val sykdomGrunnlag = sykdomRepository.hentHvisEksisterer(behandling.id)
        val bistandGrunnlag = bistandRepository.hentHvisEksisterer(behandling.id)

        val nyeVurderinger = løsning.løsningerForPerioder.map { it.toEtableringEgenVirksomhetVurdering(kontekst) }
        val gamleVurderinger =
            behandling.forrigeBehandlingId?.let { etableringEgenVirksomhetRepository.hentHvisEksisterer(it) }?.vurderinger.orEmpty()
        val alleVurderinger = gamleVurderinger + nyeVurderinger

        val førsteMuligeDato =
            utledGyldighetsPeriode(sykdomGrunnlag, bistandGrunnlag, LocalDate.now().plusDays(1)).first().fom

        require(nyeVurderinger.all { it.utviklingsPerioder.isNotEmpty() || it.oppstartsPerioder.isNotEmpty() }) {
            "Må ha definert minst én periode i tidsplanen dersom vilkåret er oppfylt for en periode"
        }

        require(alleVurderinger.all { it.vurderingenGjelderFra.isAfter(førsteMuligeDato) }) {
            "vurderingenGjelderFra må være minst én dag etter første mulige dag med AAP"
        }

        val alleUtviklingsPerioder = alleVurderinger.flatMap { it.utviklingsPerioder }
        val alleOppstartsPerioder = alleVurderinger.flatMap { it.oppstartsPerioder }

        alleUtviklingsPerioder.maxOf { uPeriode -> uPeriode.tom }.let {
            require(alleOppstartsPerioder.none { oPeriode -> oPeriode.fom.isBefore(it) }) {
                "Oppstartsperioder kan ikke ligge før en utviklingsperiode"
            }
        }

        // Finn et nice sted for disse
        val maksUtviklingsdager = 131
        val maksOppstartsdager = 66

        val bruktUtviklingsDager =
            (alleVurderinger).flatMap { it.utviklingsPerioder }.somTidslinje { it }.komprimer().segmenter()
                .sumOf { it.periode.antallHverdager().asInt }
        val bruktOppstartsdager =
            (alleVurderinger).flatMap { it.oppstartsPerioder }.somTidslinje { it }.komprimer().segmenter()
                .sumOf { it.periode.antallHverdager().asInt }

        require(bruktUtviklingsDager <= maksUtviklingsdager){
            "Oppsatte utviklingsdager overstiger gjenværende dager: $bruktUtviklingsDager / $maksUtviklingsdager"
        }
        require(bruktOppstartsdager <= maksOppstartsdager){
            "Oppsatte oppstartsdager overstiger gjenværende dager: $bruktOppstartsdager / $maksOppstartsdager"
        }

        etableringEgenVirksomhetRepository.lagre(
            behandlingId = behandling.id,
            etableringEgenvirksomhetVurderinger = alleVurderinger
        )
        return LøsningsResultat(begrunnelse = "Vurdert etablering egen virksomhet")
    }

    override fun forBehov(): Definisjon {
        return Definisjon.ETABLERING_EGEN_VIRKSOMHET
    }
}

private fun utledGyldighetsPeriode(
    sykdomGrunnlag: SykdomGrunnlag?,
    bistandGrunnlag: BistandGrunnlag?,
    fom: LocalDate
): List<Periode> {
    val sykdomsvurderinger = sykdomGrunnlag?.somSykdomsvurderingstidslinje().orEmpty()
    val bistandsvurderinger =
        bistandGrunnlag?.somBistandsvurderingstidslinje().orEmpty()

    val mapped = Tidslinje.zip2(sykdomsvurderinger, bistandsvurderinger)
        .filter {
            it.verdi.first?.erOppfyltOrdinær(
                fom,
                it.periode
            ) == true && it.verdi.second?.erBehovForArbeidsrettetTiltak == true
        }
    return mapped.perioder().toList()
}