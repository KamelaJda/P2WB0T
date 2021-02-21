# CheckToken

## Path
`api/history/list`

## Opis
Zwraca listę nadanych kar (od najnowszej do najstarszej).

## Headers
| Key           | Value             |
|---------------|-------------------|
| Authorization | Token             |

## Query
| Key    | Value             |
|--------|-------------------|
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