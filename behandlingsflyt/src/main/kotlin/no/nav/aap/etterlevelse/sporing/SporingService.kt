package no.nav.aap.etterlevelse.sporing

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.ApplikasjonsVersjon
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.utils.Diff
import no.nav.aap.behandlingsflyt.utils.Endret
import no.nav.aap.behandlingsflyt.utils.Fjernet
import no.nav.aap.behandlingsflyt.utils.LagtTil
import no.nav.aap.behandlingsflyt.utils.Uendret
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.json.DefaultJsonMapper
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.lookup.repository.RepositoryProvider
import java.time.Instant

class SporingService(
    private val sporingRepository: SporingRepository,
) {
    constructor(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider) : this(
        sporingRepository = repositoryProvider.provide(),
    )

    fun <Input, Output> kjørMedSporing(
        behandling: Behandling,
        sporbar: Sporbar<Input, Output>,
        sporbarRepo: SporbarRepo<Output>,
        input: Input,
    ): Tidslinje<Diff<Output>> {
        val forrigeResultat = behandling.forrigeBehandlingId
            ?.let { sporbarRepo.hent(it) }
            ?: Tidslinje.empty()

        val nyttResultat = sporbar.kjørMedSporing(forrigeResultat, input)
        lagreSporing(
            behandlingId = behandling.id,
            sporingsnøkkel = sporbar.sporingsnøkkel,
            input = input,
            resultat = nyttResultat,
        )
        sporbarRepo.lagre(behandling.id, nyttResultat)
        return nyttResultat
    }

    private fun <Input, Output> lagreSporing(
        behandlingId: BehandlingId,
        sporingsnøkkel: Sporingsnøkkel,
        input: Input,
        resultat: Tidslinje<Diff<Output>>,
    ) {
        sporingRepository.lagre(
            Sporing(
                behandlingId = behandlingId,
                opprettetTidspunkt = Instant.now(),
                sporingsnøkkel = sporingsnøkkel,
                versjon = ApplikasjonsVersjon.versjon,
                input = DefaultJsonMapper.objectMapper().valueToTree(input),
                operasjoner = resultat.map {
                    when (it) {
                        is Endret<*> -> Sporing.Operasjon.ENDRET
                        is Fjernet<*> -> Sporing.Operasjon.FJERNET
                        is LagtTil<*> -> Sporing.Operasjon.LAGT_TIL
                        is Uendret<*> -> Sporing.Operasjon.UENDRET
                    }
                }.komprimer()
            )
        )
    }
}

/** Metoder for å lese og skrive beregninger på en måte som passer med . */
interface SporbarRepo<Resultat> {
    /** Hvordan resultatet skal lagres ned, både det å legge til eller endre eksisterende vurderinger,
     *  eller fjerne vurderinger hvis segmentet er [Fjernet][no.nav.aap.behandlingsflyt.utils.Fjernet].
     *
     *  Diffen er i forhold til forrige behandling, så lagringen av diffen må også skje i forhold
     *  til forrige behandling.
     *  */
    fun lagre(behandlingId: BehandlingId, resultat: Tidslinje<Diff<Resultat>>)

    /** Hent fram resultatet fra en behandling. Brukes i praksis for å hente forrige behandling.
     *
     * Manglende resultat representeres med tom tidslinje.
     *  */
    fun hent(behandlingId: BehandlingId): Tidslinje<Resultat>
}
