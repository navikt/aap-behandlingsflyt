package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarBistandsbehovLøsning
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.bistand.BistandGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.bistand.BistandRepository
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.tidslinje.StandardSammenslåere
import no.nav.aap.komponenter.tidslinje.orEmpty
import no.nav.aap.lookup.repository.RepositoryProvider
import java.time.LocalDate

class AvklarBistandLøser(
    private val sakRepository: SakRepository,
    private val bistandRepository: BistandRepository,
) : AvklaringsbehovsLøser<AvklarBistandsbehovLøsning> {

    constructor(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider) : this(
        sakRepository = repositoryProvider.provide(),
        bistandRepository = repositoryProvider.provide(),
    )

    override fun løs(
        kontekst: AvklaringsbehovKontekst,
        løsning: AvklarBistandsbehovLøsning
    ): LøsningsResultat {
        val sak = sakRepository.hent(kontekst.kontekst.sakId)
        val forrigeBehandlingId = kontekst.kontekst.forrigeBehandlingId

        val forrigeVedtatteGrunnlag = forrigeBehandlingId
            ?.let { bistandRepository.hentHvisEksisterer(it) }
        val vedtatteVurderinger = forrigeVedtatteGrunnlag?.vurderinger.orEmpty()
        
        val gjeldendeBistandstidslinje = forrigeVedtatteGrunnlag
            ?.somBistandsvurderingstidslinje(LocalDate.MIN)
            .orEmpty()

        val bistandsVurdering = løsning.bistandsVurdering.tilBistandVurdering(kontekst.bruker)

        val ny = bistandsVurdering.let {
            BistandGrunnlag(
                vurderinger = listOf(it),
            ).somBistandsvurderingstidslinje(LocalDate.MIN)
        }

        val gjeldende = gjeldendeBistandstidslinje
            .kombiner(ny, StandardSammenslåere.prioriterHøyreSideCrossJoin())

        løsning.bistandsVurdering.valider(gjeldende, sak.rettighetsperiode)

        bistandRepository.lagre(
            behandlingId = kontekst.behandlingId(),
            bistandsvurderinger = vedtatteVurderinger + bistandsVurdering
        )

        return LøsningsResultat(
            begrunnelse = bistandsVurdering.begrunnelse
        )
    }

    override fun forBehov(): Definisjon {
        return Definisjon.AVKLAR_BISTANDSBEHOV
    }
}
