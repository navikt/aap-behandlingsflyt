package no.nav.aap.behandlingsflyt.flyt.steg

import no.nav.aap.behandlingsflyt.domene.person.Personlager
import no.nav.aap.behandlingsflyt.domene.sak.Sakslager
import no.nav.aap.behandlingsflyt.flyt.StegType
import no.nav.aap.behandlingsflyt.grunnlag.yrkesskade.Yrkesskade
import no.nav.aap.behandlingsflyt.grunnlag.yrkesskade.YrkesskadeRegisterMock
import no.nav.aap.behandlingsflyt.grunnlag.yrkesskade.YrkesskadeTjeneste
import no.nav.aap.behandlingsflyt.grunnlag.yrkesskade.Yrkesskader

class InnhentYrkesskadeSteg : BehandlingSteg {

    override fun utfør(input: StegInput): StegResultat {
        val sak = Sakslager.hent(input.kontekst.sakId)
        val person = Personlager.hent(sak.person.identifikator)

        val yrkesskadePeriode = YrkesskadeRegisterMock.innhent(person.identer(), sak.rettighetsperiode)

        val behandlingId = input.kontekst.behandlingId
        if (yrkesskadePeriode.isNotEmpty()) {
            YrkesskadeTjeneste.lagre(
                behandlingId,
                Yrkesskader(yrkesskadePeriode.map { periode -> Yrkesskade("ASDF", periode) })
            )
        } else if (YrkesskadeTjeneste.hentHvisEksisterer(behandlingId) != null) {
            YrkesskadeTjeneste.lagre(behandlingId, null)
        }

        return StegResultat() // DO NOTHING
    }

    override fun type(): StegType {
        return StegType.INNHENT_YRKESSKADE
    }
}
