"""Instance and subset the bundled display fonts into static, Latin-only TTFs.

The Latin-only subset is deliberate and is not a gap to fill.

Japanese and Korean are shipped languages, and their glyphs are not in here.
Rather than widen this range - which would turn two ~15KB files into two
~10MB files - the interface family for those two locales is the platform's own
CJK family, selected at runtime by AppLanguage.needsSystemFont in
app/src/main/java/com/gopu/arrow/puzzle/game/i18n/AppLanguage.kt and read by
UiSans in ui/theme/Type.kt. Android already ships Noto Sans CJK on every
device, so bundling a second copy would grow the APK for no rendering benefit.

If you widen UNICODES, widen it for Latin-script coverage only (accented
capitals, the languages in res/values-*). CJK belongs in the fallback path.
"""

import os
import sys

from fontTools import subset
from fontTools.ttLib import TTFont
from fontTools.varLib import instancer

STAGE = os.path.dirname(os.path.abspath(__file__))
OUT = sys.argv[1]

# Latin, plus the symbols the UI draws as text: the arrow glyph on the home
# stat plate, the star ratings, and the dash, quote and bullet marks the store
# copy and the version line use.
UNICODES = (
    "U+0020-007E,U+00A9,U+00AE,U+00B0,U+00B7,U+00D7,U+00E0,U+00E4,U+00E9,"
    "U+00F6,U+00FC,U+2013,U+2014,U+2018,U+2019,U+201C,U+201D,U+2022,U+2026,"
    "U+2192,U+25B6,U+2605,U+2606"
)


def save_static(src, dst, axis_value=None):
    font = TTFont(os.path.join(STAGE, src))
    if axis_value is not None:
        font = instancer.instantiateVariableFont(font, {"wght": axis_value}, inplace=True)
    font.save(os.path.join(OUT, dst))

    subset.main([
        os.path.join(OUT, dst),
        "--unicodes=" + UNICODES,
        "--layout-features=kern,liga,tnum",
        "--no-hinting",
        "--desubroutinize",
        "--name-IDs=*",
        "--drop-tables+=DSIG",
        "--output-file=" + os.path.join(OUT, dst),
    ])
    print(dst, os.path.getsize(os.path.join(OUT, dst)), "bytes")


save_static("PlayfairDisplay-var.ttf", "playfair_display_bold.ttf", 700)
save_static("PlayfairDisplay-var.ttf", "playfair_display_black.ttf", 900)
save_static("Poppins-Medium.ttf", "poppins_medium.ttf")
save_static("Poppins-SemiBold.ttf", "poppins_semibold.ttf")
save_static("Poppins-Bold.ttf", "poppins_bold.ttf")
save_static("Poppins-ExtraBold.ttf", "poppins_extrabold.ttf")