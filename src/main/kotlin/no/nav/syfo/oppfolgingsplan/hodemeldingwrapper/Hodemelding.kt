package no.nav.syfo.oppfolgingsplan.hodemeldingwrapper

import no.kith.xmlstds.msghead._2006_05_24.XMLDocument
import no.kith.xmlstds.msghead._2006_05_24.XMLMsgHead
import java.util.stream.Stream

class Hodemelding(msgHead: XMLMsgHead) {
    private val msgHead: XMLMsgHead
    private val dokumentListe: MutableList<Dokument>
    private val xMLDocumentStream: Stream<XMLDocument>
        get() = Stream.of(msgHead)
            .map { xmlMsgHead: XMLMsgHead -> xmlMsgHead.document }
            .flatMap { obj: List<XMLDocument?> -> obj.stream() }

    fun erForesporsel(): Boolean {
        return dokumentListe.stream().anyMatch { obj: Dokument -> obj.erForesporsel() }
    }

    fun erNotat(): Boolean {
        return dokumentListe.stream().anyMatch { obj: Dokument -> obj.erNotat() }
    }

    fun harVedlegg(): Boolean {
        return dokumentListe.stream().anyMatch { obj: Dokument -> obj.harVedlegg() }
    }

    val messageId: String
        get() = msgHead.getMsgInfo().getMsgId()
    val dokIdForesporselStream: Stream<String?>
        get() = dokumentListe.stream().flatMap { obj: Dokument -> obj.dokIdForesporselStream }
    val dokIdNotatStream: Stream<String?>
        get() = dokumentListe.stream().flatMap { obj: Dokument -> obj.dokIdNotatStream }

    init {
        this.msgHead = msgHead
        dokumentListe = ArrayList()
        xMLDocumentStream.map { xmlDocument: XMLDocument -> Dokument(xmlDocument) }
            .forEach { dokument -> dokumentListe.add(dokument) }
    }
}
