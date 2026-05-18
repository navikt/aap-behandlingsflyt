package no.nav.aap.behandlingsflyt.faktagrunnlag

import no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.TilkjentYtelseRepository
import no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.tilTidslinje
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskrav.Endret.ENDRET
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskrav.Endret.IKKE_ENDRET
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.Grunnbeløp
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekst
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.VurderingType.FØRSTEGANGSBEHANDLING
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.VurderingType.G_REGULERING
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.VurderingType.REVURDERING
import no.nav.aap.behandlingsflyt.unleash.BehandlingsflytFeature
import no.nav.aap.behandlingsflyt.unleash.UnleashGateway
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import org.slf4j.LoggerFactory

class GrunnbeløpInformasjonskrav(
    private val tilkjentYtelseRepository: TilkjentYtelseRepository,
    private val underveisRepository: UnderveisRepository,
    private val unleashGateway: UnleashGateway,
) : Informasjonskrav<IngenInput, IngenRegisterData> {

    private val logger = LoggerFactory.getLogger(javaClass)
    
    companion object : Informasjonskravkonstruktør {
        override val navn = InformasjonskravNavn.GRUNNBELØP

        override fun konstruer(
            repositoryProvider: RepositoryProvider,
            gatewayProvider: GatewayProvider
        ): GrunnbeløpInformasjonskrav {
            return GrunnbeløpInformasjonskrav(
                tilkjentYtelseRepository = repositoryProvider.provide(),
                underveisRepository = repositoryProvider.provide(),
                unleashGateway = gatewayProvider.provide(),
            )
        }
    }

    override val navn = Companion.navn

    override fun erRelevant(
        kontekst: FlytKontekstMedPerioder,
        steg: StegType,
        oppdatert: InformasjonskravOppdatert?
    ): Boolean {
        return unleashGateway.isEnabled(BehandlingsflytFeature.GrunnbeloepInformasjonskrav)
                && kontekst.vurderingType in setOf(FØRSTEGANGSBEHANDLING, REVURDERING, G_REGULERING)
    }

    override fun klargjør(kontekst: FlytKontekstMedPerioder) = IngenInput

    override fun hentData(input: IngenInput) = IngenRegisterData

    override fun oppdater(
        input: IngenInput,
        registerdata: IngenRegisterData,
        kontekst: FlytKontekstMedPerioder
    ): Informasjonskrav.Endret {
        return informasjonskravEndret(kontekst.behandlingId)
    }

    override fun flettOpplysningerFraAtomærBehandling(kontekst: FlytKontekst): Informasjonskrav.Endret {
        /*
         * Denne vil føre til at behandlingen tilbakeføres til steget tilkjent ytelse. I dette tilfellet er det ikke
         * aktuelt å flette inn nye opplysninger så i prinsippet er dette strengt tatt ikke nødvendig da den åpne
         * behandlingen vil prosesseres og dermed gå gjennom alle informasjonskravene på nytt.
         */
        if (unleashGateway.isEnabled(BehandlingsflytFeature.GrunnbeloepInformasjonskrav)) {
            return informasjonskravEndret(kontekst.behandlingId)
        } else {
            return IKKE_ENDRET
        }
    }

    private fun informasjonskravEndret(behandlingId: BehandlingId): Informasjonskrav.Endret {
        val underveisGrunnlag = underveisRepository.hentHvisEksisterer(behandlingId)
            ?: return IKKE_ENDRET

        val tilkjentYtelsePerioder = tilkjentYtelseRepository.hentHvisEksisterer(behandlingId)
            ?: return IKKE_ENDRET

        val oppfyltUnderveisTidslinje = underveisGrunnlag.somTidslinje().filter { it.verdi.utfall == Utfall.OPPFYLT }
        val tilkjentYtelseTidslinje = tilkjentYtelsePerioder.tilTidslinje()
        val grunnbeløpTidslinje = Grunnbeløp.tilTidslinje()

        /*
         * Eksempel: 2-ukers meldeperiode (24.apr – 7.mai) som krysser G-grensen 1.mai
         *
         *                       24.apr              1.mai             7.mai
         *                       |                   |                 |
         * Tilkjent ytelse:      |-------------- G=124 028 ------------|
         * Oppfylt underveis:    |--------------- OPPFYLT -------------|
         * Gjeldende grunnbeløp: |----- 124 028 -----|----- 130 160 ---|
         *                       |                   |                 |
         * 1) join(underveis):   |-------------- G=124 028 ------------|
         * 2) join(grunnbeløp):  |------- false -----|----- true ------|
         * 3) filter { true }:                       |----- true ------|
         *
         * → ENDRET (grunnbeløpet har endret seg etter 1. mai)
         */
        val endredeGrunnbeløp = tilkjentYtelseTidslinje
            .innerJoin(oppfyltUnderveisTidslinje, { ty, _ -> ty })
            .innerJoin(grunnbeløpTidslinje, { ty, grunnbeløp ->
                ty.grunnbeløp != grunnbeløp
            })
            .filter { it.verdi }

        if (endredeGrunnbeløp.isNotEmpty()) {
            logger.info("Perioder med endrede grunnbeløp: " + endredeGrunnbeløp.perioder())
            return ENDRET
        } else {
            return IKKE_ENDRET
        }
    }
}
