package no.nav.aap.behandlingsflyt.flyt.steg

import no.nav.aap.behandlingsflyt.grunnlag.person.PersonRegisterMock
import no.nav.aap.behandlingsflyt.grunnlag.person.PersoninformasjonTjeneste
import no.nav.aap.behandlingsflyt.grunnlag.yrkesskade.Yrkesskade
import no.nav.aap.behandlingsflyt.grunnlag.yrkesskade.YrkesskadeRegisterMock
import no.nav.aap.behandlingsflyt.grunnlag.yrkesskade.YrkesskadeTjeneste
import no.nav.aap.behandlingsflyt.grunnlag.yrkesskade.Yrkesskader
import no.nav.aap.behandlingsflyt.domene.person.Personlager
import no.nav.aap.behandlingsflyt.domene.sak.Sakslager
import no.nav.aap.behandlingsflyt.flyt.StegType

class InnhentRegisterdataSteg : BehandlingSteg {

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

        val personopplysninger = PersonRegisterMock.innhent(person.identer())
        if (personopplysninger.size != 1) {
            throw IllegalStateException("fant flere personer enn forventet")
        }

        PersoninformasjonTjeneste.lagre(behandlingId, personopplysninger.first())

        return StegResultat() // DO NOTHING
    }

    override fun type(): StegType {
        return StegType.INNHENT_REGISTERDATA
    }
}
