package no.nav.aap.behandlingsflyt.flyt.steg

import no.nav.aap.behandlingsflyt.domene.person.Personlager
import no.nav.aap.behandlingsflyt.domene.sak.Sakslager
import no.nav.aap.behandlingsflyt.flyt.StegType
import no.nav.aap.behandlingsflyt.grunnlag.person.PersonRegisterMock
import no.nav.aap.behandlingsflyt.grunnlag.person.PersoninformasjonTjeneste

class InnhentPersonopplysningerSteg : BehandlingSteg {

    override fun utfør(input: StegInput): StegResultat {
        val sak = Sakslager.hent(input.kontekst.sakId)
        val person = Personlager.hent(sak.person.identifikator)
        val behandlingId = input.kontekst.behandlingId

        val personopplysninger = PersonRegisterMock.innhent(person.identer())
        if (personopplysninger.size != 1) {
            throw IllegalStateException("fant flere personer enn forventet")
        }

        PersoninformasjonTjeneste.lagre(behandlingId, personopplysninger.first())

        return StegResultat() // DO NOTHING
    }

    override fun type(): StegType {
        return StegType.INNHENT_PERSONOPPLYSNINGER
    }
}
