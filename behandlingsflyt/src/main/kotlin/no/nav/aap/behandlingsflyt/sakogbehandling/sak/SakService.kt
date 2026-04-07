package no.nav.aap.behandlingsflyt.sakogbehandling.sak

import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
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

    fun oppdaterRettighetsperioden(sakId: SakId, brevkategori: InnsendingType, mottattDato: LocalDate) {
        if (brevkategori == InnsendingType.SØKNAD) {
            val rettighetsperiode = sakRepository.hent(sakId).rettighetsperiode
            val fom = if (rettighetsperiode.fom.isAfter(mottattDato)) {
                mottattDato
            } else {
                rettighetsperiode.fom
            }
            val periode = Periode(
                fom,
                Tid.MAKS
            )
            if (periode != rettighetsperiode) {
                sakRepository.oppdaterRettighetsperiode(sakId, periode)
            }
        }
    }

    fun overstyrRettighetsperioden(sakId: SakId, startDato: LocalDate, sluttDato: LocalDate) {
        val rettighetsperiode = sakRepository.hent(sakId).rettighetsperiode
        val periode = Periode(
            startDato,
            sluttDato
        )
        if (periode != rettighetsperiode) {
            sakRepository.oppdaterRettighetsperiode(sakId, periode)
        }
    }

}
