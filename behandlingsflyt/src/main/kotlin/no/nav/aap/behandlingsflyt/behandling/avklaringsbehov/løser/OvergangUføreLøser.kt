package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarOvergangUføreLøsning
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.overgangufore.OvergangUføreGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.overgangufore.OvergangUføreRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykdomRepository
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.komponenter.tidslinje.StandardSammenslåere
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.lookup.repository.RepositoryProvider
import java.time.LocalDate

class OvergangUføreLøser(
    private val behandlingRepository: BehandlingRepository,
    private val overgangUforeRepository: OvergangUføreRepository,
    private val sykdomRepository: SykdomRepository,
) : AvklaringsbehovsLøser<AvklarOvergangUføreLøsning> {

    constructor(repositoryProvider: RepositoryProvider) : this(
        behandlingRepository = repositoryProvider.provide(),
        overgangUforeRepository = repositoryProvider.provide(),
        sykdomRepository = repositoryProvider.provide(),
    )


    override fun løs(
        kontekst: AvklaringsbehovKontekst,
        løsning: AvklarOvergangUføreLøsning
    ): LøsningsResultat {
        løsning.overgangUføreVurdering.valider()
    
        val behandling = behandlingRepository.hent(kontekst.kontekst.behandlingId)

        val nyesteSykdomsvurdering = sykdomRepository.hentHvisEksisterer(behandling.id)
            ?.sykdomsvurderinger?.maxByOrNull { it.opprettet }

        val overgangUføreVurdering = løsning.overgangUføreVurdering.tilOvergangUføreVurdering(
            kontekst.bruker,
            nyesteSykdomsvurdering?.vurderingenGjelderFra
        )

        val eksisterendeOverganguforevurderinger = behandling.forrigeBehandlingId
            ?.let { overgangUforeRepository.hentHvisEksisterer(it) }
            ?.somOvergangUforevurderingstidslinje(LocalDate.MIN)
            ?: Tidslinje()

        val ny = overgangUføreVurdering.let {
            OvergangUføreGrunnlag(
                id = null,
                vurderinger = listOf(it),
            ).somOvergangUforevurderingstidslinje(LocalDate.MIN)
        }

        val gjeldende = eksisterendeOverganguforevurderinger
            .kombiner(ny, StandardSammenslåere.prioriterHøyreSideCrossJoin())
            .toList().map { it.verdi }

        overgangUforeRepository.lagre(
            behandlingId = behandling.id,
            bistandsvurderinger = gjeldende
        )

        return LøsningsResultat(
            begrunnelse = overgangUføreVurdering.begrunnelse
        )
    }

    override fun forBehov(): Definisjon {
        return Definisjon.OVERGANG_UFORE
    }
}
