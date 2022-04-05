package no.nav.syfo.testhelper.generator

import no.nav.syfo.behandler.kafka.dialogmeldingfrombehandler.KafkaDialogmeldingFromBehandlerDTO
import java.time.LocalDateTime
import java.util.*

const val partnerRef = "12345"
const val dokIdNotat = "OD2010199930316"

const val fellesformatXml = """<?xml version="1.0" ?>
    <EI_fellesformat xmlns="http://www.nav.no/xml/eiff/2/" >
    <MottakenhetBlokk partnerReferanse="$partnerRef" />
    </EI_fellesformat>"""

const val completeFellesformatXml = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><EI_fellesformat xmlns=\"http://www.nav.no/xml/eiff/2/\" xmlns:ns6=\"http://www.kith.no/xmlstds/base64container\" xmlns:ns5=\"http://www.kith.no/xmlstds/felleskomponent1\" xmlns:ns7=\"http://www.kith.no/xmlstds/apprec/2004-11-21\" xmlns:ns2=\"http://www.kith.no/xmlstds/msghead/2006-05-24\" xmlns:ns4=\"http://www.kith.no/xmlstds/dialog/2006-10-11\" xmlns:ns3=\"http://www.w3.org/2000/09/xmldsig#\"><ns2:MsgHead><ns2:MsgInfo><ns2:Type V=\"DIALOG_SVAR\" DN=\"Svar p.. foresp..rsel\"/><ns2:MIGversion>v1.2 2006-05-24</ns2:MIGversion><ns2:GenDate>2020-09-21T11:11:55</ns2:GenDate><ns2:MsgId>syfomock-13272642348</ns2:MsgId><ns2:Ack V=\"J\" DN=\"Ja\"/><ns2:Sender><ns2:Organisation><ns2:OrganisationName>Kule helsetjenester AS</ns2:OrganisationName><ns2:Ident><ns2:Id>0123</ns2:Id><ns2:TypeId V=\"HER\" S=\"2.16.578.1.12.4.1.1.9051\" DN=\"HER-id\"/></ns2:Ident><ns2:Ident><ns2:Id>223456789</ns2:Id><ns2:TypeId V=\"ENH\" S=\"2.16.578.1.12.4.1.1.9051\" DN=\"Organisasjonsnummeret i Enhetsregister\"/></ns2:Ident><ns2:Address><ns2:Type V=\"PST\" DN=\"POSTADRESSE\"/><ns2:StreetAdr>Oppdiktet gate 203</ns2:StreetAdr><ns2:PostalCode>1234</ns2:PostalCode><ns2:City>Oslo</ns2:City></ns2:Address><ns2:TeleCom><ns2:TypeTelecom V=\"WP\" DN=\"Arbeidsplass\"/><ns2:TeleAddress V=\"tel:12 34 56 78\"/></ns2:TeleCom><ns2:TeleCom><ns2:TypeTelecom V=\"F\" DN=\"Fax\"/><ns2:TeleAddress V=\"fax:87 65 43 21\"/></ns2:TeleCom><ns2:HealthcareProfessional><ns2:FamilyName>Valda</ns2:FamilyName><ns2:MiddleName>Fos</ns2:MiddleName><ns2:GivenName>Inga</ns2:GivenName><ns2:Ident><ns2:Id>1234567</ns2:Id><ns2:TypeId V=\"HPR\" S=\"2.16.578.1.12.4.1.1.8116\" DN=\"HPR-nummer\"/></ns2:Ident><ns2:Ident><ns2:Id>1234</ns2:Id><ns2:TypeId V=\"HER\" S=\"2.16.578.1.12.4.1.1.8116\" DN=\"HER-id\"/></ns2:Ident></ns2:HealthcareProfessional></ns2:Organisation></ns2:Sender><ns2:Receiver><ns2:Organisation><ns2:OrganisationName>NAV</ns2:OrganisationName><ns2:Ident><ns2:Id>889640782</ns2:Id><ns2:TypeId V=\"ENH\" S=\"2.16.578.1.12.4.1.1.9051\" DN=\"Organisasjonsnummeret i Enhetsregisteret\"/></ns2:Ident><ns2:Ident><ns2:Id>79768</ns2:Id><ns2:TypeId V=\"HER\" S=\"2.16.578.1.12.4.1.1.9051\" DN=\"Identifikator fra Helsetjenesteenhetsregisteret (HER-id)\"/></ns2:Ident><ns2:Organisation><ns2:OrganisationName>NAV Oslo</ns2:OrganisationName><ns2:Ident><ns2:Id>0000</ns2:Id><ns2:TypeId V=\"LIN\" S=\"2.16.578.1.12.4.1.1.9051\" DN=\"Lokal identifikator for institusjoner\"/></ns2:Ident></ns2:Organisation></ns2:Organisation></ns2:Receiver><ns2:Patient><ns2:FamilyName>Etternavn</ns2:FamilyName><ns2:GivenName>Test</ns2:GivenName><ns2:Sex V=\"1\" DN=\"Mann\"/><ns2:Ident><ns2:Id>19087999648</ns2:Id><ns2:TypeId V=\"FNR\" S=\"2.16.578.1.12.4.1.1.8116\" DN=\"Fødselsnummer\"/></ns2:Ident><ns2:Address><ns2:Type V=\"HP\" DN=\"Folkeregisteradresse\"/><ns2:StreetAdr>Sannergata 2</ns2:StreetAdr><ns2:PostalCode>0655</ns2:PostalCode><ns2:City>OSLO</ns2:City></ns2:Address><ns2:TeleCom><ns2:TypeTelecom V=\"MC\" DN=\"MobilTelefon\"/><ns2:TeleAddress V=\"tel:12345678\"/></ns2:TeleCom><ns2:TeleCom><ns2:TypeTelecom V=\"HP\" DN=\"Hovedtelefon\"/><ns2:TeleAddress V=\"tel:23456789\"/></ns2:TeleCom></ns2:Patient></ns2:MsgInfo><ns2:Document><ns2:RefDoc><ns2:IssueDate V=\"2020-09-21T21:11:26\"/><ns2:MsgType V=\"XML\" DN=\"XML-instans\"/><ns2:Content><ns4:Dialogmelding><ns4:Notat><ns4:TemaKodet V=\"1\" S=\"2.16.578.1.12.4.1.1.8126\" DN=\"Ja, jeg kommer\"/><ns4:TekstNotatInnhold>Test m xyzaaccdd</ns4:TekstNotatInnhold><ns4:DokIdNotat>$dokIdNotat</ns4:DokIdNotat><ns4:DatoNotat>2020-09-21</ns4:DatoNotat></ns4:Notat></ns4:Dialogmelding></ns2:Content></ns2:RefDoc></ns2:Document></ns2:MsgHead><MottakenhetBlokk ediLoggId=\"31465976110\" avsender=\"123123\" ebXMLSamtaleId=\"20200917-011356-538\" meldingsType=\"xml\" avsenderRef=\"SERIALNUMBER=984106610, CN=Allmennmedisinsk Senter DA, O=AMS ALLMENNMEDISINSK SENTER DA, C=NO\" avsenderFnrFraDigSignatur=\"01117302624\" mottattDatotid=\"2022-03-29T17:10:53.224+02:00\" partnerReferanse=\"$partnerRef\" herIdentifikator=\"\" ebRole=\"Sykmelder\" ebService=\"DialogmoteInnkalling\" ebAction=\"MoteRespons\"/></EI_fellesformat>"

fun generateDialogmeldingFromBehandlerDTO(uuid: UUID) = KafkaDialogmeldingFromBehandlerDTO(
    msgId = uuid.toString(),
    navLogId = "1234asd123",
    mottattTidspunkt = LocalDateTime.now(),
    personIdentPasient = "",
    personIdentBehandler = "",
    legekontorOrgNr = "",
    legekontorHerId = "",
    legekontorOrgName = "",
    legehpr = "",
    fellesformatXML = fellesformatXml,
)
