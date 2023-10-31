package no.nav.aap.behandlingsflyt.faktagrunnlag.yrkesskade

import no.nav.aap.behandlingsflyt.dbstuff.DBConnection
import no.nav.aap.behandlingsflyt.faktagrunnlag.Grunnlag
import no.nav.aap.behandlingsflyt.flyt.FlytKontekst
import no.nav.aap.behandlingsflyt.sak.SakService

class YrkesskadeService : Grunnlag {

    override fun oppdater(transaksjonsconnection: DBConnection, kontekst: FlytKontekst): Boolean {
        val sakService = SakService(transaksjonsconnection)

        val sak = sakService.hent(kontekst.sakId)
        val yrkesskadePeriode = YrkesskadeRegisterMock.innhent(sak.person.identer(), sak.rettighetsperiode)

        val behandlingId = kontekst.behandlingId
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
}
