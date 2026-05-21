import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class TextEditorSmokeTest {
    @Test
    void insertCharAddsCharactersAndMovesCursorRight() {
        TextEditor editor = new TextEditor();
        editor.enterInsertMode();
        editor.insertChar('a');
        editor.insertChar('b');
        editor.insertChar('c');

        assertEquals("abc", editor.getText());
        assertEquals(new CursorPosition(0, 3), editor.getCursor());
    }

    @Test
    void insertNewlineSplitsLineAtCursor() {
        TextEditor editor = editorWithText("abc");
        editor.moveLeft();
        editor.insertChar('\n');

        assertEquals("ab\nc", editor.getText());
        assertEquals(new CursorPosition(1, 0), editor.getCursor());
    }

    @Test
    void deleteCharRemovesPreviousCharacterAndMovesCursorLeft() {
        TextEditor editor = editorWithText("abc");

        editor.deleteChar();

        assertEquals("ab", editor.getText());
        assertEquals(new CursorPosition(0, 2), editor.getCursor());
    }

    @Test
    void deleteCharAtLineStartMergesWithPreviousLine() {
        TextEditor editor = new TextEditor();
        editor.enterInsertMode();
        editor.insertChar('a');
        editor.insertChar('b');
        editor.insertChar('\n');
        editor.insertChar('c');
        editor.moveLeft();

        editor.deleteChar();

        assertEquals("abc", editor.getText());
        assertEquals(new CursorPosition(0, 2), editor.getCursor());
    }

    @Test
    void deleteCharAtDocumentStartDoesNothing() {
        TextEditor editor = editorWithText("a");
        editor.moveLeft();

        editor.deleteChar();

        assertEquals("a", editor.getText());
        assertEquals(new CursorPosition(0, 0), editor.getCursor());
    }

    @Test
    void moveLeftWithinLineOnlyMovesCursor() {
        TextEditor editor = editorWithText("abc");

        editor.moveLeft();

        assertEquals("abc", editor.getText());
        assertEquals(new CursorPosition(0, 2), editor.getCursor());
    }

    @Test
    void moveLeftAtLineStartMovesToPreviousLineTailWithoutChangingText() {
        TextEditor editor = editorWithText("ab\nc");
        editor.moveLeft();
        editor.moveLeft();

        assertEquals("ab\nc", editor.getText());
        assertEquals(new CursorPosition(0, 2), editor.getCursor());
    }

    @Test
    void moveLeftAtDocumentStartDoesNothing() {
        TextEditor editor = editorWithText("a");
        editor.moveLeft();

        assertEquals("a", editor.getText());
        assertEquals(new CursorPosition(0, 0), editor.getCursor());
    }

    @Test
    void moveRightWithinLineOnlyMovesCursor() {
        TextEditor editor = editorWithText("abc");
        editor.moveLeft();
        editor.moveLeft();

        editor.moveRight();

        assertEquals("abc", editor.getText());
        assertEquals(new CursorPosition(0, 2), editor.getCursor());
    }

    @Test
    void moveRightAtLineTailMovesToNextLineHeadWithoutChangingText() {
        TextEditor editor = editorWithText("ab\nc");
        editor.moveLeft();
        editor.moveLeft();

        editor.moveRight();

        assertEquals("ab\nc", editor.getText());
        assertEquals(new CursorPosition(1, 0), editor.getCursor());
    }

    @Test
    void moveRightAtDocumentEndDoesNothing() {
        TextEditor editor = editorWithText("a");

        editor.moveRight();

        assertEquals("a", editor.getText());
        assertEquals(new CursorPosition(0, 1), editor.getCursor());
    }

    @Test
    void moveUpClampsColumnToShorterLine() {
        TextEditor editor = editorWithText("a\nbc");

        editor.moveUp();

        assertEquals("a\nbc", editor.getText());
        assertEquals(new CursorPosition(0, 1), editor.getCursor());
    }

    @Test
    void moveUpAtFirstLineDoesNothing() {
        TextEditor editor = editorWithText("ab\nc");
        editor.moveUp();
        editor.moveUp();

        assertEquals("ab\nc", editor.getText());
        assertEquals(new CursorPosition(0, 1), editor.getCursor());
    }

    @Test
    void moveDownClampsColumnToShorterLine() {
        TextEditor editor = editorWithText("abc\nd");
        editor.moveLeft();
        editor.moveLeft();

        editor.moveDown();

        assertEquals("abc\nd", editor.getText());
        assertEquals(new CursorPosition(1, 1), editor.getCursor());
    }

    @Test
    void moveDownAtLastLineDoesNothing() {
        TextEditor editor = editorWithText("a\nbc");

        editor.moveDown();

        assertEquals("a\nbc", editor.getText());
        assertEquals(new CursorPosition(1, 2), editor.getCursor());
    }

    @Test
    void getTextDoesNotAddTrailingNewline() {
        TextEditor editor = editorWithText("abc\ndef");

        assertEquals("abc\ndef", editor.getText());
    }

    @Test
    void newEditorStartsInNormalMode() {
        TextEditor editor = new TextEditor();

        assertEquals(Mode.NORMAL, editor.getMode());
    }

    @Test
    void enterInsertModeChangesMode() {
        TextEditor editor = new TextEditor();

        editor.enterInsertMode();

        assertEquals(Mode.INSERT, editor.getMode());
    }

    @Test
    void enterNormalModeChangesMode() {
        TextEditor editor = new TextEditor();
        editor.enterInsertMode();

        editor.enterNormalMode();

        assertEquals(Mode.NORMAL, editor.getMode());
    }

    @Test
    void insertCharInNormalModeThrowsException() {
        TextEditor editor = new TextEditor();

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> editor.insertChar('a'));

        assertEquals("Operation is not allowed in normal mode", exception.getMessage());
    }

    @Test
    void deleteCharInNormalModeThrowsException() {
        TextEditor editor = new TextEditor();

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                editor::deleteChar);

        assertEquals("Operation is not allowed in normal mode", exception.getMessage());
    }

    @Test
    void moveToLineStartMovesToColumnZero() {
        TextEditor editor = editorWithText("abc\ndef");

        editor.moveToLineStart();

        assertEquals(new CursorPosition(1, 0), editor.getCursor());
    }

    @Test
    void moveToLineEndMovesToLineLength() {
        TextEditor editor = editorWithText("abc\ndef");
        editor.moveToLineStart();

        editor.moveToLineEnd();

        assertEquals(new CursorPosition(1, 3), editor.getCursor());
    }

    @Test
    void moveWordForwardMovesToNextWordInSameLine() {
        TextEditor editor = editorWithText("one two");
        editor.moveToLineStart();

        editor.moveWordForward();

        assertEquals(new CursorPosition(0, 4), editor.getCursor());
    }

    @Test
    void moveWordForwardTreatsPunctuationAsPartOfWord() {
        TextEditor editor = editorWithText("hello, world test");
        editor.moveToLineStart();

        editor.moveWordForward();

        assertEquals(new CursorPosition(0, 7), editor.getCursor());

        editor.moveWordForward();

        assertEquals(new CursorPosition(0, 13), editor.getCursor());
    }

    @Test
    void moveWordForwardMovesAcrossLineBoundary() {
        TextEditor editor = editorWithText("one\n  two");
        editor.moveToLineStart();

        editor.moveWordForward();

        assertEquals(new CursorPosition(1, 2), editor.getCursor());
    }

    @Test
    void moveWordForwardFromSeparatorMovesToNextWord() {
        TextEditor editor = editorWithText("one  two");
        editor.moveToLineStart();
        editor.moveRight();
        editor.moveRight();
        editor.moveRight();

        editor.moveWordForward();

        assertEquals(new CursorPosition(0, 5), editor.getCursor());
    }

    @Test
    void moveWordForwardAtLastWordStartMovesToWordEnd() {
        TextEditor editor = editorWithText("one");
        editor.moveToLineStart();

        editor.moveWordForward();

        assertEquals(new CursorPosition(0, 3), editor.getCursor());
    }

    @Test
    void moveWordForwardAtDocumentEndDoesNothing() {
        TextEditor editor = editorWithText("one");

        editor.moveWordForward();

        assertEquals(new CursorPosition(0, 3), editor.getCursor());
    }

    @Test
    void moveWordBackwardAtSingleWordStartDoesNothing() {
        TextEditor editor = editorWithText("one");
        editor.moveToLineStart();

        editor.moveWordBackward();

        assertEquals(new CursorPosition(0, 0), editor.getCursor());
    }

    @Test
    void moveWordBackwardMovesToCurrentWordStart() {
        TextEditor editor = editorWithText("one two");
        editor.moveLeft();

        editor.moveWordBackward();

        assertEquals(new CursorPosition(0, 4), editor.getCursor());
    }

    @Test
    void moveWordBackwardTreatsPunctuationAsPartOfWord() {
        TextEditor editor = editorWithText("hello, world test");
        editor.moveToLineStart();
        for (int i = 0; i < 13; i++) {
            editor.moveRight();
        }

        editor.moveWordBackward();

        assertEquals(new CursorPosition(0, 7), editor.getCursor());

        editor.moveWordBackward();

        assertEquals(new CursorPosition(0, 0), editor.getCursor());
    }

    @Test
    void moveWordBackwardMovesAcrossLineBoundary() {
        TextEditor editor = editorWithText("one\n  two");

        editor.moveWordBackward();

        assertEquals(new CursorPosition(1, 2), editor.getCursor());

        editor.moveWordBackward();

        assertEquals(new CursorPosition(0, 0), editor.getCursor());
    }

    @Test
    void moveWordBackwardAtFirstWordDoesNothing() {
        TextEditor editor = editorWithText("one two");
        editor.moveToLineStart();

        editor.moveWordBackward();

        assertEquals(new CursorPosition(0, 0), editor.getCursor());
    }

    @Test
    void moveWordBackwardAtSingleWordEndMovesToWordStart() {
        TextEditor editor = editorWithText("one");

        editor.moveWordBackward();

        assertEquals(new CursorPosition(0, 0), editor.getCursor());
    }

    @Test
    void searchFindsTargetFromCurrentCursorAndMovesCursor() {
        TextEditor editor = editorWithText("hello world hello");
        editor.moveToLineStart();
        editor.moveRight();

        CursorPosition match = editor.search("world");

        assertEquals(new CursorPosition(0, 6), match);
        assertEquals(new CursorPosition(0, 6), editor.getCursor());
    }

    @Test
    void searchCanFindCrossLineTargetInFlattenedDocument() {
        TextEditor editor = editorWithText("abc\ndef");
        editor.moveToLineStart();
        editor.moveUp();

        CursorPosition match = editor.search("c\nd");

        assertEquals(new CursorPosition(0, 2), match);
        assertEquals(new CursorPosition(0, 2), editor.getCursor());
    }

    @Test
    void searchDoesNotWrapAround() {
        TextEditor editor = editorWithText("abc abc");
        editor.moveToLineEnd();

        CursorPosition match = editor.search("abc");

        assertNull(match);
        assertEquals(new CursorPosition(0, 7), editor.getCursor());
    }

    @Test
    void searchRejectsInvalidTarget() {
        TextEditor editor = new TextEditor();

        assertThrows(IllegalArgumentException.class, () -> editor.search(null));
        assertThrows(IllegalArgumentException.class, () -> editor.search(""));
    }

    @Test
    void replaceAllReplacesLineByLine() {
        TextEditor editor = editorWithText("one two\none two");

        editor.replaceAll("one", "three");

        assertEquals("three two\nthree two", editor.getText());
    }

    @Test
    void replaceAllDoesNotMatchAcrossLineBoundary() {
        TextEditor editor = editorWithText("e\nf");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> editor.replaceAll("e\nf", "x"));

        assertEquals("target must not contain newline", exception.getMessage());
        assertEquals("e\nf", editor.getText());
    }

    @Test
    void replaceAllRejectsInvalidInputs() {
        TextEditor editor = new TextEditor();

        assertThrows(IllegalArgumentException.class, () -> editor.replaceAll(null, "x"));
        assertThrows(IllegalArgumentException.class, () -> editor.replaceAll("", "x"));
        assertThrows(IllegalArgumentException.class, () -> editor.replaceAll("x", null));
    }

    @Test
    void replaceAllKeepsCursorPositionWhenPossible() {
        TextEditor editor = editorWithText("abc abc");
        editor.moveToLineStart();
        for (int i = 0; i < 5; i++) {
            editor.moveRight();
        }

        editor.replaceAll("abc", "abcd");

        assertEquals("abcd abcd", editor.getText());
        assertEquals(new CursorPosition(0, 5), editor.getCursor());
    }

    @Test
    void replaceAllClampsCursorColumnWhenLineBecomesShorter() {
        TextEditor editor = editorWithText("abc abc");

        editor.replaceAll("abc", "x");

        assertEquals("x x", editor.getText());
        assertEquals(new CursorPosition(0, 3), editor.getCursor());
    }

    @Test
    void undoAndRedoInsertChar() {
        TextEditor editor = new TextEditor();
        editor.enterInsertMode();

        editor.insertChar('a');

        assertEquals("a", editor.getText());
        assertEquals(new CursorPosition(0, 1), editor.getCursor());

        editor.undo();

        assertEquals("", editor.getText());
        assertEquals(new CursorPosition(0, 0), editor.getCursor());

        editor.redo();

        assertEquals("a", editor.getText());
        assertEquals(new CursorPosition(0, 1), editor.getCursor());
    }

    @Test
    void undoAndRedoDeleteChar() {
        TextEditor editor = editorWithText("abc");

        editor.deleteChar();

        assertEquals("ab", editor.getText());
        assertEquals(new CursorPosition(0, 2), editor.getCursor());

        editor.undo();

        assertEquals("abc", editor.getText());
        assertEquals(new CursorPosition(0, 3), editor.getCursor());

        editor.redo();

        assertEquals("ab", editor.getText());
        assertEquals(new CursorPosition(0, 2), editor.getCursor());
    }

    @Test
    void undoAndRedoReplaceAll() {
        TextEditor editor = editorWithText("abc abc");

        int count = editor.replaceAll("abc", "x");

        assertEquals(2, count);
        assertEquals("x x", editor.getText());
        assertEquals(new CursorPosition(0, 3), editor.getCursor());

        editor.undo();

        assertEquals("abc abc", editor.getText());
        assertEquals(new CursorPosition(0, 7), editor.getCursor());

        editor.redo();

        assertEquals("x x", editor.getText());
        assertEquals(new CursorPosition(0, 3), editor.getCursor());
    }

    @Test
    void undoAndRedoMultipleOperationsInOrder() {
        TextEditor editor = new TextEditor();
        editor.enterInsertMode();
        editor.insertChar('a');
        editor.insertChar('b');
        editor.insertChar('c');

        editor.undo();
        editor.undo();

        assertEquals("a", editor.getText());
        assertEquals(new CursorPosition(0, 1), editor.getCursor());

        editor.redo();

        assertEquals("ab", editor.getText());
        assertEquals(new CursorPosition(0, 2), editor.getCursor());

        editor.redo();

        assertEquals("abc", editor.getText());
        assertEquals(new CursorPosition(0, 3), editor.getCursor());
    }

    @Test
    void undoRestoresMultilineEdit() {
        TextEditor editor = new TextEditor();
        editor.enterInsertMode();
        editor.insertChar('a');
        editor.insertChar('\n');
        editor.insertChar('b');

        editor.undo();

        assertEquals("a\n", editor.getText());
        assertEquals(new CursorPosition(1, 0), editor.getCursor());

        editor.redo();

        assertEquals("a\nb", editor.getText());
        assertEquals(new CursorPosition(1, 1), editor.getCursor());
    }

    @Test
    void undoAndRedoNewlineInsertRestoresSplitLine() {
        TextEditor editor = editorWithText("abc");
        editor.moveLeft();

        editor.insertChar('\n');

        assertEquals("ab\nc", editor.getText());
        assertEquals(new CursorPosition(1, 0), editor.getCursor());

        editor.undo();

        assertEquals("abc", editor.getText());
        assertEquals(new CursorPosition(0, 2), editor.getCursor());

        editor.redo();

        assertEquals("ab\nc", editor.getText());
        assertEquals(new CursorPosition(1, 0), editor.getCursor());
    }

    @Test
    void undoAndRedoDeleteAtLineStartRestoresLineBoundary() {
        TextEditor editor = editorWithText("ab\ncd");
        editor.moveLeft();
        editor.moveLeft();

        editor.deleteChar();

        assertEquals("abcd", editor.getText());
        assertEquals(new CursorPosition(0, 2), editor.getCursor());

        editor.undo();

        assertEquals("ab\ncd", editor.getText());
        assertEquals(new CursorPosition(1, 0), editor.getCursor());

        editor.redo();

        assertEquals("abcd", editor.getText());
        assertEquals(new CursorPosition(0, 2), editor.getCursor());
    }

    @Test
    void undoAndRedoReplaceAllAcrossMultipleLinesRestoresCursor() {
        TextEditor editor = editorWithText("one two\none two");
        editor.moveToLineStart();

        int count = editor.replaceAll("one", "three");

        assertEquals(2, count);
        assertEquals("three two\nthree two", editor.getText());
        assertEquals(new CursorPosition(1, 0), editor.getCursor());

        editor.undo();

        assertEquals("one two\none two", editor.getText());
        assertEquals(new CursorPosition(1, 0), editor.getCursor());

        editor.redo();

        assertEquals("three two\nthree two", editor.getText());
        assertEquals(new CursorPosition(1, 0), editor.getCursor());
    }

    @Test
    void undoAndRedoWithEmptyStacksDoNothing() {
        TextEditor editor = new TextEditor();

        editor.undo();
        editor.redo();

        assertEquals("", editor.getText());
        assertEquals(new CursorPosition(0, 0), editor.getCursor());
    }

    @Test
    void newEditAfterUndoPrunesRedoHistory() {
        TextEditor editor = new TextEditor();
        editor.enterInsertMode();
        editor.insertChar('a');
        editor.insertChar('b');

        editor.undo();
        editor.insertChar('c');
        editor.redo();

        assertEquals("ac", editor.getText());
        assertEquals(new CursorPosition(0, 2), editor.getCursor());
    }

    @Test
    void insertAInsertBUndoRedoRestoresB() {
        TextEditor editor = new TextEditor();
        editor.enterInsertMode();
        editor.insertChar('a');
        editor.insertChar('b');

        editor.undo();

        assertEquals("a", editor.getText());
        assertEquals(new CursorPosition(0, 1), editor.getCursor());

        editor.redo();

        assertEquals("ab", editor.getText());
        assertEquals(new CursorPosition(0, 2), editor.getCursor());
    }

    @Test
    void insertAInsertBUndoInsertCRedoDoesNothing() {
        TextEditor editor = new TextEditor();
        editor.enterInsertMode();
        editor.insertChar('a');
        editor.insertChar('b');
        editor.undo();
        editor.insertChar('c');

        editor.redo();

        assertEquals("ac", editor.getText());
        assertEquals(new CursorPosition(0, 2), editor.getCursor());
    }

    @Test
    void deleteAtDocumentStartDoesNotCreateUndoEntry() {
        TextEditor editor = new TextEditor();
        editor.enterInsertMode();

        editor.deleteChar();
        editor.undo();

        assertEquals("", editor.getText());
        assertEquals(new CursorPosition(0, 0), editor.getCursor());
    }

    @Test
    void replaceAllWithZeroMatchesDoesNotCreateUndoEntry() {
        TextEditor editor = editorWithText("abc");

        int count = editor.replaceAll("x", "y");
        editor.undo();

        assertEquals(0, count);
        assertEquals("ab", editor.getText());
        assertEquals(new CursorPosition(0, 2), editor.getCursor());
    }

    private static TextEditor editorWithText(String text) {
        TextEditor editor = new TextEditor();
        editor.enterInsertMode();
        for (int i = 0; i < text.length(); i++) {
            editor.insertChar(text.charAt(i));
        }
        return editor;
    }
}
