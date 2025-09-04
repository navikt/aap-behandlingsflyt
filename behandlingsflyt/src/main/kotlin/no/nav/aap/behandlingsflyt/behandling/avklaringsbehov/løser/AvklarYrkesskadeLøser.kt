package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarYrkesskadeLøsning
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.yrkesskade.YrkesskadeRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykdomRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.Yrkesskadevurdering
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
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

        // TODO Midlertidig sjekk for å ikke kunne velge saker hvor skadedato mangler - mulighet for dette må implementeres
        løsning.yrkesskadesvurdering.relevanteSaker.forEach { sakRef ->
            yrkesskadeGrunnlag?.yrkesskader?.yrkesskader?.forEach { ys ->
                if (sakRef == ys.ref && ys.skadedato == null) {
                    throw IllegalArgumentException("Skadedato må være satt for yrkesskade med referanse $sakRef - dette er ikke støttet enda")
                }
            }
        }

        sykdomRepository.lagre(
            behandlingId = behandling.id,
            yrkesskadevurdering = Yrkesskadevurdering(
                begrunnelse = løsning.yrkesskadesvurdering.begrunnelse,
                relevanteSaker = løsning.yrkesskadesvurdering.relevanteSaker,
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
