package no.nav.aap.behandlingsflyt.flyt.steg.impl

import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.faktagrunnlag.student.db.InMemoryStudentRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.sykdom.SykdomsRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.yrkesskade.YrkesskadeService
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.StegType
import no.nav.aap.behandlingsflyt.sak.SakService

object VurderYrkesskadeÅrsakssammenhengFlytSteg : FlytSteg {

    override fun konstruer(connection: DBConnection): BehandlingSteg {
        return VurderYrkesskadeÅrsakssammenhengSteg(
            YrkesskadeService(),
            SykdomsRepository(connection),
            InMemoryStudentRepository,
            PeriodeTilVurderingService(SakService(connection))
        )
    }

    override fun type(): StegType {
        return StegType.AVKLAR_YRKESSKADE
    }
}
