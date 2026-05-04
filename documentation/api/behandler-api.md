# Behandler API

Base path: `/api/v1/behandler`

All endpoints require a valid Bearer token in the `Authorization` header.
All endpoints return BehandlerDTO objects, either as single objects or in arrays.

## BehandlerDTO

Response object representing a behandler.

| Field          | Type   | Nullable | Description                                                                                                                                                                                                                          |
|----------------|--------|----------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `type`         | String | Yes      | Relation type between the behandler and a person: `FASTLEGE`, `FASTLEGEVIKAR`, `SYKMELDER`. `null` when not in a person context (search endpoints). See [model](https://github.com/navikt/isdialogmelding/blob/master/src/main/kotlin/no/nav/syfo/behandler/domain/BehandlerArbeidstakerRelasjonstype.kt). |
| `behandlerRef` | String | No       | UUID uniquely identifying this behandler in the system                                                                                                                                                                               |
| `kategori`     | String | No       | Category of the behandler: `LEGE`, `FYSIOTERAPEUT`, `KIROPRAKTOR`, `MANUELLTERAPEUT`, `TANNLEGE`, `PSYKOLOG`. See [model](https://github.com/navikt/isdialogmelding/blob/master/src/main/kotlin/no/nav/syfo/behandler/domain/BehandlerKategori.kt).                                                        |
| `fnr`          | String | Yes      | Fødselsnummer for behandler                                                                                                                                                                                                          |
| `hprId`        | Int    | Yes      | HPR-number from Helsepersonellregisteret                                                                                                                                                                                             |
| `fornavn`      | String | No       | First name                                                                                                                                                                                                                           |
| `mellomnavn`   | String | Yes      | Middle name                                                                                                                                                                                                                          |
| `etternavn`    | String | No       | Last name                                                                                                                                                                                                                            |
| `orgnummer`    | String | Yes      | Organisation number of the behandler's office                                                                                                                                                                                        |
| `kontor`       | String | Yes      | Name of the behandler's office/clinic                                                                                                                                                                                                |
| `adresse`      | String | Yes      | Street address for office                                                                                                                                                                                                            |
| `postnummer`   | String | Yes      | Postal code                                                                                                                                                                                                                          |
| `poststed`     | String | Yes      | Postal place/city                                                                                                                                                                                                                    |
| `telefon`      | String | Yes      | Phone number                                                                                                                                                                                                                         |


---

## Endpoints

### 1. Get behandlere for a person

```
GET /api/v1/behandler/personident
```

Returns a list of behandlere (healthcare providers) associated with a given person.
Gets fastleger from [`fastlegerest`](https://github.com/navikt/fastlegerest) and sykmeldere from the database in this repo.

Access is validated via veileder tilgangskontroll — the veileder must have access to the requested person with both populasjonstilgang and fagtilgang for SYFO (SYFO-SENSITIV).
If other teams want to use this endpoint, we need to change the access control so that new users do not rely on fagtilgang for SYFO.

#### Request headers

| Header            | Required | Description                              |
|-------------------|----------|------------------------------------------|
| `Authorization`   | Yes      | `Bearer <token>`                         |
| `Nav-Personident` | Yes      | National identity number (fødselsnummer) |
| `Nav-Call-Id`     | No       | Correlation ID for tracing               |

#### Response codes

| Code | Description                                       |
|------|---------------------------------------------------|
| 200  | OK — returns an array of [BehandlerDTO](#behandlerdto) |
| 400  | Bad request — missing required headers            |
| 403  | Forbidden — veileder lacks access to the person   |

#### Example response

```json
[
  {
    "type": "FASTLEGE", 
    "behandlerRef": "018e1234-abcd-7000-beef-000000000001",
    "kategori": "LEGE", 
    "fnr": "12345678910",
    "hprId": 1234567,
    "fornavn": "Ola",
    "mellomnavn": null,
    "etternavn": "Nordmann",
    "orgnummer": "123456789",
    "kontor": "Sentrum legesenter",
    "adresse": "Storgata 1",
    "postnummer": "0155",
    "poststed": "Oslo",
    "telefon": "22000000"
  },
  {
    "type": "SYKMELDER",
    "behandlerRef": "018e1234-abcd-7000-beef-000000000002",
    "kategori": "PSYKOLOG",
    "fnr": "12345678919",
    "hprId": 1234568,
    "fornavn": "Kari",
    "mellomnavn": null,
    "etternavn": "Sykmeldersen",
    "orgnummer": "123456789",
    "kontor": "Sentrum legesenter",
    "adresse": "Storgata 1",
    "postnummer": "0155",
    "poststed": "Oslo",
    "telefon": "22000000"
  }
]
```

---

### 2. Search for behandlere (GET)

```
GET /api/v1/behandler/search
```

Searches for behandlere by name or identifier. The search string is passed as a request header.

#### Request headers

| Header          | Required | Description                   |
|-----------------|----------|-------------------------------|
| `Authorization` | Yes      | `Bearer <token>`              |
| `searchstring`  | Yes      | The search query (name, etc.) |

#### Response codes

| Code | Description                                                           |
|------|-----------------------------------------------------------------------|
| 200  | OK — returns an array of [BehandlerDTO](#behandlerdto) (without `type`) |
| 400  | Bad request — missing `searchstring` header                           |

---

### 3. Search for behandlere (POST)

```
POST /api/v1/behandler/search
```

Same as the GET search, but the search string is provided in the request body. Preferred over the GET endpoint

#### Request body

`Content-Type: application/json`

```json
{
  "searchstring": "Ola Nordmann"
}
```

| Field          | Type   | Required | Description        |
|----------------|--------|----------|--------------------|
| `searchstring` | String | Yes      | The search query   |

#### Response codes

| Code | Description                                                           |
|------|-----------------------------------------------------------------------|
| 200  | OK — returns an array of [BehandlerDTO](#behandlerdto) (without `type`) |
| 400  | Bad request — missing or null `searchstring` in body                  |

---

### 4. Get behandler by reference

```
GET /api/v1/behandler/{behandlerRef}
```

Returns a single behandler identified by their unique reference UUID.

#### Path parameters

| Parameter      | Description                                      |
|----------------|--------------------------------------------------|
| `behandlerRef` | UUID of the behandler (as assigned by this system) |

#### Response codes

| Code | Description                                              |
|------|----------------------------------------------------------|
| 200  | OK — returns a single [BehandlerDTO](#behandlerdto)      |
| 404  | Not found — no behandler exists with the given reference |
