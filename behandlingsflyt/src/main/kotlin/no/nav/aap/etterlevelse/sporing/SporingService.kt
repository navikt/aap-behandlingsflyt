package no.nav.aap.etterlevelse.sporing

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.ApplikasjonsVersjon
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.json.DefaultJsonMapper
import no.nav.aap.lookup.repository.RepositoryProvider
import java.time.Instant

class SporingService(
    private val sporingRepository: SporingRepository,
) {
    constructor(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider) : this(
        sporingRepository = repositoryProvider.provide(),
    )

    fun <Grunnlag,Resultat> kjørMedSporing(
        behandling: Behandling,
        sporbar: Sporbar<Grunnlag, Resultat>,
        sporbartResultatRepo: SporbartResultatRepo<Resultat>,
        grunnlag: Grunnlag,
    ): Resultat {
        requireNotNull(sporbar::class.objectInstance) {
            """${sporbar::class.simpleName} skal være object, ikke class. Poenget med sporing er at alle opplysninger som ble brukt
                i utregningen av [Sporbar::kjørMedSporing] er en del av grunnlaget. Hvis man lager en klasse med konstruktør og injecter
                data / services inn den veien, så blir det opplysninger som påvirker utfallet som ikke er en del av grunnlaget.
                
                Trenger du feature-toggles, så kan de legges inn i grunnlaget.
                
                Trenger du hjelpemetoder fra en annen klasse, så kan du i den andre klassen putte hjelpemetoden i
                companion-objektet. Hvis metoden gjør IO, så må metoden splittes i to: en del som lever i klassen og kan
                gjøre IO, og så en del i companion-objektet som ikke har sideeffekter.
            """
        }

        val resultat = sporbar.kjørMedSporing(grunnlag)
        sporingRepository.lagre(
            Sporing(
                behandlingId = behandling.id,
                opprettetTidspunkt = Instant.now(),
                sporingsnøkkel = sporbar.sporingsnøkkel,
                versjon = ApplikasjonsVersjon.versjon,
                grunnlag = DefaultJsonMapper.objectMapper().valueToTree(grunnlag),
            )
        )
        sporbartResultatRepo.lagre(behandling.id, resultat)
        return resultat
    }
}

interface SporbartResultatRepo<Resultat> {
    fun lagre(behandlingId: BehandlingId, resultat: Resultat)
}
