# CheckToken

## Path
`api/util/channels`

## Opis

Zwraca listę kanałów tekstowych dostępnych dla graczy.

## Autoryzacja

Poprzez IP

## Przykłady

```json
{
  "success": true,
  "data": {
    "nazwy": [
        "║vipplus", "│wasze-historie", "│wybierz-jedno"
      ],
      "ids": {
        "║vipplus": "563045169264590867",
        "│wasze-historie": "652263445835808818",
        "│wybierz-jedno":"650322097813585961"
      }
  }
}
```