package no.nav.aap.behandlingsflyt.sakogbehandling.sak

import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.utils.FarligMutering
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Tid
import no.nav.aap.lookup.repository.RepositoryProvider
import java.time.LocalDate

class SakService(private val sakRepository: SakRepository, private val behandlingRepository: BehandlingRepository) {
    constructor(repositoryProvider: RepositoryProvider, @Suppress("unused") gatewayProvider: GatewayProvider) : this(
        sakRepository = repositoryProvider.provide(),
        behandlingRepository = repositoryProvider.provide()
    )

    fun hent(sakId: SakId): Sak {
        return sakRepository.hent(sakId)
    }

    fun hent(saksnummer: Saksnummer): Sak {
        return sakRepository.hent(saksnummer)
    }

    fun hentSakFor(behandlingId: BehandlingId): Sak {
        val behandling = behandlingRepository.hent(behandlingId)
        return sakRepository.hent(behandling.sakId)
    }

    fun oppdaterRettighetsperioden(sak: Sak, brevkategori: InnsendingType, mottattDato: LocalDate) {
        if (brevkategori == InnsendingType.SØKNAD) {
            val sakMedOppdatertRettighetsperiode = sakRepository.hent(sak.id)
            val rettighetsperiode = sakMedOppdatertRettighetsperiode.rettighetsperiode
            val fom = if (rettighetsperiode.fom.isAfter(mottattDato)) {
                mottattDato
            } else {
                rettighetsperiode.fom
            }
            overstyrRettighetsperioden(sak, fom, Tid.MAKS)
        }
    }

    fun overstyrRettighetsperioden(sak: Sak, startDato: LocalDate, sluttDato: LocalDate) {
        val sakMedOppdatertRettighetsperiode = sakRepository.hent(sak.id)
        val rettighetsperiode = sakMedOppdatertRettighetsperiode.rettighetsperiode
        val periode = Periode(
            startDato,
            sluttDato
        )
        if (periode != rettighetsperiode) {
            @OptIn(FarligMutering::class)
            sakRepository.oppdaterRettighetsperiode(sak, periode)
            sak.rettighetsperiode = periode
        }
    }
}
