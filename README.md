<p align="center">
  <img src="design/drongpx-icon-preview.png" width="120" alt="DronGPX" />
</p>

<h1 align="center">DronGPX</h1>

<p align="center">
  Scan your drone's QR code, open the point in Maverick. No internet, no accounts, under 5 seconds.
</p>

## What it does

When your drone's controller shares a location as a QR code, DronGPX reads it with the camera,
builds a GPX file with that point, and opens it straight in **Maverick** so it shows up marked on
the map. Everything happens on the phone: no internet is used at any step.

## How to use it

1. Open DronGPX. The app goes straight to the camera, ready to scan.
2. Point it at the QR code shown by the drone's controller.
3. Done: Maverick opens on its own, showing the marked point.
4. When you switch back to DronGPX (back button or recent apps), the camera is already waiting for
   the next point.

No taps are needed between one point and the next, except the first time the system asks for
camera permission.

## What QR codes it can read

Any of these formats work, as long as they carry a valid coordinate:

- A Google Maps link shared from the drone's controller (the most common case).
- A link with `/@lat,lon` (map view).
- A code using the `geo:lat,lon` scheme.
- Plain text with the coordinate, like `-52.746968, -71.363613`.

If the QR code doesn't carry a recognizable coordinate, or the numbers are out of range, the app
shows you the scanned content and a clear error instead of opening Maverick with bad data.

## Where the point is saved

Each scanned point is saved as its own `.gpx` file, named with the date and time of the scan (for
example `dron_2026-11-05_143205.gpx`), and the app also tries to copy it into
**Downloads/DronGPX** on the phone so you can find it or share it later.

> On Android 9 or older, the copy to Downloads may not show up — the app intentionally doesn't ask
> for storage permission, only camera permission. The point still opens in Maverick normally.

## If Maverick isn't installed

DronGPX needs [Maverick Pro](https://play.google.com/store/apps/details?id=com.codesector.maverick.full)
installed to show the point automatically. If it can't find it, it lets you know on screen and
offers a chooser to open the GPX file with another app instead.

## Permissions

DronGPX only asks for **camera** permission, needed to scan the QR code. The app never connects to
the internet: it has no network permission, uses no accounts, and sends no data to any server.

## Requirements

- Android 8.0 (API 26) or newer.
- Maverick Pro installed, so the point opens automatically when you scan.

## Installation

DronGPX isn't distributed on Google Play. Install the APK directly on the phone (sideload), and
allow installation from "unknown sources" when the system asks.
