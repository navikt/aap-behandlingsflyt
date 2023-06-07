package no.nav.aap.flyt.steg

import no.nav.aap.domene.behandling.grunnlag.yrkesskade.Yrkesskade
import no.nav.aap.domene.behandling.grunnlag.yrkesskade.YrkesskadeRegister
import no.nav.aap.domene.behandling.grunnlag.yrkesskade.YrkesskadeTjeneste
import no.nav.aap.domene.behandling.grunnlag.yrkesskade.Yrkesskader
import no.nav.aap.domene.fagsak.FagsakTjeneste
import no.nav.aap.domene.person.PersonTjeneste
import no.nav.aap.flyt.StegType

class InnhentRegisterdataSteg : BehandlingSteg {
    override fun utfør(input: StegInput): StegResultat {
        val fagsak = FagsakTjeneste.hent(input.kontekst.fagsakId)
        val person = PersonTjeneste.hent(fagsak.person.identifikator)

        val yrkesskadePeriode = YrkesskadeRegister.innhent(person.identer(), fagsak.rettighetsperiode)

        val behandlingId = input.kontekst.behandlingId
        if (yrkesskadePeriode.isNotEmpty()) {
            YrkesskadeTjeneste.lagre(
                behandlingId,
                Yrkesskader(yrkesskadePeriode.map { periode -> Yrkesskade("ASDF", periode) })
            )
        } else if (YrkesskadeTjeneste.hentHvisEksisterer(behandlingId).isPresent) {
            YrkesskadeTjeneste.lagre(behandlingId, null)
        }

        return StegResultat() // DO NOTHING
    }

    override fun type(): StegType {
        return StegType.INNHENT_REGISTERDATA
    }
}
