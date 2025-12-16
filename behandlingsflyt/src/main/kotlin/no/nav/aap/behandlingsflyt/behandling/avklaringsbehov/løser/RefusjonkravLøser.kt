package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.RefusjonkravLøsning
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.refusjonkrav.RefusjonkravRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.refusjonkrav.RefusjonkravVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.refusjonkrav.RefusjonkravVurderingDto
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.behandlingsflyt.unleash.BehandlingsflytFeature
import no.nav.aap.behandlingsflyt.unleash.UnleashGateway
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider

class RefusjonkravLøser(
    private val refusjonkravRepository: RefusjonkravRepository,
    private val unleashGateway: UnleashGateway,
    private val sakRepository: SakRepository,
) : AvklaringsbehovsLøser<RefusjonkravLøsning> {

    constructor(
        repositoryProvider: RepositoryProvider,
        gatewayProvider: GatewayProvider
    ) : this(
        refusjonkravRepository = repositoryProvider.provide(),
        unleashGateway = gatewayProvider.provide<UnleashGateway>(),
        sakRepository = repositoryProvider.provide(),

        )

    override fun løs(kontekst: AvklaringsbehovKontekst, løsning: RefusjonkravLøsning): LøsningsResultat {

        if (unleashGateway.isEnabled(BehandlingsflytFeature.SosialRefusjon)) {
            val vurderinger = løsning.refusjonkravVurderinger.map { refusjonkrav ->
                RefusjonkravVurdering(
                    harKrav = refusjonkrav.harKrav,
                    navKontor = refusjonkrav.navKontor,
                    vurdertAv = kontekst.bruker.ident
                )
            }

            refusjonkravRepository.lagre(kontekst.kontekst.sakId, kontekst.behandlingId(), vurderinger)
            return LøsningsResultat("Vurdert refusjonskrav")

        } else {



            val vurderinger = validerRefusjonDatoer(kontekst, løsning).let {
                it.map { dto ->
                    RefusjonkravVurdering(
                        harKrav = dto.harKrav,
                        navKontor = dto.navKontor,
                        fom = dto.fom,
                        tom = dto.tom,
                        vurdertAv = kontekst.bruker.ident
                    )
                }

            }
            refusjonkravRepository.lagre(kontekst.kontekst.sakId, kontekst.behandlingId(), vurderinger)
            return LøsningsResultat("Vurdert refusjonskrav")
        }


    }

    override fun forBehov(): Definisjon {
        return Definisjon.REFUSJON_KRAV
    }

    fun validerRefusjonDatoer(
        kontekst: AvklaringsbehovKontekst,
        løsning: RefusjonkravLøsning
    ): List<RefusjonkravVurderingDto> {
        val sak = sakRepository.hent(kontekst.kontekst.sakId)
        val kravDato = sak.rettighetsperiode.fom

        return løsning.refusjonkravVurderinger.map { vurdering ->
            if (vurdering.harKrav) {
                val refusjonFomDato = vurdering.fom ?: kravDato
                val refusjonTomDato = vurdering.tom
                val navKontor = vurdering.navKontor
                if (refusjonFomDato.isBefore(kravDato)) {
                    throw IllegalArgumentException("Refusjonsdato kan ikke være før kravdato. Refusjonsdato: $refusjonFomDato, kravdato: $kravDato")
                }

                if (refusjonTomDato != null && refusjonFomDato.isAfter(refusjonTomDato)) {
                    throw IllegalArgumentException("Tom ($refusjonTomDato) er før fom ($refusjonFomDato)")
                }

                RefusjonkravVurderingDto(
                    harKrav = true,
                    fom = refusjonFomDato,
                    tom = refusjonTomDato,
                    navKontor = navKontor
                )
            } else {
                vurdering
            }
        }
    }

}
