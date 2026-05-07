package no.nav.aap.behandlingsflyt.behandling.gregulering

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.Grunnbeløp
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.Year

class GReguleringService(
    private val underveisRepository: UnderveisRepository,
) {
    companion object {
        const val ANTALL_DAGER_MELLOM_GJUSTERING = 365L
    }
    constructor(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider) : this(
        underveisRepository = repositoryProvider.provide(),
    )

    private val log = LoggerFactory.getLogger(javaClass)

    fun hentSakerMedAktuellGJustering(datoForGJustering: LocalDate): Set<SakId> {
        val nyttGrunnbeløp = Grunnbeløp.finnGrunnbeløp(datoForGJustering)
        return underveisRepository.hentSakerMedAktuellGJustering(datoForGJustering, nyttGrunnbeløp)
    }

    fun finnesGrunnbeløpForÅr(år: Year) : Grunnbeløp.GrunnbeløpDto? {
        return Grunnbeløp.finnesGrunnbeløpForÅr(år)
    }
}