package no.nav.aap.behandlingsflyt.prosessering.tilbakekreving

import no.nav.aap.komponenter.json.DefaultJsonMapper

const val TILBAKEKREVING_EVENT_TOPIC = "tilbake.privat-tilbakekreving-arbeidsavklaringspenger"

class TilbakekrevingFagsystemInfoProdusent(
    config: KafkaProducerConfig<String, String>,
): KafkaProdusent<String, String>(
    topic = TILBAKEKREVING_EVENT_TOPIC,
    config = config,
    producerName = "AapFagsystemInfoProdusent",

) {

    fun sendFagsystemInfo(key: String, fagsysteminfo: FagsysteminfoSvarHendelse) {
        val json = DefaultJsonMapper.toJson(fagsysteminfo)
        produser(key, json)
    }

}