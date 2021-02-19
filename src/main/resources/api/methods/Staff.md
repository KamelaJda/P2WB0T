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
  "success": false,
  "error": {
    "body": "Brak autoryzacji",
    "description": "Token jest nieprawidłowy."
  }
}
```

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
  "nick": "nick użytkownika",
  "discordnick": "nick na discordzie",
  "prefix": "prefix użytkownika",
  "zespoły": ["lista", "zespołów"],
  "lider": ["lista", "zespołów, w których osoba jest liderem"]
}
```