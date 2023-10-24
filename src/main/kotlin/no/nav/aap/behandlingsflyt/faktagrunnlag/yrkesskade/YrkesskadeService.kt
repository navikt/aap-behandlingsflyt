package no.nav.aap.behandlingsflyt.faktagrunnlag.yrkesskade

import no.nav.aap.behandlingsflyt.domene.person.Personlager
import no.nav.aap.behandlingsflyt.domene.sak.Sakslager
import no.nav.aap.behandlingsflyt.faktagrunnlag.Grunnlag
import no.nav.aap.behandlingsflyt.flyt.FlytKontekst

class YrkesskadeService : Grunnlag<YrkesskadeGrunnlag> {

    override fun oppdater(kontekst: FlytKontekst): Boolean {
        val sak = Sakslager.hent(kontekst.sakId)
        val person = Personlager.hent(sak.person.identifikator)
        val behandlingId = kontekst.behandlingId

        val yrkesskadePeriode = YrkesskadeRegisterMock.innhent(person.identer(), sak.rettighetsperiode)

        val gamleData = YrkesskadeRepository.hentHvisEksisterer(behandlingId)

        if (yrkesskadePeriode.isNotEmpty()) {
            YrkesskadeRepository.lagre(
                behandlingId,
                Yrkesskader(yrkesskadePeriode.map { periode -> Yrkesskade("ASDF", periode) })
            )
        } else if (YrkesskadeRepository.hentHvisEksisterer(behandlingId) != null) {
            YrkesskadeRepository.lagre(behandlingId, null)
        }
        val nyeData = YrkesskadeRepository.hentHvisEksisterer(behandlingId)

        return nyeData == gamleData
    }

    override fun hent(kontekst: FlytKontekst): YrkesskadeGrunnlag? {
        return YrkesskadeRepository.hentHvisEksisterer(kontekst.behandlingId)
    }
}
