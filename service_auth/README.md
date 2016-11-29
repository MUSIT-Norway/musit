
# Museum Collection grouping and authorization

We need to support the following (existing) collections by default.


| English term            |   Norwegian term      | Seq(OLD_SCHEMA_NAME)      |
| ------------------------|-----------------------|---------------------------|
| Archeology              |  Arkeologi            | `USD_ARK_GJENSTAND_S`, `USD_ARK_GJENSTAND_B`, `USD_ARK_GJENSTAND_O`, `USD_ARK_GJENSTAND_NTNU`, `USD_ARK_GJENSTAND_TROMSO`  |
| Ethnography             |  Etnografi            | `USD_ETNO_GJENSTAND_B`, `USD_ETNO_GJENSTAND_O`, `USD_ETNO_GJENSTAND_TROMSO` |
| Numismatics             |  Numismatikk          | `USD_NUMISMATIKK`           |
| Lichen                  |  Lav                  | `MUSIT_BOTANIKK_LAV`        |
| Moss                    |  Mose                 | `MUSIT_BOTANIKK_MOSE`       |
| Fungi                   |  Sopp                 | `MUSIT_BOTANIKK_SOPP`       |
| Algae                   |  Alger                | `MUSIT_BOTANIKK_ALGE`       |
| Vascular plants         |  Karplanter           | `MUSIT_BOTANIKK_FELLES`     |
| Entomology              |  Entomologi           | `MUSIT_ZOOLOGI_ENTOMOLOGI`  |
| Marine invertebrates    |  Marine evertebrater  | `MUSIT_ZOOLOGI_ENTOMOLOGI` (Avklar med Svein?) |

Museum collections should be stored in the `MUSIT_AUTH` schema in a separate
table, `AUTH_COLLECTION`. There will then also need to be a table for
`USER_AUTH_COLLECTION` to keep track of users with access to the collections.
Much the same way as `AUTH_GROUP` <=> `USER_AUTH_GROUP`.