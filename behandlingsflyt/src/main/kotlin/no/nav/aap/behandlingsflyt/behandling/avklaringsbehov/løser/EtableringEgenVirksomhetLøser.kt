package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.EtableringEgenVirksomhetLøsning
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.bistand.BistandGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.bistand.BistandRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.etableringegenvirksomhet.EtableringEgenVirksomhetRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykdomGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykdomRepository
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.tidslinje.orEmpty
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.lookup.repository.RepositoryProvider
import java.time.LocalDate
import kotlin.collections.orEmpty

class EtableringEgenVirksomhetLøser(
    private val etableringEgenVirksomhetRepository: EtableringEgenVirksomhetRepository,
    private val behandlingRepository: BehandlingRepository,
    private val bistandRepository: BistandRepository,
    private val sykdomRepository: SykdomRepository,
) : AvklaringsbehovsLøser<EtableringEgenVirksomhetLøsning> {

    constructor(repositoryProvider: RepositoryProvider) : this(
        etableringEgenVirksomhetRepository = repositoryProvider.provide(),
        behandlingRepository = repositoryProvider.provide(),
        bistandRepository = repositoryProvider.provide(),
        sykdomRepository = repositoryProvider.provide()
    )

    override fun løs(
        kontekst: AvklaringsbehovKontekst,
        løsning: EtableringEgenVirksomhetLøsning
    ): LøsningsResultat {
        val behandling = behandlingRepository.hent(kontekst.kontekst.behandlingId)

        val sykdomGrunnlag = sykdomRepository.hentHvisEksisterer(behandling.id)
        val bistandGrunnlag = bistandRepository.hentHvisEksisterer(behandling.id)

        val førsteMuligeDato =
            utledGyldighetsPeriode(sykdomGrunnlag, bistandGrunnlag, LocalDate.now().plusDays(1)).first().fom

        val nyeVurderinger = løsning.løsningerForPerioder.map { it.toEtableringEgenVirksomhetVurdering(kontekst) }

        require(nyeVurderinger.all { it.vurderingenGjelderFra.isAfter(førsteMuligeDato) })

        val gamleVurderinger =
            behandling.forrigeBehandlingId?.let { etableringEgenVirksomhetRepository.hentHvisEksisterer(it) }?.vurderinger.orEmpty()

        etableringEgenVirksomhetRepository.lagre(
            behandlingId = behandling.id,
            etableringEgenvirksomhetVurderinger = gamleVurderinger + nyeVurderinger
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