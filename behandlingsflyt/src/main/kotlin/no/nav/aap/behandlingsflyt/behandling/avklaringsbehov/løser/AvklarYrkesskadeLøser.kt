package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarYrkesskadeLøsning
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.yrkesskade.YrkesskadeRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykdomRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.YrkesskadeSak
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.Yrkesskadevurdering
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.komponenter.httpklient.exception.UgyldigForespørselException
import no.nav.aap.komponenter.verdityper.Prosent
import no.nav.aap.lookup.repository.RepositoryProvider

class AvklarYrkesskadeLøser(
    private val behandlingRepository: BehandlingRepository,
    private val sykdomRepository: SykdomRepository,
    private val yrkesskadeRepository: YrkesskadeRepository,
) : AvklaringsbehovsLøser<AvklarYrkesskadeLøsning> {

    constructor(repositoryProvider: RepositoryProvider) : this(
        behandlingRepository = repositoryProvider.provide(),
        sykdomRepository = repositoryProvider.provide(),
        yrkesskadeRepository = repositoryProvider.provide(),
    )

    override fun løs(kontekst: AvklaringsbehovKontekst, løsning: AvklarYrkesskadeLøsning): LøsningsResultat {
        val behandling = behandlingRepository.hent(kontekst.kontekst.behandlingId)

        val yrkesskadeGrunnlag = yrkesskadeRepository.hentHvisEksisterer(behandling.id)

        løsning.yrkesskadesvurdering.relevanteSaker().forEach { sak ->
            yrkesskadeGrunnlag?.yrkesskader?.yrkesskader?.forEach { ys ->
                if (sak.referanse == ys.ref && ys.skadedato == null && sak.manuellYrkesskadeDato == null) {
                    throw UgyldigForespørselException("Skadedato må være satt for yrkesskade med referanse ${sak.referanse}.")
                }
                if (sak.referanse == ys.ref && ys.skadedato != null && sak.manuellYrkesskadeDato != null) {
                    throw UgyldigForespørselException("Kan ikke manuelt sette yrkesskadedato når det eksisterer dato i register for ${sak.referanse}")
                }
            }
        }

        sykdomRepository.lagre(
            behandlingId = behandling.id,
            yrkesskadevurdering = Yrkesskadevurdering(
                begrunnelse = løsning.yrkesskadesvurdering.begrunnelse,
                relevanteSaker = løsning.yrkesskadesvurdering.relevanteSaker().map {
                    YrkesskadeSak(
                        it.referanse,
                        it.manuellYrkesskadeDato
                    )
                },
                erÅrsakssammenheng = løsning.yrkesskadesvurdering.erÅrsakssammenheng,
                andelAvNedsettelsen = løsning.yrkesskadesvurdering.andelAvNedsettelsen?.let { Prosent(it) },
                vurdertAv = kontekst.bruker.ident,
            ),
        )

        return LøsningsResultat(
            begrunnelse = løsning.yrkesskadesvurdering.begrunnelse
        )
    }

    override fun forBehov(): Definisjon {
        return Definisjon.AVKLAR_YRKESSKADE
    }
}
