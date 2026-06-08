# ImGui Fonts

ImGuiMC supports automatically loading `.ttf` fonts for use in ImGui windows. It's also possible to manually load fonts
with the `RegisterImGuiFontsEvent` and calling `ImGuiMC#rebuildFonts` to rebuild on demand.

# Resource Packs

All fonts placed in `assets/modid/imgui_font` will be loaded. The name must be a valid resource location followed by `-`
and the variant. If a variant isn't present then the default `regular` variant will be used.

**NOTE: Make sure to add the following to your `.gitattributes` file so your `.ttf` font files aren't corrupted by Git**

```.gitattributes
*.ttf binary
```

### Example

For the font `jetbrains_mono`, the variants would be placed in the `imgui_font` folder as follows:

| File Location                                                      | Font Variant |
|--------------------------------------------------------------------|--------------|
| `assets/imguimc/imgui_font/imguimc:jetbrains_mono-regular.ttf`     | Regular      |
| `assets/imguimc/imgui_font/imguimc:jetbrains_mono-bold.ttf`        | Bold         |
| `assets/imguimc/imgui_font/imguimc:jetbrains_mono-italic.ttf`      | Italic       |
| `assets/imguimc/imgui_font/imguimc:jetbrains_mono-bold_italic.ttf` | Bold Italic  |

## Settings

Fonts can have settings applied using the MCMeta resource metadata file next to the main `.ttf` file.
> fontid-variant.ttf.mcmeta

#### Font Size

```json
{
  "imguimc:font_size": {
    "size": 20
  }
}
```

#### Glyph Ranges

```json
{
  "imguimc:ranges": {
    "ranges": [
      "greek",
      "korean",
      "japanese",
      "chinese",
      "cyrillic",
      "thai",
      "vietnamese",
      {
        "min": 42,
        "max": 37
      }
    ]
  }
}
```

Glyph ranges are useful when using an icon font. For example, `remixicon` can be implemented by adding the `.ttf` file
to the `imgui_font` folder and adding the following `.mcmeta` file:
> remixicon-regular.ttf.mcmeta

```json
{
  "imguimc:font_ranges": {
    "ranges": [
      {
        "min": 59905,
        "max": 62924
      }
    ]
  }
}
```