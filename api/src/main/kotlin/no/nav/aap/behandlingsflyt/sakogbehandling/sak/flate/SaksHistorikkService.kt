package no.nav.aap.behandlingsflyt.sakogbehandling.sak.flate

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovForSak
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovOperasjonerRepository
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Status.SENDT_TILBAKE_FRA_BESLUTTER
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Status.SENDT_TILBAKE_FRA_KVALITETSSIKRER
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
        val behandlingerMedBehov = repositoryProvider.provide<AvklaringsbehovOperasjonerRepository>()
            .hentAlleAvklaringsbehovForSak(alleBehandlinger.map { it.id })

        val opprettelsesHendelser = utledOpprettelseHendelser(alleBehandlinger)
        val behandlingHendelser = utledBehandlingHendelser(behandlingerMedBehov)
        val returerMedÅrsakHendelser = utledReturerMedÅrsak(behandlingerMedBehov)



        // Combine and sort the above

        return SaksHistorikkDTO(emptyList())
    }

    private fun utledBehandlingHendelser(behandlingerMedBehov: List<AvklaringsbehovForSak>): List<BehandlingHendelseDTO> {
        return behandlingerMedBehov.flatMap { behandling ->
            behandling.avklaringsbehov.filter {
                val definisjoner = listOf(
                    Definisjon.FATTE_VEDTAK,
                    Definisjon.SKRIV_VEDTAKSBREV,
                    Definisjon.BESTILL_LEGEERKLÆRING,
                    Definisjon.MANUELT_SATT_PÅ_VENT,
                    Definisjon.KVALITETSSIKRING
                )
                definisjoner.contains(it.definisjon)
            }.flatMap { avklaringsbehov ->
                avklaringsbehov.historikk.mapNotNull { hendelse ->
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
                                BehandlingHendelseType.VEDTAK_FATTET // Denne må kun ta den siste, da den alltid er avsluttet dersom den gikk i retur
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
                            } else null
                        }

                        Definisjon.KVALITETSSIKRING -> {
                            // SENDT_TIL_KVALITETSSIKRER, // med resultat
                            if (hendelse.status.erAvsluttet()) {
                                null
                            }

                            BehandlingHendelseDTO(
                                hendelse = BehandlingHendelseType.SENDT_TIL_BESLUTTER,
                                tidspunkt = hendelse.tidsstempel,
                                utførtAv = hendelse.endretAv,
                                begrunnelse = hendelse.begrunnelse
                            )
                        }

                        else -> {
                            null
                        }
                    }
                }
            }
        }
    }

    private fun utledReturerMedÅrsak(behandlingerMedBehov: List<AvklaringsbehovForSak>): List<BehandlingHistorikkDTO> {
        val returStatuser = listOf(SENDT_TILBAKE_FRA_KVALITETSSIKRER, SENDT_TILBAKE_FRA_BESLUTTER)

       return behandlingerMedBehov.flatMap { behandling ->
            behandling.avklaringsbehov.map { behov ->
                val mapped = behov.historikk
                    .filter { hendelse -> returStatuser.contains(hendelse.status) }
                    .map { hendelse ->
                        val typeRetur = if (hendelse.status == SENDT_TILBAKE_FRA_KVALITETSSIKRER) {
                            BehandlingHendelseType.RETUR_FRA_KVALITETSSIKRER
                        } else {
                            BehandlingHendelseType.RETUR_FRA_BESLUTTER
                        }

                        BehandlingHendelseDTO(
                            hendelse = typeRetur,
                            tidspunkt = hendelse.tidsstempel,
                            utførtAv = hendelse.endretAv,
                            årsaker = hendelse.årsakTilRetur.map { it.årsak.name },
                            begrunnelse = hendelse.begrunnelse,
                        )
                    }
                BehandlingHistorikkDTO(
                    behandling.behandlingId,
                    mapped
                )
            }
        }
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

