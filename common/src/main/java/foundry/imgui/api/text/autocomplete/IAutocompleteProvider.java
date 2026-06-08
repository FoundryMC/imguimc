package foundry.imgui.api.text.autocomplete;

import foundry.imgui.api.text.editor.EditorCoordinates;
import foundry.imgui.api.text.editor.EditorGlyph;

import java.util.List;

public interface IAutocompleteProvider {
    List<AutocompleteItem> getCandidates(String prefix,
                                         List<List<EditorGlyph>> lines,
                                         EditorCoordinates cursor);

    // Return true to suppress the popup (e.g. after a dot for swizzles).
    boolean shouldSuppress(String prefix,
                           List<List<EditorGlyph>> lines,
                           EditorCoordinates cursor);

    default int minPrefixLength() { return 2; }

    // If true, accepted function completions get "()" appended.
    default boolean appendParens() { return true; }
}