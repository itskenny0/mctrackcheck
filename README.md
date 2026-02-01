# Create Railway Toolkit

A Minecraft NeoForge addon for the Create mod that displays track curvature radius and slope information while placing railway tracks.

## Features

- **Curvature Display**: Shows the turn radius in blocks while placing curved track segments
- **Slope/Grade Display**: Shows the track grade as a percentage
- **Curvature Rating**: Rates curves as Mainline, Yard, or Too Tight based on configurable thresholds
- **Enforcement Mode**: Hold Ctrl+Alt while placing track to enforce minimum curvature requirements

## Requirements

- Minecraft 1.21.1
- NeoForge 21.1.219 or higher
- Create mod 6.0 or higher

## Configuration

All settings can be configured in the client config file (`railwaytoolkit-client.toml`):

### Display Settings
- `showCurvatureRadius`: Show/hide the radius display
- `showSlope`: Show/hide the slope percentage
- `showCurvatureRating`: Show/hide the curve rating
- `showDecimalPlaces`: Toggle decimal precision in radius display

### Curvature Thresholds
- `mainlineMinRadius`: Minimum radius for mainline curves (default: 90 blocks)
- `yardMinRadius`: Minimum radius for yard curves (default: 20 blocks)
- `absoluteMinRadius`: Minimum allowable radius (default: 7 blocks)

### Enforcement
- `enableEnforcement`: Enable/disable the Ctrl+Alt enforcement feature
- `enforcementLevel`: Which threshold to enforce (MAINLINE, YARD, or ABSOLUTE)

## Usage

1. Install the mod alongside Create
2. While placing track with the track item, the action bar will display:
   - Current curve radius (R: XX)
   - Curve rating (Mainline/Yard/Too Tight)
   - Grade percentage if placing slopes
3. Hold Ctrl+Alt to enable enforcement mode, which prevents placing curves tighter than your configured limit

## Building

```bash
./gradlew build
```

The built JAR will be in `build/libs/`.

## License

This project is licensed under the GNU General Public License v3.0 or later (GPL-3.0-or-later).
See the LICENSE file for details.
