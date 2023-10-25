package no.nav.aap.behandlingsflyt.faktagrunnlag.yrkesskade

import no.nav.aap.behandlingsflyt.dbstuff.DbConnection
import no.nav.aap.behandlingsflyt.sak.person.Personlager
import no.nav.aap.behandlingsflyt.sak.SakRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.Grunnlag
import no.nav.aap.behandlingsflyt.flyt.FlytKontekst

class YrkesskadeService : Grunnlag {

    override fun oppdater(transaksjonsconnection: DbConnection, kontekst: FlytKontekst): Boolean {
        val sak = SakRepository.hent(kontekst.sakId)
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
}
