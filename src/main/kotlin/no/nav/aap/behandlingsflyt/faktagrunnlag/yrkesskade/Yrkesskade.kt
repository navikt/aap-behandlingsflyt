package no.nav.aap.behandlingsflyt.faktagrunnlag.yrkesskade

import no.nav.aap.behandlingsflyt.domene.person.Personlager
import no.nav.aap.behandlingsflyt.domene.sak.Sakslager
import no.nav.aap.behandlingsflyt.faktagrunnlag.Grunnlag
import no.nav.aap.behandlingsflyt.flyt.FlytKontekst
import no.nav.aap.behandlingsflyt.grunnlag.yrkesskade.Yrkesskade
import no.nav.aap.behandlingsflyt.grunnlag.yrkesskade.YrkesskadeGrunnlag
import no.nav.aap.behandlingsflyt.grunnlag.yrkesskade.YrkesskadeRegisterMock
import no.nav.aap.behandlingsflyt.grunnlag.yrkesskade.YrkesskadeTjeneste
import no.nav.aap.behandlingsflyt.grunnlag.yrkesskade.Yrkesskader

class Yrkesskade : Grunnlag<YrkesskadeGrunnlag> {

    override fun oppdater(kontekst: FlytKontekst): Boolean {
        val sak = Sakslager.hent(kontekst.sakId)
        val person = Personlager.hent(sak.person.identifikator)
        val behandlingId = kontekst.behandlingId

        val yrkesskadePeriode = YrkesskadeRegisterMock.innhent(person.identer(), sak.rettighetsperiode)

        val gamleData = YrkesskadeTjeneste.hentHvisEksisterer(behandlingId)

        if (yrkesskadePeriode.isNotEmpty()) {
            YrkesskadeTjeneste.lagre(
                behandlingId,
                Yrkesskader(yrkesskadePeriode.map { periode -> Yrkesskade("ASDF", periode) })
            )
        } else if (YrkesskadeTjeneste.hentHvisEksisterer(behandlingId) != null) {
            YrkesskadeTjeneste.lagre(behandlingId, null)
        }
        val nyeData = YrkesskadeTjeneste.hentHvisEksisterer(behandlingId)

        return nyeData == gamleData
    }

    override fun hent(kontekst: FlytKontekst): YrkesskadeGrunnlag? {
        return YrkesskadeTjeneste.hentHvisEksisterer(kontekst.behandlingId)
    }
}
