---
version: alpha
name: ATLAS
description: A pirate archipelago that serves as an Android home screen.
colors:
  primary: "#c9a567"
  ink: "#263f42"
  parchment: "#e6d2a3"
  wood: "#45372b"
  woodHover: "#6a5338"
  textOnWood: "#fff1cd"
typography:
  display:
    fontFamily: "Pirata One"
  body:
    fontFamily: "Godot default sans-serif"
rounded:
  DEFAULT: "0px"
spacing:
  control: "14px"
components:
  button: {}
  dialog: {}
---

# ATLAS design system

## Overview

Product surface for people using a portrait Android phone, initially tested in English.
The supplied Project Atlas rebuild brief is the product authority. No Japan-market scope
or additional locale support is inferred. The signature is a navigable inhabited archipelago:
installed apps occupy real buildings. Familiar utility controls remain quiet around it.
The former Canvas map is retired. This development milestone is not the cinematic final art pass.

## Colors

Gold denotes navigation and framing. Parchment with dark ink owns menus; pale text on dark
wood owns actions. Color never substitutes for a button label or an update status message.
The runtime theme in `world.gd::_interface` is canonical (model B); these values mirror it.
Theme → `_button` / `_dialog` / `_text` → every Godot menu. Native widget and security
surfaces deliberately retain Android's system UI.

## Typography

Pirata One is reserved for the ATLAS wordmark, district signs, and brief display headings.
Godot's readable sans-serif handles settings, app names and release notes. Original app
labels remain unmodified unless the user saves a building name. Unsupported-script fallback
and TalkBack support still require device verification; do not claim accessibility certification.

## Layout

720 × 1280 logical portrait canvas, stretching with the window. Persistent World, Find app,
and Settings actions sit below the scene. Menus use bounded scroll bodies with Close outside
the scrolling area. Minimum 58 logical units for action rows. Navigation preserves the four
district meanings. App count expands island terrain instead of truncating the catalog.

## Elevation & Depth

Godot meshes, sunlight and perspective create depth. A brass outline separates menus from
the ocean. Floating signs appear while visiting their island; the overview stays quiet.

## Shapes

Square-edged wood panels and parchment sheets; no rounded Android-style buttons in the
Godot surface. Modeled pitched roofs, timber framing, windows and chimneys define houses.

## Components

Buttons share normal, hover/pressed, and high-contrast focus styles. Shared Godot dialogs
own modality and Back/Escape dismissal. Search uses immediate local matching and explicit
Clear. Camera flights last 1.15 seconds, can be interrupted, and become immediate when
Reduce motion is enabled. Berth movement has an explicit button alternative to dragging.
Loading and network failure are text states in the update sheet, with retry available.
Runtime values and their source functions are listed above; no CSS adapter exists.

## Do's and Don'ts

- Use the building world as the primary launch surface.
- Keep app names, settings and update errors readable and literal.
- Never populate the Android catalog with fictional demonstration apps.
- Never represent clock/battery labels as third-party widgets or invented weather data.
