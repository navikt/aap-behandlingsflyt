package no.nav.aap.behandlingsflyt.faktagrunnlag.personopplysninger

import no.nav.aap.behandlingsflyt.domene.person.Personlager
import no.nav.aap.behandlingsflyt.domene.sak.Sakslager
import no.nav.aap.behandlingsflyt.faktagrunnlag.Grunnlag
import no.nav.aap.behandlingsflyt.flyt.FlytKontekst
import no.nav.aap.behandlingsflyt.grunnlag.person.PersonRegisterMock
import no.nav.aap.behandlingsflyt.grunnlag.person.PersoninfoGrunnlag
import no.nav.aap.behandlingsflyt.grunnlag.person.PersoninformasjonTjeneste

class Personopplysning : Grunnlag<PersoninfoGrunnlag> {

    override fun oppdater(kontekst: FlytKontekst): Boolean {
        val sak = Sakslager.hent(kontekst.sakId)
        val person = Personlager.hent(sak.person.identifikator)
        val behandlingId = kontekst.behandlingId

        val gamleData = PersoninformasjonTjeneste.hentHvisEksisterer(behandlingId)

        val personopplysninger = PersonRegisterMock.innhent(person.identer())
        if (personopplysninger.size != 1) {
            throw IllegalStateException("fant flere personer enn forventet")
        }

        PersoninformasjonTjeneste.lagre(behandlingId, personopplysninger.first())
        val nyeData = PersoninformasjonTjeneste.hentHvisEksisterer(behandlingId)

        return nyeData == gamleData
    }

    override fun hent(kontekst: FlytKontekst): PersoninfoGrunnlag? {
        return PersoninformasjonTjeneste.hentHvisEksisterer(kontekst.behandlingId)
    }
}
