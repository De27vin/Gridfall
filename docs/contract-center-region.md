# Contract center region

Center-related contracts use `Board.centerZone` independently for the board's actual
row and column counts. The region stays symmetric: choose the largest span no wider
than half the axis, with the same even/odd parity as that axis (minimum one cell).
This leaves equal margins on both sides.

| Axis size | Center indices (zero-based, inclusive) |
| --- | --- |
| 3 | 1 |
| 4 | 1–2 |
| 5 | 2 |
| 6 | 2–3 |
| 7 | 2–4 |
| 8 | 2–5 |
| 9 | 3–5 |
| 10 | 3–6 |

For example, a 4×4 map has exactly the four center cells `(1,1)`, `(1,2)`, `(2,1)`,
and `(2,2)`. A 4×7 map uses rows 1–2 and columns 2–4. The existing preset regions
remain unchanged: 3×3 for Rush (7×7), 4×4 for Classic (8×8) and Marathon (10×10).

`TouchCenter`, `AvoidCenterArea`, and the center portion of `TouchEdgeAndCenter`
all use this rule. A piece uses the center if **any placed cell** touches it;
the piece's origin alone does not decide. Positive targets and avoidance warnings
use the same calculation. Blocked map cells remain unplayable and are not painted
as usable targets; they do not shift the geometric center.
