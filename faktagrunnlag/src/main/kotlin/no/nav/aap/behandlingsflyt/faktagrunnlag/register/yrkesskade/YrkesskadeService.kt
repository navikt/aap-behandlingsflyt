package no.nav.aap.behandlingsflyt.faktagrunnlag.register.yrkesskade

import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.faktagrunnlag.Grunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.Grunnlagkonstruktør
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.yrkesskade.adapter.FakeYrkesskadeRegisterGateway
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.verdityper.flyt.FlytKontekst
import no.nav.aap.verdityper.sakogbehandling.BehandlingId

class YrkesskadeService private constructor(
    private val sakService: SakService,
    private val yrkesskadeRepository: YrkesskadeRepository,
    private val yrkesskadeRegisterGateway: YrkesskadeRegisterGateway
) : Grunnlag {

    override fun oppdater(kontekst: FlytKontekst): Boolean {
        val sak = sakService.hent(kontekst.sakId)
        val yrkesskadePeriode = yrkesskadeRegisterGateway.innhent(sak.person.identer(), sak.rettighetsperiode)

        val behandlingId = kontekst.behandlingId
        val gamleData = yrkesskadeRepository.hentHvisEksisterer(behandlingId)

        if (yrkesskadePeriode.isNotEmpty()) {
            yrkesskadeRepository.lagre(
                behandlingId,
                Yrkesskader(yrkesskadePeriode.map { periode -> Yrkesskade("ASDF", periode) })
            )
        } else if (yrkesskadeRepository.hentHvisEksisterer(behandlingId) != null) {
            yrkesskadeRepository.lagre(behandlingId, null)
        }
        val nyeData = yrkesskadeRepository.hentHvisEksisterer(behandlingId)

        return nyeData == gamleData
    }

    fun hentHvisEksisterer(behandlingId: BehandlingId): YrkesskadeGrunnlag? {
        return yrkesskadeRepository.hentHvisEksisterer(behandlingId)
    }

    companion object : Grunnlagkonstruktør {
        override fun konstruer(connection: DBConnection): YrkesskadeService {
            return YrkesskadeService(
                SakService(connection),
                YrkesskadeRepository(connection),
                FakeYrkesskadeRegisterGateway
            )
        }
    }
}
