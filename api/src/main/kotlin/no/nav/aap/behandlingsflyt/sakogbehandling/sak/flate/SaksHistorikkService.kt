package no.nav.aap.behandlingsflyt.sakogbehandling.sak.flate

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovForSak
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovOperasjonerRepository
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Status
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Status.SENDT_TILBAKE_FRA_BESLUTTER
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Status.SENDT_TILBAKE_FRA_KVALITETSSIKRER
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
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

        val historikk: List<BehandlingHistorikkDTO> =
            (opprettelsesHendelser + behandlingHendelser + returerMedÅrsakHendelser)
                .groupBy { it.behandlingId }
                .map { (_, behandlingData) ->
                    val samlet = behandlingData
                        .flatMap { it.hendelser }
                        .sortedByDescending { it.tidspunkt }
                    BehandlingHistorikkDTO(samlet)
                }

        return SaksHistorikkDTO(historikk)
    }

    private fun utledBehandlingHendelser(behandlingerMedBehov: List<AvklaringsbehovForSak>): List<BehandlingHistorikkInternal> {
        val relevanteDefinisjoner = setOf(
            Definisjon.FATTE_VEDTAK,
            Definisjon.FORESLÅ_VEDTAK,
            Definisjon.SKRIV_VEDTAKSBREV,
            Definisjon.BESTILL_LEGEERKLÆRING,
            Definisjon.MANUELT_SATT_PÅ_VENT,
            Definisjon.KVALITETSSIKRING
        )

        val avklaringsbehovene = behandlingerMedBehov.flatMap { it.avklaringsbehov }
        val erVedtatt = avklaringsbehovene.filter { it.erTotrinn() }.all { it.erTotrinnsVurdert() }

        return behandlingerMedBehov.mapNotNull { behandling ->
            val hendelser: List<BehandlingHendelseDTO> =
                behandling.avklaringsbehov
                    .filter { it.definisjon in relevanteDefinisjoner }
                    .flatMap { avklaringsbehov ->
                        avklaringsbehov.historikk.asSequence().mapNotNull { hendelse ->

                            when (avklaringsbehov.definisjon) {
                                Definisjon.MANUELT_SATT_PÅ_VENT -> {
                                    val type = if (hendelse.status == Status.AVSLUTTET) {
                                        BehandlingHendelseType.TATT_AV_VENT
                                    } else {
                                        BehandlingHendelseType.SATT_PÅ_VENT
                                    }

                                    BehandlingHendelseDTO(
                                        hendelse = type,
                                        tidspunkt = hendelse.tidsstempel,
                                        utførtAv = hendelse.endretAv,
                                        årsaker = listOfNotNull(
                                            hendelse.grunn?.name
                                        ),
                                        begrunnelse = hendelse.begrunnelse
                                    )
                                }

                                Definisjon.FATTE_VEDTAK -> {
                                    if (hendelse.status == Status.AVSLUTTET && erVedtatt) {
                                        BehandlingHendelseDTO(
                                            hendelse = BehandlingHendelseType.VEDTAK_FATTET,
                                            tidspunkt = hendelse.tidsstempel,
                                            utførtAv = hendelse.endretAv,
                                        )
                                    } else {
                                        null
                                    }
                                }

                                Definisjon.FORESLÅ_VEDTAK -> {
                                    if (hendelse.status == Status.AVSLUTTET) {
                                        BehandlingHendelseDTO(
                                            hendelse = BehandlingHendelseType.SENDT_TIL_BESLUTTER,
                                            tidspunkt = hendelse.tidsstempel,
                                            utførtAv = hendelse.endretAv,
                                        )
                                    } else {
                                        null
                                    }
                                }

                                Definisjon.SKRIV_VEDTAKSBREV -> {
                                    if (hendelse.status == Status.AVSLUTTET) {
                                        BehandlingHendelseDTO(
                                            hendelse = BehandlingHendelseType.BREV_SENDT,
                                            tidspunkt = hendelse.tidsstempel,
                                            utførtAv = hendelse.endretAv,
                                            begrunnelse = hendelse.begrunnelse
                                        )
                                    } else {
                                        null
                                    }
                                }

                                Definisjon.KVALITETSSIKRING -> {
                                    if (hendelse.status == Status.AVSLUTTET) {
                                        null
                                    } else {
                                        BehandlingHendelseDTO(
                                            hendelse = BehandlingHendelseType.SENDT_TIL_KVALITETSSIKRER,
                                            tidspunkt = hendelse.tidsstempel,
                                            utførtAv = hendelse.endretAv,
                                            begrunnelse = hendelse.begrunnelse
                                        )
                                    }
                                }

                                Definisjon.BESTILL_LEGEERKLÆRING -> {
                                    if (hendelse.status == Status.AVSLUTTET) {
                                        null
                                    } else {
                                        BehandlingHendelseDTO(
                                            hendelse = BehandlingHendelseType.BESTILT_LEGEERKLÆRING,
                                            tidspunkt = hendelse.tidsstempel,
                                            utførtAv = hendelse.endretAv,
                                            begrunnelse = hendelse.begrunnelse
                                        )
                                    }
                                }

                                else -> null
                            }
                        }
                    }
                    .toList()

            if (hendelser.isNotEmpty()) {
                BehandlingHistorikkInternal(behandling.behandlingId, hendelser)
            } else {
                null
            }
        }
    }


    private fun utledReturerMedÅrsak(
        behandlingerMedBehov: List<AvklaringsbehovForSak>
    ): List<BehandlingHistorikkInternal> {
        val returStatuser = listOf(
            SENDT_TILBAKE_FRA_KVALITETSSIKRER,
            SENDT_TILBAKE_FRA_BESLUTTER
        )

        return behandlingerMedBehov.mapNotNull { behandling ->
            val mapped = behandling.avklaringsbehov
                .flatMap { behov ->
                    behov.historikk
                        .filter { it.status in returStatuser }
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
                }

            if (mapped.isNotEmpty()) {
                BehandlingHistorikkInternal(
                    behandling.behandlingId,
                    mapped
                )
            } else {
                null
            }
        }
    }

    private fun utledOpprettelseHendelser(alleBehandlinger: List<Behandling>): List<BehandlingHistorikkInternal> {
        return alleBehandlinger.map { behandling ->

            val hendelseType = when (behandling.årsakTilOpprettelse) {
                ÅrsakTilOpprettelse.SØKNAD -> BehandlingHendelseType.FØRSTEGANGSBEHANDLING_OPPRETTET
                ÅrsakTilOpprettelse.MANUELL_OPPRETTELSE -> BehandlingHendelseType.REVURDERING_OPPRETTET
                ÅrsakTilOpprettelse.KLAGE -> BehandlingHendelseType.KLAGE_OPPRETTET

                // Kan legge til flere hendelsestyper her, defaulter enn så lenge
                else -> {
                    BehandlingHendelseType.REVURDERING_OPPRETTET
                }
            }

            BehandlingHistorikkInternal(
                behandling.id,
                listOf(
                    BehandlingHendelseDTO(
                        hendelse = hendelseType,
                        årsaker = behandling.vurderingsbehov().map { it.type.name },
                        tidspunkt = behandling.opprettetTidspunkt
                    )
                )
            )
        }
    }

    data class BehandlingHistorikkInternal(
        val behandlingId: BehandlingId,
        val hendelser: List<BehandlingHendelseDTO> = emptyList()
    )
}

