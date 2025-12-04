package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarOvergangUføreLøsning
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.overgangufore.OvergangUføreGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.overgangufore.OvergangUføreRepository
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.komponenter.tidslinje.StandardSammenslåere
import no.nav.aap.komponenter.tidslinje.orEmpty
import no.nav.aap.lookup.repository.RepositoryProvider
import java.time.LocalDate

class AvklarOvergangUføreLøser(
    private val behandlingRepository: BehandlingRepository,
    private val overgangUforeRepository: OvergangUføreRepository,
    private val sakRepository: SakRepository,
) : AvklaringsbehovsLøser<AvklarOvergangUføreLøsning> {

    constructor(repositoryProvider: RepositoryProvider) : this(
        behandlingRepository = repositoryProvider.provide(),
        overgangUforeRepository = repositoryProvider.provide(),
        sakRepository = repositoryProvider.provide(),
    )


    override fun løs(
        kontekst: AvklaringsbehovKontekst,
        løsning: AvklarOvergangUføreLøsning
    ): LøsningsResultat {

        val behandling = behandlingRepository.hent(kontekst.kontekst.behandlingId)
        
        val rettighetsperiode = sakRepository.hent(behandling.sakId).rettighetsperiode
        
        val overgangUføreVurdering =
            løsning.overgangUføreVurdering.tilOvergangUføreVurdering(kontekst.bruker, rettighetsperiode.fom, kontekst.behandlingId())

        val eksisterendeOverganguforevurderinger = behandling.forrigeBehandlingId
            ?.let { overgangUforeRepository.hentHvisEksisterer(it) }
            ?.somOvergangUforevurderingstidslinje(LocalDate.MIN)
            .orEmpty()

        val ny = overgangUføreVurdering.let {
            OvergangUføreGrunnlag(
                id = null,
                vurderinger = listOf(it),
            ).somOvergangUforevurderingstidslinje(LocalDate.MIN)
        }

        val gjeldende = eksisterendeOverganguforevurderinger
            .kombiner(ny, StandardSammenslåere.prioriterHøyreSideCrossJoin())
            .segmenter().map { it.verdi }

        overgangUforeRepository.lagre(
            behandlingId = behandling.id,
            overgangUføreVurderinger = gjeldende
        )

        return LøsningsResultat(
            begrunnelse = overgangUføreVurdering.begrunnelse
        )
    }

    override fun forBehov(): Definisjon {
        return Definisjon.AVKLAR_OVERGANG_UFORE
    }
}
