/*
 * Copyright (C) 2015 Federico Iosue (federico.iosue@gmail.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package it.feio.android.omninotes.utils;

import android.support.v4.util.Pair;
import it.feio.android.omninotes.db.DbHelper;
import it.feio.android.omninotes.models.Note;
import it.feio.android.omninotes.models.Tag;
import it.feio.android.pixlui.links.RegexPatternsConstants;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;


public class TagsHelper {


    public static List<Tag> getAllTags() {
        return DbHelper.getInstance().getTags();
    }


    /**
     * Returns a map of space-separated text tokens (matched to hashtag pattern) to count of how many theirs found in a {note}.
     *
     *
     * Attention: now is resource-intensive algorithm to search whole note for hashtag text tokens.
     * */
    // todo reduce computational complexity.
    public static HashMap<String, Integer> retrieveTags(Note note) {
        HashMap<String, Integer> tagsMap = new HashMap<>();

        String[] wordsOfTheNote = (note.getTitle() + " " + note.getContent())
          .replaceAll("\n", " ").trim().split(" ");

        for (String token : wordsOfTheNote) {
            if (RegexPatternsConstants.HASH_TAG.matcher(token).matches()) {
                //increment count of found tokens in a hashmap.
                int count = tagsMap.get(token) == null ? 0 : tagsMap.get(token);
                tagsMap.put(token, ++count);
            }
        }
        return tagsMap;
    }


    /**
     * Doesn't modify anything. Pure function.
     * Creates a Pair of a tags-to-add string and of tags-to-remove list.
     * @param selectedTags indices which are checked true in {tags}
     * */
    public static Pair<String, List<Tag>> considerTagsOfNote(List<Tag> tags, Integer[] selectedTags, Note note) {
        StringBuilder sbTags = new StringBuilder();
        List<Tag> tagsToRemove = new ArrayList<>();
        HashMap<String, Integer> tagsMap = retrieveTags(note);
        List<Integer> selectedTagsList = Arrays.asList(selectedTags);

        //go trough all tags
        for (int i = 0; i < tags.size(); i++) {
            if (mapContainsTag(tagsMap, tags.get(i))) {
                // tag is in the note, but not in the checked-in.
                if (!selectedTagsList.contains(i)) {
                    tagsToRemove.add(tags.get(i));
                }
            } else {
                // tag is not in the note, but is in the checked-in.
                if (selectedTagsList.contains(i)) {
                    if (sbTags.length() > 0) sbTags.append(" ");
                    sbTags.append(tags.get(i));
                }
            }
        }
        return Pair.create(sbTags.toString(), tagsToRemove);
    }


    private static boolean mapContainsTag(HashMap<String, Integer> tagsMap, Tag tag) {
        for (String tagsMapItem : tagsMap.keySet()) {
            if (tagsMapItem.equals(tag.getText())) {
                return true;
            }
        }
        return false;
    }


    public static Pair<String, String> removeTag(String noteTitle, String noteContent, List<Tag> tagsToRemove) {
        String title = noteTitle, content = noteContent;
        for (Tag tagToRemove : tagsToRemove) {
            title = title.replaceAll(tagToRemove.getText(), "");
            content = content.replaceAll(tagToRemove.getText(), "");
        }
        return new Pair<>(title, content);
    }


    public static String[] getTagsArray(List<Tag> tags) {
        String[] tagsArray = new String[tags.size()];
        for (int i = 0; i < tags.size(); i++) {
            tagsArray[i] = tags.get(i).getText().substring(1) + " (" + tags.get(i).getCount() + ")";
        }
        return tagsArray;
    }


    public static Integer[] getPreselectedTagsArray(Note note, List<Tag> tags) {
        List<Note> notes = new ArrayList<>();
        notes.add(note);
        return getPreselectedTagsArray(notes, tags);
    }

    public static Integer getIndexInList(String tagtext, List<Tag> tags) {
        for (Tag tag : tags) {
            if (tag.getText().equals(tagtext)) {
                return tags.indexOf(tag);
            }
        }
        return null;
    }

    /**
     * Returns empty Integer[] if more than one Note in notes. If one note then indices of every tag in {tags} List  that match this one note.
     * If less than 50, then returns indices of {tags} which included in every of these notes.
     * @param notes some notes
     * @param tags All tags for app.
     * */
    public static Integer[] getPreselectedTagsArray(List<Note> notes, List<Tag> tags) {
        final Integer[] preSelectedTags;
        if (notes.size() == 1) {
            List<Integer> unknownSizeArray = new ArrayList<>();
            for (String noteTag : TagsHelper.retrieveTags(notes.get(0)).keySet()) {
                unknownSizeArray.add(getIndexInList(noteTag, tags));
                    }
            //Cannot generate this array in a first place, because we don't know the exact size.
            preSelectedTags = unknownSizeArray.toArray(new Integer[unknownSizeArray.size()]);
        } else if (notes.size() <= 50){
            //go through all notes
            //search for each tag if it is in Note, or for tags in Note if it in all other Notes.
            HashMap<String, Integer> hashtagToNotesQuantity = new HashMap<>();

            for (Note note: notes){
                Set<String> hashtags = TagsHelper.retrieveTags(note).keySet();
                for(String hashtag : hashtags){
                    final Integer quantity = hashtagToNotesQuantity.get(hashtag);
                    if (quantity == null) hashtagToNotesQuantity.put(hashtag, 1);
                    else hashtagToNotesQuantity.put(hashtag, quantity+1);
                }
            }

            List<Integer> unknownSizeArray = new ArrayList<>();

            for (String hashtag : hashtagToNotesQuantity.keySet()) {
                Integer quantity = hashtagToNotesQuantity.get(hashtag);
                if( quantity != null && quantity == notes.size()) {
                    //this hashtag is in every note.
                    unknownSizeArray.add(getIndexInList(hashtag, tags));
                }
            }

            //Cannot generate this array in a first place, because we don't know the exact size.
            preSelectedTags = unknownSizeArray.toArray(new Integer[unknownSizeArray.size()]);
        } else {
            preSelectedTags = new Integer[]{};
        }
        return preSelectedTags;
    }
}
