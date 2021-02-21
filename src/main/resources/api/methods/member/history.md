# CheckToken

## Path
`api/member/{member}/history`

## Opis
Zwraca historię kar danego użytkownika

## Headers
| Key           | Value             |
|---------------|-------------------|
| Authorization | Token             |

## Query
| Key    | Value             |
|--------|-------------------|
| member | ID użytkownika    |
| offset | Offset (limit 10) |

## Przykłady
```json
{
  "success": true,
  "data": [
    {"Patrz Kara.md"},
    {"Patrz Kara.md"}
  ]
}
```