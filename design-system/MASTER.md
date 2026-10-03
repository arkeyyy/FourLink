# FourLink UI — modern game

## Direction
User selected **modern game: compact layout, strong contrast, crisp typography, subtle depth**.
Retain the original light blue / blue / dark blue palette, bright yellow and red chips, and geometric menu background.
Keep the existing logo, developer photos, Amaranth font, and XML Views. The user explicitly rejected white main backgrounds and the replacement font.

## Screens
- Landing: centered brand lockup and board illustration; existing 3-second transition.
- Menu: small brand header, royal-blue game panel, light-blue illustration backing, Play now, two secondary navigation tiles.
- Match: both player names, explicit turn text and outline, square cells, direct board input, and a filled light-red surrender button. No toolbar title or numbered control row.
- Tutorial: one card with three short steps and four separate vector examples, including red and yellow wins.
- Settings: working System / Light / Dark controls, developer link and version. Unfinished settings are omitted.
- Developers: left-aligned profiles; paired columns on tablets.
- Result and surrender: compact scrolling dialogs with explicit actions and consequences.

## Foundations
- Colors: `res/values/ui_colors.xml`; dark overrides: `res/values-night/ui_colors.xml`.
- Light canvas #99CCFF, surface #4DA6FF, black ink, muted #102B4B; menu uses the original geometric blue asset.
- Dark canvas #003366, surface #0052A3, white ink, muted #D1E7FF; menu retains a shaded blue pattern.
- `submit` is the original color reference: untinted geometric menu background, royal-blue #0073E6 menu/settings panels, white panel headings, and #4DA6FF settings rows. Keep the enhanced structure and persistent theme choices; dark mode retains its darker outer canvas.
- Illustration backgrounds #99CCFF, board #0073E6, holes #99CCFF; yellow #FFFF33; red #FF1A1A. Menu shortcuts and selected settings use the original light yellow #FFFF99. Developer credits retain the original royal-blue hero.
- Tutorial cards and dialogs use #B8DDFF in light mode and #16558A in dark mode. Surrender buttons use the original light red #FF8080, navy labels, and 2dp white borders.
- Typography: bundled Amaranth throughout; headings 24/36sp, subheadings 18sp, body 16sp, captions 13sp.
- Spacing: 8/12/16/20/24/32dp. Phone gutter 20dp; tablet gutter 32dp.
- Primary button: 56dp minimum, yellow with dark text, 14dp corners.
- Alternate controls: native Material controls, ripple feedback, 48dp minimum touch targets.
- Corners: 14dp controls, 18dp panels, 24dp hero. Thin outlines and chip edges provide depth.

## Responsive behavior
Content width is capped at 600dp on phones and 900dp on tablets.
Menus use two columns in landscape and on tablets; the landscape match places controls beside the board.
Large text wraps; supporting screens and dialogs scroll.
Player panels stack when enlarged names cannot fit side by side, and retain that arrangement between turns.
Board taps select columns directly. Seven named accessibility actions provide equivalent input; Left/Right selects a keyboard column and Enter/Space drops, with a visible focus outline.
Tutorial example cards stack below 360dp available width or at 150% text and above. Each diagram includes both chip colors, supported columns, and an outlined winning line.
All diagrams are vector assets. The static rotated board illustration uses a software layer to retain every path on the emulator renderer.

## Behavior to preserve
- Yellow starts; successful moves alternate; full columns do not consume a turn.
- Chip drop: original Android BounceInterpolator easing over 400ms at every board height.
- Play Again appears only in the result dialog; no replay button appears on the board during the winning blink.
- Only the first extra tap is queued during a drop.
- Win presentation starts after landing, with the existing 2-second result delay.
- Back opens surrender confirmation; Back during a drop discards the queue and waits for landing. Winning moves take precedence.
- Saved matches restore accepted moves, turn, result deadline, queued input, and dialog actions.
- Appearance follows the device by default; explicit choices persist.
- Reduced motion settles chips immediately and uses a static winning outline.

## Verification
Run:
```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug --offline
.\gradlew.bat :app:connectedDebugAndroidTest --offline
```

`GameAnimationTest` checks direct board touches, accessibility/keyboard input, landing, queueing, cancellation, results, and disabled animation.
`GameRestorationTest` checks recreation, Back, restored dialog actions, and theme selection.
`UiLayoutTest` checks all eight views at 360×640, 448×997 and 800×1280, including landscape, dark mode, and 200% text.
It writes review captures to `/sdcard/Download/fourlink-modern` and restores device settings afterward.

Verified: nine rule/snapshot unit tests, fifteen emulator tests (including 48 screen/configuration checks),
and Android lint with zero errors. The layout matrix covers the refined colors, direct-input game layout,
settings cards, dialogs, and tutorial at large text sizes.
