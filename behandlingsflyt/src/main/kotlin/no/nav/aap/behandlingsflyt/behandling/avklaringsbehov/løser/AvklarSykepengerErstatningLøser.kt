package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarSykepengerErstatningLøsning
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykepengerErstatningGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykepengerErstatningRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykepengerVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.flate.SykepengerVurderingDto
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.unleash.BehandlingsflytFeature
import no.nav.aap.behandlingsflyt.unleash.UnleashGateway
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.tidslinje.StandardSammenslåere
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.lookup.repository.RepositoryProvider
import java.time.LocalDate

class AvklarSykepengerErstatningLøser(
    private val behandlingRepository: BehandlingRepository,
    private val sykepengerErstatningRepository: SykepengerErstatningRepository,
    private val unleashGateway: UnleashGateway
) : AvklaringsbehovsLøser<AvklarSykepengerErstatningLøsning> {

    constructor(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider) : this(
        behandlingRepository = repositoryProvider.provide(),
        sykepengerErstatningRepository = repositoryProvider.provide(),
        unleashGateway = gatewayProvider.provide()
    )

    override fun løs(
        kontekst: AvklaringsbehovKontekst,
        løsning: AvklarSykepengerErstatningLøsning
    ): LøsningsResultat {
        val behandling = behandlingRepository.hent(kontekst.kontekst.behandlingId)

        if (unleashGateway.isEnabled(BehandlingsflytFeature.SykepengerPeriodisert)) {

            val nyVurdering = tilVurdering(løsning.sykepengeerstatningVurdering, kontekst.bruker.ident)
                .let(::listOf)
                .let(::SykepengerErstatningGrunnlag)
                .somTidslinje(LocalDate.MIN, LocalDate.MAX)

            val eksisterendeVurderinger =
                behandling.forrigeBehandlingId?.let { sykepengerErstatningRepository.hentHvisEksisterer(it) }
                    ?.somTidslinje(LocalDate.MIN, LocalDate.MAX)
                    ?: Tidslinje()

            val gjeldendeVurderinger = eksisterendeVurderinger
                .kombiner(nyVurdering, StandardSammenslåere.prioriterHøyreSideCrossJoin())
                .komprimer()
                .segmenter()
                .map { it.verdi }


            sykepengerErstatningRepository.lagre(
                behandlingId = behandling.id,
                vurderinger = gjeldendeVurderinger
            )
        } else {
            sykepengerErstatningRepository.lagre(
                behandlingId = behandling.id,
                vurderinger = listOf(tilVurdering(løsning.sykepengeerstatningVurdering, kontekst.bruker.ident))
            )
        }

        return LøsningsResultat(
            begrunnelse = løsning.sykepengeerstatningVurdering.begrunnelse
        )
    }

    private fun tilVurdering(
        dto: SykepengerVurderingDto,
        vurdertAv: String
    ): SykepengerVurdering = SykepengerVurdering(
        begrunnelse = dto.begrunnelse,
        dokumenterBruktIVurdering = dto.dokumenterBruktIVurdering,
        harRettPå = dto.harRettPå,
        grunn = dto.grunn,
        vurdertAv = vurdertAv,
        gjelderFra = dto.gjelderFra
    )

    override fun forBehov(): Definisjon {
        return Definisjon.AVKLAR_SYKEPENGEERSTATNING
    }
}
