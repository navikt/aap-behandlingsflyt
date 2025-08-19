package no.nav.aap.behandlingsflyt.sakogbehandling.sak.flate

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovOperasjonerRepository
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.ÅrsakTilOpprettelse
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.komponenter.repository.RepositoryProvider

class SaksHistorikkService(
    val repositoryProvider: RepositoryProvider
) {

    fun utledSaksHistorikk(sakId: SakId): SaksHistorikkDTO {
        val alleBehandlinger = repositoryProvider.provide<BehandlingRepository>().hentAlleFor(sakId)

        val opprettelsesHendelser = utledOpprettelseHendelser(alleBehandlinger)
        val behandlingHendelser = utledBehandlingHendelser(alleBehandlinger)

        // Combine and sort the above

        return SaksHistorikkDTO(emptyList())
    }

    private fun utledBehandlingHendelser(alleBehandlinger: List<Behandling>): List<BehandlingHendelseDTO> {
        val behandlingerMedBehov = repositoryProvider.provide<AvklaringsbehovOperasjonerRepository>()
            .hentAlleAvklaringsbehovForSak(alleBehandlinger.map { it.id })

        // Behandlingsnivå
        val behandlingsHendelser = behandlingerMedBehov.map { behandling ->
            behandling.avklaringsbehov.filter {
                val definisjoner = listOf(
                    Definisjon.FATTE_VEDTAK,
                    Definisjon.SKRIV_VEDTAKSBREV,
                    Definisjon.BESTILL_LEGEERKLÆRING,
                    Definisjon.MANUELT_SATT_PÅ_VENT,
                    Definisjon.KVALITETSSIKRING
                )
                definisjoner.contains(it.definisjon)
            }.map { avklaringsbehov ->
                // Hendelsenivå - Her finner du knasket
                avklaringsbehov.historikk.map { hendelse ->

                    when (avklaringsbehov.definisjon) {
                        Definisjon.MANUELT_SATT_PÅ_VENT -> {
                            val hendelseType = if (hendelse.status.erAvsluttet()) {
                                BehandlingHendelseType.TATT_AV_VENT
                            } else {
                                BehandlingHendelseType.SATT_PÅ_VENT
                            }
                            BehandlingHendelseDTO(
                                hendelse = hendelseType,
                                tidspunkt = hendelse.tidsstempel,
                                utførtAv = hendelse.endretAv,
                                årsaker = listOf(hendelse.grunn.toString()),
                                begrunnelse = hendelse.begrunnelse,
                            )
                        }

                        Definisjon.FATTE_VEDTAK -> {
                            //SENDT_TIL_BESLUTTER,
                            //VEDTAK_FATTET, // Resultat, evt avslag med årsak
                            val hendelseType = if (hendelse.status.erAvsluttet()) {
                                BehandlingHendelseType.VEDTAK_FATTET
                            } else {
                                BehandlingHendelseType.SENDT_TIL_BESLUTTER
                            }
                            BehandlingHendelseDTO(
                                hendelse = hendelseType,
                                tidspunkt = hendelse.tidsstempel,
                                utførtAv = hendelse.endretAv,
                                årsaker = listOf(hendelse.grunn.toString()), // Mangler outcome
                                begrunnelse = hendelse.begrunnelse, // Mangler resultat?? Har visst noe??
                            )
                        }

                        Definisjon.SKRIV_VEDTAKSBREV -> {
                            //BREV_SENDT, //  Sendte brev med tittel
                            if (hendelse.status.erAvsluttet()) {
                                BehandlingHendelseDTO(
                                    hendelse = BehandlingHendelseType.BREV_SENDT,
                                    tidspunkt = hendelse.tidsstempel,
                                    utførtAv = hendelse.endretAv,
                                    begrunnelse = hendelse.begrunnelse // mangler tittel, defaulter til "Brev ferdig",
                                )
                            }
                        }

                        Definisjon.KVALITETSSIKRING -> {
                            val hendelseType = if (hendelse.status.erAvsluttet()) {
                                BehandlingHendelseType.RETUR_FRA_KVALITETSSIKRER
                            } else {
                                BehandlingHendelseType.SENDT_TIL_BESLUTTER
                            }

                            BehandlingHendelseDTO(
                                hendelse = hendelseType,
                                tidspunkt = hendelse.tidsstempel,
                                utførtAv = hendelse.endretAv,
                                begrunnelse = hendelse.begrunnelse, // mangler tittel, defaulter til "Brev ferdig",
                                //årsaker = MANGLER DENNE
                            )
                        }
                        // SENDT_TIL_KVALITETSSIKRER, // med resultat
                        // RETUR_FRA_KVALITETSSIKRER, // med resultat og eventuell årsak for retur + begrunnelse
                    }

                    else -> {}
                }
            }
        }
    }
    return listOf()
}


private fun utledOpprettelseHendelser(alleBehandlinger: List<Behandling>): List<BehandlingHistorikkDTO> {
    return alleBehandlinger.map { behandling ->

        val hendelseType = when (behandling.årsakTilOpprettelse) {
            ÅrsakTilOpprettelse.SØKNAD -> BehandlingHendelseType.FØRSTEGANGSBEHANDLING_OPPRETTET
            ÅrsakTilOpprettelse.MANUELL_OPPRETTELSE -> BehandlingHendelseType.REVURDERING_OPPRETTET

            // Kan legge til flere hendelsestyper her, defaulter enn så lenge
            else -> {
                BehandlingHendelseType.REVURDERING_OPPRETTET
            }
        }

        BehandlingHistorikkDTO(
            behandling.id,
            listOf(
                BehandlingHendelseDTO(
                    hendelse = hendelseType,
                    årsaker = behandling.vurderingsbehov().map { it.type.name },
                    tidspunkt = behandling.opprettetTidspunkt,
                )
            )
        )
    }
}
}
