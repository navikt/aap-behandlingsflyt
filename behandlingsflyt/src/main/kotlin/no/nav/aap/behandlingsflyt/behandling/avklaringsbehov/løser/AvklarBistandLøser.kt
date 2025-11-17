package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarBistandsbehovLøsning
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.bistand.BistandGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.bistand.BistandRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykdomRepository
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.tidslinje.StandardSammenslåere
import no.nav.aap.komponenter.tidslinje.orEmpty
import no.nav.aap.lookup.repository.RepositoryProvider

class AvklarBistandLøser(
    private val behandlingRepository: BehandlingRepository,
    private val bistandRepository: BistandRepository,
    private val sykdomRepository: SykdomRepository,
) : AvklaringsbehovsLøser<AvklarBistandsbehovLøsning> {

    constructor(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider) : this(
        behandlingRepository = repositoryProvider.provide(),
        bistandRepository = repositoryProvider.provide(),
        sykdomRepository = repositoryProvider.provide(),
    )


    override fun løs(
        kontekst: AvklaringsbehovKontekst,
        løsning: AvklarBistandsbehovLøsning
    ): LøsningsResultat {
        løsning.bistandsVurdering.valider()

        val behandling = behandlingRepository.hent(kontekst.kontekst.behandlingId)

        val nyesteSykdomsvurdering = sykdomRepository.hent(behandling.id)
            .sykdomsvurderinger.maxBy { it.opprettet }

        val bistandsVurdering = løsning.bistandsVurdering.tilBistandVurdering(
            kontekst.bruker,
            nyesteSykdomsvurdering.vurderingenGjelderFra,  // TODO: Gjør uavhengig fra sykdom
            vurdertIBehandling = kontekst.behandlingId()
        )

        val eksisterendeBistandsvurderinger = behandling.forrigeBehandlingId
            ?.let { bistandRepository.hentHvisEksisterer(it) }
            ?.somBistandsvurderingstidslinje()
            .orEmpty()

        val ny = bistandsVurdering.let {
            BistandGrunnlag(
                vurderinger = listOf(it),
            ).somBistandsvurderingstidslinje()
        }

        val gjeldende = eksisterendeBistandsvurderinger
            .kombiner(ny, StandardSammenslåere.prioriterHøyreSideCrossJoin())
            .segmenter().map { it.verdi }

        bistandRepository.lagre(
            behandlingId = behandling.id,
            bistandsvurderinger = gjeldende
        )

        return LøsningsResultat(
            begrunnelse = bistandsVurdering.begrunnelse
        )
    }

    override fun forBehov(): Definisjon {
        return Definisjon.AVKLAR_BISTANDSBEHOV
    }
}
