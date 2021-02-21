# CheckToken

## Path
`/api/util/staff`

## Opis

Zwraca liste administracji

## Headers
| Key           | Value   |
|---------------|---------|
| Authorization | API Key |

## Przykłady

```json
{
  "success": true,
  "data": {
    "zarzad": [
      {
        "Patrz #Osoba na dole strony"
      },
      {
        "Patrz #Osoba na dole strony"
      }
    ],
    "administratorzy": [
      "..."
    ],
    "moderatorzy": [
      "..."
    ],
    "pomocnicy": [
      "..."
    ],
    "stazysci": [
      "..."
    ]
  }
}
```

#### Osoba
```json
{
  "nick": "username na Discordzie",
  "discordnick": "nick na serwerze Discord",
  "prefix": "prefix użytkownika",
  "zespoły": ["lista", "zespołów"],
  "lider": ["lista", "zespołów, w których osoba jest liderem"]
}
```